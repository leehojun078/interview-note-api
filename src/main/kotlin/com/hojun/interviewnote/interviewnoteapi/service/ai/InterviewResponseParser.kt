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

            // 평가 파싱 (null 안전성 추가)
            val evalNode = json["evaluation"]
                ?: throw IllegalArgumentException("evaluation 필드 누락")

            val logicScore = evalNode["logicScore"]?.asInt()
                ?: throw IllegalArgumentException("logicScore 필드 누락")
            val specificityScore = evalNode["specificityScore"]?.asInt()
                ?: throw IllegalArgumentException("specificityScore 필드 누락")
            val deliveryScore = evalNode["deliveryScore"]?.asInt()
                ?: throw IllegalArgumentException("deliveryScore 필드 누락")
            val comment = evalNode["comment"]?.asText()
                ?: throw IllegalArgumentException("comment 필드 누락")

            val evaluation = InterviewEvaluation(
                logicScore = logicScore,
                specificityScore = specificityScore,
                deliveryScore = deliveryScore,
                comment = comment
            )

            // 다음 질문 파싱 (null 안전성 추가)
            val actionNode = json["nextAction"]
                ?: throw IllegalArgumentException("nextAction 필드 누락")

            var question = actionNode["question"]?.asText()
                ?: throw IllegalArgumentException("question 필드 누락")
            val reasoning = actionNode["reasoning"]?.asText()
                ?: throw IllegalArgumentException("reasoning 필드 누락")
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
            logger.debug("원본 응답: $rawResponse")
            throw AiResponseParseException("면접 응답 파싱 실패", rawResponse, e)
        }
    }

    /**
     * 종합 평가 응답 파싱 (Phase 8A: 엄격한 검증)
     *
     * @param rawResponse OpenAI JSON 응답
     * @return 종합 평가 결과
     */
    fun parseFinalEvaluation(rawResponse: String): FinalEvaluationResult {
        try {
            val json = objectMapper.readTree(rawResponse)

            // 1. overallFeedback 파싱 및 검증
            val overallFeedback = json["overallFeedback"]?.asText()
                ?: throw IllegalArgumentException("overallFeedback 필드 누락")

            // Phase 8A: 최소 500자 요구 (권장: 800-1200자)
            if (overallFeedback.length < 500) {
                logger.warn("종합 피드백 길이 부족: ${overallFeedback.length}자 (권장: 800-1200자)")
            }

            require(overallFeedback.length >= 300) {
                "종합 피드백이 너무 짧습니다: ${overallFeedback.length}자 (최소 300자)"
            }

            // 2. keyStrengths 파싱 및 검증
            val keyStrengths = json["keyStrengths"]?.map { it.asText() }
                ?: throw IllegalArgumentException("keyStrengths 필드 누락")

            // Phase 8A: 0-5개 허용
            require(keyStrengths.size in 0..5) {
                "keyStrengths 개수 오류: ${keyStrengths.size}개 (0-5개 허용)"
            }

            // 3. keyImprovements 파싱 및 검증
            val keyImprovements = json["keyImprovements"]?.map { it.asText() }
                ?: throw IllegalArgumentException("keyImprovements 필드 누락")

            // Phase 8A: 1-5개 허용 (최소 1개)
            require(keyImprovements.size in 1..5) {
                "keyImprovements 개수 오류: ${keyImprovements.size}개 (1-5개 필요)"
            }

            // 4. averageScore 파싱 및 검증
            val averageScore = json["averageScore"]?.asDouble()
                ?: throw IllegalArgumentException("averageScore 필드 누락")

            require(averageScore in 1.0..5.0) {
                "averageScore 범위 오류: $averageScore (1.0-5.0 허용)"
            }

            // 5. recommendation 파싱
            val recommendation = json["recommendation"]?.asText()
                ?: throw IllegalArgumentException("recommendation 필드 누락")

            logger.info(
                "종합 평가 파싱 완료 - 피드백: ${overallFeedback.length}자, " +
                        "강점: ${keyStrengths.size}개, 개선: ${keyImprovements.size}개, " +
                        "평균: $averageScore"
            )

            return FinalEvaluationResult(
                overallFeedback = overallFeedback,
                keyStrengths = objectMapper.writeValueAsString(keyStrengths),
                keyImprovements = objectMapper.writeValueAsString(keyImprovements),
                averageScore = averageScore,
                recommendation = recommendation
            )
        } catch (e: Exception) {
            logger.error("종합 평가 파싱 실패: ${e.message}", e)
            logger.debug("원본 응답: $rawResponse")
            throw AiResponseParseException("종합 평가 파싱 실패: ${e.message}", rawResponse, e)
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
