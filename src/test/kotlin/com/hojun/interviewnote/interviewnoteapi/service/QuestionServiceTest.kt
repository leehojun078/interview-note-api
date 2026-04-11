package com.hojun.interviewnote.interviewnoteapi.service

import com.hojun.interviewnote.interviewnoteapi.domain.Question
import com.hojun.interviewnote.interviewnoteapi.exception.QuestionNotFoundException
import com.hojun.interviewnote.interviewnoteapi.repository.QuestionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class QuestionServiceTest {

    @Mock
    private lateinit var questionRepository: QuestionRepository

    @InjectMocks
    private lateinit var questionService: QuestionService

    private fun createQuestion(
        id: Long = 1L,
        category: String = "기술역량",
        difficulty: String = "MEDIUM"
    ): Question {
        return Question(
            id = id,
            jobField = "IT",
            targetJob = "백엔드 개발자",
            category = category,
            content = "테스트 질문",
            difficulty = difficulty,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    @Test
    fun `필터 없이 전체 활성화된 질문을 조회한다`() {
        // given
        val questions = listOf(createQuestion(1L), createQuestion(2L))
        whenever(questionRepository.findByIsActiveTrue()).thenReturn(questions)

        // when
        val result = questionService.findAll(null, null)

        // then
        assertThat(result).hasSize(2)
    }

    @Test
    fun `카테고리로 필터링하여 질문을 조회한다`() {
        // given
        val questions = listOf(createQuestion(category = "기술역량"))
        whenever(questionRepository.findByCategoryAndIsActiveTrue("기술역량")).thenReturn(questions)

        // when
        val result = questionService.findAll("기술역량", null)

        // then
        assertThat(result).hasSize(1)
        assertThat(result.first().category).isEqualTo("기술역량")
    }

    @Test
    fun `난이도로 필터링하여 질문을 조회한다`() {
        // given
        val questions = listOf(createQuestion(difficulty = "HARD"))
        whenever(questionRepository.findByDifficultyAndIsActiveTrue("HARD")).thenReturn(questions)

        // when
        val result = questionService.findAll(null, "HARD")

        // then
        assertThat(result).hasSize(1)
        assertThat(result.first().difficulty).isEqualTo("HARD")
    }

    @Test
    fun `카테고리와 난이도로 복합 필터링한다`() {
        // given
        val questions = listOf(createQuestion(category = "문제해결", difficulty = "EASY"))
        whenever(questionRepository.findByCategoryAndDifficultyAndIsActiveTrue("문제해결", "EASY"))
            .thenReturn(questions)

        // when
        val result = questionService.findAll("문제해결", "EASY")

        // then
        assertThat(result).hasSize(1)
        assertThat(result.first().category).isEqualTo("문제해결")
        assertThat(result.first().difficulty).isEqualTo("EASY")
    }

    @Test
    fun `ID로 질문을 조회한다`() {
        // given
        val question = createQuestion(1L)
        whenever(questionRepository.findById(1L)).thenReturn(Optional.of(question))

        // when
        val result = questionService.findById(1L)

        // then
        assertThat(result.id).isEqualTo(1L)
    }

    @Test
    fun `존재하지 않는 ID로 조회시 QuestionNotFoundException을 발생시킨다`() {
        // given
        whenever(questionRepository.findById(999L)).thenReturn(Optional.empty())

        // when & then
        assertThrows<QuestionNotFoundException> {
            questionService.findById(999L)
        }
    }

    @Test
    fun `ID로 질문을 DTO로 조회한다`() {
        // given
        val question = createQuestion(1L)
        whenever(questionRepository.findById(1L)).thenReturn(Optional.of(question))

        // when
        val result = questionService.findDtoById(1L)

        // then
        assertThat(result.id).isEqualTo(1L)
        assertThat(result.content).isEqualTo("테스트 질문")
    }
}
