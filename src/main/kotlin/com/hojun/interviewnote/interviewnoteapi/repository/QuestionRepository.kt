package com.hojun.interviewnote.interviewnoteapi.repository

import com.hojun.interviewnote.interviewnoteapi.domain.Question
import org.springframework.data.jpa.repository.JpaRepository
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
}
