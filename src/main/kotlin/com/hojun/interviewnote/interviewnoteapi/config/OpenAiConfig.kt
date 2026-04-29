package com.hojun.interviewnote.interviewnoteapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * OpenAI API 설정 프로퍼티
 *
 * OpenAI Java Spring Boot Starter가 자동으로 OpenAIClient Bean을 생성합니다.
 * 이 클래스는 추가적인 설정 값들을 관리합니다.
 */
@Configuration
@ConfigurationProperties(prefix = "openai")
class OpenAiProperties {
    lateinit var apiKey: String
    var model: String = "gpt-4o-mini"
    var promptVersion: String = "v1.0"
    var maxTokens: Int = 3000  // 800 → 3000 (질문 생성 시 JSON 잘림 방지)
    var temperature: Double = 0.7
    var timeout: Long = 30000  // 30초
}
