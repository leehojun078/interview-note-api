package com.hojun.interviewnote.interviewnoteapi.domain

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 모의 면접 세션
 *
 * Phase 7: 실시간 AI 채팅 면접 시스템의 핵심 엔티티
 * - 직무 기반 또는 채용 공고 기반 면접 지원
 * - SSE 실시간 통신으로 대화 진행
 */
@Entity
@Table(name = "mock_interviews")
class MockInterview(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val userId: Long,

    /**
     * 채용 공고 ID (nullable)
     * - null: 직무 기반 일반 면접
     * - not null: 특정 공고 기반 맞춤 면접
     */
    val jobPostingId: Long? = null,

    /**
     * 선택 직무 (필수)
     * - 공고 기반: jobPosting.effectiveJobField 사용
     * - 직무 기반: 사용자 직접 선택
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val selectedJobField: JobField,

    /**
     * 경력 수준 (Phase 8A)
     * - null: 기본 난이도
     * - ENTRY, JUNIOR, SENIOR, SENIOR_PLUS
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var careerLevel: CareerLevel? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: MockInterviewStatus = MockInterviewStatus.IN_PROGRESS,

    @Column(nullable = false)
    val startedAt: LocalDateTime = LocalDateTime.now(),

    var endedAt: LocalDateTime? = null,

    // === 종합 평가 (종료 시 생성) ===

    @Column(columnDefinition = "TEXT")
    var overallFeedback: String? = null,

    /**
     * 주요 강점 (JSON array string)
     * 예: ["Kotlin 숙련도", "명확한 커뮤니케이션"]
     */
    @Column(columnDefinition = "TEXT")
    var keyStrengths: String? = null,

    /**
     * 개선점 (JSON array string)
     * 예: ["분산 시스템 학습", "대규모 트래픽 처리 경험"]
     */
    @Column(columnDefinition = "TEXT")
    var keyImprovements: String? = null,

    var averageScore: Double? = null,

    /**
     * 가중 평균 점수 (Phase 8A)
     * - 첫 답변(자기소개) 제외한 평균
     * - 저품질 답변 50% 이상 시 0.8 패널티
     */
    var weightedAverageScore: Double? = null,

    /**
     * 점수 계산 방법 (Phase 8A)
     * - SIMPLE_AVERAGE: Phase 7 방식 (모든 답변 평균)
     * - WEIGHTED_AVERAGE: Phase 8 방식 (첫 답변 제외 + 패널티)
     */
    @Column(length = 50)
    var scoreCalculationMethod: String = "WEIGHTED_AVERAGE",

    /**
     * 채용 추천도
     * 예: "추천 - 기술 역량 우수", "보류 - Kafka 경험 부족"
     */
    @Column(columnDefinition = "TEXT")
    var recommendation: String? = null
) {
    /**
     * 면접 종료 및 종합 평가 저장 (Phase 8A: weightedAverageScore 추가)
     */
    fun complete(
        overallFeedback: String,
        keyStrengths: String,
        keyImprovements: String,
        averageScore: Double,
        weightedAverageScore: Double,
        recommendation: String
    ) {
        require(status == MockInterviewStatus.IN_PROGRESS) {
            "진행 중인 면접만 완료할 수 있습니다: status=$status"
        }

        this.status = MockInterviewStatus.COMPLETED
        this.endedAt = LocalDateTime.now()
        this.overallFeedback = overallFeedback
        this.keyStrengths = keyStrengths
        this.keyImprovements = keyImprovements
        this.averageScore = averageScore
        this.weightedAverageScore = weightedAverageScore
        this.recommendation = recommendation
    }

    /**
     * 면접 재개 (Phase 8C)
     * COMPLETED 상태의 면접을 다시 IN_PROGRESS로 전환
     */
    fun resume() {
        require(status == MockInterviewStatus.COMPLETED) {
            "완료된 면접만 재개할 수 있습니다: status=$status"
        }
        this.status = MockInterviewStatus.IN_PROGRESS
        this.endedAt = null
    }

    /**
     * 면접 중단 (사용자 취소)
     */
    fun abort() {
        require(status == MockInterviewStatus.IN_PROGRESS) {
            "진행 중인 면접만 중단할 수 있습니다: status=$status"
        }
        this.status = MockInterviewStatus.ABORTED
        this.endedAt = LocalDateTime.now()
    }

    /**
     * JPA 최적화: equals/hashCode
     * data class 사용 시 문제 발생 방지
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MockInterview) return false
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String {
        return "MockInterview(id=$id, userId=$userId, jobPostingId=$jobPostingId, " +
                "selectedJobField=$selectedJobField, status=$status)"
    }
}

/**
 * 모의 면접 상태
 */
enum class MockInterviewStatus {
    /** 진행 중 */
    IN_PROGRESS,

    /** 정상 완료 (종합 평가 완료) */
    COMPLETED,

    /** 중단 (사용자 취소) */
    ABORTED
}
