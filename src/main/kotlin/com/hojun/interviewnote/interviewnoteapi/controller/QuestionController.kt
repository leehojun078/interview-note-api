package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.domain.JobField
import com.hojun.interviewnote.interviewnoteapi.service.QuestionService
import com.hojun.interviewnote.interviewnoteapi.service.UserService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/questions")
class QuestionController(
    private val questionService: QuestionService,
    private val userService: UserService
) {
    /**
     * 질문 목록 페이지
     *
     * Phase 5: 직무 필터링 추가
     * - 로그인한 사용자의 기본 직무 적용
     * - jobField 파라미터가 없으면 사용자 직무 사용
     * - 미로그인 or 직무 미설정 시 "IT" 기본값
     */
    @GetMapping
    fun list(
        @AuthenticationPrincipal userDetails: UserDetails?,
        @RequestParam(required = false) jobField: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) difficulty: String?,
        model: Model
    ): String {
        // 로그인한 사용자의 기본 직무 가져오기
        val defaultJobField = userDetails?.let { details ->
            userService.findByEmail(details.username)?.jobField?.name
        }

        // jobField 파라미터가 없으면 사용자 기본 직무 사용
        val effectiveJobField = jobField ?: defaultJobField

        val questions = questionService.findAll(effectiveJobField, category, difficulty)

        // Phase 5: 직무별 카테고리 맵 전달 (동적 필터링용)
        val categoriesByJobField = questionService.getCategoriesByAllJobFields()

        model.addAttribute("questions", questions)
        model.addAttribute("selectedJobField", effectiveJobField ?: "")
        model.addAttribute("selectedCategory", category ?: "")
        model.addAttribute("selectedDifficulty", difficulty ?: "")
        model.addAttribute("jobFields", JobField.values())
        model.addAttribute("categoriesByJobField", categoriesByJobField)

        return "questions/list"
    }

    /**
     * Phase 3: HTMX - 질문 목록 Fragment만 반환
     *
     * HTMX 요청 시 전체 페이지가 아닌 질문 목록 Fragment만 반환
     */
    @GetMapping("/fragment")
    fun listFragment(
        @AuthenticationPrincipal userDetails: UserDetails?,
        @RequestParam(required = false) jobField: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) difficulty: String?,
        model: Model
    ): String {
        // 로그인한 사용자의 기본 직무 가져오기
        val defaultJobField = userDetails?.let { details ->
            userService.findByEmail(details.username)?.jobField?.name
        }

        // jobField 파라미터가 없으면 사용자 기본 직무 사용
        val effectiveJobField = jobField ?: defaultJobField

        val questions = questionService.findAll(effectiveJobField, category, difficulty)

        model.addAttribute("questions", questions)

        // Fragment만 반환
        return "questions/list :: question-list-fragment"
    }

    /**
     * 질문 상세 + 답변 작성 페이지
     */
    @GetMapping("/{id}/answer")
    fun answerForm(
        @PathVariable id: Long,
        model: Model
    ): String {
        val question = questionService.findDtoById(id)
        model.addAttribute("question", question)

        return "questions/answer"
    }
}
