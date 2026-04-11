package com.hojun.interviewnote.interviewnoteapi.repository

import com.hojun.interviewnote.interviewnoteapi.domain.Question
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface QuestionRepository : JpaRepository<Question, Long> {
    fun findByIsActiveTrue(): List<Question>
    fun findByCategoryAndIsActiveTrue(category: String): List<Question>
    fun findByDifficultyAndIsActiveTrue(difficulty: String): List<Question>
    fun findByCategoryAndDifficultyAndIsActiveTrue(category: String, difficulty: String): List<Question>
}
