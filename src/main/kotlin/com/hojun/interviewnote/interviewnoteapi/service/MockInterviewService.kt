package com.hojun.interviewnote.interviewnoteapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.hojun.interviewnote.interviewnoteapi.domain.*
import com.hojun.interviewnote.interviewnoteapi.exception.*
import com.hojun.interviewnote.interviewnoteapi.repository.InterviewMessageRepository
import com.hojun.interviewnote.interviewnoteapi.repository.JobPostingRepository
import com.hojun.interviewnote.interviewnoteapi.repository.MockInterviewRepository
import com.hojun.interviewnote.interviewnoteapi.service.ai.AiInterviewResponse
import com.hojun.interviewnote.interviewnoteapi.service.ratelimit.RateLimitService
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * 모의 면접 서비스
 *
 * Phase 7A: 기본 CRUD
 * Phase 7B: AI 통합 (InterviewAiService 연동)
 * Phase 7C: SSE 실시간 통신
 */
@Service
@Transactional
class MockInterviewService(
    private val mockInterviewRepository: MockInterviewRepository,
    private val interviewMessageRepository: InterviewMessageRepository,
    private val jobPostingRepository: JobPostingRepository,
    private val rateLimitService: RateLimitService,
    private val interviewAiService: InterviewAiService,
    private val objectMapper: ObjectMapper,
    private val meterRegistry: MeterRegistry
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val MAX_TURNS = 30
    }

    // Phase 7C: SSE Emitter 관리 (stub)
    private val emitters = ConcurrentHashMap<Long, SseEmitter>()

    /**
     * 면접 시작
     *
     * Phase 7A: 세션 생성 + Rate Limiting
     * Phase 7B: AI 첫 질문 생성 (동기)
     * Phase 8B: careerLevel 파라미터 추가
     */
    fun startInterview(
        userId: Long,
        jobPostingId: Long?,
        selectedJobField: JobField,
        careerLevel: CareerLevel? = null
    ): MockInterview {
        // 1. Rate Limiting 체크
        rateLimitService.checkMockInterviewLimit(userId)

        // 2. 공고 검증 (있는 경우)
        val jobPosting = jobPostingId?.let {
            val posting = jobPostingRepository.findById(it).orElse(null)
                ?: throw JobPostingNotFoundException("채용 공고를 찾을 수 없습니다: $it")

            // 소유권 검증
            if (posting.userId != userId) {
                throw JobPostingNotFoundException("접근 권한이 없습니다: $it")
            }

            posting
        }

        // 3. 직무 결정 (공고 기반 면접 시 공고의 effectiveJobField 사용)
        val effectiveJobField = jobPosting?.effectiveJobField ?: selectedJobField

        // 4. 면접 세션 생성 (Phase 8B: careerLevel 추가)
        val interview = MockInterview(
            userId = userId,
            jobPostingId = jobPostingId,
            selectedJobField = effectiveJobField,
            careerLevel = careerLevel
        )
        val savedInterview = mockInterviewRepository.save(interview)

        logger.info(
            "면접 시작 - interviewId: ${savedInterview.id}, " +
                    "userId: $userId, jobField: $effectiveJobField, " +
                    "careerLevel: ${careerLevel?.name}, " +
                    "jobPostingId: $jobPostingId"
        )

        meterRegistry.counter("mock_interview.started").increment()

        // Phase 7B: AI 첫 질문 생성 (동기)
        val firstQuestion = interviewAiService.generateFirstQuestion(savedInterview, jobPosting)
        saveAiMessage(savedInterview.id, firstQuestion, 0)

        return savedInterview
    }

    /**
     * 사용자 메시지 전송
     *
     * Phase 7A: 메시지 저장 + 검증
     * Phase 7C: SSE 브로드캐스트 + 비동기 AI 응답 추가 예정
     */
    fun sendUserMessage(
        interviewId: Long,
        userId: Long,
        content: String
    ): InterviewMessage {
        // 1. 면접 세션 검증
        val interview = validateAndGetInterview(interviewId, userId)

        // 2. 대화 히스토리 조회
        val messages = interviewMessageRepository
            .findByMockInterviewIdOrderByCreatedAtAsc(interviewId)

        // 3. 턴 수 체크
        if (messages.size >= MAX_TURNS) {
            throw MaxTurnExceededException(interviewId, MAX_TURNS)
        }

        // 4. 사용자 메시지 저장
        val userMessage = InterviewMessage(
            mockInterviewId = interviewId,
            sender = MessageSender.USER,
            content = content,
            messageIndex = messages.size
        )
        val savedUserMessage = interviewMessageRepository.save(userMessage)

        logger.info(
            "사용자 메시지 저장 - interviewId: $interviewId, " +
                    "messageIndex: ${savedUserMessage.messageIndex}"
        )

        // Phase 7C: SSE 브로드캐스트 (네트워크 지연 대응 - POST보다 빠를 수 있음)
        broadcastMessage(interviewId, savedUserMessage)

        // Phase 7C: AI 응답 비동기 생성
        generateAndBroadcastAiResponseAsync(interview, messages + savedUserMessage)

        return savedUserMessage
    }

    /**
     * 면접 종료
     *
     * Phase 7A: 상태 변경
     * Phase 7B: AI 종합 평가 생성
     */
    fun endInterview(interviewId: Long, userId: Long): MockInterview {
        val interview = validateAndGetInterview(interviewId, userId)

        val messages = interviewMessageRepository
            .findByMockInterviewIdOrderByCreatedAtAsc(interviewId)

        // 공고 조회 (있는 경우)
        val jobPosting = interview.jobPostingId?.let {
            jobPostingRepository.findById(it).orElse(null)
        }

        // Phase 7B: AI 종합 평가 생성
        val evaluation = interviewAiService.generateFinalEvaluation(interview, messages, jobPosting)

        // Phase 8A: 가중 평균 점수 계산 (첫 답변 제외 + 저품질 패널티)
        val weightedScore = interviewAiService.calculateWeightedScore(messages)

        interview.complete(
            overallFeedback = evaluation.overallFeedback,
            keyStrengths = evaluation.keyStrengths,
            keyImprovements = evaluation.keyImprovements,
            averageScore = evaluation.averageScore,
            weightedAverageScore = weightedScore,
            recommendation = evaluation.recommendation
        )

        val saved = mockInterviewRepository.save(interview)

        logger.info("면접 종료 - interviewId: $interviewId, " +
                    "averageScore: ${evaluation.averageScore}, weightedScore: $weightedScore")
        meterRegistry.counter("mock_interview.ended").increment()

        return saved
    }

    /**
     * 면접 재개
     *
     * Phase 8C: 완료된 면접을 다시 시작
     * COMPLETED → IN_PROGRESS 상태 전환
     */
    fun resumeInterview(interviewId: Long, userId: Long): MockInterview {
        val interview = getInterview(interviewId, userId)

        require(interview.status == MockInterviewStatus.COMPLETED) {
            "완료된 면접만 재개할 수 있습니다: interviewId=$interviewId, status=${interview.status.name}"
        }

        interview.resume()
        val resumed = mockInterviewRepository.save(interview)

        logger.info("면접 재개 - interviewId: $interviewId, userId: $userId")
        meterRegistry.counter("mock_interview.resumed").increment()

        return resumed
    }

    /**
     * 면접 세션 조회 (소유권 검증 포함)
     */
    @Transactional(readOnly = true)
    fun getInterview(id: Long, userId: Long): MockInterview {
        return mockInterviewRepository.findByIdAndUserId(id, userId)
            ?: throw MockInterviewAccessDeniedException(id, userId)
    }

    /**
     * 면접 메시지 조회
     */
    @Transactional(readOnly = true)
    fun getMessages(interviewId: Long): List<InterviewMessage> {
        return interviewMessageRepository
            .findByMockInterviewIdOrderByCreatedAtAsc(interviewId)
    }

    /**
     * 사용자의 모든 면접 세션 조회
     */
    @Transactional(readOnly = true)
    fun getUserInterviews(userId: Long): List<MockInterview> {
        return mockInterviewRepository.findByUserIdOrderByStartedAtDesc(userId)
    }

    /**
     * 면접 세션 검증
     *
     * - 존재 여부
     * - 소유권
     * - 상태 (IN_PROGRESS만 허용)
     */
    private fun validateAndGetInterview(interviewId: Long, userId: Long): MockInterview {
        val interview = mockInterviewRepository.findByIdAndUserId(interviewId, userId)
            ?: throw MockInterviewAccessDeniedException(interviewId, userId)

        if (interview.status != MockInterviewStatus.IN_PROGRESS) {
            throw InterviewAlreadyEndedException(interviewId, interview.status.name)
        }

        return interview
    }

    /**
     * AI 메시지 저장
     *
     * Phase 7B: 첫 질문 저장
     */
    private fun saveAiMessage(
        interviewId: Long,
        response: AiInterviewResponse,
        index: Int
    ): InterviewMessage {
        val message = InterviewMessage(
            mockInterviewId = interviewId,
            sender = MessageSender.AI,
            content = response.question,
            messageIndex = index,
            aiReasoning = response.reasoning
        )
        return interviewMessageRepository.save(message)
    }

    // ===== Phase 7C: SSE 관리 =====

    /**
     * SSE Emitter 등록
     *
     * 클라이언트가 /stream 엔드포인트에 연결하면 호출됨
     */
    fun registerEmitter(interviewId: Long, emitter: SseEmitter) {
        emitters[interviewId] = emitter
        logger.info("SSE emitter 등록 - interviewId: $interviewId")
        meterRegistry.gauge("mock_interview.active_connections", emitters.size)
    }

    /**
     * SSE Emitter 제거
     *
     * 연결 종료, 타임아웃, 에러 발생 시 호출됨
     */
    fun removeEmitter(interviewId: Long) {
        emitters.remove(interviewId)
        logger.info("SSE emitter 제거 - interviewId: $interviewId")
        meterRegistry.gauge("mock_interview.active_connections", emitters.size)
    }

    /**
     * SSE 메시지 브로드캐스트
     *
     * 메시지를 JSON으로 변환하여 SSE를 통해 클라이언트에 전송
     *
     * @return 전송 성공 여부
     */
    fun broadcastMessage(interviewId: Long, message: InterviewMessage): Boolean {
        val emitter = emitters[interviewId]
        if (emitter == null) {
            logger.warn("SSE emitter가 없음 - interviewId: $interviewId")
            return false
        }

        return try {
            val messageDto = mapToMessageDto(message)
            emitter.send(
                SseEmitter.event()
                    .name("message")
                    .data(messageDto)
            )
            logger.debug("SSE 메시지 전송 성공 - interviewId: $interviewId, sender: ${message.sender}")
            meterRegistry.counter("mock_interview.sse.messages_sent").increment()
            true
        } catch (e: IOException) {
            logger.error("SSE 전송 실패 - interviewId: $interviewId", e)
            removeEmitter(interviewId)
            meterRegistry.counter("mock_interview.sse.errors").increment()
            false
        } catch (e: Exception) {
            logger.error("SSE 전송 중 예외 발생 - interviewId: $interviewId", e)
            meterRegistry.counter("mock_interview.sse.errors").increment()
            false
        }
    }

    /**
     * AI 응답 비동기 생성 및 브로드캐스트
     *
     * @Async를 사용하여 AI 응답 생성을 백그라운드에서 수행
     * 사용자 메시지 저장 후 즉시 반환하여 응답 속도 향상
     */
    @Async("taskExecutor")
    fun generateAndBroadcastAiResponseAsync(
        interview: MockInterview,
        conversation: List<InterviewMessage>
    ) {
        try {
            logger.info("AI 응답 비동기 생성 시작 - interviewId: ${interview.id}")

            // 공고 조회 (있는 경우)
            val jobPosting = interview.jobPostingId?.let {
                jobPostingRepository.findById(it).orElse(null)
            }

            // AI 응답 생성 (InterviewAiService 호출)
            // Pair<InterviewEvaluation, AiInterviewResponse> 반환
            val (evaluation, aiResponse) = interviewAiService.generateFollowUpQuestion(
                interview,
                conversation,
                jobPosting
            )

            // AI 메시지 저장
            val aiMessage = InterviewMessage(
                mockInterviewId = interview.id,
                sender = MessageSender.AI,
                content = aiResponse.question,
                messageIndex = conversation.size,
                aiReasoning = aiResponse.reasoning,
                // 이전 사용자 메시지에 대한 평가 점수 저장
                logicScore = null,
                specificityScore = null,
                deliveryScore = null,
                feedbackComment = null
            )
            val savedAiMessage = interviewMessageRepository.save(aiMessage)

            // 이전 사용자 메시지에 평가 점수 업데이트
            val lastUserMessage = conversation.lastOrNull { it.sender == MessageSender.USER }
            lastUserMessage?.let { userMsg ->
                userMsg.logicScore = evaluation.logicScore
                userMsg.specificityScore = evaluation.specificityScore
                userMsg.deliveryScore = evaluation.deliveryScore
                userMsg.feedbackComment = evaluation.comment
                interviewMessageRepository.save(userMsg)
            }

            // SSE 브로드캐스트
            broadcastMessage(interview.id, savedAiMessage)

            logger.info("AI 응답 비동기 생성 완료 - interviewId: ${interview.id}")
        } catch (e: Exception) {
            logger.error("AI 응답 생성 실패 - interviewId: ${interview.id}", e)
            meterRegistry.counter("mock_interview.ai_response_generation.errors").increment()

            // 에러 발생 시 fallback 메시지 전송
            try {
                val fallbackMessage = InterviewMessage(
                    mockInterviewId = interview.id,
                    sender = MessageSender.AI,
                    content = "죄송합니다. 일시적인 오류로 질문을 생성하지 못했습니다. 잠시 후 다시 시도해 주세요.",
                    messageIndex = conversation.size,
                    aiReasoning = "fallback_error"
                )
                val saved = interviewMessageRepository.save(fallbackMessage)
                broadcastMessage(interview.id, saved)
            } catch (fallbackError: Exception) {
                logger.error("Fallback 메시지 전송 실패 - interviewId: ${interview.id}", fallbackError)
            }
        }
    }

    /**
     * InterviewMessage를 SSE 전송용 DTO로 변환
     */
    private fun mapToMessageDto(message: InterviewMessage): Map<String, Any?> {
        return mapOf(
            "id" to message.id,
            "sender" to message.sender.name,
            "content" to message.content,
            "messageIndex" to message.messageIndex,
            "createdAt" to message.createdAt.toString(),
            "aiReasoning" to message.aiReasoning,
            "logicScore" to message.logicScore,
            "specificityScore" to message.specificityScore,
            "deliveryScore" to message.deliveryScore,
            "feedbackComment" to message.feedbackComment
        )
    }
}
