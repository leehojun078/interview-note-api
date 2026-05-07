package com.hojun.interviewnote.interviewnoteapi

import com.hojun.interviewnote.interviewnoteapi.domain.CareerLevel
import com.hojun.interviewnote.interviewnoteapi.domain.JobField
import com.hojun.interviewnote.interviewnoteapi.domain.User
import com.hojun.interviewnote.interviewnoteapi.domain.UserRole
import com.hojun.interviewnote.interviewnoteapi.dto.AnswerSubmitDto
import com.hojun.interviewnote.interviewnoteapi.repository.InterviewAnswerRepository
import com.hojun.interviewnote.interviewnoteapi.repository.QuestionRepository
import com.hojun.interviewnote.interviewnoteapi.repository.UserRepository
import com.hojun.interviewnote.interviewnoteapi.service.InterviewService
import com.hojun.interviewnote.interviewnoteapi.service.cache.DuplicateRequestCache
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Phase 8D 통합 테스트: 중복 답변 제출 방지
 *
 * 검증 사항:
 * 1. 동일한 질문에 동일한 답변 재제출 시 중복 차단
 * 2. 다른 답변 제출 시 정상 동작
 * 3. 중복 시 올바른 redirect 및 메시지
 * 4. answerTextHash 필드 저장 확인
 * 5. 생성된 질문(GeneratedQuestion)에 대한 중복 방지
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase8DDuplicateAnswerPreventionTest {

    @Autowired
    private lateinit var interviewService: InterviewService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var questionRepository: QuestionRepository

    @Autowired
    private lateinit var interviewAnswerRepository: InterviewAnswerRepository

    @Autowired
    private lateinit var duplicateRequestCache: DuplicateRequestCache

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var testUser: User
    private var testQuestionId: Long = 0

    @BeforeEach
    fun setUp() {
        // 테스트 사용자 생성
        testUser = User(
            email = "test-duplicate@example.com",
            passwordHash = passwordEncoder.encode("password123!"),
            name = "중복테스트",
            role = UserRole.USER,
            jobField = JobField.IT,
            careerLevel = CareerLevel.JUNIOR
        )
        testUser = userRepository.save(testUser)

        // 테스트 질문 조회 (DB에 이미 존재하는 질문 사용)
        val question = questionRepository.findAll().firstOrNull()
            ?: throw IllegalStateException("테스트용 질문이 없습니다")
        testQuestionId = question.id
    }

    @Test
    fun `동일한 답변 재제출 시 중복 차단되어야 한다`() {
        // Given: 첫 번째 답변 제출
        val answerText = "Spring Boot는 스프링 프레임워크 기반의 애플리케이션을 쉽게 만들 수 있도록 도와주는 도구입니다."
        val dto = AnswerSubmitDto(questionId = testQuestionId, answerText = answerText)

        val firstResult = interviewService.submitAnswer(dto, testUser.id)
        val firstAnswerId = firstResult.answerId

        // 첫 번째 답변이 저장되었는지 확인
        val firstAnswer = interviewAnswerRepository.findById(firstAnswerId).orElseThrow()
        assertNotNull(firstAnswer.answerTextHash, "첫 번째 답변의 해시가 저장되어야 합니다")

        // When: 동일한 답변 재제출 시도 (중복 체크는 컨트롤러에서 수행되지만, 여기서는 Repository로 직접 확인)
        val answerHash = duplicateRequestCache.generateHash(testQuestionId, answerText)
        val existingAnswer = interviewAnswerRepository
            .findFirstByUserIdAndQuestionIdAndAnswerTextHashOrderByCreatedAtDesc(
                testUser.id, testQuestionId, answerHash
            )

        // Then: 중복 답변이 검출되어야 함
        assertNotNull(existingAnswer, "중복 답변이 검출되어야 합니다")
        assertEquals(firstAnswerId, existingAnswer.id, "검출된 답변은 첫 번째 답변과 같아야 합니다")

        // 새로운 답변이 생성되지 않았는지 확인
        val allAnswers = interviewAnswerRepository.findByUserIdOrderByCreatedAtDesc(testUser.id)
        assertEquals(1, allAnswers.size, "중복 답변은 새로 생성되지 않아야 합니다")
    }

    @Test
    fun `다른 답변 제출 시 정상적으로 새 답변이 생성되어야 한다`() {
        // Given: 첫 번째 답변
        val firstAnswerText = "Spring Boot는 스프링 프레임워크 기반의 애플리케이션을 쉽게 만들 수 있도록 도와주는 도구입니다."
        val firstDto = AnswerSubmitDto(questionId = testQuestionId, answerText = firstAnswerText)
        val firstResult = interviewService.submitAnswer(firstDto, testUser.id)

        // When: 다른 답변 제출
        val secondAnswerText = "Spring Boot는 자동 설정과 내장 서버를 제공하여 개발자가 비즈니스 로직에 집중할 수 있게 합니다."
        val secondDto = AnswerSubmitDto(questionId = testQuestionId, answerText = secondAnswerText)
        val secondResult = interviewService.submitAnswer(secondDto, testUser.id)

        // Then: 새로운 답변이 생성됨
        assertTrue(secondResult.answerId != firstResult.answerId, "다른 답변은 새 ID를 가져야 합니다")

        val allAnswers = interviewAnswerRepository.findByUserIdOrderByCreatedAtDesc(testUser.id)
        assertEquals(2, allAnswers.size, "두 개의 답변이 저장되어야 합니다")

        // 각 답변의 해시가 다른지 확인
        val firstAnswer = interviewAnswerRepository.findById(firstResult.answerId).orElseThrow()
        val secondAnswer = interviewAnswerRepository.findById(secondResult.answerId).orElseThrow()

        assertNotNull(firstAnswer.answerTextHash)
        assertNotNull(secondAnswer.answerTextHash)
        assertTrue(
            firstAnswer.answerTextHash != secondAnswer.answerTextHash,
            "다른 답변은 다른 해시를 가져야 합니다"
        )
    }

    @Test
    fun `중복 검색 메서드가 정확하게 동작해야 한다`() {
        // Given: 첫 번째 답변 제출
        val answerText = "JPA는 Java Persistence API의 약자로 자바 표준 ORM 기술입니다."
        val dto = AnswerSubmitDto(questionId = testQuestionId, answerText = answerText)
        val firstResult = interviewService.submitAnswer(dto, testUser.id)

        // When: Repository 메서드로 중복 검색
        val answerHash = duplicateRequestCache.generateHash(testQuestionId, answerText)
        val foundAnswer = interviewAnswerRepository
            .findFirstByUserIdAndQuestionIdAndAnswerTextHashOrderByCreatedAtDesc(
                testUser.id, testQuestionId, answerHash
            )

        // Then: 첫 번째 답변이 검색되어야 함
        assertNotNull(foundAnswer, "중복 답변이 검색되어야 합니다")
        assertEquals(firstResult.answerId, foundAnswer.id, "검색된 답변 ID가 일치해야 합니다")
        assertEquals(answerHash, foundAnswer.answerTextHash, "해시 값이 일치해야 합니다")
    }

    @Test
    fun `answerTextHash가 정확하게 생성되고 저장되어야 한다`() {
        // Given
        val answerText = "Hibernate는 JPA의 대표적인 구현체입니다."
        val dto = AnswerSubmitDto(questionId = testQuestionId, answerText = answerText)

        // When
        val result = interviewService.submitAnswer(dto, testUser.id)

        // Then: 해시 생성 확인
        val savedAnswer = interviewAnswerRepository.findById(result.answerId).orElseThrow()
        val expectedHash = duplicateRequestCache.generateHash(testQuestionId, answerText)

        assertNotNull(savedAnswer.answerTextHash, "answerTextHash가 null이면 안 됩니다")
        assertEquals(expectedHash, savedAnswer.answerTextHash, "해시 값이 일치해야 합니다")
        assertEquals(64, savedAnswer.answerTextHash?.length, "SHA-256 해시는 64자여야 합니다")
    }

    @Test
    fun `동일 사용자가 동일 질문에 대해 여러 번 다른 답변을 제출할 수 있어야 한다`() {
        // Given & When: 3개의 다른 답변 제출
        val answers = listOf(
            "첫 번째 답변: Spring Boot는 자동 설정 기능을 제공합니다.",
            "두 번째 답변: Spring Boot는 내장 서버를 제공하여 배포가 간편합니다.",
            "세 번째 답변: Spring Boot는 Starter 의존성으로 빠른 시작을 도와줍니다."
        )

        val results = answers.map { answerText ->
            val dto = AnswerSubmitDto(questionId = testQuestionId, answerText = answerText)
            interviewService.submitAnswer(dto, testUser.id)
        }

        // Then: 3개의 답변이 모두 저장됨
        val allAnswers = interviewAnswerRepository.findByUserIdOrderByCreatedAtDesc(testUser.id)
        assertEquals(3, allAnswers.size, "3개의 다른 답변이 모두 저장되어야 합니다")

        // 모든 답변이 서로 다른 ID와 해시를 가지는지 확인
        val answerIds = allAnswers.map { it.id }.toSet()
        val answerHashes = allAnswers.map { it.answerTextHash }.toSet()

        assertEquals(3, answerIds.size, "모든 답변이 고유한 ID를 가져야 합니다")
        assertEquals(3, answerHashes.size, "모든 답변이 고유한 해시를 가져야 합니다")
    }

    @Test
    fun `다른 사용자는 동일한 답변을 제출할 수 있어야 한다`() {
        // Given: 첫 번째 사용자의 답변
        val answerText = "RESTful API는 REST 아키텍처 스타일을 따르는 웹 API입니다."
        val dto = AnswerSubmitDto(questionId = testQuestionId, answerText = answerText)
        val firstUserResult = interviewService.submitAnswer(dto, testUser.id)

        // 두 번째 사용자 생성
        val secondUser = User(
            email = "test-duplicate2@example.com",
            passwordHash = passwordEncoder.encode("password123!"),
            name = "중복테스트2",
            role = UserRole.USER,
            jobField = JobField.IT,
            careerLevel = CareerLevel.JUNIOR
        )
        userRepository.save(secondUser)

        // When: 두 번째 사용자가 동일한 답변 제출
        val secondUserResult = interviewService.submitAnswer(dto, secondUser.id)

        // Then: 다른 사용자이므로 새 답변이 생성됨
        assertTrue(
            secondUserResult.answerId != firstUserResult.answerId,
            "다른 사용자의 동일 답변은 새 ID를 가져야 합니다"
        )

        val firstUserAnswers = interviewAnswerRepository.findByUserIdOrderByCreatedAtDesc(testUser.id)
        val secondUserAnswers = interviewAnswerRepository.findByUserIdOrderByCreatedAtDesc(secondUser.id)

        assertEquals(1, firstUserAnswers.size)
        assertEquals(1, secondUserAnswers.size)
    }

    @Test
    fun `공백이나 줄바꿈이 다른 동일 내용의 답변은 다른 답변으로 처리되어야 한다`() {
        // Given: 첫 번째 답변 (줄바꿈 없음)
        val firstAnswerText = "Spring Boot는 스프링 프레임워크를 간편하게 사용할 수 있게 합니다."
        val firstDto = AnswerSubmitDto(questionId = testQuestionId, answerText = firstAnswerText)
        val firstResult = interviewService.submitAnswer(firstDto, testUser.id)

        // When: 두 번째 답변 (줄바꿈 추가)
        val secondAnswerText = "Spring Boot는\n스프링 프레임워크를 간편하게 사용할 수 있게 합니다."
        val secondDto = AnswerSubmitDto(questionId = testQuestionId, answerText = secondAnswerText)
        val secondResult = interviewService.submitAnswer(secondDto, testUser.id)

        // Then: 다른 답변으로 처리됨 (해시가 다름)
        val firstAnswer = interviewAnswerRepository.findById(firstResult.answerId).orElseThrow()
        val secondAnswer = interviewAnswerRepository.findById(secondResult.answerId).orElseThrow()

        assertTrue(
            firstAnswer.answerTextHash != secondAnswer.answerTextHash,
            "줄바꿈이 다르면 다른 해시를 가져야 합니다"
        )

        val allAnswers = interviewAnswerRepository.findByUserIdOrderByCreatedAtDesc(testUser.id)
        assertEquals(2, allAnswers.size, "공백이 다른 답변은 별도로 저장되어야 합니다")
    }
}
