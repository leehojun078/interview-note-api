package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.service.InterviewService
import com.hojun.interviewnote.interviewnoteapi.service.ReviewService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/reviews")
class ReviewController(
    private val reviewService: ReviewService,
    private val interviewService: InterviewService
) {
    /**
     * 복기 이력 목록 페이지
     */
    @GetMapping
    fun list(model: Model): String {
        val reviews = reviewService.getReviewList()
        model.addAttribute("reviews", reviews)

        return "reviews/list"
    }

    /**
     * 복기 상세 페이지
     */
    @GetMapping("/{answerId}")
    fun detail(
        @PathVariable answerId: Long,
        model: Model
    ): String {
        val answerWithFeedback = interviewService.getAnswerWithFeedback(answerId)

        model.addAttribute("answer", answerWithFeedback)
        model.addAttribute("averageScore", answerWithFeedback.feedback.averageScore)

        return "reviews/detail"
    }
}
