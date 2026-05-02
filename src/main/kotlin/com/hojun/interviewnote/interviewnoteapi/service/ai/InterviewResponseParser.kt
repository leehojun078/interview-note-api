package com.hojun.interviewnote.interviewnoteapi.service.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.hojun.interviewnote.interviewnoteapi.exception.AiResponseParseException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 면접 AI 응답 파서
 *
 * Phase 7B: 실시간 채팅 면접 응답 파싱
 * - 질문 200자 제한 검증
 * - evaluation + nextAction 구조
 */
@Service
class InterviewResponseParser(
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val MAX_QUESTION_LENGTH = 200
        const val MIN_SCORE = 0  // 0 = 평가 없음 (첫 질문 생성 시)
        const val MAX_SCORE = 5
    }

    /**
     * 면접 진행 응답 파싱
     *
     * @param rawResponse OpenAI JSON 응답
     * @return 파싱된 평가 + 다음 질문
     */
    fun parseInterviewResponse(rawResponse: String): ParsedInterviewResponse {
        try {
            val json = objectMapper.readTree(rawResponse)

            // 평가 파싱
            val evalNode = json["evaluation"]
            val evaluation = InterviewEvaluation(
                logicScore = evalNode["logicScore"].asInt(),
                specificityScore = evalNode["specificityScore"].asInt(),
                deliveryScore = evalNode["deliveryScore"].asInt(),
                comment = evalNode["comment"].asText()
            )

            // 다음 질문 파싱
            val actionNode = json["nextAction"]
            var question = actionNode["question"].asText()
            val reasoning = actionNode["reasoning"].asText()
            val isFollowUp = actionNode["isFollowUp"]?.asBoolean() ?: false

            // 질문 길이 검증 (200자 초과 시 절단)
            if (question.length > MAX_QUESTION_LENGTH) {
                logger.warn("질문 길이 초과 (${question.length}자), 200자로 절단 처리")
                question = question.take(MAX_QUESTION_LENGTH - 3) + "..."
            }

            // 점수 범위 검증
            validateScores(evaluation)

            return ParsedInterviewResponse(
                evaluation = evaluation,
                nextAction = NextAction(
                    question = question,
                    reasoning = reasoning,
                    isFollowUp = isFollowUp
                )
            )
        } catch (e: Exception) {
            logger.error("면접 응답 파싱 실패: ${e.message}", e)
            throw AiResponseParseException("면접 응답 파싱 실패", rawResponse, e)
        }
    }

    /**
     * 종합 평가 응답 파싱
     *
     * @param rawResponse OpenAI JSON 응답
     * @return 종합 평가 결과
     */
    fun parseFinalEvaluation(rawResponse: String): FinalEvaluationResult {
        try {
            val json = objectMapper.readTree(rawResponse)

            val keyStrengths = json["keyStrengths"].map { it.asText() }
            val keyImprovements = json["keyImprovements"].map { it.asText() }

            return FinalEvaluationResult(
                overallFeedback = json["overallFeedback"].asText(),
                keyStrengths = objectMapper.writeValueAsString(keyStrengths),
                keyImprovements = objectMapper.writeValueAsString(keyImprovements),
                averageScore = json["averageScore"].asDouble(),
                recommendation = json["recommendation"].asText()
            )
        } catch (e: Exception) {
            logger.error("종합 평가 파싱 실패: ${e.message}", e)
            throw AiResponseParseException("종합 평가 파싱 실패", rawResponse, e)
        }
    }

    /**
     * 점수 범위 검증
     */
    private fun validateScores(evaluation: InterviewEvaluation) {
        require(evaluation.logicScore in MIN_SCORE..MAX_SCORE) {
            "logicScore 범위 오류: ${evaluation.logicScore}"
        }
        require(evaluation.specificityScore in MIN_SCORE..MAX_SCORE) {
            "specificityScore 범위 오류: ${evaluation.specificityScore}"
        }
        require(evaluation.deliveryScore in MIN_SCORE..MAX_SCORE) {
            "deliveryScore 범위 오류: ${evaluation.deliveryScore}"
        }
    }
}

// ===== DTOs =====

/**
 * 면접 응답 (평가 + 다음 질문)
 */
data class ParsedInterviewResponse(
    val evaluation: InterviewEvaluation,
    val nextAction: NextAction
)

/**
 * 답변 평가
 */
data class InterviewEvaluation(
    val logicScore: Int,
    val specificityScore: Int,
    val deliveryScore: Int,
    val comment: String
)

/**
 * 다음 질문
 */
data class NextAction(
    val question: String,       // 200자 이내
    val reasoning: String,       // 질문 의도
    val isFollowUp: Boolean      // 꼬리 질문 여부
)

/**
 * 종합 평가 결과
 */
data class FinalEvaluationResult(
    val overallFeedback: String,
    val keyStrengths: String,        // JSON array string
    val keyImprovements: String,     // JSON array string
    val averageScore: Double,
    val recommendation: String
)

/**
 * AI 면접 응답 (Service용)
 */
data class AiInterviewResponse(
    val question: String,
    val reasoning: String
)
