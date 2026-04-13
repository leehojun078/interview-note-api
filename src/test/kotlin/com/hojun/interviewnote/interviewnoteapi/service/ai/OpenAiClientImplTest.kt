package com.hojun.interviewnote.interviewnoteapi.service.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.hojun.interviewnote.interviewnoteapi.config.OpenAiProperties
import com.hojun.interviewnote.interviewnoteapi.exception.AiApiException
import com.hojun.interviewnote.interviewnoteapi.exception.AiRequestException
import com.hojun.interviewnote.interviewnoteapi.exception.AiResponseException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate
import java.lang.reflect.Field

/**
 * OpenAiClientImpl 단위 테스트
 *
 * MockRestServiceServer를 사용하여 실제 HTTP 호출 없이
 * OpenAI API 통신을 테스트합니다.
 */
class OpenAiClientImplTest {

    private lateinit var openAiClient: OpenAiClientImpl
    private lateinit var mockServer: MockRestServiceServer
    private lateinit var properties: OpenAiProperties
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        properties = OpenAiProperties().apply {
            apiKey = "test-api-key"
            model = "gpt-4o-mini"
            maxTokens = 800
            temperature = 0.7
            promptVersion = "v1.0"
            timeout = 30000
        }
        objectMapper = ObjectMapper().registerKotlinModule()
        openAiClient = OpenAiClientImpl(properties, objectMapper)

        // Reflection을 사용하여 private RestTemplate 필드에 접근
        val restTemplateField: Field = OpenAiClientImpl::class.java.getDeclaredField("restTemplate")
        restTemplateField.isAccessible = true
        val restTemplate = restTemplateField.get(openAiClient) as RestTemplate
        mockServer = MockRestServiceServer.createServer(restTemplate)
    }

    @Test
    fun `requestFeedback - 성공 시 응답 내용을 반환한다`() {
        // Given
        val systemPrompt = "You are a helpful assistant."
        val userPrompt = "Test question"
        val expectedContent = "{\"scores\": {\"logic\": 4}}"

        val mockResponse = """
            {
              "id": "chatcmpl-test",
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "${expectedContent.replace("\"", "\\\"")}"
                  },
                  "finish_reason": "stop"
                }
              ],
              "usage": {
                "prompt_tokens": 100,
                "completion_tokens": 50,
                "total_tokens": 150
              }
            }
        """.trimIndent()

        mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer test-api-key"))
            .andExpect(header("Content-Type", "application/json"))
            .andExpect(jsonPath("$.model").value("gpt-4o-mini"))
            .andExpect(jsonPath("$.temperature").value(0.7))
            .andExpect(jsonPath("$.max_tokens").value(800))
            .andExpect(jsonPath("$.response_format.type").value("json_object"))
            .andExpect(jsonPath("$.messages[0].role").value("system"))
            .andExpect(jsonPath("$.messages[0].content").value(systemPrompt))
            .andExpect(jsonPath("$.messages[1].role").value("user"))
            .andExpect(jsonPath("$.messages[1].content").value(userPrompt))
            .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON))

        // When
        val result = openAiClient.requestFeedback(systemPrompt, userPrompt)

        // Then
        mockServer.verify()
        assertEquals(expectedContent, result)
    }

    @Test
    fun `requestFeedback - 401 인증 오류 시 AiApiException 발생`() {
        // Given
        val errorResponse = """{"error": {"message": "Invalid API key"}}"""

        mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body(errorResponse))

        // When & Then
        val exception = assertThrows<AiApiException> {
            openAiClient.requestFeedback("system", "user")
        }
        assertNotNull(exception.message)
        assert(exception.message!!.contains("401"))
    }

    @Test
    fun `requestFeedback - 429 Rate Limit 오류 시 AiApiException 발생`() {
        // Given
        val errorResponse = """{"error": {"message": "Rate limit exceeded"}}"""

        mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse))

        // When & Then
        val exception = assertThrows<AiApiException> {
            openAiClient.requestFeedback("system", "user")
        }
        assertNotNull(exception.message)
        assert(exception.message!!.contains("429"))
    }

    @Test
    fun `requestFeedback - 500 서버 오류 시 AiApiException 발생`() {
        // Given
        mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
            .andRespond(withServerError())

        // When & Then
        val exception = assertThrows<AiApiException> {
            openAiClient.requestFeedback("system", "user")
        }
        assertNotNull(exception.message)
        assert(exception.message!!.contains("500"))
    }

    @Test
    fun `requestFeedback - 응답 본문이 비어있으면 예외 발생`() {
        // Given
        mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON))

        // When & Then
        // 빈 문자열은 JSON 파싱 실패로 AiRequestException 발생
        assertThrows<AiRequestException> {
            openAiClient.requestFeedback("system", "user")
        }
    }

    @Test
    fun `requestFeedback - choices가 비어있으면 예외 발생`() {
        // Given
        val mockResponse = """
            {
              "id": "chatcmpl-test",
              "choices": [],
              "usage": {"prompt_tokens": 0, "completion_tokens": 0, "total_tokens": 0}
            }
        """.trimIndent()

        mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
            .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON))

        // When & Then
        // AiResponseException이 발생하지만 catch 블록에서 AiRequestException으로 wrapping됨
        assertThrows<AiRequestException> {
            openAiClient.requestFeedback("system", "user")
        }
    }

    @Test
    fun `requestFeedback - message content가 null이면 예외 발생`() {
        // Given - Kotlin nullable 처리로 인해 JSON 파싱 단계에서 예외 발생
        val mockResponse = """
            {
              "id": "chatcmpl-test",
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": null
                  },
                  "finish_reason": "stop"
                }
              ],
              "usage": null
            }
        """.trimIndent()

        mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
            .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON))

        // When & Then
        // Kotlin의 non-nullable String에 null을 할당하려고 하면 파싱 단계에서 예외 발생
        assertThrows<AiRequestException> {
            openAiClient.requestFeedback("system", "user")
        }
    }

    @Test
    fun `requestFeedback - 잘못된 JSON 응답 시 AiRequestException 발생`() {
        // Given
        val invalidJson = "This is not JSON"

        mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
            .andRespond(withSuccess(invalidJson, MediaType.APPLICATION_JSON))

        // When & Then
        assertThrows<AiRequestException> {
            openAiClient.requestFeedback("system", "user")
        }
    }

    @Test
    fun `requestFeedback - Bearer 토큰이 올바르게 설정되는지 확인`() {
        // Given
        val mockResponse = """
            {
              "id": "test",
              "choices": [
                {"message": {"role": "assistant", "content": "test"}, "finish_reason": "stop"}
              ],
              "usage": null
            }
        """.trimIndent()

        mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
            .andExpect(header("Authorization", "Bearer test-api-key"))
            .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON))

        // When
        openAiClient.requestFeedback("system", "user")

        // Then
        mockServer.verify()
    }

    @Test
    fun `requestFeedback - JSON Mode가 활성화되는지 확인`() {
        // Given
        val mockResponse = """
            {
              "id": "test",
              "choices": [
                {"message": {"role": "assistant", "content": "test"}, "finish_reason": "stop"}
              ],
              "usage": null
            }
        """.trimIndent()

        mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
            .andExpect(jsonPath("$.response_format.type").value("json_object"))
            .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON))

        // When
        openAiClient.requestFeedback("system", "user")

        // Then
        mockServer.verify()
    }
}
