package com.hojun.interviewnote.interviewnoteapi.repository

import com.hojun.interviewnote.interviewnoteapi.domain.AiFeedback
import com.hojun.interviewnote.interviewnoteapi.domain.InterviewAnswer
import com.hojun.interviewnote.interviewnoteapi.domain.Question
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import java.time.LocalDateTime

@DataJpaTest
class AiFeedbackRepositoryTest {

    @Autowired
    private lateinit var aiFeedbackRepository: AiFeedbackRepository

    @Autowired
    private lateinit var entityManager: TestEntityManager

    private var answerId1: Long = 0
    private var answerId2: Long = 0

    @BeforeEach
    fun setUp() {
        // Foreign Key 제약을 위해 Question과 InterviewAnswer 먼저 생성
        val question = Question(
            jobField = "IT",
            targetJob = "백엔드 개발자",
            category = "기술역량",
            content = "테스트 질문",
            difficulty = "MEDIUM",
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(question)

        val answer1 = InterviewAnswer(
            questionId = question.id,
            userId = 1L,
            answerText = "답변1",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val answer2 = InterviewAnswer(
            questionId = question.id,
            userId = 1L,
            answerText = "답변2",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(answer1)
        entityManager.persist(answer2)
        entityManager.flush()

        answerId1 = answer1.id
        answerId2 = answer2.id

        // 테스트 데이터 생성
        aiFeedbackRepository.saveAll(
            listOf(
                AiFeedback(
                    interviewAnswerId = answer1.id,
                    logicScore = 4,
                    specificityScore = 3,
                    jobFitScore = 4,
                    deliveryScore = 3,
                    strengths = "[\"강점1\", \"강점2\"]",
                    improvements = "[\"개선점1\", \"개선점2\"]",
                    modelAnswer = "모범답변입니다.",
                    overallComment = "전반적으로 좋습니다.",
                    jobField = "IT",
                    modelName = "test-model",
                    promptVersion = "v1.0",
                    tokenUsageInput = 100,
                    tokenUsageOutput = 200,
                    rawResponse = "{}",
                    createdAt = LocalDateTime.now()
                ),
                AiFeedback(
                    interviewAnswerId = answer2.id,
                    logicScore = 3,
                    specificityScore = 3,
                    jobFitScore = 3,
                    deliveryScore = 3,
                    strengths = "[\"강점A\"]",
                    improvements = "[\"개선점A\"]",
                    modelAnswer = "또 다른 모범답변입니다.",
                    overallComment = "보통입니다.",
                    jobField = "IT",
                    modelName = "test-model",
                    promptVersion = "v1.0",
                    tokenUsageInput = 150,
                    tokenUsageOutput = 250,
                    rawResponse = "{}",
                    createdAt = LocalDateTime.now()
                )
            )
        )
    }

    @Test
    fun `답변ID로 피드백을 조회한다`() {
        // when
        val feedback = aiFeedbackRepository.findByInterviewAnswerId(answerId1)

        // then
        assertThat(feedback).isNotNull
        assertThat(feedback?.interviewAnswerId).isEqualTo(answerId1)
        assertThat(feedback?.logicScore).isEqualTo(4)
    }

    @Test
    fun `존재하지 않는 답변ID로 조회시 null을 반환한다`() {
        // when
        val feedback = aiFeedbackRepository.findByInterviewAnswerId(999L)

        // then
        assertThat(feedback).isNull()
    }

    @Test
    fun `피드백의 averageScore를 계산할 수 있다`() {
        // given
        val feedback = aiFeedbackRepository.findByInterviewAnswerId(answerId1)!!

        // when
        val averageScore = feedback.averageScore

        // then
        assertThat(averageScore).isEqualTo(3.5) // (4+3+4+3)/4
    }
}
