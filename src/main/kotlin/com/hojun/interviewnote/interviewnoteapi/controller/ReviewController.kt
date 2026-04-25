package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.repository.UserRepository
import com.hojun.interviewnote.interviewnoteapi.service.InterviewService
import com.hojun.interviewnote.interviewnoteapi.service.ReviewService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

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
     * Phase 2 (UI/UX): 페이지네이션 추가
     */
    @GetMapping
    fun list(
        model: Model,
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): String {
        // 현재 로그인한 사용자 조회
        val user = userRepository.findByEmail(userDetails.username)
            ?: throw IllegalStateException("로그인한 사용자를 찾을 수 없습니다")

        // 페이지네이션된 리뷰 조회
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val reviewsPage = reviewService.getUserReviewsPage(user.id, pageable)

        model.addAttribute("reviews", reviewsPage.content)
        model.addAttribute("page", reviewsPage)
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
