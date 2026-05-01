package com.hojun.interviewnote.interviewnoteapi.service

import com.hojun.interviewnote.interviewnoteapi.domain.InterviewMessage
import com.hojun.interviewnote.interviewnoteapi.domain.JobPosting
import com.hojun.interviewnote.interviewnoteapi.domain.MessageSender
import com.hojun.interviewnote.interviewnoteapi.domain.MockInterview
import com.hojun.interviewnote.interviewnoteapi.service.ai.AiClient
import com.hojun.interviewnote.interviewnoteapi.service.ai.AiInterviewResponse
import com.hojun.interviewnote.interviewnoteapi.service.ai.FinalEvaluationResult
import com.hojun.interviewnote.interviewnoteapi.service.ai.InterviewEvaluation
import com.hojun.interviewnote.interviewnoteapi.service.ai.InterviewResponseParser
import com.hojun.interviewnote.interviewnoteapi.service.ai.PromptBuilder
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 면접 AI 서비스
 *
 * Phase 7B: 실시간 채팅 면접 AI 통합
 * - 첫 질문 생성 (동기)
 * - 꼬리 질문 생성 (평가 포함)
 * - 종합 평가 생성
 */
@Service
class InterviewAiService(
    private val aiClient: AiClient,
    private val promptBuilder: PromptBuilder,
    private val interviewResponseParser: InterviewResponseParser,
    private val meterRegistry: MeterRegistry
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val interviewAiCallsCounter by lazy {
        meterRegistry.counter("interview.ai.calls.total")
    }
    private val interviewAiTimer by lazy {
        meterRegistry.timer("interview.ai.calls.duration")
    }

    /**
     * 첫 질문 생성
     *
     * Phase 7B: 면접 시작 시 자기소개 요청 질문 생성 (동기)
     *
     * @param interview 면접 세션
     * @param jobPosting 채용 공고 (nullable)
     * @return AI 질문
     */
    fun generateFirstQuestion(
        interview: MockInterview,
        jobPosting: JobPosting?
    ): AiInterviewResponse {
        logger.info("첫 질문 생성 시작 - interviewId: ${interview.id}")

        val systemPrompt = if (jobPosting != null) {
            promptBuilder.buildInterviewSystemPromptWithJobPosting(
                jobField = interview.selectedJobField.name,
                companyName = jobPosting.companyName,
                jobTitle = jobPosting.jobTitle,
                jobDescription = jobPosting.jobDescription,
                requiredSkills = jobPosting.requiredSkills,
                preferredSkills = jobPosting.preferredSkills
            )
        } else {
            promptBuilder.buildInterviewSystemPrompt(interview.selectedJobField.name)
        }

        val userPrompt = promptBuilder.buildFirstQuestionPrompt(
            jobField = interview.selectedJobField.displayName,
            companyName = jobPosting?.companyName,
            jobTitle = jobPosting?.jobTitle
        )

        return callAiForQuestion(systemPrompt, userPrompt, "첫 질문")
    }

    /**
     * 꼬리 질문 생성 (답변 평가 포함)
     *
     * Phase 7B: 대화 히스토리 기반 다음 질문 + 평가
     *
     * @param interview 면접 세션
     * @param conversation 대화 히스토리
     * @param jobPosting 채용 공고 (nullable)
     * @return 평가 + 다음 질문
     */
    fun generateFollowUpQuestion(
        interview: MockInterview,
        conversation: List<InterviewMessage>,
        jobPosting: JobPosting?
    ): Pair<InterviewEvaluation, AiInterviewResponse> {
        logger.info("꼬리 질문 생성 시작 - interviewId: ${interview.id}")

        val systemPrompt = if (jobPosting != null) {
            promptBuilder.buildInterviewSystemPromptWithJobPosting(
                jobField = interview.selectedJobField.name,
                companyName = jobPosting.companyName,
                jobTitle = jobPosting.jobTitle,
                jobDescription = jobPosting.jobDescription,
                requiredSkills = jobPosting.requiredSkills,
                preferredSkills = jobPosting.preferredSkills
            )
        } else {
            promptBuilder.buildInterviewSystemPrompt(interview.selectedJobField.name)
        }

        val conversationHistory = buildConversationHistory(conversation)
        val userPrompt = promptBuilder.buildFollowUpPrompt(conversationHistory)

        return interviewAiTimer.recordCallable {
            interviewAiCallsCounter.increment()

            val rawResponse = aiClient.requestFeedback(systemPrompt, userPrompt)
            val parsed = interviewResponseParser.parseInterviewResponse(rawResponse)

            logger.info(
                "꼬리 질문 생성 완료 - interviewId: ${interview.id}, " +
                        "isFollowUp: ${parsed.nextAction.isFollowUp}"
            )

            Pair(
                parsed.evaluation,
                AiInterviewResponse(
                    question = parsed.nextAction.question,
                    reasoning = parsed.nextAction.reasoning
                )
            )
        }!!
    }

    /**
     * 종합 평가 생성
     *
     * Phase 7B: 면접 종료 시 전체 대화 기반 종합 평가
     *
     * @param interview 면접 세션
     * @param conversation 전체 대화 히스토리
     * @param jobPosting 채용 공고 (nullable)
     * @return 종합 평가
     */
    fun generateFinalEvaluation(
        interview: MockInterview,
        conversation: List<InterviewMessage>,
        jobPosting: JobPosting?
    ): FinalEvaluationResult {
        logger.info("종합 평가 생성 시작 - interviewId: ${interview.id}")

        val systemPrompt = """
            당신은 면접 평가 전문가입니다.
            전체 면접 대화를 기반으로 종합 평가를 작성하세요.
            JSON 형식으로 응답하세요.
        """.trimIndent()

        val allUserAnswers = conversation
            .filter { it.sender == MessageSender.USER }
            .mapIndexed { idx, msg -> "답변 ${idx + 1}: ${msg.content}" }
            .joinToString("\n")

        val userPrompt = promptBuilder.buildFinalEvaluationPrompt(
            jobField = interview.selectedJobField.displayName,
            companyName = jobPosting?.companyName,
            jobTitle = jobPosting?.jobTitle,
            allUserAnswers = allUserAnswers
        )

        return interviewAiTimer.recordCallable {
            interviewAiCallsCounter.increment()

            val rawResponse = aiClient.requestFeedback(systemPrompt, userPrompt)
            val result = interviewResponseParser.parseFinalEvaluation(rawResponse)

            logger.info(
                "종합 평가 생성 완료 - interviewId: ${interview.id}, " +
                        "averageScore: ${result.averageScore}"
            )

            result
        }!!
    }

    /**
     * AI 호출 (질문만)
     *
     * 첫 질문 생성 시 사용
     */
    private fun callAiForQuestion(
        systemPrompt: String,
        userPrompt: String,
        logContext: String
    ): AiInterviewResponse {
        return interviewAiTimer.recordCallable {
            interviewAiCallsCounter.increment()

            val rawResponse = aiClient.requestFeedback(systemPrompt, userPrompt)
            val parsed = interviewResponseParser.parseInterviewResponse(rawResponse)

            logger.info("$logContext 생성 완료")

            AiInterviewResponse(
                question = parsed.nextAction.question,
                reasoning = parsed.nextAction.reasoning
            )
        }!!
    }

    /**
     * 대화 히스토리 포맷
     *
     * AI에게 전달할 대화 내용 문자열로 변환
     */
    private fun buildConversationHistory(messages: List<InterviewMessage>): String {
        return messages.joinToString("\n") { msg ->
            val role = if (msg.sender == MessageSender.AI) "면접관" else "지원자"
            "[$role]: ${msg.content}"
        }
    }
}
