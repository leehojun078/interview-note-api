package com.hojun.interviewnote.interviewnoteapi.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Rate Limit 설정
 *
 * 외부 설정 파일에서 Rate Limit 관련 값을 주입받습니다.
 */
@ConfigurationProperties(prefix = "rate-limit")
data class RateLimitProperties(
    /** IP당 시간당 최대 요청 수 */
    val maxRequestsPerHour: Int = 33,

    /** Rate Limit 윈도우 (분) */
    val windowDurationMinutes: Long = 60,

    /** 사용자당 일일 질문 생성 한도 */
    val maxQuestionGenerationsPerDay: Int = 10,

    /** 질문 생성 윈도우 (시간) */
    val questionGenerationWindowHours: Long = 24,

    /** 사용자당 일일 모의 면접 한도 */
    val maxMockInterviewsPerDay: Int = 5,

    /** 모의 면접 윈도우 (시간) */
    val mockInterviewWindowHours: Long = 24
)
