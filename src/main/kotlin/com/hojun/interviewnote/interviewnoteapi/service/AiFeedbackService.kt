package com.hojun.interviewnote.interviewnoteapi.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.hojun.interviewnote.interviewnoteapi.domain.AiFeedback
import com.hojun.interviewnote.interviewnoteapi.domain.InterviewAnswer
import com.hojun.interviewnote.interviewnoteapi.domain.Question
import com.hojun.interviewnote.interviewnoteapi.repository.AiFeedbackRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class AiFeedbackService(
    private val aiFeedbackRepository: AiFeedbackRepository
) {
    private val objectMapper = jacksonObjectMapper()

    /**
     * Phase 1: 더미 AI 피드백 생성
     * Phase 2에서 실제 OpenAI API로 교체 예정
     */
    fun generateDummyFeedback(answer: InterviewAnswer, question: Question): AiFeedback {
        val answerLength = answer.answerText.length

        // 답변 길이에 따라 점수 조정 (간단한 로직)
        val baseScore = when {
            answerLength >= 500 -> 4
            answerLength >= 300 -> 3
            else -> 2
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
            tokenUsageInput = answerLength / 4, // 대략적인 토큰 수
            tokenUsageOutput = modelAnswer.length / 4,
            rawResponse = "{\"dummy\": true, \"message\": \"This is a dummy response for Phase 1\"}",
            createdAt = LocalDateTime.now()
        )

        return aiFeedbackRepository.save(aiFeedback)
    }

    fun findByInterviewAnswerId(interviewAnswerId: Long): AiFeedback? {
        return aiFeedbackRepository.findByInterviewAnswerId(interviewAnswerId)
    }
}
