package com.hojun.interviewnote.interviewnoteapi.repository

import com.hojun.interviewnote.interviewnoteapi.domain.InterviewAnswer
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InterviewAnswerRepository : JpaRepository<InterviewAnswer, Long> {
    fun findByQuestionId(questionId: Long): List<InterviewAnswer>
    fun findAllByOrderByCreatedAtDesc(): List<InterviewAnswer>

    /**
     * 특정 사용자의 답변을 생성일시 내림차순으로 조회
     * Phase 4A-2에서 추가: 사용자별 답변 이력 분리
     */
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<InterviewAnswer>

    /**
     * 특정 사용자의 답변을 페이지네이션하여 조회
     * Phase 2 (UI/UX): 리뷰 페이지네이션
     */
    fun findByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<InterviewAnswer>

    /**
     * 중복 답변 검색 (정적 질문용)
     * Phase 8D: 동일한 사용자가 동일한 정적 질문에 동일한 답변을 제출했는지 확인
     */
    fun findFirstByUserIdAndQuestionIdAndAnswerTextHashOrderByCreatedAtDesc(
        userId: Long,
        questionId: Long,
        answerTextHash: String
    ): InterviewAnswer?

    /**
     * 중복 답변 검색 (AI 생성 질문용)
     * Phase 8D: 동일한 사용자가 동일한 AI 생성 질문에 동일한 답변을 제출했는지 확인
     */
    fun findFirstByUserIdAndGeneratedQuestionIdAndAnswerTextHashOrderByCreatedAtDesc(
        userId: Long,
        generatedQuestionId: Long,
        answerTextHash: String
    ): InterviewAnswer?
}
