package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.service.ReviewService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HomeController(
    private val reviewService: ReviewService
) {
    /**
     * 홈 페이지
     */
    @GetMapping("/", "/home")
    fun home(model: Model): String {
        // 최근 답변 3개
        val recentReviews = reviewService.getReviewList().take(3)
        model.addAttribute("recentReviews", recentReviews)

        return "home"
    }
}
