package com.hojun.interviewnote.interviewnoteapi.dto

import com.hojun.interviewnote.interviewnoteapi.domain.JobField
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * 파싱된 채용 공고 데이터
 *
 * Phase 6A: JobPostingParserService의 반환 타입
 * - Jsoup 또는 AI 파싱 결과
 */
data class ParsedJobPosting(
    val companyName: String,
    val jobTitle: String,
    val jobDescription: String,
    val inferredJobField: JobField?,       // AI가 추론한 직무 (nullable)
    val requiredSkills: List<String>?,     // 필수 기술 (nullable)
    val preferredSkills: List<String>?     // 우대 기술 (nullable)
)

/**
 * 채용 공고 등록 요청 DTO
 *
 * Phase 6A: 사용자가 공고 URL 제출 시 사용
 * - Form submit 데이터
 */
data class CreateJobPostingRequest(
    @field:NotBlank(message = "채용 공고 URL을 입력해주세요")
    @field:Size(max = 2000, message = "URL은 최대 2000자까지 입력 가능합니다")
    val originalUrl: String,

    /**
     * 사용자 선택 직무 (선택 사항)
     * - 미선택 시 null
     */
    val selectedJobField: JobField? = null
)

/**
 * 채용 공고 상세 조회 ViewModel
 *
 * Phase 6A: 공고 정보 + 생성된 질문 목록
 * - Thymeleaf 템플릿에 전달
 */
data class JobPostingViewModel(
    val id: Long,
    val userId: Long,
    val originalUrl: String,
    val companyName: String,
    val jobTitle: String,
    val jobDescription: String,
    val effectiveJobField: JobField?,      // selectedJobField 우선, 없으면 inferredJobField
    val requiredSkills: List<String>?,
    val preferredSkills: List<String>?,
    val createdAt: LocalDateTime,
    val questions: List<GeneratedQuestionDto>
)

/**
 * 생성된 질문 DTO
 *
 * Phase 6A: GeneratedQuestion 엔티티 → DTO
 * - JobPostingViewModel에 포함
 */
data class GeneratedQuestionDto(
    val id: Long,
    val content: String,
    val category: String,
    val difficulty: String,
    val aiReasoning: String,
    val orderIndex: Int
)

/**
 * 채용 공고 목록 조회 ViewModel
 *
 * Phase 6C: 내 공고 목록 페이지용
 * - 간단한 요약 정보
 */
data class JobPostingSummaryDto(
    val id: Long,
    val companyName: String,
    val jobTitle: String,
    val effectiveJobField: JobField?,
    val questionCount: Int,                // 생성된 질문 개수 (정상: 10개)
    val createdAt: LocalDateTime
)
