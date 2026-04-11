package com.hojun.interviewnote.interviewnoteapi.dto

import java.time.LocalDateTime

data class AnswerWithFeedbackDto(
    val answerId: Long,
    val questionId: Long,
    val questionContent: String,
    val answerText: String,
    val answeredAt: LocalDateTime,
    val feedback: FeedbackDto
)
