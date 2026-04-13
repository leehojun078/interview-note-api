package com.hojun.interviewnote.interviewnoteapi.service.ratelimit

import com.hojun.interviewnote.interviewnoteapi.exception.RateLimitExceededException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

/**
 * RateLimitService 단위 테스트
 *
 * IP별 요청 제한 로직을 테스트합니다.
 */
class RateLimitServiceTest {

    private lateinit var rateLimitService: RateLimitService

    @BeforeEach
    fun setUp() {
        rateLimitService = RateLimitService()
    }

    @Test
    fun `checkAndRecordRequest - 첫 요청은 허용된다`() {
        // Given
        val ip = "192.168.1.1"

        // When & Then
        assertDoesNotThrow {
            rateLimitService.checkAndRecordRequest(ip)
        }
        assertEquals(1, rateLimitService.getCurrentRequestCount(ip))
    }

    @Test
    fun `checkAndRecordRequest - 33회까지 요청이 허용된다`() {
        // Given
        val ip = "192.168.1.2"

        // When & Then
        repeat(33) {
            assertDoesNotThrow {
                rateLimitService.checkAndRecordRequest(ip)
            }
        }
        assertEquals(33, rateLimitService.getCurrentRequestCount(ip))
    }

    @Test
    fun `checkAndRecordRequest - 34번째 요청은 거부된다`() {
        // Given
        val ip = "192.168.1.3"
        repeat(33) {
            rateLimitService.checkAndRecordRequest(ip)
        }

        // When & Then
        val exception = assertThrows<RateLimitExceededException> {
            rateLimitService.checkAndRecordRequest(ip)
        }

        assertTrue(exception.message!!.contains("요청 한도"))
        assertTrue(exception.message!!.contains("33"))
    }

    @Test
    fun `checkAndRecordRequest - 다른 IP는 독립적으로 카운트된다`() {
        // Given
        val ip1 = "192.168.1.4"
        val ip2 = "192.168.1.5"

        // When
        repeat(33) {
            rateLimitService.checkAndRecordRequest(ip1)
        }
        rateLimitService.checkAndRecordRequest(ip2)

        // Then
        assertEquals(33, rateLimitService.getCurrentRequestCount(ip1))
        assertEquals(1, rateLimitService.getCurrentRequestCount(ip2))

        // ip1은 거부되지만 ip2는 허용
        assertThrows<RateLimitExceededException> {
            rateLimitService.checkAndRecordRequest(ip1)
        }
        assertDoesNotThrow {
            rateLimitService.checkAndRecordRequest(ip2)
        }
    }

    @Test
    fun `getCurrentRequestCount - 요청이 없으면 0을 반환한다`() {
        // Given
        val ip = "192.168.1.6"

        // When
        val count = rateLimitService.getCurrentRequestCount(ip)

        // Then
        assertEquals(0, count)
    }

    @Test
    fun `getCurrentRequestCount - 요청 후 카운트가 증가한다`() {
        // Given
        val ip = "192.168.1.7"

        // When
        rateLimitService.checkAndRecordRequest(ip)
        rateLimitService.checkAndRecordRequest(ip)
        rateLimitService.checkAndRecordRequest(ip)

        // Then
        assertEquals(3, rateLimitService.getCurrentRequestCount(ip))
    }

    @Test
    fun `checkAndRecordRequest - IPv6 주소도 처리한다`() {
        // Given
        val ipv6 = "2001:0db8:85a3:0000:0000:8a2e:0370:7334"

        // When & Then
        assertDoesNotThrow {
            rateLimitService.checkAndRecordRequest(ipv6)
        }
        assertEquals(1, rateLimitService.getCurrentRequestCount(ipv6))
    }

    @Test
    fun `checkAndRecordRequest - localhost IP도 처리한다`() {
        // Given
        val localhost = "127.0.0.1"

        // When & Then
        assertDoesNotThrow {
            rateLimitService.checkAndRecordRequest(localhost)
        }
        assertEquals(1, rateLimitService.getCurrentRequestCount(localhost))
    }

    @Test
    fun `checkAndRecordRequest - 33회 요청 후 한도 초과 시 resetTime을 포함한다`() {
        // Given
        val ip = "192.168.1.8"
        repeat(33) {
            rateLimitService.checkAndRecordRequest(ip)
        }

        // When
        val exception = assertThrows<RateLimitExceededException> {
            rateLimitService.checkAndRecordRequest(ip)
        }

        // Then
        assertTrue(exception.message!!.contains("재설정 시간"))
        assertTrue(exception.resetTime.isAfter(java.time.LocalDateTime.now()))
    }

    @Test
    fun `checkAndRecordRequest - 빈 문자열 IP도 처리한다`() {
        // Given
        val emptyIp = ""

        // When & Then
        assertDoesNotThrow {
            rateLimitService.checkAndRecordRequest(emptyIp)
        }
        assertEquals(1, rateLimitService.getCurrentRequestCount(emptyIp))
    }

    @Test
    fun `checkAndRecordRequest - 특수문자가 포함된 IP도 처리한다`() {
        // Given
        val specialIp = "192.168.1.1:8080" // 포트 포함

        // When & Then
        assertDoesNotThrow {
            rateLimitService.checkAndRecordRequest(specialIp)
        }
        assertEquals(1, rateLimitService.getCurrentRequestCount(specialIp))
    }

    @Test
    fun `checkAndRecordRequest - 연속 요청 시 카운트가 정확히 증가한다`() {
        // Given
        val ip = "192.168.1.9"

        // When & Then
        for (i in 1..10) {
            rateLimitService.checkAndRecordRequest(ip)
            assertEquals(i, rateLimitService.getCurrentRequestCount(ip))
        }
    }

    @Test
    fun `checkAndRecordRequest - 한도 초과 후에도 카운트는 변하지 않는다`() {
        // Given
        val ip = "192.168.1.10"
        repeat(33) {
            rateLimitService.checkAndRecordRequest(ip)
        }

        // When
        val countBefore = rateLimitService.getCurrentRequestCount(ip)
        assertThrows<RateLimitExceededException> {
            rateLimitService.checkAndRecordRequest(ip)
        }
        val countAfter = rateLimitService.getCurrentRequestCount(ip)

        // Then
        assertEquals(33, countBefore)
        assertEquals(33, countAfter) // 거부된 요청은 카운트에 포함되지 않음
    }

    @Test
    fun `checkAndRecordRequest - 여러 IP에 대해 동시 처리가 가능하다`() {
        // Given
        val ips = (1..10).map { "192.168.1.$it" }

        // When
        ips.forEach { ip ->
            repeat(5) {
                rateLimitService.checkAndRecordRequest(ip)
            }
        }

        // Then
        ips.forEach { ip ->
            assertEquals(5, rateLimitService.getCurrentRequestCount(ip))
        }
    }

    @Test
    fun `RateLimitExceededException - IP와 한도 정보를 포함한다`() {
        // Given
        val ip = "192.168.1.11"
        repeat(33) {
            rateLimitService.checkAndRecordRequest(ip)
        }

        // When
        val exception = assertThrows<RateLimitExceededException> {
            rateLimitService.checkAndRecordRequest(ip)
        }

        // Then
        assertTrue(exception.message!!.contains(ip))
        assertTrue(exception.message!!.contains("33"))
    }

    @Test
    fun `getCurrentRequestCount - 한도 초과 후에도 정확한 카운트를 반환한다`() {
        // Given
        val ip = "192.168.1.12"
        repeat(33) {
            rateLimitService.checkAndRecordRequest(ip)
        }

        // When
        try {
            rateLimitService.checkAndRecordRequest(ip)
        } catch (e: RateLimitExceededException) {
            // 예외 무시
        }

        // Then
        assertEquals(33, rateLimitService.getCurrentRequestCount(ip))
    }
}
