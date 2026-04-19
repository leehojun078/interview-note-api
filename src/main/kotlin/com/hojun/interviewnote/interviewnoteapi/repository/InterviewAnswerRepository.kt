package com.hojun.interviewnote.interviewnoteapi.repository

import com.hojun.interviewnote.interviewnoteapi.domain.InterviewAnswer
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
}
