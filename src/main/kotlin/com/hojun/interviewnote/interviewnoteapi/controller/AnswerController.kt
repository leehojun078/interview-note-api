package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.dto.AnswerSubmitDto
import com.hojun.interviewnote.interviewnoteapi.service.InterviewService
import jakarta.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*

@Controller
class AnswerController(
    private val interviewService: InterviewService
) {
    /**
     * 답변 제출 처리
     */
    @PostMapping("/questions/{questionId}/answer")
    fun submitAnswer(
        @PathVariable questionId: Long,
        @Valid @ModelAttribute dto: AnswerSubmitDto,
        bindingResult: BindingResult,
        model: Model
    ): String {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", bindingResult.allErrors)
            return "redirect:/questions/$questionId/answer?error=validation"
        }

        val result = interviewService.submitAnswer(dto)

        return "redirect:/answers/${result.answerId}/feedback"
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
