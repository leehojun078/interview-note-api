package com.hojun.interviewnote.interviewnoteapi.service.ai

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.hojun.interviewnote.interviewnoteapi.domain.JobField
import com.hojun.interviewnote.interviewnoteapi.exception.AiResponseParseException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 파싱된 AI 질문 생성 데이터
 *
 * Phase 6B: QuestionGeneratorService의 반환 타입
 */
data class ParsedQuestions(
    val inferredJobField: JobField?,  // 사용자가 직무 미선택 시에만 사용
    val questions: List<GeneratedQuestionData>
)

/**
 * 생성된 질문 데이터 (엔티티 변환 전)
 */
data class GeneratedQuestionData(
    val content: String,
    val category: String,
    val difficulty: String,
    val reasoning: String  // aiReasoning으로 저장됨
)

/**
 * OpenAI 질문 생성 응답 구조 (내부용)
 */
internal data class OpenAiQuestionResponse(
    val inferredJobField: String?,
    val questions: List<QuestionItem>
) {
    data class QuestionItem(
        val content: String,
        val category: String,
        val difficulty: String,
        val reasoning: String
    )
}

/**
 * OpenAI 질문 생성 응답 파싱 및 검증
 *
 * Phase 6B에서 추가됨
 * - 10개 질문 검증
 * - difficulty 유효성 검증
 * - 난이도 분포 로그
 */
@Service
class QuestionResponseParser(
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val EXPECTED_QUESTION_COUNT = 10
        private val VALID_DIFFICULTIES = setOf("EASY", "MEDIUM", "HARD")
        private const val MIN_CONTENT_LENGTH = 10
        private const val MAX_CONTENT_LENGTH = 500
        private const val MIN_REASONING_LENGTH = 10
        private const val MAX_REASONING_LENGTH = 500
    }

    /**
     * OpenAI JSON 응답을 ParsedQuestions로 파싱
     *
     * @param jsonString AI 응답 JSON
     * @return 파싱된 질문 데이터
     * @throws AiResponseParseException 파싱 실패 또는 검증 실패 시
     */
    fun parseQuestionResponse(jsonString: String): ParsedQuestions {
        val rawResponse = jsonString  // 디버깅용
        try {
            val response = objectMapper.readValue(jsonString, OpenAiQuestionResponse::class.java)

            // 검증
            validateQuestionCount(response.questions)
            response.questions.forEachIndexed { index, question ->
                validateQuestion(question, index + 1)
            }

            // 난이도 분포 로그
            logDifficultyDistribution(response.questions)

            // inferredJobField 변환
            val inferredJobField = response.inferredJobField?.let { code ->
                JobField.fromCode(code) ?: run {
                    logger.warn("알 수 없는 직무 코드: $code, null로 처리")
                    null
                }
            }

            logger.info(
                "질문 생성 응답 파싱 성공 - 질문: ${response.questions.size}개, " +
                        "추론된 직무: ${inferredJobField?.displayName ?: "없음"}"
            )

            return ParsedQuestions(
                inferredJobField = inferredJobField,
                questions = response.questions.map { item ->
                    GeneratedQuestionData(
                        content = item.content,
                        category = item.category,
                        difficulty = item.difficulty,
                        reasoning = item.reasoning
                    )
                }
            )

        } catch (e: JsonProcessingException) {
            logger.error("질문 생성 JSON 파싱 실패: ${e.message}", e)
            throw AiResponseParseException(
                "질문 생성 응답의 JSON 형식이 잘못되었습니다: ${e.message}",
                rawResponse,
                e
            )
        } catch (e: IllegalArgumentException) {
            logger.error("질문 생성 응답 검증 실패: ${e.message}", e)
            throw AiResponseParseException(
                "질문 생성 응답 내용이 유효하지 않습니다: ${e.message}",
                rawResponse,
                e
            )
        }
    }

    /**
     * 질문 개수 검증 (정확히 10개)
     */
    private fun validateQuestionCount(questions: List<OpenAiQuestionResponse.QuestionItem>) {
        require(questions.size == EXPECTED_QUESTION_COUNT) {
            "질문은 정확히 ${EXPECTED_QUESTION_COUNT}개여야 합니다 (현재: ${questions.size}개)"
        }
    }

    /**
     * 개별 질문 검증
     */
    private fun validateQuestion(question: OpenAiQuestionResponse.QuestionItem, index: Int) {
        // content 검증
        require(question.content.isNotBlank()) {
            "질문 #$index: content가 비어있습니다"
        }
        require(question.content.length in MIN_CONTENT_LENGTH..MAX_CONTENT_LENGTH) {
            "질문 #$index: content 길이는 $MIN_CONTENT_LENGTH-${MAX_CONTENT_LENGTH}자 사이여야 합니다 " +
                    "(현재: ${question.content.length}자)"
        }

        // category 검증
        require(question.category.isNotBlank()) {
            "질문 #$index: category가 비어있습니다"
        }

        // difficulty 검증
        require(question.difficulty in VALID_DIFFICULTIES) {
            "질문 #$index: difficulty는 $VALID_DIFFICULTIES 중 하나여야 합니다 " +
                    "(현재: ${question.difficulty})"
        }

        // reasoning 검증
        require(question.reasoning.isNotBlank()) {
            "질문 #$index: reasoning이 비어있습니다"
        }
        require(question.reasoning.length in MIN_REASONING_LENGTH..MAX_REASONING_LENGTH) {
            "질문 #$index: reasoning 길이는 $MIN_REASONING_LENGTH-${MAX_REASONING_LENGTH}자 사이여야 합니다 " +
                    "(현재: ${question.reasoning.length}자)"
        }
    }

    /**
     * 난이도 분포 로그 (권장: EASY 3개, MEDIUM 4개, HARD 3개)
     */
    private fun logDifficultyDistribution(questions: List<OpenAiQuestionResponse.QuestionItem>) {
        val distribution = questions.groupingBy { it.difficulty }.eachCount()
        val easy = distribution["EASY"] ?: 0
        val medium = distribution["MEDIUM"] ?: 0
        val hard = distribution["HARD"] ?: 0

        logger.info("난이도 분포 - EASY: ${easy}개, MEDIUM: ${medium}개, HARD: ${hard}개")

        // 권장 분포와 비교 (경고만, 실패는 아님)
        if (easy !in 2..4 || medium !in 3..5 || hard !in 2..4) {
            logger.warn(
                "난이도 분포가 권장 범위를 벗어났습니다 " +
                        "(권장: EASY 2-4개, MEDIUM 3-5개, HARD 2-4개)"
            )
        }
    }
}
