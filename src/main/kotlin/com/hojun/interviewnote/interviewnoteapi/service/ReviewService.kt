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
        return answers.mapNotNull { buildReviewSummary(it) }
    }

    /**
     * 특정 사용자의 리뷰 이력 목록 조회
     * Phase 4A-2에서 추가: 사용자별 답변 이력 분리
     * Phase 6E에서 수정: GeneratedQuestion 지원
     * 리팩토링 Week 3: N+1 쿼리 최적화 (배치 조회)
     */
    fun getUserReviews(userId: Long): List<ReviewSummaryDto> {
        val answers = interviewAnswerRepository.findByUserIdOrderByCreatedAtDesc(userId)
        return buildReviewSummariesBatch(answers)
    }

    /**
     * 특정 사용자의 리뷰 이력을 페이지네이션하여 조회
     * Phase 2 (UI/UX): 리뷰 페이지네이션
     * Phase 6E에서 수정: GeneratedQuestion 지원
     * 리팩토링 Week 3: N+1 쿼리 최적화 (배치 조회)
     */
    fun getUserReviewsPage(userId: Long, pageable: Pageable): Page<ReviewSummaryDto> {
        val answersPage = interviewAnswerRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
        val reviewDtos = buildReviewSummariesBatch(answersPage.content)
        return PageImpl(reviewDtos, pageable, answersPage.totalElements)
    }

    /**
     * 질문 정보 조회 (공통 메서드)
     *
     * InterviewAnswer에서 questionId 또는 generatedQuestionId를 사용하여
     * 질문 내용과 카테고리를 조회합니다.
     *
     * @param answer 답변 엔티티
     * @return Pair(질문 내용, 카테고리) 또는 null
     */
    private fun resolveQuestion(answer: com.hojun.interviewnote.interviewnoteapi.domain.InterviewAnswer): Pair<String, String>? {
        return when {
            answer.questionId != null -> {
                questionRepository.findById(answer.questionId).orElse(null)
                    ?.let { it.content to it.category }
            }
            answer.generatedQuestionId != null -> {
                generatedQuestionRepository.findById(answer.generatedQuestionId).orElse(null)
                    ?.let { it.content to it.category }
            }
            else -> null
        }
    }

    /**
     * ReviewSummaryDto 생성 (공통 메서드)
     *
     * @param answer 답변 엔티티
     * @return ReviewSummaryDto 또는 null (질문 또는 피드백이 없는 경우)
     */
    private fun buildReviewSummary(answer: com.hojun.interviewnote.interviewnoteapi.domain.InterviewAnswer): ReviewSummaryDto? {
        val (questionContent, category) = resolveQuestion(answer) ?: return null
        val feedback = aiFeedbackRepository.findByInterviewAnswerId(answer.id) ?: return null

        return ReviewSummaryDto(
            answerId = answer.id,
            questionContent = questionContent,
            category = category,
            answeredAt = answer.createdAt,
            averageScore = feedback.averageScore
        )
    }

    /**
     * 배치 조회 방식으로 ReviewSummaryDto 목록 생성 (N+1 최적화)
     *
     * 리팩토링 Week 3: N+1 쿼리 방지를 위한 배치 조회
     * - 질문, 생성된 질문, 피드백을 한 번에 조회하여 메모리에서 조합
     * - 데이터베이스 쿼리 횟수: O(n) → O(1)
     *
     * @param answers 답변 엔티티 목록
     * @return ReviewSummaryDto 목록
     */
    private fun buildReviewSummariesBatch(answers: List<com.hojun.interviewnote.interviewnoteapi.domain.InterviewAnswer>): List<ReviewSummaryDto> {
        if (answers.isEmpty()) return emptyList()

        // 1. 배치 조회
        val questionIds = answers.mapNotNull { it.questionId }
        val questions = if (questionIds.isNotEmpty()) {
            questionRepository.findAllById(questionIds).associateBy { it.id }
        } else {
            emptyMap()
        }

        val generatedQuestionIds = answers.mapNotNull { it.generatedQuestionId }
        val generatedQuestions = if (generatedQuestionIds.isNotEmpty()) {
            generatedQuestionRepository.findAllById(generatedQuestionIds).associateBy { it.id }
        } else {
            emptyMap()
        }

        val answerIds = answers.map { it.id }
        val feedbacks = aiFeedbackRepository.findAllByInterviewAnswerIdIn(answerIds)
            .associateBy { it.interviewAnswerId }

        // 2. 메모리에서 조합
        return answers.mapNotNull { answer ->
            val (content, category) = when {
                answer.questionId != null -> {
                    questions[answer.questionId]?.let { it.content to it.category }
                }
                answer.generatedQuestionId != null -> {
                    generatedQuestions[answer.generatedQuestionId]?.let { it.content to it.category }
                }
                else -> null
            } ?: return@mapNotNull null

            val feedback = feedbacks[answer.id] ?: return@mapNotNull null

            ReviewSummaryDto(
                answerId = answer.id,
                questionContent = content,
                category = category,
                answeredAt = answer.createdAt,
                averageScore = feedback.averageScore
            )
        }
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
