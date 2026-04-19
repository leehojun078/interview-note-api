package com.hojun.interviewnote.interviewnoteapi.domain

/**
 * 사용자 역할
 *
 * Phase 4A에서 추가됨
 */
enum class UserRole {
    /**
     * 일반 사용자
     * - 자신의 답변만 조회/생성 가능
     * - 모든 질문 조회 가능
     */
    USER,

    /**
     * 관리자
     * - 모든 사용자 데이터 조회 가능
     * - 질문 관리 가능 (Phase 5+)
     * - 사용자 관리 가능 (Phase 5+)
     */
    ADMIN
}
