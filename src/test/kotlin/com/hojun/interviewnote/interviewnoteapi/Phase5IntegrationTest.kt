package com.hojun.interviewnote.interviewnoteapi

import com.hojun.interviewnote.interviewnoteapi.domain.CareerLevel
import com.hojun.interviewnote.interviewnoteapi.domain.JobField
import com.hojun.interviewnote.interviewnoteapi.domain.User
import com.hojun.interviewnote.interviewnoteapi.domain.UserRole
import com.hojun.interviewnote.interviewnoteapi.dto.UpdateProfileRequest
import com.hojun.interviewnote.interviewnoteapi.repository.QuestionRepository
import com.hojun.interviewnote.interviewnoteapi.repository.UserRepository
import com.hojun.interviewnote.interviewnoteapi.service.QuestionService
import com.hojun.interviewnote.interviewnoteapi.service.UserService
import com.hojun.interviewnote.interviewnoteapi.service.ai.PromptBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.*

/**
 * Phase 5 통합 테스트
 *
 * Step 17: Integration Testing and Bug Fixes
 *
 * 테스트 범위:
 * 1. 340개 질문 데이터 검증 (17개 직무 × 20개)
 * 2. JobField, CareerLevel enum 검증
 * 3. User 프로필 업데이트 검증
 * 4. QuestionService jobField 필터링 검증
 * 5. PromptBuilder 17개 직무 프롬프트 생성 검증
 * 6. Edge Case 처리 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase5IntegrationTest {

    @Autowired
    private lateinit var questionRepository: QuestionRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var questionService: QuestionService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var promptBuilder: PromptBuilder

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        // 테스트 사용자 생성
        testUser = User(
            email = "phase5test@example.com",
            passwordHash = passwordEncoder.encode("password123"),
            name = "Phase5 Test User",
            role = UserRole.USER
        )
        userRepository.save(testUser)
    }

    // ========================================
    // 1. 질문 데이터 검증 (340개 질문)
    // ========================================

    @Test
    fun `340개 질문 데이터가 정상적으로 로드되었는지 확인`() {
        // Given & When
        val allQuestions = questionRepository.findAll()

        // Then
        assertEquals(340, allQuestions.size, "전체 질문은 340개여야 합니다 (17개 직무 × 20개)")
    }

    @Test
    fun `17개 직무별로 20개씩 질문이 존재하는지 확인`() {
        // Given
        val expectedJobFields = listOf(
            "PLANNING", "MARKETING", "ACCOUNTING", "HR", "ADMIN", "IT", "DESIGN",
            "SALES", "MD", "SERVICE", "PRODUCTION", "CONSTRUCTION", "MEDICAL",
            "EDUCATION", "MEDIA", "FINANCE", "PUBLIC"
        )

        // When
        val allQuestions = questionRepository.findAll()
        val jobFields = allQuestions.map { it.jobField }.distinct().sorted()

        // Then
        assertEquals(17, jobFields.size, "17개 직무가 모두 있어야 합니다")
        assertTrue(jobFields.containsAll(expectedJobFields), "모든 직무가 포함되어야 합니다")

        // 각 직무별로 20개씩 있는지 확인
        expectedJobFields.forEach { jobField ->
            val questionsPerJob = questionRepository.findByJobFieldAndIsActiveTrue(jobField)
            assertEquals(20, questionsPerJob.size, "$jobField 직무는 20개 질문이 있어야 합니다")
        }
    }

    @Test
    fun `각 직무별 난이도 분포 확인 (EASY 5개, MEDIUM 10개, HARD 5개)`() {
        // Given
        val jobField = "SALES" // 영업 직무 테스트

        // When
        val salesQuestions = questionRepository.findByJobFieldAndIsActiveTrue(jobField)
        val easyCount = salesQuestions.count { it.difficulty == "EASY" }
        val mediumCount = salesQuestions.count { it.difficulty == "MEDIUM" }
        val hardCount = salesQuestions.count { it.difficulty == "HARD" }

        // Then
        assertEquals(5, easyCount, "$jobField EASY 질문은 5개여야 합니다")
        assertEquals(10, mediumCount, "$jobField MEDIUM 질문은 10개여야 합니다")
        assertEquals(5, hardCount, "$jobField HARD 질문은 5개여야 합니다")
    }

    // ========================================
    // 2. JobField, CareerLevel Enum 검증
    // ========================================

    @Test
    fun `JobField enum 17개 값이 정의되어 있는지 확인`() {
        // Given & When
        val jobFields = JobField.values()

        // Then
        assertEquals(17, jobFields.size, "JobField는 17개여야 합니다")

        // 각 JobField에 displayName과 code가 있는지 확인
        jobFields.forEach { jobField ->
            assertNotNull(jobField.displayName, "${jobField.name} displayName이 있어야 합니다")
            assertNotNull(jobField.code, "${jobField.name} code가 있어야 합니다")
        }
    }

    @Test
    fun `CareerLevel enum 4개 값이 정의되어 있는지 확인`() {
        // Given & When
        val careerLevels = CareerLevel.values()

        // Then
        assertEquals(4, careerLevels.size, "CareerLevel은 4개여야 합니다")

        // 예상 값 확인
        assertTrue(careerLevels.any { it == CareerLevel.ENTRY })
        assertTrue(careerLevels.any { it == CareerLevel.JUNIOR })
        assertTrue(careerLevels.any { it == CareerLevel.SENIOR })
        assertTrue(careerLevels.any { it == CareerLevel.SENIOR_PLUS })
    }

    // ========================================
    // 3. User 프로필 업데이트 검증
    // ========================================

    @Test
    fun `사용자 직무 선호도 업데이트가 정상 작동하는지 확인`() {
        // Given
        val request = UpdateProfileRequest(
            name = "Updated Name",
            jobField = JobField.SALES,
            careerLevel = CareerLevel.JUNIOR
        )

        // When
        val updatedUser = userService.updateProfile(testUser.id, request)

        // Then
        assertEquals("Updated Name", updatedUser.name)
        assertEquals(JobField.SALES, updatedUser.jobField)
        assertEquals(CareerLevel.JUNIOR, updatedUser.careerLevel)
    }

    @Test
    fun `직무 미설정 사용자의 jobField는 null이어야 함`() {
        // Given & When
        val user = userRepository.findById(testUser.id).get()

        // Then
        assertNull(user.jobField, "초기 사용자의 jobField는 null이어야 합니다")
        assertNull(user.careerLevel, "초기 사용자의 careerLevel은 null이어야 합니다")
    }

    @Test
    fun `프로필 업데이트 시 jobField를 null로 설정 가능`() {
        // Given
        testUser.updateJobPreferences(JobField.IT, CareerLevel.ENTRY)
        userRepository.save(testUser)

        val request = UpdateProfileRequest(
            name = "Test User",
            jobField = null, // null로 재설정
            careerLevel = null
        )

        // When
        val updatedUser = userService.updateProfile(testUser.id, request)

        // Then
        assertNull(updatedUser.jobField)
        assertNull(updatedUser.careerLevel)
    }

    // ========================================
    // 4. QuestionService jobField 필터링 검증
    // ========================================

    @Test
    fun `QuestionService가 jobField로 질문을 필터링하는지 확인`() {
        // Given
        val jobField = "IT"

        // When
        val questions = questionService.findAll(jobField = jobField, category = null, difficulty = null)

        // Then
        assertEquals(20, questions.size, "IT 직무 질문은 20개여야 합니다")
        assertTrue(questions.all { it.jobField == jobField }, "모든 질문이 IT 직무여야 합니다")
    }

    @Test
    fun `QuestionService에서 jobField null 시 IT 기본값 사용`() {
        // Given & When
        val questions = questionService.findAll(jobField = null, category = null, difficulty = null)

        // Then
        assertEquals(20, questions.size, "기본값(IT) 질문은 20개여야 합니다")
        assertTrue(questions.all { it.jobField == "IT" }, "모든 질문이 IT 직무여야 합니다")
    }

    @Test
    fun `QuestionService에서 jobField와 category 조합 필터링`() {
        // Given
        val jobField = "SALES"
        val category = "고객관리"

        // When
        val questions = questionService.findAll(jobField = jobField, category = category, difficulty = null)

        // Then
        assertTrue(questions.isNotEmpty(), "영업-고객관리 질문이 있어야 합니다")
        assertTrue(questions.all { it.jobField == jobField && it.category == category })
    }

    @Test
    fun `QuestionService에서 jobField, category, difficulty 조합 필터링`() {
        // Given
        val jobField = "IT"
        val category = "기술역량"
        val difficulty = "MEDIUM"

        // When
        val questions = questionService.findAll(
            jobField = jobField,
            category = category,
            difficulty = difficulty
        )

        // Then
        assertTrue(questions.isNotEmpty(), "IT-기술역량-MEDIUM 질문이 있어야 합니다")
        assertTrue(questions.all {
            it.jobField == jobField &&
            it.category == category &&
            it.difficulty == difficulty
        })
    }

    // ========================================
    // 5. PromptBuilder 17개 직무 프롬프트 생성 검증
    // ========================================

    @Test
    fun `PromptBuilder가 IT 직무 프롬프트를 생성하는지 확인`() {
        // Given
        val jobField = "IT"
        val targetJob = "백엔드 개발자"

        // When
        val prompt = promptBuilder.buildSystemPrompt(jobField, targetJob)

        // Then
        assertNotNull(prompt)
        assertTrue(prompt.contains(targetJob), "프롬프트에 targetJob이 포함되어야 합니다")
        assertTrue(prompt.contains("논리성"), "평가 기준이 포함되어야 합니다")
        assertTrue(prompt.contains("구체성"), "평가 기준이 포함되어야 합니다")
    }

    @Test
    fun `PromptBuilder가 영업 직무 프롬프트를 생성하는지 확인`() {
        // Given
        val jobField = "SALES"
        val targetJob = "영업매니저"

        // When
        val prompt = promptBuilder.buildSystemPrompt(jobField, targetJob)

        // Then
        assertNotNull(prompt)
        assertTrue(prompt.contains(targetJob), "프롬프트에 targetJob이 포함되어야 합니다")
        assertTrue(prompt.contains("영업") || prompt.contains("고객") || prompt.contains("실적"),
            "영업 관련 키워드가 포함되어야 합니다")
    }

    @Test
    fun `PromptBuilder가 17개 모든 직무의 프롬프트를 생성할 수 있는지 확인`() {
        // Given
        val jobFields = listOf(
            "PLANNING", "MARKETING", "ACCOUNTING", "HR", "ADMIN", "IT", "DESIGN",
            "SALES", "MD", "SERVICE", "PRODUCTION", "CONSTRUCTION", "MEDICAL",
            "EDUCATION", "MEDIA", "FINANCE", "PUBLIC"
        )

        // When & Then
        jobFields.forEach { jobField ->
            val prompt = promptBuilder.buildSystemPrompt(jobField, "테스트 직무")
            assertNotNull(prompt, "$jobField 프롬프트가 생성되어야 합니다")
            assertTrue(prompt.length > 100, "$jobField 프롬프트가 충분한 길이여야 합니다")
        }
    }

    @Test
    fun `PromptBuilder가 지원하지 않는 직무에 대해 예외를 던지는지 확인`() {
        // Given
        val invalidJobField = "INVALID_JOB"

        // When & Then
        assertFailsWith<IllegalArgumentException> {
            promptBuilder.buildSystemPrompt(invalidJobField, "테스트 직무")
        }
    }

    // ========================================
    // 6. Edge Case 처리 검증
    // ========================================

    @Test
    fun `존재하지 않는 jobField로 질문 조회 시 빈 리스트 반환`() {
        // Given
        val nonExistentJobField = "INVALID"

        // When
        val questions = questionRepository.findByJobFieldAndIsActiveTrue(nonExistentJobField)

        // Then
        assertTrue(questions.isEmpty(), "존재하지 않는 직무는 빈 리스트를 반환해야 합니다")
    }

    @Test
    fun `빈 문자열 jobField는 IT 기본값으로 처리`() {
        // Given
        val emptyJobField = ""

        // When
        val questions = questionService.findAll(jobField = emptyJobField, category = null, difficulty = null)

        // Then
        assertEquals(20, questions.size, "빈 문자열은 IT 기본값으로 처리되어야 합니다")
        assertTrue(questions.all { it.jobField == "IT" })
    }

    @Test
    fun `공백 문자열 jobField는 IT 기본값으로 처리`() {
        // Given
        val blankJobField = "   "

        // When
        val questions = questionService.findAll(jobField = blankJobField, category = null, difficulty = null)

        // Then
        assertEquals(20, questions.size, "공백 문자열은 IT 기본값으로 처리되어야 합니다")
        assertTrue(questions.all { it.jobField == "IT" })
    }

    // ========================================
    // 7. 성능 테스트 (간단한 확인)
    // ========================================

    @Test
    fun `340개 질문 전체 조회 성능 확인`() {
        // Given & When
        val startTime = System.currentTimeMillis()
        val allQuestions = questionRepository.findAll()
        val endTime = System.currentTimeMillis()

        // Then
        assertEquals(340, allQuestions.size)
        val elapsedTime = endTime - startTime
        assertTrue(elapsedTime < 1000, "340개 질문 조회는 1초 이내여야 합니다 (실제: ${elapsedTime}ms)")
    }

    @Test
    fun `jobField 인덱스가 적용되어 빠른 조회가 가능한지 확인`() {
        // Given
        val jobField = "IT"

        // When
        val startTime = System.currentTimeMillis()
        val questions = questionRepository.findByJobFieldAndIsActiveTrue(jobField)
        val endTime = System.currentTimeMillis()

        // Then
        assertEquals(20, questions.size)
        val elapsedTime = endTime - startTime
        assertTrue(elapsedTime < 200, "jobField 필터링은 200ms 이내여야 합니다 (실제: ${elapsedTime}ms)")
    }

    // ========================================
    // 8. 회귀 테스트 (기존 기능 정상 동작)
    // ========================================

    @Test
    fun `기존 IT 질문 데이터가 여전히 정상 작동하는지 확인`() {
        // Given
        val jobField = "IT"

        // When
        val itQuestions = questionRepository.findByJobFieldAndIsActiveTrue(jobField)

        // Then
        assertEquals(20, itQuestions.size, "IT 질문은 20개여야 합니다")

        // 기존 카테고리 확인
        val categories = itQuestions.map { it.category }.distinct()
        assertTrue(categories.contains("기술역량"))
        assertTrue(categories.contains("문제해결"))
        assertTrue(categories.contains("협업경험"))
    }

    @Test
    fun `QuestionRepository 기존 메서드가 정상 작동하는지 확인`() {
        // Given & When
        val categoryQuestions = questionRepository.findByCategoryAndIsActiveTrue("기술역량")
        val difficultyQuestions = questionRepository.findByDifficultyAndIsActiveTrue("MEDIUM")
        val combinedQuestions = questionRepository.findByCategoryAndDifficultyAndIsActiveTrue(
            "기술역량", "MEDIUM"
        )

        // Then
        assertTrue(categoryQuestions.isNotEmpty())
        assertTrue(difficultyQuestions.isNotEmpty())
        assertTrue(combinedQuestions.isNotEmpty())
    }
}
