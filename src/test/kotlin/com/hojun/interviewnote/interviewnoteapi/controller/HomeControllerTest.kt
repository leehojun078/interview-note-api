package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.dto.ReviewSummaryDto
import com.hojun.interviewnote.interviewnoteapi.service.ReviewService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@WebMvcTest(HomeController::class)
class HomeControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var reviewService: ReviewService

    @Test
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
        whenever(reviewService.getReviewList()).thenReturn(recentReviews)

        // when & then
        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(view().name("home"))
            .andExpect(model().attributeExists("recentReviews"))
            .andExpect(model().attribute("recentReviews", recentReviews))
    }

    @Test
    fun `home 경로로 홈 페이지를 렌더링한다`() {
        // given
        whenever(reviewService.getReviewList()).thenReturn(emptyList())

        // when & then
        mockMvc.perform(get("/home"))
            .andExpect(status().isOk)
            .andExpect(view().name("home"))
            .andExpect(model().attributeExists("recentReviews"))
    }

    @Test
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
        whenever(reviewService.getReviewList()).thenReturn(manyReviews)

        // when & then
        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(model().attribute("recentReviews", manyReviews.take(3)))
    }
}
