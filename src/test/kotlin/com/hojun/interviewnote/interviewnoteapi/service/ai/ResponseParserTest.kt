package com.hojun.interviewnote.interviewnoteapi.service.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.hojun.interviewnote.interviewnoteapi.exception.AiResponseParseException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * ResponseParser 단위 테스트
 *
 * OpenAI JSON 응답 파싱 및 검증 로직을 테스트합니다.
 */
class ResponseParserTest {

    private lateinit var responseParser: ResponseParser
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper().registerKotlinModule()
        responseParser = ResponseParser(objectMapper)
    }

    @Test
    fun `parseOpenAiResponse - 올바른 JSON 응답을 파싱한다`() {
        // Given
        val validJson = """
            {
              "scores": {
                "logic": 4,
                "specificity": 3,
                "jobFit": 5,
                "delivery": 4
              },
              "strengths": [
                "논리적인 구조",
                "구체적인 예시 제시"
              ],
              "improvements": [
                "기술적 깊이 추가",
                "결과 측정 지표 포함"
              ],
              "modelAnswer": "${"x".repeat(400)}",
              "overallComment": "전반적으로 좋은 답변입니다."
            }
        """.trimIndent()

        // When
        val result = responseParser.parseOpenAiResponse(validJson)

        // Then
        assertEquals(4, result.logicScore)
        assertEquals(3, result.specificityScore)
        assertEquals(5, result.jobFitScore)
        assertEquals(4, result.deliveryScore)
        assertEquals(2, result.strengths.size)
        assertEquals("논리적인 구조", result.strengths[0])
        assertEquals(2, result.improvements.size)
        assertEquals("기술적 깊이 추가", result.improvements[0])
        assertEquals(400, result.modelAnswer.length)
        assertEquals("전반적으로 좋은 답변입니다.", result.overallComment)
    }

    @Test
    fun `parseOpenAiResponse - 점수가 1-5 범위를 벗어나면 예외 발생`() {
        // Given - logic 점수가 0
        val invalidScoreJson = """
            {
              "scores": {
                "logic": 0,
                "specificity": 3,
                "jobFit": 4,
                "delivery": 3
              },
              "strengths": ["강점1", "강점2"],
              "improvements": ["개선1", "개선2"],
              "modelAnswer": "${"x".repeat(400)}",
              "overallComment": "코멘트"
            }
        """.trimIndent()

        // When & Then
        val exception = assertThrows<AiResponseParseException> {
            responseParser.parseOpenAiResponse(invalidScoreJson)
        }
        assert(exception.message!!.contains("logic 점수는 1-5 사이여야 합니다"))
    }

    @Test
    fun `parseOpenAiResponse - 점수가 6 이상이면 예외 발생`() {
        // Given - delivery 점수가 6
        val invalidScoreJson = """
            {
              "scores": {
                "logic": 4,
                "specificity": 3,
                "jobFit": 4,
                "delivery": 6
              },
              "strengths": ["강점1", "강점2"],
              "improvements": ["개선1", "개선2"],
              "modelAnswer": "${"x".repeat(400)}",
              "overallComment": "코멘트"
            }
        """.trimIndent()

        // When & Then
        val exception = assertThrows<AiResponseParseException> {
            responseParser.parseOpenAiResponse(invalidScoreJson)
        }
        assert(exception.message!!.contains("delivery 점수는 1-5 사이여야 합니다"))
    }

    @Test
    fun `parseOpenAiResponse - strengths가 0개이면 예외 발생`() {
        // Given
        val invalidJson = """
            {
              "scores": {"logic": 4, "specificity": 3, "jobFit": 4, "delivery": 3},
              "strengths": [],
              "improvements": ["개선1", "개선2"],
              "modelAnswer": "${"x".repeat(400)}",
              "overallComment": "코멘트"
            }
        """.trimIndent()

        // When & Then
        val exception = assertThrows<AiResponseParseException> {
            responseParser.parseOpenAiResponse(invalidJson)
        }
        assert(exception.message!!.contains("strengths는 1-5개여야 합니다"))
    }

    @Test
    fun `parseOpenAiResponse - strengths가 5개 초과하면 예외 발생`() {
        // Given
        val invalidJson = """
            {
              "scores": {"logic": 4, "specificity": 3, "jobFit": 4, "delivery": 3},
              "strengths": ["강점1", "강점2", "강점3", "강점4", "강점5", "강점6"],
              "improvements": ["개선1", "개선2"],
              "modelAnswer": "${"x".repeat(400)}",
              "overallComment": "코멘트"
            }
        """.trimIndent()

        // When & Then
        val exception = assertThrows<AiResponseParseException> {
            responseParser.parseOpenAiResponse(invalidJson)
        }
        assert(exception.message!!.contains("strengths는 1-5개여야 합니다"))
    }

    @Test
    fun `parseOpenAiResponse - improvements가 0개이면 예외 발생`() {
        // Given
        val invalidJson = """
            {
              "scores": {"logic": 4, "specificity": 3, "jobFit": 4, "delivery": 3},
              "strengths": ["강점1", "강점2"],
              "improvements": [],
              "modelAnswer": "${"x".repeat(400)}",
              "overallComment": "코멘트"
            }
        """.trimIndent()

        // When & Then
        val exception = assertThrows<AiResponseParseException> {
            responseParser.parseOpenAiResponse(invalidJson)
        }
        assert(exception.message!!.contains("improvements는 1-5개여야 합니다"))
    }

    @Test
    fun `parseOpenAiResponse - strengths에 빈 문자열이 포함되면 예외 발생`() {
        // Given
        val invalidJson = """
            {
              "scores": {"logic": 4, "specificity": 3, "jobFit": 4, "delivery": 3},
              "strengths": ["강점1", ""],
              "improvements": ["개선1", "개선2"],
              "modelAnswer": "${"x".repeat(400)}",
              "overallComment": "코멘트"
            }
        """.trimIndent()

        // When & Then
        val exception = assertThrows<AiResponseParseException> {
            responseParser.parseOpenAiResponse(invalidJson)
        }
        assert(exception.message!!.contains("strengths에 빈 항목이 포함되어 있습니다"))
    }

    @Test
    fun `parseOpenAiResponse - modelAnswer가 100자 미만이면 예외 발생`() {
        // Given
        val invalidJson = """
            {
              "scores": {"logic": 4, "specificity": 3, "jobFit": 4, "delivery": 3},
              "strengths": ["강점1", "강점2"],
              "improvements": ["개선1", "개선2"],
              "modelAnswer": "짧은 답변",
              "overallComment": "코멘트"
            }
        """.trimIndent()

        // When & Then
        val exception = assertThrows<AiResponseParseException> {
            responseParser.parseOpenAiResponse(invalidJson)
        }
        assert(exception.message!!.contains("모범답변 길이는 100-1000자 사이여야 합니다"))
    }

    @Test
    fun `parseOpenAiResponse - modelAnswer가 1000자 초과하면 예외 발생`() {
        // Given
        val invalidJson = """
            {
              "scores": {"logic": 4, "specificity": 3, "jobFit": 4, "delivery": 3},
              "strengths": ["강점1", "강점2"],
              "improvements": ["개선1", "개선2"],
              "modelAnswer": "${"x".repeat(1001)}",
              "overallComment": "코멘트"
            }
        """.trimIndent()

        // When & Then
        val exception = assertThrows<AiResponseParseException> {
            responseParser.parseOpenAiResponse(invalidJson)
        }
        assert(exception.message!!.contains("모범답변 길이는 100-1000자 사이여야 합니다"))
    }

    @Test
    fun `parseOpenAiResponse - 잘못된 JSON 형식이면 예외 발생`() {
        // Given
        val malformedJson = """
            {
              "scores": {
                "logic": "not a number"
              }
            }
        """.trimIndent()

        // When & Then
        val exception = assertThrows<AiResponseParseException> {
            responseParser.parseOpenAiResponse(malformedJson)
        }
        assert(exception.message!!.contains("JSON 형식이 잘못되었습니다"))
    }

    @Test
    fun `parseOpenAiResponse - 필수 필드가 없으면 예외 발생`() {
        // Given - scores 필드 누락
        val incompleteJson = """
            {
              "strengths": ["강점1", "강점2"],
              "improvements": ["개선1", "개선2"],
              "modelAnswer": "${"x".repeat(400)}",
              "overallComment": "코멘트"
            }
        """.trimIndent()

        // When & Then
        assertThrows<AiResponseParseException> {
            responseParser.parseOpenAiResponse(incompleteJson)
        }
    }

    @Test
    fun `parseOpenAiResponse - strengths가 정확히 3개일 때 정상 처리`() {
        // Given
        val validJson = """
            {
              "scores": {"logic": 4, "specificity": 3, "jobFit": 4, "delivery": 3},
              "strengths": ["강점1", "강점2", "강점3"],
              "improvements": ["개선1", "개선2"],
              "modelAnswer": "${"x".repeat(400)}",
              "overallComment": "코멘트"
            }
        """.trimIndent()

        // When
        val result = responseParser.parseOpenAiResponse(validJson)

        // Then
        assertEquals(3, result.strengths.size)
    }

    @Test
    fun `parseOpenAiResponse - modelAnswer가 정확히 100자일 때 정상 처리`() {
        // Given
        val validJson = """
            {
              "scores": {"logic": 4, "specificity": 3, "jobFit": 4, "delivery": 3},
              "strengths": ["강점1", "강점2"],
              "improvements": ["개선1", "개선2"],
              "modelAnswer": "${"x".repeat(100)}",
              "overallComment": "코멘트"
            }
        """.trimIndent()

        // When
        val result = responseParser.parseOpenAiResponse(validJson)

        // Then
        assertEquals(100, result.modelAnswer.length)
    }

    @Test
    fun `parseOpenAiResponse - modelAnswer가 정확히 1000자일 때 정상 처리`() {
        // Given
        val validJson = """
            {
              "scores": {"logic": 4, "specificity": 3, "jobFit": 4, "delivery": 3},
              "strengths": ["강점1", "강점2"],
              "improvements": ["개선1", "개선2"],
              "modelAnswer": "${"x".repeat(1000)}",
              "overallComment": "코멘트"
            }
        """.trimIndent()

        // When
        val result = responseParser.parseOpenAiResponse(validJson)

        // Then
        assertEquals(1000, result.modelAnswer.length)
    }

    @Test
    fun `parseOpenAiResponse - 모든 점수가 최소값(1)일 때 정상 처리`() {
        // Given
        val validJson = """
            {
              "scores": {"logic": 1, "specificity": 1, "jobFit": 1, "delivery": 1},
              "strengths": ["강점1", "강점2"],
              "improvements": ["개선1", "개선2"],
              "modelAnswer": "${"x".repeat(400)}",
              "overallComment": "코멘트"
            }
        """.trimIndent()

        // When
        val result = responseParser.parseOpenAiResponse(validJson)

        // Then
        assertEquals(1, result.logicScore)
        assertEquals(1, result.specificityScore)
        assertEquals(1, result.jobFitScore)
        assertEquals(1, result.deliveryScore)
    }

    @Test
    fun `parseOpenAiResponse - 모든 점수가 최대값(5)일 때 정상 처리`() {
        // Given
        val validJson = """
            {
              "scores": {"logic": 5, "specificity": 5, "jobFit": 5, "delivery": 5},
              "strengths": ["강점1", "강점2"],
              "improvements": ["개선1", "개선2"],
              "modelAnswer": "${"x".repeat(400)}",
              "overallComment": "코멘트"
            }
        """.trimIndent()

        // When
        val result = responseParser.parseOpenAiResponse(validJson)

        // Then
        assertEquals(5, result.logicScore)
        assertEquals(5, result.specificityScore)
        assertEquals(5, result.jobFitScore)
        assertEquals(5, result.deliveryScore)
    }
}
