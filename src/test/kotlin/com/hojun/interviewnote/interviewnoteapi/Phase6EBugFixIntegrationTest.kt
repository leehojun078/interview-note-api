package com.hojun.interviewnote.interviewnoteapi

import com.hojun.interviewnote.interviewnoteapi.domain.CareerLevel
import com.hojun.interviewnote.interviewnoteapi.domain.GeneratedQuestion
import com.hojun.interviewnote.interviewnoteapi.domain.JobField
import com.hojun.interviewnote.interviewnoteapi.domain.JobPosting
import com.hojun.interviewnote.interviewnoteapi.domain.User
import com.hojun.interviewnote.interviewnoteapi.domain.UserRole
import com.hojun.interviewnote.interviewnoteapi.exception.JobPostingNotFoundException
import com.hojun.interviewnote.interviewnoteapi.repository.GeneratedQuestionRepository
import com.hojun.interviewnote.interviewnoteapi.repository.JobPostingRepository
import com.hojun.interviewnote.interviewnoteapi.repository.UserRepository
import com.hojun.interviewnote.interviewnoteapi.service.JobPostingService
import com.hojun.interviewnote.interviewnoteapi.service.cache.QuestionCache
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.*

/**
 * Phase 6E 버그 수정 통합 테스트
 *
 * 버그 시나리오:
 * 1. 사용자 A가 URL X로 공고 등록 → JobPosting(id=1, userId=A) 생성
 * 2. 사용자 B가 동일한 URL X로 공고 등록 시도
 * 3. 캐시 히트로 JobPosting(id=1, userId=A)를 재사용 (버그!)
 * 4. 사용자 B가 /job-postings/1/questions 접근 시 권한 오류 발생
 *
 * 수정 내용 (방안 1):
 * - JobPostingCache → QuestionCache로 교체
 * - 동일 URL이라도 각 사용자는 자신만의 JobPosting 생성
 * - 질문만 캐싱하여 재사용 (AI 비용 절감)
 *
 * 테스트 범위:
 * 1. QuestionCache 기능 검증
 * 2. 소유권 검증 (각 사용자는 자신의 공고에만 접근)
 * 3. 질문 복사 로직 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase6EBugFixIntegrationTest {

    @Autowired
    private lateinit var jobPostingService: JobPostingService

    @Autowired
    private lateinit var jobPostingRepository: JobPostingRepository

    @Autowired
    private lateinit var generatedQuestionRepository: GeneratedQuestionRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var questionCache: QuestionCache

    private lateinit var userA: User
    private lateinit var userB: User

    @BeforeEach
    fun setUp() {
        // 테스트 사용자 생성
        userA = userRepository.save(
            User(
                email = "tester@example.com",
                passwordHash = passwordEncoder.encode("password123"),
                name = "테스터 A",
                role = UserRole.USER,
                jobField = JobField.IT,
                careerLevel = CareerLevel.JUNIOR
            )
        )

        userB = userRepository.save(
            User(
                email = "ex@naver.com",
                passwordHash = passwordEncoder.encode("password123"),
                name = "테스터 B",
                role = UserRole.USER,
                jobField = JobField.IT,
                careerLevel = CareerLevel.JUNIOR
            )
        )
    }

    // ========================================
    // Helper Methods
    // ========================================

    private fun createTestJobPosting(
        userId: Long,
        url: String,
        jobField: JobField = JobField.IT
    ): JobPosting {
        val jobPosting = JobPosting(
            userId = userId,
            originalUrl = url,
            companyName = "테스트 회사",
            jobTitle = "${jobField.displayName} 개발자",
            jobDescription = "테스트 직무 설명",
            selectedJobField = jobField,
            requiredSkills = """["Kotlin", "Spring Boot"]""",
            preferredSkills = """["Docker"]"""
        )
        return jobPostingRepository.save(jobPosting)
    }

    private fun createTestQuestions(jobPostingId: Long): List<GeneratedQuestion> {
        val questions = (1..10).map { index ->
            GeneratedQuestion(
                jobPostingId = jobPostingId,
                content = "테스트 질문 $index",
                category = "기술역량",
                difficulty = when (index % 3) {
                    0 -> "EASY"
                    1 -> "MEDIUM"
                    else -> "HARD"
                },
                aiReasoning = "테스트 근거 $index",
                orderIndex = index
            )
        }
        return questions.map { generatedQuestionRepository.save(it) }
    }

    // ========================================
    // 테스트 1: 버그 재현 - 소유권 검증
    // ========================================

    @Test
    fun `버그 재현 - 다른 사용자의 공고에 접근 시 예외 발생`() {
        // Given
        val testUrl = "https://www.wanted.co.kr/wd/281357"
        val postingA = createTestJobPosting(userA.id, testUrl)
        createTestQuestions(postingA.id)

        // When & Then
        // 사용자 B가 사용자 A의 공고에 접근 시 예외 발생
        assertFailsWith<JobPostingNotFoundException> {
            jobPostingService.getJobPostingWithQuestions(postingA.id, userB.id)
        }
    }

    @Test
    fun `각 사용자는 자신의 공고에만 접근 가능`() {
        // Given
        val urlA = "https://www.wanted.co.kr/wd/aaa"
        val urlB = "https://www.wanted.co.kr/wd/bbb"

        val postingA = createTestJobPosting(userA.id, urlA)
        val postingB = createTestJobPosting(userB.id, urlB)

        createTestQuestions(postingA.id)
        createTestQuestions(postingB.id)

        // When & Then
        // 1. 사용자 A는 자신의 공고에 접근 가능
        assertDoesNotThrow {
            val viewModel = jobPostingService.getJobPostingWithQuestions(postingA.id, userA.id)
            assertEquals(userA.id, viewModel.userId)
        }

        // 2. 사용자 B는 자신의 공고에 접근 가능
        assertDoesNotThrow {
            val viewModel = jobPostingService.getJobPostingWithQuestions(postingB.id, userB.id)
            assertEquals(userB.id, viewModel.userId)
        }

        // 3. 크로스 접근 불가
        assertFailsWith<JobPostingNotFoundException> {
            jobPostingService.getJobPostingWithQuestions(postingA.id, userB.id)
        }
        assertFailsWith<JobPostingNotFoundException> {
            jobPostingService.getJobPostingWithQuestions(postingB.id, userA.id)
        }
    }

    // ========================================
    // 테스트 2: QuestionCache 검증
    // ========================================

    @Test
    fun `QuestionCache 캐시 히트 - 동일 URL의 질문 조회`() {
        // Given
        val testUrl = "https://www.wanted.co.kr/wd/cached123"
        val posting = createTestJobPosting(userA.id, testUrl)
        val questions = createTestQuestions(posting.id)

        // When
        val cachedQuestions = questionCache.findCachedQuestions(testUrl)

        // Then
        assertTrue(cachedQuestions.isNotEmpty(), "캐시된 질문이 있어야 함")
        assertEquals(10, cachedQuestions.size, "10개 질문이 캐시되어야 함")
        assertEquals(questions[0].content, cachedQuestions[0].content, "첫 질문 내용이 동일해야 함")
    }

    @Test
    fun `QuestionCache 캐시 미스 - 존재하지 않는 URL`() {
        // Given
        val uncachedUrl = "https://www.wanted.co.kr/wd/uncached999"

        // When
        val cachedQuestions = questionCache.findCachedQuestions(uncachedUrl)

        // Then
        assertTrue(cachedQuestions.isEmpty(), "캐시되지 않은 URL은 빈 리스트 반환")
        assertFalse(questionCache.hasCachedQuestions(uncachedUrl), "캐시 존재 여부 false")
    }

    @Test
    fun `QuestionCache hasCachedQuestions 메서드 검증`() {
        // Given
        val cachedUrl = "https://www.wanted.co.kr/wd/cached456"
        val uncachedUrl = "https://www.wanted.co.kr/wd/uncached789"

        val posting = createTestJobPosting(userA.id, cachedUrl)
        createTestQuestions(posting.id)

        // When & Then
        assertTrue(questionCache.hasCachedQuestions(cachedUrl), "캐시 존재")
        assertFalse(questionCache.hasCachedQuestions(uncachedUrl), "캐시 미존재")
    }

    // ========================================
    // 테스트 3: 다중 사용자 시나리오
    // ========================================

    @Test
    fun `동일 URL의 공고를 여러 사용자가 등록 시 각자 독립 공고 가져야 함`() {
        // Given
        val testUrl = "https://www.wanted.co.kr/wd/multi123"

        // 사용자 A가 먼저 공고 등록
        val postingA = createTestJobPosting(userA.id, testUrl)
        createTestQuestions(postingA.id)

        // When
        // 사용자 B가 동일 URL로 공고 등록 (캐시 시뮬레이션)
        // 실제로는 createJobPosting을 호출하지만, 여기서는 Repository 직접 사용
        val postingB = createTestJobPosting(userB.id, testUrl)

        // 캐시된 질문 복사
        val cachedQuestions = questionCache.findCachedQuestions(testUrl)
        val copiedQuestions = cachedQuestions.map { cached ->
            GeneratedQuestion(
                jobPostingId = postingB.id,
                content = cached.content,
                category = cached.category,
                difficulty = cached.difficulty,
                aiReasoning = cached.aiReasoning,
                orderIndex = cached.orderIndex
            )
        }
        copiedQuestions.forEach { generatedQuestionRepository.save(it) }

        // Then
        // 1. 두 공고는 서로 다른 ID
        assertNotEquals(postingA.id, postingB.id, "공고 ID가 달라야 함")

        // 2. 각 공고의 소유자가 올바름
        assertEquals(userA.id, postingA.userId, "공고 A는 사용자 A 소유")
        assertEquals(userB.id, postingB.userId, "공고 B는 사용자 B 소유")

        // 3. 각 사용자는 자신의 공고에만 접근 가능
        assertDoesNotThrow {
            jobPostingService.getJobPostingWithQuestions(postingA.id, userA.id)
        }
        assertDoesNotThrow {
            jobPostingService.getJobPostingWithQuestions(postingB.id, userB.id)
        }

        // 4. 크로스 접근 불가
        assertFailsWith<JobPostingNotFoundException> {
            jobPostingService.getJobPostingWithQuestions(postingA.id, userB.id)
        }
    }

    @Test
    fun `세 명 이상의 사용자가 동일 URL 등록 시 각자 독립 공고`() {
        // Given
        val testUrl = "https://www.wanted.co.kr/wd/three123"
        val userC = userRepository.save(
            User(
                email = "userc@example.com",
                passwordHash = passwordEncoder.encode("password123"),
                name = "테스터 C",
                role = UserRole.USER,
                jobField = JobField.IT,
                careerLevel = CareerLevel.SENIOR
            )
        )

        // When
        val postingA = createTestJobPosting(userA.id, testUrl)
        val postingB = createTestJobPosting(userB.id, testUrl)
        val postingC = createTestJobPosting(userC.id, testUrl)

        createTestQuestions(postingA.id)
        createTestQuestions(postingB.id)
        createTestQuestions(postingC.id)

        // Then
        val ids = setOf(postingA.id, postingB.id, postingC.id)
        assertEquals(3, ids.size, "세 공고는 서로 다른 ID")

        assertEquals(userA.id, postingA.userId)
        assertEquals(userB.id, postingB.userId)
        assertEquals(userC.id, postingC.userId)

        // 각 사용자는 자신의 공고에만 접근 가능
        assertDoesNotThrow { jobPostingService.getJobPostingWithQuestions(postingA.id, userA.id) }
        assertDoesNotThrow { jobPostingService.getJobPostingWithQuestions(postingB.id, userB.id) }
        assertDoesNotThrow { jobPostingService.getJobPostingWithQuestions(postingC.id, userC.id) }

        // 크로스 접근 불가
        assertFailsWith<JobPostingNotFoundException> {
            jobPostingService.getJobPostingWithQuestions(postingA.id, userB.id)
        }
        assertFailsWith<JobPostingNotFoundException> {
            jobPostingService.getJobPostingWithQuestions(postingB.id, userC.id)
        }
    }

    // ========================================
    // 테스트 4: 질문 복사 로직 검증
    // ========================================

    @Test
    fun `질문 복사 시 내용은 동일하지만 엔티티는 별개`() {
        // Given
        val testUrl = "https://www.wanted.co.kr/wd/copy123"
        val postingA = createTestJobPosting(userA.id, testUrl)
        val questionsA = createTestQuestions(postingA.id)

        // When
        // 캐시 조회 (postingB 생성 전에 조회해야 postingA의 질문을 가져올 수 있음)
        val cachedQuestions = questionCache.findCachedQuestions(testUrl)

        // 캐시가 제대로 동작하는지 확인
        assertTrue(cachedQuestions.isNotEmpty(), "캐시된 질문이 있어야 함")
        assertEquals(10, cachedQuestions.size, "캐시된 질문이 10개여야 함")

        // 사용자 B가 동일 URL로 등록 (질문 복사)
        val postingB = createTestJobPosting(userB.id, testUrl)

        val questionsB = cachedQuestions.map { cached ->
            GeneratedQuestion(
                jobPostingId = postingB.id,
                content = cached.content,
                category = cached.category,
                difficulty = cached.difficulty,
                aiReasoning = cached.aiReasoning,
                orderIndex = cached.orderIndex
            )
        }
        questionsB.forEach { generatedQuestionRepository.save(it) }

        // Then
        // 1. 질문 개수 동일
        assertEquals(questionsA.size, questionsB.size, "질문 개수 동일")

        // 2. 질문 내용 동일
        questionsA.forEachIndexed { index, qA ->
            val qB = questionsB[index]
            assertEquals(qA.content, qB.content, "질문 내용 동일 (index=$index)")
            assertEquals(qA.category, qB.category, "카테고리 동일 (index=$index)")
            assertEquals(qA.difficulty, qB.difficulty, "난이도 동일 (index=$index)")
            assertEquals(qA.orderIndex, qB.orderIndex, "순서 동일 (index=$index)")
        }

        // 3. 질문 엔티티는 별개 (ID 다름, jobPostingId 다름)
        val savedQuestionsB = generatedQuestionRepository.findByJobPostingIdOrderByOrderIndexAsc(postingB.id)
        questionsA.forEachIndexed { index, qA ->
            val qB = savedQuestionsB[index]
            assertNotEquals(qA.id, qB.id, "질문 ID는 달라야 함 (index=$index)")
            assertEquals(postingA.id, qA.jobPostingId, "질문 A는 공고 A에 속함")
            assertEquals(postingB.id, qB.jobPostingId, "질문 B는 공고 B에 속함")
        }
    }
}
