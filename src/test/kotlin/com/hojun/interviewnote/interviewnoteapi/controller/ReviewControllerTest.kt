package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.domain.User
import com.hojun.interviewnote.interviewnoteapi.domain.UserRole
import com.hojun.interviewnote.interviewnoteapi.dto.AnswerWithFeedbackDto
import com.hojun.interviewnote.interviewnoteapi.dto.FeedbackDto
import com.hojun.interviewnote.interviewnoteapi.dto.ReviewSummaryDto
import com.hojun.interviewnote.interviewnoteapi.repository.UserRepository
import com.hojun.interviewnote.interviewnoteapi.service.InterviewService
import com.hojun.interviewnote.interviewnoteapi.service.ReviewService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@WebMvcTest(ReviewController::class)
@Import(com.hojun.interviewnote.interviewnoteapi.config.SecurityConfig::class)
class ReviewControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var reviewService: ReviewService

    @MockitoBean
    private lateinit var interviewService: InterviewService

    @MockitoBean
    private lateinit var userRepository: UserRepository

    private val testUser = User(
        id = 1L,
        email = "user",
        passwordHash = "hashed",
        name = "Test User",
        role = UserRole.USER,
        isActive = true,
        createdAt = LocalDateTime.now()
    )

    @Test
    @WithMockUser(username = "user")
    fun`리뷰 이력 목록을 렌더링한다`() {
        // given
        val reviews = listOf(
            ReviewSummaryDto(
                answerId = 1L,
                questionContent = "질문1",
                category = "기술역량",
                answeredAt = LocalDateTime.now(),
                averageScore = 4.0
            ),
            ReviewSummaryDto(
                answerId = 2L,
                questionContent = "질문2",
                category = "문제해결",
                answeredAt = LocalDateTime.now(),
                averageScore = 3.5
            )
        )
        whenever(userRepository.findByEmail("user")).thenReturn(testUser)
        whenever(reviewService.getUserReviews(1L)).thenReturn(reviews)

        // when & then
        mockMvc.perform(get("/reviews"))
            .andExpect(status().isOk)
            .andExpect(view().name("reviews/list"))
            .andExpect(model().attributeExists("reviews"))
            .andExpect(model().attribute("reviews", reviews))
    }

    @Test
    @WithMockUser(username = "user")
    fun`리뷰 이력이 없으면 빈 목록을 렌더링한다`() {
        // given
        whenever(userRepository.findByEmail("user")).thenReturn(testUser)
        whenever(reviewService.getUserReviews(1L)).thenReturn(emptyList())

        // when & then
        mockMvc.perform(get("/reviews"))
            .andExpect(status().isOk)
            .andExpect(view().name("reviews/list"))
            .andExpect(model().attribute("reviews", emptyList<ReviewSummaryDto>()))
    }

    // 템플릿 렌더링 테스트는 Integration Test에서 수행
    // @WebMvcTest에서는 템플릿 파일을 찾지 못할 수 있음
}
