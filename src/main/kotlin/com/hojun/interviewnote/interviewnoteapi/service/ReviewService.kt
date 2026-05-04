package com.hojun.interviewnote.interviewnoteapi.service

import com.hojun.interviewnote.interviewnoteapi.domain.MessageSender
import com.hojun.interviewnote.interviewnoteapi.domain.MockInterviewStatus
import com.hojun.interviewnote.interviewnoteapi.dto.JobPostingInfoDto
import com.hojun.interviewnote.interviewnoteapi.dto.MockInterviewReviewDto
import com.hojun.interviewnote.interviewnoteapi.dto.ReviewSummaryDto
import com.hojun.interviewnote.interviewnoteapi.repository.AiFeedbackRepository
import com.hojun.interviewnote.interviewnoteapi.repository.GeneratedQuestionRepository
import com.hojun.interviewnote.interviewnoteapi.repository.InterviewAnswerRepository
import com.hojun.interviewnote.interviewnoteapi.repository.InterviewMessageRepository
import com.hojun.interviewnote.interviewnoteapi.repository.JobPostingRepository
import com.hojun.interviewnote.interviewnoteapi.repository.MockInterviewRepository
import com.hojun.interviewnote.interviewnoteapi.repository.QuestionRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ReviewService(
    private val interviewAnswerRepository: InterviewAnswerRepository,
    private val questionRepository: QuestionRepository,
    private val generatedQuestionRepository: GeneratedQuestionRepository,
    private val aiFeedbackRepository: AiFeedbackRepository,
    private val mockInterviewRepository: MockInterviewRepository,
    private val interviewMessageRepository: InterviewMessageRepository,
    private val jobPostingRepository: JobPostingRepository
) {
    /**
     * 리뷰 이력 목록 조회 (모든 사용자)
     * @deprecated Phase 4A-2에서 getUserReviews()로 대체됨
     */
    @Deprecated("사용자별 조회를 사용하세요", ReplaceWith("getUserReviews(userId)"))
    fun getReviewList(): List<ReviewSummaryDto> {
        val answers = interviewAnswerRepository.findAllByOrderByCreatedAtDesc()

        return answers.mapNotNull { answer ->
            // Phase 6E: questionId nullable 대응 + GeneratedQuestion 지원
            val (questionContent, category) = when {
                answer.questionId != null -> {
                    val question = questionRepository.findById(answer.questionId).orElse(null)
                    question?.let { it.content to it.category }
                }
                answer.generatedQuestionId != null -> {
                    val genQuestion = generatedQuestionRepository.findById(answer.generatedQuestionId).orElse(null)
                    genQuestion?.let { it.content to it.category }
                }
                else -> null
            } ?: return@mapNotNull null

            val feedback = aiFeedbackRepository.findByInterviewAnswerId(answer.id)

            if (feedback != null) {
                ReviewSummaryDto(
                    answerId = answer.id,
                    questionContent = questionContent,
                    category = category,
                    answeredAt = answer.createdAt,
                    averageScore = feedback.averageScore
                )
            } else {
                null
            }
        }
    }

    /**
     * 특정 사용자의 리뷰 이력 목록 조회
     * Phase 4A-2에서 추가: 사용자별 답변 이력 분리
     * Phase 6E에서 수정: GeneratedQuestion 지원
     */
    fun getUserReviews(userId: Long): List<ReviewSummaryDto> {
        val answers = interviewAnswerRepository.findByUserIdOrderByCreatedAtDesc(userId)

        return answers.mapNotNull { answer ->
            // Phase 6E: questionId nullable 대응 + GeneratedQuestion 지원
            val (questionContent, category) = when {
                answer.questionId != null -> {
                    val question = questionRepository.findById(answer.questionId).orElse(null)
                    question?.let { it.content to it.category }
                }
                answer.generatedQuestionId != null -> {
                    val genQuestion = generatedQuestionRepository.findById(answer.generatedQuestionId).orElse(null)
                    genQuestion?.let { it.content to it.category }
                }
                else -> null
            } ?: return@mapNotNull null

            val feedback = aiFeedbackRepository.findByInterviewAnswerId(answer.id)

            if (feedback != null) {
                ReviewSummaryDto(
                    answerId = answer.id,
                    questionContent = questionContent,
                    category = category,
                    answeredAt = answer.createdAt,
                    averageScore = feedback.averageScore
                )
            } else {
                null
            }
        }
    }

    /**
     * 특정 사용자의 리뷰 이력을 페이지네이션하여 조회
     * Phase 2 (UI/UX): 리뷰 페이지네이션
     * Phase 6E에서 수정: GeneratedQuestion 지원
     */
    fun getUserReviewsPage(userId: Long, pageable: Pageable): Page<ReviewSummaryDto> {
        val answersPage = interviewAnswerRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)

        val reviewDtos = answersPage.content.mapNotNull { answer ->
            // Phase 6E: questionId nullable 대응 + GeneratedQuestion 지원
            val (questionContent, category) = when {
                answer.questionId != null -> {
                    val question = questionRepository.findById(answer.questionId).orElse(null)
                    question?.let { it.content to it.category }
                }
                answer.generatedQuestionId != null -> {
                    val genQuestion = generatedQuestionRepository.findById(answer.generatedQuestionId).orElse(null)
                    genQuestion?.let { it.content to it.category }
                }
                else -> null
            } ?: return@mapNotNull null

            val feedback = aiFeedbackRepository.findByInterviewAnswerId(answer.id)

            if (feedback != null) {
                ReviewSummaryDto(
                    answerId = answer.id,
                    questionContent = questionContent,
                    category = category,
                    answeredAt = answer.createdAt,
                    averageScore = feedback.averageScore
                )
            } else {
                null
            }
        }

        return PageImpl(reviewDtos, pageable, answersPage.totalElements)
    }

    /**
     * 사용자의 AI 면접 이력 조회
     * Phase 8C: 리뷰 이력 통합
     */
    fun getUserMockInterviewReviews(userId: Long): List<MockInterviewReviewDto> {
        val interviews = mockInterviewRepository
            .findByUserIdAndStatusOrderByStartedAtDesc(userId, MockInterviewStatus.COMPLETED)

        return interviews.map { interview ->
            val messageCount = interviewMessageRepository
                .countByMockInterviewIdAndSender(interview.id, MessageSender.AI)

            val jobPostingInfo = interview.jobPostingId?.let { id ->
                jobPostingRepository.findById(id).orElse(null)?.let {
                    JobPostingInfoDto(
                        companyName = it.companyName,
                        jobTitle = it.jobTitle
                    )
                }
            }

            MockInterviewReviewDto(
                interviewId = interview.id,
                jobField = interview.selectedJobField.displayName,
                careerLevel = interview.careerLevel?.displayName,
                startedAt = interview.startedAt,
                averageScore = interview.weightedAverageScore ?: interview.averageScore,
                messageCount = messageCount,
                jobPostingInfo = jobPostingInfo
            )
        }
    }
}
