package com.hojun.interviewnote.interviewnoteapi.repository

import com.hojun.interviewnote.interviewnoteapi.domain.Question
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import java.time.LocalDateTime

@DataJpaTest
class QuestionRepositoryTest {

    @Autowired
    private lateinit var questionRepository: QuestionRepository

    @BeforeEach
    fun setUp() {
        // 기존 데이터 초기화 (Flyway 마이그레이션 데이터 제거)
        questionRepository.deleteAll()

        // 테스트 데이터 생성
        questionRepository.saveAll(
            listOf(
                Question(
                    jobField = "IT",
                    targetJob = "백엔드 개발자",
                    category = "기술역량",
                    content = "RESTful API 설계 원칙을 설명해주세요",
                    difficulty = "MEDIUM",
                    isActive = true,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                ),
                Question(
                    jobField = "IT",
                    targetJob = "백엔드 개발자",
                    category = "문제해결",
                    content = "성능 최적화 경험을 설명해주세요",
                    difficulty = "HARD",
                    isActive = true,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                ),
                Question(
                    jobField = "IT",
                    targetJob = "프론트엔드 개발자",
                    category = "기술역량",
                    content = "React Hooks에 대해 설명해주세요",
                    difficulty = "EASY",
                    isActive = true,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                ),
                Question(
                    jobField = "IT",
                    targetJob = "백엔드 개발자",
                    category = "협업경험",
                    content = "팀 프로젝트 경험을 설명해주세요",
                    difficulty = "EASY",
                    isActive = false, // 비활성화
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            )
        )
    }

    @Test
    fun `활성화된 질문만 조회한다`() {
        // when
        val questions = questionRepository.findByIsActiveTrue()

        // then
        assertThat(questions).hasSize(3)
        assertThat(questions).allMatch { it.isActive }
    }

    @Test
    fun `카테고리별 활성화된 질문을 조회한다`() {
        // when
        val questions = questionRepository.findByCategoryAndIsActiveTrue("기술역량")

        // then
        assertThat(questions).hasSize(2)
        assertThat(questions).allMatch { it.category == "기술역량" && it.isActive }
    }

    @Test
    fun `난이도별 활성화된 질문을 조회한다`() {
        // when
        val questions = questionRepository.findByDifficultyAndIsActiveTrue("EASY")

        // then
        assertThat(questions).hasSize(1)
        assertThat(questions).allMatch { it.difficulty == "EASY" && it.isActive }
    }

    @Test
    fun `카테고리와 난이도로 복합 필터링한다`() {
        // when
        val questions = questionRepository.findByCategoryAndDifficultyAndIsActiveTrue("기술역량", "MEDIUM")

        // then
        assertThat(questions).hasSize(1)
        assertThat(questions.first().category).isEqualTo("기술역량")
        assertThat(questions.first().difficulty).isEqualTo("MEDIUM")
        assertThat(questions.first().isActive).isTrue()
    }
}
