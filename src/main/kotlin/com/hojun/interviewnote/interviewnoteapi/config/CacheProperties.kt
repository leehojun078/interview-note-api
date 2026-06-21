package com.hojun.interviewnote.interviewnoteapi.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 캐시 설정
 *
 * 외부 설정 파일에서 캐시 관련 값을 주입받습니다.
 */
@ConfigurationProperties(prefix = "cache")
data class CacheProperties(
    /** 중복 요청 캐시 유지 시간 (시간) */
    val duplicateRequestHours: Long = 24,

    /** 질문 캐시 유지 기간 (일) */
    val questionDays: Long = 7,

    /** 채용 공고 캐시 유지 기간 (일) */
    val jobPostingDays: Long = 7
)
