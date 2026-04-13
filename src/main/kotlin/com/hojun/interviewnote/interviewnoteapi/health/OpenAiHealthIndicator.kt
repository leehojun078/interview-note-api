package com.hojun.interviewnote.interviewnoteapi.health

import com.hojun.interviewnote.interviewnoteapi.config.OpenAiProperties
import com.hojun.interviewnote.interviewnoteapi.service.ai.AiClient
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

/**
 * OpenAI API 헬스 체크
 *
 * OpenAI API의 연결 상태를 확인합니다.
 * Kubernetes liveness/readiness probe에 사용될 수 있습니다.
 *
 * 현재는 설정 정보만 확인하며, 실제 API 호출은 하지 않습니다.
 * (비용 절감 및 불필요한 API 호출 방지)
 */
@Component
class OpenAiHealthIndicator(
    private val openAiProperties: OpenAiProperties,
    private val aiClient: AiClient
) : HealthIndicator {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun health(): Health {
        return try {
            // OpenAI 설정 확인
            val apiKeyConfigured = openAiProperties.apiKey.isNotBlank()

            if (!apiKeyConfigured) {
                logger.warn("OpenAI API 키가 설정되지 않음")
                return Health.down()
                    .withDetail("provider", "OpenAI")
                    .withDetail("error", "API 키가 설정되지 않았습니다")
                    .build()
            }

            // 정상 상태
            Health.up()
                .withDetail("provider", "OpenAI")
                .withDetail("model", openAiProperties.model)
                .withDetail("promptVersion", openAiProperties.promptVersion)
                .withDetail("maxTokens", openAiProperties.maxTokens)
                .withDetail("temperature", openAiProperties.temperature)
                .withDetail("timeout", "${openAiProperties.timeout}ms")
                .build()

        } catch (e: Exception) {
            logger.error("OpenAI 헬스 체크 실패", e)
            Health.down()
                .withDetail("provider", "OpenAI")
                .withDetail("error", e.message ?: "알 수 없는 오류")
                .build()
        }
    }
}
