package com.hojun.interviewnote.interviewnoteapi.dto

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.hojun.interviewnote.interviewnoteapi.domain.AiFeedback

data class FeedbackDto(
    val logicScore: Int,
    val specificityScore: Int,
    val jobFitScore: Int,
    val deliveryScore: Int,
    val strengths: List<String>,
    val improvements: List<String>,
    val modelAnswer: String,
    val overallComment: String
) {
    companion object {
        private val objectMapper = jacksonObjectMapper()

        fun from(aiFeedback: AiFeedback): FeedbackDto {
            return FeedbackDto(
                logicScore = aiFeedback.logicScore,
                specificityScore = aiFeedback.specificityScore,
                jobFitScore = aiFeedback.jobFitScore,
                deliveryScore = aiFeedback.deliveryScore,
                strengths = parseJsonArray(aiFeedback.strengths),
                improvements = parseJsonArray(aiFeedback.improvements),
                modelAnswer = aiFeedback.modelAnswer,
                overallComment = aiFeedback.overallComment
            )
        }

        private fun parseJsonArray(json: String): List<String> {
            return try {
                objectMapper.readValue(json, List::class.java) as List<String>
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
