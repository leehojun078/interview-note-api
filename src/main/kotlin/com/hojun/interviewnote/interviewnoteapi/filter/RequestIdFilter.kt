package com.hojun.interviewnote.interviewnoteapi.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*

/**
 * 요청 ID 추적 필터
 *
 * 모든 HTTP 요청에 고유한 requestId를 부여하고 MDC에 저장하여
 * 해당 요청의 모든 로그에 requestId가 포함되도록 합니다.
 *
 * 또한 클라이언트 IP 주소를 MDC에 저장합니다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestIdFilter : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val REQUEST_ID_HEADER = "X-Request-ID"
        private const val MDC_REQUEST_ID = "requestId"
        private const val MDC_IP = "ip"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // 요청 ID 생성 (클라이언트가 제공한 경우 사용, 없으면 새로 생성)
        val requestId = request.getHeader(REQUEST_ID_HEADER) ?: UUID.randomUUID().toString()

        // 클라이언트 IP 주소 추출
        val clientIp = getClientIp(request)

        // MDC에 저장 (이후 모든 로그에 자동 포함)
        MDC.put(MDC_REQUEST_ID, requestId)
        MDC.put(MDC_IP, clientIp)

        // 응답 헤더에 요청 ID 추가
        response.addHeader(REQUEST_ID_HEADER, requestId)

        logger.debug("요청 시작 - method: ${request.method}, uri: ${request.requestURI}, ip: $clientIp, requestId: $requestId")

        try {
            filterChain.doFilter(request, response)
        } finally {
            logger.debug("요청 종료 - status: ${response.status}, requestId: $requestId")

            // 요청 처리 완료 후 MDC 정리 (메모리 누수 방지)
            MDC.clear()
        }
    }

    /**
     * 클라이언트 IP 주소 추출
     *
     * 프록시/로드밸런서 뒤에 있을 경우 X-Forwarded-For 헤더 확인
     */
    private fun getClientIp(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) {
            // X-Forwarded-For는 "client, proxy1, proxy2" 형식
            return xForwardedFor.split(",").first().trim()
        }

        val xRealIp = request.getHeader("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) {
            return xRealIp
        }

        return request.remoteAddr
    }
}
