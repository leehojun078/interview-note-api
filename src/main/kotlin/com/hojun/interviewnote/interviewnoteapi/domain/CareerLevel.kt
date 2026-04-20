package com.hojun.interviewnote.interviewnoteapi.domain

/**
 * 경력 수준 Enum
 *
 * Phase 5에서 추가됨
 * - 사용자의 경력 단계를 구분
 * - 향후 경력별 맞춤 질문 제공 가능
 */
enum class CareerLevel(
    val displayName: String,
    val code: String
) {
    ENTRY("신입", "ENTRY"),
    JUNIOR("주니어(1-3년)", "JUNIOR"),
    SENIOR("시니어(3-7년)", "SENIOR"),
    SENIOR_PLUS("시니어+(7년 이상)", "SENIOR_PLUS");

    companion object {
        /**
         * code로 CareerLevel 찾기
         */
        fun fromCode(code: String): CareerLevel? {
            return values().find { it.code == code }
        }

        /**
         * displayName으로 CareerLevel 찾기
         */
        fun fromDisplayName(displayName: String): CareerLevel? {
            return values().find { it.displayName == displayName }
        }
    }
}
