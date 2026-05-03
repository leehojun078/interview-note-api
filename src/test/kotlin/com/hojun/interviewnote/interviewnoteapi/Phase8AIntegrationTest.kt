package com.hojun.interviewnote.interviewnoteapi

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.hojun.interviewnote.interviewnoteapi.exception.AiResponseParseException
import com.hojun.interviewnote.interviewnoteapi.service.ai.InterviewResponseParser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * Phase 8A: 종합 평가 개선 통합 테스트
 *
 * 요구사항:
 * - 종합 피드백 길이 800-1200자 (최소 300자)
 * - 강점/개선점 개수 유연화 (0-5개 / 1-5개)
 * - AI 응답 파서 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Phase 8A: 종합 평가 개선 통합 테스트")
class Phase8AIntegrationTest {

    @Autowired
    private lateinit var interviewResponseParser: InterviewResponseParser

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @DisplayName("종합 피드백 길이 800자 이상 - 정상 케이스")
    fun `should accept feedback with 800+ characters`() {
        // Given: 900자 종합 피드백
        val feedback = "첫 단락(200자): 전반적으로 면접에서 보여준 기술 역량과 커뮤니케이션 능력이 우수했습니다. " +
                "특히 Spring Boot와 Kotlin을 활용한 백엔드 개발 경험을 구체적으로 설명하신 점이 인상적이었으며, " +
                "실무 프로젝트에서의 문제 해결 사례도 명확하게 전달되었습니다. 면접 내내 자신감 있는 태도로 임하셨고, " +
                "질문의 핵심을 정확히 파악하여 답변하는 모습이 좋았습니다. " +
                "둘째 단락(300자): 주요 강점으로는 첫째, RESTful API 설계 및 구현에 대한 깊은 이해를 보여주셨습니다. " +
                "트레이드오프를 고려한 설계 결정 과정을 설명하실 때 기술적 사고력이 돋보였습니다. " +
                "둘째, JPA와 Hibernate를 사용한 ORM 경험이 풍부하며, N+1 문제 해결 사례를 구체적으로 언급하신 점이 좋았습니다. " +
                "셋째, Docker와 Kubernetes를 활용한 컨테이너 배포 경험도 실무에 바로 적용 가능한 수준입니다. " +
                "넷째, 코드 리뷰와 협업 경험을 통해 팀워크 역량도 충분히 검증되었습니다. " +
                "셋째 단락(300자): 개선이 필요한 부분으로는 첫째, 대규모 트래픽 처리 경험이 다소 부족해 보입니다. " +
                "Redis나 Kafka 같은 분산 시스템 기술을 학습하시면 더욱 성장할 수 있을 것입니다. " +
                "둘째, 테스트 코드 작성에 대한 경험을 더 쌓으시면 좋겠습니다. 단위 테스트와 통합 테스트의 차이를 이해하고 " +
                "실무에서 적용하는 연습이 필요합니다. 셋째, 보안 관련 지식을 보강하시면 더욱 완성도 높은 개발자가 되실 수 있습니다. " +
                "특히 OWASP Top 10 취약점에 대한 이해와 대응 방안을 학습하시길 권장합니다. " +
                "마지막 단락(100자): 전반적으로 백엔드 개발자로서의 역량이 충분하며, 지속적인 학습과 성장 의지가 보여 " +
                "긍정적으로 평가됩니다. 우리 팀에 큰 기여를 하실 것으로 기대됩니다. 화이팅하세요!"

        val rawResponse = objectMapper.writeValueAsString(
            mapOf(
                "overallFeedback" to feedback,
                "keyStrengths" to listOf("Spring Boot 숙련도", "API 설계 역량", "문제 해결 능력"),
                "keyImprovements" to listOf("분산 시스템 학습", "테스트 코드 작성"),
                "averageScore" to 3.8,
                "recommendation" to "합격 추천 - 기술 역량 우수"
            )
        )

        // When: 파싱
        val result = interviewResponseParser.parseFinalEvaluation(rawResponse)

        // Then: 800자 이상 피드백 허용
        assertThat(result.overallFeedback.length).isGreaterThanOrEqualTo(800)
        assertThat(result.overallFeedback.length).isLessThanOrEqualTo(1500)
        assertThat(result.averageScore).isEqualTo(3.8)
    }

    @Test
    @DisplayName("종합 피드백 최소 300자 검증")
    fun `should reject feedback with less than 300 characters`() {
        // Given: 200자 피드백 (너무 짧음)
        val shortFeedback = "이 피드백은 200자 미만입니다. " + "X".repeat(160)
        assertThat(shortFeedback.length).isLessThan(300)

        val rawResponse = objectMapper.writeValueAsString(
            mapOf(
                "overallFeedback" to shortFeedback,
                "keyStrengths" to listOf("강점1"),
                "keyImprovements" to listOf("개선1"),
                "averageScore" to 3.0,
                "recommendation" to "보류"
            )
        )

        // When/Then: 300자 미만 거부
        org.junit.jupiter.api.assertThrows<AiResponseParseException> {
            interviewResponseParser.parseFinalEvaluation(rawResponse)
        }
    }

    @Test
    @DisplayName("강점 0개 허용 - 낮은 점수")
    fun `should allow zero strengths for low quality answers`() {
        // Given: 평균 점수 1.5 (매우 낮음), 강점 0개
        val rawResponse = objectMapper.writeValueAsString(
            mapOf(
                "overallFeedback" to "답변 품질이 매우 낮습니다. ".repeat(30),
                "keyStrengths" to emptyList<String>(),
                "keyImprovements" to listOf(
                    "구체적인 기술 스택 언급 필요",
                    "STAR 기법 활용 권장",
                    "수치와 성과 제시",
                    "명확한 커뮤니케이션"
                ),
                "averageScore" to 1.5,
                "recommendation" to "불합격 - 답변 품질 개선 필요"
            )
        )

        // When: 파싱
        val result = interviewResponseParser.parseFinalEvaluation(rawResponse)

        // Then: 강점 0개 허용, 개선점 4개
        val strengths = objectMapper.readValue(
            result.keyStrengths,
            object : TypeReference<List<String>>() {}
        )
        val improvements = objectMapper.readValue(
            result.keyImprovements,
            object : TypeReference<List<String>>() {}
        )

        assertThat(strengths).isEmpty()
        assertThat(improvements).hasSize(4)
        assertThat(result.averageScore).isEqualTo(1.5)
    }

    @Test
    @DisplayName("강점 5개 허용 - 높은 점수")
    fun `should allow five strengths for high quality answers`() {
        // Given: 평균 점수 4.5 (매우 높음), 강점 5개
        val rawResponse = objectMapper.writeValueAsString(
            mapOf(
                "overallFeedback" to "답변 품질이 우수합니다. ".repeat(30),
                "keyStrengths" to listOf(
                    "Spring Boot 전문 지식",
                    "RESTful API 설계 역량",
                    "JPA 최적화 경험",
                    "Docker 배포 경험",
                    "명확한 커뮤니케이션"
                ),
                "keyImprovements" to listOf("대규모 트래픽 처리 경험 보강"),
                "averageScore" to 4.5,
                "recommendation" to "적극 추천 - 즉시 채용 가능"
            )
        )

        // When: 파싱
        val result = interviewResponseParser.parseFinalEvaluation(rawResponse)

        // Then: 강점 5개, 개선점 1개
        val strengths = objectMapper.readValue(
            result.keyStrengths,
            object : TypeReference<List<String>>() {}
        )
        val improvements = objectMapper.readValue(
            result.keyImprovements,
            object : TypeReference<List<String>>() {}
        )

        assertThat(strengths).hasSize(5)
        assertThat(improvements).hasSize(1)
        assertThat(result.averageScore).isEqualTo(4.5)
    }

    @Test
    @DisplayName("강점 6개 거부 - 범위 초과")
    fun `should reject more than 5 strengths`() {
        // Given: 강점 6개 (허용 범위 초과)
        val rawResponse = objectMapper.writeValueAsString(
            mapOf(
                "overallFeedback" to "피드백 내용입니다. ".repeat(40),
                "keyStrengths" to listOf("강점1", "강점2", "강점3", "강점4", "강점5", "강점6"),
                "keyImprovements" to listOf("개선1"),
                "averageScore" to 4.0,
                "recommendation" to "합격"
            )
        )

        // When/Then: 6개 거부
        org.junit.jupiter.api.assertThrows<AiResponseParseException> {
            interviewResponseParser.parseFinalEvaluation(rawResponse)
        }
    }

    @Test
    @DisplayName("개선점 0개 거부 - 최소 1개 필요")
    fun `should reject zero improvements`() {
        // Given: 개선점 0개 (최소 1개 필요)
        val rawResponse = objectMapper.writeValueAsString(
            mapOf(
                "overallFeedback" to "피드백 내용입니다. ".repeat(40),
                "keyStrengths" to listOf("강점1", "강점2"),
                "keyImprovements" to emptyList<String>(),
                "averageScore" to 4.5,
                "recommendation" to "합격"
            )
        )

        // When/Then: 0개 거부
        org.junit.jupiter.api.assertThrows<AiResponseParseException> {
            interviewResponseParser.parseFinalEvaluation(rawResponse)
        }
    }

    @Test
    @DisplayName("개선점 5개 허용 - 최대")
    fun `should allow five improvements`() {
        // Given: 개선점 5개
        val rawResponse = objectMapper.writeValueAsString(
            mapOf(
                "overallFeedback" to "피드백 내용입니다. ".repeat(40),  // 최소 300자
                "keyStrengths" to listOf("강점1"),
                "keyImprovements" to listOf("개선1", "개선2", "개선3", "개선4", "개선5"),
                "averageScore" to 2.5,
                "recommendation" to "보류"
            )
        )

        // When: 파싱
        val result = interviewResponseParser.parseFinalEvaluation(rawResponse)

        // Then: 5개 허용
        val improvements = objectMapper.readValue(
            result.keyImprovements,
            object : TypeReference<List<String>>() {}
        )
        assertThat(improvements).hasSize(5)
    }

    @Test
    @DisplayName("점수 범위 검증 - 1.0-5.0")
    fun `should validate score range`() {
        // Given: 점수 0.5 (범위 밖)
        val rawResponse = objectMapper.writeValueAsString(
            mapOf(
                "overallFeedback" to "피드백 내용입니다. ".repeat(40),
                "keyStrengths" to listOf("강점1"),
                "keyImprovements" to listOf("개선1"),
                "averageScore" to 0.5,
                "recommendation" to "불합격"
            )
        )

        // When/Then: 범위 밖 점수 거부
        org.junit.jupiter.api.assertThrows<AiResponseParseException> {
            interviewResponseParser.parseFinalEvaluation(rawResponse)
        }
    }

    @Test
    @DisplayName("필수 필드 누락 검증")
    fun `should reject missing required fields`() {
        // Given: recommendation 필드 누락
        val rawResponse = objectMapper.writeValueAsString(
            mapOf(
                "overallFeedback" to "피드백 내용입니다. ".repeat(40),
                "keyStrengths" to listOf("강점1"),
                "keyImprovements" to listOf("개선1"),
                "averageScore" to 3.0
            )
        )

        // When/Then: 필수 필드 누락 거부
        org.junit.jupiter.api.assertThrows<AiResponseParseException> {
            interviewResponseParser.parseFinalEvaluation(rawResponse)
        }
    }
}
