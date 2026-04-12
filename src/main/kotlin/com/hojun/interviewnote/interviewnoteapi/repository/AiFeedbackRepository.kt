package com.hojun.interviewnote.interviewnoteapi.repository

import com.hojun.interviewnote.interviewnoteapi.domain.AiFeedback
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface AiFeedbackRepository : JpaRepository<AiFeedback, Long> {
    fun findByInterviewAnswerId(interviewAnswerId: Long): AiFeedback?

    /**
     * Phase 2D: 중복 요청 방지를 위한 캐시 조회
     * 특정 해시와 생성일시 이후의 피드백 조회 (24시간 내 중복 확인)
     */
    fun findByAnswerTextHashAndCreatedAtAfter(
        answerTextHash: String,
        createdAt: LocalDateTime
    ): AiFeedback?
}
