package com.hojun.interviewnote.interviewnoteapi.service.ratelimit

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.hojun.interviewnote.interviewnoteapi.config.RateLimitProperties
import com.hojun.interviewnote.interviewnoteapi.exception.RateLimitExceededException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Rate Limiting 서비스
 *
 * IP 주소당 시간당 요청 횟수를 제한하여 악의적 사용을 방지합니다.
 * Caffeine Cache를 사용한 in-memory 저장 방식으로 구현되었습니다.
 */
@Service
class RateLimitService(
    private val properties: RateLimitProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    // IP별 요청 기록 (설정된 시간 자동 만료)
    private val requestCache: Cache<String, MutableList<LocalDateTime>> by lazy {
        Caffeine.newBuilder()
            .expireAfterWrite(properties.windowDurationMinutes, TimeUnit.MINUTES)
            .build()
    }

    // Phase 6B: 사용자별 질문 생성 기록 (24시간 자동 만료)
    private val questionGenerationCache: Cache<Long, MutableList<LocalDateTime>> by lazy {
        Caffeine.newBuilder()
            .expireAfterWrite(properties.questionGenerationWindowHours, TimeUnit.HOURS)
            .build()
    }

    // Phase 7B: 사용자별 모의 면접 기록 (24시간 자동 만료)
    private val mockInterviewCache: Cache<Long, MutableList<LocalDateTime>> by lazy {
        Caffeine.newBuilder()
            .expireAfterWrite(properties.mockInterviewWindowHours, TimeUnit.HOURS)
            .build()
    }

    /**
     * 요청 허용 여부 확인 및 기록
     *
     * @param ip 클라이언트 IP 주소
     * @throws RateLimitExceededException 한도 초과 시
     *
     * Thread-safe: synchronized 블록으로 동시성 문제 해결
     */
    fun checkAndRecordRequest(ip: String) {
        synchronized(requestCache) {
            val now = LocalDateTime.now()
            val cutoffTime = now.minusMinutes(properties.windowDurationMinutes)

            // 현재 IP의 요청 기록 가져오기
            val requests = requestCache.get(ip) { mutableListOf() }!!

            // 윈도우 이내 요청만 필터링
            requests.removeIf { it.isBefore(cutoffTime) }

            // 한도 확인
            if (requests.size >= properties.maxRequestsPerHour) {
                val resetTime = requests.first().plusMinutes(properties.windowDurationMinutes)
                logger.warn("Rate limit 초과 - IP: $ip, 현재 요청 수: ${requests.size}/${properties.maxRequestsPerHour}")
                throw RateLimitExceededException(ip, properties.maxRequestsPerHour, resetTime)
            }

            // 요청 기록
            requests.add(now)
            logger.debug("요청 기록 - IP: $ip, 현재 요청 수: ${requests.size}/${properties.maxRequestsPerHour}")
        }
    }

    /**
     * 특정 IP의 현재 요청 수 조회 (테스트용)
     */
    fun getCurrentRequestCount(ip: String): Int {
        val now = LocalDateTime.now()
        val cutoffTime = now.minusMinutes(properties.windowDurationMinutes)
        val requests = requestCache.getIfPresent(ip) ?: return 0

        requests.removeIf { it.isBefore(cutoffTime) }
        return requests.size
    }

    // ========================================
    // Phase 6B: 질문 생성 Rate Limiting
    // ========================================

    /**
     * 질문 생성 허용 여부 확인 및 기록
     *
     * Phase 6B: 사용자당 설정된 횟수/24시간 제한
     *
     * @param userId 사용자 ID
     * @throws RateLimitExceededException 한도 초과 시
     *
     * Thread-safe: synchronized 블록으로 동시성 문제 해결
     */
    fun checkAndRecordQuestionGeneration(userId: Long) {
        synchronized(questionGenerationCache) {
            val now = LocalDateTime.now()
            val cutoffTime = now.minusHours(properties.questionGenerationWindowHours)

            // 현재 사용자의 질문 생성 기록 가져오기
            val generations = questionGenerationCache.get(userId) { mutableListOf() }!!

            // 24시간 이내 생성 기록만 필터링
            generations.removeIf { it.isBefore(cutoffTime) }

            // 한도 확인
            if (generations.size >= properties.maxQuestionGenerationsPerDay) {
                val resetTime = generations.first().plusHours(properties.questionGenerationWindowHours)
                logger.warn(
                    "질문 생성 한도 초과 - 사용자 ID: $userId, " +
                            "현재 생성 수: ${generations.size}/${properties.maxQuestionGenerationsPerDay}"
                )
                throw RateLimitExceededException(
                    ip = "User#$userId",
                    limit = properties.maxQuestionGenerationsPerDay,
                    resetTime = resetTime
                )
            }

            // 생성 기록
            generations.add(now)
            logger.debug(
                "질문 생성 기록 - 사용자 ID: $userId, " +
                        "현재 생성 수: ${generations.size}/${properties.maxQuestionGenerationsPerDay}"
            )
        }
    }

    /**
     * 특정 사용자의 현재 질문 생성 수 조회 (테스트용)
     *
     * Phase 6B
     *
     * @param userId 사용자 ID
     * @return 24시간 내 질문 생성 횟수
     */
    fun getCurrentQuestionGenerationCount(userId: Long): Int {
        val now = LocalDateTime.now()
        val cutoffTime = now.minusHours(properties.questionGenerationWindowHours)
        val generations = questionGenerationCache.getIfPresent(userId) ?: return 0

        generations.removeIf { it.isBefore(cutoffTime) }
        return generations.size
    }

    // ========================================
    // Phase 7B: 모의 면접 Rate Limiting
    // ========================================

    /**
     * 모의 면접 시작 허용 여부 확인 및 기록
     *
     * Phase 7B: 사용자당 설정된 횟수/24시간 제한
     *
     * @param userId 사용자 ID
     * @throws RateLimitExceededException 한도 초과 시
     *
     * Thread-safe: synchronized 블록으로 동시성 문제 해결
     */
    fun checkMockInterviewLimit(userId: Long) {
        synchronized(mockInterviewCache) {
            val now = LocalDateTime.now()
            val cutoffTime = now.minusHours(properties.mockInterviewWindowHours)

            val interviews = mockInterviewCache.get(userId) { mutableListOf() }!!
            interviews.removeIf { it.isBefore(cutoffTime) }

            if (interviews.size >= properties.maxMockInterviewsPerDay) {
                val resetTime = interviews.first().plusHours(properties.mockInterviewWindowHours)
                logger.warn(
                    "모의 면접 한도 초과 - 사용자 ID: $userId, " +
                            "현재: ${interviews.size}/${properties.maxMockInterviewsPerDay}"
                )
                throw RateLimitExceededException(
                    ip = "User#$userId",
                    limit = properties.maxMockInterviewsPerDay,
                    resetTime = resetTime
                )
            }

            interviews.add(now)
            logger.debug(
                "모의 면접 기록 - 사용자 ID: $userId, " +
                        "현재: ${interviews.size}/${properties.maxMockInterviewsPerDay}"
            )
        }
    }

    /**
     * 특정 사용자의 현재 모의 면접 횟수 조회 (테스트용)
     *
     * Phase 7B
     *
     * @param userId 사용자 ID
     * @return 24시간 내 모의 면접 횟수
     */
    fun getCurrentMockInterviewCount(userId: Long): Int {
        val now = LocalDateTime.now()
        val cutoffTime = now.minusHours(properties.mockInterviewWindowHours)
        val interviews = mockInterviewCache.getIfPresent(userId) ?: return 0

        interviews.removeIf { it.isBefore(cutoffTime) }
        return interviews.size
    }
}
