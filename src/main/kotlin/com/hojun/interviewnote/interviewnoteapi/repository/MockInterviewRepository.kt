package com.hojun.interviewnote.interviewnoteapi.repository

import com.hojun.interviewnote.interviewnoteapi.domain.MockInterview
import com.hojun.interviewnote.interviewnoteapi.domain.MockInterviewStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

/**
 * 모의 면접 세션 Repository
 *
 * Phase 7: 실시간 AI 채팅 면접
 */
interface MockInterviewRepository : JpaRepository<MockInterview, Long> {

    /**
     * 사용자 ID와 면접 ID로 조회 (소유권 검증용)
     */
    fun findByIdAndUserId(id: Long, userId: Long): MockInterview?

    /**
     * 사용자의 모든 면접 세션 조회 (최신순)
     */
    fun findByUserIdOrderByStartedAtDesc(userId: Long): List<MockInterview>

    /**
     * 사용자의 특정 상태 면접 세션 조회 (최신순)
     */
    fun findByUserIdAndStatusOrderByStartedAtDesc(
        userId: Long,
        status: MockInterviewStatus
    ): List<MockInterview>

    /**
     * 사용자의 특정 기간 이후 시작된 면접 개수
     * Rate Limiting용 (5회/24시간)
     */
    fun countByUserIdAndStartedAtAfter(
        userId: Long,
        startedAt: LocalDateTime
    ): Long
}
