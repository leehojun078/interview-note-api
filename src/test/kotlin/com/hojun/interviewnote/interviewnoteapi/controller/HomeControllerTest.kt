package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.domain.JobField
import com.hojun.interviewnote.interviewnoteapi.domain.User
import com.hojun.interviewnote.interviewnoteapi.domain.UserRole
import com.hojun.interviewnote.interviewnoteapi.dto.QuestionDto
import com.hojun.interviewnote.interviewnoteapi.dto.ReviewSummaryDto
import com.hojun.interviewnote.interviewnoteapi.service.QuestionService
import com.hojun.interviewnote.interviewnoteapi.service.ReviewService
import com.hojun.interviewnote.interviewnoteapi.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
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

@WebMvcTest(HomeController::class)
@Import(com.hojun.interviewnote.interviewnoteapi.config.SecurityConfig::class)
class HomeControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var reviewService: ReviewService

    @MockitoBean
    private lateinit var userService: UserService

    @MockitoBean
    private lateinit var questionService: QuestionService

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
    fun `루트 경로로 홈 페이지를 렌더링한다`() {
        // given
        val recentReviews = listOf(
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
            ),
            ReviewSummaryDto(
                answerId = 3L,
                questionContent = "질문3",
                category = "협업경험",
                answeredAt = LocalDateTime.now(),
                averageScore = 3.0
            )
        )
        val recommendedQuestions = listOf(
            QuestionDto(1L, "IT", "백엔드 개발자", "기술역량", "질문1", "EASY"),
            QuestionDto(2L, "IT", "백엔드 개발자", "문제해결", "질문2", "MEDIUM"),
            QuestionDto(3L, "IT", "백엔드 개발자", "협업경험", "질문3", "HARD"),
            QuestionDto(4L, "IT", "백엔드 개발자", "기술역량", "질문4", "MEDIUM"),
            QuestionDto(5L, "IT", "백엔드 개발자", "문제해결", "질문5", "EASY")
        )
        whenever(userService.findByEmail("user")).thenReturn(testUser)
        whenever(reviewService.getUserReviews(1L)).thenReturn(recentReviews)
        whenever(questionService.findAll("IT", null, null)).thenReturn(recommendedQuestions)

        // when & then
        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(view().name("home"))
            .andExpect(model().attributeExists("recentReviews"))
            .andExpect(model().attribute("recentReviews", recentReviews.take(3)))
            .andExpect(model().attributeExists("recommendedQuestions"))
            .andExpect(model().attributeExists("hasJobFieldSet"))
            .andExpect(model().attribute("hasJobFieldSet", false))
    }

    @Test
    @WithMockUser(username = "user")
    fun `home 경로로 홈 페이지를 렌더링한다`() {
        // given
        whenever(userService.findByEmail("user")).thenReturn(testUser)
        whenever(reviewService.getUserReviews(1L)).thenReturn(emptyList())
        whenever(questionService.findAll("IT", null, null)).thenReturn(emptyList())

        // when & then
        mockMvc.perform(get("/home"))
            .andExpect(status().isOk)
            .andExpect(view().name("home"))
            .andExpect(model().attributeExists("recentReviews"))
            .andExpect(model().attributeExists("recommendedQuestions"))
    }

    @Test
    @WithMockUser(username = "user")
    fun `최근 답변 3개만 표시한다`() {
        // given
        val manyReviews = (1..10).map { i ->
            ReviewSummaryDto(
                answerId = i.toLong(),
                questionContent = "질문$i",
                category = "기술역량",
                answeredAt = LocalDateTime.now(),
                averageScore = 3.5
            )
        }
        whenever(userService.findByEmail("user")).thenReturn(testUser)
        whenever(reviewService.getUserReviews(1L)).thenReturn(manyReviews)
        whenever(questionService.findAll("IT", null, null)).thenReturn(emptyList())

        // when & then
        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(model().attribute("recentReviews", manyReviews.take(3)))
    }
}
