package com.hojun.interviewnote.interviewnoteapi.domain

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 채용 공고 엔티티
 *
 * Phase 6A에서 추가됨
 * - 사용자가 입력한 채용 공고 정보
 * - AI가 이 공고를 기반으로 맞춤형 질문 10개 생성
 * - 직무는 사용자 선택(우선) 또는 AI 추론(대체)
 */
@Entity
@Table(name = "job_postings")
class JobPosting(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 공고 등록자 (FK: users.id)
     */
    @Column(nullable = false)
    val userId: Long,

    /**
     * 원본 채용 공고 URL
     * - 최대 2000자 (TEXT 타입)
     * - 중복 캐싱에 사용 (7일간)
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    val originalUrl: String,

    /**
     * 회사명
     * - 최대 200자
     */
    @Column(nullable = false, length = 200)
    val companyName: String,

    /**
     * 포지션명 (직무명)
     * - 최대 200자
     * - 예: "백엔드 개발자", "영업관리자"
     */
    @Column(nullable = false, length = 200)
    val jobTitle: String,

    /**
     * 공고 전문 (직무 설명, 자격 요건 등)
     * - TEXT 타입 (최대 6,000자, AI 토큰 제한 고려)
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    val jobDescription: String,

    /**
     * 사용자 선택 직무 (우선순위 높음)
     * - 사용자가 공고 등록 시 직접 선택한 직무
     * - nullable: 미선택 시 null
     */
    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    var selectedJobField: JobField? = null,

    /**
     * AI 추론 직무 (사용자 미선택 시 사용)
     * - AI가 공고 내용을 분석하여 추론한 직무
     * - nullable: AI 추론 실패 시 null
     */
    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    var inferredJobField: JobField? = null,

    /**
     * 필수 기술 스택
     * - JSON array 형태 문자열
     * - 예: ["Kotlin", "Spring Boot", "MySQL", "Docker"]
     * - nullable: 파싱 실패 시 null
     */
    @Column(columnDefinition = "TEXT")
    val requiredSkills: String? = null,

    /**
     * 우대 기술 스택
     * - JSON array 형태 문자열
     * - 예: ["Kubernetes", "Redis", "Kafka"]
     * - nullable: 파싱 실패 시 null
     */
    @Column(columnDefinition = "TEXT")
    val preferredSkills: String? = null,

    /**
     * 활성 상태
     * - false: 사용자가 삭제한 공고 (soft delete)
     */
    @Column(nullable = false)
    var isActive: Boolean = true,

    /**
     * 공고 등록 일시
     */
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * 유효한 직무 필드 반환
     * - selectedJobField가 있으면 우선 반환
     * - 없으면 inferredJobField 반환
     * - 둘 다 없으면 null (에러 상황)
     */
    val effectiveJobField: JobField?
        get() = selectedJobField ?: inferredJobField

    /**
     * 공고 비활성화 (soft delete)
     */
    fun deactivate() {
        this.isActive = false
    }

    /**
     * AI 추론 직무 업데이트
     * - AI가 공고 분석 후 직무를 추론한 경우 호출
     */
    fun updateInferredJobField(jobField: JobField) {
        this.inferredJobField = jobField
    }

    /**
     * JPA 최적화를 위한 equals/hashCode 구현
     * - id 기반 동등성 비교
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JobPosting) return false
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun toString(): String {
        return "JobPosting(id=$id, companyName='$companyName', jobTitle='$jobTitle', " +
                "effectiveJobField=$effectiveJobField, isActive=$isActive)"
    }
}
