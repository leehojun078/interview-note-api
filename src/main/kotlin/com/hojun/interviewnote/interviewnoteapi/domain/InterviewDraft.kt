package com.hojun.interviewnote.interviewnoteapi.domain

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Phase 3: 답변 자동 저장 (Draft)
 *
 * 사용자가 답변 작성 중 임시 저장한 내용을 보관
 * - 브라우저 종료나 새로고침으로 인한 데이터 손실 방지
 * - 2초마다 자동 저장 (HTMX keyup changed delay:2s)
 * - 제출 시 Draft 삭제
 */
@Entity
@Table(
    name = "interview_drafts",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "question_id"])
    ]
)
class InterviewDraft(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, name = "user_id")
    val userId: Long,

    @Column(nullable = false, name = "question_id")
    val questionId: Long,

    @Column(columnDefinition = "TEXT", nullable = false, name = "draft_text")
    var draftText: String,

    @Column(nullable = false, name = "last_saved")
    var lastSaved: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false, name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Draft 텍스트 업데이트
     */
    fun updateDraft(newText: String) {
        this.draftText = newText
        this.lastSaved = LocalDateTime.now()
    }
}
