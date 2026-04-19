package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.repository.UserRepository
import com.hojun.interviewnote.interviewnoteapi.service.ReviewService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HomeController(
    private val reviewService: ReviewService,
    private val userRepository: UserRepository
) {
    /**
     * 홈 페이지
     * Phase 4A-2에서 수정: 로그인한 사용자의 최근 리뷰 표시
     */
    @GetMapping("/", "/home")
    fun home(
        model: Model,
        @AuthenticationPrincipal userDetails: UserDetails?
    ): String {
        // 로그인한 경우: 최근 답변 3개 표시
        val recentReviews = if (userDetails != null) {
            val user = userRepository.findByEmail(userDetails.username)
            if (user != null) {
                reviewService.getUserReviews(user.id).take(3)
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }

        model.addAttribute("recentReviews", recentReviews)
        model.addAttribute("isLoggedIn", userDetails != null)

        return "home"
    }
}
