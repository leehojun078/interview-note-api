package com.hojun.interviewnote.interviewnoteapi.service.cache

import com.hojun.interviewnote.interviewnoteapi.domain.JobPosting
import com.hojun.interviewnote.interviewnoteapi.repository.JobPostingRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 채용 공고 캐시 서비스
 *
 * Phase 6B에서 추가됨
 * - 동일 URL의 공고를 7일 이내에 재생성하지 않도록 방지
 * - 비용 절감 및 중복 방지
 */
@Service
class JobPostingCache(
    private val jobPostingRepository: JobPostingRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val CACHE_DURATION_DAYS = 7L
    }

    /**
     * 캐시된 공고 조회 (7일 이내)
     *
     * 동일 URL로 7일 내 생성된 공고가 있으면 반환
     *
     * @param originalUrl 원본 공고 URL
     * @return 캐시된 공고 (없으면 null)
     */
    fun findCachedByUrl(originalUrl: String): JobPosting? {
        val cutoffTime = LocalDateTime.now().minusDays(CACHE_DURATION_DAYS)

        val cached = jobPostingRepository.findFirstByOriginalUrlAndCreatedAtAfterOrderByCreatedAtDesc(
            originalUrl = originalUrl,
            createdAfter = cutoffTime
        )

        if (cached != null) {
            logger.info(
                "캐시된 공고 사용 - URL: $originalUrl, 공고 ID: ${cached.id}, " +
                        "회사: ${cached.companyName}, 생성일: ${cached.createdAt}"
            )
        } else {
            logger.debug("캐시 미스 - URL: $originalUrl (7일 내 공고 없음)")
        }

        return cached
    }

    /**
     * 캐시 존재 여부 확인 (빠른 체크)
     *
     * @param originalUrl 원본 공고 URL
     * @return 캐시 존재 여부
     */
    fun isCached(originalUrl: String): Boolean {
        return findCachedByUrl(originalUrl) != null
    }
}
