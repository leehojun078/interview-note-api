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
        val allQuestions = questionRepository.findAll()
        val itQuestions = allQuestions.filter { it.jobField == "IT" }

        // Then - Phase 5: 전체 340개 질문 확인 (IT 20개 + 신규 16개 직무 320개)
        assertEquals(340, allQuestions.size, "전체 질문은 340개여야 합니다 (17개 직무 × 20개)")
        assertEquals(20, itQuestions.size, "IT 직무 질문은 20개여야 합니다")

        // IT 직무 카테고리별 개수 확인
        val techQuestions = itQuestions.filter { it.category == "기술역량" }
        val problemQuestions = itQuestions.filter { it.category == "문제해결" }
        val teamQuestions = itQuestions.filter { it.category == "협업경험" }

        assertEquals(13, techQuestions.size, "IT 기술역량 질문은 13개여야 합니다")
        assertEquals(5, problemQuestions.size, "IT 문제해결 질문은 5개여야 합니다")
        assertEquals(2, teamQuestions.size, "IT 협업경험 질문은 2개여야 합니다")

        // 난이도별 개수 확인
        val easyQuestions = allQuestions.filter { it.difficulty == "EASY" }
        val mediumQuestions = allQuestions.filter { it.difficulty == "MEDIUM" }
        val hardQuestions = allQuestions.filter { it.difficulty == "HARD" }

        assertTrue(easyQuestions.isNotEmpty(), "EASY 난이도 질문이 있어야 합니다")
        assertTrue(mediumQuestions.isNotEmpty(), "MEDIUM 난이도 질문이 있어야 합니다")
        assertTrue(hardQuestions.isNotEmpty(), "HARD 난이도 질문이 있어야 합니다")

        // 모든 질문이 활성화되어 있는지 확인
        assertTrue(allQuestions.all { it.isActive }, "모든 질문이 활성화되어 있어야 합니다")

        // Phase 5: 17개 직무 모두 있는지 확인
        val jobFields = allQuestions.map { it.jobField }.distinct().sorted()
        assertEquals(17, jobFields.size, "17개 직무가 모두 있어야 합니다")

        // 각 직무별로 20개씩 있는지 확인
        jobFields.forEach { jobField ->
            val questionsPerJob = allQuestions.filter { it.jobField == jobField }
            assertEquals(20, questionsPerJob.size, "$jobField 직무는 20개 질문이 있어야 합니다")
        }
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
