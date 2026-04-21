package com.hojun.interviewnote.interviewnoteapi.controller

import com.hojun.interviewnote.interviewnoteapi.domain.CareerLevel
import com.hojun.interviewnote.interviewnoteapi.domain.JobField
import com.hojun.interviewnote.interviewnoteapi.domain.User
import com.hojun.interviewnote.interviewnoteapi.domain.UserRole
import com.hojun.interviewnote.interviewnoteapi.dto.UpdateProfileRequest
import com.hojun.interviewnote.interviewnoteapi.dto.UserProfileDto
import com.hojun.interviewnote.interviewnoteapi.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@WebMvcTest(ProfileController::class)
@Import(com.hojun.interviewnote.interviewnoteapi.config.SecurityConfig::class)
class ProfileControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userService: UserService

    private fun createUser(
        id: Long = 1L,
        email: String = "test@example.com",
        name: String = "테스트",
        jobField: JobField? = null,
        careerLevel: CareerLevel? = null
    ): User {
        return User(
            id = id,
            email = email,
            passwordHash = "hashed",
            name = name,
            role = UserRole.USER,
            jobField = jobField,
            careerLevel = careerLevel
        )
    }

    private fun createProfileDto(
        id: Long = 1L,
        email: String = "test@example.com",
        name: String = "테스트",
        jobField: JobField? = null,
        careerLevel: CareerLevel? = null
    ): UserProfileDto {
        return UserProfileDto(
            id = id,
            email = email,
            name = name,
            jobField = jobField,
            careerLevel = careerLevel,
            createdAt = LocalDateTime.now()
        )
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `GET profile - 프로필 설정 페이지를 렌더링한다`() {
        // given
        val user = createUser()
        val profile = createProfileDto()

        whenever(userService.findByEmail("test@example.com")).thenReturn(user)
        whenever(userService.getProfile(1L)).thenReturn(profile)

        // when & then
        mockMvc.perform(get("/profile"))
            .andExpect(status().isOk)
            .andExpect(view().name("profile/settings"))
            .andExpect(model().attributeExists("profile"))
            .andExpect(model().attributeExists("jobFields"))
            .andExpect(model().attributeExists("careerLevels"))
            .andExpect(model().attribute("profile", profile))
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `GET profile - 직무와 경력이 설정된 사용자의 프로필 페이지`() {
        // given
        val user = createUser(jobField = JobField.IT, careerLevel = CareerLevel.JUNIOR)
        val profile = createProfileDto(jobField = JobField.IT, careerLevel = CareerLevel.JUNIOR)

        whenever(userService.findByEmail("test@example.com")).thenReturn(user)
        whenever(userService.getProfile(1L)).thenReturn(profile)

        // when & then
        mockMvc.perform(get("/profile"))
            .andExpect(status().isOk)
            .andExpect(view().name("profile/settings"))
            .andExpect(model().attribute("profile", profile))
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `POST profile update - 프로필을 성공적으로 업데이트한다`() {
        // given
        val user = createUser()
        val updatedUser = createUser(name = "수정된이름", jobField = JobField.SALES, careerLevel = CareerLevel.SENIOR)

        whenever(userService.findByEmail("test@example.com")).thenReturn(user)
        whenever(userService.updateProfile(any(), any())).thenReturn(updatedUser)

        // when & then
        mockMvc.perform(
            post("/profile/update")
                .with(csrf())
                .param("name", "수정된이름")
                .param("jobField", "SALES")
                .param("careerLevel", "SENIOR")
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/profile"))
            .andExpect(flash().attribute("success", "프로필이 업데이트되었습니다"))
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `POST profile update - 이름이 비어있으면 검증 실패`() {
        // given
        val user = createUser()
        whenever(userService.findByEmail("test@example.com")).thenReturn(user)

        // when & then
        mockMvc.perform(
            post("/profile/update")
                .with(csrf())
                .param("name", "")
                .param("jobField", "IT")
                .param("careerLevel", "ENTRY")
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/profile"))
            .andExpect(flash().attribute("error", "입력 값을 확인해주세요"))
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `POST profile update - 이름이 1자이면 검증 실패`() {
        // given
        val user = createUser()
        whenever(userService.findByEmail("test@example.com")).thenReturn(user)

        // when & then
        mockMvc.perform(
            post("/profile/update")
                .with(csrf())
                .param("name", "김")
                .param("jobField", "IT")
                .param("careerLevel", "ENTRY")
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/profile"))
            .andExpect(flash().attribute("error", "입력 값을 확인해주세요"))
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `POST profile update - jobField가 없으면 검증 실패`() {
        // given
        val user = createUser()
        whenever(userService.findByEmail("test@example.com")).thenReturn(user)

        // when & then
        mockMvc.perform(
            post("/profile/update")
                .with(csrf())
                .param("name", "테스트")
                .param("careerLevel", "ENTRY")
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/profile"))
            .andExpect(flash().attribute("error", "입력 값을 확인해주세요"))
    }

    @Test
    @WithMockUser(username = "test@example.com")
    fun `POST profile update - careerLevel이 없으면 검증 실패`() {
        // given
        val user = createUser()
        whenever(userService.findByEmail("test@example.com")).thenReturn(user)

        // when & then
        mockMvc.perform(
            post("/profile/update")
                .with(csrf())
                .param("name", "테스트")
                .param("jobField", "IT")
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/profile"))
            .andExpect(flash().attribute("error", "입력 값을 확인해주세요"))
    }

    @Test
    fun `GET profile - 인증되지 않은 사용자는 접근 불가`() {
        // when & then
        mockMvc.perform(get("/profile"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrlPattern("**/login"))
    }

    @Test
    fun `POST profile update - 인증되지 않은 사용자는 접근 불가`() {
        // when & then
        mockMvc.perform(
            post("/profile/update")
                .with(csrf())
                .param("name", "테스트")
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrlPattern("**/login"))
    }
}
