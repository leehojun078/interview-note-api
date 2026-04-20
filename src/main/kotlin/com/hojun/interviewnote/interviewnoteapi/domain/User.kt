package com.hojun.interviewnote.interviewnoteapi.domain

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 사용자 엔티티
 *
 * Phase 4A에서 추가됨
 * - 이메일 기반 회원가입/로그인
 * - BCrypt 비밀번호 암호화
 * - 역할 기반 접근 제어 (USER, ADMIN)
 */
@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 이메일 (로그인 ID)
     * - unique constraint
     * - 회원가입 시 중복 체크 필수
     */
    @Column(unique = true, nullable = false, length = 255)
    val email: String,

    /**
     * BCrypt 해시된 비밀번호
     * - 평문 비밀번호는 저장하지 않음
     * - BCryptPasswordEncoder 사용
     */
    @Column(nullable = false, length = 255)
    var passwordHash: String,

    /**
     * 사용자 이름 (닉네임)
     */
    @Column(nullable = false, length = 100)
    var name: String,

    /**
     * 사용자 역할
     * - USER: 일반 사용자
     * - ADMIN: 관리자
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val role: UserRole = UserRole.USER,

    /**
     * 계정 활성 상태
     * - false: 비활성화된 계정 (로그인 불가)
     * - Phase 5+에서 관리자가 사용자 계정 비활성화 가능
     */
    @Column(nullable = false)
    var isActive: Boolean = true,

    /**
     * 계정 생성 일시
     */
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /**
     * 마지막 로그인 일시
     * - 로그인 성공 시마다 갱신
     */
    var lastLoginAt: LocalDateTime? = null,

    /**
     * 직무 분야 (Phase 5에서 추가)
     * - nullable: 미설정 시 IT 기본값 사용
     * - 사용자가 프로필에서 선택 가능
     */
    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    var jobField: JobField? = null,

    /**
     * 경력 수준 (Phase 5에서 추가)
     * - nullable: 미설정 가능
     * - 사용자가 프로필에서 선택 가능
     */
    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    var careerLevel: CareerLevel? = null
) {
    /**
     * JPA 최적화를 위한 equals/hashCode 구현
     * - id 기반 동등성 비교
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    /**
     * 비밀번호 변경
     */
    fun changePassword(newPasswordHash: String) {
        this.passwordHash = newPasswordHash
    }

    /**
     * 이름 변경
     */
    fun changeName(newName: String) {
        this.name = newName
    }

    /**
     * 마지막 로그인 일시 갱신
     */
    fun updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now()
    }

    /**
     * 계정 비활성화
     */
    fun deactivate() {
        this.isActive = false
    }

    /**
     * 계정 활성화
     */
    fun activate() {
        this.isActive = true
    }

    /**
     * 직무 선호도 업데이트 (Phase 5에서 추가)
     * - 사용자 프로필 설정에서 호출
     */
    fun updateJobPreferences(jobField: JobField?, careerLevel: CareerLevel?) {
        this.jobField = jobField
        this.careerLevel = careerLevel
    }
}
