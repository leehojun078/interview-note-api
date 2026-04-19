package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.dto.AnswerSubmitDto
import com.hojun.interviewnote.interviewnoteapi.exception.RateLimitExceededException
import com.hojun.interviewnote.interviewnoteapi.repository.UserRepository
import com.hojun.interviewnote.interviewnoteapi.service.InterviewService
import com.hojun.interviewnote.interviewnoteapi.service.ratelimit.RateLimitService
import com.hojun.interviewnote.interviewnoteapi.service.validation.AnswerValidator
import com.hojun.interviewnote.interviewnoteapi.service.validation.ValidationResult
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
class AnswerController(
    private val interviewService: InterviewService,
    private val rateLimitService: RateLimitService,
    private val answerValidator: AnswerValidator,
    private val userRepository: UserRepository
) {
    /**
     * 답변 제출 처리
     * Phase 4A-2에서 수정: @AuthenticationPrincipal 추가, userId 기반 Rate Limit
     */
    @PostMapping("/questions/{questionId}/answer")
    fun submitAnswer(
        @PathVariable questionId: Long,
        @Valid @ModelAttribute dto: AnswerSubmitDto,
        bindingResult: BindingResult,
        model: Model,
        request: HttpServletRequest,
        redirectAttributes: RedirectAttributes,
        @AuthenticationPrincipal userDetails: UserDetails
    ): String {
        // 현재 로그인한 사용자 조회
        val user = userRepository.findByEmail(userDetails.username)
            ?: throw IllegalStateException("로그인한 사용자를 찾을 수 없습니다")

        // Phase 4A-2: Rate Limit 체크 (IP → User ID로 변경)
        try {
            rateLimitService.checkAndRecordRequest(user.id.toString())
        } catch (e: RateLimitExceededException) {
            return "redirect:/questions/$questionId/answer?error=ratelimit"
        }

        // Bean Validation 체크
        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", bindingResult.allErrors)
            return "redirect:/questions/$questionId/answer?error=validation"
        }

        // Phase 3A: 답변 품질 사전 검증
        val validationResult = answerValidator.validate(dto.answerText ?: "")
        if (validationResult is ValidationResult.Invalid) {
            redirectAttributes.addFlashAttribute("validationError", validationResult.message)
            return "redirect:/questions/$questionId/answer?error=invalid_answer"
        }

        // 답변 제출 및 AI 평가 (userId 전달)
        val result = interviewService.submitAnswer(dto, user.id)

        // Phase 3A: 저품질 경고 체크
        if (result.feedback.averageScore < 1.5) {
            return "redirect:/answers/${result.answerId}/feedback?warning=low_quality"
        }

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
        @RequestParam(required = false) warning: String?,
        model: Model
    ): String {
        val answerWithFeedback = interviewService.getAnswerWithFeedback(answerId)

        model.addAttribute("answer", answerWithFeedback)
        model.addAttribute("averageScore", answerWithFeedback.feedback.averageScore)

        // Phase 3A: 저품질 경고
        if (warning == "low_quality") {
            model.addAttribute("lowQualityWarning", true)
        }

        return "answers/feedback"
    }
}
