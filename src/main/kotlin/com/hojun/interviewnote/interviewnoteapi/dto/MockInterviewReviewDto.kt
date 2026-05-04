package com.hojun.interviewnote.interviewnoteapi.dto

import java.time.LocalDateTime

/**
 * AI 면접 리뷰 목록 DTO
 * Phase 8C: 리뷰 이력 통합
 */
data class MockInterviewReviewDto(
    val interviewId: Long,
    val jobField: String,               // "IT개발"
    val careerLevel: String?,           // "신입", "주니어" 등
    val startedAt: LocalDateTime,
    val averageScore: Double?,          // weightedAverageScore 우선, 없으면 averageScore
    val messageCount: Int,              // AI 질문 개수
    val jobPostingInfo: JobPostingInfoDto?  // 공고 기반인 경우
)

/**
 * 채용 공고 요약 DTO
 */
data class JobPostingInfoDto(
    val companyName: String,
    val jobTitle: String
)
