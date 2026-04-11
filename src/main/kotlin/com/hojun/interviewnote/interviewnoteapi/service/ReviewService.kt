package com.hojun.interviewnote.interviewnoteapi.service

import com.hojun.interviewnote.interviewnoteapi.dto.ReviewSummaryDto
import com.hojun.interviewnote.interviewnoteapi.repository.AiFeedbackRepository
import com.hojun.interviewnote.interviewnoteapi.repository.InterviewAnswerRepository
import com.hojun.interviewnote.interviewnoteapi.repository.QuestionRepository
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
     * 복기 이력 목록 조회
     */
    fun getReviewList(): List<ReviewSummaryDto> {
        val answers = interviewAnswerRepository.findAllByOrderByCreatedAtDesc()

        return answers.mapNotNull { answer ->
            val question = questionRepository.findById(answer.questionId).orElse(null) ?: return@mapNotNull null
            val feedback = aiFeedbackRepository.findByInterviewAnswerId(answer.id) ?: return@mapNotNull null

            val averageScore = (feedback.logicScore + feedback.specificityScore +
                    feedback.jobFitScore + feedback.deliveryScore) / 4.0

            ReviewSummaryDto(
                answerId = answer.id,
                questionContent = question.content,
                category = question.category,
                answeredAt = answer.createdAt,
                averageScore = averageScore
            )
        }
    }
}
