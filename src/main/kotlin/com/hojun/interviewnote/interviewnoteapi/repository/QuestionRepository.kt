package com.hojun.interviewnote.interviewnoteapi.repository

import com.hojun.interviewnote.interviewnoteapi.domain.Question
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface QuestionRepository : JpaRepository<Question, Long> {
    // 기존 필터링 메서드 (jobField 없이)
    fun findByIsActiveTrue(): List<Question>
    fun findByCategoryAndIsActiveTrue(category: String): List<Question>
    fun findByDifficultyAndIsActiveTrue(difficulty: String): List<Question>
    fun findByCategoryAndDifficultyAndIsActiveTrue(category: String, difficulty: String): List<Question>

    // Phase 5: jobField 필터링 메서드
    fun findByJobFieldAndIsActiveTrue(jobField: String): List<Question>
    fun findByJobFieldAndCategoryAndIsActiveTrue(jobField: String, category: String): List<Question>
    fun findByJobFieldAndDifficultyAndIsActiveTrue(jobField: String, difficulty: String): List<Question>
    fun findByJobFieldAndCategoryAndDifficultyAndIsActiveTrue(
        jobField: String,
        category: String,
        difficulty: String
    ): List<Question>

    /**
     * 특정 직무의 고유 카테고리 목록 조회 (중복 제거, 정렬)
     *
     * Phase 5: 직무별 동적 카테고리 필터링
     * - 활성화된 질문만 조회
     * - 중복 제거 (DISTINCT)
     * - 알파벳 순 정렬
     *
     * @param jobField 직무 분야 (예: "IT", "SALES")
     * @return 해당 직무의 고유 카테고리 목록
     */
    @Query("""
        SELECT DISTINCT q.category
        FROM Question q
        WHERE q.jobField = :jobField AND q.isActive = true
        ORDER BY q.category
    """)
    fun findDistinctCategoriesByJobField(@Param("jobField") jobField: String): List<String>
}
