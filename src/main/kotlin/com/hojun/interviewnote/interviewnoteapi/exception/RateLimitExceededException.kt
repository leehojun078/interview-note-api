package com.hojun.interviewnote.interviewnoteapi.exception

import java.time.LocalDateTime

/**
 * Rate Limit 초과 예외
 */
class RateLimitExceededException(
    val ip: String,
    val limit: Int,
    val resetTime: LocalDateTime
) : RuntimeException("IP ${ip}의 요청 한도(${limit}회/시간)를 초과했습니다. 재설정 시간: ${resetTime}")
