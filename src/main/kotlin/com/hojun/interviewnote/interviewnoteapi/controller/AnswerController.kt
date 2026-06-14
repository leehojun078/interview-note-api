package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.domain.InterviewDraft
import com.hojun.interviewnote.interviewnoteapi.dto.AnswerSubmitDto
import com.hojun.interviewnote.interviewnoteapi.exception.RateLimitExceededException
import com.hojun.interviewnote.interviewnoteapi.repository.InterviewAnswerRepository
import com.hojun.interviewnote.interviewnoteapi.repository.InterviewDraftRepository
import com.hojun.interviewnote.interviewnoteapi.repository.UserRepository
import com.hojun.interviewnote.interviewnoteapi.service.InterviewService
import com.hojun.interviewnote.interviewnoteapi.service.cache.DuplicateRequestCache
import com.hojun.interviewnote.interviewnoteapi.service.ratelimit.RateLimitService
import com.hojun.interviewnote.interviewnoteapi.service.validation.AnswerValidator
import com.hojun.interviewnote.interviewnoteapi.service.validation.ValidationResult
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
class AnswerController(
    private val interviewService: InterviewService,
    private val rateLimitService: RateLimitService,
    private val answerValidator: AnswerValidator,
    private val userRepository: UserRepository,
    private val draftRepository: InterviewDraftRepository,
    private val duplicateRequestCache: DuplicateRequestCache,
    private val interviewAnswerRepository: InterviewAnswerRepository
) {
    private val logger = LoggerFactory.getLogger(AnswerController::class.java)
    /**
     * 답변 제출 처리
     * Phase 4A-2에서 수정: @AuthenticationPrincipal 추가, userId 기반 Rate Limit
     */
    @PostMapping("/questions/{questionId}/answer")
    @Transactional
    fun submitAnswer(
        @PathVariable questionId: Long,
        @Valid @ModelAttribute dto: AnswerSubmitDto,
        bindingResult: BindingResult,
        model: Model,
        request: HttpServletRequest,
        redirectAttributes: RedirectAttributes,
        @AuthenticationPrincipal userDetails: UserDetails
    ): String {
        // 현재 로그인한 사용자 조회
        val user = userRepository.findByEmail(userDetails.username)
            ?: throw IllegalStateException("로그인한 사용자를 찾을 수 없습니다")

        // Phase 4A-2: Rate Limit 체크 (IP → User ID로 변경)
        try {
            rateLimitService.checkAndRecordRequest(user.id.toString())
        } catch (e: RateLimitExceededException) {
            return ControllerConstants.buildRedirectWithError("/questions/$questionId/answer", ControllerConstants.ERROR_RATELIMIT)
        }

        // Bean Validation 체크
        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", bindingResult.allErrors)
            return ControllerConstants.buildRedirectWithError("/questions/$questionId/answer", ControllerConstants.ERROR_VALIDATION)
        }

        // Phase 3A: 답변 품질 사전 검증
        val validationResult = answerValidator.validate(dto.answerText ?: "")
        if (validationResult is ValidationResult.Invalid) {
            redirectAttributes.addFlashAttribute("validationError", validationResult.message)
            return ControllerConstants.buildRedirectWithError("/questions/$questionId/answer", ControllerConstants.ERROR_INVALID_ANSWER)
        }

        // Phase 8D: 중복 답변 제출 방지
        val answerText = dto.answerText ?: ""
        val answerHash = duplicateRequestCache.generateHash(questionId, answerText)

        val existingAnswer = interviewAnswerRepository
            .findFirstByUserIdAndQuestionIdAndAnswerTextHashOrderByCreatedAtDesc(
                user.id, questionId, answerHash
            )

        if (existingAnswer != null) {
            logger.info("중복 답변 감지 - userId: ${user.id}, questionId: $questionId, 기존 answerId: ${existingAnswer.id}")

            redirectAttributes.addFlashAttribute("info",
                "이전과 동일한 답변입니다. 답변을 수정해서 더 나은 평가를 받아보세요!")

            return ControllerConstants.buildRedirectWithDuplicate("/answers/${existingAnswer.id}/feedback")
        }

        // 답변 제출 및 AI 평가 (userId 전달)
        return try {
            logger.info("답변 제출 시작 - questionId: $questionId, userId: ${user.id}")

            val result = interviewService.submitAnswer(dto, user.id)

            logger.info("답변 제출 완료 - answerId: ${result.answerId}, 평균점수: ${result.feedback.averageScore}")

            // Phase 3: 답변 제출 시 Draft 삭제
            draftRepository.deleteByUserIdAndQuestionId(user.id, questionId)

            // Phase 3A: 저품질 경고 체크
            if (result.feedback.averageScore < 1.5) {
                return ControllerConstants.buildRedirectWithWarning("/answers/${result.answerId}/feedback", ControllerConstants.WARNING_LOW_QUALITY)
            }

            "redirect:/answers/${result.answerId}/feedback"
        } catch (e: Exception) {
            logger.error("답변 제출 중 오류 발생 - questionId: $questionId, userId: ${user.id}", e)
            redirectAttributes.addFlashAttribute("error", "답변 처리 중 오류가 발생했습니다: ${e.message}")
            ControllerConstants.buildRedirectWithError("/questions/$questionId/answer", ControllerConstants.ERROR_SUBMIT_FAILED)
        }
    }

    /**
     * 클라이언트 IP 주소 추출
     * X-Forwarded-For 헤더를 우선 확인 (프록시 환경 대응)
     */
    private fun getClientIp(request: HttpServletRequest): String {
        val forwardedFor = request.getHeader("X-Forwarded-For")
        return if (!forwardedFor.isNullOrBlank()) {
            forwardedFor.split(",").first().trim()
        } else {
            request.remoteAddr
        }
    }

    /**
     * 평가 결과 페이지
     */
    @GetMapping("/answers/{answerId}/feedback")
    fun feedback(
        @PathVariable answerId: Long,
        @RequestParam(required = false) warning: String?,
        @RequestParam(required = false) duplicate: String?,
        model: Model
    ): String {
        val answerWithFeedback = interviewService.getAnswerWithFeedback(answerId)

        model.addAttribute("answer", answerWithFeedback)
        model.addAttribute("averageScore", answerWithFeedback.feedback.averageScore)

        // Phase 3A: 저품질 경고
        if (warning == ControllerConstants.WARNING_LOW_QUALITY) {
            model.addAttribute("lowQualityWarning", true)
        }

        // Phase 8D: 중복 답변 알림
        if (duplicate == ControllerConstants.VALUE_TRUE) {
            model.addAttribute("duplicateAnswer", true)
        }

        return "answers/feedback"
    }

    /**
     * Phase 3: 답변 자동 저장 (Draft)
     *
     * HTMX를 통해 2초마다 자동 저장
     * - hx-post="/questions/{id}/draft"
     * - hx-trigger="keyup changed delay:2s"
     */
    @PostMapping("/questions/{questionId}/draft")
    @Transactional
    @ResponseBody
    fun saveDraft(
        @PathVariable questionId: Long,
        @RequestParam answerText: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<Map<String, Any>> {
        val draftText = answerText
        // 현재 로그인한 사용자 조회
        val user = userRepository.findByEmail(userDetails.username)
            ?: return ResponseEntity.badRequest().body(mapOf("success" to false, "message" to "사용자를 찾을 수 없습니다"))

        // 기존 Draft가 있으면 업데이트, 없으면 새로 생성
        val draft = draftRepository.findByUserIdAndQuestionId(user.id, questionId)

        if (draft != null) {
            // 기존 Draft 업데이트
            draft.updateDraft(draftText)
            draftRepository.save(draft)
        } else {
            // 새 Draft 생성
            val newDraft = InterviewDraft(
                userId = user.id,
                questionId = questionId,
                draftText = draftText
            )
            draftRepository.save(newDraft)
        }

        return ResponseEntity.ok(mapOf(
            "success" to true,
            "message" to "임시 저장 완료",
            "savedAt" to java.time.LocalDateTime.now().toString()
        ))
    }

    /**
     * Phase 3: Draft 불러오기
     *
     * 페이지 로드 시 저장된 Draft가 있으면 textarea에 자동 입력
     */
    @GetMapping("/questions/{questionId}/draft")
    @ResponseBody
    fun getDraft(
        @PathVariable questionId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<Map<String, Any?>> {
        // 현재 로그인한 사용자 조회
        val user = userRepository.findByEmail(userDetails.username)
            ?: return ResponseEntity.badRequest().body(mapOf("success" to false, "draftText" to null))

        val draft = draftRepository.findByUserIdAndQuestionId(user.id, questionId)

        return if (draft != null) {
            ResponseEntity.ok(mapOf(
                "success" to true,
                "draftText" to draft.draftText,
                "lastSaved" to draft.lastSaved.toString()
            ))
        } else {
            ResponseEntity.ok(mapOf(
                "success" to false,
                "draftText" to null
            ))
        }
    }
}
