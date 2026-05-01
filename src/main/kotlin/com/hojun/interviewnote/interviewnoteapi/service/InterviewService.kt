package com.hojun.interviewnote.interviewnoteapi.service

import com.hojun.interviewnote.interviewnoteapi.domain.InterviewAnswer
import com.hojun.interviewnote.interviewnoteapi.dto.AnswerSubmitDto
import com.hojun.interviewnote.interviewnoteapi.dto.AnswerWithFeedbackDto
import com.hojun.interviewnote.interviewnoteapi.dto.FeedbackDto
import com.hojun.interviewnote.interviewnoteapi.exception.AnswerNotFoundException
import com.hojun.interviewnote.interviewnoteapi.exception.FeedbackNotFoundException
import com.hojun.interviewnote.interviewnoteapi.repository.GeneratedQuestionRepository
import com.hojun.interviewnote.interviewnoteapi.repository.InterviewAnswerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class InterviewService(
    private val interviewAnswerRepository: InterviewAnswerRepository,
    private val questionService: QuestionService,
    private val aiFeedbackService: AiFeedbackService,
    private val generatedQuestionRepository: GeneratedQuestionRepository
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
            feedback = FeedbackDto.from(aiFeedback),
            generatedQuestionId = null,  // 일반 질문이므로 null
            isGenerated = false  // 일반 질문
        )
    }

    /**
     * 생성된 질문에 대한 답변 제출 및 AI 평가
     * Phase 6D에서 추가됨: GeneratedQuestion 지원
     */
    fun submitAnswerForGeneratedQuestion(
        generatedQuestionId: Long,
        dto: AnswerSubmitDto,
        userId: Long
    ): AnswerWithFeedbackDto {
        val answerText = dto.answerText
            ?: throw IllegalArgumentException("답변은 필수입니다")

        // 1. 생성된 질문 존재 여부 확인
        val generatedQuestion = generatedQuestionRepository.findById(generatedQuestionId)
            .orElseThrow { IllegalArgumentException("생성된 질문을 찾을 수 없습니다: ID=$generatedQuestionId") }

        // 2. 답변 저장 (generatedQuestionId 포함, questionId는 null)
        val answer = InterviewAnswer(
            questionId = null,  // 생성된 질문의 경우 questionId는 null
            userId = userId,
            answerText = answerText,
            generatedQuestionId = generatedQuestionId,  // 핵심: 생성된 질문 ID 저장
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val savedAnswer = interviewAnswerRepository.save(answer)

        // 3. AI 피드백 생성 (GeneratedQuestion을 Question처럼 변환)
        val questionForFeedback = com.hojun.interviewnote.interviewnoteapi.domain.Question(
            id = generatedQuestion.id,
            jobField = "IT",  // 임시값, AI 피드백에서 실제로는 사용 안 함
            targetJob = "개발자",
            category = generatedQuestion.category,
            content = generatedQuestion.content,
            difficulty = generatedQuestion.difficulty,
            isActive = true
        )
        val aiFeedback = aiFeedbackService.generateFeedback(savedAnswer, questionForFeedback)

        // 4. 결합된 DTO 반환
        return AnswerWithFeedbackDto(
            answerId = savedAnswer.id,
            questionId = 0,  // 생성된 질문의 경우 0
            questionContent = generatedQuestion.content,
            answerText = savedAnswer.answerText,
            answeredAt = savedAnswer.createdAt,
            feedback = FeedbackDto.from(aiFeedback),
            generatedQuestionId = generatedQuestionId,  // 생성된 질문 ID
            isGenerated = true  // 생성된 질문
        )
    }

    /**
     * 답변 상세 조회 (평가 포함)
     * Phase 6D에서 수정: GeneratedQuestion 지원
     */
    fun getAnswerWithFeedback(answerId: Long): AnswerWithFeedbackDto {
        val answer = interviewAnswerRepository.findById(answerId)
            .orElseThrow { AnswerNotFoundException(answerId) }

        val aiFeedback = aiFeedbackService.findByInterviewAnswerId(answerId)
            ?: throw FeedbackNotFoundException(answerId)

        // GeneratedQuestion인지 일반 Question인지 확인 (Phase 6E: questionId nullable 대응)
        val (questionId, questionContent, generatedQuestionId, isGenerated) = when {
            answer.generatedQuestionId != null -> {
                // 생성된 질문
                val genQuestionId = answer.generatedQuestionId  // Smart cast
                val generatedQuestion = generatedQuestionRepository.findById(genQuestionId)
                    .orElseThrow { IllegalStateException("생성된 질문을 찾을 수 없습니다: ID=$genQuestionId") }
                QuestionInfo(0L, generatedQuestion.content, genQuestionId, true)
            }
            answer.questionId != null -> {
                // 일반 질문
                val question = questionService.findById(answer.questionId)
                QuestionInfo(question.id, question.content, null, false)
            }
            else -> {
                throw IllegalStateException("답변에 questionId와 generatedQuestionId가 모두 null입니다: answerId=${answer.id}")
            }
        }

        return AnswerWithFeedbackDto(
            answerId = answer.id,
            questionId = questionId,
            questionContent = questionContent,
            answerText = answer.answerText,
            answeredAt = answer.createdAt,
            feedback = FeedbackDto.from(aiFeedback),
            generatedQuestionId = generatedQuestionId,
            isGenerated = isGenerated
        )
    }

    // Helper data class for destructuring
    private data class QuestionInfo(
        val questionId: Long,
        val questionContent: String,
        val generatedQuestionId: Long?,
        val isGenerated: Boolean
    )
}
