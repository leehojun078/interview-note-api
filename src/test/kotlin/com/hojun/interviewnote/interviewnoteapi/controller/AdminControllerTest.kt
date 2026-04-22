package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.domain.CareerLevel
import com.hojun.interviewnote.interviewnoteapi.domain.JobField
import com.hojun.interviewnote.interviewnoteapi.domain.User
import com.hojun.interviewnote.interviewnoteapi.domain.UserRole
import com.hojun.interviewnote.interviewnoteapi.repository.UserRepository
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

/**
 * AdminController 테스트
 *
 * Phase 5 Step 16
 */
@WebMvcTest(AdminController::class)
@Import(com.hojun.interviewnote.interviewnoteapi.config.SecurityConfig::class)
class AdminControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userRepository: UserRepository

    private val testUsers = listOf(
        User(
            id = 1L,
            email = "user1@example.com",
            passwordHash = "hash",
            name = "User 1",
            role = UserRole.USER,
            jobField = JobField.IT,
            careerLevel = CareerLevel.JUNIOR
        ),
        User(
            id = 2L,
            email = "user2@example.com",
            passwordHash = "hash",
            name = "User 2",
            role = UserRole.USER,
            jobField = JobField.IT,
            careerLevel = CareerLevel.SENIOR
        ),
        User(
            id = 3L,
            email = "user3@example.com",
            passwordHash = "hash",
            name = "User 3",
            role = UserRole.USER,
            jobField = JobField.SALES,
            careerLevel = CareerLevel.ENTRY
        ),
        User(
            id = 4L,
            email = "user4@example.com",
            passwordHash = "hash",
            name = "User 4",
            role = UserRole.USER,
            jobField = null,
            careerLevel = null
        )
    )

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `직무별 사용자 통계를 반환한다`() {
        // given
        whenever(userRepository.findAll()).thenReturn(testUsers)

        // when & then
        mockMvc.perform(get("/api/admin/stats/job-fields"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.IT").value(2))
            .andExpect(jsonPath("$.SALES").value(1))
            .andExpect(jsonPath("$.NONE").value(1))
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `경력별 사용자 통계를 반환한다`() {
        // given
        whenever(userRepository.findAll()).thenReturn(testUsers)

        // when & then
        mockMvc.perform(get("/api/admin/stats/career-levels"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.JUNIOR").value(1))
            .andExpect(jsonPath("$.SENIOR").value(1))
            .andExpect(jsonPath("$.ENTRY").value(1))
            .andExpect(jsonPath("$.NONE").value(1))
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `전체 통계 요약을 반환한다`() {
        // given
        whenever(userRepository.findAll()).thenReturn(testUsers)

        // when & then
        mockMvc.perform(get("/api/admin/stats/summary"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalUsers").value(4))
            .andExpect(jsonPath("$.activeUsers").value(4))
            .andExpect(jsonPath("$.usersWithJobField").value(3))
            .andExpect(jsonPath("$.usersWithCareerLevel").value(3))
            .andExpect(jsonPath("$.jobFieldDistribution['IT개발']").value(2))
            .andExpect(jsonPath("$.jobFieldDistribution['영업·판매·무역']").value(1))
            .andExpect(jsonPath("$.jobFieldDistribution['미설정']").value(1))
    }

    @Test
    @WithMockUser(username = "user", roles = ["USER"])
    fun `일반 사용자는 통계 조회 시 403 에러가 발생한다`() {
        // when & then
        mockMvc.perform(get("/api/admin/stats/job-fields"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `비로그인 사용자는 통계 조회 시 302 리다이렉트가 발생한다`() {
        // when & then
        mockMvc.perform(get("/api/admin/stats/job-fields"))
            .andExpect(status().is3xxRedirection)
    }
}
