package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.domain.*
import com.hojun.interviewnote.interviewnoteapi.repository.MockInterviewRepository
import com.hojun.interviewnote.interviewnoteapi.repository.UserRepository
import com.hojun.interviewnote.interviewnoteapi.service.MockInterviewService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

/**
 * MockInterviewController 테스트
 *
 * Phase 7C: SSE 엔드포인트 기본 동작 검증
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MockInterviewControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var mockInterviewRepository: MockInterviewRepository

    @Autowired
    private lateinit var mockInterviewService: MockInterviewService

    private lateinit var testUser: User
    private val testEmail = "mockinterviewtest@example.com"

    @BeforeEach
    fun setUp() {
        // 테스트 사용자 생성
        testUser = userRepository.save(
            User(
                email = testEmail,
                passwordHash = "encodedPassword",
                name = "TestUser",
                role = UserRole.USER,
                jobField = JobField.IT,
                careerLevel = CareerLevel.JUNIOR
            )
        )
    }

    @Test
    @WithMockUser(username = "mockinterviewtest@example.com")
    fun `면접 시작 성공`() {
        mockMvc.perform(
            post("/mock-interviews/start")
                .with(csrf())
                .param("selectedJobField", "IT")
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrlPattern("/mock-interviews/*/chat"))
    }

    @Test
    @WithMockUser(username = "mockinterviewtest@example.com")
    fun `채팅 페이지 조회 성공`() {
        // given: 면접 세션 생성
        val interview = mockInterviewService.startInterview(
            userId = testUser.id,
            jobPostingId = null,
            selectedJobField = JobField.IT
        )

        // when & then
        mockMvc.perform(get("/mock-interviews/${interview.id}/chat"))
            .andExpect(status().isOk)
            .andExpect(view().name("mock-interviews/chat"))
            .andExpect(model().attributeExists("interview"))
            .andExpect(model().attributeExists("messages"))
    }

    @Test
    @WithMockUser(username = "mockinterviewtest@example.com")
    fun `사용자 메시지 전송 성공`() {
        // given: 면접 세션 생성
        val interview = mockInterviewService.startInterview(
            userId = testUser.id,
            jobPostingId = null,
            selectedJobField = JobField.IT
        )

        // when & then
        mockMvc.perform(
            post("/mock-interviews/${interview.id}/messages")
                .with(csrf())
                .param("content", "안녕하세요, 저는 3년차 백엔드 개발자입니다.")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    @WithMockUser(username = "mockinterviewtest@example.com")
    fun `200자 초과 메시지 전송 실패`() {
        // given: 면접 세션 생성
        val interview = mockInterviewService.startInterview(
            userId = testUser.id,
            jobPostingId = null,
            selectedJobField = JobField.IT
        )

        // 200자 초과 메시지
        val longMessage = "a".repeat(201)

        // when & then
        mockMvc.perform(
            post("/mock-interviews/${interview.id}/messages")
                .with(csrf())
                .param("content", longMessage)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("답변은 200자 이내로 작성해주세요"))
    }

    @Test
    @WithMockUser(username = "mockinterviewtest@example.com")
    fun `SSE 스트림 연결 성공`() {
        // given: 면접 세션 생성
        val interview = mockInterviewService.startInterview(
            userId = testUser.id,
            jobPostingId = null,
            selectedJobField = JobField.IT
        )

        // when & then
        mockMvc.perform(get("/mock-interviews/${interview.id}/stream"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/event-stream"))
    }

    @Test
    @WithMockUser(username = "mockinterviewtest@example.com")
    fun `면접 종료 성공`() {
        // given: 면접 세션 생성 및 메시지 추가
        val interview = mockInterviewService.startInterview(
            userId = testUser.id,
            jobPostingId = null,
            selectedJobField = JobField.IT
        )
        mockInterviewService.sendUserMessage(interview.id, testUser.id, "첫 번째 답변")
        mockInterviewService.sendUserMessage(interview.id, testUser.id, "두 번째 답변")

        // when & then
        mockMvc.perform(
            post("/mock-interviews/${interview.id}/end")
                .with(csrf())
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/mock-interviews/${interview.id}/result"))
    }

    @Test
    @WithMockUser(username = "mockinterviewtest@example.com")
    fun `결과 페이지 조회 성공`() {
        // given: 면접 세션 생성 및 종료
        val interview = mockInterviewService.startInterview(
            userId = testUser.id,
            jobPostingId = null,
            selectedJobField = JobField.IT
        )
        mockInterviewService.sendUserMessage(interview.id, testUser.id, "답변")
        mockInterviewService.endInterview(interview.id, testUser.id)

        // when & then
        mockMvc.perform(get("/mock-interviews/${interview.id}/result"))
            .andExpect(status().isOk)
            .andExpect(view().name("mock-interviews/result"))
            .andExpect(model().attributeExists("interview"))
            .andExpect(model().attributeExists("messages"))
    }

    @Test
    @WithMockUser(username = "other@example.com")
    fun `타 사용자 면접 접근 시 예외 발생`() {
        // given: 다른 사용자 생성
        val otherUser = userRepository.save(
            User(
                email = "other@example.com",
                passwordHash = "encodedPassword",
                name = "OtherUser",
                role = UserRole.USER,
                jobField = JobField.IT,
                careerLevel = CareerLevel.JUNIOR
            )
        )

        // 첫 번째 사용자의 면접 세션
        val interview = mockInterviewService.startInterview(
            userId = testUser.id,
            jobPostingId = null,
            selectedJobField = JobField.IT
        )

        // when & then: 두 번째 사용자가 접근 시도
        mockMvc.perform(get("/mock-interviews/${interview.id}/chat"))
            .andExpect(status().is4xxClientError)
    }
}
