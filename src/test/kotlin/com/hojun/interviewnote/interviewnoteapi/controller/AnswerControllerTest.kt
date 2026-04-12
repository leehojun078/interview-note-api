package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.dto.AnswerWithFeedbackDto
import com.hojun.interviewnote.interviewnoteapi.dto.FeedbackDto
import com.hojun.interviewnote.interviewnoteapi.service.InterviewService
import com.hojun.interviewnote.interviewnoteapi.service.ratelimit.RateLimitService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@WebMvcTest(AnswerController::class)
class AnswerControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var interviewService: InterviewService

    @MockitoBean
    private lateinit var rateLimitService: RateLimitService

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
    fun `정상적인 답변 제출시 피드백 페이지로 리다이렉트한다`() {
        // given
        val result = createAnswerWithFeedback()
        whenever(interviewService.submitAnswer(any())).thenReturn(result)

        // when & then
        mockMvc.perform(
            post("/questions/1/answer")
                .param("questionId", "1")
                .param("answerText", "a".repeat(100)) // 50자 이상
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/answers/1/feedback"))
    }

    @Test
    fun `답변이 50자 미만이면 Validation 에러로 리다이렉트한다`() {
        // when & then
        mockMvc.perform(
            post("/questions/1/answer")
                .param("questionId", "1")
                .param("answerText", "짧은 답변") // 50자 미만
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/questions/1/answer?error=validation"))
    }

    @Test
    fun `답변이 2000자 초과하면 Validation 에러로 리다이렉트한다`() {
        // when & then
        mockMvc.perform(
            post("/questions/1/answer")
                .param("questionId", "1")
                .param("answerText", "a".repeat(2001)) // 2000자 초과
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/questions/1/answer?error=validation"))
    }

    @Test
    fun `답변이 비어있으면 Validation 에러로 리다이렉트한다`() {
        // when & then
        mockMvc.perform(
            post("/questions/1/answer")
                .param("questionId", "1")
                .param("answerText", "")
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/questions/1/answer?error=validation"))
    }

    @Test
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
}
