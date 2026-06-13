package com.hojun.interviewnote.interviewnoteapi.service

import com.hojun.interviewnote.interviewnoteapi.domain.*
import com.hojun.interviewnote.interviewnoteapi.service.ai.*
import com.hojun.interviewnote.interviewnoteapi.service.ai.prompt.InterviewPromptBuilder
import com.hojun.interviewnote.interviewnoteapi.service.ai.prompt.EvaluationPromptBuilder
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.util.concurrent.Callable

/**
 * InterviewAiService 단위 테스트
 *
 * Phase 7B: 면접 AI 서비스 테스트
 * - generateFirstQuestion: 첫 질문 생성
 * - generateFollowUpQuestion: 꼬리 질문 + 평가
 * - generateFinalEvaluation: 종합 평가
 */
@ExtendWith(MockitoExtension::class)
class InterviewAiServiceTest {

    @Mock
    private lateinit var aiClient: AiClient

    @Mock
    private lateinit var interviewPromptBuilder: InterviewPromptBuilder

    @Mock
    private lateinit var evaluationPromptBuilder: EvaluationPromptBuilder

    @Mock
    private lateinit var interviewResponseParser: InterviewResponseParser

    @Mock
    private lateinit var meterRegistry: MeterRegistry

    @Mock
    private lateinit var mockCounter: Counter

    @Mock
    private lateinit var mockTimer: Timer

    @InjectMocks
    private lateinit var interviewAiService: InterviewAiService

    @BeforeEach
    fun setUp() {
        // MeterRegistry Mock 설정
        lenient().`when`(meterRegistry.counter(any<String>())).thenReturn(mockCounter)
        lenient().`when`(meterRegistry.timer(any<String>())).thenReturn(mockTimer)
        lenient().`when`(mockTimer.recordCallable(any<Callable<Any>>())).thenAnswer { invocation ->
            val callable = invocation.getArgument<Callable<Any>>(0)
            callable.call()
        }
    }

    // ===== generateFirstQuestion 테스트 =====

    @Test
    fun `generateFirstQuestion - 직무 기반 면접 첫 질문 생성`() {
        // Given
        val interview = createMockInterview(jobPostingId = null)
        val systemPrompt = "You are an interviewer for IT."
        val userPrompt = "Generate first question for IT."
        val rawResponse = """
            {
              "evaluation": {
                "logicScore": 3,
                "specificityScore": 3,
                "deliveryScore": 3,
                "comment": "첫 질문입니다"
              },
              "nextAction": {
                "question": "간단히 자기소개를 해주세요.",
                "reasoning": "지원자의 배경을 파악하기 위함",
                "isFollowUp": false
              }
            }
        """.trimIndent()

        val parsedResponse = ParsedInterviewResponse(
            evaluation = InterviewEvaluation(
                logicScore = 3,
                specificityScore = 3,
                deliveryScore = 3,
                comment = "첫 질문입니다"
            ),
            nextAction = NextAction(
                question = "간단히 자기소개를 해주세요.",
                reasoning = "지원자의 배경을 파악하기 위함",
                isFollowUp = false
            )
        )

        whenever(interviewPromptBuilder.buildInterviewSystemPrompt(interview.selectedJobField.name))
            .thenReturn(systemPrompt)
        whenever(interviewPromptBuilder.buildFirstQuestionPrompt(
            jobField = interview.selectedJobField.displayName,
            companyName = null,
            jobTitle = null
        )).thenReturn(userPrompt)
        whenever(aiClient.requestFeedback(systemPrompt, userPrompt))
            .thenReturn(rawResponse)
        whenever(interviewResponseParser.parseInterviewResponse(rawResponse))
            .thenReturn(parsedResponse)

        // When
        val result = interviewAiService.generateFirstQuestion(interview, null)

        // Then
        assertThat(result.question).isEqualTo("간단히 자기소개를 해주세요.")
        assertThat(result.reasoning).isEqualTo("지원자의 배경을 파악하기 위함")

        verify(interviewPromptBuilder).buildInterviewSystemPrompt(interview.selectedJobField.name)
        verify(interviewPromptBuilder).buildFirstQuestionPrompt(
            jobField = interview.selectedJobField.displayName,
            companyName = null,
            jobTitle = null
        )
        verify(aiClient).requestFeedback(systemPrompt, userPrompt)
        verify(interviewResponseParser).parseInterviewResponse(rawResponse)
    }

    @Test
    fun `generateFirstQuestion - 공고 기반 면접 첫 질문 생성`() {
        // Given
        val interview = createMockInterview(jobPostingId = 1L)
        val jobPosting = createJobPosting()
        val systemPrompt = "You are an interviewer for IT with job posting."
        val userPrompt = "Generate first question with company and job title."
        val rawResponse = """
            {
              "evaluation": {
                "logicScore": 3,
                "specificityScore": 3,
                "deliveryScore": 3,
                "comment": "첫 질문입니다"
              },
              "nextAction": {
                "question": "테크컴퍼니의 백엔드 개발자 포지션에 지원하신 이유를 말씀해주세요.",
                "reasoning": "지원 동기와 회사 이해도를 파악하기 위함",
                "isFollowUp": false
              }
            }
        """.trimIndent()

        val parsedResponse = ParsedInterviewResponse(
            evaluation = InterviewEvaluation(3, 3, 3, "첫 질문입니다"),
            nextAction = NextAction(
                question = "테크컴퍼니의 백엔드 개발자 포지션에 지원하신 이유를 말씀해주세요.",
                reasoning = "지원 동기와 회사 이해도를 파악하기 위함",
                isFollowUp = false
            )
        )

        whenever(interviewPromptBuilder.buildInterviewSystemPromptWithJobPosting(
            jobField = interview.selectedJobField.name,
            companyName = jobPosting.companyName,
            jobTitle = jobPosting.jobTitle,
            jobDescription = jobPosting.jobDescription,
            requiredSkills = jobPosting.requiredSkills,
            preferredSkills = jobPosting.preferredSkills
        )).thenReturn(systemPrompt)
        whenever(interviewPromptBuilder.buildFirstQuestionPrompt(
            jobField = interview.selectedJobField.displayName,
            companyName = jobPosting.companyName,
            jobTitle = jobPosting.jobTitle
        )).thenReturn(userPrompt)
        whenever(aiClient.requestFeedback(systemPrompt, userPrompt)).thenReturn(rawResponse)
        whenever(interviewResponseParser.parseInterviewResponse(rawResponse)).thenReturn(parsedResponse)

        // When
        val result = interviewAiService.generateFirstQuestion(interview, jobPosting)

        // Then
        assertThat(result.question).contains("테크컴퍼니")
        assertThat(result.question).contains("백엔드 개발자")

        verify(interviewPromptBuilder).buildInterviewSystemPromptWithJobPosting(any(), any(), any(), any(), any(), any())
    }

    // ===== generateFollowUpQuestion 테스트 =====

    @Test
    fun `generateFollowUpQuestion - 답변 평가와 꼬리 질문 생성`() {
        // Given
        val interview = createMockInterview(jobPostingId = null)
        val conversation = createConversation()
        val systemPrompt = "System prompt for IT"
        val userPrompt = "Follow-up prompt with conversation"
        val rawResponse = """
            {
              "evaluation": {
                "logicScore": 4,
                "specificityScore": 3,
                "deliveryScore": 4,
                "comment": "답변이 논리적이고 구체적입니다. 기술 스택을 명확히 제시했습니다."
              },
              "nextAction": {
                "question": "그 프로젝트에서 가장 어려웠던 기술적 도전은 무엇이었나요?",
                "reasoning": "기술적 깊이와 문제 해결 능력을 확인하기 위함",
                "isFollowUp": true
              }
            }
        """.trimIndent()

        val parsedResponse = ParsedInterviewResponse(
            evaluation = InterviewEvaluation(
                logicScore = 4,
                specificityScore = 3,
                deliveryScore = 4,
                comment = "답변이 논리적이고 구체적입니다. 기술 스택을 명확히 제시했습니다."
            ),
            nextAction = NextAction(
                question = "그 프로젝트에서 가장 어려웠던 기술적 도전은 무엇이었나요?",
                reasoning = "기술적 깊이와 문제 해결 능력을 확인하기 위함",
                isFollowUp = true
            )
        )

        whenever(interviewPromptBuilder.buildInterviewSystemPrompt(interview.selectedJobField.name))
            .thenReturn(systemPrompt)
        whenever(interviewPromptBuilder.buildFollowUpPrompt(any())).thenReturn(userPrompt)
        whenever(aiClient.requestFeedback(systemPrompt, userPrompt)).thenReturn(rawResponse)
        whenever(interviewResponseParser.parseInterviewResponse(rawResponse)).thenReturn(parsedResponse)

        // When
        val (evaluation, nextQuestion) = interviewAiService.generateFollowUpQuestion(
            interview, conversation, null
        )

        // Then
        assertThat(evaluation.logicScore).isEqualTo(4)
        assertThat(evaluation.specificityScore).isEqualTo(3)
        assertThat(evaluation.deliveryScore).isEqualTo(4)
        assertThat(evaluation.comment).contains("논리적이고 구체적")

        assertThat(nextQuestion.question).contains("기술적 도전")
        assertThat(nextQuestion.reasoning).contains("문제 해결 능력")

        verify(interviewPromptBuilder).buildFollowUpPrompt(any())
        verify(aiClient).requestFeedback(systemPrompt, userPrompt)
    }

    @Test
    fun `generateFollowUpQuestion - 공고 기반 면접의 꼬리 질문 생성`() {
        // Given
        val interview = createMockInterview(jobPostingId = 1L)
        val jobPosting = createJobPosting()
        val conversation = createConversation()
        val systemPrompt = "System prompt with job posting"
        val userPrompt = "Follow-up prompt"
        val rawResponse = """
            {
              "evaluation": {
                "logicScore": 5,
                "specificityScore": 4,
                "deliveryScore": 5,
                "comment": "매우 구체적이고 우수한 답변입니다."
              },
              "nextAction": {
                "question": "Spring Boot와 Kotlin을 함께 사용한 경험을 설명해주세요.",
                "reasoning": "필수 기술 스택 경험을 확인하기 위함",
                "isFollowUp": true
              }
            }
        """.trimIndent()

        val parsedResponse = ParsedInterviewResponse(
            evaluation = InterviewEvaluation(5, 4, 5, "매우 구체적이고 우수한 답변입니다."),
            nextAction = NextAction(
                question = "Spring Boot와 Kotlin을 함께 사용한 경험을 설명해주세요.",
                reasoning = "필수 기술 스택 경험을 확인하기 위함",
                isFollowUp = true
            )
        )

        whenever(interviewPromptBuilder.buildInterviewSystemPromptWithJobPosting(any(), any(), any(), any(), any(), any()))
            .thenReturn(systemPrompt)
        whenever(interviewPromptBuilder.buildFollowUpPrompt(any())).thenReturn(userPrompt)
        whenever(aiClient.requestFeedback(systemPrompt, userPrompt)).thenReturn(rawResponse)
        whenever(interviewResponseParser.parseInterviewResponse(rawResponse)).thenReturn(parsedResponse)

        // When
        val (evaluation, nextQuestion) = interviewAiService.generateFollowUpQuestion(
            interview, conversation, jobPosting
        )

        // Then
        assertThat(evaluation.logicScore).isEqualTo(5)
        assertThat(nextQuestion.question).contains("Spring Boot와 Kotlin")
    }

    // ===== generateFinalEvaluation 테스트 =====

    @Test
    fun `generateFinalEvaluation - 직무 기반 면접 종합 평가 생성`() {
        // Given
        val interview = createMockInterview(jobPostingId = null)
        val conversation = createConversation()
        val userPrompt = "Final evaluation prompt"
        val rawResponse = """
            {
              "overallFeedback": "전반적으로 우수한 답변이었습니다. 기술적 깊이가 돋보였습니다.",
              "keyStrengths": [
                "논리적이고 체계적인 답변 구조",
                "구체적인 기술 스택 및 프로젝트 경험 제시",
                "문제 해결 과정의 명확한 설명"
              ],
              "keyImprovements": [
                "팀 협업 경험을 더 강조하면 좋을 것",
                "성과 측정 지표를 구체적으로 제시"
              ],
              "averageScore": 4.2,
              "recommendation": "기술 역량과 커뮤니케이션 능력이 우수하여 적극 추천합니다."
            }
        """.trimIndent()

        val parsedResult = FinalEvaluationResult(
            overallFeedback = "전반적으로 우수한 답변이었습니다. 기술적 깊이가 돋보였습니다.",
            keyStrengths = "[\"논리적이고 체계적인 답변 구조\",\"구체적인 기술 스택 및 프로젝트 경험 제시\",\"문제 해결 과정의 명확한 설명\"]",
            keyImprovements = "[\"팀 협업 경험을 더 강조하면 좋을 것\",\"성과 측정 지표를 구체적으로 제시\"]",
            averageScore = 4.2,
            recommendation = "기술 역량과 커뮤니케이션 능력이 우수하여 적극 추천합니다."
        )

        whenever(evaluationPromptBuilder.buildFinalEvaluationPrompt(
            jobField = anyOrNull(),
            companyName = isNull(),
            jobTitle = isNull(),
            allUserAnswers = any()
        )).thenReturn(userPrompt)
        whenever(aiClient.requestFeedback(any(), any())).thenReturn(rawResponse)
        whenever(interviewResponseParser.parseFinalEvaluation(rawResponse)).thenReturn(parsedResult)

        // When
        val result = interviewAiService.generateFinalEvaluation(interview, conversation, null)

        // Then
        assertThat(result.overallFeedback).contains("전반적으로 우수한 답변")
        assertThat(result.averageScore).isEqualTo(4.2)
        assertThat(result.keyStrengths).contains("논리적이고 체계적인 답변 구조")
        assertThat(result.keyImprovements).contains("팀 협업 경험")
        assertThat(result.recommendation).contains("적극 추천")

        verify(evaluationPromptBuilder).buildFinalEvaluationPrompt(anyOrNull(), isNull(), isNull(), any())
        verify(aiClient).requestFeedback(any(), any())
        verify(interviewResponseParser).parseFinalEvaluation(rawResponse)
    }

    @Test
    fun `generateFinalEvaluation - 공고 기반 면접 종합 평가 생성`() {
        // Given
        val interview = createMockInterview(jobPostingId = 1L)
        val jobPosting = createJobPosting()
        val conversation = createConversation()
        val userPrompt = "Final evaluation prompt with job posting"
        val rawResponse = """
            {
              "overallFeedback": "테크컴퍼니의 백엔드 개발자 포지션에 적합한 역량을 갖추었습니다.",
              "keyStrengths": [
                "Spring Boot 실무 경험 풍부",
                "Kotlin 활용 능력 우수"
              ],
              "keyImprovements": [
                "Redis 경험 보강 필요"
              ],
              "averageScore": 4.0,
              "recommendation": "필수 기술 스택을 충분히 갖추어 추천합니다."
            }
        """.trimIndent()

        val parsedResult = FinalEvaluationResult(
            overallFeedback = "테크컴퍼니의 백엔드 개발자 포지션에 적합한 역량을 갖추었습니다.",
            keyStrengths = "[\"Spring Boot 실무 경험 풍부\",\"Kotlin 활용 능력 우수\"]",
            keyImprovements = "[\"Redis 경험 보강 필요\"]",
            averageScore = 4.0,
            recommendation = "필수 기술 스택을 충분히 갖추어 추천합니다."
        )

        whenever(evaluationPromptBuilder.buildFinalEvaluationPrompt(
            jobField = anyOrNull(),
            companyName = anyOrNull(),
            jobTitle = anyOrNull(),
            allUserAnswers = any()
        )).thenReturn(userPrompt)
        whenever(aiClient.requestFeedback(any(), any())).thenReturn(rawResponse)
        whenever(interviewResponseParser.parseFinalEvaluation(rawResponse)).thenReturn(parsedResult)

        // When
        val result = interviewAiService.generateFinalEvaluation(interview, conversation, jobPosting)

        // Then
        assertThat(result.overallFeedback).contains("테크컴퍼니")
        assertThat(result.keyStrengths).contains("Spring Boot")
        assertThat(result.keyStrengths).contains("Kotlin")
    }

    // ===== Helper Methods =====

    private fun createMockInterview(jobPostingId: Long?): MockInterview {
        return MockInterview(
            userId = 1L,
            jobPostingId = jobPostingId,
            selectedJobField = JobField.IT
        ).apply {
            // Reflection을 사용하여 id 설정 (테스트용)
            val idField = MockInterview::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, 1L)
        }
    }

    private fun createJobPosting(): JobPosting {
        return JobPosting(
            userId = 1L,
            originalUrl = "https://example.com/job/1",
            companyName = "테크컴퍼니",
            jobTitle = "백엔드 개발자",
            jobDescription = "Spring Boot 기반 REST API 개발",
            requiredSkills = "Java, Spring Boot, MySQL",
            preferredSkills = "Kotlin, Redis, Docker",
            selectedJobField = JobField.IT
        )
    }

    private fun createConversation(): List<InterviewMessage> {
        return listOf(
            InterviewMessage(
                mockInterviewId = 1L,
                sender = MessageSender.AI,
                content = "간단히 자기소개를 해주세요.",
                messageIndex = 0,
                aiReasoning = "지원자의 배경을 파악하기 위함"
            ),
            InterviewMessage(
                mockInterviewId = 1L,
                sender = MessageSender.USER,
                content = "저는 3년차 백엔드 개발자로 Spring Boot로 RESTful API를 개발해왔습니다.",
                messageIndex = 1
            ),
            InterviewMessage(
                mockInterviewId = 1L,
                sender = MessageSender.AI,
                content = "가장 기억에 남는 프로젝트는 무엇인가요?",
                messageIndex = 2,
                aiReasoning = "구체적인 경험을 확인하기 위함"
            ),
            InterviewMessage(
                mockInterviewId = 1L,
                sender = MessageSender.USER,
                content = "결제 시스템을 개발했던 경험이 가장 기억에 남습니다.",
                messageIndex = 3
            )
        ).map {
            it.apply {
                val idField = InterviewMessage::class.java.getDeclaredField("id")
                idField.isAccessible = true
                idField.set(this, messageIndex.toLong() + 1)

                val createdAtField = InterviewMessage::class.java.getDeclaredField("createdAt")
                createdAtField.isAccessible = true
                createdAtField.set(this, LocalDateTime.now())
            }
        }
    }
}
