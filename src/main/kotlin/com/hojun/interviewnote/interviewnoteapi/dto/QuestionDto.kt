package com.hojun.interviewnote.interviewnoteapi.dto

import com.hojun.interviewnote.interviewnoteapi.domain.Question

data class QuestionDto(
    val id: Long,
    val jobField: String,
    val targetJob: String,
    val category: String,
    val content: String,
    val difficulty: String
) {
    companion object {
        fun from(question: Question): QuestionDto {
            return QuestionDto(
                id = question.id,
                jobField = question.jobField,
                targetJob = question.targetJob,
                category = question.category,
                content = question.content,
                difficulty = question.difficulty
            )
        }
    }
}
