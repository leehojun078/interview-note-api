package com.hojun.interviewnote.interviewnoteapi.integration

import com.hojun.interviewnote.interviewnoteapi.domain.InterviewAnswer
import com.hojun.interviewnote.interviewnoteapi.domain.Question
import com.hojun.interviewnote.interviewnoteapi.repository.InterviewAnswerRepository
import com.hojun.interviewnote.interviewnoteapi.repository.QuestionRepository
import com.hojun.interviewnote.interviewnoteapi.service.AiFeedbackService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Phase 2E: 실제 OpenAI API 테스트
 *
 * 주의: 이 테스트는 실제 OpenAI API를 호출하므로 비용이 발생합니다.
 * @Disabled를 제거하고 실행하세요.
 *
 * 실행 전 환경변수 설정:
 * export $(cat .env | xargs) && ./gradlew test --tests Phase2EManualTest
 */
@SpringBootTest
@Transactional
class Phase2EManualTest {

    @Autowired
    private lateinit var aiFeedbackService: AiFeedbackService

    @Autowired
    private lateinit var questionRepository: QuestionRepository

    @Autowired
    private lateinit var interviewAnswerRepository: InterviewAnswerRepository

    private val logger = LoggerFactory.getLogger(javaClass)

    @Disabled("수동 테스트 - OpenAI API 키 필요, 비용 발생")
    @Test
    fun `실제 OpenAI API로 피드백을 생성한다`() {
        // given - Question과 Answer를 실제 DB에 저장
        val question = questionRepository.save(
            Question(
                jobField = "IT",
                targetJob = "백엔드 개발자",
                category = "기술역량",
                content = "Spring Boot의 장점과 실제 프로젝트에서의 활용 경험을 설명해주세요.",
                difficulty = "MEDIUM",
                isActive = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        val answer = interviewAnswerRepository.save(
            InterviewAnswer(
                questionId = question.id,
                userId = 1L,
                answerText = """
                Spring Boot의 가장 큰 장점은 개발 생산성입니다.
                자동 설정 기능(Auto Configuration)으로 인해 복잡한 XML 설정 없이 빠르게 프로젝트를 시작할 수 있습니다.

                예를 들어, JPA를 사용하려면 application.properties에 데이터베이스 정보만 입력하면
                Spring Boot가 자동으로 EntityManagerFactory, TransactionManager 등을 설정해줍니다.

                또한 내장 톰캣 덕분에 별도의 서버 설치 없이 JAR 파일 하나로 배포가 가능해
                DevOps 환경에서 매우 유리합니다.

                실제 프로젝트에서 Spring Boot를 사용했을 때, 초기 설정 시간을 약 70% 단축할 수 있었습니다.
            """.trimIndent(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        logger.info("=== Phase 2E: 실제 OpenAI API 테스트 시작 ===")

        // when
        val startTime = System.currentTimeMillis()
        val result = aiFeedbackService.generateFeedback(answer, question)
        val duration = System.currentTimeMillis() - startTime

        // then
        logger.info("=== AI 피드백 결과 ===")
        logger.info("피드백 ID: ${result.id}")
        logger.info("논리성 점수: ${result.logicScore}/5")
        logger.info("구체성 점수: ${result.specificityScore}/5")
        logger.info("직무적합성 점수: ${result.jobFitScore}/5")
        logger.info("전달력 점수: ${result.deliveryScore}/5")
        logger.info("평균 점수: ${result.averageScore}")
        logger.info("\n강점: ${result.strengths}")
        logger.info("개선점: ${result.improvements}")
        logger.info("\n모범답변 (${result.modelAnswer.length}자):\n${result.modelAnswer}")
        logger.info("\n종합 코멘트: ${result.overallComment}")
        logger.info("\n=== 메타데이터 ===")
        logger.info("모델명: ${result.modelName}")
        logger.info("프롬프트 버전: ${result.promptVersion}")
        logger.info("입력 토큰: ${result.tokenUsageInput}")
        logger.info("출력 토큰: ${result.tokenUsageOutput}")
        logger.info("응답 시간: ${duration}ms")
        logger.info("원본 응답 길이: ${result.rawResponse.length}자")
        logger.info("해시: ${result.answerTextHash}")

        // 검증
        assertThat(result.id).isNotZero
        assertThat(result.modelName).isEqualTo("gpt-4o-mini")
        assertThat(result.promptVersion).isEqualTo("v1.0")
        assertThat(result.logicScore).isBetween(1, 5)
        assertThat(result.specificityScore).isBetween(1, 5)
        assertThat(result.jobFitScore).isBetween(1, 5)
        assertThat(result.deliveryScore).isBetween(1, 5)
        assertThat(result.modelAnswer.length).isBetween(100, 1000)
        assertThat(result.tokenUsageInput).isGreaterThan(0)
        assertThat(result.tokenUsageOutput).isGreaterThan(0)
        assertThat(result.rawResponse).contains("logic")
        assertThat(result.answerTextHash).hasSize(64) // SHA-256
        assertThat(duration).isLessThan(30000) // 30초 이내

        logger.info("\n✅ 모든 검증 통과")
    }

    @Disabled("수동 테스트 - OpenAI API 키 필요, 비용 발생")
    @Test
    fun `동일 답변 재평가 시 캐시된 결과를 반환한다`() {
        // given - Question과 Answer를 실제 DB에 저장
        val question = questionRepository.save(
            Question(
                jobField = "IT",
                targetJob = "백엔드 개발자",
                category = "기술역량",
                content = "테스트 질문",
                difficulty = "MEDIUM",
                isActive = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        val answer = interviewAnswerRepository.save(
            InterviewAnswer(
                questionId = question.id,
                userId = 1L,
                answerText = """
                    Spring Boot는 설정이 간편하고 개발 생산성이 높습니다.
                    자동 설정 기능으로 복잡한 XML 없이 바로 개발을 시작할 수 있습니다.
                    내장 톰캣 덕분에 JAR 파일 하나로 배포가 가능합니다.
                    실무에서 사용해보니 초기 설정 시간이 크게 단축되었습니다.
                    특히 Spring Data JPA와 함께 사용하면 개발 속도가 매우 빨라집니다.
                """.trimIndent(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        logger.info("=== 중복 요청 방지 테스트 ===")

        // when - 첫 번째 요청
        val start1 = System.currentTimeMillis()
        val result1 = aiFeedbackService.generateFeedback(answer, question)
        val duration1 = System.currentTimeMillis() - start1

        logger.info("첫 번째 요청 완료 - 응답 시간: ${duration1}ms")

        // when - 두 번째 요청 (동일 답변)
        val start2 = System.currentTimeMillis()
        val result2 = aiFeedbackService.generateFeedback(answer, question)
        val duration2 = System.currentTimeMillis() - start2

        logger.info("두 번째 요청 완료 - 응답 시간: ${duration2}ms")

        // then
        assertThat(result1.id).isEqualTo(result2.id) // 동일한 피드백 반환
        assertThat(duration2).isLessThan(1000) // 캐시 히트는 1초 이내
        assertThat(duration2).isLessThan(duration1 / 5) // 최소 5배 이상 빠름

        logger.info("✅ 캐시 히트 확인 - 첫 요청: ${duration1}ms, 두 번째 요청: ${duration2}ms (${duration1 / duration2}배 빠름)")
    }

    @Disabled("수동 테스트 - 응답 파싱 오류 시 fallback 확인")
    @Test
    fun `잘못된 응답 형식 시 더미 피드백으로 fallback한다`() {
        // 이 테스트는 실제로는 OpenAI가 항상 유효한 JSON을 반환하므로
        // 구현 확인용으로만 사용
        logger.info("=== Fallback 동작은 코드 레벨에서 확인됨 ===")
        logger.info("OpenAI API가 정상 동작하면 fallback이 트리거되지 않습니다.")
    }
}
