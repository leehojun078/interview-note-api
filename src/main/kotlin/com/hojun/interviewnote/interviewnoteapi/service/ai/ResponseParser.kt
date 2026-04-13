package com.hojun.interviewnote.interviewnoteapi.service.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.core.JsonProcessingException
import com.hojun.interviewnote.interviewnoteapi.exception.AiResponseParseException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 파싱된 AI 피드백 데이터
 */
data class ParsedFeedback(
    val logicScore: Int,
    val specificityScore: Int,
    val jobFitScore: Int,
    val deliveryScore: Int,
    val strengths: List<String>,
    val improvements: List<String>,
    val modelAnswer: String,
    val overallComment: String
)

/**
 * OpenAI JSON 응답 구조 (내부용)
 */
internal data class OpenAiResponse(
    val scores: Scores,
    val strengths: List<String>,
    val improvements: List<String>,
    val modelAnswer: String,
    val overallComment: String
) {
    data class Scores(
        val logic: Int,
        val specificity: Int,
        val jobFit: Int,
        val delivery: Int
    )
}

/**
 * OpenAI 응답 파싱 및 검증
 *
 * OpenAI API의 JSON 응답을 파싱하고, 데이터 유효성을 검증합니다.
 * 검증 실패 시 AiResponseParseException을 발생시킵니다.
 */
@Service
class ResponseParser(
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MIN_SCORE = 1
        private const val MAX_SCORE = 5
        private const val MIN_FEEDBACK_ITEMS = 2
        private const val MAX_FEEDBACK_ITEMS = 3
        private const val MIN_MODEL_ANSWER_LENGTH = 100
        private const val MAX_MODEL_ANSWER_LENGTH = 1000
    }

    /**
     * OpenAI JSON 응답을 ParsedFeedback으로 파싱
     * 파싱 실패 또는 검증 실패 시 AiResponseParseException 발생
     */
    fun parseOpenAiResponse(jsonString: String, rawResponse: String = jsonString): ParsedFeedback {
        try {
            val response = objectMapper.readValue(jsonString, OpenAiResponse::class.java)

            // 검증
            validateScores(response.scores)
            validateFeedbackItems(response.strengths, "strengths")
            validateFeedbackItems(response.improvements, "improvements")
            validateModelAnswer(response.modelAnswer)

            logger.info("OpenAI 응답 파싱 성공")

            return ParsedFeedback(
                logicScore = response.scores.logic,
                specificityScore = response.scores.specificity,
                jobFitScore = response.scores.jobFit,
                deliveryScore = response.scores.delivery,
                strengths = response.strengths,
                improvements = response.improvements,
                modelAnswer = response.modelAnswer,
                overallComment = response.overallComment
            )

        } catch (e: JsonProcessingException) {
            logger.error("JSON 파싱 실패: ${e.message}", e)
            throw AiResponseParseException("AI 응답의 JSON 형식이 잘못되었습니다: ${e.message}", rawResponse, e)
        } catch (e: IllegalArgumentException) {
            logger.error("응답 검증 실패: ${e.message}", e)
            throw AiResponseParseException("AI 응답 내용이 유효하지 않습니다: ${e.message}", rawResponse, e)
        }
    }

    private fun validateScores(scores: OpenAiResponse.Scores) {
        listOf(
            "logic" to scores.logic,
            "specificity" to scores.specificity,
            "jobFit" to scores.jobFit,
            "delivery" to scores.delivery
        ).forEach { (name, score) ->
            require(score in MIN_SCORE..MAX_SCORE) {
                "$name 점수는 $MIN_SCORE-$MAX_SCORE 사이여야 합니다 (현재: $score)"
            }
        }
    }

    private fun validateFeedbackItems(items: List<String>, fieldName: String) {
        require(items.size in MIN_FEEDBACK_ITEMS..MAX_FEEDBACK_ITEMS) {
            "${fieldName}는 ${MIN_FEEDBACK_ITEMS}-${MAX_FEEDBACK_ITEMS}개여야 합니다 (현재: ${items.size}개)"
        }
        require(items.all { it.isNotBlank() }) {
            "${fieldName}에 빈 항목이 포함되어 있습니다"
        }
    }

    private fun validateModelAnswer(answer: String) {
        require(answer.length in MIN_MODEL_ANSWER_LENGTH..MAX_MODEL_ANSWER_LENGTH) {
            "모범답변 길이는 ${MIN_MODEL_ANSWER_LENGTH}-${MAX_MODEL_ANSWER_LENGTH}자 사이여야 합니다 (현재: ${answer.length}자)"
        }
    }
}
