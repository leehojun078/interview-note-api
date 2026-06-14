package com.hojun.interviewnote.interviewnoteapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.hojun.interviewnote.interviewnoteapi.config.OpenAiProperties
import com.hojun.interviewnote.interviewnoteapi.domain.AiFeedback
import com.hojun.interviewnote.interviewnoteapi.domain.InterviewAnswer
import com.hojun.interviewnote.interviewnoteapi.domain.Question
import com.hojun.interviewnote.interviewnoteapi.exception.AiException
import com.hojun.interviewnote.interviewnoteapi.repository.AiFeedbackRepository
import com.hojun.interviewnote.interviewnoteapi.service.ai.AiClient
import com.hojun.interviewnote.interviewnoteapi.service.ai.prompt.FeedbackPromptBuilder
import com.hojun.interviewnote.interviewnoteapi.service.ai.ResponseParser
import com.hojun.interviewnote.interviewnoteapi.service.cache.DuplicateRequestCache
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AiFeedbackService(
    private val aiFeedbackRepository: AiFeedbackRepository,
    private val objectMapper: ObjectMapper,
    private val aiClient: AiClient,
    private val feedbackPromptBuilder: FeedbackPromptBuilder,
    private val responseParser: ResponseParser,
    private val openAiProperties: OpenAiProperties,
    private val duplicateRequestCache: DuplicateRequestCache,
    private val meterRegistry: MeterRegistry
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    // 메트릭 정의 (lazy 초기화로 테스트 호환성 확보)
    private val aiCallsCounter by lazy { meterRegistry.counter("ai.calls.total") }
    private val aiCallsTimer by lazy { meterRegistry.timer("ai.calls.duration") }
    private val cacheHitsCounter by lazy { meterRegistry.counter("cache.hits", "type", "duplicate_request") }
    private val cacheMissesCounter by lazy { meterRegistry.counter("cache.misses", "type", "duplicate_request") }
    private val tokenUsageCounter by lazy { meterRegistry.counter("ai.tokens.total") }

    companion object {
        // 답변 길이 기준
        private const val ANSWER_LENGTH_THRESHOLD_HIGH = 500
        private const val ANSWER_LENGTH_THRESHOLD_MEDIUM = 300

        // 더미 점수
        private const val DUMMY_SCORE_HIGH = 4
        private const val DUMMY_SCORE_MEDIUM = 3
        private const val DUMMY_SCORE_LOW = 2

        // 토큰 추정 계수 (대략 4글자 = 1토큰)
        private const val TOKEN_ESTIMATION_FACTOR = 4
    }

    /**
     * Phase 2: 실제 AI 피드백 생성
     * OpenAI API를 호출하여 피드백을 생성합니다.
     * 실패 시 fallback으로 더미 피드백을 반환합니다.
     *
     * 트랜잭션 경계 분리: 각 save 작업이 독립적인 트랜잭션에서 실행되어
     * AI 파싱 실패 시 중복 피드백 생성을 방지합니다.
     */
    fun generateFeedback(answer: InterviewAnswer, question: Question): AiFeedback {
        return try {
            logger.info("AI 피드백 생성 시작 - answerId: ${answer.id}, questionId: ${question.id}")

            // 1. 캐시 확인 (중복 방지)
            val cached = duplicateRequestCache.findCached(question.id, answer.answerText)
            if (cached != null) {
                cacheHitsCounter.increment()
                logger.info("캐시된 피드백 발견 - 원본 피드백 ID: ${cached.id}, 새 답변 ID: ${answer.id}")
                return copyAndSaveCachedFeedback(cached, answer)
            }
            cacheMissesCounter.increment()

            // 2. 실제 AI 피드백 생성 (별도 트랜잭션)
            generateRealFeedback(answer, question)

        } catch (e: AiException) {
            // AI 오류 발생 시 fallback으로 더미 피드백 생성 (별도 트랜잭션)
            logger.warn("AI 피드백 생성 실패, 더미 피드백으로 fallback - 오류: ${e.message}", e)
            generateDummyFeedback(answer, question)
        } catch (e: Exception) {
            // 예상치 못한 오류 발생 시에도 fallback
            logger.error("예상치 못한 오류 발생, 더미 피드백으로 fallback - 오류: ${e.message}", e)
            generateDummyFeedback(answer, question)
        }
    }

    /**
     * 캐시된 피드백을 새로운 답변 ID로 복사하여 저장
     *
     * 별도 트랜잭션: 캐시 피드백 저장이 독립적으로 커밋됩니다.
     */
    @Transactional
    private fun copyAndSaveCachedFeedback(cached: AiFeedback, answer: InterviewAnswer): AiFeedback {
        val copiedFeedback = AiFeedback(
            interviewAnswerId = answer.id,  // 새로운 답변 ID로 연결
            logicScore = cached.logicScore,
            specificityScore = cached.specificityScore,
            jobFitScore = cached.jobFitScore,
            deliveryScore = cached.deliveryScore,
            strengths = cached.strengths,
            improvements = cached.improvements,
            modelAnswer = cached.modelAnswer,
            overallComment = cached.overallComment,
            jobField = cached.jobField,
            modelName = cached.modelName,
            promptVersion = cached.promptVersion,
            tokenUsageInput = cached.tokenUsageInput,
            tokenUsageOutput = cached.tokenUsageOutput,
            rawResponse = cached.rawResponse,
            answerTextHash = cached.answerTextHash,
            createdAt = LocalDateTime.now()
        )

        val savedCopy = aiFeedbackRepository.save(copiedFeedback)
        logger.info("캐시 피드백 복사 완료 - 새 피드백 ID: ${savedCopy.id}, 답변 ID: ${answer.id}")
        return savedCopy
    }

    /**
     * 실제 AI 호출을 통한 피드백 생성
     *
     * 별도 트랜잭션: AI 호출, 파싱, 저장이 모두 성공하거나 모두 실패합니다.
     * 파싱 실패 시 트랜잭션이 롤백되어 중복 피드백이 생성되지 않습니다.
     */
    @Transactional
    private fun generateRealFeedback(answer: InterviewAnswer, question: Question): AiFeedback {
        // 1. 프롬프트 생성
        val systemPrompt = feedbackPromptBuilder.buildSystemPrompt(question.jobField, question.targetJob)
        val userPrompt = feedbackPromptBuilder.buildUserPrompt(question, answer.answerText)

        // 2. AI 클라이언트 호출 (메트릭 기록)
        val (rawResponse, parsedFeedback) = aiCallsTimer.recordCallable {
            aiCallsCounter.increment()
            logger.info("AI 호출 시작 - questionId: ${question.id}, answerLength: ${answer.answerText.length}")

            val response = aiClient.requestFeedback(systemPrompt, userPrompt)

            // 3. 응답 파싱 (여기서 예외 발생 시 트랜잭션 롤백)
            val feedback = responseParser.parseOpenAiResponse(response, response)

            logger.info("AI 호출 완료 - questionId: ${question.id}")
            Pair(response, feedback)
        }!!

        // 4. 해시 생성
        val answerTextHash = duplicateRequestCache.generateHash(question.id, answer.answerText)

        // 5. 토큰 사용량 추정
        val tokenUsageInput = estimateTokens(systemPrompt + userPrompt)
        val tokenUsageOutput = estimateTokens(rawResponse)
        val totalTokens = tokenUsageInput + tokenUsageOutput

        // 토큰 사용량 메트릭 기록
        tokenUsageCounter.increment(totalTokens.toDouble())

        // 6. AiFeedback 엔티티 생성
        val aiFeedback = AiFeedback(
            interviewAnswerId = answer.id,
            logicScore = parsedFeedback.logicScore,
            specificityScore = parsedFeedback.specificityScore,
            jobFitScore = parsedFeedback.jobFitScore,
            deliveryScore = parsedFeedback.deliveryScore,
            strengths = objectMapper.writeValueAsString(parsedFeedback.strengths),
            improvements = objectMapper.writeValueAsString(parsedFeedback.improvements),
            modelAnswer = parsedFeedback.modelAnswer,
            overallComment = parsedFeedback.overallComment,
            jobField = question.jobField,
            modelName = openAiProperties.model,
            promptVersion = openAiProperties.promptVersion,
            tokenUsageInput = tokenUsageInput,
            tokenUsageOutput = tokenUsageOutput,
            rawResponse = rawResponse,
            answerTextHash = answerTextHash,
            createdAt = LocalDateTime.now()
        )

        // 7. 저장 및 반환 (파싱까지 성공했으므로 커밋)
        val savedFeedback = aiFeedbackRepository.save(aiFeedback)
        logger.info("AI 피드백 생성 완료 - feedbackId: ${savedFeedback.id}, totalTokens: $totalTokens")
        return savedFeedback
    }

    /**
     * 토큰 수 추정 (대략 4글자 = 1토큰)
     */
    private fun estimateTokens(text: String): Int {
        return text.length / TOKEN_ESTIMATION_FACTOR
    }

    /**
     * Phase 1: 더미 AI 피드백 생성
     * Phase 2에서는 fallback용으로 사용됩니다.
     *
     * 별도 트랜잭션: AI 실패 시 더미 피드백만 독립적으로 저장됩니다.
     */
    @Transactional
    fun generateDummyFeedback(answer: InterviewAnswer, question: Question): AiFeedback {
        val answerLength = answer.answerText.length

        // 답변 길이에 따라 점수 조정 (간단한 로직)
        val baseScore = when {
            answerLength >= ANSWER_LENGTH_THRESHOLD_HIGH -> DUMMY_SCORE_HIGH
            answerLength >= ANSWER_LENGTH_THRESHOLD_MEDIUM -> DUMMY_SCORE_MEDIUM
            else -> DUMMY_SCORE_LOW
        }

        val strengths = listOf(
            "답변의 구조가 논리적입니다",
            "구체적인 예시를 제시했습니다",
            "질문의 의도를 정확히 파악했습니다"
        )

        val improvements = listOf(
            "기술적 깊이를 더할 수 있는 부분이 있습니다",
            "더 구체적인 수치나 데이터를 추가하면 좋겠습니다",
            "실제 경험을 더 강조하면 설득력이 높아집니다"
        )

        val modelAnswer = """
            [더미 모범답변]

            ${question.content}에 대한 모범답변입니다.

            1. 핵심 개념 설명
            - 중요한 개념들을 명확히 설명합니다
            - 관련 기술이나 원리를 언급합니다

            2. 구체적인 예시
            - 실제 프로젝트 경험을 바탕으로 설명합니다
            - 수치나 성과를 구체적으로 제시합니다

            3. 결론
            - 핵심 내용을 요약합니다
            - 향후 개선 방향이나 학습 계획을 언급합니다

            (Phase 2에서 실제 AI 모범답변으로 교체됩니다)
        """.trimIndent()

        val aiFeedback = AiFeedback(
            interviewAnswerId = answer.id,
            logicScore = baseScore,
            specificityScore = baseScore - 1,
            jobFitScore = baseScore,
            deliveryScore = baseScore - 1,
            strengths = objectMapper.writeValueAsString(strengths.take(2)),
            improvements = objectMapper.writeValueAsString(improvements.take(2)),
            modelAnswer = modelAnswer,
            overallComment = "전반적으로 좋은 답변입니다. 더 구체적인 예시를 추가하면 더욱 좋겠습니다. (더미 피드백)",
            jobField = question.jobField,
            modelName = "dummy-model-v1",
            promptVersion = "v1.0-dummy",
            tokenUsageInput = answerLength / TOKEN_ESTIMATION_FACTOR,
            tokenUsageOutput = modelAnswer.length / TOKEN_ESTIMATION_FACTOR,
            rawResponse = "{\"dummy\": true, \"message\": \"This is a dummy response for Phase 1\"}",
            createdAt = LocalDateTime.now()
        )

        return aiFeedbackRepository.save(aiFeedback)
    }

    fun findByInterviewAnswerId(interviewAnswerId: Long): AiFeedback? {
        return aiFeedbackRepository.findByInterviewAnswerId(interviewAnswerId)
    }
}
