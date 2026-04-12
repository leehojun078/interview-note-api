package com.hojun.interviewnote.interviewnoteapi.exception

/**
 * AI 관련 예외의 기본 클래스
 */
sealed class AiException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * OpenAI API 호출 실패 (네트워크, 인증, Rate Limit 등)
 */
class AiApiException(message: String, cause: Throwable? = null) : AiException(message, cause)

/**
 * OpenAI 응답 파싱 실패 (잘못된 JSON 형식)
 */
class AiResponseParseException(
    message: String,
    val rawResponse: String,        // 디버깅용 원본 응답 저장
    cause: Throwable? = null
) : AiException(message, cause)

/**
 * AI 요청 중 일반 오류 (타임아웃, 알 수 없는 오류)
 */
class AiRequestException(message: String, cause: Throwable? = null) : AiException(message, cause)

/**
 * AI 응답이 비어있음
 */
class AiResponseException(message: String) : AiException(message)
