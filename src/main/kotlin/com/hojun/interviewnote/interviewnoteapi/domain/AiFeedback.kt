package com.hojun.interviewnote.interviewnoteapi.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "ai_feedbacks")
class AiFeedback(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val interviewAnswerId: Long,

    // 평가 점수 (1-5) - 모든 직무에 공통 적용 가능한 기준
    @Column(nullable = false)
    val logicScore: Int,

    @Column(nullable = false)
    val specificityScore: Int,

    @Column(nullable = false)
    val jobFitScore: Int,

    @Column(nullable = false)
    val deliveryScore: Int,

    // 피드백 내용
    @Column(nullable = false, columnDefinition = "TEXT")
    val strengths: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val improvements: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val modelAnswer: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val overallComment: String,

    // 메타데이터
    @Column(nullable = false, length = 50)
    val jobField: String = "IT",

    @Column(nullable = false, length = 50)
    val modelName: String,

    @Column(nullable = false, length = 20)
    val promptVersion: String,

    @Column(nullable = false)
    val tokenUsageInput: Int,

    @Column(nullable = false)
    val tokenUsageOutput: Int,

    @Column(nullable = false, columnDefinition = "TEXT")
    val rawResponse: String,

    // Phase 2D: 중복 요청 방지를 위한 해시 (questionId + answerText의 SHA-256)
    @Column(name = "answer_text_hash", length = 64)
    val answerTextHash: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * 4가지 평가 점수의 평균
     */
    val averageScore: Double
        get() = (logicScore + specificityScore + jobFitScore + deliveryScore) / 4.0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AiFeedback) return false
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String {
        return "AiFeedback(id=$id, interviewAnswerId=$interviewAnswerId, averageScore=$averageScore)"
    }
}
