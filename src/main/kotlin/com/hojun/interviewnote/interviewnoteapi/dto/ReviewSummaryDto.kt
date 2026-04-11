package com.hojun.interviewnote.interviewnoteapi.dto

import java.time.LocalDateTime

data class ReviewSummaryDto(
    val answerId: Long,
    val questionContent: String,
    val category: String,
    val answeredAt: LocalDateTime,
    val averageScore: Double
)
