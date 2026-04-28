package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.domain.User
import com.hojun.interviewnote.interviewnoteapi.domain.UserRole
import com.hojun.interviewnote.interviewnoteapi.dto.AnswerWithFeedbackDto
import com.hojun.interviewnote.interviewnoteapi.dto.FeedbackDto
import com.hojun.interviewnote.interviewnoteapi.exception.RateLimitExceededException
import com.hojun.interviewnote.interviewnoteapi.repository.InterviewDraftRepository
import com.hojun.interviewnote.interviewnoteapi.repository.UserRepository
import com.hojun.interviewnote.interviewnoteapi.service.InterviewService
import com.hojun.interviewnote.interviewnoteapi.service.ratelimit.RateLimitService
import com.hojun.interviewnote.interviewnoteapi.service.validation.AnswerValidator
import com.hojun.interviewnote.interviewnoteapi.service.validation.ValidationResult
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@WebMvcTest(AnswerController::class)
@Import(com.hojun.interviewnote.interviewnoteapi.config.SecurityConfig::class)
class AnswerControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var interviewService: InterviewService

    @MockitoBean
    private lateinit var rateLimitService: RateLimitService

    @MockitoBean
    private lateinit var answerValidator: AnswerValidator

    @MockitoBean
    private lateinit var userRepository: UserRepository

    @MockitoBean
    private lateinit var draftRepository: InterviewDraftRepository

    private val testUser = User(
        id = 1L,
        email = "test@example.com",
        passwordHash = "hashed",
        name = "Test User",
        role = UserRole.USER,
        isActive = true,
        createdAt = LocalDateTime.now()
    )

    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        // 기본적으로 모든 답변은 검증 통과로 설정
        whenever(answerValidator.validate(any())).thenReturn(ValidationResult.Valid)
        // 인증된 사용자 mock
        whenever(userRepository.findByEmail("user")).thenReturn(testUser)
    }

    private fun createAnswerWithFeedback(): AnswerWithFeedbackDto {
        val feedback = FeedbackDto(
            logicScore = 4,
            specificityScore = 3,
            jobFitScore = 4,
            deliveryScore = 3,
            strengths = listOf("강점1", "강점2"),
            improvements = listOf("개선점1", "개선점2"),
            modelAnswer = "모범답변입니다.",
            overallComment = "전반적으로 좋습니다."
        )

        return AnswerWithFeedbackDto(
            answerId = 1L,
            questionId = 1L,
            questionContent = "테스트 질문",
            answerText = "테스트 답변",
            answeredAt = LocalDateTime.now(),
            feedback = feedback
        )
    }

    @Test
    @WithMockUser(username = "user")
    fun `정상적인 답변 제출시 피드백 페이지로 리다이렉트한다`() {
        // given
        val result = createAnswerWithFeedback()
        whenever(interviewService.submitAnswer(any(), any())).thenReturn(result)

        // when & then
        mockMvc.perform(
            post("/questions/1/answer")
                .param("questionId", "1")
                .param("answerText", "a".repeat(100)) // 50자 이상
                .with(csrf())
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/answers/1/feedback"))
    }

    @Test
    @WithMockUser(username = "user")
    fun `답변이 50자 미만이면 Validation 에러로 리다이렉트한다`() {
        // when & then
        mockMvc.perform(
            post("/questions/1/answer")
                .param("questionId", "1")
                .param("answerText", "짧은 답변") // 50자 미만
                .with(csrf())
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/questions/1/answer?error=validation"))
    }

    @Test
    @WithMockUser(username = "user")
    fun `답변이 2000자 초과하면 Validation 에러로 리다이렉트한다`() {
        // when & then
        mockMvc.perform(
            post("/questions/1/answer")
                .param("questionId", "1")
                .param("answerText", "a".repeat(2001)) // 2000자 초과
                .with(csrf())
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/questions/1/answer?error=validation"))
    }

    @Test
    @WithMockUser(username = "user")
    fun `답변이 비어있으면 Validation 에러로 리다이렉트한다`() {
        // when & then
        mockMvc.perform(
            post("/questions/1/answer")
                .param("questionId", "1")
                .param("answerText", "")
                .with(csrf())
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/questions/1/answer?error=validation"))
    }

    @Test
    @WithMockUser(username = "user")
    fun `피드백 페이지를 렌더링한다`() {
        // given
        val result = createAnswerWithFeedback()
        whenever(interviewService.getAnswerWithFeedback(1L)).thenReturn(result)

        // when & then
        mockMvc.perform(get("/answers/1/feedback"))
            .andExpect(status().isOk)
            .andExpect(view().name("answers/feedback"))
            .andExpect(model().attributeExists("answer"))
            .andExpect(model().attributeExists("averageScore"))
            .andExpect(model().attribute("averageScore", 3.5)) // (4+3+4+3)/4
    }

    // ========== Rate Limit 테스트 ==========

    @Test
    @WithMockUser(username = "user")
    fun `답변 제출 시 Rate Limit 체크가 호출된다`() {
        // given
        val result = createAnswerWithFeedback()
        whenever(interviewService.submitAnswer(any(), any())).thenReturn(result)

        // when
        mockMvc.perform(
            post("/questions/1/answer")
                .param("questionId", "1")
                .param("answerText", "a".repeat(100))
                .with(csrf())
        )
            .andExpect(status().is3xxRedirection)

        // then
        verify(rateLimitService).checkAndRecordRequest("1")
    }

    @Test
    @WithMockUser(username = "user")
    fun `Rate Limit 초과 시 에러 페이지로 리다이렉트한다`() {
        // given
        val resetTime = LocalDateTime.now().plusHours(1)
        doThrow(RateLimitExceededException("1", 33, resetTime))
            .whenever(rateLimitService).checkAndRecordRequest(any())

        // when & then
        mockMvc.perform(
            post("/questions/1/answer")
                .param("questionId", "1")
                .param("answerText", "a".repeat(100))
                .with(csrf())
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/questions/1/answer?error=ratelimit"))

        verify(rateLimitService).checkAndRecordRequest("1")
    }

    @Test
    @WithMockUser(username = "user")
    fun `사용자 ID 기반으로 Rate Limit이 적용된다`() {
        // given
        val result = createAnswerWithFeedback()
        whenever(interviewService.submitAnswer(any(), any())).thenReturn(result)

        // when
        mockMvc.perform(
            post("/questions/1/answer")
                .param("questionId", "1")
                .param("answerText", "a".repeat(100))
                .with(csrf())
        )
            .andExpect(status().is3xxRedirection)

        // then - 사용자 ID로 Rate Limit 체크
        verify(rateLimitService).checkAndRecordRequest("1")
    }

    @Test
    @WithMockUser(username = "user")
    fun `동일 사용자는 일관된 Rate Limit을 적용받는다`() {
        // given
        val result = createAnswerWithFeedback()
        whenever(interviewService.submitAnswer(any(), any())).thenReturn(result)

        // when
        mockMvc.perform(
            post("/questions/1/answer")
                .param("questionId", "1")
                .param("answerText", "a".repeat(100))
                .with(csrf())
        )
            .andExpect(status().is3xxRedirection)

        // then - 동일한 사용자 ID로 Rate Limit 체크
        verify(rateLimitService).checkAndRecordRequest("1")
    }

    @Test
    @WithMockUser(username = "user")
    fun `Rate Limit 체크는 Validation 전에 실행된다`() {
        // given
        val resetTime = LocalDateTime.now().plusHours(1)
        doThrow(RateLimitExceededException("1", 33, resetTime))
            .whenever(rateLimitService).checkAndRecordRequest(any())

        // when - Validation 오류가 있는 요청 (50자 미만)
        mockMvc.perform(
            post("/questions/1/answer")
                .param("questionId", "1")
                .param("answerText", "짧은 답변") // Validation 실패
                .with(csrf())
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/questions/1/answer?error=ratelimit")) // Rate Limit 에러가 먼저

        // then
        verify(rateLimitService).checkAndRecordRequest("1")
    }

    @Test
    @WithMockUser(username = "user")
    fun `사용자 ID 기반 Rate Limit이 정상 동작한다`() {
        // given
        val result = createAnswerWithFeedback()
        whenever(interviewService.submitAnswer(any(), any())).thenReturn(result)

        // when
        mockMvc.perform(
            post("/questions/1/answer")
                .param("questionId", "1")
                .param("answerText", "a".repeat(100))
                .with(csrf())
        )
            .andExpect(status().is3xxRedirection)

        // then - 사용자 ID로 Rate Limit 체크
        verify(rateLimitService).checkAndRecordRequest("1")
    }
}
