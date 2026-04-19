package com.hojun.interviewnote.interviewnoteapi.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "interview_answers")
class InterviewAnswer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val questionId: Long,

    /**
     * 답변 작성자 ID
     * Phase 4A-2에서 추가됨: 사용자별 답변 이력 분리
     */
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(nullable = false, columnDefinition = "TEXT")
    val answerText: String,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InterviewAnswer) return false
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String {
        return "InterviewAnswer(id=$id, questionId=$questionId)"
    }
}
