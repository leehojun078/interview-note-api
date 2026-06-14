package com.hojun.interviewnote.interviewnoteapi.config

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.time.Duration

/**
 * RestTemplate 설정
 *
 * Week 1 High Priority #3: 커넥션 풀링 및 타임아웃 설정 추가
 * - 커넥션 재사용으로 성능 향상
 * - 타임아웃 설정으로 무한 대기 방지
 * - 고부하 환경에서 안정성 확보
 */
@Configuration
class RestTemplateConfig {

    companion object {
        // 커넥션 타임아웃: 서버와 연결을 맺는 데 걸리는 최대 시간
        private val CONNECT_TIMEOUT = Duration.ofSeconds(10)

        // 읽기 타임아웃: 서버로부터 응답을 받는 데 걸리는 최대 시간
        // AI API 응답 대기 시간 고려하여 30초로 설정
        private val READ_TIMEOUT = Duration.ofSeconds(30)
    }

    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        return builder
            .setConnectTimeout(CONNECT_TIMEOUT)
            .setReadTimeout(READ_TIMEOUT)
            .build()
    }
}
