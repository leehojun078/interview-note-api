package com.hojun.interviewnote.interviewnoteapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.hojun.interviewnote.interviewnoteapi.config.OpenAiProperties
import com.hojun.interviewnote.interviewnoteapi.domain.AiFeedback
import com.hojun.interviewnote.interviewnoteapi.domain.InterviewAnswer
import com.hojun.interviewnote.interviewnoteapi.domain.Question
import com.hojun.interviewnote.interviewnoteapi.repository.AiFeedbackRepository
import com.hojun.interviewnote.interviewnoteapi.service.ai.AiClient
import com.hojun.interviewnote.interviewnoteapi.service.ai.PromptBuilder
import com.hojun.interviewnote.interviewnoteapi.service.ai.ResponseParser
import com.hojun.interviewnote.interviewnoteapi.service.cache.DuplicateRequestCache
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class AiFeedbackServiceTest {

    @Mock
    private lateinit var aiFeedbackRepository: AiFeedbackRepository

    @Mock
    private lateinit var objectMapper: ObjectMapper

    @Mock
    private lateinit var aiClient: AiClient

    @Mock
    private lateinit var promptBuilder: PromptBuilder

    @Mock
    private lateinit var responseParser: ResponseParser

    @Mock
    private lateinit var openAiProperties: OpenAiProperties

    @Mock
    private lateinit var duplicateRequestCache: DuplicateRequestCache

    @InjectMocks
    private lateinit var aiFeedbackService: AiFeedbackService

    private fun createAnswer(answerText: String): InterviewAnswer {
        return InterviewAnswer(
            id = 1L,
            questionId = 1L,
            answerText = answerText,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    private fun createQuestion(): Question {
        return Question(
            id = 1L,
            jobField = "IT",
            targetJob = "백엔드 개발자",
            category = "기술역량",
            content = "테스트 질문",
            difficulty = "MEDIUM",
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    @Test
    fun `답변 길이가 500자 이상이면 높은 점수를 부여한다`() {
        // given
        val longAnswer = "a".repeat(600)
        val answer = createAnswer(longAnswer)
        val question = createQuestion()

        whenever(objectMapper.writeValueAsString(any())).thenReturn("[]")
        doAnswer { it.arguments[0] }.whenever(aiFeedbackRepository).save(any())

        // when
        val result = aiFeedbackService.generateDummyFeedback(answer, question)

        // then
        assertThat(result.logicScore).isEqualTo(4) // DUMMY_SCORE_HIGH
    }

    @Test
    fun `답변 길이가 300자 이상 500자 미만이면 중간 점수를 부여한다`() {
        // given
        val mediumAnswer = "a".repeat(400)
        val answer = createAnswer(mediumAnswer)
        val question = createQuestion()

        whenever(objectMapper.writeValueAsString(any())).thenReturn("[]")
        doAnswer { it.arguments[0] }.whenever(aiFeedbackRepository).save(any())

        // when
        val result = aiFeedbackService.generateDummyFeedback(answer, question)

        // then
        assertThat(result.logicScore).isEqualTo(3) // DUMMY_SCORE_MEDIUM
    }

    @Test
    fun `답변 길이가 300자 미만이면 낮은 점수를 부여한다`() {
        // given
        val shortAnswer = "a".repeat(100)
        val answer = createAnswer(shortAnswer)
        val question = createQuestion()

        whenever(objectMapper.writeValueAsString(any())).thenReturn("[]")
        doAnswer { it.arguments[0] }.whenever(aiFeedbackRepository).save(any())

        // when
        val result = aiFeedbackService.generateDummyFeedback(answer, question)

        // then
        assertThat(result.logicScore).isEqualTo(2) // DUMMY_SCORE_LOW
    }

    @Test
    fun `피드백을 JSON으로 직렬화하여 저장한다`() {
        // given
        val answer = createAnswer("테스트 답변")
        val question = createQuestion()

        whenever(objectMapper.writeValueAsString(any()))
            .thenReturn("[\"강점1\", \"강점2\"]")
        doAnswer { it.arguments[0] }.whenever(aiFeedbackRepository).save(any())

        // when
        val result = aiFeedbackService.generateDummyFeedback(answer, question)

        // then
        assertThat(result.strengths).isEqualTo("[\"강점1\", \"강점2\"]")
        assertThat(result.improvements).isEqualTo("[\"강점1\", \"강점2\"]")
    }

    @Test
    fun `답변ID로 피드백을 조회한다`() {
        // given
        val feedback = AiFeedback(
            id = 1L,
            interviewAnswerId = 1L,
            logicScore = 4,
            specificityScore = 3,
            jobFitScore = 4,
            deliveryScore = 3,
            strengths = "[]",
            improvements = "[]",
            modelAnswer = "모범답변",
            overallComment = "좋습니다",
            jobField = "IT",
            modelName = "test",
            promptVersion = "v1.0",
            tokenUsageInput = 100,
            tokenUsageOutput = 200,
            rawResponse = "{}",
            createdAt = LocalDateTime.now()
        )
        whenever(aiFeedbackRepository.findByInterviewAnswerId(1L)).thenReturn(feedback)

        // when
        val result = aiFeedbackService.findByInterviewAnswerId(1L)

        // then
        assertThat(result).isNotNull
        assertThat(result?.interviewAnswerId).isEqualTo(1L)
    }
}
