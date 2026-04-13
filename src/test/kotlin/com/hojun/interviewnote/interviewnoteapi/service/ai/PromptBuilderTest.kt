package com.hojun.interviewnote.interviewnoteapi.service.ai

import com.hojun.interviewnote.interviewnoteapi.config.OpenAiProperties
import com.hojun.interviewnote.interviewnoteapi.domain.Question
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

/**
 * PromptBuilder 단위 테스트
 *
 * 시스템 프롬프트와 사용자 프롬프트 생성 로직을 테스트합니다.
 */
class PromptBuilderTest {

    private lateinit var promptBuilder: PromptBuilder
    private lateinit var properties: OpenAiProperties

    @BeforeEach
    fun setUp() {
        properties = OpenAiProperties().apply {
            model = "gpt-4o-mini"
            promptVersion = "v1.0"
            maxTokens = 800
            temperature = 0.7
        }
        promptBuilder = PromptBuilder(properties)
    }

    @Test
    fun `buildSystemPrompt - IT 직무에 대한 시스템 프롬프트를 생성한다`() {
        // Given
        val jobField = "IT"
        val targetJob = "백엔드 개발자"

        // When
        val result = promptBuilder.buildSystemPrompt(jobField, targetJob)

        // Then
        assertTrue(result.contains("백엔드 개발자"))
        assertTrue(result.contains("논리성(logic)"))
        assertTrue(result.contains("구체성(specificity)"))
        assertTrue(result.contains("직무 적합성(jobFit)"))
        assertTrue(result.contains("전달력(delivery)"))
        assertTrue(result.contains("JSON 형식"))
        assertTrue(result.contains("1-5"))
        assertTrue(result.contains("한국어"))
    }

    @Test
    fun `buildSystemPrompt - 다양한 IT 직무에 대해 올바른 프롬프트를 생성한다`() {
        // Given
        val testCases = listOf(
            "프론트엔드 개발자",
            "백엔드 개발자",
            "풀스택 개발자",
            "데브옵스 엔지니어",
            "데이터 엔지니어"
        )

        // When & Then
        testCases.forEach { targetJob ->
            val result = promptBuilder.buildSystemPrompt("IT", targetJob)
            assertTrue(result.contains(targetJob), "프롬프트에 직무명 '$targetJob'이 포함되어야 함")
        }
    }

    @Test
    fun `buildSystemPrompt - 지원하지 않는 직무 분야에 대해 예외를 발생시킨다`() {
        // Given
        val unsupportedJobField = "영업"

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            promptBuilder.buildSystemPrompt(unsupportedJobField, "영업관리자")
        }
        assertTrue(exception.message!!.contains("지원하지 않는 직무 분야"))
        assertTrue(exception.message!!.contains("영업"))
    }

    @Test
    fun `buildSystemPrompt - JSON 스키마가 포함되어야 한다`() {
        // Given
        val jobField = "IT"
        val targetJob = "백엔드 개발자"

        // When
        val result = promptBuilder.buildSystemPrompt(jobField, targetJob)

        // Then
        assertTrue(result.contains("\"scores\""))
        assertTrue(result.contains("\"logic\""))
        assertTrue(result.contains("\"specificity\""))
        assertTrue(result.contains("\"jobFit\""))
        assertTrue(result.contains("\"delivery\""))
        assertTrue(result.contains("\"strengths\""))
        assertTrue(result.contains("\"improvements\""))
        assertTrue(result.contains("\"modelAnswer\""))
        assertTrue(result.contains("\"overallComment\""))
    }

    @Test
    fun `buildSystemPrompt - 평가 기준 설명이 포함되어야 한다`() {
        // Given
        val jobField = "IT"
        val targetJob = "백엔드 개발자"

        // When
        val result = promptBuilder.buildSystemPrompt(jobField, targetJob)

        // Then
        assertTrue(result.contains("기술적 사고"))
        assertTrue(result.contains("논리적 흐름"))
        assertTrue(result.contains("구체적 기술 스택"))
        assertTrue(result.contains("개발 직무 연관성"))
        assertTrue(result.contains("명확하고 이해하기 쉽게"))
    }

    @Test
    fun `buildSystemPrompt - 출력 규칙이 명시되어야 한다`() {
        // Given
        val jobField = "IT"
        val targetJob = "백엔드 개발자"

        // When
        val result = promptBuilder.buildSystemPrompt(jobField, targetJob)

        // Then
        assertTrue(result.contains("2-3개 항목"))
        assertTrue(result.contains("400-600자"))
        assertTrue(result.contains("과도한 단정이나 공격적 표현 금지"))
    }

    @Test
    fun `buildUserPrompt - 질문과 답변을 포함한 사용자 프롬프트를 생성한다`() {
        // Given
        val question = Question(
            id = 1L,
            jobField = "IT",
            targetJob = "백엔드 개발자",
            category = "기술역량",
            content = "Spring Boot의 장점을 설명해주세요.",
            difficulty = "MEDIUM",
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val answer = "Spring Boot는 설정이 간편하고 자동 설정 기능이 있습니다."

        // When
        val result = promptBuilder.buildUserPrompt(question, answer)

        // Then
        assertTrue(result.contains("면접 질문"))
        assertTrue(result.contains(question.content))
        assertTrue(result.contains("지원자 답변"))
        assertTrue(result.contains(answer))
        assertTrue(result.contains("평가하고"))
        assertTrue(result.contains("JSON 형식"))
    }

    @Test
    fun `buildUserPrompt - 긴 답변도 올바르게 포함한다`() {
        // Given
        val question = Question(
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
        val longAnswer = "답변 ".repeat(500) // 3000자

        // When
        val result = promptBuilder.buildUserPrompt(question, longAnswer)

        // Then
        assertTrue(result.contains(longAnswer))
    }

    @Test
    fun `buildUserPrompt - 특수문자가 포함된 질문과 답변을 올바르게 처리한다`() {
        // Given
        val question = Question(
            id = 1L,
            jobField = "IT",
            targetJob = "백엔드 개발자",
            category = "기술역량",
            content = "\"REST API\"와 'GraphQL'의 차이점은?",
            difficulty = "MEDIUM",
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val answer = "REST는 \"Resource\"를 중심으로, GraphQL은 '쿼리'를 중심으로 설계되었습니다."

        // When
        val result = promptBuilder.buildUserPrompt(question, answer)

        // Then
        assertTrue(result.contains(question.content))
        assertTrue(result.contains(answer))
        assertTrue(result.contains("\""))
        assertTrue(result.contains("'"))
    }

    @Test
    fun `buildUserPrompt - 개행 문자가 포함된 답변을 올바르게 처리한다`() {
        // Given
        val question = Question(
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
        val answer = """
            첫 번째 단락입니다.

            두 번째 단락입니다.
            세 번째 줄입니다.
        """.trimIndent()

        // When
        val result = promptBuilder.buildUserPrompt(question, answer)

        // Then
        assertTrue(result.contains(answer))
    }

    @Test
    fun `buildSystemPrompt와 buildUserPrompt를 함께 사용한 전체 프롬프트 생성`() {
        // Given
        val question = Question(
            id = 1L,
            jobField = "IT",
            targetJob = "백엔드 개발자",
            category = "기술역량",
            content = "Spring Boot의 장점을 설명해주세요.",
            difficulty = "MEDIUM",
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val answer = "Spring Boot는 설정이 간편합니다."

        // When
        val systemPrompt = promptBuilder.buildSystemPrompt(question.jobField, question.targetJob)
        val userPrompt = promptBuilder.buildUserPrompt(question, answer)

        // Then
        // System prompt 검증
        assertTrue(systemPrompt.isNotEmpty())
        assertTrue(systemPrompt.contains("백엔드 개발자"))

        // User prompt 검증
        assertTrue(userPrompt.isNotEmpty())
        assertTrue(userPrompt.contains(question.content))
        assertTrue(userPrompt.contains(answer))

        // 두 프롬프트가 서로 다른지 확인
        assertTrue(systemPrompt != userPrompt)
    }

    @Test
    fun `buildSystemPrompt - 프롬프트 길이가 적절한지 확인`() {
        // Given
        val jobField = "IT"
        val targetJob = "백엔드 개발자"

        // When
        val result = promptBuilder.buildSystemPrompt(jobField, targetJob)

        // Then
        // 너무 짧거나 너무 길지 않은지 확인 (200자 이상, 2000자 이하)
        assertTrue(result.length in 200..2000,
            "프롬프트 길이가 적절해야 함 (현재: ${result.length}자)")
    }

    @Test
    fun `buildUserPrompt - 빈 답변도 올바르게 처리한다`() {
        // Given
        val question = Question(
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
        val emptyAnswer = ""

        // When
        val result = promptBuilder.buildUserPrompt(question, emptyAnswer)

        // Then
        assertTrue(result.contains("면접 질문"))
        assertTrue(result.contains(question.content))
        assertTrue(result.contains("지원자 답변"))
    }
}
