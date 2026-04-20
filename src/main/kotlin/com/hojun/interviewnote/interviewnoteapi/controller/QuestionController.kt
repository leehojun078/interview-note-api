package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.service.QuestionService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/questions")
class QuestionController(
    private val questionService: QuestionService
) {
    /**
     * 질문 목록 페이지
     */
    @GetMapping
    fun list(
        @RequestParam(required = false) jobField: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) difficulty: String?,
        model: Model
    ): String {
        val questions = questionService.findAll(jobField, category, difficulty)

        model.addAttribute("questions", questions)
        model.addAttribute("selectedJobField", jobField ?: "")
        model.addAttribute("selectedCategory", category ?: "")
        model.addAttribute("selectedDifficulty", difficulty ?: "")

        return "questions/list"
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
