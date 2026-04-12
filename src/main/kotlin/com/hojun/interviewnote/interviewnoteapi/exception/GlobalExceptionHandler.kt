package com.hojun.interviewnote.interviewnoteapi.exception

import org.slf4j.LoggerFactory
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(e: NotFoundException, model: Model): String {
        logger.warn("Resource not found: ${e.message}")
        model.addAttribute("errorMessage", e.message)
        return "error/404"
    }

    /**
     * AI 관련 예외 처리
     */
    @ExceptionHandler(AiException::class)
    fun handleAiException(e: AiException, model: Model): String {
        logger.error("AI 작업 실패: ${e.message}", e)

        // 파싱 오류 시 원본 응답 로깅
        if (e is AiResponseParseException) {
            logger.error("원본 AI 응답: ${e.rawResponse}")
        }

        model.addAttribute("errorMessage", "AI 평가 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
        model.addAttribute("errorDetail", e.message)
        return "error/ai-error"
    }

    /**
     * Rate Limit 초과 예외 처리
     */
    @ExceptionHandler(RateLimitExceededException::class)
    fun handleRateLimitExceeded(e: RateLimitExceededException, model: Model): String {
        logger.warn("Rate limit 초과: ${e.message}")

        model.addAttribute("errorMessage", "요청 한도를 초과했습니다")
        model.addAttribute(
            "errorDetail",
            "1시간에 최대 ${e.limit}회까지 평가를 요청할 수 있습니다. ${e.resetTime}에 재설정됩니다."
        )
        return "error/rate-limit"
    }
}
