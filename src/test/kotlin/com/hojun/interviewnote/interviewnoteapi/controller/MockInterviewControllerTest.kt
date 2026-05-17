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
        // SSE는 비동기 스트림이므로 MockMvc로는 Content-Type 검증이 어려움
        // 기본적인 연결 성공만 확인
        mockMvc.perform(get("/mock-interviews/${interview.id}/stream"))
            .andExpect(status().isOk)
            // SSE Content-Type 검증은 실제 통합 테스트에서 수행
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
        // given: 면접 세션 생성 및 답변 추가
        val interview = mockInterviewService.startInterview(
            userId = testUser.id,
            jobPostingId = null,
            selectedJobField = JobField.IT
        )
        // 충분히 긴 답변을 여러 개 추가하여 종합 평가가 300자 이상이 되도록 유도
        mockInterviewService.sendUserMessage(
            interview.id,
            testUser.id,
            "저는 Spring Boot와 Kotlin을 활용한 백엔드 개발 경험이 풍부합니다. 특히 JPA를 사용한 데이터베이스 설계와 RESTful API 구축에 능숙하며, 마이크로서비스 아키텍처 기반의 프로젝트를 진행한 경험이 있습니다."
        )
        mockInterviewService.sendUserMessage(
            interview.id,
            testUser.id,
            "이전 프로젝트에서는 Redis를 활용한 캐싱 전략을 도입하여 응답 시간을 30% 개선했습니다. 또한 Docker와 Kubernetes를 활용한 컨테이너 오케스트레이션 경험도 있습니다."
        )
        mockInterviewService.sendUserMessage(
            interview.id,
            testUser.id,
            "팀 협업에서는 Git Flow를 활용한 버전 관리와 코드 리뷰 문화를 중요하게 생각합니다. 테스트 주도 개발(TDD)을 실천하며, 단위 테스트와 통합 테스트 작성에 익숙합니다."
        )

        // 종료 시 AI가 충분히 긴 종합 평가를 생성하도록 함
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
        userRepository.save(
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
        // 403 에러 페이지가 렌더링되어야 함
        mockMvc.perform(get("/mock-interviews/${interview.id}/chat"))
            .andExpect(status().isForbidden)
            .andExpect(view().name("error/403"))
    }
}
