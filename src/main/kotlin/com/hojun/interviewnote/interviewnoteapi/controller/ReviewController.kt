package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.repository.UserRepository
import com.hojun.interviewnote.interviewnoteapi.service.InterviewService
import com.hojun.interviewnote.interviewnoteapi.service.ReviewService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/reviews")
class ReviewController(
    private val reviewService: ReviewService,
    private val interviewService: InterviewService,
    private val userRepository: UserRepository
) {
    /**
     * 리뷰 이력 목록 페이지
     * Phase 4A-2에서 수정: 사용자별 리뷰 조회
     */
    @GetMapping
    fun list(
        model: Model,
        @AuthenticationPrincipal userDetails: UserDetails
    ): String {
        // 현재 로그인한 사용자 조회
        val user = userRepository.findByEmail(userDetails.username)
            ?: throw IllegalStateException("로그인한 사용자를 찾을 수 없습니다")

        // 사용자별 리뷰 조회
        val reviews = reviewService.getUserReviews(user.id)
        model.addAttribute("reviews", reviews)
        model.addAttribute("currentUser", user)

        return "reviews/list"
    }

    /**
     * 리뷰 상세 페이지
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
