package com.hojun.interviewnote.interviewnoteapi

import com.hojun.interviewnote.interviewnoteapi.repository.QuestionRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
class Phase1IntegrationTest {

    @Autowired
    private lateinit var questionRepository: QuestionRepository

    @Test
    fun `초기 질문 20개가 정상적으로 삽입되었는지 확인`() {
        // Given & When
        val questions = questionRepository.findAll()

        // Then
        assertEquals(20, questions.size, "초기 질문은 20개여야 합니다")

        // 카테고리별 개수 확인
        val techQuestions = questions.filter { it.category == "기술역량" }
        val problemQuestions = questions.filter { it.category == "문제해결" }
        val teamQuestions = questions.filter { it.category == "협업경험" }

        assertEquals(13, techQuestions.size, "기술역량 질문은 13개여야 합니다")
        assertEquals(5, problemQuestions.size, "문제해결 질문은 5개여야 합니다")
        assertEquals(2, teamQuestions.size, "협업경험 질문은 2개여야 합니다")

        // 난이도별 개수 확인
        val easyQuestions = questions.filter { it.difficulty == "EASY" }
        val mediumQuestions = questions.filter { it.difficulty == "MEDIUM" }
        val hardQuestions = questions.filter { it.difficulty == "HARD" }

        assertTrue(easyQuestions.isNotEmpty(), "EASY 난이도 질문이 있어야 합니다")
        assertTrue(mediumQuestions.isNotEmpty(), "MEDIUM 난이도 질문이 있어야 합니다")
        assertTrue(hardQuestions.isNotEmpty(), "HARD 난이도 질문이 있어야 합니다")

        // 모든 질문이 활성화되어 있는지 확인
        assertTrue(questions.all { it.isActive }, "모든 질문이 활성화되어 있어야 합니다")

        // jobField가 IT인지 확인
        assertTrue(questions.all { it.jobField == "IT" }, "모든 질문의 jobField는 IT여야 합니다")
    }

    @Test
    fun `QuestionRepository의 필터링 메서드가 정상 작동하는지 확인`() {
        // Given & When
        val techQuestions = questionRepository.findByCategoryAndIsActiveTrue("기술역량")
        val mediumQuestions = questionRepository.findByDifficultyAndIsActiveTrue("MEDIUM")
        val techMediumQuestions = questionRepository.findByCategoryAndDifficultyAndIsActiveTrue("기술역량", "MEDIUM")

        // Then
        assertTrue(techQuestions.isNotEmpty(), "기술역량 카테고리 질문이 있어야 합니다")
        assertTrue(mediumQuestions.isNotEmpty(), "MEDIUM 난이도 질문이 있어야 합니다")
        assertTrue(techMediumQuestions.isNotEmpty(), "기술역량 + MEDIUM 질문이 있어야 합니다")

        // 필터링 결과 검증
        assertTrue(techQuestions.all { it.category == "기술역량" })
        assertTrue(mediumQuestions.all { it.difficulty == "MEDIUM" })
        assertTrue(techMediumQuestions.all { it.category == "기술역량" && it.difficulty == "MEDIUM" })
    }
}
