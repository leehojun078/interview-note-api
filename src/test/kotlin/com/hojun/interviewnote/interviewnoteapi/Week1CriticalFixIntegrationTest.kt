package com.hojun.interviewnote.interviewnoteapi

import com.hojun.interviewnote.interviewnoteapi.domain.InterviewAnswer
import com.hojun.interviewnote.interviewnoteapi.domain.Question
import com.hojun.interviewnote.interviewnoteapi.exception.AiResponseParseException
import com.hojun.interviewnote.interviewnoteapi.repository.AiFeedbackRepository
import com.hojun.interviewnote.interviewnoteapi.repository.InterviewAnswerRepository
import com.hojun.interviewnote.interviewnoteapi.repository.QuestionRepository
import com.hojun.interviewnote.interviewnoteapi.service.AiFeedbackService
import com.hojun.interviewnote.interviewnoteapi.service.ai.AiClient
import com.hojun.interviewnote.interviewnoteapi.service.ai.ResponseParser
import com.hojun.interviewnote.interviewnoteapi.service.ratelimit.RateLimitService
import com.hojun.interviewnote.interviewnoteapi.exception.RateLimitExceededException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Week 1 Critical Fix 통합 테스트
 *
 * Critical Issue #1: AiFeedbackService 트랜잭션 경계 분리 검증
 * - AI 파싱 실패 시 중복 피드백 생성되지 않음
 * - 각 save 작업이 독립적인 트랜잭션에서 실행됨
 *
 * Critical Issue #2: RateLimitService Race Condition 검증
 * - 동시 요청 시 Rate Limit이 정확히 적용됨
 * - synchronized 블록으로 동시성 문제 해결
 */
@SpringBootTest
@ActiveProfiles("test")
class Week1CriticalFixIntegrationTest {

    @Autowired
    private lateinit var aiFeedbackService: AiFeedbackService

    @Autowired
    private lateinit var aiFeedbackRepository: AiFeedbackRepository

    @Autowired
    private lateinit var interviewAnswerRepository: InterviewAnswerRepository

    @Autowired
    private lateinit var questionRepository: QuestionRepository

    @Autowired
    private lateinit var rateLimitService: RateLimitService

    @MockBean
    private lateinit var aiClient: AiClient

    @MockBean
    private lateinit var responseParser: ResponseParser

    private lateinit var testQuestion: Question
    private lateinit var testAnswer: InterviewAnswer

    @BeforeEach
    fun setUp() {
        // 테스트용 질문 조회 (Migration에서 삽입된 질문 사용)
        testQuestion = questionRepository.findAll().first()

        // 테스트용 답변 생성
        testAnswer = InterviewAnswer(
            questionId = testQuestion.id,
            userId = 1L,
            answerText = "트랜잭션 경계 테스트용 답변입니다. " + "x".repeat(100),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        testAnswer = interviewAnswerRepository.save(testAnswer)
    }

    @AfterEach
    fun tearDown() {
        // 테스트 데이터 정리
        aiFeedbackRepository.deleteAll()
        interviewAnswerRepository.deleteAll()
    }

    @Test
    fun `Critical Fix #1 - AI 파싱 실패 시 더미 피드백만 저장되어야 함 (중복 방지)`() {
        // Given
        // AI 응답은 성공하지만 파싱 실패 시나리오
        whenever(aiClient.requestFeedback(any(), any())).thenReturn("{invalid json}")
        whenever(responseParser.parseOpenAiResponse(any(), any()))
            .thenThrow(AiResponseParseException("JSON 파싱 실패", "{invalid json}"))

        // When
        val result = aiFeedbackService.generateFeedback(testAnswer, testQuestion)

        // Then
        // 1. 더미 피드백이 반환되었는지 확인
        assertThat(result.modelName).isEqualTo("dummy-model-v1")
        assertThat(result.promptVersion).isEqualTo("v1.0-dummy")
        assertThat(result.overallComment).contains("더미 피드백")

        // 2. 데이터베이스에 저장된 피드백이 1개만 있어야 함 (중복 방지)
        val allFeedbacks = aiFeedbackRepository.findAll()
        assertThat(allFeedbacks).hasSize(1)

        // 3. 저장된 피드백이 더미 피드백인지 확인
        val savedFeedback = allFeedbacks[0]
        assertThat(savedFeedback.modelName).isEqualTo("dummy-model-v1")
        assertThat(savedFeedback.interviewAnswerId).isEqualTo(testAnswer.id)
    }

    @Test
    @Transactional
    fun `Critical Fix #1 - AI 성공 시 실제 피드백만 저장되어야 함`() {
        // Given
        val rawResponse = """
            {
              "scores": {"logic": 4, "specificity": 3, "jobFit": 5, "delivery": 4},
              "strengths": ["강점1", "강점2"],
              "improvements": ["개선1", "개선2"],
              "modelAnswer": "모범답변입니다.",
              "overallComment": "좋은 답변입니다."
            }
        """.trimIndent()

        val parsedFeedback = com.hojun.interviewnote.interviewnoteapi.service.ai.ParsedFeedback(
            logicScore = 4,
            specificityScore = 3,
            jobFitScore = 5,
            deliveryScore = 4,
            strengths = listOf("강점1", "강점2"),
            improvements = listOf("개선1", "개선2"),
            modelAnswer = "모범답변입니다.",
            overallComment = "좋은 답변입니다."
        )

        whenever(aiClient.requestFeedback(any(), any())).thenReturn(rawResponse)
        whenever(responseParser.parseOpenAiResponse(any(), any())).thenReturn(parsedFeedback)

        // When
        val result = aiFeedbackService.generateFeedback(testAnswer, testQuestion)

        // Then
        // 1. 실제 AI 피드백이 반환되었는지 확인
        assertThat(result.logicScore).isEqualTo(4)
        assertThat(result.specificityScore).isEqualTo(3)
        assertThat(result.jobFitScore).isEqualTo(5)
        assertThat(result.deliveryScore).isEqualTo(4)
        assertThat(result.modelAnswer).isEqualTo("모범답변입니다.")

        // 2. 데이터베이스에 저장된 피드백이 1개만 있어야 함
        val allFeedbacks = aiFeedbackRepository.findAll()
        assertThat(allFeedbacks).hasSize(1)

        // 3. 저장된 피드백이 실제 AI 피드백인지 확인 (더미가 아님)
        val savedFeedback = allFeedbacks[0]
        assertThat(savedFeedback.modelName).isNotEqualTo("dummy-model-v1")
        assertThat(savedFeedback.promptVersion).isNotEqualTo("v1.0-dummy")
        assertThat(savedFeedback.interviewAnswerId).isEqualTo(testAnswer.id)
    }

    @Test
    fun `Critical Fix #1 - 트랜잭션 분리로 인해 각 save는 독립적으로 커밋됨`() {
        // Given
        // 첫 번째 호출: AI 성공
        val rawResponse1 = """
            {
              "scores": {"logic": 5, "specificity": 5, "jobFit": 5, "delivery": 5},
              "strengths": ["강점1"],
              "improvements": ["개선1"],
              "modelAnswer": "첫 번째 모범답변",
              "overallComment": "훌륭합니다."
            }
        """.trimIndent()

        val parsedFeedback1 = com.hojun.interviewnote.interviewnoteapi.service.ai.ParsedFeedback(
            logicScore = 5,
            specificityScore = 5,
            jobFitScore = 5,
            deliveryScore = 5,
            strengths = listOf("강점1"),
            improvements = listOf("개선1"),
            modelAnswer = "첫 번째 모범답변",
            overallComment = "훌륭합니다."
        )

        whenever(aiClient.requestFeedback(any(), any())).thenReturn(rawResponse1)
        whenever(responseParser.parseOpenAiResponse(any(), any())).thenReturn(parsedFeedback1)

        // When
        val result1 = aiFeedbackService.generateFeedback(testAnswer, testQuestion)

        // Then - 첫 번째 호출 후 1개 저장됨
        assertThat(aiFeedbackRepository.findAll()).hasSize(1)
        assertThat(result1.modelAnswer).isEqualTo("첫 번째 모범답변")

        // Given - 두 번째 답변 생성
        val testAnswer2 = InterviewAnswer(
            questionId = testQuestion.id,
            userId = 1L,
            answerText = "두 번째 답변입니다.",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        interviewAnswerRepository.save(testAnswer2)

        // 두 번째 호출: AI 파싱 실패
        whenever(aiClient.requestFeedback(any(), any())).thenReturn("{invalid}")
        whenever(responseParser.parseOpenAiResponse(any(), any()))
            .thenThrow(AiResponseParseException("파싱 실패", "{invalid}"))

        // When
        aiFeedbackService.generateFeedback(testAnswer2, testQuestion)

        // Then - 두 번째 호출 후 2개 저장됨 (첫 번째는 유지, 두 번째는 더미)
        val allFeedbacks = aiFeedbackRepository.findAll()
        assertThat(allFeedbacks).hasSize(2)

        val firstFeedback = allFeedbacks.find { it.interviewAnswerId == testAnswer.id }
        val secondFeedback = allFeedbacks.find { it.interviewAnswerId == testAnswer2.id }

        // 첫 번째는 실제 AI 피드백으로 유지됨
        assertThat(firstFeedback).isNotNull
        assertThat(firstFeedback!!.modelAnswer).isEqualTo("첫 번째 모범답변")

        // 두 번째는 더미 피드백
        assertThat(secondFeedback).isNotNull
        assertThat(secondFeedback!!.modelName).isEqualTo("dummy-model-v1")
    }

    // ========================================
    // Critical Issue #2: RateLimitService Race Condition 테스트
    // ========================================

    @Test
    fun `Critical Fix #2 - 동시 요청 시 Rate Limit이 정확히 적용되어야 함`() {
        // Given
        val ip = "192.168.1.100"
        val threadCount = 50
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)

        val executor = Executors.newFixedThreadPool(threadCount)

        // When - 50개 스레드에서 동시에 요청
        repeat(threadCount) {
            executor.submit {
                try {
                    rateLimitService.checkAndRecordRequest(ip)
                    successCount.incrementAndGet()
                } catch (e: RateLimitExceededException) {
                    failureCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        // 모든 스레드 완료 대기 (최대 10초)
        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        // Then
        // 1. 최대 33개까지만 성공해야 함 (MAX_REQUESTS_PER_HOUR)
        assertThat(successCount.get()).isLessThanOrEqualTo(33)
        assertThat(successCount.get()).isGreaterThan(0) // 최소 1개는 성공

        // 2. 성공 + 실패 = 전체 요청 수
        assertThat(successCount.get() + failureCount.get()).isEqualTo(threadCount)

        // 3. 실패한 요청은 최소 17개 (50 - 33)
        assertThat(failureCount.get()).isGreaterThanOrEqualTo(17)

        println("동시성 테스트 결과 - 성공: ${successCount.get()}, 실패: ${failureCount.get()}")
    }

    @Test
    fun `Critical Fix #2 - 질문 생성 동시 요청 시 Rate Limit이 정확히 적용되어야 함`() {
        // Given
        val userId = 999L
        val threadCount = 20
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)

        val executor = Executors.newFixedThreadPool(threadCount)

        // When - 20개 스레드에서 동시에 질문 생성 요청
        repeat(threadCount) {
            executor.submit {
                try {
                    rateLimitService.checkAndRecordQuestionGeneration(userId)
                    successCount.incrementAndGet()
                } catch (e: RateLimitExceededException) {
                    failureCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        // Then
        // 1. 최대 10개까지만 성공해야 함 (MAX_QUESTION_GENERATIONS_PER_DAY)
        assertThat(successCount.get()).isLessThanOrEqualTo(10)
        assertThat(successCount.get()).isGreaterThan(0)

        // 2. 성공 + 실패 = 전체 요청 수
        assertThat(successCount.get() + failureCount.get()).isEqualTo(threadCount)

        // 3. 실패한 요청은 최소 10개 (20 - 10)
        assertThat(failureCount.get()).isGreaterThanOrEqualTo(10)

        println("질문 생성 동시성 테스트 결과 - 성공: ${successCount.get()}, 실패: ${failureCount.get()}")
    }

    @Test
    fun `Critical Fix #2 - 모의 면접 동시 요청 시 Rate Limit이 정확히 적용되어야 함`() {
        // Given
        val userId = 888L
        val threadCount = 15
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)

        val executor = Executors.newFixedThreadPool(threadCount)

        // When - 15개 스레드에서 동시에 모의 면접 요청
        repeat(threadCount) {
            executor.submit {
                try {
                    rateLimitService.checkMockInterviewLimit(userId)
                    successCount.incrementAndGet()
                } catch (e: RateLimitExceededException) {
                    failureCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        // Then
        // 1. 최대 5개까지만 성공해야 함 (MAX_MOCK_INTERVIEWS_PER_DAY)
        assertThat(successCount.get()).isLessThanOrEqualTo(5)
        assertThat(successCount.get()).isGreaterThan(0)

        // 2. 성공 + 실패 = 전체 요청 수
        assertThat(successCount.get() + failureCount.get()).isEqualTo(threadCount)

        // 3. 실패한 요청은 최소 10개 (15 - 5)
        assertThat(failureCount.get()).isGreaterThanOrEqualTo(10)

        println("모의 면접 동시성 테스트 결과 - 성공: ${successCount.get()}, 실패: ${failureCount.get()}")
    }
}
