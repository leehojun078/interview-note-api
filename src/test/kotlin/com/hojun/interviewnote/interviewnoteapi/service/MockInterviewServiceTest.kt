package com.hojun.interviewnote.interviewnoteapi.service

import com.hojun.interviewnote.interviewnoteapi.domain.*
import com.hojun.interviewnote.interviewnoteapi.exception.InterviewAlreadyEndedException
import com.hojun.interviewnote.interviewnoteapi.exception.MaxTurnExceededException
import com.hojun.interviewnote.interviewnoteapi.exception.MockInterviewAccessDeniedException
import com.hojun.interviewnote.interviewnoteapi.repository.InterviewMessageRepository
import com.hojun.interviewnote.interviewnoteapi.repository.MockInterviewRepository
import com.hojun.interviewnote.interviewnoteapi.repository.UserRepository
import com.hojun.interviewnote.interviewnoteapi.service.ai.AiInterviewResponse
import com.hojun.interviewnote.interviewnoteapi.service.ai.FinalEvaluationResult
import com.hojun.interviewnote.interviewnoteapi.service.ai.InterviewEvaluation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.transaction.annotation.Transactional

/**
 * MockInterviewService 단위 테스트
 *
 * Phase 7A: 기본 CRUD 및 검증 로직 테스트
 */
@SpringBootTest
@Transactional
class MockInterviewServiceTest {

    @Autowired
    private lateinit var mockInterviewService: MockInterviewService

    @Autowired
    private lateinit var mockInterviewRepository: MockInterviewRepository

    @Autowired
    private lateinit var interviewMessageRepository: InterviewMessageRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @MockBean
    private lateinit var interviewAiService: InterviewAiService

    private var userIdCounter = 10000L

    private fun nextUserId(): Long {
        val userId = userIdCounter++
        // User 생성
        val savedUser = userRepository.save(
            User(
                email = "test$userId@example.com",
                passwordHash = "encodedPassword",
                name = "TestUser$userId",
                role = UserRole.USER,
                jobField = JobField.IT,
                careerLevel = CareerLevel.JUNIOR
            )
        )
        return savedUser.id
    }

    @BeforeEach
    fun setUp() {
        // Mock AI 서비스 응답
        whenever(interviewAiService.generateFirstQuestion(any(), any())).thenReturn(
            AiInterviewResponse(
                question = "간단히 자기소개를 해주세요.",
                reasoning = "지원자의 배경을 파악하기 위함"
            )
        )

        whenever(interviewAiService.generateFinalEvaluation(any(), any(), any())).thenReturn(
            FinalEvaluationResult(
                overallFeedback = "전반적으로 우수한 답변이었습니다.",
                keyStrengths = "[\"강점1\", \"강점2\", \"강점3\"]",
                keyImprovements = "[\"개선점1\", \"개선점2\", \"개선점3\"]",
                averageScore = 4.0,
                recommendation = "추천합니다."
            )
        )
    }

    @Test
    fun `직무 기반 면접 시작 성공`() {
        // given
        val userId = nextUserId()
        val selectedJobField = JobField.IT

        // when
        val interview = mockInterviewService.startInterview(
            userId = userId,
            jobPostingId = null,
            selectedJobField = selectedJobField
        )

        // then
        assertThat(interview.id).isGreaterThan(0)
        assertThat(interview.userId).isEqualTo(userId)
        assertThat(interview.jobPostingId).isNull()
        assertThat(interview.selectedJobField).isEqualTo(JobField.IT)
        assertThat(interview.status).isEqualTo(MockInterviewStatus.IN_PROGRESS)
        assertThat(interview.endedAt).isNull()
    }

    @Test
    fun `사용자 메시지 전송 성공`() {
        // given
        val userId = nextUserId()
        val interview = createTestInterview(userId = userId)
        val content = "안녕하세요, 저는 3년차 백엔드 개발자입니다."

        // when
        val message = mockInterviewService.sendUserMessage(
            interviewId = interview.id,
            userId = userId,
            content = content
        )

        // then
        assertThat(message.id).isGreaterThan(0)
        assertThat(message.mockInterviewId).isEqualTo(interview.id)
        assertThat(message.sender).isEqualTo(MessageSender.USER)
        assertThat(message.content).isEqualTo(content)
        assertThat(message.messageIndex).isGreaterThanOrEqualTo(0)
    }

    @Test
    fun `메시지 순서 보장 확인`() {
        // given
        val userId = nextUserId()
        val interview = createTestInterview(userId = userId)

        // when
        val message1 = mockInterviewService.sendUserMessage(interview.id, userId, "첫 번째 답변")
        val message2 = mockInterviewService.sendUserMessage(interview.id, userId, "두 번째 답변")
        val message3 = mockInterviewService.sendUserMessage(interview.id, userId, "세 번째 답변")

        // then
        assertThat(message1.messageIndex).isLessThan(message2.messageIndex)
        assertThat(message2.messageIndex).isLessThan(message3.messageIndex)
    }

    @Test
    fun `30턴 초과 시 예외 발생`() {
        // given
        val userId = nextUserId()
        val interview = createTestInterview(userId = userId)
        createMultipleMessages(interview.id, MockInterviewService.MAX_TURNS)

        // when & then
        assertThrows<MaxTurnExceededException> {
            mockInterviewService.sendUserMessage(interview.id, userId, "31번째 메시지")
        }
    }

    @Test
    fun `타 사용자 면접 접근 시 예외 발생`() {
        // given
        val userId = nextUserId()
        val interview = createTestInterview(userId = userId)

        // when & then
        assertThrows<MockInterviewAccessDeniedException> {
            mockInterviewService.sendUserMessage(interview.id, 999999L, "다른 사용자")
        }
    }

    @Test
    fun `종료된 면접에 메시지 전송 시 예외 발생`() {
        // given
        val userId = nextUserId()
        val interview = createTestInterview(userId = userId)
        mockInterviewService.endInterview(interview.id, userId)

        // when & then
        assertThrows<InterviewAlreadyEndedException> {
            mockInterviewService.sendUserMessage(interview.id, userId, "종료 후 메시지")
        }
    }

    @Test
    fun `면접 종료 성공`() {
        // given
        val userId = nextUserId()
        val interview = createTestInterview(userId = userId)
        mockInterviewService.sendUserMessage(interview.id, userId, "답변 1")
        mockInterviewService.sendUserMessage(interview.id, userId, "답변 2")

        // when
        val ended = mockInterviewService.endInterview(interview.id, userId)

        // then
        assertThat(ended.status).isEqualTo(MockInterviewStatus.COMPLETED)
        assertThat(ended.endedAt).isNotNull()
        assertThat(ended.overallFeedback).isNotNull()
        assertThat(ended.averageScore).isNotNull()
    }

    @Test
    fun `면접 조회 성공`() {
        // given
        val userId = nextUserId()
        val interview = createTestInterview(userId = userId)

        // when
        val fetched = mockInterviewService.getInterview(interview.id, userId)

        // then
        assertThat(fetched.id).isEqualTo(interview.id)
        assertThat(fetched.userId).isEqualTo(userId)
    }

    @Test
    fun `메시지 목록 조회 성공`() {
        // given
        val userId = nextUserId()
        val interview = createTestInterview(userId = userId)
        mockInterviewService.sendUserMessage(interview.id, userId, "답변 1")
        mockInterviewService.sendUserMessage(interview.id, userId, "답변 2")
        mockInterviewService.sendUserMessage(interview.id, userId, "답변 3")

        // when
        val messages = mockInterviewService.getMessages(interview.id)

        // then
        assertThat(messages).hasSizeGreaterThanOrEqualTo(3)
        assertThat(messages[0].messageIndex).isLessThan(messages[1].messageIndex)
        assertThat(messages[1].messageIndex).isLessThan(messages[2].messageIndex)
    }

    @Test
    fun `사용자의 모든 면접 조회 성공`() {
        // given
        val userId = nextUserId()
        createTestInterview(userId)
        createTestInterview(userId)
        createTestInterview(userId)

        // when
        val interviews = mockInterviewService.getUserInterviews(userId)

        // then
        assertThat(interviews).hasSizeGreaterThanOrEqualTo(3)
        assertThat(interviews).allMatch { it.userId == userId }
    }

    @Test
    fun `면접 상태 전환 테스트`() {
        // given
        val userId = nextUserId()
        val interview = createTestInterview(userId = userId)

        // when & then
        assertThat(interview.status).isEqualTo(MockInterviewStatus.IN_PROGRESS)

        mockInterviewService.endInterview(interview.id, userId)
        val ended = mockInterviewRepository.findById(interview.id).get()
        assertThat(ended.status).isEqualTo(MockInterviewStatus.COMPLETED)
    }

    // === Helper Methods ===

    private fun createTestInterview(userId: Long): MockInterview {
        return mockInterviewService.startInterview(
            userId = userId,
            jobPostingId = null,
            selectedJobField = JobField.IT
        )
    }

    private fun createMultipleMessages(interviewId: Long, count: Int) {
        repeat(count) { index ->
            interviewMessageRepository.save(
                InterviewMessage(
                    mockInterviewId = interviewId,
                    sender = if (index % 2 == 0) MessageSender.AI else MessageSender.USER,
                    content = "메시지 $index",
                    messageIndex = index
                )
            )
        }
    }
}
