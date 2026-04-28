package com.hojun.interviewnote.interviewnoteapi.exception

/**
 * 채용 공고 관련 예외의 기본 클래스
 *
 * Phase 6A에서 추가됨
 */
sealed class JobPostingException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

/**
 * 채용 공고 파싱 실패
 * - Jsoup 파싱 실패 (HTML 구조 변경, 네트워크 오류)
 * - AI Fallback도 실패한 경우
 */
class JobPostingParseException(message: String, cause: Throwable? = null) :
    JobPostingException(message, cause)

/**
 * 채용 공고를 찾을 수 없음
 * - 존재하지 않는 ID
 * - 권한 없음 (다른 사용자의 공고)
 */
class JobPostingNotFoundException(message: String) : JobPostingException(message)

/**
 * 채용 공고 생성 제한 초과
 * - Rate Limiting: 10회/24시간 초과
 */
class JobPostingRateLimitException(message: String) : JobPostingException(message)

/**
 * 잘못된 공고 URL
 * - URL 형식 오류
 * - 지원하지 않는 사이트
 */
class InvalidJobPostingUrlException(message: String) : JobPostingException(message)
