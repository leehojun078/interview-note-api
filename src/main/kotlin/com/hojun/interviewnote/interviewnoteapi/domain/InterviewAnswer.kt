package com.hojun.interviewnote.interviewnoteapi.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "interview_answers")
class InterviewAnswer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 정적 질문 ID (Phase 6E에서 nullable로 변경)
     * - null: AI 생성 질문에 대한 답변 (generatedQuestionId 사용)
     * - not null: 정적 질문에 대한 답변
     */
    @Column(name = "question_id", nullable = true)
    val questionId: Long? = null,

    /**
     * 답변 작성자 ID
     * Phase 4A-2에서 추가됨: 사용자별 답변 이력 분리
     */
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(nullable = false, columnDefinition = "TEXT")
    val answerText: String,

    /**
     * AI 생성 질문 ID (Phase 6C에서 추가)
     * - nullable: 정적 질문 답변 시 null
     * - questionId와 generatedQuestionId 중 하나만 사용
     */
    @Column(name = "generated_question_id")
    val generatedQuestionId: Long? = null,

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
