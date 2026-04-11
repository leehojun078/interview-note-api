package com.hojun.interviewnote.interviewnoteapi.repository

import com.hojun.interviewnote.interviewnoteapi.domain.InterviewAnswer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import java.time.LocalDateTime

@DataJpaTest
class InterviewAnswerRepositoryTest {

    @Autowired
    private lateinit var interviewAnswerRepository: InterviewAnswerRepository

    @BeforeEach
    fun setUp() {
        // 테스트 데이터 생성 (시간 순서대로)
        interviewAnswerRepository.saveAll(
            listOf(
                InterviewAnswer(
                    questionId = 1L,
                    answerText = "첫 번째 답변입니다.",
                    createdAt = LocalDateTime.now().minusDays(2),
                    updatedAt = LocalDateTime.now().minusDays(2)
                ),
                InterviewAnswer(
                    questionId = 1L,
                    answerText = "두 번째 답변입니다.",
                    createdAt = LocalDateTime.now().minusDays(1),
                    updatedAt = LocalDateTime.now().minusDays(1)
                ),
                InterviewAnswer(
                    questionId = 2L,
                    answerText = "다른 질문에 대한 답변입니다.",
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            )
        )
    }

    @Test
    fun `특정 질문에 대한 답변을 조회한다`() {
        // when
        val answers = interviewAnswerRepository.findByQuestionId(1L)

        // then
        assertThat(answers).hasSize(2)
        assertThat(answers).allMatch { it.questionId == 1L }
    }

    @Test
    fun `모든 답변을 최신순으로 조회한다`() {
        // when
        val answers = interviewAnswerRepository.findAllByOrderByCreatedAtDesc()

        // then
        assertThat(answers).hasSize(3)
        // 최신 답변이 첫 번째
        assertThat(answers[0].answerText).isEqualTo("다른 질문에 대한 답변입니다.")
        assertThat(answers[1].answerText).isEqualTo("두 번째 답변입니다.")
        assertThat(answers[2].answerText).isEqualTo("첫 번째 답변입니다.")
    }

    @Test
    fun `존재하지 않는 질문ID로 조회시 빈 리스트를 반환한다`() {
        // when
        val answers = interviewAnswerRepository.findByQuestionId(999L)

        // then
        assertThat(answers).isEmpty()
    }
}
