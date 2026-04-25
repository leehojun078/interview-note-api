package com.hojun.interviewnote.interviewnoteapi.service

import com.hojun.interviewnote.interviewnoteapi.dto.ReviewSummaryDto
import com.hojun.interviewnote.interviewnoteapi.repository.AiFeedbackRepository
import com.hojun.interviewnote.interviewnoteapi.repository.InterviewAnswerRepository
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
    private val aiFeedbackRepository: AiFeedbackRepository
) {
    /**
     * 리뷰 이력 목록 조회 (모든 사용자)
     * @deprecated Phase 4A-2에서 getUserReviews()로 대체됨
     */
    @Deprecated("사용자별 조회를 사용하세요", ReplaceWith("getUserReviews(userId)"))
    fun getReviewList(): List<ReviewSummaryDto> {
        val answers = interviewAnswerRepository.findAllByOrderByCreatedAtDesc()

        return answers.mapNotNull { answer ->
            val question = questionRepository.findById(answer.questionId).orElse(null)
            val feedback = aiFeedbackRepository.findByInterviewAnswerId(answer.id)

            if (question != null && feedback != null) {
                ReviewSummaryDto(
                    answerId = answer.id,
                    questionContent = question.content,
                    category = question.category,
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
     */
    fun getUserReviews(userId: Long): List<ReviewSummaryDto> {
        val answers = interviewAnswerRepository.findByUserIdOrderByCreatedAtDesc(userId)

        return answers.mapNotNull { answer ->
            val question = questionRepository.findById(answer.questionId).orElse(null)
            val feedback = aiFeedbackRepository.findByInterviewAnswerId(answer.id)

            if (question != null && feedback != null) {
                ReviewSummaryDto(
                    answerId = answer.id,
                    questionContent = question.content,
                    category = question.category,
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
     */
    fun getUserReviewsPage(userId: Long, pageable: Pageable): Page<ReviewSummaryDto> {
        val answersPage = interviewAnswerRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)

        val reviewDtos = answersPage.content.mapNotNull { answer ->
            val question = questionRepository.findById(answer.questionId).orElse(null)
            val feedback = aiFeedbackRepository.findByInterviewAnswerId(answer.id)

            if (question != null && feedback != null) {
                ReviewSummaryDto(
                    answerId = answer.id,
                    questionContent = question.content,
                    category = question.category,
                    answeredAt = answer.createdAt,
                    averageScore = feedback.averageScore
                )
            } else {
                null
            }
        }

        return PageImpl(reviewDtos, pageable, answersPage.totalElements)
    }
}
