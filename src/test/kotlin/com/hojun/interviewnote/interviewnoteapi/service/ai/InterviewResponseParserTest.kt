package com.hojun.interviewnote.interviewnoteapi.service.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.hojun.interviewnote.interviewnoteapi.exception.AiResponseParseException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * InterviewResponseParser 단위 테스트
 *
 * Phase 7B: 면접 AI 응답 파싱 및 검증 테스트
 * - parseInterviewResponse: 평가 + 다음 질문
 * - parseFinalEvaluation: 종합 평가
 * - 200자 질문 제한 검증
 */
class InterviewResponseParserTest {

    private lateinit var interviewResponseParser: InterviewResponseParser
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper().registerKotlinModule()
        interviewResponseParser = InterviewResponseParser(objectMapper)
    }

    // ===== parseInterviewResponse 테스트 =====

    @Test
    fun `parseInterviewResponse - 올바른 JSON 응답을 파싱한다`() {
        // Given
        val validJson = """
            {
              "evaluation": {
                "logicScore": 4,
                "specificityScore": 3,
                "deliveryScore": 4,
                "comment": "답변이 논리적이고 구체적입니다."
              },
              "nextAction": {
                "question": "그 프로젝트에서 가장 어려웠던 기술적 도전은 무엇이었나요?",
                "reasoning": "구체적인 기술 경험을 더 깊이 파악하기 위함",
                "isFollowUp": true
              }
            }
        """.trimIndent()

        // When
        val result = interviewResponseParser.parseInterviewResponse(validJson)

        // Then
        assertEquals(4, result.evaluation.logicScore)
        assertEquals(3, result.evaluation.specificityScore)
        assertEquals(4, result.evaluation.deliveryScore)
        assertEquals("답변이 논리적이고 구체적입니다.", result.evaluation.comment)
        assertEquals("그 프로젝트에서 가장 어려웠던 기술적 도전은 무엇이었나요?", result.nextAction.question)
        assertEquals("구체적인 기술 경험을 더 깊이 파악하기 위함", result.nextAction.reasoning)
        assertTrue(result.nextAction.isFollowUp)
    }

    @Test
    fun `parseInterviewResponse - isFollowUp이 false일 때 정상 처리`() {
        // Given
        val validJson = """
            {
              "evaluation": {
                "logicScore": 3,
                "specificityScore": 3,
                "deliveryScore": 3,
                "comment": "답변 평가"
              },
              "nextAction": {
                "question": "새로운 주제의 질문입니다.",
                "reasoning": "다음 단계로 진행",
                "isFollowUp": false
              }
            }
        """.trimIndent()

        // When
        val result = interviewResponseParser.parseInterviewResponse(validJson)

        // Then
        assertFalse(result.nextAction.isFollowUp)
    }

    @Test
    fun `parseInterviewResponse - isFollowUp이 없으면 기본값 false로 처리`() {
        // Given
        val validJson = """
            {
              "evaluation": {
                "logicScore": 3,
                "specificityScore": 3,
                "deliveryScore": 3,
                "comment": "답변 평가"
              },
              "nextAction": {
                "question": "질문입니다.",
                "reasoning": "이유"
              }
            }
        """.trimIndent()

        // When
        val result = interviewResponseParser.parseInterviewResponse(validJson)

        // Then
        assertFalse(result.nextAction.isFollowUp)
    }

    @Test
    fun `parseInterviewResponse - 질문이 200자일 때 정상 처리`() {
        // Given
        val question = "x".repeat(200)
        val validJson = """
            {
              "evaluation": {
                "logicScore": 3,
                "specificityScore": 3,
                "deliveryScore": 3,
                "comment": "답변 평가"
              },
              "nextAction": {
                "question": "$question",
                "reasoning": "이유",
                "isFollowUp": true
              }
            }
        """.trimIndent()

        // When
        val result = interviewResponseParser.parseInterviewResponse(validJson)

        // Then
        assertEquals(200, result.nextAction.question.length)
    }

    @Test
    fun `parseInterviewResponse - 질문이 200자 초과하면 절단하고 말줄임표를 붙인다`() {
        // Given
        val question = "x".repeat(250)
        val validJson = """
            {
              "evaluation": {
                "logicScore": 3,
                "specificityScore": 3,
                "deliveryScore": 3,
                "comment": "답변 평가"
              },
              "nextAction": {
                "question": "$question",
                "reasoning": "이유",
                "isFollowUp": true
              }
            }
        """.trimIndent()

        // When
        val result = interviewResponseParser.parseInterviewResponse(validJson)

        // Then
        assertEquals(200, result.nextAction.question.length)
        assertTrue(result.nextAction.question.endsWith("..."))
        assertEquals("x".repeat(197) + "...", result.nextAction.question)
    }

    @Test
    fun `parseInterviewResponse - 점수가 1일 때 정상 처리`() {
        // Given
        val validJson = """
            {
              "evaluation": {
                "logicScore": 1,
                "specificityScore": 1,
                "deliveryScore": 1,
                "comment": "답변이 부족합니다"
              },
              "nextAction": {
                "question": "좀 더 구체적으로 설명해주세요.",
                "reasoning": "추가 설명 필요",
                "isFollowUp": true
              }
            }
        """.trimIndent()

        // When
        val result = interviewResponseParser.parseInterviewResponse(validJson)

        // Then
        assertEquals(1, result.evaluation.logicScore)
        assertEquals(1, result.evaluation.specificityScore)
        assertEquals(1, result.evaluation.deliveryScore)
    }

    @Test
    fun `parseInterviewResponse - 점수가 5일 때 정상 처리`() {
        // Given
        val validJson = """
            {
              "evaluation": {
                "logicScore": 5,
                "specificityScore": 5,
                "deliveryScore": 5,
                "comment": "매우 훌륭한 답변입니다"
              },
              "nextAction": {
                "question": "다음 질문입니다.",
                "reasoning": "이유",
                "isFollowUp": false
              }
            }
        """.trimIndent()

        // When
        val result = interviewResponseParser.parseInterviewResponse(validJson)

        // Then
        assertEquals(5, result.evaluation.logicScore)
        assertEquals(5, result.evaluation.specificityScore)
        assertEquals(5, result.evaluation.deliveryScore)
    }

    @Test
    fun `parseInterviewResponse - 점수가 0이면 예외 발생`() {
        // Given
        val invalidJson = """
            {
              "evaluation": {
                "logicScore": 0,
                "specificityScore": 3,
                "deliveryScore": 3,
                "comment": "답변 평가"
              },
              "nextAction": {
                "question": "질문",
                "reasoning": "이유",
                "isFollowUp": true
              }
            }
        """.trimIndent()

        // When & Then
        val exception = assertThrows<AiResponseParseException> {
            interviewResponseParser.parseInterviewResponse(invalidJson)
        }
        assertTrue(exception.cause?.message?.contains("logicScore") == true)
    }

    @Test
    fun `parseInterviewResponse - 점수가 6이면 예외 발생`() {
        // Given
        val invalidJson = """
            {
              "evaluation": {
                "logicScore": 3,
                "specificityScore": 6,
                "deliveryScore": 3,
                "comment": "답변 평가"
              },
              "nextAction": {
                "question": "질문",
                "reasoning": "이유",
                "isFollowUp": true
              }
            }
        """.trimIndent()

        // When & Then
        val exception = assertThrows<AiResponseParseException> {
            interviewResponseParser.parseInterviewResponse(invalidJson)
        }
        assertTrue(exception.cause?.message?.contains("specificityScore") == true)
    }

    @Test
    fun `parseInterviewResponse - 필수 필드 누락 시 예외 발생`() {
        // Given - evaluation 누락
        val invalidJson = """
            {
              "nextAction": {
                "question": "질문",
                "reasoning": "이유",
                "isFollowUp": true
              }
            }
        """.trimIndent()

        // When & Then
        assertThrows<AiResponseParseException> {
            interviewResponseParser.parseInterviewResponse(invalidJson)
        }
    }

    @Test
    fun `parseInterviewResponse - 잘못된 JSON 형식 시 예외 발생`() {
        // Given
        val malformedJson = """
            {
              "evaluation": {
                "logicScore": "not a number"
              }
            }
        """.trimIndent()

        // When & Then
        assertThrows<AiResponseParseException> {
            interviewResponseParser.parseInterviewResponse(malformedJson)
        }
    }

    // ===== parseFinalEvaluation 테스트 =====

    @Test
    fun `parseFinalEvaluation - 올바른 JSON 응답을 파싱한다`() {
        // Given
        val validJson = """
            {
              "overallFeedback": "전반적으로 우수한 답변이었습니다. 기술적 깊이와 커뮤니케이션 능력이 돋보였습니다.",
              "keyStrengths": [
                "논리적이고 체계적인 답변 구조",
                "구체적인 기술 스택 및 프로젝트 경험 제시",
                "문제 해결 과정의 명확한 설명"
              ],
              "keyImprovements": [
                "팀 협업 경험을 더 강조하면 좋을 것",
                "성과 측정 지표를 구체적으로 제시"
              ],
              "averageScore": 4.2,
              "recommendation": "기술 역량과 커뮤니케이션 능력이 우수하여 적극 추천합니다."
            }
        """.trimIndent()

        // When
        val result = interviewResponseParser.parseFinalEvaluation(validJson)

        // Then
        assertTrue(result.overallFeedback.contains("전반적으로 우수한 답변"))
        assertTrue(result.keyStrengths.contains("논리적이고 체계적인 답변 구조"))
        assertTrue(result.keyImprovements.contains("팀 협업 경험을 더 강조하면 좋을 것"))
        assertEquals(4.2, result.averageScore)
        assertTrue(result.recommendation.contains("적극 추천"))
    }

    @Test
    fun `parseFinalEvaluation - keyStrengths가 JSON array string으로 저장된다`() {
        // Given
        val validJson = """
            {
              "overallFeedback": "좋은 답변이었습니다.",
              "keyStrengths": [
                "강점1",
                "강점2"
              ],
              "keyImprovements": [
                "개선1"
              ],
              "averageScore": 3.5,
              "recommendation": "추천합니다."
            }
        """.trimIndent()

        // When
        val result = interviewResponseParser.parseFinalEvaluation(validJson)

        // Then
        val strengths = objectMapper.readTree(result.keyStrengths)
        assertEquals(2, strengths.size())
        assertEquals("강점1", strengths[0].asText())
        assertEquals("강점2", strengths[1].asText())
    }

    @Test
    fun `parseFinalEvaluation - averageScore가 소수점 포함`() {
        // Given
        val validJson = """
            {
              "overallFeedback": "평가",
              "keyStrengths": ["강점"],
              "keyImprovements": ["개선"],
              "averageScore": 3.75,
              "recommendation": "추천"
            }
        """.trimIndent()

        // When
        val result = interviewResponseParser.parseFinalEvaluation(validJson)

        // Then
        assertEquals(3.75, result.averageScore)
    }

    @Test
    fun `parseFinalEvaluation - 필수 필드 누락 시 예외 발생`() {
        // Given - averageScore 누락
        val invalidJson = """
            {
              "overallFeedback": "평가",
              "keyStrengths": ["강점"],
              "keyImprovements": ["개선"],
              "recommendation": "추천"
            }
        """.trimIndent()

        // When & Then
        assertThrows<AiResponseParseException> {
            interviewResponseParser.parseFinalEvaluation(invalidJson)
        }
    }

    @Test
    fun `parseFinalEvaluation - 잘못된 JSON 형식 시 예외 발생`() {
        // Given
        val malformedJson = """
            {
              "averageScore": "not a number"
            }
        """.trimIndent()

        // When & Then
        assertThrows<AiResponseParseException> {
            interviewResponseParser.parseFinalEvaluation(malformedJson)
        }
    }
}
