package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.domain.CareerLevel
import com.hojun.interviewnote.interviewnoteapi.domain.JobField
import com.hojun.interviewnote.interviewnoteapi.dto.UpdateProfileRequest
import com.hojun.interviewnote.interviewnoteapi.exception.UserNotFoundException
import com.hojun.interviewnote.interviewnoteapi.service.UserService
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.mvc.support.RedirectAttributes

/**
 * 프로필 설정 컨트롤러
 *
 * Phase 5에서 추가됨
 * - 프로필 조회 (GET /profile)
 * - 프로필 업데이트 (POST /profile/update)
 */
@Controller
@RequestMapping("/profile")
class ProfileController(
    private val userService: UserService
) {

    /**
     * 프로필 설정 페이지
     *
     * @param userDetails 인증된 사용자 정보
     * @param model 뷰 모델
     * @return 프로필 설정 페이지 템플릿
     */
    @GetMapping
    fun showProfileSettings(
        @AuthenticationPrincipal userDetails: UserDetails,
        model: Model
    ): String {
        val user = userService.findByEmail(userDetails.username)
            ?: throw UserNotFoundException("사용자를 찾을 수 없습니다")

        val profile = userService.getProfile(user.id)

        model.addAttribute("profile", profile)
        model.addAttribute("jobFields", JobField.values())
        model.addAttribute("careerLevels", CareerLevel.values())

        return "profile/settings"
    }

    /**
     * 프로필 업데이트
     *
     * @param userDetails 인증된 사용자 정보
     * @param request 업데이트 요청 정보
     * @param bindingResult 검증 결과
     * @param redirectAttributes Flash 메시지용 attributes
     * @return 리다이렉트 URL
     */
    @PostMapping("/update")
    fun updateProfile(
        @AuthenticationPrincipal userDetails: UserDetails,
        @Valid @ModelAttribute request: UpdateProfileRequest,
        bindingResult: BindingResult,
        redirectAttributes: RedirectAttributes
    ): String {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "입력 값을 확인해주세요")
            return "redirect:/profile"
        }

        val user = userService.findByEmail(userDetails.username)
            ?: throw UserNotFoundException("사용자를 찾을 수 없습니다")

        userService.updateProfile(user.id, request)

        redirectAttributes.addFlashAttribute("success", "프로필이 업데이트되었습니다")
        return "redirect:/profile"
    }
}
