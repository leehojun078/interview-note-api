package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.dto.QuestionDto
import com.hojun.interviewnote.interviewnoteapi.service.QuestionService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(QuestionController::class)
@Import(com.hojun.interviewnote.interviewnoteapi.config.SecurityConfig::class)
class QuestionControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var questionService: QuestionService

    private fun createQuestionDto(
        id: Long = 1L,
        category: String = "기술역량",
        difficulty: String = "MEDIUM"
    ): QuestionDto {
        return QuestionDto(
            id = id,
            jobField = "IT",
            targetJob = "백엔드 개발자",
            category = category,
            content = "테스트 질문 $id",
            difficulty = difficulty
        )
    }

    @Test
    @WithMockUser
    fun`필터 없이 질문 목록을 조회한다`() {
        // given
        val questions = listOf(createQuestionDto(1L), createQuestionDto(2L))
        whenever(questionService.findAll(null, null)).thenReturn(questions)

        // when & then
        mockMvc.perform(get("/questions"))
            .andExpect(status().isOk)
            .andExpect(view().name("questions/list"))
            .andExpect(model().attributeExists("questions"))
            .andExpect(model().attribute("questions", questions))
            .andExpect(model().attribute("selectedCategory", ""))
            .andExpect(model().attribute("selectedDifficulty", ""))
    }

    @Test
    @WithMockUser
    fun`카테고리 필터로 질문을 조회한다`() {
        // given
        val questions = listOf(createQuestionDto(category = "기술역량"))
        whenever(questionService.findAll("기술역량", null)).thenReturn(questions)

        // when & then
        mockMvc.perform(get("/questions").param("category", "기술역량"))
            .andExpect(status().isOk)
            .andExpect(view().name("questions/list"))
            .andExpect(model().attribute("selectedCategory", "기술역량"))
            .andExpect(model().attribute("selectedDifficulty", ""))
    }

    @Test
    @WithMockUser
    fun`난이도 필터로 질문을 조회한다`() {
        // given
        val questions = listOf(createQuestionDto(difficulty = "EASY"))
        whenever(questionService.findAll(null, "EASY")).thenReturn(questions)

        // when & then
        mockMvc.perform(get("/questions").param("difficulty", "EASY"))
            .andExpect(status().isOk)
            .andExpect(view().name("questions/list"))
            .andExpect(model().attribute("selectedCategory", ""))
            .andExpect(model().attribute("selectedDifficulty", "EASY"))
    }

    @Test
    @WithMockUser
    fun`카테고리와 난이도로 복합 필터링한다`() {
        // given
        val questions = listOf(createQuestionDto(category = "문제해결", difficulty = "HARD"))
        whenever(questionService.findAll("문제해결", "HARD")).thenReturn(questions)

        // when & then
        mockMvc.perform(
            get("/questions")
                .param("category", "문제해결")
                .param("difficulty", "HARD")
        )
            .andExpect(status().isOk)
            .andExpect(view().name("questions/list"))
            .andExpect(model().attribute("selectedCategory", "문제해결"))
            .andExpect(model().attribute("selectedDifficulty", "HARD"))
    }

    @Test
    @WithMockUser
    fun`질문 상세 및 답변 작성 폼을 렌더링한다`() {
        // given
        val question = createQuestionDto(1L)
        whenever(questionService.findDtoById(1L)).thenReturn(question)

        // when & then
        mockMvc.perform(get("/questions/1/answer"))
            .andExpect(status().isOk)
            .andExpect(view().name("questions/answer"))
            .andExpect(model().attributeExists("question"))
            .andExpect(model().attribute("question", question))
    }
}
