package com.hojun.interviewnote.interviewnoteapi

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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.*

/**
 * Phase 6C 통합 테스트
 *
 * 테스트 범위:
 * 1. JobPostingService 조회 기능
 * 2. 소유권 검증
 * 3. JobPosting 및 GeneratedQuestion 연동
 *
 * 참고: 실제 URL 파싱은 Phase6A/6B 테스트에서 검증됨
 * 여기서는 Repository를 통해 직접 데이터 생성
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase6CIntegrationTest {

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

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        // 테스트 사용자 생성
        testUser = User(
            email = "phase6ctest@example.com",
            passwordHash = passwordEncoder.encode("password123"),
            name = "Phase6C Test User",
            role = UserRole.USER
        )
        userRepository.save(testUser)
    }

    // ========================================
    // 1. 데이터 생성 Helper
    // ========================================

    private fun createTestJobPosting(
        userId: Long = testUser.id,
        jobField: JobField = JobField.IT
    ): JobPosting {
        val jobPosting = JobPosting(
            userId = userId,
            originalUrl = "https://test.com/posting-${System.currentTimeMillis()}",
            companyName = "테스트 회사",
            jobTitle = "${jobField.displayName} 담당자",
            jobDescription = "직무 설명",
            selectedJobField = jobField,
            requiredSkills = """["기술1", "기술2"]""",
            preferredSkills = """["기술3"]"""
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
                aiReasoning = "테스트 근거",
                orderIndex = index
            )
        }
        return questions.map { generatedQuestionRepository.save(it) }
    }

    // ========================================
    // 2. getJobPostingWithQuestions 테스트
    // ========================================

    @Test
    fun `getJobPostingWithQuestions는 공고와 질문을 함께 반환한다`() {
        // Given
        val jobPosting = createTestJobPosting(jobField = JobField.MARKETING)
        createTestQuestions(jobPosting.id)

        // When
        val viewModel = jobPostingService.getJobPostingWithQuestions(jobPosting.id, testUser.id)

        // Then
        assertEquals(jobPosting.id, viewModel.id)
        assertEquals(testUser.id, viewModel.userId)
        assertEquals(10, viewModel.questions.size)
        assertEquals(JobField.MARKETING, viewModel.effectiveJobField)
    }

    @Test
    fun `다른 사용자의 공고는 조회할 수 없다`() {
        // Given: 첫 번째 사용자가 공고 생성
        val jobPosting = createTestJobPosting()

        // 두 번째 사용자 생성
        val anotherUser = User(
            email = "another@example.com",
            passwordHash = passwordEncoder.encode("password123"),
            name = "Another User",
            role = UserRole.USER
        )
        userRepository.save(anotherUser)

        // When & Then: 두 번째 사용자가 조회 시도
        assertFailsWith<JobPostingNotFoundException> {
            jobPostingService.getJobPostingWithQuestions(jobPosting.id, anotherUser.id)
        }
    }

    @Test
    fun `존재하지 않는 공고 조회 시 예외 발생`() {
        // When & Then
        assertFailsWith<JobPostingNotFoundException> {
            jobPostingService.getJobPostingWithQuestions(99999L, testUser.id)
        }
    }

    // ========================================
    // 3. findByUserId 테스트
    // ========================================

    @Test
    fun `findByUserId는 사용자의 공고 목록을 반환한다`() {
        // Given: 3개 공고 생성
        repeat(3) { index ->
            val jobPosting = createTestJobPosting(jobField = JobField.IT)
            createTestQuestions(jobPosting.id)
        }

        // When
        val summaries = jobPostingService.findByUserId(testUser.id)

        // Then
        assertEquals(3, summaries.size)
        summaries.forEach { summary ->
            assertEquals(10, summary.questionCount)
            assertNotNull(summary.effectiveJobField)
        }
    }

    @Test
    fun `비활성화된 공고는 목록에 표시되지 않는다`() {
        // Given
        val jobPosting = createTestJobPosting()

        // 공고 비활성화
        jobPosting.deactivate()
        jobPostingRepository.save(jobPosting)

        // When
        val summaries = jobPostingService.findByUserId(testUser.id)

        // Then: 비활성화된 공고는 제외
        assertTrue(summaries.none { it.id == jobPosting.id })
    }

    // ========================================
    // 4. JSON 스킬 파싱 테스트
    // ========================================

    @Test
    fun `requiredSkills와 preferredSkills가 정상적으로 파싱된다`() {
        // Given
        val jobPosting = createTestJobPosting()
        createTestQuestions(jobPosting.id)

        // When
        val viewModel = jobPostingService.getJobPostingWithQuestions(jobPosting.id, testUser.id)

        // Then: JSON 스킬이 파싱되어 List<String>으로 변환됨
        assertNotNull(viewModel.requiredSkills)
        assertEquals(2, viewModel.requiredSkills?.size)
        assertNotNull(viewModel.preferredSkills)
        assertEquals(1, viewModel.preferredSkills?.size)
    }

    // ========================================
    // 5. effectiveJobField 테스트
    // ========================================

    @Test
    fun `selectedJobField가 우선 적용된다`() {
        // Given
        val jobPosting = JobPosting(
            userId = testUser.id,
            originalUrl = "https://test.com/test",
            companyName = "Test Company",
            jobTitle = "Test Job",
            jobDescription = "Description",
            selectedJobField = JobField.SALES,
            inferredJobField = JobField.IT
        )
        val saved = jobPostingRepository.save(jobPosting)

        // When
        val effectiveField = saved.effectiveJobField

        // Then
        assertEquals(JobField.SALES, effectiveField)
    }
}
