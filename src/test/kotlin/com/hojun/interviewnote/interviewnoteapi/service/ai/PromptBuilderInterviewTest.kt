package com.hojun.interviewnote.interviewnoteapi.service.ai

import com.hojun.interviewnote.interviewnoteapi.config.OpenAiProperties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

/**
 * PromptBuilder 면접용 프롬프트 테스트
 *
 * Phase 7B: 5개 면접 메서드 테스트
 * - buildInterviewSystemPrompt
 * - buildInterviewSystemPromptWithJobPosting
 * - buildFirstQuestionPrompt
 * - buildFollowUpPrompt
 * - buildFinalEvaluationPrompt
 */
class PromptBuilderInterviewTest {

    private lateinit var promptBuilder: PromptBuilder
    private val mockProperties: OpenAiProperties = mock()

    @BeforeEach
    fun setUp() {
        promptBuilder = PromptBuilder(mockProperties)
    }

    // ===== buildInterviewSystemPrompt 테스트 =====

    @Test
    fun `buildInterviewSystemPrompt - IT 직무 프롬프트 생성`() {
        // When
        val prompt = promptBuilder.buildInterviewSystemPrompt("IT")

        // Then
        assertTrue(prompt.contains("면접관"))
        assertTrue(prompt.contains("IT"))
        assertTrue(prompt.contains("평가 기준"))
        assertTrue(prompt.contains("logicScore") || prompt.contains("logic"))
        assertTrue(prompt.contains("specificityScore") || prompt.contains("specificity"))
        assertTrue(prompt.contains("deliveryScore") || prompt.contains("delivery"))
        assertTrue(prompt.contains("JSON"))
    }

    @Test
    fun `buildInterviewSystemPrompt - SALES 직무 프롬프트 생성`() {
        // When
        val prompt = promptBuilder.buildInterviewSystemPrompt("SALES")

        // Then
        assertTrue(prompt.contains("면접관"))
        assertTrue(prompt.contains("영업"))
        assertTrue(prompt.contains("고객"))
    }

    @Test
    fun `buildInterviewSystemPrompt - DESIGN 직무 프롬프트 생성`() {
        // When
        val prompt = promptBuilder.buildInterviewSystemPrompt("DESIGN")

        // Then
        assertTrue(prompt.contains("면접관"))
        assertTrue(prompt.contains("디자인"))
    }

    // ===== buildInterviewSystemPromptWithJobPosting 테스트 =====

    @Test
    fun `buildInterviewSystemPromptWithJobPosting - 공고 정보 포함 프롬프트 생성`() {
        // Given
        val companyName = "테크컴퍼니"
        val jobTitle = "백엔드 개발자"
        val jobDescription = "Spring Boot 기반 REST API 개발"
        val requiredSkills = "Java, Spring Boot, MySQL"
        val preferredSkills = "Kotlin, Redis, Docker"

        // When
        val prompt = promptBuilder.buildInterviewSystemPromptWithJobPosting(
            jobField = "IT",
            companyName = companyName,
            jobTitle = jobTitle,
            jobDescription = jobDescription,
            requiredSkills = requiredSkills,
            preferredSkills = preferredSkills
        )

        // Then
        assertTrue(prompt.contains("면접관"))
        assertTrue(prompt.contains(companyName))
        assertTrue(prompt.contains(jobTitle))
        assertTrue(prompt.contains(jobDescription))
        assertTrue(prompt.contains(requiredSkills))
        assertTrue(prompt.contains(preferredSkills))
        assertTrue(prompt.contains("평가 기준"))
        assertTrue(prompt.contains("JSON"))
    }

    @Test
    fun `buildInterviewSystemPromptWithJobPosting - 우대사항 없는 경우 처리`() {
        // Given
        val companyName = "스타트업"
        val jobTitle = "프론트엔드 개발자"
        val jobDescription = "React 기반 웹 개발"
        val requiredSkills = "JavaScript, React"
        val preferredSkills = null

        // When
        val prompt = promptBuilder.buildInterviewSystemPromptWithJobPosting(
            jobField = "IT",
            companyName = companyName,
            jobTitle = jobTitle,
            jobDescription = jobDescription,
            requiredSkills = requiredSkills,
            preferredSkills = preferredSkills
        )

        // Then
        assertTrue(prompt.contains(companyName))
        assertTrue(prompt.contains(jobTitle))
        assertTrue(prompt.contains(requiredSkills))
        assertFalse(prompt.contains("우대사항") || prompt.contains("preferredSkills"))
    }

    // ===== buildFirstQuestionPrompt 테스트 =====

    @Test
    fun `buildFirstQuestionPrompt - 직무 기반 첫 질문 프롬프트`() {
        // When
        val prompt = promptBuilder.buildFirstQuestionPrompt(
            jobField = "백엔드 개발자",
            companyName = null,
            jobTitle = null
        )

        // Then
        assertTrue(prompt.contains("백엔드 개발자"))
        assertTrue(prompt.contains("자기소개"))
        assertTrue(prompt.contains("200자 이내"))
    }

    @Test
    fun `buildFirstQuestionPrompt - 공고 기반 첫 질문 프롬프트`() {
        // Given
        val companyName = "네이버"
        val jobTitle = "시니어 백엔드 개발자"

        // When
        val prompt = promptBuilder.buildFirstQuestionPrompt(
            jobField = "백엔드 개발자",
            companyName = companyName,
            jobTitle = jobTitle
        )

        // Then
        assertTrue(prompt.contains(companyName))
        assertTrue(prompt.contains(jobTitle))
        assertTrue(prompt.contains("자기소개"))
        assertTrue(prompt.contains("200자 이내"))
    }

    // ===== buildFollowUpPrompt 테스트 =====

    @Test
    fun `buildFollowUpPrompt - 대화 히스토리 포함 프롬프트`() {
        // Given
        val conversationHistory = """
            [면접관]: 간단히 자기소개를 해주세요.
            [지원자]: 저는 3년차 백엔드 개발자로 Spring Boot로 RESTful API를 개발해왔습니다.
            [면접관]: 가장 기억에 남는 프로젝트는 무엇인가요?
            [지원자]: 결제 시스템을 개발했던 경험이 가장 기억에 남습니다.
        """.trimIndent()

        // When
        val prompt = promptBuilder.buildFollowUpPrompt(conversationHistory)

        // Then
        assertTrue(prompt.contains(conversationHistory))
        assertTrue(prompt.contains("평가"))
        assertTrue(prompt.contains("다음 질문"))
        assertTrue(prompt.contains("200자 이내"))
        assertTrue(prompt.contains("JSON"))
    }

    @Test
    fun `buildFollowUpPrompt - 빈 대화 히스토리도 처리 가능`() {
        // Given
        val conversationHistory = ""

        // When
        val prompt = promptBuilder.buildFollowUpPrompt(conversationHistory)

        // Then
        assertTrue(prompt.contains("평가"))
        assertTrue(prompt.contains("다음 질문"))
    }

    // ===== buildFinalEvaluationPrompt 테스트 =====

    @Test
    fun `buildFinalEvaluationPrompt - 직무 기반 종합 평가 프롬프트`() {
        // Given
        val allUserAnswers = """
            답변 1: 저는 3년차 백엔드 개발자입니다.
            답변 2: Spring Boot로 결제 시스템을 개발했습니다.
            답변 3: Redis를 활용해 성능을 30% 개선했습니다.
        """.trimIndent()

        // When
        val prompt = promptBuilder.buildFinalEvaluationPrompt(
            jobField = "백엔드 개발자",
            companyName = null,
            jobTitle = null,
            allUserAnswers = allUserAnswers
        )

        // Then
        assertTrue(prompt.contains("백엔드 개발자"))
        assertTrue(prompt.contains(allUserAnswers))
        assertTrue(prompt.contains("종합 평가"))
        assertTrue(prompt.contains("overallFeedback"))
        assertTrue(prompt.contains("keyStrengths"))
        assertTrue(prompt.contains("keyImprovements"))
        assertTrue(prompt.contains("averageScore"))
        assertTrue(prompt.contains("recommendation"))
        assertTrue(prompt.contains("JSON"))
    }

    @Test
    fun `buildFinalEvaluationPrompt - 공고 기반 종합 평가 프롬프트`() {
        // Given
        val companyName = "카카오"
        val jobTitle = "백엔드 개발자"
        val allUserAnswers = """
            답변 1: 저는 Spring 전문 개발자입니다.
            답변 2: MSA 아키텍처 경험이 있습니다.
        """.trimIndent()

        // When
        val prompt = promptBuilder.buildFinalEvaluationPrompt(
            jobField = "백엔드 개발자",
            companyName = companyName,
            jobTitle = jobTitle,
            allUserAnswers = allUserAnswers
        )

        // Then
        assertTrue(prompt.contains(companyName))
        assertTrue(prompt.contains(jobTitle))
        assertTrue(prompt.contains(allUserAnswers))
        assertTrue(prompt.contains("종합 평가"))
    }

    // ===== 일관성 검증 테스트 =====

    @Test
    fun `모든 면접 프롬프트가 JSON 응답 형식을 요구한다`() {
        // When
        val systemPrompt = promptBuilder.buildInterviewSystemPrompt("IT")
        val firstPrompt = promptBuilder.buildFirstQuestionPrompt("IT", null, null)
        val followUpPrompt = promptBuilder.buildFollowUpPrompt("대화 내용")
        val finalPrompt = promptBuilder.buildFinalEvaluationPrompt("IT", null, null, "답변들")

        // Then
        assertTrue(systemPrompt.contains("JSON"))
        assertTrue(firstPrompt.contains("JSON") || systemPrompt.contains("JSON"))
        assertTrue(followUpPrompt.contains("JSON"))
        assertTrue(finalPrompt.contains("JSON"))
    }

    @Test
    fun `면접 질문은 200자 제한을 명시한다`() {
        // When
        val firstPrompt = promptBuilder.buildFirstQuestionPrompt("IT", null, null)
        val followUpPrompt = promptBuilder.buildFollowUpPrompt("대화 내용")

        // Then
        assertTrue(firstPrompt.contains("200자"))
        assertTrue(followUpPrompt.contains("200자"))
    }
}
