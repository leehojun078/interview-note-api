package com.hojun.interviewnote.interviewnoteapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

/**
 * Phase 7C: 비동기 처리 설정
 *
 * AI 응답 생성을 비동기로 처리하여 사용자 대기 시간 단축
 * SSE를 통해 생성 완료 시 클라이언트에게 전송
 */
@Configuration
@EnableAsync
class AsyncConfig {

    /**
     * 비동기 AI 응답 생성용 ThreadPool
     *
     * - corePoolSize: 10 (기본 유지 스레드)
     * - maxPoolSize: 50 (최대 동시 AI 응답 생성)
     * - queueCapacity: 100 (대기 큐)
     *
     * 동시 면접 세션이 많을 때 스레드 풀로 AI 호출을 비동기 처리
     */
    @Bean(name = ["taskExecutor"])
    fun taskExecutor(): Executor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 10
            maxPoolSize = 50
            queueCapacity = 100
            setThreadNamePrefix("async-interview-")
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(30)
            initialize()
        }
    }
}
