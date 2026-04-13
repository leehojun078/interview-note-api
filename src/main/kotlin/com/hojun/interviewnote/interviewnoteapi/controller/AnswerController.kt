package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.dto.AnswerSubmitDto
import com.hojun.interviewnote.interviewnoteapi.exception.RateLimitExceededException
import com.hojun.interviewnote.interviewnoteapi.service.InterviewService
import com.hojun.interviewnote.interviewnoteapi.service.ratelimit.RateLimitService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*

@Controller
class AnswerController(
    private val interviewService: InterviewService,
    private val rateLimitService: RateLimitService
) {
    /**
     * 답변 제출 처리
     */
    @PostMapping("/questions/{questionId}/answer")
    fun submitAnswer(
        @PathVariable questionId: Long,
        @Valid @ModelAttribute dto: AnswerSubmitDto,
        bindingResult: BindingResult,
        model: Model,
        request: HttpServletRequest
    ): String {
        // Phase 2D: Rate Limit 체크
        try {
            val clientIp = getClientIp(request)
            rateLimitService.checkAndRecordRequest(clientIp)
        } catch (e: RateLimitExceededException) {
            return "redirect:/questions/$questionId/answer?error=ratelimit"
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", bindingResult.allErrors)
            return "redirect:/questions/$questionId/answer?error=validation"
        }

        val result = interviewService.submitAnswer(dto)

        return "redirect:/answers/${result.answerId}/feedback"
    }

    /**
     * 클라이언트 IP 주소 추출
     * X-Forwarded-For 헤더를 우선 확인 (프록시 환경 대응)
     */
    private fun getClientIp(request: HttpServletRequest): String {
        val forwardedFor = request.getHeader("X-Forwarded-For")
        return if (!forwardedFor.isNullOrBlank()) {
            forwardedFor.split(",").first().trim()
        } else {
            request.remoteAddr
        }
    }

    /**
     * 평가 결과 페이지
     */
    @GetMapping("/answers/{answerId}/feedback")
    fun feedback(
        @PathVariable answerId: Long,
        model: Model
    ): String {
        val answerWithFeedback = interviewService.getAnswerWithFeedback(answerId)

        model.addAttribute("answer", answerWithFeedback)
        model.addAttribute("averageScore", answerWithFeedback.feedback.averageScore)

        return "answers/feedback"
    }
}
