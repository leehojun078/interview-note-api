package com.hojun.interviewnote.interviewnoteapi.bugfix

import com.hojun.interviewnote.interviewnoteapi.domain.GeneratedQuestion
import com.hojun.interviewnote.interviewnoteapi.domain.JobPosting
import com.hojun.interviewnote.interviewnoteapi.domain.User
import com.hojun.interviewnote.interviewnoteapi.dto.AnswerSubmitDto
import com.hojun.interviewnote.interviewnoteapi.repository.GeneratedQuestionRepository
import com.hojun.interviewnote.interviewnoteapi.repository.InterviewAnswerRepository
import com.hojun.interviewnote.interviewnoteapi.repository.JobPostingRepository
import com.hojun.interviewnote.interviewnoteapi.repository.UserRepository
import com.hojun.interviewnote.interviewnoteapi.service.InterviewService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Phase 6E 버그 수정 테스트
 *
 * 버그: GeneratedQuestion에 대한 답변 제출 시 questionId=0으로 설정되어
 *      외래키 제약 조건 위반 발생
 *
 * 수정: questionId를 nullable로 변경하고, GeneratedQuestion 답변 시 null로 설정
 *
 * 에러 시나리오:
 * 1. 채용 공고 URL 입력
 * 2. 생성된 질문 중 하나에 답변 작성
 * 3. 제출 시 500 에러 발생 (외래키 제약 조건 위반)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase6EGeneratedQuestionAnswerBugTest {

    @Autowired
    private lateinit var interviewService: InterviewService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jobPostingRepository: JobPostingRepository

    @Autowired
    private lateinit var generatedQuestionRepository: GeneratedQuestionRepository

    @Autowired
    private lateinit var interviewAnswerRepository: InterviewAnswerRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var testUser: User
    private lateinit var testJobPosting: JobPosting
    private lateinit var testGeneratedQuestion: GeneratedQuestion

    @BeforeEach
    fun setUp() {
        // 1. 테스트 사용자 생성
        testUser = userRepository.save(
            User(
                email = "bugfix@test.com",
                passwordHash = passwordEncoder.encode("password123!"),
                name = "버그픽스 테스터",
                jobField = com.hojun.interviewnote.interviewnoteapi.domain.JobField.IT,
                careerLevel = com.hojun.interviewnote.interviewnoteapi.domain.CareerLevel.JUNIOR,
                role = com.hojun.interviewnote.interviewnoteapi.domain.UserRole.USER
            )
        )

        // 2. 테스트 채용 공고 생성
        testJobPosting = jobPostingRepository.save(
            JobPosting(
                userId = testUser.id,
                originalUrl = "https://www.wanted.co.kr/wd/281357",
                companyName = "테스트 회사",
                jobTitle = "백엔드 개발자",
                jobDescription = "Java/Kotlin 백엔드 개발자를 모집합니다. Spring Boot, MySQL, Docker 경험이 필요합니다.",
                selectedJobField = com.hojun.interviewnote.interviewnoteapi.domain.JobField.IT,
                inferredJobField = com.hojun.interviewnote.interviewnoteapi.domain.JobField.IT
            )
        )

        // 3. 테스트 생성 질문 생성
        testGeneratedQuestion = generatedQuestionRepository.save(
            GeneratedQuestion(
                jobPostingId = testJobPosting.id,
                content = "Java와 Kotlin 중 어떤 언어를 더 선호하나요? 그 이유는 무엇인가요?",
                category = "기술역량",
                difficulty = "EASY",
                aiReasoning = "공고에 Java, Kotlin이 언급되어 생성됨",
                orderIndex = 1
            )
        )
    }

    @Test
    @DisplayName("Phase 6E 버그: GeneratedQuestion 답변 제출 시 외래키 제약 조건 위반 발생 → 수정됨")
    fun testGeneratedQuestionAnswerSubmit_shouldNotThrowConstraintViolation() {
        // Given: 생성된 질문에 대한 답변 DTO
        val answerDto = AnswerSubmitDto(
            questionId = null,  // GeneratedQuestion은 questionId가 null
            answerText = "Kotlin을 더 선호합니다. 그 이유는 자바에서 불편했던 부분들인 null safety, extension function 등 유연한 기능들을 제공하기 때문입니다."
        )

        // When: 답변 제출 (버그 수정 전에는 DataIntegrityViolationException 발생)
        val result = interviewService.submitAnswerForGeneratedQuestion(
            generatedQuestionId = testGeneratedQuestion.id,
            dto = answerDto,
            userId = testUser.id
        )

        // Then: 정상적으로 답변이 저장됨
        assertNotNull(result)
        assertNotNull(result.answerId)
        assertEquals(testGeneratedQuestion.content, result.questionContent)
        assertEquals(answerDto.answerText, result.answerText)

        // DB에서 실제로 저장되었는지 확인
        val savedAnswer = interviewAnswerRepository.findById(result.answerId).orElseThrow()
        assertNull(savedAnswer.questionId, "GeneratedQuestion 답변의 questionId는 null이어야 함")
        assertEquals(testGeneratedQuestion.id, savedAnswer.generatedQuestionId, "generatedQuestionId가 올바르게 설정되어야 함")
        assertEquals(testUser.id, savedAnswer.userId)
        assertEquals(answerDto.answerText, savedAnswer.answerText)
    }

    @Test
    @DisplayName("InterviewAnswer: questionId와 generatedQuestionId 둘 다 null이면 안 됨")
    fun testInterviewAnswer_shouldHaveEitherQuestionIdOrGeneratedQuestionId() {
        // 이 테스트는 애플리케이션 레벨에서 보장되어야 함
        // DB 레벨에서는 둘 다 nullable이지만, 비즈니스 로직에서는 하나만 있어야 함

        val answers = interviewAnswerRepository.findAll()
        answers.forEach { answer ->
            val hasQuestionId = answer.questionId != null
            val hasGeneratedQuestionId = answer.generatedQuestionId != null

            assertTrue(
                hasQuestionId xor hasGeneratedQuestionId,
                "questionId와 generatedQuestionId 중 정확히 하나만 있어야 함: answerId=${answer.id}"
            )
        }
    }

    @Test
    @DisplayName("에러 로그에 나타난 정확한 시나리오 재현 테스트")
    fun testExactBugScenario_fromErrorLog() {
        // Given: 에러 로그에서 확인된 시나리오
        // - generatedQuestionId: 1
        // - userId: 1
        // - 답변: "kotlin을 더 선호합니다. 그 이유는..."

        val answerText = """
            kotlin을 더 선호합니다.
            그 이유는 자바에서 불편했던 부분들인 null safety, extend function 등 유연한 기능들을 제공하기 때문입니다.
        """.trimIndent()

        val answerDto = AnswerSubmitDto(
            questionId = null,
            answerText = answerText
        )

        // When: 답변 제출 (버그 수정 전: DataIntegrityViolationException)
        assertDoesNotThrow {
            val result = interviewService.submitAnswerForGeneratedQuestion(
                generatedQuestionId = testGeneratedQuestion.id,
                dto = answerDto,
                userId = testUser.id
            )

            // Then: 정상적으로 평가 결과까지 받음
            assertNotNull(result.feedback)
            assertTrue(result.feedback.averageScore >= 1.0)
            assertTrue(result.feedback.averageScore <= 5.0)
        }
    }
}
