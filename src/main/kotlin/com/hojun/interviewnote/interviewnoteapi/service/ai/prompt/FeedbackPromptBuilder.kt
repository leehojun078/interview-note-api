package com.hojun.interviewnote.interviewnoteapi.service.ai.prompt

import com.hojun.interviewnote.interviewnoteapi.domain.Question
import org.springframework.stereotype.Service

/**
 * 답변 평가용 프롬프트 생성기
 *
 * Phase 2 리팩토링: PromptBuilder에서 분리
 * 역할: 사용자 답변에 대한 AI 피드백 프롬프트 생성
 */
@Service
class FeedbackPromptBuilder {

    /**
     * 직무에 따른 시스템 프롬프트 생성
     *
     * @param jobField 직무 분야 (IT, SALES, HR 등 17개)
     * @param targetJob 목표 직무 (예: "백엔드 개발자")
     * @return 시스템 프롬프트 문자열
     */
    fun buildSystemPrompt(jobField: String, targetJob: String): String {
        val config = JobFieldConfigs.feedbackConfigs[jobField]
            ?: throw IllegalArgumentException("지원하지 않는 직무 분야입니다: $jobField")
        return buildBasePrompt(targetJob, config.logicDescription, config.specificityDescription, config.badExamples)
    }

    /**
     * 질문과 답변을 포함한 사용자 프롬프트 생성
     *
     * @param question 면접 질문
     * @param answer 사용자 답변
     * @return 사용자 프롬프트 문자열
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

    /**
     * 공통 프롬프트 구조 (중복 방지)
     *
     * @param targetJob 목표 직무
     * @param logicDescription 논리성 평가 기준 설명
     * @param specificityDescription 구체성 평가 기준 설명
     * @param badExamples 나쁜 답변 예시 목록
     * @return 시스템 프롬프트 문자열
     */
    private fun buildBasePrompt(
        targetJob: String,
        logicDescription: String,
        specificityDescription: String,
        badExamples: List<String>
    ): String {
        val badExamplesText = badExamples.joinToString("\n            - ")
        return """
            당신은 ${targetJob} 면접을 준비하는 지원자를 돕는 면접 코치입니다.
            당신의 역할은 합격/불합격을 판정하는 것이 아니라, 답변을 개선하도록 구체적인 피드백을 제공하는 것입니다.

            평가 기준:
            - 논리성(logic): $logicDescription (1-5점)
            - 구체성(specificity): $specificityDescription (1-5점)
            - 직무 적합성(jobFit): 질문 의도와 직무 연관성 (1-5점)
            - 전달력(delivery): 명확하고 이해하기 쉽게 설명하는 능력 (1-5점)

            중요한 평가 지침:
            1. **정직한 평가**: 답변이 반복적이거나 무의미하면 솔직하게 지적하세요
            2. **사실 기반**: 답변에 없는 내용을 추측하거나 창작하지 마세요
            3. **강점 검증**: 실제로 답변에 나타난 강점만 언급하세요
            4. **반복 표현 감지**: 같은 단어/문구가 반복되면 improvements에 "반복 표현을 피하고 구체적인 경험과 예시를 들어 설명"을 포함하세요
            5. **내용 부족 시**: 답변이 짧거나 구체성이 부족하면 strengths를 억지로 만들지 말고, improvements를 더 구체적으로 작성하세요
            6. **엄격한 기준**: 형식적이거나 추상적인 답변은 낮은 점수를 주세요

            나쁜 답변 예시:
            - 반복 표현: "저는 중요하게 여기는 여기는 여기는..."
            - $badExamplesText

            이런 경우:
            - strengths: 가능한 한 적게 (또는 비어있어도 됨)
            - improvements: 구체적이고 실질적인 개선 방향 제시
            - 점수: 1-2점 (매우 낮게)

            출력 규칙:
            - 반드시 JSON 형식으로 응답
            - 각 점수는 1-5 사이 정수
            - strengths는 0-5개 (답변이 부실하면 0개도 가능)
            - improvements는 1-5개 필수 (최소 1개, 내용이 부실하면 많게)
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
}
