package com.hojun.interviewnote.interviewnoteapi.repository

import com.hojun.interviewnote.interviewnoteapi.domain.JobPosting
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 채용 공고 Repository
 *
 * Phase 6A에서 추가됨
 * - 사용자별 공고 조회
 * - URL 기반 중복 체크 (7일 캐싱)
 */
@Repository
interface JobPostingRepository : JpaRepository<JobPosting, Long> {

    /**
     * 특정 사용자의 활성화된 공고 목록 조회
     * - 최신순 정렬 (createdAt DESC)
     * - soft delete된 공고는 제외
     *
     * @param userId 사용자 ID
     * @return 사용자의 활성 공고 목록
     */
    fun findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId: Long): List<JobPosting>

    /**
     * 특정 URL의 최근 공고 조회 (7일 캐싱용)
     * - 동일 URL로 7일 내 생성된 공고가 있으면 재사용
     * - createdAt이 특정 시각 이후인 공고 중 가장 최근 것
     *
     * @param originalUrl 원본 공고 URL
     * @param createdAfter 이 시각 이후 생성된 공고만 조회
     * @return 가장 최근 공고 (없으면 null)
     */
    fun findFirstByOriginalUrlAndCreatedAtAfterOrderByCreatedAtDesc(
        originalUrl: String,
        createdAfter: LocalDateTime
    ): JobPosting?

    /**
     * 특정 사용자의 활성 공고 개수
     * - Rate Limiting 검증용
     *
     * @param userId 사용자 ID
     * @return 활성 공고 개수
     */
    fun countByUserIdAndIsActiveTrue(userId: Long): Long
}
