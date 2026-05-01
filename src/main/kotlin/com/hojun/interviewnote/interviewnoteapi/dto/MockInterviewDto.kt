package com.hojun.interviewnote.interviewnoteapi.dto

import com.hojun.interviewnote.interviewnoteapi.domain.JobField
import com.hojun.interviewnote.interviewnoteapi.domain.MockInterviewStatus
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * 모의 면접 시작 요청
 *
 * Phase 7: 직무 기반 또는 공고 기반 면접 시작
 */
data class StartInterviewRequest(
    /**
     * 채용 공고 ID (선택 사항)
     * - null: 직무 기반 일반 면접
     * - not null: 공고 기반 맞춤 면접
     */
    val jobPostingId: Long? = null,

    /**
     * 선택 직무 (필수)
     * - 공고 기반: 공고의 effectiveJobField 사용 (자동)
     * - 직무 기반: 사용자 직접 선택
     */
    @field:NotNull(message = "직무를 선택해주세요")
    val selectedJobField: JobField
)

/**
 * 메시지 전송 요청
 *
 * Phase 7: 사용자 답변 제출
 */
data class SendMessageRequest(
    /**
     * 답변 내용 (200자 이내)
     */
    @field:Size(min = 1, max = 200, message = "답변은 1-200자 이내로 작성해주세요")
    val content: String
)

/**
 * 면접 메시지 DTO
 *
 * Phase 7: SSE 실시간 전송용
 */
data class InterviewMessageDto(
    val id: Long,
    val sender: String,  // "AI" or "USER"
    val content: String,
    val timestamp: LocalDateTime,

    /**
     * 평가 정보 (USER 메시지에만 존재)
     */
    val evaluation: EvaluationDto? = null
)

/**
 * 평가 정보
 */
data class EvaluationDto(
    val logicScore: Int,
    val specificityScore: Int,
    val deliveryScore: Int,
    val comment: String
)

/**
 * 면접 세션 ViewModel
 *
 * Phase 7: 면접 결과 페이지 표시용
 */
data class MockInterviewViewModel(
    val id: Long,
    val selectedJobField: JobField,
    val status: MockInterviewStatus,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime?,
    val messageCount: Int,

    // 공고 정보 (nullable)
    val jobPosting: JobPostingSummaryDto?,

    // 종합 평가 (종료 시에만 존재)
    val averageScore: Double?,
    val overallFeedback: String?,
    val keyStrengths: List<String>?,  // JSON array 파싱 후
    val keyImprovements: List<String>?,  // JSON array 파싱 후
    val recommendation: String?
)

/**
 * 면접 목록 아이템
 *
 * Phase 7: 면접 히스토리 목록용
 */
data class MockInterviewListItemDto(
    val id: Long,
    val selectedJobField: JobField,
    val status: MockInterviewStatus,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime?,
    val averageScore: Double?,
    val jobPosting: JobPostingSummaryDto?
)
