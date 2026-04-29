package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.domain.JobField
import com.hojun.interviewnote.interviewnoteapi.dto.CreateJobPostingRequest
import com.hojun.interviewnote.interviewnoteapi.exception.JobPostingNotFoundException
import com.hojun.interviewnote.interviewnoteapi.exception.JobPostingParseException
import com.hojun.interviewnote.interviewnoteapi.exception.RateLimitExceededException
import com.hojun.interviewnote.interviewnoteapi.repository.UserRepository
import com.hojun.interviewnote.interviewnoteapi.service.JobPostingService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes

/**
 * 채용 공고 Controller
 *
 * Phase 6C에서 추가됨
 * - 공고 등록 폼, 공고 등록 처리, 질문 목록 조회
 */
@Controller
@RequestMapping("/job-postings")
class JobPostingController(
    private val jobPostingService: JobPostingService,
    private val userRepository: UserRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 공고 등록 폼
     *
     * GET /job-postings/create
     */
    @GetMapping("/create")
    fun createForm(model: Model): String {
        model.addAttribute("jobFields", JobField.values())
        return "job-postings/create"
    }

    /**
     * 공고 등록 처리
     *
     * POST /job-postings
     *
     * 흐름:
     * 1. Rate Limiting 체크
     * 2. 7일 캐시 체크
     * 3. URL 파싱
     * 4. 공고 저장
     * 5. 질문 10개 생성
     * 6. /job-postings/{id}/questions로 redirect
     */
    @PostMapping
    fun create(
        @AuthenticationPrincipal userDetails: UserDetails,
        @Valid @ModelAttribute request: CreateJobPostingRequest,
        bindingResult: BindingResult,
        redirectAttributes: RedirectAttributes
    ): String {
        // Validation 오류
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "URL을 올바르게 입력해주세요")
            return "redirect:/job-postings/create"
        }

        try {
            // 사용자 ID 조회
            val user = userRepository.findByEmail(userDetails.username)
                ?: throw IllegalStateException("사용자를 찾을 수 없습니다")

            // 공고 등록 + 질문 생성
            val jobPosting = jobPostingService.createJobPosting(
                userId = user.id,
                originalUrl = request.originalUrl,
                selectedJobField = request.selectedJobField
            )

            logger.info("공고 등록 성공 - 공고 ID: ${jobPosting.id}, 사용자: ${user.email}")

            redirectAttributes.addFlashAttribute("success", "질문이 생성되었습니다")
            return "redirect:/job-postings/${jobPosting.id}/questions"

        } catch (e: RateLimitExceededException) {
            logger.warn("질문 생성 한도 초과 - 사용자: ${userDetails.username}", e)
            redirectAttributes.addFlashAttribute(
                "error",
                "질문 생성 한도를 초과했습니다 (10회/24시간). 나중에 다시 시도해주세요."
            )
            return "redirect:/job-postings/create"

        } catch (e: JobPostingParseException) {
            logger.error("공고 파싱 실패 - URL: ${request.originalUrl}", e)
            redirectAttributes.addFlashAttribute(
                "error",
                "채용 공고를 불러올 수 없습니다. URL을 확인하거나 다른 공고를 시도해주세요."
            )
            return "redirect:/job-postings/create"

        } catch (e: Exception) {
            logger.error("공고 등록 실패 - 예상치 못한 오류", e)
            redirectAttributes.addFlashAttribute(
                "error",
                "질문 생성 중 오류가 발생했습니다: ${e.message}"
            )
            return "redirect:/job-postings/create"
        }
    }

    /**
     * 공고 상세 + 질문 목록
     *
     * GET /job-postings/{id}/questions
     */
    @GetMapping("/{id}/questions")
    fun questions(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: Long,
        model: Model,
        redirectAttributes: RedirectAttributes
    ): String {
        try {
            // 사용자 ID 조회
            val user = userRepository.findByEmail(userDetails.username)
                ?: throw IllegalStateException("사용자를 찾을 수 없습니다")

            // 공고 + 질문 조회
            val jobPosting = jobPostingService.getJobPostingWithQuestions(id, user.id)

            model.addAttribute("jobPosting", jobPosting)
            return "job-postings/questions"

        } catch (e: JobPostingNotFoundException) {
            logger.warn("공고 접근 실패 - 공고 ID: $id, 사용자: ${userDetails.username}", e)
            redirectAttributes.addFlashAttribute("error", e.message)
            return "redirect:/job-postings"
        }
    }

    /**
     * 내 공고 목록
     *
     * GET /job-postings
     */
    @GetMapping
    fun list(
        @AuthenticationPrincipal userDetails: UserDetails,
        model: Model
    ): String {
        // 사용자 ID 조회
        val user = userRepository.findByEmail(userDetails.username)
            ?: throw IllegalStateException("사용자를 찾을 수 없습니다")

        // 공고 목록 조회
        val jobPostings = jobPostingService.findByUserId(user.id)

        model.addAttribute("jobPostings", jobPostings)
        return "job-postings/list"
    }
}
