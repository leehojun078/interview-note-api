package com.hojun.interviewnote.interviewnoteapi.service

import com.hojun.interviewnote.interviewnoteapi.domain.*
import com.hojun.interviewnote.interviewnoteapi.exception.MaxTurnExceededException
import com.hojun.interviewnote.interviewnoteapi.exception.MockInterviewAccessDeniedException
import com.hojun.interviewnote.interviewnoteapi.repository.InterviewMessageRepository
import com.hojun.interviewnote.interviewnoteapi.repository.MockInterviewRepository
import com.hojun.interviewnote.interviewnoteapi.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

/**
 * MockInterviewService 단위 테스트
 *
 * Phase 7E: 세션 관리, 메시지 저장, 상태 전환 검증
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

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        testUser = userRepository.save(
            User(
                email = "servicetest@example.com",
                passwordHash = "password",
                name = "ServiceTestUser",
                role = UserRole.USER,
                jobField = JobField.IT,
                careerLevel = CareerLevel.JUNIOR
            )
        )
    }

    @Test
    fun `면접 시작 시 세션 생성 및 첫 질문 자동 생성`() {
        // when
        val interview = mockInterviewService.startInterview(
            userId = testUser.id,
            jobPostingId = null,
            selectedJobField = JobField.IT
        )

        // then: 세션 생성
        assertThat(interview.id).isGreaterThan(0)
        assertThat(interview.userId).isEqualTo(testUser.id)
        assertThat(interview.status).isEqualTo(MockInterviewStatus.IN_PROGRESS)
        assertThat(interview.startedAt).isNotNull()
        assertThat(interview.endedAt).isNull()

        // then: 첫 질문 자동 생성
        val messages = mockInterviewService.getMessages(interview.id)
        assertThat(messages).hasSize(1)
        assertThat(messages[0].sender).isEqualTo(MessageSender.AI)
        assertThat(messages[0].messageIndex).isEqualTo(0)
    }

    @Test
    fun `사용자 메시지 전송 시 메시지 저장 및 인덱스 증가`() {
        // given: 면접 시작
        val interview = mockInterviewService.startInterview(testUser.id, null, JobField.IT)
        val initialCount = mockInterviewService.getMessages(interview.id).size

        // when: 사용자 메시지 전송
        val userMessage = mockInterviewService.sendUserMessage(
            interviewId = interview.id,
            userId = testUser.id,
            content = "첫 번째 답변입니다."
        )

        // then
        assertThat(userMessage.sender).isEqualTo(MessageSender.USER)
        assertThat(userMessage.content).isEqualTo("첫 번째 답변입니다.")
        assertThat(userMessage.messageIndex).isEqualTo(initialCount)

        // then: DB에 저장됨
        val saved = interviewMessageRepository.findById(userMessage.id)
        assertThat(saved).isPresent
    }

    @Test
    fun `메시지 인덱스가 순차적으로 증가`() {
        // given: 면접 시작
        val interview = mockInterviewService.startInterview(testUser.id, null, JobField.IT)

        // when: 여러 메시지 전송
        val msg1 = mockInterviewService.sendUserMessage(interview.id, testUser.id, "첫 번째")
        Thread.sleep(500)
        val msg2 = mockInterviewService.sendUserMessage(interview.id, testUser.id, "두 번째")
        Thread.sleep(500)
        val msg3 = mockInterviewService.sendUserMessage(interview.id, testUser.id, "세 번째")

        // then: 인덱스가 1, 3, 5 (AI 응답이 2, 4, 6에 들어감)
        assertThat(msg1.messageIndex).isEqualTo(1)  // 첫 AI 질문이 0
        assertThat(msg2.messageIndex).isGreaterThan(msg1.messageIndex)
        assertThat(msg3.messageIndex).isGreaterThan(msg2.messageIndex)
    }

    @Test
    fun `30턴 제한 초과 시 예외 발생`() {
        // given: 면접 시작
        val interview = mockInterviewService.startInterview(testUser.id, null, JobField.IT)

        // when: 메시지를 계속 전송하다가 MaxTurnExceededException 발생 확인
        var exceptionThrown = false

        try {
            // 최대 35개까지 시도
            for (i in 1..35) {
                mockInterviewService.sendUserMessage(interview.id, testUser.id, "메시지 $i")
            }
        } catch (e: MaxTurnExceededException) {
            exceptionThrown = true
        }

        // then: MaxTurnExceededException이 발생했는지 확인
        assertThat(exceptionThrown).isTrue()
    }

    @Test
    fun `종료된 면접에 메시지 전송 시 예외 발생`() {
        // given: 면접 시작 및 종료
        val interview = mockInterviewService.startInterview(testUser.id, null, JobField.IT)
        mockInterviewService.sendUserMessage(interview.id, testUser.id, "답변")
        Thread.sleep(2000)
        mockInterviewService.endInterview(interview.id, testUser.id)

        // when & then: 종료된 면접에 메시지 전송 시도
        assertThrows<Exception> {
            mockInterviewService.sendUserMessage(interview.id, testUser.id, "종료 후 메시지")
        }
    }

    @Test
    fun `타 사용자 면접 접근 시 예외 발생`() {
        // given: 다른 사용자
        val otherUser = userRepository.save(
            User(
                email = "other@example.com",
                passwordHash = "password",
                name = "OtherUser",
                role = UserRole.USER,
                jobField = JobField.IT,
                careerLevel = CareerLevel.JUNIOR
            )
        )

        // given: testUser의 면접
        val interview = mockInterviewService.startInterview(testUser.id, null, JobField.IT)

        // when & then: otherUser가 접근 시도
        assertThrows<MockInterviewAccessDeniedException> {
            mockInterviewService.sendUserMessage(interview.id, otherUser.id, "타 사용자")
        }

        assertThrows<MockInterviewAccessDeniedException> {
            mockInterviewService.getInterview(interview.id, otherUser.id)
        }
    }

    @Test
    fun `면접 종료 시 상태 변경 및 종합 평가 생성`() {
        // given: 면접 시작 및 메시지 전송
        val interview = mockInterviewService.startInterview(testUser.id, null, JobField.IT)
        mockInterviewService.sendUserMessage(interview.id, testUser.id, "첫 번째 답변")
        Thread.sleep(2000)
        mockInterviewService.sendUserMessage(interview.id, testUser.id, "두 번째 답변")
        Thread.sleep(2000)

        // when: 면접 종료
        val ended = mockInterviewService.endInterview(interview.id, testUser.id)

        // then: 상태 변경
        assertThat(ended.status).isEqualTo(MockInterviewStatus.COMPLETED)
        assertThat(ended.endedAt).isNotNull()

        // then: 종합 평가 생성
        assertThat(ended.overallFeedback).isNotBlank()
        assertThat(ended.keyStrengths).isNotNull()
        assertThat(ended.keyImprovements).isNotNull()
        assertThat(ended.averageScore).isNotNull()
        assertThat(ended.averageScore).isBetween(0.0, 5.0)
        assertThat(ended.recommendation).isNotBlank()
    }

    @Test
    fun `사용자의 모든 면접 세션 조회 - 최신순 정렬`() {
        // given: 여러 면접 생성
        val interview1 = mockInterviewService.startInterview(testUser.id, null, JobField.IT)
        Thread.sleep(100)
        val interview2 = mockInterviewService.startInterview(testUser.id, null, JobField.MARKETING)
        Thread.sleep(100)
        val interview3 = mockInterviewService.startInterview(testUser.id, null, JobField.DESIGN)

        // when: 사용자의 모든 면접 조회
        val interviews = mockInterviewService.getUserInterviews(testUser.id)

        // then: 최신순 정렬 (interview3 > interview2 > interview1)
        assertThat(interviews).hasSizeGreaterThanOrEqualTo(3)
        assertThat(interviews[0].id).isEqualTo(interview3.id)
        assertThat(interviews[1].id).isEqualTo(interview2.id)
        assertThat(interviews[2].id).isEqualTo(interview1.id)
    }

    @Test
    fun `메시지 조회 시 인덱스 순서대로 정렬`() {
        // given: 면접 시작 및 메시지 전송
        val interview = mockInterviewService.startInterview(testUser.id, null, JobField.IT)
        mockInterviewService.sendUserMessage(interview.id, testUser.id, "첫 번째")
        Thread.sleep(500)
        mockInterviewService.sendUserMessage(interview.id, testUser.id, "두 번째")
        Thread.sleep(500)

        // when: 메시지 조회
        val messages = mockInterviewService.getMessages(interview.id)

        // then: 인덱스 순서대로 정렬
        for (i in 0 until messages.size - 1) {
            assertThat(messages[i].messageIndex).isLessThan(messages[i + 1].messageIndex)
        }
    }

    @Test
    fun `SSE Emitter 등록 및 제거`() {
        // given: 면접 시작
        val interview = mockInterviewService.startInterview(testUser.id, null, JobField.IT)
        val emitter = org.springframework.web.servlet.mvc.method.annotation.SseEmitter(30000)

        // when: Emitter 등록
        mockInterviewService.registerEmitter(interview.id, emitter)

        // then: 정상 등록 (별도 검증 방법 없음, 로그 확인)

        // when: Emitter 제거
        mockInterviewService.removeEmitter(interview.id)

        // then: 정상 제거 (별도 검증 방법 없음, 로그 확인)
    }

    @Test
    fun `브로드캐스트 - Emitter가 없으면 false 반환`() {
        // given: 면접 시작 (Emitter 등록 안 함)
        val interview = mockInterviewService.startInterview(testUser.id, null, JobField.IT)
        val message = mockInterviewService.sendUserMessage(interview.id, testUser.id, "메시지")

        // when: 브로드캐스트 시도
        val result = mockInterviewService.broadcastMessage(interview.id, message)

        // then: Emitter가 없으므로 false
        assertThat(result).isFalse()
    }
}
