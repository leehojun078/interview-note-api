package com.hojun.interviewnote.interviewnoteapi.domain

/**
 * 직무 분야 Enum
 *
 * Phase 5에서 추가됨
 * - IT 단일 직무에서 17개 직무로 확장
 * - 각 직무별 면접 질문과 AI 프롬프트 제공
 */
enum class JobField(
    val displayName: String,
    val code: String
) {
    PLANNING("기획·전략", "PLANNING"),
    MARKETING("마케팅·홍보·조사", "MARKETING"),
    ACCOUNTING("회계·세무·재무", "ACCOUNTING"),
    HR("인사·노무·HRD", "HR"),
    ADMIN("총무·법무·사무", "ADMIN"),
    IT("IT개발", "IT"),
    DESIGN("디자인", "DESIGN"),
    SALES("영업·판매·무역", "SALES"),
    MD("상품기획·MD", "MD"),
    SERVICE("서비스", "SERVICE"),
    PRODUCTION("생산", "PRODUCTION"),
    CONSTRUCTION("건설·건축", "CONSTRUCTION"),
    MEDICAL("의료", "MEDICAL"),
    EDUCATION("교육", "EDUCATION"),
    MEDIA("미디어·문화·스포츠", "MEDIA"),
    FINANCE("금융·보험", "FINANCE"),
    PUBLIC("공공·복지", "PUBLIC");

    companion object {
        /**
         * code로 JobField 찾기
         */
        fun fromCode(code: String): JobField? {
            return values().find { it.code == code }
        }

        /**
         * displayName으로 JobField 찾기
         */
        fun fromDisplayName(displayName: String): JobField? {
            return values().find { it.displayName == displayName }
        }
    }
}
