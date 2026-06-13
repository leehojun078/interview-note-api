package com.hojun.interviewnote.interviewnoteapi.service.ai

import com.hojun.interviewnote.interviewnoteapi.domain.JobField
import com.hojun.interviewnote.interviewnoteapi.domain.Question
import com.hojun.interviewnote.interviewnoteapi.service.ai.prompt.FeedbackPromptBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime

/**
 * FeedbackPromptBuilder 단위 테스트
 *
 * Phase 2 리팩토링: PromptBuilder에서 분리된 FeedbackPromptBuilder 테스트
 */
class FeedbackPromptBuilderTest {

    private lateinit var feedbackPromptBuilder: FeedbackPromptBuilder

    @BeforeEach
    fun setUp() {
        feedbackPromptBuilder = FeedbackPromptBuilder()
    }

    @Test
    fun `buildSystemPrompt - IT 직무에 대한 시스템 프롬프트를 생성한다`() {
        // Given
        val jobField = "IT"
        val targetJob = "백엔드 개발자"

        // When
        val result = feedbackPromptBuilder.buildSystemPrompt(jobField, targetJob)

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
            val result = feedbackPromptBuilder.buildSystemPrompt("IT", targetJob)
            assertTrue(result.contains(targetJob), "프롬프트에 직무명 '$targetJob'이 포함되어야 함")
        }
    }

    @Test
    fun `buildSystemPrompt - 지원하지 않는 직무 분야에 대해 예외를 발생시킨다`() {
        // Given
        val unsupportedJobField = "UNKNOWN"

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            feedbackPromptBuilder.buildSystemPrompt(unsupportedJobField, "미지의 직무")
        }
        assertTrue(exception.message!!.contains("지원하지 않는 직무 분야"))
        assertTrue(exception.message!!.contains("UNKNOWN"))
    }

    @Test
    fun `buildSystemPrompt - JSON 스키마가 포함되어야 한다`() {
        // Given
        val jobField = "IT"
        val targetJob = "백엔드 개발자"

        // When
        val result = feedbackPromptBuilder.buildSystemPrompt(jobField, targetJob)

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
        val result = feedbackPromptBuilder.buildSystemPrompt(jobField, targetJob)

        // Then
        assertTrue(result.contains("기술적 사고"))
        assertTrue(result.contains("논리적 흐름"))
        assertTrue(result.contains("구체적 기술 스택"))
        assertTrue(result.contains("직무 연관성"))
        assertTrue(result.contains("명확하고 이해하기 쉽게"))
    }

    @Test
    fun `buildSystemPrompt - 출력 규칙이 명시되어야 한다`() {
        // Given
        val jobField = "IT"
        val targetJob = "백엔드 개발자"

        // When
        val result = feedbackPromptBuilder.buildSystemPrompt(jobField, targetJob)

        // Then
        assertTrue(result.contains("0-5개"))  // strengths는 0개도 가능
        assertTrue(result.contains("1-5개 필수"))  // improvements는 최소 1개
        assertTrue(result.contains("400-600자"))
        assertTrue(result.contains("과도한 단정이나 공격적 표현 금지"))
    }

    @Test
    fun `buildSystemPrompt - AI Hallucination 방지 지침이 포함되어야 한다`() {
        // Given
        val jobField = "IT"
        val targetJob = "백엔드 개발자"

        // When
        val result = feedbackPromptBuilder.buildSystemPrompt(jobField, targetJob)

        // Then
        assertTrue(result.contains("정직한 평가"))
        assertTrue(result.contains("사실 기반"))
        assertTrue(result.contains("강점 검증"))
        assertTrue(result.contains("답변에 없는 내용을 추측하거나 창작하지"))
        assertTrue(result.contains("억지로 만들지 말고"))
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
        val result = feedbackPromptBuilder.buildUserPrompt(question, answer)

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
        val result = feedbackPromptBuilder.buildUserPrompt(question, longAnswer)

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
        val result = feedbackPromptBuilder.buildUserPrompt(question, answer)

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
        val result = feedbackPromptBuilder.buildUserPrompt(question, answer)

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
        val systemPrompt = feedbackPromptBuilder.buildSystemPrompt(question.jobField, question.targetJob)
        val userPrompt = feedbackPromptBuilder.buildUserPrompt(question, answer)

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
        val result = feedbackPromptBuilder.buildSystemPrompt(jobField, targetJob)

        // Then
        // 너무 짧거나 너무 길지 않은지 확인 (200자 이상, 2000자 이하)
        assertTrue(result.length in 200..2000,
            "프롬프트 길이가 적절해야 함 (현재: ${result.length}자)")
    }

    // ===== Phase 5: 17개 직무 지원 테스트 =====

    @ParameterizedTest
    @EnumSource(JobField::class)
    fun `모든 직무에 대해 시스템 프롬프트를 생성할 수 있다`(jobField: JobField) {
        // When
        val prompt = feedbackPromptBuilder.buildSystemPrompt(jobField.name, "${jobField.displayName} 담당자")

        // Then
        assertTrue(prompt.isNotBlank())
        assertTrue(prompt.contains("면접 코치"))
        assertTrue(prompt.contains("논리성"))
        assertTrue(prompt.contains("구체성"))
        assertTrue(prompt.contains("직무 적합성"))
        assertTrue(prompt.contains("전달력"))
        assertTrue(prompt.contains("JSON 형식"))
    }

    @ParameterizedTest
    @EnumSource(JobField::class)
    fun `모든 직무의 프롬프트는 2000자 이하이다`(jobField: JobField) {
        // When
        val prompt = feedbackPromptBuilder.buildSystemPrompt(jobField.name, "${jobField.displayName} 담당자")

        // Then
        assertTrue(prompt.length <= 2000,
            "${jobField.displayName} 프롬프트 길이가 2000자를 초과함 (현재: ${prompt.length}자)")
    }

    @Test
    fun `영업 직무 프롬프트는 영업 관련 키워드를 포함한다`() {
        // When
        val prompt = feedbackPromptBuilder.buildSystemPrompt("SALES", "영업관리자")

        // Then
        assertTrue(prompt.contains("영업관리자"))
        assertTrue(prompt.contains("실적"))
    }

    @Test
    fun `회계 직무 프롬프트는 재무 관련 키워드를 포함한다`() {
        // When
        val prompt = feedbackPromptBuilder.buildSystemPrompt("ACCOUNTING", "회계담당자")

        // Then
        assertTrue(prompt.contains("회계담당자"))
        assertTrue(prompt.contains("재무"))
    }

    @Test
    fun `마케팅 직무 프롬프트는 마케팅 관련 키워드를 포함한다`() {
        // When
        val prompt = feedbackPromptBuilder.buildSystemPrompt("MARKETING", "마케팅매니저")

        // Then
        assertTrue(prompt.contains("마케팅매니저"))
        assertTrue(prompt.contains("캠페인"))
    }

    @Test
    fun `HR 직무 프롬프트는 인사 관련 키워드를 포함한다`() {
        // When
        val prompt = feedbackPromptBuilder.buildSystemPrompt("HR", "인사담당자")

        // Then
        assertTrue(prompt.contains("인사담당자"))
        assertTrue(prompt.contains("채용"))
    }

    @Test
    fun `디자인 직무 프롬프트는 디자인 관련 키워드를 포함한다`() {
        // When
        val prompt = feedbackPromptBuilder.buildSystemPrompt("DESIGN", "UI/UX 디자이너")

        // Then
        assertTrue(prompt.contains("UI/UX 디자이너"))
        assertTrue(prompt.contains("디자인"))
    }

    @Test
    fun `의료 직무 프롬프트는 의료 관련 키워드를 포함한다`() {
        // When
        val prompt = feedbackPromptBuilder.buildSystemPrompt("MEDICAL", "간호사")

        // Then
        assertTrue(prompt.contains("간호사"))
        assertTrue(prompt.contains("환자"))
    }

    @Test
    fun `교육 직무 프롬프트는 교육 관련 키워드를 포함한다`() {
        // When
        val prompt = feedbackPromptBuilder.buildSystemPrompt("EDUCATION", "교사")

        // Then
        assertTrue(prompt.contains("교사"))
        assertTrue(prompt.contains("교육"))
    }

    @Test
    fun `금융 직무 프롬프트는 금융 관련 키워드를 포함한다`() {
        // When
        val prompt = feedbackPromptBuilder.buildSystemPrompt("FINANCE", "금융상담사")

        // Then
        assertTrue(prompt.contains("금융상담사"))
        assertTrue(prompt.contains("상품"))
    }

    @Test
    fun `기획 직무 프롬프트는 기획 관련 키워드를 포함한다`() {
        // When
        val prompt = feedbackPromptBuilder.buildSystemPrompt("PLANNING", "경영기획자")

        // Then
        assertTrue(prompt.contains("경영기획자"))
        assertTrue(prompt.contains("전략") || prompt.contains("기획"))
    }

    @Test
    fun `각 직무별 프롬프트는 서로 다른 평가 기준을 가진다`() {
        // When
        val itPrompt = feedbackPromptBuilder.buildSystemPrompt("IT", "개발자")
        val salesPrompt = feedbackPromptBuilder.buildSystemPrompt("SALES", "영업사원")
        val accountingPrompt = feedbackPromptBuilder.buildSystemPrompt("ACCOUNTING", "회계사")

        // Then - IT는 기술 관련
        assertTrue(itPrompt.contains("기술"))
        assertTrue(!itPrompt.contains("실적"))
        assertTrue(!itPrompt.contains("재무"))

        // Then - 영업은 실적 관련
        assertTrue(salesPrompt.contains("실적"))
        assertTrue(!salesPrompt.contains("기술 스택"))
        assertTrue(!salesPrompt.contains("재무 지표"))

        // Then - 회계는 재무 관련
        assertTrue(accountingPrompt.contains("재무"))
        assertTrue(!accountingPrompt.contains("기술 스택"))
        assertTrue(!accountingPrompt.contains("실적 수치"))
    }

    @Test
    fun `모든 프롬프트는 공통 평가 지침을 포함한다`() {
        // When
        val itPrompt = feedbackPromptBuilder.buildSystemPrompt("IT", "개발자")
        val salesPrompt = feedbackPromptBuilder.buildSystemPrompt("SALES", "영업사원")
        val medicalPrompt = feedbackPromptBuilder.buildSystemPrompt("MEDICAL", "간호사")

        // Then - 공통 지침
        val commonKeywords = listOf(
            "정직한 평가",
            "사실 기반",
            "강점 검증",
            "반복 표현",
            "내용 부족",
            "엄격한 기준",
            "strengths",
            "improvements",
            "modelAnswer",
            "overallComment"
        )

        commonKeywords.forEach { keyword ->
            assertTrue(itPrompt.contains(keyword), "IT 프롬프트에 '$keyword'가 없음")
            assertTrue(salesPrompt.contains(keyword), "영업 프롬프트에 '$keyword'가 없음")
            assertTrue(medicalPrompt.contains(keyword), "의료 프롬프트에 '$keyword'가 없음")
        }
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
        val result = feedbackPromptBuilder.buildUserPrompt(question, emptyAnswer)

        // Then
        assertTrue(result.contains("면접 질문"))
        assertTrue(result.contains(question.content))
        assertTrue(result.contains("지원자 답변"))
    }
}
