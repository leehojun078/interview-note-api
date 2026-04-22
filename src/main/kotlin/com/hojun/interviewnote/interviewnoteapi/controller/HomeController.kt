package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.exception.UserNotFoundException
import com.hojun.interviewnote.interviewnoteapi.service.QuestionService
import com.hojun.interviewnote.interviewnoteapi.service.ReviewService
import com.hojun.interviewnote.interviewnoteapi.service.UserService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HomeController(
    private val reviewService: ReviewService,
    private val userService: UserService,
    private val questionService: QuestionService
) {
    /**
     * 홈 페이지
     *
     * Phase 4A-2에서 수정: 로그인한 사용자의 최근 리뷰 표시
     * Phase 5에서 수정: 직무 기반 개인화 (추천 질문, 직무 미설정 배너)
     */
    @GetMapping("/", "/home")
    fun home(
        @AuthenticationPrincipal userDetails: UserDetails?,
        model: Model
    ): String {
        val isLoggedIn = userDetails != null
        model.addAttribute("isLoggedIn", isLoggedIn)

        if (userDetails != null) {
            val user = userService.findByEmail(userDetails.username)
                ?: throw UserNotFoundException("사용자를 찾을 수 없습니다")

            // 최근 리뷰 3개
            val recentReviews = reviewService.getUserReviews(user.id).take(3)
            model.addAttribute("recentReviews", recentReviews)

            // 직무 설정 여부
            val hasJobFieldSet = user.jobField != null
            model.addAttribute("hasJobFieldSet", hasJobFieldSet)

            // 추천 질문 (사용자 직무 기반, 없으면 IT 기본)
            val jobField = user.jobField?.name ?: "IT"
            val recommendedQuestions = questionService.findAll(jobField, null, null)
                .shuffled()
                .take(5)
            model.addAttribute("recommendedQuestions", recommendedQuestions)
            model.addAttribute("userJobField", user.jobField?.displayName ?: "IT개발")
        } else {
            model.addAttribute("recentReviews", emptyList<Any>())
        }

        return "home"
    }
}
