package com.hojun.interviewnote.interviewnoteapi

import com.hojun.interviewnote.interviewnoteapi.domain.GeneratedQuestion
import com.hojun.interviewnote.interviewnoteapi.domain.JobField
import com.hojun.interviewnote.interviewnoteapi.domain.JobPosting
import com.hojun.interviewnote.interviewnoteapi.domain.User
import com.hojun.interviewnote.interviewnoteapi.domain.UserRole
import com.hojun.interviewnote.interviewnoteapi.repository.GeneratedQuestionRepository
import com.hojun.interviewnote.interviewnoteapi.repository.JobPostingRepository
import com.hojun.interviewnote.interviewnoteapi.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.test.*

/**
 * Phase 6A 통합 테스트
 *
 * 테스트 범위:
 * 1. JobPosting 엔티티 생성 및 조회
 * 2. GeneratedQuestion 엔티티 생성 및 조회
 * 3. effectiveJobField 프로퍼티 검증
 * 4. Repository 커스텀 쿼리 검증
 * 5. 7일 캐싱 로직 검증
 * 6. CHECK 제약 조건 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase6AIntegrationTest {

    @Autowired
    private lateinit var jobPostingRepository: JobPostingRepository

    @Autowired
    private lateinit var generatedQuestionRepository: GeneratedQuestionRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        // 테스트 사용자 생성
        testUser = User(
            email = "phase6atest@example.com",
            passwordHash = passwordEncoder.encode("password123"),
            name = "Phase6A Test User",
            role = UserRole.USER
        )
        userRepository.save(testUser)
    }

    // ========================================
    // 1. JobPosting 엔티티 검증
    // ========================================

    @Test
    fun `JobPosting 엔티티 생성 및 저장 검증`() {
        // Given
        val jobPosting = JobPosting(
            userId = testUser.id,
            originalUrl = "https://www.wanted.co.kr/wd/123456",
            companyName = "카카오",
            jobTitle = "백엔드 개발자",
            jobDescription = "Kotlin, Spring Boot 기반 백엔드 개발",
            selectedJobField = JobField.IT,
            inferredJobField = null,
            requiredSkills = """["Kotlin", "Spring Boot", "MySQL"]""",
            preferredSkills = """["Kubernetes", "Redis"]"""
        )

        // When
        val saved = jobPostingRepository.save(jobPosting)

        // Then
        assertNotNull(saved.id)
        assertEquals("카카오", saved.companyName)
        assertEquals(JobField.IT, saved.effectiveJobField)
        assertTrue(saved.isActive)
    }

    @Test
    fun `effectiveJobField는 selectedJobField 우선, 없으면 inferredJobField 반환`() {
        // Case 1: selectedJobField가 있는 경우
        val posting1 = JobPosting(
            userId = testUser.id,
            originalUrl = "https://example.com/1",
            companyName = "Company1",
            jobTitle = "Job1",
            jobDescription = "Description",
            selectedJobField = JobField.IT,
            inferredJobField = JobField.SALES
        )
        assertEquals(JobField.IT, posting1.effectiveJobField)

        // Case 2: selectedJobField가 없고 inferredJobField만 있는 경우
        val posting2 = JobPosting(
            userId = testUser.id,
            originalUrl = "https://example.com/2",
            companyName = "Company2",
            jobTitle = "Job2",
            jobDescription = "Description",
            selectedJobField = null,
            inferredJobField = JobField.MARKETING
        )
        assertEquals(JobField.MARKETING, posting2.effectiveJobField)

        // Case 3: 둘 다 없는 경우 (에러 상황)
        val posting3 = JobPosting(
            userId = testUser.id,
            originalUrl = "https://example.com/3",
            companyName = "Company3",
            jobTitle = "Job3",
            jobDescription = "Description",
            selectedJobField = null,
            inferredJobField = null
        )
        assertNull(posting3.effectiveJobField)
    }

    @Test
    fun `deactivate 메서드로 공고 비활성화 검증`() {
        // Given
        val jobPosting = JobPosting(
            userId = testUser.id,
            originalUrl = "https://example.com/deactivate",
            companyName = "TestCompany",
            jobTitle = "TestJob",
            jobDescription = "Description"
        )
        val saved = jobPostingRepository.save(jobPosting)

        // When
        saved.deactivate()
        jobPostingRepository.save(saved)

        // Then
        val found = jobPostingRepository.findById(saved.id).get()
        assertFalse(found.isActive)
    }

    @Test
    fun `updateInferredJobField 메서드로 AI 추론 직무 업데이트 검증`() {
        // Given
        val jobPosting = JobPosting(
            userId = testUser.id,
            originalUrl = "https://example.com/update",
            companyName = "TestCompany",
            jobTitle = "TestJob",
            jobDescription = "Description",
            inferredJobField = null
        )
        val saved = jobPostingRepository.save(jobPosting)

        // When
        saved.updateInferredJobField(JobField.ACCOUNTING)
        jobPostingRepository.save(saved)

        // Then
        val found = jobPostingRepository.findById(saved.id).get()
        assertEquals(JobField.ACCOUNTING, found.inferredJobField)
    }

    // ========================================
    // 2. GeneratedQuestion 엔티티 검증
    // ========================================

    @Test
    fun `GeneratedQuestion 엔티티 생성 및 저장 검증`() {
        // Given
        val jobPosting = jobPostingRepository.save(
            JobPosting(
                userId = testUser.id,
                originalUrl = "https://example.com/question-test",
                companyName = "TestCompany",
                jobTitle = "TestJob",
                jobDescription = "Description"
            )
        )

        val question = GeneratedQuestion(
            jobPostingId = jobPosting.id,
            content = "Kotlin의 코루틴을 실무에서 어떻게 활용했나요?",
            category = "기술역량",
            difficulty = "MEDIUM",
            aiReasoning = "필수 기술인 Kotlin의 핵심 기능 이해도를 확인하기 위한 질문",
            orderIndex = 1
        )

        // When
        val saved = generatedQuestionRepository.save(question)

        // Then
        assertNotNull(saved.id)
        assertEquals("기술역량", saved.category)
        assertEquals("MEDIUM", saved.difficulty)
        assertEquals(1, saved.orderIndex)
    }

    @Test
    fun `GeneratedQuestion orderIndex는 1-10 범위만 허용`() {
        // Given
        val jobPosting = jobPostingRepository.save(
            JobPosting(
                userId = testUser.id,
                originalUrl = "https://example.com/order-test",
                companyName = "TestCompany",
                jobTitle = "TestJob",
                jobDescription = "Description"
            )
        )

        // When & Then: orderIndex = 0 (범위 밖)
        assertFailsWith<IllegalArgumentException> {
            GeneratedQuestion(
                jobPostingId = jobPosting.id,
                content = "Test Question",
                category = "Test",
                difficulty = "EASY",
                aiReasoning = "Test",
                orderIndex = 0
            )
        }

        // When & Then: orderIndex = 11 (범위 밖)
        assertFailsWith<IllegalArgumentException> {
            GeneratedQuestion(
                jobPostingId = jobPosting.id,
                content = "Test Question",
                category = "Test",
                difficulty = "EASY",
                aiReasoning = "Test",
                orderIndex = 11
            )
        }
    }

    @Test
    fun `GeneratedQuestion difficulty는 EASY, MEDIUM, HARD만 허용`() {
        // Given
        val jobPosting = jobPostingRepository.save(
            JobPosting(
                userId = testUser.id,
                originalUrl = "https://example.com/difficulty-test",
                companyName = "TestCompany",
                jobTitle = "TestJob",
                jobDescription = "Description"
            )
        )

        // When & Then: 잘못된 difficulty
        assertFailsWith<IllegalArgumentException> {
            GeneratedQuestion(
                jobPostingId = jobPosting.id,
                content = "Test Question",
                category = "Test",
                difficulty = "INVALID",
                aiReasoning = "Test",
                orderIndex = 1
            )
        }
    }

    // ========================================
    // 3. Repository 커스텀 쿼리 검증
    // ========================================

    @Test
    fun `findByUserIdAndIsActiveTrueOrderByCreatedAtDesc 검증`() {
        // Given
        jobPostingRepository.save(
            JobPosting(
                userId = testUser.id,
                originalUrl = "https://example.com/1",
                companyName = "Company1",
                jobTitle = "Job1",
                jobDescription = "Description"
            )
        )
        Thread.sleep(10)  // createdAt 차이 보장
        jobPostingRepository.save(
            JobPosting(
                userId = testUser.id,
                originalUrl = "https://example.com/2",
                companyName = "Company2",
                jobTitle = "Job2",
                jobDescription = "Description"
            )
        )

        // When
        val postings = jobPostingRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(testUser.id)

        // Then
        assertEquals(2, postings.size)
        assertEquals("Company2", postings[0].companyName)  // 최신순
        assertEquals("Company1", postings[1].companyName)
    }

    @Test
    fun `findFirstByOriginalUrlAndCreatedAtAfterOrderByCreatedAtDesc 검증 (7일 캐싱)`() {
        // Given
        val url = "https://example.com/cache-test"
        val sevenDaysAgo = LocalDateTime.now().minusDays(7)
        val fiveDaysAgo = LocalDateTime.now().minusDays(5)

        // 5일 전 공고
        val posting = JobPosting(
            userId = testUser.id,
            originalUrl = url,
            companyName = "CachedCompany",
            jobTitle = "CachedJob",
            jobDescription = "Description"
        )
        jobPostingRepository.save(posting)

        // When: 7일 내 캐시 조회
        val cached = jobPostingRepository.findFirstByOriginalUrlAndCreatedAtAfterOrderByCreatedAtDesc(
            url,
            sevenDaysAgo
        )

        // Then
        assertNotNull(cached)
        assertEquals("CachedCompany", cached.companyName)
    }

    @Test
    fun `findByJobPostingIdOrderByOrderIndexAsc 검증 (질문 순서대로 조회)`() {
        // Given
        val jobPosting = jobPostingRepository.save(
            JobPosting(
                userId = testUser.id,
                originalUrl = "https://example.com/order-test",
                companyName = "TestCompany",
                jobTitle = "TestJob",
                jobDescription = "Description"
            )
        )

        // 순서 뒤섞어서 저장
        generatedQuestionRepository.save(
            GeneratedQuestion(
                jobPostingId = jobPosting.id,
                content = "Question 3",
                category = "Test",
                difficulty = "MEDIUM",
                aiReasoning = "Test",
                orderIndex = 3
            )
        )
        generatedQuestionRepository.save(
            GeneratedQuestion(
                jobPostingId = jobPosting.id,
                content = "Question 1",
                category = "Test",
                difficulty = "EASY",
                aiReasoning = "Test",
                orderIndex = 1
            )
        )
        generatedQuestionRepository.save(
            GeneratedQuestion(
                jobPostingId = jobPosting.id,
                content = "Question 2",
                category = "Test",
                difficulty = "EASY",
                aiReasoning = "Test",
                orderIndex = 2
            )
        )

        // When
        val questions = generatedQuestionRepository.findByJobPostingIdOrderByOrderIndexAsc(jobPosting.id)

        // Then
        assertEquals(3, questions.size)
        assertEquals(1, questions[0].orderIndex)
        assertEquals(2, questions[1].orderIndex)
        assertEquals(3, questions[2].orderIndex)
    }

    @Test
    fun `countByJobPostingId 검증 (질문 개수 카운트)`() {
        // Given
        val jobPosting = jobPostingRepository.save(
            JobPosting(
                userId = testUser.id,
                originalUrl = "https://example.com/count-test",
                companyName = "TestCompany",
                jobTitle = "TestJob",
                jobDescription = "Description"
            )
        )

        repeat(10) { i ->
            generatedQuestionRepository.save(
                GeneratedQuestion(
                    jobPostingId = jobPosting.id,
                    content = "Question ${i + 1}",
                    category = "Test",
                    difficulty = "EASY",
                    aiReasoning = "Test",
                    orderIndex = i + 1
                )
            )
        }

        // When
        val count = generatedQuestionRepository.countByJobPostingId(jobPosting.id)

        // Then
        assertEquals(10, count)
    }

    @Test
    fun `ON DELETE CASCADE 검증 (공고 삭제 시 질문도 함께 삭제)`() {
        // Given
        val jobPosting = jobPostingRepository.save(
            JobPosting(
                userId = testUser.id,
                originalUrl = "https://example.com/cascade-test",
                companyName = "TestCompany",
                jobTitle = "TestJob",
                jobDescription = "Description"
            )
        )

        generatedQuestionRepository.save(
            GeneratedQuestion(
                jobPostingId = jobPosting.id,
                content = "Test Question",
                category = "Test",
                difficulty = "EASY",
                aiReasoning = "Test",
                orderIndex = 1
            )
        )

        // When
        jobPostingRepository.delete(jobPosting)
        jobPostingRepository.flush()

        // Then
        val questions = generatedQuestionRepository.findByJobPostingIdOrderByOrderIndexAsc(jobPosting.id)
        assertTrue(questions.isEmpty())
    }
}
