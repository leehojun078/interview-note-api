package com.hojun.interviewnote.interviewnoteapi.dto

import com.hojun.interviewnote.interviewnoteapi.config.ObjectMapperConfig
import com.hojun.interviewnote.interviewnoteapi.domain.AiFeedback
import org.slf4j.LoggerFactory

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
    /**
     * 4가지 평가 점수의 평균
     */
    val averageScore: Double
        get() = (logicScore + specificityScore + jobFitScore + deliveryScore) / 4.0

    companion object {
        private val logger = LoggerFactory.getLogger(FeedbackDto::class.java)

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
                ObjectMapperConfig.objectMapper.readValue(json, List::class.java) as List<String>
            } catch (e: Exception) {
                logger.warn("JSON 파싱 실패 - 원본: $json", e)
                emptyList()
            }
        }
    }
}
