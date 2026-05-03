package com.hojun.interviewnote.interviewnoteapi

import com.hojun.interviewnote.interviewnoteapi.domain.InterviewMessage
import com.hojun.interviewnote.interviewnoteapi.domain.MessageSender
import com.hojun.interviewnote.interviewnoteapi.service.InterviewAiService
import com.hojun.interviewnote.interviewnoteapi.service.ai.AiClient
import com.hojun.interviewnote.interviewnoteapi.service.ai.InterviewResponseParser
import com.hojun.interviewnote.interviewnoteapi.service.ai.PromptBuilder
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

/**
 * Phase 8A: 가중 평균 점수 계산 로직 테스트
 *
 * 요구사항:
 * - 첫 답변(자기소개) 제외
 * - 저품질 답변 50% 이상 시 0.8 패널티
 * - 짧은 답변(50자 미만) 평균 점수 < 2.0점
 */
@DisplayName("Phase 8A: 가중 평균 점수 계산")
class Phase8AWeightedScoreTest {

    private val aiClient = mock(AiClient::class.java)
    private val promptBuilder = mock(PromptBuilder::class.java)
    private val parser = mock(InterviewResponseParser::class.java)
    private val meterRegistry = SimpleMeterRegistry()

    private val interviewAiService = InterviewAiService(
        aiClient, promptBuilder, parser, meterRegistry
    )

    @Test
    @DisplayName("첫 답변 제외한 가중 평균 계산")
    fun `should exclude first answer from weighted average`() {
        // Given: 5개 답변 (첫 답변 4.0점, 나머지 2.0점)
        val messages = listOf(
            createUserMessage(1, 4.0, 4, 4, 4),  // 자기소개 (제외됨)
            createAiMessage(2),
            createUserMessage(3, 2.0, 2, 2, 2),  // 포함
            createAiMessage(4),
            createUserMessage(5, 2.0, 2, 2, 2),  // 포함
            createAiMessage(6),
            createUserMessage(7, 2.0, 2, 2, 2)   // 포함
        )

        // When: 가중 평균 계산
        val weightedScore = interviewAiService.calculateWeightedScore(messages)

        // Then: 첫 답변 제외한 평균 = 2.0
        assertThat(weightedScore).isEqualTo(2.0)
    }

    @Test
    @DisplayName("저품질 답변 50% 이상 시 패널티 적용")
    fun `should apply penalty when low quality answers exceed 50 percent`() {
        // Given: 4개 답변 (첫 답변 제외 후 3개, 그 중 2개가 저품질)
        val messages = listOf(
            createUserMessage(1, 3.0, 3, 3, 3),  // 자기소개 (제외)
            createAiMessage(2),
            createUserMessage(3, 1.5, 1, 2, 2),  // 저품질 (< 2.0)
            createAiMessage(4),
            createUserMessage(5, 1.8, 2, 2, 1),  // 저품질 (< 2.0)
            createAiMessage(6),
            createUserMessage(7, 3.0, 3, 3, 3)   // 정상
        )

        // When: 가중 평균 계산
        val weightedScore = interviewAiService.calculateWeightedScore(messages)

        // Then: 평균 = (1.5 + 1.8 + 3.0) / 3 = 2.1
        //       저품질 비율 = 2/3 = 66.7% >= 50%
        //       패널티 = 2.1 * 0.8 = 1.68
        val expectedAvg = (1.5 + 1.8 + 3.0) / 3
        val expectedPenalty = expectedAvg * 0.8

        assertThat(weightedScore).isCloseTo(expectedPenalty, org.assertj.core.data.Offset.offset(0.01))
        assertThat(weightedScore).isLessThan(2.0)
    }

    @Test
    @DisplayName("저품질 답변 50% 미만 시 패널티 없음")
    fun `should not apply penalty when low quality answers below 50 percent`() {
        // Given: 4개 답변 (첫 답변 제외 후 3개, 그 중 1개만 저품질)
        val messages = listOf(
            createUserMessage(1, 4.0, 4, 4, 4),  // 자기소개 (제외) avg = 4.0
            createAiMessage(2),
            createUserMessage(3, 1.67, 1, 2, 2),  // 저품질 avg = 5/3 = 1.67
            createAiMessage(4),
            createUserMessage(5, 3.67, 4, 3, 4),  // 정상 avg = 11/3 = 3.67
            createAiMessage(6),
            createUserMessage(7, 4.0, 4, 4, 4)   // 정상 avg = 4.0
        )

        // When: 가중 평균 계산
        val weightedScore = interviewAiService.calculateWeightedScore(messages)

        // Then: 평균 = (5/3 + 11/3 + 12/3) / 3 = (1.67 + 3.67 + 4.0) / 3 = 3.11
        //       저품질 비율 = 1/3 = 33.3% < 50%
        //       패널티 없음
        val score3 = (1 + 2 + 2) / 3.0
        val score5 = (4 + 3 + 4) / 3.0
        val score7 = (4 + 4 + 4) / 3.0
        val expectedAvg = (score3 + score5 + score7) / 3

        assertThat(weightedScore).isCloseTo(expectedAvg, org.assertj.core.data.Offset.offset(0.01))
        assertThat(weightedScore).isGreaterThan(2.5)
    }

    @Test
    @DisplayName("자기소개만 있는 경우 첫 답변 점수 반환")
    fun `should return first answer score when only intro exists`() {
        // Given: 자기소개 1개만 (logic:4, specificity:3, delivery:4 → avg = 11/3 = 3.67)
        val messages = listOf(
            createUserMessage(1, 3.67, 4, 3, 4),
            createAiMessage(2)
        )

        // When: 가중 평균 계산
        val weightedScore = interviewAiService.calculateWeightedScore(messages)

        // Then: 첫 답변 점수 그대로 반환 (11/3 = 3.67)
        val expected = (4 + 3 + 4) / 3.0
        assertThat(weightedScore).isCloseTo(expected, org.assertj.core.data.Offset.offset(0.01))
    }

    @Test
    @DisplayName("짧은 답변 시뮬레이션 - 평균 점수 < 2.0")
    fun `should have low average score for short answers`() {
        // Given: 짧은 답변들 (자기소개 제외 후 모두 저품질)
        val messages = listOf(
            createUserMessage(1, 3.0, 3, 3, 3),  // 자기소개 (제외)
            createAiMessage(2),
            createUserMessage(3, 1.0, 1, 1, 1),  // "ㅎㅎ"
            createAiMessage(4),
            createUserMessage(5, 1.5, 2, 1, 2),  // "열심히 노력"
            createAiMessage(6),
            createUserMessage(7, 1.3, 1, 2, 1)   // "안녕하세요"
        )

        // When: 가중 평균 계산
        val weightedScore = interviewAiService.calculateWeightedScore(messages)

        // Then: 평균 = (1.0 + 1.5 + 1.3) / 3 = 1.27
        //       저품질 비율 = 3/3 = 100% >= 50%
        //       패널티 = 1.27 * 0.8 = 1.01
        assertThat(weightedScore).isLessThan(2.0)
        assertThat(weightedScore).isGreaterThanOrEqualTo(1.0)
    }

    // ===== Helper Methods =====

    private fun createUserMessage(
        index: Int,
        @Suppress("UNUSED_PARAMETER") avgScore: Double,  // 문서화 목적 (실제 averageScore는 계산 프로퍼티)
        logic: Int,
        specificity: Int,
        delivery: Int
    ): InterviewMessage {
        return InterviewMessage(
            id = index.toLong(),
            mockInterviewId = 1L,
            sender = MessageSender.USER,
            content = "답변 내용 $index",
            messageIndex = index,
            logicScore = logic,
            specificityScore = specificity,
            deliveryScore = delivery,
            feedbackComment = "피드백 $index"
        )
    }

    private fun createAiMessage(index: Int): InterviewMessage {
        return InterviewMessage(
            id = index.toLong(),
            mockInterviewId = 1L,
            sender = MessageSender.AI,
            content = "질문 내용 $index",
            messageIndex = index
        )
    }
}
