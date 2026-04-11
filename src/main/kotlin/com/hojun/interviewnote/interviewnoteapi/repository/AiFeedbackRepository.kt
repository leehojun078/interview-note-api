package com.hojun.interviewnote.interviewnoteapi.repository

import com.hojun.interviewnote.interviewnoteapi.domain.AiFeedback
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AiFeedbackRepository : JpaRepository<AiFeedback, Long> {
    fun findByInterviewAnswerId(interviewAnswerId: Long): AiFeedback?
}
