package com.hojun.interviewnote.interviewnoteapi.domain

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 면접 채팅 메시지
 *
 * Phase 7: 실시간 대화 기록
 * - AI 메시지: 질문, aiReasoning
 * - USER 메시지: 답변, 평가 점수
 */
@Entity
@Table(name = "interview_messages")
class InterviewMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val mockInterviewId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val sender: MessageSender,

    /**
     * 메시지 내용
     * - AI: 질문 (200자 이내)
     * - USER: 답변 (200자 이내)
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    /**
     * 메시지 순서 (0부터 시작)
     * - 대화 흐름 추적용
     * - 첫 질문: 0, 첫 답변: 1, 두 번째 질문: 2, ...
     */
    @Column(nullable = false)
    val messageIndex: Int,

    // === AI 메시지 전용 필드 ===

    /**
     * AI 질문 생성 근거
     * 예: "구체적인 기술 경험을 확인하기 위한 꼬리 질문"
     */
    @Column(columnDefinition = "TEXT")
    val aiReasoning: String? = null,

    // === USER 메시지 전용 필드 (평가) ===

    /**
     * 논리성 점수 (1-5)
     * AI가 사용자 답변을 평가한 결과
     */
    var logicScore: Int? = null,

    /**
     * 구체성 점수 (1-5)
     */
    var specificityScore: Int? = null,

    /**
     * 전달력 점수 (1-5)
     */
    var deliveryScore: Int? = null,

    /**
     * 평가 코멘트
     * 예: "답변이 간결하나, 구체적인 기술 스택 언급이 부족합니다."
     */
    @Column(columnDefinition = "TEXT")
    var feedbackComment: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * 사용자 답변에 대한 평가 업데이트
     * AI 응답 수신 후 호출
     */
    fun updateEvaluation(
        logicScore: Int,
        specificityScore: Int,
        deliveryScore: Int,
        comment: String
    ) {
        require(sender == MessageSender.USER) {
            "평가는 USER 메시지에만 가능합니다"
        }
        this.logicScore = logicScore
        this.specificityScore = specificityScore
        this.deliveryScore = deliveryScore
        this.feedbackComment = comment
    }

    /**
     * 평균 점수 계산
     */
    val averageScore: Double?
        get() = if (logicScore != null && specificityScore != null && deliveryScore != null) {
            (logicScore!! + specificityScore!! + deliveryScore!!) / 3.0
        } else {
            null
        }

    /**
     * JPA 최적화: equals/hashCode
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InterviewMessage) return false
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String {
        return "InterviewMessage(id=$id, mockInterviewId=$mockInterviewId, " +
                "sender=$sender, messageIndex=$messageIndex)"
    }
}

/**
 * 메시지 발신자
 */
enum class MessageSender {
    /** AI 면접관 */
    AI,

    /** 사용자 (지원자) */
    USER
}
