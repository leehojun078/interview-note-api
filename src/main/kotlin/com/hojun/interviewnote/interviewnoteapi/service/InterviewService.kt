package com.hojun.interviewnote.interviewnoteapi.service

import com.hojun.interviewnote.interviewnoteapi.domain.InterviewAnswer
import com.hojun.interviewnote.interviewnoteapi.dto.AnswerSubmitDto
import com.hojun.interviewnote.interviewnoteapi.dto.AnswerWithFeedbackDto
import com.hojun.interviewnote.interviewnoteapi.dto.FeedbackDto
import com.hojun.interviewnote.interviewnoteapi.exception.AnswerNotFoundException
import com.hojun.interviewnote.interviewnoteapi.exception.FeedbackNotFoundException
import com.hojun.interviewnote.interviewnoteapi.repository.InterviewAnswerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class InterviewService(
    private val interviewAnswerRepository: InterviewAnswerRepository,
    private val questionService: QuestionService,
    private val aiFeedbackService: AiFeedbackService
) {
    /**
     * 답변 제출 및 AI 평가
     * Phase 4A-2에서 수정: userId 파라미터 추가
     */
    fun submitAnswer(dto: AnswerSubmitDto, userId: Long): AnswerWithFeedbackDto {
        val questionId = dto.questionId
            ?: throw IllegalArgumentException("질문 ID는 필수입니다")
        val answerText = dto.answerText
            ?: throw IllegalArgumentException("답변은 필수입니다")

        // 1. 질문 존재 여부 확인
        val question = questionService.findById(questionId)

        // 2. 답변 저장 (userId 포함)
        val answer = InterviewAnswer(
            questionId = questionId,
            userId = userId,
            answerText = answerText,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val savedAnswer = interviewAnswerRepository.save(answer)

        // 3. AI 피드백 생성 (Phase 2: 실제 AI 또는 fallback)
        val aiFeedback = aiFeedbackService.generateFeedback(savedAnswer, question)

        // 4. 결합된 DTO 반환
        return AnswerWithFeedbackDto(
            answerId = savedAnswer.id,
            questionId = question.id,
            questionContent = question.content,
            answerText = savedAnswer.answerText,
            answeredAt = savedAnswer.createdAt,
            feedback = FeedbackDto.from(aiFeedback)
        )
    }

    /**
     * 답변 상세 조회 (평가 포함)
     */
    fun getAnswerWithFeedback(answerId: Long): AnswerWithFeedbackDto {
        val answer = interviewAnswerRepository.findById(answerId)
            .orElseThrow { AnswerNotFoundException(answerId) }

        val question = questionService.findById(answer.questionId)

        val aiFeedback = aiFeedbackService.findByInterviewAnswerId(answerId)
            ?: throw FeedbackNotFoundException(answerId)

        return AnswerWithFeedbackDto(
            answerId = answer.id,
            questionId = question.id,
            questionContent = question.content,
            answerText = answer.answerText,
            answeredAt = answer.createdAt,
            feedback = FeedbackDto.from(aiFeedback)
        )
    }
}
