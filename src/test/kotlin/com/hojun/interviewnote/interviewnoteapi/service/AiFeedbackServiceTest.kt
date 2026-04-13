package com.hojun.interviewnote.interviewnoteapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.hojun.interviewnote.interviewnoteapi.config.OpenAiProperties
import com.hojun.interviewnote.interviewnoteapi.domain.AiFeedback
import com.hojun.interviewnote.interviewnoteapi.domain.InterviewAnswer
import com.hojun.interviewnote.interviewnoteapi.domain.Question
import com.hojun.interviewnote.interviewnoteapi.repository.AiFeedbackRepository
import com.hojun.interviewnote.interviewnoteapi.service.ai.AiClient
import com.hojun.interviewnote.interviewnoteapi.service.ai.PromptBuilder
import com.hojun.interviewnote.interviewnoteapi.service.ai.ResponseParser
import com.hojun.interviewnote.interviewnoteapi.service.cache.DuplicateRequestCache
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.Mockito.lenient
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.concurrent.Callable

@ExtendWith(MockitoExtension::class)
class AiFeedbackServiceTest {

    @Mock
    private lateinit var aiFeedbackRepository: AiFeedbackRepository

    @Mock
    private lateinit var objectMapper: ObjectMapper

    @Mock
    private lateinit var aiClient: AiClient

    @Mock
    private lateinit var promptBuilder: PromptBuilder

    @Mock
    private lateinit var responseParser: ResponseParser

    @Mock
    private lateinit var openAiProperties: OpenAiProperties

    @Mock
    private lateinit var duplicateRequestCache: DuplicateRequestCache

    @Mock
    private lateinit var meterRegistry: MeterRegistry

    @Mock
    private lateinit var mockCounter: Counter

    @Mock
    private lateinit var mockTimer: Timer

    @InjectMocks
    private lateinit var aiFeedbackService: AiFeedbackService

    @BeforeEach
    fun setUp() {
        // MeterRegistry Mock 설정 (lenient로 설정하여 UnnecessaryStubbingException 방지)
        lenient().`when`(meterRegistry.counter(any<String>())).thenReturn(mockCounter)
        lenient().`when`(meterRegistry.counter(any<String>(), any<String>(), any<String>())).thenReturn(mockCounter)
        lenient().`when`(meterRegistry.timer(any<String>())).thenReturn(mockTimer)
        lenient().`when`(mockTimer.recordCallable(any<Callable<Any>>())).thenAnswer { invocation ->
            val callable = invocation.getArgument<Callable<Any>>(0)
            callable.call()
        }
    }

    private fun createAnswer(answerText: String): InterviewAnswer {
        return InterviewAnswer(
            id = 1L,
            questionId = 1L,
            answerText = answerText,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    private fun createQuestion(): Question {
        return Question(
            id = 1L,
            jobField = "IT",
            targetJob = "백엔드 개발자",
            category = "기술역량",
            content = "테스트 질문",
            difficulty = "MEDIUM",
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    @Test
    fun `답변 길이가 500자 이상이면 높은 점수를 부여한다`() {
        // given
        val longAnswer = "a".repeat(600)
        val answer = createAnswer(longAnswer)
        val question = createQuestion()

        whenever(objectMapper.writeValueAsString(any())).thenReturn("[]")
        doAnswer { it.arguments[0] }.whenever(aiFeedbackRepository).save(any())

        // when
        val result = aiFeedbackService.generateDummyFeedback(answer, question)

        // then
        assertThat(result.logicScore).isEqualTo(4) // DUMMY_SCORE_HIGH
    }

    @Test
    fun `답변 길이가 300자 이상 500자 미만이면 중간 점수를 부여한다`() {
        // given
        val mediumAnswer = "a".repeat(400)
        val answer = createAnswer(mediumAnswer)
        val question = createQuestion()

        whenever(objectMapper.writeValueAsString(any())).thenReturn("[]")
        doAnswer { it.arguments[0] }.whenever(aiFeedbackRepository).save(any())

        // when
        val result = aiFeedbackService.generateDummyFeedback(answer, question)

        // then
        assertThat(result.logicScore).isEqualTo(3) // DUMMY_SCORE_MEDIUM
    }

    @Test
    fun `답변 길이가 300자 미만이면 낮은 점수를 부여한다`() {
        // given
        val shortAnswer = "a".repeat(100)
        val answer = createAnswer(shortAnswer)
        val question = createQuestion()

        whenever(objectMapper.writeValueAsString(any())).thenReturn("[]")
        doAnswer { it.arguments[0] }.whenever(aiFeedbackRepository).save(any())

        // when
        val result = aiFeedbackService.generateDummyFeedback(answer, question)

        // then
        assertThat(result.logicScore).isEqualTo(2) // DUMMY_SCORE_LOW
    }

    @Test
    fun `피드백을 JSON으로 직렬화하여 저장한다`() {
        // given
        val answer = createAnswer("테스트 답변")
        val question = createQuestion()

        whenever(objectMapper.writeValueAsString(any()))
            .thenReturn("[\"강점1\", \"강점2\"]")
        doAnswer { it.arguments[0] }.whenever(aiFeedbackRepository).save(any())

        // when
        val result = aiFeedbackService.generateDummyFeedback(answer, question)

        // then
        assertThat(result.strengths).isEqualTo("[\"강점1\", \"강점2\"]")
        assertThat(result.improvements).isEqualTo("[\"강점1\", \"강점2\"]")
    }

    @Test
    fun `답변ID로 피드백을 조회한다`() {
        // given
        val feedback = AiFeedback(
            id = 1L,
            interviewAnswerId = 1L,
            logicScore = 4,
            specificityScore = 3,
            jobFitScore = 4,
            deliveryScore = 3,
            strengths = "[]",
            improvements = "[]",
            modelAnswer = "모범답변",
            overallComment = "좋습니다",
            jobField = "IT",
            modelName = "test",
            promptVersion = "v1.0",
            tokenUsageInput = 100,
            tokenUsageOutput = 200,
            rawResponse = "{}",
            createdAt = LocalDateTime.now()
        )
        whenever(aiFeedbackRepository.findByInterviewAnswerId(1L)).thenReturn(feedback)

        // when
        val result = aiFeedbackService.findByInterviewAnswerId(1L)

        // then
        assertThat(result).isNotNull
        assertThat(result?.interviewAnswerId).isEqualTo(1L)
    }

    // ========== generateFeedback() 실제 AI 플로우 테스트 ==========

    @Test
    fun `generateFeedback - 캐시 히트 시 캐시된 피드백을 반환한다`() {
        // given
        val answer = createAnswer("테스트 답변")
        val question = createQuestion()
        val cachedFeedback = AiFeedback(
            id = 100L,
            interviewAnswerId = answer.id,
            logicScore = 5,
            specificityScore = 4,
            jobFitScore = 5,
            deliveryScore = 4,
            strengths = "[\"캐시된 강점\"]",
            improvements = "[\"캐시된 개선점\"]",
            modelAnswer = "캐시된 모범답변",
            overallComment = "캐시된 코멘트",
            jobField = "IT",
            modelName = "gpt-4o-mini",
            promptVersion = "v1.0",
            tokenUsageInput = 100,
            tokenUsageOutput = 50,
            rawResponse = "{}",
            answerTextHash = "cached-hash",
            createdAt = LocalDateTime.now()
        )

        whenever(duplicateRequestCache.findCached(question.id, answer.answerText))
            .thenReturn(cachedFeedback)

        // when
        val result = aiFeedbackService.generateFeedback(answer, question)

        // then
        assertThat(result.id).isEqualTo(100L)
        assertThat(result.modelAnswer).isEqualTo("캐시된 모범답변")
        // AI 클라이언트가 호출되지 않음을 확인
        org.mockito.kotlin.verify(aiClient, org.mockito.kotlin.never()).requestFeedback(any(), any())
    }

    @Test
    fun `generateFeedback - 캐시 미스 시 AI 호출 후 피드백을 생성한다`() {
        // given
        val answer = createAnswer("테스트 답변 내용입니다.")
        val question = createQuestion()

        val systemPrompt = "System prompt"
        val userPrompt = "User prompt"
        val rawResponse = """
            {
              "scores": {"logic": 4, "specificity": 3, "jobFit": 5, "delivery": 4},
              "strengths": ["강점1", "강점2"],
              "improvements": ["개선1", "개선2"],
              "modelAnswer": "${"x".repeat(400)}",
              "overallComment": "좋습니다"
            }
        """.trimIndent()

        val parsedFeedback = com.hojun.interviewnote.interviewnoteapi.service.ai.ParsedFeedback(
            logicScore = 4,
            specificityScore = 3,
            jobFitScore = 5,
            deliveryScore = 4,
            strengths = listOf("강점1", "강점2"),
            improvements = listOf("개선1", "개선2"),
            modelAnswer = "x".repeat(400),
            overallComment = "좋습니다"
        )

        // Mocking
        whenever(duplicateRequestCache.findCached(question.id, answer.answerText)).thenReturn(null)
        whenever(promptBuilder.buildSystemPrompt(question.jobField, question.targetJob)).thenReturn(systemPrompt)
        whenever(promptBuilder.buildUserPrompt(question, answer.answerText)).thenReturn(userPrompt)
        whenever(aiClient.requestFeedback(systemPrompt, userPrompt)).thenReturn(rawResponse)
        whenever(responseParser.parseOpenAiResponse(rawResponse, rawResponse)).thenReturn(parsedFeedback)
        whenever(duplicateRequestCache.generateHash(question.id, answer.answerText)).thenReturn("test-hash")
        whenever(objectMapper.writeValueAsString(any())).thenReturn("[\"item1\", \"item2\"]")
        whenever(openAiProperties.model).thenReturn("gpt-4o-mini")
        whenever(openAiProperties.promptVersion).thenReturn("v1.0")

        doAnswer { invocation ->
            val arg = invocation.arguments[0] as AiFeedback
            AiFeedback(
                id = 1L,
                interviewAnswerId = arg.interviewAnswerId,
                logicScore = arg.logicScore,
                specificityScore = arg.specificityScore,
                jobFitScore = arg.jobFitScore,
                deliveryScore = arg.deliveryScore,
                strengths = arg.strengths,
                improvements = arg.improvements,
                modelAnswer = arg.modelAnswer,
                overallComment = arg.overallComment,
                jobField = arg.jobField,
                modelName = arg.modelName,
                promptVersion = arg.promptVersion,
                tokenUsageInput = arg.tokenUsageInput,
                tokenUsageOutput = arg.tokenUsageOutput,
                rawResponse = arg.rawResponse,
                answerTextHash = arg.answerTextHash,
                createdAt = arg.createdAt
            )
        }.whenever(aiFeedbackRepository).save(any())

        // when
        val result = aiFeedbackService.generateFeedback(answer, question)

        // then
        assertThat(result.logicScore).isEqualTo(4)
        assertThat(result.specificityScore).isEqualTo(3)
        assertThat(result.jobFitScore).isEqualTo(5)
        assertThat(result.deliveryScore).isEqualTo(4)
        assertThat(result.answerTextHash).isEqualTo("test-hash")
        assertThat(result.modelName).isEqualTo("gpt-4o-mini")
        assertThat(result.promptVersion).isEqualTo("v1.0")

        // 모든 단계가 호출되었는지 확인
        org.mockito.kotlin.verify(duplicateRequestCache).findCached(question.id, answer.answerText)
        org.mockito.kotlin.verify(promptBuilder).buildSystemPrompt(question.jobField, question.targetJob)
        org.mockito.kotlin.verify(promptBuilder).buildUserPrompt(question, answer.answerText)
        org.mockito.kotlin.verify(aiClient).requestFeedback(systemPrompt, userPrompt)
        org.mockito.kotlin.verify(responseParser).parseOpenAiResponse(rawResponse, rawResponse)
        org.mockito.kotlin.verify(duplicateRequestCache).generateHash(question.id, answer.answerText)
        org.mockito.kotlin.verify(aiFeedbackRepository).save(any())
    }

    @Test
    fun `generateFeedback - AI 호출 실패 시 더미 피드백으로 fallback한다`() {
        // given
        val answer = createAnswer("테스트 답변")
        val question = createQuestion()

        whenever(duplicateRequestCache.findCached(question.id, answer.answerText)).thenReturn(null)
        whenever(promptBuilder.buildSystemPrompt(any(), any())).thenReturn("system")
        whenever(promptBuilder.buildUserPrompt(any(), any())).thenReturn("user")
        whenever(aiClient.requestFeedback(any(), any()))
            .thenThrow(com.hojun.interviewnote.interviewnoteapi.exception.AiApiException("API 오류", null))
        whenever(objectMapper.writeValueAsString(any())).thenReturn("[]")

        doAnswer { invocation ->
            val arg = invocation.arguments[0] as AiFeedback
            AiFeedback(
                id = 1L,
                interviewAnswerId = arg.interviewAnswerId,
                logicScore = arg.logicScore,
                specificityScore = arg.specificityScore,
                jobFitScore = arg.jobFitScore,
                deliveryScore = arg.deliveryScore,
                strengths = arg.strengths,
                improvements = arg.improvements,
                modelAnswer = arg.modelAnswer,
                overallComment = arg.overallComment,
                jobField = arg.jobField,
                modelName = arg.modelName,
                promptVersion = arg.promptVersion,
                tokenUsageInput = arg.tokenUsageInput,
                tokenUsageOutput = arg.tokenUsageOutput,
                rawResponse = arg.rawResponse,
                answerTextHash = arg.answerTextHash,
                createdAt = arg.createdAt
            )
        }.whenever(aiFeedbackRepository).save(any())

        // when
        val result = aiFeedbackService.generateFeedback(answer, question)

        // then
        assertThat(result.modelName).isEqualTo("dummy-model-v1")
        assertThat(result.promptVersion).isEqualTo("v1.0-dummy")
        assertThat(result.overallComment).contains("더미 피드백")

        // AI 클라이언트가 호출되었지만 실패했음을 확인
        org.mockito.kotlin.verify(aiClient).requestFeedback(any(), any())
    }

    @Test
    fun `generateFeedback - 예상치 못한 예외 발생 시 더미 피드백으로 fallback한다`() {
        // given
        val answer = createAnswer("테스트 답변")
        val question = createQuestion()

        whenever(duplicateRequestCache.findCached(question.id, answer.answerText)).thenReturn(null)
        whenever(promptBuilder.buildSystemPrompt(any(), any())).thenThrow(RuntimeException("예상치 못한 오류"))
        whenever(objectMapper.writeValueAsString(any())).thenReturn("[]")

        doAnswer { invocation ->
            val arg = invocation.arguments[0] as AiFeedback
            AiFeedback(
                id = 1L,
                interviewAnswerId = arg.interviewAnswerId,
                logicScore = arg.logicScore,
                specificityScore = arg.specificityScore,
                jobFitScore = arg.jobFitScore,
                deliveryScore = arg.deliveryScore,
                strengths = arg.strengths,
                improvements = arg.improvements,
                modelAnswer = arg.modelAnswer,
                overallComment = arg.overallComment,
                jobField = arg.jobField,
                modelName = arg.modelName,
                promptVersion = arg.promptVersion,
                tokenUsageInput = arg.tokenUsageInput,
                tokenUsageOutput = arg.tokenUsageOutput,
                rawResponse = arg.rawResponse,
                answerTextHash = arg.answerTextHash,
                createdAt = arg.createdAt
            )
        }.whenever(aiFeedbackRepository).save(any())

        // when
        val result = aiFeedbackService.generateFeedback(answer, question)

        // then
        assertThat(result.modelName).isEqualTo("dummy-model-v1")
        assertThat(result.promptVersion).isEqualTo("v1.0-dummy")
    }

    @Test
    fun `generateFeedback - 토큰 사용량을 올바르게 추정한다`() {
        // given
        val answer = createAnswer("x".repeat(400)) // 400자 = 약 100 토큰
        val question = createQuestion()

        val systemPrompt = "x".repeat(80) // 80자 = 약 20 토큰
        val userPrompt = "x".repeat(420) // 420자 = 약 105 토큰 (질문 + 답변)
        val rawResponse = "x".repeat(200) // 200자 = 약 50 토큰

        val parsedFeedback = com.hojun.interviewnote.interviewnoteapi.service.ai.ParsedFeedback(
            logicScore = 4,
            specificityScore = 3,
            jobFitScore = 5,
            deliveryScore = 4,
            strengths = listOf("강점1", "강점2"),
            improvements = listOf("개선1", "개선2"),
            modelAnswer = "x".repeat(400),
            overallComment = "좋습니다"
        )

        whenever(duplicateRequestCache.findCached(any(), any())).thenReturn(null)
        whenever(promptBuilder.buildSystemPrompt(any(), any())).thenReturn(systemPrompt)
        whenever(promptBuilder.buildUserPrompt(any(), any())).thenReturn(userPrompt)
        whenever(aiClient.requestFeedback(any(), any())).thenReturn(rawResponse)
        whenever(responseParser.parseOpenAiResponse(any(), any())).thenReturn(parsedFeedback)
        whenever(duplicateRequestCache.generateHash(any(), any())).thenReturn("hash")
        whenever(objectMapper.writeValueAsString(any())).thenReturn("[]")
        whenever(openAiProperties.model).thenReturn("gpt-4o-mini")
        whenever(openAiProperties.promptVersion).thenReturn("v1.0")

        doAnswer { invocation ->
            val arg = invocation.arguments[0] as AiFeedback
            AiFeedback(
                id = 1L,
                interviewAnswerId = arg.interviewAnswerId,
                logicScore = arg.logicScore,
                specificityScore = arg.specificityScore,
                jobFitScore = arg.jobFitScore,
                deliveryScore = arg.deliveryScore,
                strengths = arg.strengths,
                improvements = arg.improvements,
                modelAnswer = arg.modelAnswer,
                overallComment = arg.overallComment,
                jobField = arg.jobField,
                modelName = arg.modelName,
                promptVersion = arg.promptVersion,
                tokenUsageInput = arg.tokenUsageInput,
                tokenUsageOutput = arg.tokenUsageOutput,
                rawResponse = arg.rawResponse,
                answerTextHash = arg.answerTextHash,
                createdAt = arg.createdAt
            )
        }.whenever(aiFeedbackRepository).save(any())

        // when
        val result = aiFeedbackService.generateFeedback(answer, question)

        // then
        // (80 + 420) / 4 = 125 토큰
        assertThat(result.tokenUsageInput).isEqualTo(125)
        // 200 / 4 = 50 토큰
        assertThat(result.tokenUsageOutput).isEqualTo(50)
    }
}
