package com.hojun.interviewnote.interviewnoteapi.service

import com.hojun.interviewnote.interviewnoteapi.domain.AiFeedback
import com.hojun.interviewnote.interviewnoteapi.domain.InterviewAnswer
import com.hojun.interviewnote.interviewnoteapi.domain.Question
import com.hojun.interviewnote.interviewnoteapi.repository.AiFeedbackRepository
import com.hojun.interviewnote.interviewnoteapi.repository.GeneratedQuestionRepository
import com.hojun.interviewnote.interviewnoteapi.repository.InterviewAnswerRepository
import com.hojun.interviewnote.interviewnoteapi.repository.InterviewMessageRepository
import com.hojun.interviewnote.interviewnoteapi.repository.JobPostingRepository
import com.hojun.interviewnote.interviewnoteapi.repository.MockInterviewRepository
import com.hojun.interviewnote.interviewnoteapi.repository.QuestionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class ReviewServiceTest {

    @Mock
    private lateinit var interviewAnswerRepository: InterviewAnswerRepository

    @Mock
    private lateinit var questionRepository: QuestionRepository

    @Mock
    private lateinit var generatedQuestionRepository: GeneratedQuestionRepository

    @Mock
    private lateinit var aiFeedbackRepository: AiFeedbackRepository

    // Phase 8C: 추가된 의존성들
    @Mock
    private lateinit var mockInterviewRepository: MockInterviewRepository

    @Mock
    private lateinit var interviewMessageRepository: InterviewMessageRepository

    @Mock
    private lateinit var jobPostingRepository: JobPostingRepository

    @InjectMocks
    private lateinit var reviewService: ReviewService

    private fun createAnswer(id: Long, questionId: Long): InterviewAnswer {
        return InterviewAnswer(
            id = id,
            questionId = questionId,
            userId = 1L,
            answerText = "테스트 답변 $id",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    private fun createQuestion(id: Long): Question {
        return Question(
            id = id,
            jobField = "IT",
            targetJob = "백엔드 개발자",
            category = "기술역량",
            content = "테스트 질문 $id",
            difficulty = "MEDIUM",
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    private fun createFeedback(answerId: Long): AiFeedback {
        return AiFeedback(
            id = answerId,
            interviewAnswerId = answerId,
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
    }

    @Test
    fun `리뷰 이력 목록을 조회한다`() {
        // given
        val answer1 = createAnswer(1L, 1L)
        val answer2 = createAnswer(2L, 2L)
        val question1 = createQuestion(1L)
        val question2 = createQuestion(2L)
        val feedback1 = createFeedback(1L)
        val feedback2 = createFeedback(2L)

        whenever(interviewAnswerRepository.findAllByOrderByCreatedAtDesc())
            .thenReturn(listOf(answer1, answer2))
        whenever(questionRepository.findById(1L)).thenReturn(Optional.of(question1))
        whenever(questionRepository.findById(2L)).thenReturn(Optional.of(question2))
        whenever(aiFeedbackRepository.findByInterviewAnswerId(1L)).thenReturn(feedback1)
        whenever(aiFeedbackRepository.findByInterviewAnswerId(2L)).thenReturn(feedback2)

        // when
        val result = reviewService.getReviewList()

        // then
        assertThat(result).hasSize(2)
        assertThat(result[0].answerId).isEqualTo(1L)
        assertThat(result[1].answerId).isEqualTo(2L)
    }

    @Test
    fun `Question이 없는 답변은 결과에서 제외된다`() {
        // given
        val answer = createAnswer(1L, 999L)

        whenever(interviewAnswerRepository.findAllByOrderByCreatedAtDesc())
            .thenReturn(listOf(answer))
        whenever(questionRepository.findById(999L)).thenReturn(Optional.empty())
        // Question이 없으면 feedback 조회 전에 필터링되므로 stubbing 불필요

        // when
        val result = reviewService.getReviewList()

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `Feedback이 없는 답변은 결과에서 제외된다`() {
        // given
        val answer = createAnswer(1L, 1L)
        val question = createQuestion(1L)

        whenever(interviewAnswerRepository.findAllByOrderByCreatedAtDesc())
            .thenReturn(listOf(answer))
        whenever(questionRepository.findById(1L)).thenReturn(Optional.of(question))
        whenever(aiFeedbackRepository.findByInterviewAnswerId(1L)).thenReturn(null)

        // when
        val result = reviewService.getReviewList()

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `averageScore가 정확히 계산된다`() {
        // given
        val answer = createAnswer(1L, 1L)
        val question = createQuestion(1L)
        val feedback = createFeedback(1L)

        whenever(interviewAnswerRepository.findAllByOrderByCreatedAtDesc())
            .thenReturn(listOf(answer))
        whenever(questionRepository.findById(1L)).thenReturn(Optional.of(question))
        whenever(aiFeedbackRepository.findByInterviewAnswerId(1L)).thenReturn(feedback)

        // when
        val result = reviewService.getReviewList()

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].averageScore).isEqualTo(3.5) // (4+3+4+3)/4
    }
}
