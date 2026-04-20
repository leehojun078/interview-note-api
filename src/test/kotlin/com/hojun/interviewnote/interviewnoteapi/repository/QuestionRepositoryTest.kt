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

        // 테스트 데이터 생성 (IT, SALES, MARKETING 직무)
        questionRepository.saveAll(
            listOf(
                // IT 직무
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
                ),
                // SALES 직무
                Question(
                    jobField = "SALES",
                    targetJob = "영업관리자",
                    category = "고객관리",
                    content = "어려운 고객을 상대한 경험을 설명해주세요",
                    difficulty = "MEDIUM",
                    isActive = true,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                ),
                Question(
                    jobField = "SALES",
                    targetJob = "영업관리자",
                    category = "실적달성",
                    content = "목표를 초과 달성한 경험을 설명해주세요",
                    difficulty = "HARD",
                    isActive = true,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                ),
                Question(
                    jobField = "SALES",
                    targetJob = "영업관리자",
                    category = "협상스킬",
                    content = "가격 협상 경험을 설명해주세요",
                    difficulty = "EASY",
                    isActive = true,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                ),
                // MARKETING 직무
                Question(
                    jobField = "MARKETING",
                    targetJob = "마케팅매니저",
                    category = "캠페인기획",
                    content = "성공적인 마케팅 캠페인을 기획한 경험을 설명해주세요",
                    difficulty = "MEDIUM",
                    isActive = true,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                ),
                Question(
                    jobField = "MARKETING",
                    targetJob = "마케팅매니저",
                    category = "데이터분석",
                    content = "데이터 기반으로 의사결정한 경험을 설명해주세요",
                    difficulty = "HARD",
                    isActive = true,
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
        assertThat(questions).hasSize(8) // IT 3개 + SALES 3개 + MARKETING 2개
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
        assertThat(questions).hasSize(2) // IT 1개 + SALES 1개
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

    // Phase 5: jobField 필터링 테스트

    @Test
    fun `jobField로 활성화된 질문을 조회한다 - IT`() {
        // when
        val questions = questionRepository.findByJobFieldAndIsActiveTrue("IT")

        // then
        assertThat(questions).hasSize(3) // 비활성화 1개 제외
        assertThat(questions).allMatch { it.jobField == "IT" && it.isActive }
    }

    @Test
    fun `jobField로 활성화된 질문을 조회한다 - SALES`() {
        // when
        val questions = questionRepository.findByJobFieldAndIsActiveTrue("SALES")

        // then
        assertThat(questions).hasSize(3)
        assertThat(questions).allMatch { it.jobField == "SALES" && it.isActive }
    }

    @Test
    fun `jobField로 활성화된 질문을 조회한다 - MARKETING`() {
        // when
        val questions = questionRepository.findByJobFieldAndIsActiveTrue("MARKETING")

        // then
        assertThat(questions).hasSize(2)
        assertThat(questions).allMatch { it.jobField == "MARKETING" && it.isActive }
    }

    @Test
    fun `존재하지 않는 jobField는 빈 리스트를 반환한다`() {
        // when
        val questions = questionRepository.findByJobFieldAndIsActiveTrue("NONEXISTENT")

        // then
        assertThat(questions).isEmpty()
    }

    @Test
    fun `jobField와 category로 필터링한다 - IT + 기술역량`() {
        // when
        val questions = questionRepository.findByJobFieldAndCategoryAndIsActiveTrue("IT", "기술역량")

        // then
        assertThat(questions).hasSize(2)
        assertThat(questions).allMatch { it.jobField == "IT" && it.category == "기술역량" && it.isActive }
    }

    @Test
    fun `jobField와 category로 필터링한다 - SALES + 고객관리`() {
        // when
        val questions = questionRepository.findByJobFieldAndCategoryAndIsActiveTrue("SALES", "고객관리")

        // then
        assertThat(questions).hasSize(1)
        assertThat(questions.first().jobField).isEqualTo("SALES")
        assertThat(questions.first().category).isEqualTo("고객관리")
        assertThat(questions.first().isActive).isTrue()
    }

    @Test
    fun `jobField와 difficulty로 필터링한다 - IT + EASY`() {
        // when
        val questions = questionRepository.findByJobFieldAndDifficultyAndIsActiveTrue("IT", "EASY")

        // then
        assertThat(questions).hasSize(1)
        assertThat(questions).allMatch { it.jobField == "IT" && it.difficulty == "EASY" && it.isActive }
    }

    @Test
    fun `jobField와 difficulty로 필터링한다 - SALES + MEDIUM`() {
        // when
        val questions = questionRepository.findByJobFieldAndDifficultyAndIsActiveTrue("SALES", "MEDIUM")

        // then
        assertThat(questions).hasSize(1)
        assertThat(questions.first().jobField).isEqualTo("SALES")
        assertThat(questions.first().difficulty).isEqualTo("MEDIUM")
    }

    @Test
    fun `jobField, category, difficulty로 복합 필터링한다 - IT + 기술역량 + MEDIUM`() {
        // when
        val questions = questionRepository.findByJobFieldAndCategoryAndDifficultyAndIsActiveTrue(
            "IT",
            "기술역량",
            "MEDIUM"
        )

        // then
        assertThat(questions).hasSize(1)
        assertThat(questions.first().jobField).isEqualTo("IT")
        assertThat(questions.first().category).isEqualTo("기술역량")
        assertThat(questions.first().difficulty).isEqualTo("MEDIUM")
        assertThat(questions.first().isActive).isTrue()
    }

    @Test
    fun `jobField, category, difficulty로 복합 필터링한다 - SALES + 실적달성 + HARD`() {
        // when
        val questions = questionRepository.findByJobFieldAndCategoryAndDifficultyAndIsActiveTrue(
            "SALES",
            "실적달성",
            "HARD"
        )

        // then
        assertThat(questions).hasSize(1)
        assertThat(questions.first().jobField).isEqualTo("SALES")
        assertThat(questions.first().category).isEqualTo("실적달성")
        assertThat(questions.first().difficulty).isEqualTo("HARD")
    }

    @Test
    fun `jobField 필터링 시 비활성화된 질문은 제외된다`() {
        // when - IT에는 비활성화된 질문 1개 존재
        val questions = questionRepository.findByJobFieldAndIsActiveTrue("IT")

        // then
        assertThat(questions).hasSize(3) // 전체 4개 중 활성화 3개만
        assertThat(questions).allMatch { it.isActive }
        assertThat(questions).noneMatch { it.content.contains("팀 프로젝트") }
    }
}
