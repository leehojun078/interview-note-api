package com.hojun.interviewnote.interviewnoteapi.service.ratelimit

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
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
class RateLimitService {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MAX_REQUESTS_PER_HOUR = 33
        private const val WINDOW_DURATION_MINUTES = 60L

        // Phase 6B: 질문 생성 Rate Limiting
        private const val MAX_QUESTION_GENERATIONS_PER_DAY = 10
        private const val QUESTION_GENERATION_WINDOW_HOURS = 24L

        // Phase 7B: 모의 면접 Rate Limiting
        private const val MAX_MOCK_INTERVIEWS_PER_DAY = 5
        private const val MOCK_INTERVIEW_WINDOW_HOURS = 24L
    }

    // IP별 요청 기록 (1시간 자동 만료)
    private val requestCache: Cache<String, MutableList<LocalDateTime>> = Caffeine.newBuilder()
        .expireAfterWrite(WINDOW_DURATION_MINUTES, TimeUnit.MINUTES)
        .build()

    // Phase 6B: 사용자별 질문 생성 기록 (24시간 자동 만료)
    private val questionGenerationCache: Cache<Long, MutableList<LocalDateTime>> = Caffeine.newBuilder()
        .expireAfterWrite(QUESTION_GENERATION_WINDOW_HOURS, TimeUnit.HOURS)
        .build()

    // Phase 7B: 사용자별 모의 면접 기록 (24시간 자동 만료)
    private val mockInterviewCache: Cache<Long, MutableList<LocalDateTime>> = Caffeine.newBuilder()
        .expireAfterWrite(MOCK_INTERVIEW_WINDOW_HOURS, TimeUnit.HOURS)
        .build()

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
            val cutoffTime = now.minusMinutes(WINDOW_DURATION_MINUTES)

            // 현재 IP의 요청 기록 가져오기
            val requests = requestCache.get(ip) { mutableListOf() }!!

            // 1시간 이내 요청만 필터링
            requests.removeIf { it.isBefore(cutoffTime) }

            // 한도 확인
            if (requests.size >= MAX_REQUESTS_PER_HOUR) {
                val resetTime = requests.first().plusMinutes(WINDOW_DURATION_MINUTES)
                logger.warn("Rate limit 초과 - IP: $ip, 현재 요청 수: ${requests.size}/$MAX_REQUESTS_PER_HOUR")
                throw RateLimitExceededException(ip, MAX_REQUESTS_PER_HOUR, resetTime)
            }

            // 요청 기록
            requests.add(now)
            logger.debug("요청 기록 - IP: $ip, 현재 요청 수: ${requests.size}/$MAX_REQUESTS_PER_HOUR")
        }
    }

    /**
     * 특정 IP의 현재 요청 수 조회 (테스트용)
     */
    fun getCurrentRequestCount(ip: String): Int {
        val now = LocalDateTime.now()
        val cutoffTime = now.minusMinutes(WINDOW_DURATION_MINUTES)
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
     * Phase 6B: 사용자당 10회/24시간 제한
     *
     * @param userId 사용자 ID
     * @throws RateLimitExceededException 한도 초과 시
     *
     * Thread-safe: synchronized 블록으로 동시성 문제 해결
     */
    fun checkAndRecordQuestionGeneration(userId: Long) {
        synchronized(questionGenerationCache) {
            val now = LocalDateTime.now()
            val cutoffTime = now.minusHours(QUESTION_GENERATION_WINDOW_HOURS)

            // 현재 사용자의 질문 생성 기록 가져오기
            val generations = questionGenerationCache.get(userId) { mutableListOf() }!!

            // 24시간 이내 생성 기록만 필터링
            generations.removeIf { it.isBefore(cutoffTime) }

            // 한도 확인
            if (generations.size >= MAX_QUESTION_GENERATIONS_PER_DAY) {
                val resetTime = generations.first().plusHours(QUESTION_GENERATION_WINDOW_HOURS)
                logger.warn(
                    "질문 생성 한도 초과 - 사용자 ID: $userId, " +
                            "현재 생성 수: ${generations.size}/$MAX_QUESTION_GENERATIONS_PER_DAY"
                )
                throw RateLimitExceededException(
                    ip = "User#$userId",
                    limit = MAX_QUESTION_GENERATIONS_PER_DAY,
                    resetTime = resetTime
                )
            }

            // 생성 기록
            generations.add(now)
            logger.debug(
                "질문 생성 기록 - 사용자 ID: $userId, " +
                        "현재 생성 수: ${generations.size}/$MAX_QUESTION_GENERATIONS_PER_DAY"
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
        val cutoffTime = now.minusHours(QUESTION_GENERATION_WINDOW_HOURS)
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
     * Phase 7B: 사용자당 5회/24시간 제한
     *
     * @param userId 사용자 ID
     * @throws RateLimitExceededException 한도 초과 시
     *
     * Thread-safe: synchronized 블록으로 동시성 문제 해결
     */
    fun checkMockInterviewLimit(userId: Long) {
        synchronized(mockInterviewCache) {
            val now = LocalDateTime.now()
            val cutoffTime = now.minusHours(MOCK_INTERVIEW_WINDOW_HOURS)

            val interviews = mockInterviewCache.get(userId) { mutableListOf() }!!
            interviews.removeIf { it.isBefore(cutoffTime) }

            if (interviews.size >= MAX_MOCK_INTERVIEWS_PER_DAY) {
                val resetTime = interviews.first().plusHours(MOCK_INTERVIEW_WINDOW_HOURS)
                logger.warn(
                    "모의 면접 한도 초과 - 사용자 ID: $userId, " +
                            "현재: ${interviews.size}/$MAX_MOCK_INTERVIEWS_PER_DAY"
                )
                throw RateLimitExceededException(
                    ip = "User#$userId",
                    limit = MAX_MOCK_INTERVIEWS_PER_DAY,
                    resetTime = resetTime
                )
            }

            interviews.add(now)
            logger.debug(
                "모의 면접 기록 - 사용자 ID: $userId, " +
                        "현재: ${interviews.size}/$MAX_MOCK_INTERVIEWS_PER_DAY"
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
        val cutoffTime = now.minusHours(MOCK_INTERVIEW_WINDOW_HOURS)
        val interviews = mockInterviewCache.getIfPresent(userId) ?: return 0

        interviews.removeIf { it.isBefore(cutoffTime) }
        return interviews.size
    }
}
