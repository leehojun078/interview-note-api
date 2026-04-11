package com.hojun.interviewnote.interviewnoteapi.service

import com.hojun.interviewnote.interviewnoteapi.domain.InterviewAnswer
import com.hojun.interviewnote.interviewnoteapi.dto.AnswerSubmitDto
import com.hojun.interviewnote.interviewnoteapi.dto.AnswerWithFeedbackDto
import com.hojun.interviewnote.interviewnoteapi.dto.FeedbackDto
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
     */
    fun submitAnswer(dto: AnswerSubmitDto): AnswerWithFeedbackDto {
        // 1. 질문 존재 여부 확인
        val question = questionService.findById(dto.questionId!!)

        // 2. 답변 저장
        val answer = InterviewAnswer(
            questionId = dto.questionId,
            answerText = dto.answerText!!,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val savedAnswer = interviewAnswerRepository.save(answer)

        // 3. AI 피드백 생성 (Phase 1: 더미)
        val aiFeedback = aiFeedbackService.generateDummyFeedback(savedAnswer, question)

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
            .orElseThrow { IllegalArgumentException("답변을 찾을 수 없습니다: $answerId") }

        val question = questionService.findById(answer.questionId)

        val aiFeedback = aiFeedbackService.findByInterviewAnswerId(answerId)
            ?: throw IllegalStateException("평가 결과를 찾을 수 없습니다: $answerId")

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
