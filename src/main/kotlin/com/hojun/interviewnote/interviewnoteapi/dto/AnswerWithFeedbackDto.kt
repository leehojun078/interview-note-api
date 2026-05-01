package com.hojun.interviewnote.interviewnoteapi.dto

import java.time.LocalDateTime

data class AnswerWithFeedbackDto(
    val answerId: Long,
    val questionId: Long,
    val questionContent: String,
    val answerText: String,
    val answeredAt: LocalDateTime,
    val feedback: FeedbackDto,

    // Phase 6: GeneratedQuestion 지원
    val generatedQuestionId: Long? = null,  // GeneratedQuestion ID (없으면 null)
    val isGenerated: Boolean = false         // true면 GeneratedQuestion, false면 일반 Question
)
