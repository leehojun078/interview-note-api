package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.dto.AnswerSubmitDto
import com.hojun.interviewnote.interviewnoteapi.exception.JobPostingNotFoundException
import com.hojun.interviewnote.interviewnoteapi.exception.RateLimitExceededException
import com.hojun.interviewnote.interviewnoteapi.repository.GeneratedQuestionRepository
import com.hojun.interviewnote.interviewnoteapi.repository.InterviewDraftRepository
import com.hojun.interviewnote.interviewnoteapi.repository.UserRepository
import com.hojun.interviewnote.interviewnoteapi.service.InterviewService
import com.hojun.interviewnote.interviewnoteapi.service.ratelimit.RateLimitService
import com.hojun.interviewnote.interviewnoteapi.service.validation.AnswerValidator
import com.hojun.interviewnote.interviewnoteapi.service.validation.ValidationResult
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes

/**
 * AI 생성 질문 Controller
 *
 * Phase 6C에서 추가됨
 * - 생성된 질문에 대한 답변 작성 폼 및 제출
 */
@Controller
@RequestMapping("/generated-questions")
class GeneratedQuestionController(
    private val generatedQuestionRepository: GeneratedQuestionRepository,
    private val interviewService: InterviewService,
    private val rateLimitService: RateLimitService,
    private val answerValidator: AnswerValidator,
    private val userRepository: UserRepository,
    private val draftRepository: InterviewDraftRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 생성된 질문 답변 작성 폼
     *
     * GET /generated-questions/{id}/answer
     *
     * 기존 questions/answer.html 재사용
     */
    @GetMapping("/{id}/answer")
    fun answerForm(
        @PathVariable id: Long,
        model: Model
    ): String {
        val question = generatedQuestionRepository.findById(id).orElseThrow {
            JobPostingNotFoundException("질문을 찾을 수 없습니다: ID=$id")
        }

        // 기존 QuestionDto와 유사한 구조로 변환
        val questionDto = object {
            val id = question.id
            val content = question.content
            val category = question.category
            val difficulty = question.difficulty
            val isGenerated = true  // AI 생성 질문 플래그
        }

        model.addAttribute("question", questionDto)
        return "questions/answer"  // 기존 템플릿 재사용
    }

    /**
     * 생성된 질문 답변 제출
     *
     * POST /generated-questions/{id}/answer
     *
     * AnswerController와 유사하지만 generatedQuestionId를 사용
     */
    @PostMapping("/{id}/answer")
    @Transactional
    fun submitAnswer(
        @PathVariable id: Long,
        @Valid @ModelAttribute dto: AnswerSubmitDto,
        bindingResult: BindingResult,
        model: Model,
        redirectAttributes: RedirectAttributes,
        @AuthenticationPrincipal userDetails: UserDetails
    ): String {
        // 현재 로그인한 사용자 조회
        val user = userRepository.findByEmail(userDetails.username)
            ?: throw IllegalStateException("로그인한 사용자를 찾을 수 없습니다")

        // Rate Limit 체크
        try {
            rateLimitService.checkAndRecordRequest(user.id.toString())
        } catch (e: RateLimitExceededException) {
            return ControllerConstants.buildRedirectWithError("/generated-questions/$id/answer", ControllerConstants.ERROR_RATELIMIT)
        }

        // Bean Validation 체크
        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", bindingResult.allErrors)
            return ControllerConstants.buildRedirectWithError("/generated-questions/$id/answer", ControllerConstants.ERROR_VALIDATION)
        }

        // 답변 품질 사전 검증
        val validationResult = answerValidator.validate(dto.answerText ?: "")
        if (validationResult is ValidationResult.Invalid) {
            redirectAttributes.addFlashAttribute("validationError", validationResult.message)
            return ControllerConstants.buildRedirectWithError("/generated-questions/$id/answer", ControllerConstants.ERROR_INVALID_ANSWER)
        }

        // 답변 제출 및 AI 평가 (generatedQuestionId 포함)
        return try {
            logger.info("생성된 질문 답변 제출 시작 - generatedQuestionId: $id, userId: ${user.id}")

            val result = interviewService.submitAnswerForGeneratedQuestion(id, dto, user.id)

            logger.info("답변 제출 완료 - answerId: ${result.answerId}, 평균점수: ${result.feedback.averageScore}")

            // TODO: Draft 삭제 (GeneratedQuestion용 draft 지원 필요)
            // draftRepository.deleteByUserIdAndGeneratedQuestionId(user.id, id)

            // 저품질 경고 체크
            if (result.feedback.averageScore < 1.5) {
                return ControllerConstants.buildRedirectWithWarning("/answers/${result.answerId}/feedback", ControllerConstants.WARNING_LOW_QUALITY)
            }

            "redirect:/answers/${result.answerId}/feedback"
        } catch (e: Exception) {
            logger.error("생성된 질문 답변 제출 중 오류 - generatedQuestionId: $id, userId: ${user.id}", e)
            redirectAttributes.addFlashAttribute("error", "답변 처리 중 오류가 발생했습니다: ${e.message}")
            ControllerConstants.buildRedirectWithError("/generated-questions/$id/answer", ControllerConstants.ERROR_SUBMIT_FAILED)
        }
    }
}
