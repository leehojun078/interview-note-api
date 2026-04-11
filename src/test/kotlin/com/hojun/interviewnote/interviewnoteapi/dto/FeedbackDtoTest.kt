package com.hojun.interviewnote.interviewnoteapi.dto

import com.hojun.interviewnote.interviewnoteapi.domain.AiFeedback
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class FeedbackDtoTest {

    private fun createAiFeedback(
        strengths: String = "[\"강점1\", \"강점2\"]",
        improvements: String = "[\"개선점1\", \"개선점2\"]"
    ): AiFeedback {
        return AiFeedback(
            id = 1L,
            interviewAnswerId = 1L,
            logicScore = 4,
            specificityScore = 3,
            jobFitScore = 4,
            deliveryScore = 3,
            strengths = strengths,
            improvements = improvements,
            modelAnswer = "모범답변입니다.",
            overallComment = "전반적으로 좋습니다.",
            jobField = "IT",
            modelName = "test-model",
            promptVersion = "v1.0",
            tokenUsageInput = 100,
            tokenUsageOutput = 200,
            rawResponse = "{}",
            createdAt = LocalDateTime.now()
        )
    }

    @Test
    fun `AiFeedback을 FeedbackDto로 변환한다`() {
        // given
        val aiFeedback = createAiFeedback()

        // when
        val dto = FeedbackDto.from(aiFeedback)

        // then
        assertThat(dto.logicScore).isEqualTo(4)
        assertThat(dto.specificityScore).isEqualTo(3)
        assertThat(dto.jobFitScore).isEqualTo(4)
        assertThat(dto.deliveryScore).isEqualTo(3)
        assertThat(dto.modelAnswer).isEqualTo("모범답변입니다.")
        assertThat(dto.overallComment).isEqualTo("전반적으로 좋습니다.")
    }

    @Test
    fun `averageScore를 정확히 계산한다`() {
        // given
        val aiFeedback = createAiFeedback()
        val dto = FeedbackDto.from(aiFeedback)

        // when
        val averageScore = dto.averageScore

        // then
        assertThat(averageScore).isEqualTo(3.5) // (4+3+4+3)/4
    }

    @Test
    fun `JSON 배열을 List로 파싱한다`() {
        // given
        val aiFeedback = createAiFeedback(
            strengths = "[\"강점1\", \"강점2\", \"강점3\"]",
            improvements = "[\"개선점1\"]"
        )

        // when
        val dto = FeedbackDto.from(aiFeedback)

        // then
        assertThat(dto.strengths).hasSize(3)
        assertThat(dto.strengths).containsExactly("강점1", "강점2", "강점3")
        assertThat(dto.improvements).hasSize(1)
        assertThat(dto.improvements).containsExactly("개선점1")
    }

    @Test
    fun `잘못된 JSON 형식이면 빈 리스트를 반환한다`() {
        // given
        val aiFeedback = createAiFeedback(
            strengths = "invalid json",
            improvements = "also invalid"
        )

        // when
        val dto = FeedbackDto.from(aiFeedback)

        // then
        assertThat(dto.strengths).isEmpty()
        assertThat(dto.improvements).isEmpty()
    }
}
