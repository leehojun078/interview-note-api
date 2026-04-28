package com.hojun.interviewnote.interviewnoteapi.domain

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * AI 생성 질문 엔티티
 *
 * Phase 6A에서 추가됨
 * - 채용 공고 기반으로 AI가 생성한 맞춤형 면접 질문
 * - 공고당 10개 질문 생성 (orderIndex 1-10)
 * - 기존 Question 엔티티와 분리하여 출처 구분
 */
@Entity
@Table(name = "generated_questions")
class GeneratedQuestion(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 소속 채용 공고 (FK: job_postings.id)
     * - ON DELETE CASCADE: 공고 삭제 시 질문도 함께 삭제
     */
    @Column(nullable = false)
    val jobPostingId: Long,

    /**
     * 질문 내용
     * - TEXT 타입 (최대 2000자)
     * - 예: "Kotlin의 코루틴을 실무에서 어떻게 활용했나요?"
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    /**
     * 카테고리
     * - 직무별로 동적으로 결정됨 (PromptBuilder 참조)
     * - IT: 기술역량, 문제해결, 협업경험
     * - SALES: 고객관리, 실적달성, 협상스킬
     * - 최대 100자
     */
    @Column(nullable = false, length = 100)
    val category: String,

    /**
     * 난이도
     * - EASY: 기본 개념, 경험 유무 (3문항)
     * - MEDIUM: 심화 기술, 프로젝트 경험 (4문항)
     * - HARD: 트레이드오프, 설계 결정 (3문항)
     * - CHECK 제약: DB 레벨에서 EASY/MEDIUM/HARD만 허용
     */
    @Column(nullable = false, length = 20)
    val difficulty: String,

    /**
     * AI 생성 근거
     * - AI가 이 질문을 생성한 이유
     * - 예: "필수 기술인 Kotlin의 핵심 기능 이해도를 확인하기 위한 질문"
     * - TEXT 타입 (최대 500자)
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    val aiReasoning: String,

    /**
     * 질문 순서
     * - 1-10 사이의 정수
     * - CHECK 제약: DB 레벨에서 1-10 범위 강제
     * - orderIndex가 낮을수록 먼저 표시
     */
    @Column(nullable = false)
    val orderIndex: Int,

    /**
     * 질문 생성 일시
     */
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    init {
        // Application 레벨 검증 (DB CHECK 제약 전 1차 방어)
        require(orderIndex in 1..10) {
            "orderIndex must be between 1 and 10, but was $orderIndex"
        }
        require(difficulty in setOf("EASY", "MEDIUM", "HARD")) {
            "difficulty must be EASY, MEDIUM, or HARD, but was $difficulty"
        }
    }

    /**
     * JPA 최적화를 위한 equals/hashCode 구현
     * - id 기반 동등성 비교
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GeneratedQuestion) return false
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun toString(): String {
        return "GeneratedQuestion(id=$id, jobPostingId=$jobPostingId, " +
                "category='$category', difficulty='$difficulty', orderIndex=$orderIndex)"
    }
}
