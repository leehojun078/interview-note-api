package com.hojun.interviewnote.interviewnoteapi.repository

import com.hojun.interviewnote.interviewnoteapi.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * User Repository
 *
 * Phase 4A에서 추가됨
 */
@Repository
interface UserRepository : JpaRepository<User, Long> {
    /**
     * 이메일로 사용자 조회
     * - 로그인 시 사용
     * - 회원가입 시 중복 체크용
     */
    fun findByEmail(email: String): User?

    /**
     * 이메일 존재 여부 확인
     * - 회원가입 시 중복 체크용
     */
    fun existsByEmail(email: String): Boolean

    /**
     * 활성화된 사용자만 이메일로 조회
     * - 로그인 시 사용
     */
    fun findByEmailAndIsActiveTrue(email: String): User?
}
