package com.hojun.interviewnote.interviewnoteapi

import com.hojun.interviewnote.interviewnoteapi.domain.*
import com.hojun.interviewnote.interviewnoteapi.repository.*
import com.hojun.interviewnote.interviewnoteapi.service.MockInterviewService
import com.hojun.interviewnote.interviewnoteapi.service.ReviewService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Phase 8C: 리뷰 통합 및 재개 기능 통합 테스트
 *
 * 테스트 목록:
 * 1. 리뷰 이력 2개 탭 조회
 * 2. 면접 재개 기능 (COMPLETED → IN_PROGRESS)
 * 3. AI 면접 리뷰 DTO 생성 (채용 공고 정보 포함)
 * 4. AI 면접 리뷰 DTO 생성 (경력 수준 포함)
 * 5. weightedAverageScore 우선 표시
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase8CIntegrationTest {

    @Autowired
    private lateinit var reviewService: ReviewService

    @Autowired
    private lateinit var mockInterviewService: MockInterviewService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var mockInterviewRepository: MockInterviewRepository

    @Autowired
    private lateinit var interviewMessageRepository: InterviewMessageRepository

    @Autowired
    private lateinit var jobPostingRepository: JobPostingRepository

    @Autowired
    private lateinit var questionRepository: QuestionRepository

    @Autowired
    private lateinit var interviewAnswerRepository: InterviewAnswerRepository

    @Autowired
    private lateinit var aiFeedbackRepository: AiFeedbackRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        // 테스트 사용자 생성
        testUser = User(
            email = "phase8c@test.com",
            passwordHash = passwordEncoder.encode("password"),
            name = "Phase 8C Tester",
            role = UserRole.USER,
            jobField = JobField.IT,
            careerLevel = CareerLevel.ENTRY
        )
        userRepository.save(testUser)
    }

    /**
     * 테스트 1: 리뷰 이력 2개 탭 조회
     * - 질문 연습 리뷰 조회
     * - AI 면접 리뷰 조회
     */
    @Test
    fun `리뷰 이력 2개 탭 조회 - 질문 연습과 AI 면접 분리`() {
        // Given: 질문 연습 답변 1개
        val question = Question(
            jobField = "IT",
            targetJob = "백엔드 개발자",
            category = "기술역량",
            content = "Spring Boot의 DI에 대해 설명하세요",
            difficulty = "MEDIUM"
        )
        questionRepository.save(question)

        val answer = InterviewAnswer(
            questionId = question.id,
            userId = testUser.id,
            answerText = "Spring Boot의 DI는 의존성 주입을 의미합니다..."
        )
        interviewAnswerRepository.save(answer)

        val feedback = AiFeedback(
            interviewAnswerId = answer.id,
            logicScore = 4,
            specificityScore = 3,
            jobFitScore = 4,
            deliveryScore = 3,
            strengths = "[\"STAR 기법 활용\"]",
            improvements = "[\"구체적인 예시 필요\"]",
            modelAnswer = "모범답변",
            overallComment = "좋습니다",
            jobField = "IT",
            modelName = "gpt-4o-mini",
            promptVersion = "v1.0",
            tokenUsageInput = 100,
            tokenUsageOutput = 200,
            rawResponse = "{}"
        )
        aiFeedbackRepository.save(feedback)

        // Given: 완료된 AI 면접 1개
        val interview = MockInterview(
            userId = testUser.id,
            selectedJobField = JobField.IT,
            careerLevel = CareerLevel.ENTRY,
            status = MockInterviewStatus.COMPLETED,
            overallFeedback = "전반적으로 우수한 답변입니다.",
            keyStrengths = "[\"기술 역량 우수\", \"명확한 커뮤니케이션\"]",
            keyImprovements = "[\"실무 경험 필요\"]",
            averageScore = 3.8,
            weightedAverageScore = 3.5,
            recommendation = "합격 추천"
        )
        interview.status = MockInterviewStatus.COMPLETED
        interview.endedAt = LocalDateTime.now()
        mockInterviewRepository.save(interview)

        // AI 질문 2개 추가 (messageCount 계산용)
        val aiMessage1 = InterviewMessage(
            mockInterviewId = interview.id,
            sender = MessageSender.AI,
            content = "자기소개를 부탁드립니다.",
            messageIndex = 0
        )
        val aiMessage2 = InterviewMessage(
            mockInterviewId = interview.id,
            sender = MessageSender.AI,
            content = "Spring Boot 경험을 말씀해주세요.",
            messageIndex = 2
        )
        interviewMessageRepository.saveAll(listOf(aiMessage1, aiMessage2))

        // When: 질문 연습 리뷰 조회
        val questionReviews = reviewService.getUserReviews(testUser.id)

        // When: AI 면접 리뷰 조회
        val mockInterviewReviews = reviewService.getUserMockInterviewReviews(testUser.id)

        // Then: 질문 연습 리뷰 1개
        assertThat(questionReviews).hasSize(1)
        assertThat(questionReviews[0].answerId).isEqualTo(answer.id)
        assertThat(questionReviews[0].averageScore).isEqualTo(3.5)

        // Then: AI 면접 리뷰 1개
        assertThat(mockInterviewReviews).hasSize(1)
        assertThat(mockInterviewReviews[0].interviewId).isEqualTo(interview.id)
        assertThat(mockInterviewReviews[0].jobField).isEqualTo("IT개발")
        assertThat(mockInterviewReviews[0].careerLevel).isEqualTo("신입")
        assertThat(mockInterviewReviews[0].messageCount).isEqualTo(2) // AI 메시지 2개
        assertThat(mockInterviewReviews[0].averageScore).isEqualTo(3.5) // weightedAverageScore 우선
    }

    /**
     * 테스트 2: 면접 재개 기능
     * - COMPLETED → IN_PROGRESS 전환
     * - endedAt null로 초기화
     */
    @Test
    fun `면접 재개 - COMPLETED 상태를 IN_PROGRESS로 전환`() {
        // Given: 완료된 면접
        val interview = MockInterview(
            userId = testUser.id,
            selectedJobField = JobField.IT,
            status = MockInterviewStatus.COMPLETED,
            overallFeedback = "좋습니다",
            keyStrengths = "[\"강점1\"]",
            keyImprovements = "[\"개선1\"]",
            averageScore = 3.5,
            weightedAverageScore = 3.3,
            recommendation = "합격"
        )
        interview.status = MockInterviewStatus.COMPLETED
        interview.endedAt = LocalDateTime.now()
        mockInterviewRepository.save(interview)

        // When: 면접 재개
        val resumed = mockInterviewService.resumeInterview(interview.id, testUser.id)

        // Then: IN_PROGRESS 상태로 전환
        assertThat(resumed.status).isEqualTo(MockInterviewStatus.IN_PROGRESS)
        assertThat(resumed.endedAt).isNull()
        assertThat(resumed.overallFeedback).isEqualTo("좋습니다") // 평가 내용은 유지
    }

    /**
     * 테스트 3: AI 면접 리뷰 - 채용 공고 정보 포함
     */
    @Test
    fun `AI 면접 리뷰 - 채용 공고 정보 포함`() {
        // Given: 채용 공고
        val jobPosting = JobPosting(
            userId = testUser.id,
            originalUrl = "https://example.com/job/123",
            companyName = "카카오",
            jobTitle = "백엔드 개발자",
            jobDescription = "Kotlin, Spring Boot를 활용한 백엔드 개발자를 모집합니다.",
            selectedJobField = JobField.IT,
            inferredJobField = JobField.IT
        )
        jobPostingRepository.save(jobPosting)

        // Given: 공고 기반 완료된 면접
        val interview = MockInterview(
            userId = testUser.id,
            jobPostingId = jobPosting.id,
            selectedJobField = JobField.IT,
            status = MockInterviewStatus.COMPLETED,
            overallFeedback = "좋습니다",
            keyStrengths = "[\"강점1\"]",
            keyImprovements = "[\"개선1\"]",
            averageScore = 4.0,
            weightedAverageScore = 3.8,
            recommendation = "합격"
        )
        interview.status = MockInterviewStatus.COMPLETED
        interview.endedAt = LocalDateTime.now()
        mockInterviewRepository.save(interview)

        // When: AI 면접 리뷰 조회
        val reviews = reviewService.getUserMockInterviewReviews(testUser.id)

        // Then: 채용 공고 정보 포함
        assertThat(reviews).hasSize(1)
        assertThat(reviews[0].jobPostingInfo).isNotNull
        assertThat(reviews[0].jobPostingInfo?.companyName).isEqualTo("카카오")
        assertThat(reviews[0].jobPostingInfo?.jobTitle).isEqualTo("백엔드 개발자")
    }

    /**
     * 테스트 4: AI 면접 리뷰 - 경력 수준 표시
     */
    @Test
    fun `AI 면접 리뷰 - 경력 수준 표시`() {
        // Given: 시니어 경력 수준으로 완료된 면접
        val interview = MockInterview(
            userId = testUser.id,
            selectedJobField = JobField.IT,
            careerLevel = CareerLevel.SENIOR,
            status = MockInterviewStatus.COMPLETED,
            overallFeedback = "좋습니다",
            keyStrengths = "[\"강점1\"]",
            keyImprovements = "[\"개선1\"]",
            averageScore = 4.2,
            weightedAverageScore = 4.0,
            recommendation = "합격"
        )
        interview.status = MockInterviewStatus.COMPLETED
        interview.endedAt = LocalDateTime.now()
        mockInterviewRepository.save(interview)

        // When: AI 면접 리뷰 조회
        val reviews = reviewService.getUserMockInterviewReviews(testUser.id)

        // Then: 경력 수준 표시
        assertThat(reviews).hasSize(1)
        assertThat(reviews[0].careerLevel).isEqualTo("시니어(3-7년)")
    }

    /**
     * 테스트 5: weightedAverageScore 우선 표시
     */
    @Test
    fun `weightedAverageScore 우선 표시 - 없으면 averageScore 사용`() {
        // Given: weightedAverageScore가 있는 면접
        val interview1 = MockInterview(
            userId = testUser.id,
            selectedJobField = JobField.IT,
            status = MockInterviewStatus.COMPLETED,
            overallFeedback = "좋습니다",
            keyStrengths = "[\"강점1\"]",
            keyImprovements = "[\"개선1\"]",
            averageScore = 4.0,
            weightedAverageScore = 3.5, // 가중 평균 더 낮음
            recommendation = "합격"
        )
        interview1.status = MockInterviewStatus.COMPLETED
        interview1.endedAt = LocalDateTime.now()
        mockInterviewRepository.save(interview1)

        // Given: weightedAverageScore가 없는 면접
        val interview2 = MockInterview(
            userId = testUser.id,
            selectedJobField = JobField.IT,
            status = MockInterviewStatus.COMPLETED,
            overallFeedback = "좋습니다",
            keyStrengths = "[\"강점1\"]",
            keyImprovements = "[\"개선1\"]",
            averageScore = 3.8,
            weightedAverageScore = null, // 없음
            recommendation = "합격"
        )
        interview2.status = MockInterviewStatus.COMPLETED
        interview2.endedAt = LocalDateTime.now()
        mockInterviewRepository.save(interview2)

        // When: AI 면접 리뷰 조회
        val reviews = reviewService.getUserMockInterviewReviews(testUser.id)

        // Then: weightedAverageScore 우선, 없으면 averageScore
        assertThat(reviews).hasSize(2)
        val review1 = reviews.find { it.interviewId == interview1.id }
        val review2 = reviews.find { it.interviewId == interview2.id }

        assertThat(review1?.averageScore).isEqualTo(3.5) // weightedAverageScore
        assertThat(review2?.averageScore).isEqualTo(3.8) // averageScore
    }

    /**
     * 테스트 6: 면접 재개 - 완료되지 않은 면접은 재개 불가
     */
    @Test
    fun `면접 재개 실패 - IN_PROGRESS 상태는 재개 불가`() {
        // Given: 진행 중인 면접
        val interview = MockInterview(
            userId = testUser.id,
            selectedJobField = JobField.IT,
            status = MockInterviewStatus.IN_PROGRESS
        )
        mockInterviewRepository.save(interview)

        // When & Then: 예외 발생
        try {
            mockInterviewService.resumeInterview(interview.id, testUser.id)
            assert(false) { "예외가 발생해야 합니다" }
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("완료된 면접만 재개할 수 있습니다")
        }
    }

    /**
     * 테스트 7: 빈 리뷰 목록 조회
     */
    @Test
    fun `빈 리뷰 목록 조회 - 빈 리스트 반환`() {
        // When: 아무 면접도 없는 상태에서 조회
        val reviews = reviewService.getUserMockInterviewReviews(testUser.id)

        // Then: 빈 리스트 반환
        assertThat(reviews).isEmpty()
    }

    /**
     * 테스트 8: 여러 면접 정렬 순서 (최신순)
     */
    @Test
    fun `여러 AI 면접 정렬 - 최신순 정렬`() {
        // Given: 3개의 완료된 면접 (시간차)
        val interview1 = MockInterview(
            userId = testUser.id,
            selectedJobField = JobField.IT,
            status = MockInterviewStatus.COMPLETED,
            overallFeedback = "첫번째",
            keyStrengths = "[]",
            keyImprovements = "[]",
            averageScore = 3.0,
            recommendation = "합격"
        )
        interview1.status = MockInterviewStatus.COMPLETED
        interview1.endedAt = LocalDateTime.now().minusDays(3)
        mockInterviewRepository.save(interview1)

        Thread.sleep(10)

        val interview2 = MockInterview(
            userId = testUser.id,
            selectedJobField = JobField.IT,
            status = MockInterviewStatus.COMPLETED,
            overallFeedback = "두번째",
            keyStrengths = "[]",
            keyImprovements = "[]",
            averageScore = 3.5,
            recommendation = "합격"
        )
        interview2.status = MockInterviewStatus.COMPLETED
        interview2.endedAt = LocalDateTime.now().minusDays(1)
        mockInterviewRepository.save(interview2)

        Thread.sleep(10)

        val interview3 = MockInterview(
            userId = testUser.id,
            selectedJobField = JobField.IT,
            status = MockInterviewStatus.COMPLETED,
            overallFeedback = "세번째",
            keyStrengths = "[]",
            keyImprovements = "[]",
            averageScore = 4.0,
            recommendation = "합격"
        )
        interview3.status = MockInterviewStatus.COMPLETED
        interview3.endedAt = LocalDateTime.now()
        mockInterviewRepository.save(interview3)

        // When: AI 면접 리뷰 조회
        val reviews = reviewService.getUserMockInterviewReviews(testUser.id)

        // Then: 최신순 정렬 (interview3 → interview2 → interview1)
        assertThat(reviews).hasSize(3)
        assertThat(reviews[0].interviewId).isEqualTo(interview3.id)
        assertThat(reviews[1].interviewId).isEqualTo(interview2.id)
        assertThat(reviews[2].interviewId).isEqualTo(interview1.id)
    }
}
