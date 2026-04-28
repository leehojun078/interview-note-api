package com.hojun.interviewnote.interviewnoteapi.repository

import com.hojun.interviewnote.interviewnoteapi.domain.GeneratedQuestion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * AI 생성 질문 Repository
 *
 * Phase 6A에서 추가됨
 * - 공고별 생성된 질문 조회 (순서대로)
 * - 질문 개수 카운트 (검증용)
 */
@Repository
interface GeneratedQuestionRepository : JpaRepository<GeneratedQuestion, Long> {

    /**
     * 특정 공고의 질문 목록 조회
     * - orderIndex 오름차순 정렬 (1, 2, 3, ..., 10)
     *
     * @param jobPostingId 채용 공고 ID
     * @return 순서대로 정렬된 질문 목록 (10개)
     */
    fun findByJobPostingIdOrderByOrderIndexAsc(jobPostingId: Long): List<GeneratedQuestion>

    /**
     * 특정 공고의 질문 개수
     * - 검증용: 정확히 10개인지 확인
     *
     * @param jobPostingId 채용 공고 ID
     * @return 질문 개수 (정상: 10개)
     */
    fun countByJobPostingId(jobPostingId: Long): Long

    /**
     * 특정 공고의 질문 존재 여부
     * - 질문 생성 여부 빠른 확인
     *
     * @param jobPostingId 채용 공고 ID
     * @return 질문이 하나라도 있으면 true
     */
    fun existsByJobPostingId(jobPostingId: Long): Boolean
}
