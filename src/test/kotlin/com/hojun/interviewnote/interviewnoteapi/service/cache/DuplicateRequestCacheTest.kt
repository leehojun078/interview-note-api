package com.hojun.interviewnote.interviewnoteapi.service.cache

import com.hojun.interviewnote.interviewnoteapi.config.CacheProperties
import com.hojun.interviewnote.interviewnoteapi.domain.AiFeedback
import com.hojun.interviewnote.interviewnoteapi.repository.AiFeedbackRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

/**
 * DuplicateRequestCache 단위 테스트
 *
 * SHA-256 해시 생성 및 중복 요청 감지 로직을 테스트합니다.
 */
@ExtendWith(MockitoExtension::class)
class DuplicateRequestCacheTest {

    @Mock
    private lateinit var aiFeedbackRepository: AiFeedbackRepository

    private lateinit var duplicateRequestCache: DuplicateRequestCache

    private val cacheProperties = CacheProperties(
        duplicateRequestHours = 24,
        questionDays = 7,
        jobPostingDays = 7
    )

    @BeforeEach
    fun setUp() {
        duplicateRequestCache = DuplicateRequestCache(aiFeedbackRepository, cacheProperties)
    }

    @Test
    fun `generateHash - 동일한 입력에 대해 동일한 해시를 생성한다`() {
        // Given
        val questionId = 1L
        val answerText = "테스트 답변"

        // When
        val hash1 = duplicateRequestCache.generateHash(questionId, answerText)
        val hash2 = duplicateRequestCache.generateHash(questionId, answerText)

        // Then
        assertEquals(hash1, hash2)
    }

    @Test
    fun `generateHash - SHA-256 해시는 64자 16진수 문자열이다`() {
        // Given
        val questionId = 1L
        val answerText = "테스트 답변"

        // When
        val hash = duplicateRequestCache.generateHash(questionId, answerText)

        // Then
        assertEquals(64, hash.length, "SHA-256 해시는 64자여야 함")
        assertTrue(hash.matches(Regex("^[0-9a-f]{64}$")), "16진수 문자열이어야 함")
    }

    @Test
    fun `generateHash - 다른 질문ID에 대해 다른 해시를 생성한다`() {
        // Given
        val answerText = "동일한 답변"
        val questionId1 = 1L
        val questionId2 = 2L

        // When
        val hash1 = duplicateRequestCache.generateHash(questionId1, answerText)
        val hash2 = duplicateRequestCache.generateHash(questionId2, answerText)

        // Then
        assertTrue(hash1 != hash2, "다른 질문 ID는 다른 해시를 생성해야 함")
    }

    @Test
    fun `generateHash - 다른 답변에 대해 다른 해시를 생성한다`() {
        // Given
        val questionId = 1L
        val answerText1 = "첫 번째 답변"
        val answerText2 = "두 번째 답변"

        // When
        val hash1 = duplicateRequestCache.generateHash(questionId, answerText1)
        val hash2 = duplicateRequestCache.generateHash(questionId, answerText2)

        // Then
        assertTrue(hash1 != hash2, "다른 답변은 다른 해시를 생성해야 함")
    }

    @Test
    fun `generateHash - 공백 차이도 다른 해시를 생성한다`() {
        // Given
        val questionId = 1L
        val answerText1 = "답변"
        val answerText2 = "답변 "

        // When
        val hash1 = duplicateRequestCache.generateHash(questionId, answerText1)
        val hash2 = duplicateRequestCache.generateHash(questionId, answerText2)

        // Then
        assertTrue(hash1 != hash2, "공백 차이도 다른 해시를 생성해야 함")
    }

    @Test
    fun `generateHash - 빈 문자열도 올바르게 처리한다`() {
        // Given
        val questionId = 1L
        val answerText = ""

        // When
        val hash = duplicateRequestCache.generateHash(questionId, answerText)

        // Then
        assertNotNull(hash)
        assertEquals(64, hash.length)
    }

    @Test
    fun `generateHash - 긴 텍스트도 올바르게 처리한다`() {
        // Given
        val questionId = 1L
        val answerText = "답변 ".repeat(1000) // 6000자

        // When
        val hash = duplicateRequestCache.generateHash(questionId, answerText)

        // Then
        assertNotNull(hash)
        assertEquals(64, hash.length)
    }

    @Test
    fun `generateHash - 특수문자가 포함된 텍스트도 올바르게 처리한다`() {
        // Given
        val questionId = 1L
        val answerText = "답변 with 특수문자!@#\$%^&*()_+-=[]{}|;':\",./<>?"

        // When
        val hash = duplicateRequestCache.generateHash(questionId, answerText)

        // Then
        assertNotNull(hash)
        assertEquals(64, hash.length)
    }

    @Test
    fun `generateHash - 유니코드 문자도 올바르게 처리한다`() {
        // Given
        val questionId = 1L
        val answerText = "한글 답변 🎉 with emoji"

        // When
        val hash = duplicateRequestCache.generateHash(questionId, answerText)

        // Then
        assertNotNull(hash)
        assertEquals(64, hash.length)
    }

    @Test
    fun `findCached - 캐시 히트 시 피드백을 반환한다`() {
        // Given
        val questionId = 1L
        val answerText = "테스트 답변"
        val hash = duplicateRequestCache.generateHash(questionId, answerText)
        val now = LocalDateTime.now()

        val cachedFeedback = AiFeedback(
            id = 100L,
            interviewAnswerId = 1L,
            logicScore = 4,
            specificityScore = 3,
            jobFitScore = 5,
            deliveryScore = 4,
            strengths = "[]",
            improvements = "[]",
            modelAnswer = "모범답변",
            overallComment = "좋습니다",
            jobField = "IT",
            modelName = "gpt-4o-mini",
            promptVersion = "v1.0",
            tokenUsageInput = 100,
            tokenUsageOutput = 50,
            rawResponse = "{}",
            answerTextHash = hash,
            createdAt = now.minusHours(1) // 1시간 전
        )

        whenever(aiFeedbackRepository.findByAnswerTextHashAndCreatedAtAfter(any(), any()))
            .thenReturn(cachedFeedback)

        // When
        val result = duplicateRequestCache.findCached(questionId, answerText)

        // Then
        assertNotNull(result)
        assertEquals(100L, result!!.id)
        assertEquals(hash, result.answerTextHash)

        verify(aiFeedbackRepository).findByAnswerTextHashAndCreatedAtAfter(
            org.mockito.kotlin.eq(hash),
            any()
        )
    }

    @Test
    fun `findCached - 캐시 미스 시 null을 반환한다`() {
        // Given
        val questionId = 1L
        val answerText = "테스트 답변"

        whenever(aiFeedbackRepository.findByAnswerTextHashAndCreatedAtAfter(any(), any()))
            .thenReturn(null)

        // When
        val result = duplicateRequestCache.findCached(questionId, answerText)

        // Then
        assertNull(result)
    }

    @Test
    fun `findCached - 24시간 기준으로 캐시를 조회한다`() {
        // Given
        val questionId = 1L
        val answerText = "테스트 답변"
        val hash = duplicateRequestCache.generateHash(questionId, answerText)

        whenever(aiFeedbackRepository.findByAnswerTextHashAndCreatedAtAfter(any(), any()))
            .thenReturn(null)

        // When
        duplicateRequestCache.findCached(questionId, answerText)

        // Then
        verify(aiFeedbackRepository).findByAnswerTextHashAndCreatedAtAfter(
            org.mockito.kotlin.eq(hash),
            org.mockito.kotlin.argThat { cutoffTime ->
                // cutoffTime이 대략 24시간 전인지 확인 (1분 오차 허용)
                val now = LocalDateTime.now()
                val expectedCutoff = now.minusHours(24)
                cutoffTime.isAfter(expectedCutoff.minusMinutes(1)) &&
                        cutoffTime.isBefore(expectedCutoff.plusMinutes(1))
            }
        )
    }

    @Test
    fun `findCached - 같은 입력에 대해 항상 같은 해시로 조회한다`() {
        // Given
        val questionId = 1L
        val answerText = "테스트 답변"

        whenever(aiFeedbackRepository.findByAnswerTextHashAndCreatedAtAfter(any(), any()))
            .thenReturn(null)

        // When
        duplicateRequestCache.findCached(questionId, answerText)
        duplicateRequestCache.findCached(questionId, answerText)

        // Then
        // 두 번 호출했을 때 같은 해시로 조회하는지 확인
        val expectedHash = duplicateRequestCache.generateHash(questionId, answerText)
        verify(aiFeedbackRepository, org.mockito.kotlin.times(2))
            .findByAnswerTextHashAndCreatedAtAfter(
                org.mockito.kotlin.eq(expectedHash),
                any()
            )
    }

    @Test
    fun `generateHash - 콜론이 포함된 형식으로 입력을 조합한다`() {
        // Given
        val questionId = 123L
        val answerText = "테스트"

        // When
        val hash = duplicateRequestCache.generateHash(questionId, answerText)

        // Then
        // "123:테스트"의 SHA-256 해시인지 검증
        val expectedInput = "123:테스트"
        val expectedHash = java.security.MessageDigest.getInstance("SHA-256")
            .digest(expectedInput.toByteArray())
            .joinToString("") { "%02x".format(it) }

        assertEquals(expectedHash, hash)
    }
}
