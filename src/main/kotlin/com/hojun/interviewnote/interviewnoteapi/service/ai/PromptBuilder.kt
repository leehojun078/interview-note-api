package com.hojun.interviewnote.interviewnoteapi.service.ai

import com.hojun.interviewnote.interviewnoteapi.config.OpenAiProperties
import com.hojun.interviewnote.interviewnoteapi.domain.Question
import org.springframework.stereotype.Service

/**
 * AI 평가를 위한 프롬프트 생성기
 *
 * 직무(jobField)별로 다른 시스템 프롬프트를 생성하여
 * 평가 기준을 직무에 맞게 조정할 수 있습니다.
 *
 * MVP에서는 IT 직무만 지원하며, 향후 확장 가능한 구조로 설계되었습니다.
 */
@Service
class PromptBuilder(
    private val properties: OpenAiProperties
) {

    companion object {
        private const val IT_JOB_FIELD = "IT"
    }

    /**
     * 직무에 따른 시스템 프롬프트 생성
     * MVP: IT 직무만 구현, 향후 확장 가능
     */
    fun buildSystemPrompt(jobField: String, targetJob: String): String {
        return when (jobField) {
            IT_JOB_FIELD -> buildItSystemPrompt(targetJob)
            else -> throw IllegalArgumentException("지원하지 않는 직무 분야입니다: $jobField")
        }
    }

    /**
     * IT 직무용 시스템 프롬프트 (CLAUDE.md 스펙 준수)
     */
    private fun buildItSystemPrompt(targetJob: String): String {
        return """
            당신은 ${targetJob} 면접을 준비하는 지원자를 돕는 면접 코치입니다.
            당신의 역할은 합격/불합격을 판정하는 것이 아니라, 답변을 개선하도록 구체적인 피드백을 제공하는 것입니다.

            평가 기준:
            - 논리성(logic): 기술적 사고의 논리적 흐름과 일관성 (1-5점)
            - 구체성(specificity): 구체적 기술 스택, 사례, 수치 제시 정도 (1-5점)
            - 직무 적합성(jobFit): 질문 의도와 개발 직무 연관성 (1-5점)
            - 전달력(delivery): 기술 개념을 명확하고 이해하기 쉽게 설명하는 능력 (1-5점)

            출력 규칙:
            - 반드시 JSON 형식으로 응답
            - 각 점수는 1-5 사이 정수
            - strengths와 improvements는 각각 2-3개 항목
            - modelAnswer는 400-600자 이내
            - 한국어로 답변
            - 과도한 단정이나 공격적 표현 금지

            JSON 형식:
            {
              "scores": {
                "logic": 4,
                "specificity": 3,
                "jobFit": 4,
                "delivery": 3
              },
              "strengths": ["강점1", "강점2"],
              "improvements": ["개선점1", "개선점2"],
              "modelAnswer": "모범답변 내용...",
              "overallComment": "종합 코멘트"
            }
        """.trimIndent()
    }

    /**
     * 질문과 답변을 포함한 사용자 프롬프트 생성
     */
    fun buildUserPrompt(question: Question, answer: String): String {
        return """
            면접 질문:
            ${question.content}

            지원자 답변:
            $answer

            위 답변을 평가하고, JSON 형식으로 피드백을 제공해주세요.
        """.trimIndent()
    }
}
