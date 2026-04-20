package com.hojun.interviewnote.interviewnoteapi.dto

import com.hojun.interviewnote.interviewnoteapi.domain.CareerLevel
import com.hojun.interviewnote.interviewnoteapi.domain.JobField
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * 사용자 프로필 정보 DTO
 */
data class UserProfileDto(
    val id: Long,
    val email: String,
    val name: String,
    val jobField: JobField?,
    val careerLevel: CareerLevel?,
    val createdAt: LocalDateTime
)

/**
 * 프로필 업데이트 요청 DTO
 */
data class UpdateProfileRequest(
    @field:NotBlank(message = "이름을 입력해주세요")
    @field:Size(min = 2, max = 50, message = "이름은 2-50자 이내여야 합니다")
    val name: String,

    val jobField: JobField?,
    val careerLevel: CareerLevel?
)
