package com.hojun.interviewnote.interviewnoteapi.service.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.hojun.interviewnote.interviewnoteapi.exception.AiResponseParseException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * InterviewResponseParser 단위 테스트
 *
 * Phase 7E: JSON 파싱, 200자 제한, 점수 검증
 */
class InterviewResponseParserTest {

    private lateinit var parser: InterviewResponseParser
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        parser = InterviewResponseParser(objectMapper)
    }

    @Test
    fun `정상 면접 응답 파싱 성공`() {
        // given: 정상 JSON 응답
        val rawResponse = """
            {
              "evaluation": {
                "logicScore": 4,
                "specificityScore": 3,
                "deliveryScore": 4,
                "comment": "답변이 논리적이고 구체적입니다."
              },
              "nextAction": {
                "question": "주로 사용한 백엔드 프레임워크는 무엇인가요?",
                "reasoning": "구체적인 기술 경험을 확인하기 위한 꼬리 질문",
                "isFollowUp": true
              }
            }
        """.trimIndent()

        // when
        val result = parser.parseInterviewResponse(rawResponse)

        // then
        assertThat(result.evaluation.logicScore).isEqualTo(4)
        assertThat(result.evaluation.specificityScore).isEqualTo(3)
        assertThat(result.evaluation.deliveryScore).isEqualTo(4)
        assertThat(result.evaluation.comment).isEqualTo("답변이 논리적이고 구체적입니다.")

        assertThat(result.nextAction.question).isEqualTo("주로 사용한 백엔드 프레임워크는 무엇인가요?")
        assertThat(result.nextAction.reasoning).isEqualTo("구체적인 기술 경험을 확인하기 위한 꼬리 질문")
        assertThat(result.nextAction.isFollowUp).isTrue()
    }

    @Test
    fun `첫 질문 생성 시 점수 0 허용`() {
        // given: 첫 질문 생성 시 평가 없음 (점수 0)
        val rawResponse = """
            {
              "evaluation": {
                "logicScore": 0,
                "specificityScore": 0,
                "deliveryScore": 0,
                "comment": "첫 질문 생성"
              },
              "nextAction": {
                "question": "자기소개를 부탁드립니다.",
                "reasoning": "면접 시작 질문",
                "isFollowUp": false
              }
            }
        """.trimIndent()

        // when
        val result = parser.parseInterviewResponse(rawResponse)

        // then: MIN_SCORE = 0이므로 통과
        assertThat(result.evaluation.logicScore).isEqualTo(0)
        assertThat(result.evaluation.specificityScore).isEqualTo(0)
        assertThat(result.evaluation.deliveryScore).isEqualTo(0)
    }

    @Test
    fun `질문 길이 200자 초과 시 자동 절단`() {
        // given: 201자 질문
        val longQuestion = "a".repeat(201)
        val rawResponse = """
            {
              "evaluation": {
                "logicScore": 3,
                "specificityScore": 3,
                "deliveryScore": 3,
                "comment": "좋습니다"
              },
              "nextAction": {
                "question": "$longQuestion",
                "reasoning": "테스트",
                "isFollowUp": false
              }
            }
        """.trimIndent()

        // when
        val result = parser.parseInterviewResponse(rawResponse)

        // then: 200자로 절단 (마지막 3글자 "...")
        assertThat(result.nextAction.question.length).isEqualTo(200)
        assertThat(result.nextAction.question).endsWith("...")
    }

    @Test
    fun `점수 범위 초과 시 예외 발생 - 상한`() {
        // given: logicScore = 6 (최대 5 초과)
        val rawResponse = """
            {
              "evaluation": {
                "logicScore": 6,
                "specificityScore": 3,
                "deliveryScore": 3,
                "comment": "범위 초과"
              },
              "nextAction": {
                "question": "질문",
                "reasoning": "이유",
                "isFollowUp": false
              }
            }
        """.trimIndent()

        // when & then
        val exception = assertThrows<Exception> {
            parser.parseInterviewResponse(rawResponse)
        }

        // IllegalArgumentException이 AiResponseParseException으로 래핑됨
        assertThat(exception).isInstanceOfAny(
            IllegalArgumentException::class.java,
            AiResponseParseException::class.java
        )
    }

    @Test
    fun `점수 범위 초과 시 예외 발생 - 하한`() {
        // given: specificityScore = -1 (최소 0 미만)
        val rawResponse = """
            {
              "evaluation": {
                "logicScore": 3,
                "specificityScore": -1,
                "deliveryScore": 3,
                "comment": "범위 초과"
              },
              "nextAction": {
                "question": "질문",
                "reasoning": "이유",
                "isFollowUp": false
              }
            }
        """.trimIndent()

        // when & then
        val exception = assertThrows<Exception> {
            parser.parseInterviewResponse(rawResponse)
        }

        // IllegalArgumentException이 AiResponseParseException으로 래핑됨
        assertThat(exception).isInstanceOfAny(
            IllegalArgumentException::class.java,
            AiResponseParseException::class.java
        )
    }

    @Test
    fun `잘못된 JSON 형식 시 예외 발생`() {
        // given: 잘못된 JSON
        val invalidJson = """
            {
              "evaluation": {
                "logicScore": "잘못된 문자열",
              }
            }
        """.trimIndent()

        // when & then
        assertThrows<AiResponseParseException> {
            parser.parseInterviewResponse(invalidJson)
        }
    }

    @Test
    fun `필수 필드 누락 시 예외 발생`() {
        // given: nextAction 필드 누락
        val missingField = """
            {
              "evaluation": {
                "logicScore": 3,
                "specificityScore": 3,
                "deliveryScore": 3,
                "comment": "좋습니다"
              }
            }
        """.trimIndent()

        // when & then
        assertThrows<Exception> {
            parser.parseInterviewResponse(missingField)
        }
    }

    @Test
    fun `종합 평가 파싱 성공`() {
        // given: 종합 평가 JSON (overallFeedback 최소 300자)
        val rawResponse = """
            {
              "overallFeedback": "지원자는 Spring Boot와 Kotlin을 활용한 백엔드 개발 경험이 풍부하며, 면접 전반에 걸쳐 높은 기술적 이해도를 보여주었습니다. 특히 JPA와 트랜잭션 관리, RESTful API 설계, 마이크로서비스 아키텍처에 대한 깊이 있는 지식이 인상적이었습니다. 실무 프로젝트에서 Redis 캐싱을 도입하여 성능을 30% 개선한 경험과 Docker, Kubernetes를 활용한 컨테이너 오케스트레이션 경험도 구체적으로 설명하였습니다. 다만 대규모 트래픽 처리나 분산 시스템 설계에 대한 실무 경험이 다소 부족한 점은 아쉬웠습니다. 전반적으로 중급에서 시니어 수준의 백엔드 개발자로서 충분한 역량을 갖추고 있으나, 더 높은 수준의 시스템 설계 경험을 쌓는다면 시니어 포지션에도 적합할 것으로 판단됩니다.",
              "keyStrengths": ["Kotlin 숙련도", "명확한 커뮤니케이션", "실무 경험 풍부"],
              "keyImprovements": ["분산 시스템 학습", "대규모 트래픽 처리 경험", "아키텍처 설계 역량"],
              "averageScore": 3.8,
              "recommendation": "보류 - 기술 역량은 우수하나, 요구사항인 Kafka 경험이 부족"
            }
        """.trimIndent()

        // when
        val result = parser.parseFinalEvaluation(rawResponse)

        // then
        assertThat(result.overallFeedback).contains("Spring Boot")
        assertThat(result.overallFeedback.length).isGreaterThanOrEqualTo(300)
        assertThat(result.averageScore).isEqualTo(3.8)
        assertThat(result.recommendation).contains("보류")

        // keyStrengths는 JSON 문자열로 저장됨
        assertThat(result.keyStrengths).contains("Kotlin 숙련도")
        assertThat(result.keyImprovements).contains("분산 시스템 학습")
    }

    @Test
    fun `isFollowUp 필드 누락 시 기본값 false`() {
        // given: isFollowUp 필드 없음
        val rawResponse = """
            {
              "evaluation": {
                "logicScore": 3,
                "specificityScore": 3,
                "deliveryScore": 3,
                "comment": "좋습니다"
              },
              "nextAction": {
                "question": "다음 질문은?",
                "reasoning": "이유"
              }
            }
        """.trimIndent()

        // when
        val result = parser.parseInterviewResponse(rawResponse)

        // then: 기본값 false
        assertThat(result.nextAction.isFollowUp).isFalse()
    }

    @Test
    fun `모든 점수가 최대값 5일 때 정상 처리`() {
        // given: 모든 점수 5점
        val rawResponse = """
            {
              "evaluation": {
                "logicScore": 5,
                "specificityScore": 5,
                "deliveryScore": 5,
                "comment": "완벽한 답변입니다"
              },
              "nextAction": {
                "question": "다음 질문",
                "reasoning": "이유",
                "isFollowUp": false
              }
            }
        """.trimIndent()

        // when
        val result = parser.parseInterviewResponse(rawResponse)

        // then
        assertThat(result.evaluation.logicScore).isEqualTo(5)
        assertThat(result.evaluation.specificityScore).isEqualTo(5)
        assertThat(result.evaluation.deliveryScore).isEqualTo(5)
    }
}
