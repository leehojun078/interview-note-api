package com.hojun.interviewnote.interviewnoteapi.repository

import com.hojun.interviewnote.interviewnoteapi.domain.InterviewMessage
import com.hojun.interviewnote.interviewnoteapi.domain.MessageSender
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 면접 채팅 메시지 Repository
 *
 * Phase 7: 실시간 대화 기록
 */
interface InterviewMessageRepository : JpaRepository<InterviewMessage, Long> {

    /**
     * 면접 세션의 모든 메시지 조회 (시간순)
     * createdAt 오름차순 정렬 (비동기 AI 응답으로 인한 순서 문제 해결)
     */
    fun findByMockInterviewIdOrderByCreatedAtAsc(
        mockInterviewId: Long
    ): List<InterviewMessage>

    /**
     * 면접 세션의 메시지 개수
     * 30턴 제한 검증용
     */
    fun countByMockInterviewId(mockInterviewId: Long): Int

    /**
     * 면접 세션의 특정 발신자 메시지 개수
     * USER 메시지만 카운트 등
     */
    fun countByMockInterviewIdAndSender(
        mockInterviewId: Long,
        sender: MessageSender
    ): Int

    /**
     * 면접 세션의 마지막 메시지 조회
     * 대화 흐름 추적용 (시간 기준)
     */
    fun findTopByMockInterviewIdOrderByCreatedAtDesc(
        mockInterviewId: Long
    ): InterviewMessage?
}
