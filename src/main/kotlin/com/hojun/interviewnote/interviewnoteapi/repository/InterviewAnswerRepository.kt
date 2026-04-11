package com.hojun.interviewnote.interviewnoteapi.repository

import com.hojun.interviewnote.interviewnoteapi.domain.InterviewAnswer
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InterviewAnswerRepository : JpaRepository<InterviewAnswer, Long> {
    fun findByQuestionId(questionId: Long): List<InterviewAnswer>
    fun findAllByOrderByCreatedAtDesc(): List<InterviewAnswer>
}
