package com.hojun.interviewnote.interviewnoteapi.service

import com.hojun.interviewnote.interviewnoteapi.domain.AiFeedback
import com.hojun.interviewnote.interviewnoteapi.domain.InterviewAnswer
import com.hojun.interviewnote.interviewnoteapi.domain.Question
import com.hojun.interviewnote.interviewnoteapi.dto.AnswerSubmitDto
import com.hojun.interviewnote.interviewnoteapi.exception.AnswerNotFoundException
import com.hojun.interviewnote.interviewnoteapi.exception.FeedbackNotFoundException
import com.hojun.interviewnote.interviewnoteapi.repository.GeneratedQuestionRepository
import com.hojun.interviewnote.interviewnoteapi.repository.InterviewAnswerRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class InterviewServiceTest {

    @Mock
    private lateinit var interviewAnswerRepository: InterviewAnswerRepository

    @Mock
    private lateinit var questionService: QuestionService

    @Mock
    private lateinit var aiFeedbackService: AiFeedbackService

    @Mock
    private lateinit var generatedQuestionRepository: GeneratedQuestionRepository

    @InjectMocks
    private lateinit var interviewService: InterviewService

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

    private fun createAnswer(): InterviewAnswer {
        return InterviewAnswer(
            id = 1L,
            questionId = 1L,
            userId = 1L,
            answerText = "테스트 답변",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    private fun createFeedback(): AiFeedback {
        return AiFeedback(
            id = 1L,
            interviewAnswerId = 1L,
            logicScore = 4,
            specificityScore = 3,
            jobFitScore = 4,
            deliveryScore = 3,
            strengths = "[\"강점1\", \"강점2\"]",
            improvements = "[\"개선점1\", \"개선점2\"]",
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
    }

    // submitAnswer의 전체 플로우 테스트는 Integration Test에서 수행
    // Unit Test에서는 FeedbackDto.from()이 ObjectMapperConfig를 사용하므로 제한적

    @Test
    fun `questionId가 null이면 IllegalArgumentException을 발생시킨다`() {
        // given
        val dto = AnswerSubmitDto(
            questionId = null,
            answerText = "테스트 답변"
        )

        // when & then
        val exception = assertThrows<IllegalArgumentException> {
            interviewService.submitAnswer(dto, 1L)
        }
        assertThat(exception.message).isEqualTo("질문 ID는 필수입니다")
    }

    @Test
    fun `answerText가 null이면 IllegalArgumentException을 발생시킨다`() {
        // given
        val dto = AnswerSubmitDto(
            questionId = 1L,
            answerText = null
        )

        // when & then
        val exception = assertThrows<IllegalArgumentException> {
            interviewService.submitAnswer(dto, 1L)
        }
        assertThat(exception.message).isEqualTo("답변은 필수입니다")
    }

    @Test
    fun `답변과 피드백을 조회한다`() {
        // given
        val answer = createAnswer()
        val question = createQuestion()
        val feedback = createFeedback()

        whenever(interviewAnswerRepository.findById(1L)).thenReturn(Optional.of(answer))
        whenever(questionService.findById(1L)).thenReturn(question)
        whenever(aiFeedbackService.findByInterviewAnswerId(1L)).thenReturn(feedback)

        // when
        val result = interviewService.getAnswerWithFeedback(1L)

        // then
        assertThat(result.answerId).isEqualTo(1L)
        assertThat(result.questionContent).isEqualTo("테스트 질문")
        assertThat(result.feedback.logicScore).isEqualTo(4)
    }

    @Test
    fun `존재하지 않는 답변 조회시 AnswerNotFoundException을 발생시킨다`() {
        // given
        whenever(interviewAnswerRepository.findById(999L)).thenReturn(Optional.empty())

        // when & then
        assertThrows<AnswerNotFoundException> {
            interviewService.getAnswerWithFeedback(999L)
        }
    }

    @Test
    fun `피드백이 없으면 FeedbackNotFoundException을 발생시킨다`() {
        // given
        val answer = createAnswer()

        whenever(interviewAnswerRepository.findById(1L)).thenReturn(Optional.of(answer))
        whenever(aiFeedbackService.findByInterviewAnswerId(1L)).thenReturn(null)

        // when & then
        assertThrows<FeedbackNotFoundException> {
            interviewService.getAnswerWithFeedback(1L)
        }
    }
}
