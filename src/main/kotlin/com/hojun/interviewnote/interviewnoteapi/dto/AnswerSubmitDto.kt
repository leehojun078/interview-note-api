package com.hojun.interviewnote.interviewnoteapi.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class AnswerSubmitDto(
    @field:NotNull(message = "질문 ID는 필수입니다")
    val questionId: Long?,

    @field:NotBlank(message = "답변은 필수입니다")
    @field:Size(min = 50, max = 2000, message = "답변은 50자 이상 2000자 이하여야 합니다")
    val answerText: String?
)
