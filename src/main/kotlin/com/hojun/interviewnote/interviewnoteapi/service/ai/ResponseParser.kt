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
        private const val MIN_STRENGTHS_ITEMS = 0    // 강점은 0개도 가능 (답변이 부실할 경우)
        private const val MIN_IMPROVEMENTS_ITEMS = 1  // 개선점은 최소 1개 필수
        private const val MAX_FEEDBACK_ITEMS = 5      // 유연하게 최대 5개까지 허용
        private const val MIN_MODEL_ANSWER_LENGTH = 100
        private const val MAX_MODEL_ANSWER_LENGTH = 1000
        private const val MIN_OVERALL_COMMENT_LENGTH = 10
        private const val MAX_OVERALL_COMMENT_LENGTH = 500
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
            validateStrengths(response.strengths)
            validateImprovements(response.improvements)
            validateModelAnswer(response.modelAnswer)
            validateOverallComment(response.overallComment)  // Week 2: Null-Safety 개선

            logger.info("OpenAI 응답 파싱 성공 - strengths: ${response.strengths.size}개, improvements: ${response.improvements.size}개")

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

    /**
     * strengths 검증 (0개도 허용 - 답변이 부실할 경우)
     */
    private fun validateStrengths(items: List<String>) {
        require(items.size in MIN_STRENGTHS_ITEMS..MAX_FEEDBACK_ITEMS) {
            "strengths는 ${MIN_STRENGTHS_ITEMS}-${MAX_FEEDBACK_ITEMS}개여야 합니다 (현재: ${items.size}개)"
        }
        require(items.all { it.isNotBlank() }) {
            "strengths에 빈 항목이 포함되어 있습니다"
        }
    }

    /**
     * improvements 검증 (최소 1개 필수 - 사용자에게 피드백 제공)
     */
    private fun validateImprovements(items: List<String>) {
        require(items.size in MIN_IMPROVEMENTS_ITEMS..MAX_FEEDBACK_ITEMS) {
            "improvements는 최소 ${MIN_IMPROVEMENTS_ITEMS}개 이상이어야 합니다 (현재: ${items.size}개)"
        }
        require(items.all { it.isNotBlank() }) {
            "improvements에 빈 항목이 포함되어 있습니다"
        }
    }

    private fun validateModelAnswer(answer: String) {
        require(answer.length in MIN_MODEL_ANSWER_LENGTH..MAX_MODEL_ANSWER_LENGTH) {
            "모범답변 길이는 ${MIN_MODEL_ANSWER_LENGTH}-${MAX_MODEL_ANSWER_LENGTH}자 사이여야 합니다 (현재: ${answer.length}자)"
        }
    }

    /**
     * overallComment 검증 (Week 2: Null-Safety 개선)
     */
    private fun validateOverallComment(comment: String) {
        require(comment.isNotBlank()) {
            "종합 코멘트가 비어있습니다"
        }
        require(comment.length in MIN_OVERALL_COMMENT_LENGTH..MAX_OVERALL_COMMENT_LENGTH) {
            "종합 코멘트 길이는 ${MIN_OVERALL_COMMENT_LENGTH}-${MAX_OVERALL_COMMENT_LENGTH}자 사이여야 합니다 (현재: ${comment.length}자)"
        }
    }
}
