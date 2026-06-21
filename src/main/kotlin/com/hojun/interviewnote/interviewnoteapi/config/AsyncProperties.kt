package com.hojun.interviewnote.interviewnoteapi.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 비동기 처리 스레드 풀 설정
 *
 * 외부 설정 파일에서 스레드 풀 관련 값을 주입받습니다.
 */
@ConfigurationProperties(prefix = "async")
data class AsyncProperties(
    /** 기본 유지 스레드 수 */
    val corePoolSize: Int = 10,

    /** 최대 동시 스레드 수 */
    val maxPoolSize: Int = 50,

    /** 대기 큐 용량 */
    val queueCapacity: Int = 100,

    /** 셧다운 시 작업 완료 대기 시간 (초) */
    val awaitTerminationSeconds: Int = 30
)
