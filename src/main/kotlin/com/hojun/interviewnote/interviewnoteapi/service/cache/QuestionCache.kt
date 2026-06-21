package com.hojun.interviewnote.interviewnoteapi.service.cache

import com.hojun.interviewnote.interviewnoteapi.config.CacheProperties
import com.hojun.interviewnote.interviewnoteapi.domain.GeneratedQuestion
import com.hojun.interviewnote.interviewnoteapi.repository.GeneratedQuestionRepository
import com.hojun.interviewnote.interviewnoteapi.repository.JobPostingRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 질문 캐시 서비스
 *
 * Phase 6E에서 추가됨 (버그 수정)
 * - 동일 URL의 질문을 설정된 기간 이내에 재생성하지 않도록 방지
 * - JobPosting은 사용자별로 독립 생성하되, 질문만 캐싱하여 재사용
 * - 비용 절감 및 중복 방지
 *
 * **JobPostingCache와의 차이점**:
 * - JobPostingCache: JobPosting 전체를 캐싱 (소유권 문제 발생) ❌
 * - QuestionCache: GeneratedQuestion만 캐싱 (사용자별 독립 + 비용 절감) ✅
 */
@Service
class QuestionCache(
    private val jobPostingRepository: JobPostingRepository,
    private val generatedQuestionRepository: GeneratedQuestionRepository,
    private val cacheProperties: CacheProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 캐시된 질문 조회 (설정된 기간 이내)
     *
     * 동일 URL로 설정된 기간 내 생성된 공고가 있으면 해당 질문들을 반환
     * userId와 무관하게 전역적으로 캐싱 (AI 비용 절감)
     *
     * @param originalUrl 원본 공고 URL
     * @return 캐시된 질문 리스트 (없으면 emptyList)
     */
    fun findCachedQuestions(originalUrl: String): List<GeneratedQuestion> {
        val cutoffTime = LocalDateTime.now().minusDays(cacheProperties.questionDays)

        // 1. 동일 URL의 최근 공고 찾기
        val cachedPosting = jobPostingRepository.findFirstByOriginalUrlAndCreatedAtAfterOrderByCreatedAtDesc(
            originalUrl = originalUrl,
            createdAfter = cutoffTime
        )

        if (cachedPosting == null) {
            logger.debug("질문 캐시 미스 - URL: $originalUrl (7일 내 공고 없음)")
            return emptyList()
        }

        // 2. 해당 공고의 질문들 조회
        val questions = generatedQuestionRepository.findByJobPostingIdOrderByOrderIndexAsc(cachedPosting.id)

        if (questions.isNotEmpty()) {
            logger.info(
                "질문 캐시 히트 - URL: $originalUrl, 원본 공고 ID: ${cachedPosting.id}, " +
                        "질문 개수: ${questions.size}, 생성일: ${cachedPosting.createdAt}"
            )
        } else {
            logger.warn("질문 캐시 이상 - 공고는 있으나 질문 없음: 공고 ID=${cachedPosting.id}")
        }

        return questions
    }

    /**
     * 캐시 존재 여부 확인 (빠른 체크)
     *
     * @param originalUrl 원본 공고 URL
     * @return 캐시 존재 여부
     */
    fun hasCachedQuestions(originalUrl: String): Boolean {
        return findCachedQuestions(originalUrl).isNotEmpty()
    }
}
