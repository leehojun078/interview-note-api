package com.hojun.interviewnote.interviewnoteapi.service.ai

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.hojun.interviewnote.interviewnoteapi.config.OpenAiProperties
import com.hojun.interviewnote.interviewnoteapi.exception.AiApiException
import com.hojun.interviewnote.interviewnoteapi.exception.AiRequestException
import com.hojun.interviewnote.interviewnoteapi.exception.AiResponseException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import java.net.SocketTimeoutException

/**
 * OpenAI API 구현체
 *
 * RestTemplate을 사용하여 OpenAI Chat Completions API를 직접 호출합니다.
 * JSON 모드를 강제하여 구조화된 응답을 보장합니다.
 */
@Service
class OpenAiClientImpl(
    private val properties: OpenAiProperties,
    private val objectMapper: ObjectMapper
) : AiClient {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val restTemplate = RestTemplate()

    companion object {
        private const val OPENAI_API_URL = "https://api.openai.com/v1/chat/completions"
    }

    override fun requestFeedback(systemPrompt: String, userPrompt: String): String {
        try {
            logger.info("OpenAI 피드백 요청 - 모델: ${properties.model}")

            // 1. 요청 본문 생성
            val requestBody = OpenAiRequest(
                model = properties.model,
                messages = listOf(
                    Message(role = "system", content = systemPrompt),
                    Message(role = "user", content = userPrompt)
                ),
                temperature = properties.temperature,
                maxTokens = properties.maxTokens,
                responseFormat = ResponseFormat(type = "json_object")
            )

            // 2. HTTP 헤더 설정
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                setBearerAuth(properties.apiKey)
            }

            // 3. HTTP 요청 전송
            val request = HttpEntity(requestBody, headers)
            val response = restTemplate.exchange(
                OPENAI_API_URL,
                HttpMethod.POST,
                request,
                String::class.java
            )

            // 4. 응답 파싱
            val responseBody = response.body
                ?: throw AiResponseException("OpenAI 응답이 비어있습니다")

            val openAiResponse = objectMapper.readValue(responseBody, OpenAiResponse::class.java)
            val content = openAiResponse.choices.firstOrNull()?.message?.content
                ?: throw AiResponseException("OpenAI 응답에 내용이 없습니다")

            logger.info(
                "OpenAI 피드백 수신 - 토큰: ${openAiResponse.usage?.totalTokens ?: 0}, " +
                        "응답 길이: ${content.length}자"
            )

            return content

        } catch (e: HttpClientErrorException) {
            // 4xx 오류 (인증, Rate Limit 등)
            logger.error("OpenAI API 클라이언트 오류 (${e.statusCode}): ${e.responseBodyAsString}", e)
            throw AiApiException("OpenAI API 호출 실패 (${e.statusCode}): ${e.message}", e)
        } catch (e: HttpServerErrorException) {
            // 5xx 오류
            logger.error("OpenAI API 서버 오류 (${e.statusCode}): ${e.responseBodyAsString}", e)
            throw AiApiException("OpenAI 서버 오류 (${e.statusCode}): ${e.message}", e)
        } catch (e: SocketTimeoutException) {
            // 타임아웃
            logger.error("OpenAI API 타임아웃: ${e.message}", e)
            throw AiRequestException("OpenAI API 타임아웃: ${e.message}", e)
        } catch (e: Exception) {
            // 기타 예외
            logger.error("예상치 못한 오류: ${e.message}", e)
            throw AiRequestException("AI 피드백 요청 실패: ${e.message}", e)
        }
    }

    /**
     * OpenAI API 요청 본문
     */
    internal data class OpenAiRequest(
        val model: String,
        val messages: List<Message>,
        val temperature: Double,
        @JsonProperty("max_tokens")
        val maxTokens: Int,
        @JsonProperty("response_format")
        val responseFormat: ResponseFormat
    )

    /**
     * 메시지
     */
    internal data class Message(
        val role: String,
        val content: String
    )

    /**
     * 응답 형식
     */
    internal data class ResponseFormat(
        val type: String
    )

    /**
     * OpenAI API 응답
     */
    internal data class OpenAiResponse(
        val id: String?,
        val choices: List<Choice>,
        val usage: Usage?
    )

    /**
     * 선택지
     */
    internal data class Choice(
        val message: Message?,
        @JsonProperty("finish_reason")
        val finishReason: String?
    )

    /**
     * 토큰 사용량
     */
    internal data class Usage(
        @JsonProperty("prompt_tokens")
        val promptTokens: Int?,
        @JsonProperty("completion_tokens")
        val completionTokens: Int?,
        @JsonProperty("total_tokens")
        val totalTokens: Int?
    )
}


