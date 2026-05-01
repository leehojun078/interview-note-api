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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
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
     */
    fun startInterview(
        userId: Long,
        jobPostingId: Long?,
        selectedJobField: JobField
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

        // 4. 면접 세션 생성
        val interview = MockInterview(
            userId = userId,
            jobPostingId = jobPostingId,
            selectedJobField = effectiveJobField
        )
        val savedInterview = mockInterviewRepository.save(interview)

        logger.info(
            "면접 시작 - interviewId: ${savedInterview.id}, " +
                    "userId: $userId, jobField: $effectiveJobField, " +
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
            .findByMockInterviewIdOrderByMessageIndexAsc(interviewId)

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

        // Phase 7C: SSE 브로드캐스트 (사용자 메시지)
        // broadcastMessage(interviewId, savedUserMessage)

        // Phase 7C: AI 응답 비동기 생성
        // generateAndBroadcastAiResponseAsync(interview, messages + savedUserMessage)

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
            .findByMockInterviewIdOrderByMessageIndexAsc(interviewId)

        // 공고 조회 (있는 경우)
        val jobPosting = interview.jobPostingId?.let {
            jobPostingRepository.findById(it).orElse(null)
        }

        // Phase 7B: AI 종합 평가 생성
        val evaluation = interviewAiService.generateFinalEvaluation(interview, messages, jobPosting)
        interview.complete(
            overallFeedback = evaluation.overallFeedback,
            keyStrengths = evaluation.keyStrengths,
            keyImprovements = evaluation.keyImprovements,
            averageScore = evaluation.averageScore,
            recommendation = evaluation.recommendation
        )

        val saved = mockInterviewRepository.save(interview)

        logger.info("면접 종료 - interviewId: $interviewId, averageScore: ${evaluation.averageScore}")
        meterRegistry.counter("mock_interview.ended").increment()

        return saved
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
            .findByMockInterviewIdOrderByMessageIndexAsc(interviewId)
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

    // ===== Phase 7C: SSE 관리 (stub) =====

    fun registerEmitter(interviewId: Long, emitter: SseEmitter) {
        // TODO: Phase 7C에서 구현
        emitters[interviewId] = emitter
        logger.debug("SSE emitter 등록 - interviewId: $interviewId")
    }

    fun removeEmitter(interviewId: Long) {
        // TODO: Phase 7C에서 구현
        emitters.remove(interviewId)
        logger.debug("SSE emitter 제거 - interviewId: $interviewId")
    }

    fun broadcastMessage(interviewId: Long, message: InterviewMessage) {
        // TODO: Phase 7C에서 구현
        logger.debug("SSE 브로드캐스트 (stub) - interviewId: $interviewId")
    }
}
