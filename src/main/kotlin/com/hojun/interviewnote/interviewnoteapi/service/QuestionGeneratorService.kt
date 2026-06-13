package com.hojun.interviewnote.interviewnoteapi.service

import com.hojun.interviewnote.interviewnoteapi.domain.GeneratedQuestion
import com.hojun.interviewnote.interviewnoteapi.domain.JobField
import com.hojun.interviewnote.interviewnoteapi.domain.JobPosting
import com.hojun.interviewnote.interviewnoteapi.exception.AiException
import com.hojun.interviewnote.interviewnoteapi.service.ai.AiClient
import com.hojun.interviewnote.interviewnoteapi.service.ai.prompt.QuestionPromptBuilder
import com.hojun.interviewnote.interviewnoteapi.service.ai.QuestionResponseParser
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

/**
 * AI 기반 질문 생성 서비스
 *
 * Phase 6B에서 추가됨
 * - 채용 공고 기반 맞춤형 질문 10개 생성
 * - AI 실패 시 Fallback 질문 제공
 * - Micrometer 메트릭 수집
 */
@Service
class QuestionGeneratorService(
    private val aiClient: AiClient,
    private val questionPromptBuilder: QuestionPromptBuilder,
    private val questionResponseParser: QuestionResponseParser,
    private val meterRegistry: MeterRegistry
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        // Fallback 질문 (AI 실패 시)
        private val FALLBACK_QUESTIONS = listOf(
            Triple("자기소개와 지원 동기를 말씀해주세요.", "직무역량", "EASY"),
            Triple("이 직무에 지원하게 된 계기는 무엇인가요?", "직무역량", "EASY"),
            Triple("본인의 강점과 약점을 말씀해주세요.", "직무역량", "EASY"),
            Triple("과거 프로젝트나 업무 경험 중 가장 기억에 남는 것은?", "문제해결", "MEDIUM"),
            Triple("팀 내 갈등 상황을 경험한 적이 있나요? 어떻게 해결했나요?", "협업능력", "MEDIUM"),
            Triple("업무 중 우선순위를 어떻게 정하나요?", "문제해결", "MEDIUM"),
            Triple("실패 경험과 그로부터 배운 점을 말씀해주세요.", "문제해결", "MEDIUM"),
            Triple("이 회사에서 이루고 싶은 목표는 무엇인가요?", "직무역량", "HARD"),
            Triple("5년 후 본인의 모습을 어떻게 그리고 있나요?", "직무역량", "HARD"),
            Triple("마지막으로 하고 싶은 말씀이 있나요?", "협업능력", "HARD")
        )
    }

    /**
     * 채용 공고 기반 질문 10개 생성
     *
     * 흐름:
     * 1. JobPosting에서 effectiveJobField 가져오기
     * 2. PromptBuilder로 System/User 프롬프트 생성
     * 3. AiClient로 OpenAI 호출
     * 4. QuestionResponseParser로 응답 파싱
     * 5. GeneratedQuestion 엔티티 리스트 생성
     * 6. AI 실패 시 Fallback 질문 반환
     *
     * @param jobPosting 채용 공고
     * @return 생성된 질문 10개 (orderIndex 1-10)
     */
    fun generateQuestions(jobPosting: JobPosting): List<GeneratedQuestion> {
        val startTime = System.currentTimeMillis()

        try {
            logger.info(
                "질문 생성 시작 - 공고 ID: ${jobPosting.id}, 회사: ${jobPosting.companyName}, " +
                        "직무: ${jobPosting.effectiveJobField?.displayName}"
            )

            // 1. 직무 필드 확인
            val jobField = jobPosting.effectiveJobField
                ?: throw IllegalArgumentException("JobPosting에 effectiveJobField가 없습니다")

            // 2. 프롬프트 생성
            val systemPrompt = questionPromptBuilder.buildQuestionGenerationSystemPrompt(
                jobField = jobField.code,
                companyName = jobPosting.companyName,
                jobTitle = jobPosting.jobTitle
            )

            val userPrompt = questionPromptBuilder.buildQuestionGenerationUserPrompt(
                jobDescription = jobPosting.jobDescription,
                requiredSkills = jobPosting.requiredSkills,
                preferredSkills = jobPosting.preferredSkills
            )

            // 3. AI 호출
            val rawResponse = aiClient.requestFeedback(systemPrompt, userPrompt)

            // 4. 응답 파싱
            val parsedQuestions = questionResponseParser.parseQuestionResponse(rawResponse)

            // 5. GeneratedQuestion 엔티티 생성
            val questions = parsedQuestions.questions.mapIndexed { index, data ->
                GeneratedQuestion(
                    jobPostingId = jobPosting.id,
                    content = data.content,
                    category = data.category,
                    difficulty = data.difficulty,
                    aiReasoning = data.reasoning,
                    orderIndex = index + 1  // 1-10
                )
            }

            val elapsed = System.currentTimeMillis() - startTime
            logger.info(
                "질문 생성 성공 - 공고 ID: ${jobPosting.id}, 소요 시간: ${elapsed}ms, " +
                        "질문 개수: ${questions.size}개"
            )

            // 메트릭 기록
            recordSuccessMetrics(jobField, elapsed)

            return questions

        } catch (e: AiException) {
            logger.error("AI 질문 생성 실패 - 공고 ID: ${jobPosting.id}, Fallback 사용", e)
            recordFailureMetrics(jobPosting.effectiveJobField)
            return generateFallbackQuestions(jobPosting)
        } catch (e: Exception) {
            logger.error("질문 생성 중 예상치 못한 오류 - 공고 ID: ${jobPosting.id}, Fallback 사용", e)
            recordFailureMetrics(jobPosting.effectiveJobField)
            return generateFallbackQuestions(jobPosting)
        }
    }

    /**
     * Fallback 질문 생성 (AI 실패 시)
     *
     * 직무에 상관없이 사용 가능한 일반적인 질문 10개 반환
     */
    private fun generateFallbackQuestions(jobPosting: JobPosting): List<GeneratedQuestion> {
        logger.warn("Fallback 질문 생성 - 공고 ID: ${jobPosting.id}")

        return FALLBACK_QUESTIONS.mapIndexed { index, (content, category, difficulty) ->
            GeneratedQuestion(
                jobPostingId = jobPosting.id,
                content = content,
                category = category,
                difficulty = difficulty,
                aiReasoning = "AI 질문 생성 실패로 인한 기본 질문",
                orderIndex = index + 1
            )
        }
    }

    /**
     * 성공 메트릭 기록
     */
    private fun recordSuccessMetrics(jobField: JobField, elapsedMs: Long) {
        // 성공 카운터
        meterRegistry.counter(
            "question_generation.success",
            "job_field", jobField.code
        ).increment()

        // 소요 시간
        meterRegistry.timer(
            "question_generation.duration",
            "job_field", jobField.code
        ).record(Duration.ofMillis(elapsedMs))
    }

    /**
     * 실패 메트릭 기록
     */
    private fun recordFailureMetrics(jobField: JobField?) {
        meterRegistry.counter(
            "question_generation.failure",
            "job_field", jobField?.code ?: "unknown"
        ).increment()
    }
}
