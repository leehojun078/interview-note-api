package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.mvc.support.RedirectAttributes

/**
 * 인증 컨트롤러
 *
 * Phase 4A에서 추가됨
 * - 로그인 페이지
 * - 회원가입 페이지
 * - 회원가입 처리
 */
@Controller
@RequestMapping("/auth")
class AuthController(
    private val userService: UserService,
    private val userDetailsService: UserDetailsService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 로그인 페이지
     *
     * GET /auth/login
     */
    @GetMapping("/login")
    fun loginPage(
        @org.springframework.web.bind.annotation.RequestParam(required = false) error: String?,
        @org.springframework.web.bind.annotation.RequestParam(required = false) logout: String?,
        model: Model
    ): String {
        if (error != null) {
            model.addAttribute("error", "이메일 또는 비밀번호가 올바르지 않습니다.")
        }
        if (logout != null) {
            model.addAttribute("message", "로그아웃되었습니다.")
        }
        return "auth/login"
    }

    /**
     * 회원가입 페이지
     *
     * GET /auth/register
     */
    @GetMapping("/register")
    fun registerPage(model: Model): String {
        model.addAttribute("registerForm", RegisterForm())
        return "auth/register"
    }

    /**
     * 회원가입 처리
     *
     * POST /auth/register-process
     */
    @PostMapping("/register-process")
    fun register(
        @Validated @ModelAttribute registerForm: RegisterForm,
        bindingResult: BindingResult,
        redirectAttributes: RedirectAttributes,
        request: HttpServletRequest
    ): String {
        logger.info("회원가입 요청: email={}", registerForm.email)

        // Validation 에러 체크
        if (bindingResult.hasErrors()) {
            logger.warn("회원가입 실패: Validation 에러 - errors={}", bindingResult.allErrors)
            return "auth/register"
        }

        // 비밀번호 확인 체크
        if (registerForm.password != registerForm.passwordConfirm) {
            bindingResult.rejectValue("passwordConfirm", "error.passwordConfirm", "비밀번호가 일치하지 않습니다.")
            return "auth/register"
        }

        try {
            // 회원가입
            val user = userService.register(
                email = registerForm.email,
                password = registerForm.password,
                name = registerForm.name
            )

            logger.info("회원가입 성공: userId={}, email={}", user.id, user.email)

            // 자동 로그인
            val userDetails = userDetailsService.loadUserByUsername(user.email)
            val authentication = UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.authorities
            )
            authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
            SecurityContextHolder.getContext().authentication = authentication

            // 로그인 일시 갱신
            userService.updateLastLoginAt(user.id)

            // 성공 메시지와 함께 질문 목록으로 리다이렉트
            redirectAttributes.addFlashAttribute("message", "회원가입이 완료되었습니다! 환영합니다, ${user.name}님!")
            return "redirect:/questions"

        } catch (e: IllegalArgumentException) {
            // 비즈니스 로직 에러 (이메일 중복, 비밀번호 유효성 등)
            logger.warn("회원가입 실패: {}", e.message)
            bindingResult.rejectValue("email", "error.email", e.message!!)
            return "auth/register"

        } catch (e: Exception) {
            // 예상치 못한 에러
            logger.error("회원가입 중 예상치 못한 에러 발생", e)
            bindingResult.reject("error.global", "회원가입 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
            return "auth/register"
        }
    }
}

/**
 * 회원가입 폼 DTO
 */
data class RegisterForm(
    @field:NotBlank(message = "이메일을 입력해주세요")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    val email: String = "",

    @field:NotBlank(message = "비밀번호를 입력해주세요")
    @field:Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다")
    val password: String = "",

    @field:NotBlank(message = "비밀번호 확인을 입력해주세요")
    val passwordConfirm: String = "",

    @field:NotBlank(message = "이름을 입력해주세요")
    @field:Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다")
    val name: String = ""
)
