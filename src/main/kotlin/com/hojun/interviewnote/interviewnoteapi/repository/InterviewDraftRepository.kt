package com.hojun.interviewnote.interviewnoteapi.repository

import com.hojun.interviewnote.interviewnoteapi.domain.InterviewDraft
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Phase 3: 답변 Draft Repository
 */
@Repository
interface InterviewDraftRepository : JpaRepository<InterviewDraft, Long> {
    /**
     * 특정 사용자의 특정 질문에 대한 Draft 조회
     */
    fun findByUserIdAndQuestionId(userId: Long, questionId: Long): InterviewDraft?

    /**
     * 특정 사용자의 모든 Draft 조회
     */
    fun findByUserId(userId: Long): List<InterviewDraft>

    /**
     * 특정 사용자의 특정 질문 Draft 삭제
     */
    fun deleteByUserIdAndQuestionId(userId: Long, questionId: Long)

    /**
     * 특정 사용자의 모든 Draft 삭제
     */
    fun deleteByUserId(userId: Long)
}
