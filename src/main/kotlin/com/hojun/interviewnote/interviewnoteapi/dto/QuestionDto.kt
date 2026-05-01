package com.hojun.interviewnote.interviewnoteapi.dto

import com.hojun.interviewnote.interviewnoteapi.domain.Question

data class QuestionDto(
    val id: Long,
    val jobField: String,
    val targetJob: String,
    val category: String,
    val content: String,
    val difficulty: String,
    val isGenerated: Boolean = false  // Phase 6: 일반 질문은 false, GeneratedQuestion은 true
) {
    companion object {
        fun from(question: Question): QuestionDto {
            return QuestionDto(
                id = question.id,
                jobField = question.jobField,
                targetJob = question.targetJob,
                category = question.category,
                content = question.content,
                difficulty = question.difficulty,
                isGenerated = false  // 일반 Question은 항상 false
            )
        }
    }
}
