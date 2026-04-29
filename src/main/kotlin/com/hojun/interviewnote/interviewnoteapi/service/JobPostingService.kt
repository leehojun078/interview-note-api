package com.hojun.interviewnote.interviewnoteapi.service

import com.hojun.interviewnote.interviewnoteapi.domain.JobField
import com.hojun.interviewnote.interviewnoteapi.domain.JobPosting
import com.hojun.interviewnote.interviewnoteapi.dto.GeneratedQuestionDto
import com.hojun.interviewnote.interviewnoteapi.dto.JobPostingSummaryDto
import com.hojun.interviewnote.interviewnoteapi.dto.JobPostingViewModel
import com.hojun.interviewnote.interviewnoteapi.exception.JobPostingNotFoundException
import com.hojun.interviewnote.interviewnoteapi.exception.JobPostingParseException
import com.hojun.interviewnote.interviewnoteapi.repository.GeneratedQuestionRepository
import com.hojun.interviewnote.interviewnoteapi.repository.JobPostingRepository
import com.hojun.interviewnote.interviewnoteapi.service.cache.JobPostingCache
import com.hojun.interviewnote.interviewnoteapi.service.ratelimit.RateLimitService
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 채용 공고 서비스 (Orchestration)
 *
 * Phase 6C에서 추가됨
 * - 공고 등록 플로우: Rate limit → 캐시 체크 → 파싱 → 저장 → 질문 생성
 * - 공고 조회: 소유권 검증
 */
@Service
@Transactional
class JobPostingService(
    private val jobPostingRepository: JobPostingRepository,
    private val generatedQuestionRepository: GeneratedQuestionRepository,
    private val jobPostingParserService: JobPostingParserService,
    private val questionGeneratorService: QuestionGeneratorService,
    private val rateLimitService: RateLimitService,
    private val jobPostingCache: JobPostingCache,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 채용 공고 등록 및 질문 생성
     *
     * 흐름:
     * 1. Rate Limiting 체크 (10회/24시간)
     * 2. 7일 캐시 체크 (동일 URL 재사용)
     * 3. URL 파싱 (Jsoup + AI Fallback)
     * 4. JobPosting 엔티티 저장
     * 5. AI 질문 10개 생성
     * 6. GeneratedQuestion 엔티티 저장
     *
     * @param userId 사용자 ID
     * @param originalUrl 채용 공고 URL
     * @param selectedJobField 사용자 선택 직무 (선택 사항)
     * @return 생성된 공고 (질문 포함)
     * @throws RateLimitExceededException Rate Limiting 초과 시
     * @throws JobPostingParseException 파싱 실패 시
     */
    fun createJobPosting(
        userId: Long,
        originalUrl: String,
        selectedJobField: JobField?
    ): JobPosting {
        logger.info("공고 등록 시작 - 사용자: $userId, URL: $originalUrl")

        // 1. Rate Limiting 체크
        rateLimitService.checkAndRecordQuestionGeneration(userId)

        // 2. 7일 캐시 체크
        val cached = jobPostingCache.findCachedByUrl(originalUrl)
        if (cached != null) {
            logger.info("캐시된 공고 재사용 - 공고 ID: ${cached.id}")
            return cached
        }

        // 3. URL 파싱
        val parsed = jobPostingParserService.parseFromUrl(originalUrl)
            ?: throw JobPostingParseException("채용 공고 파싱에 실패했습니다. URL을 확인해주세요.")

        // 4. JobPosting 엔티티 생성 및 저장
        val jobPosting = JobPosting(
            userId = userId,
            originalUrl = originalUrl,
            companyName = parsed.companyName,
            jobTitle = parsed.jobTitle,
            jobDescription = parsed.jobDescription,
            selectedJobField = selectedJobField,
            inferredJobField = parsed.inferredJobField,
            requiredSkills = parsed.requiredSkills?.let { objectMapper.writeValueAsString(it) },
            preferredSkills = parsed.preferredSkills?.let { objectMapper.writeValueAsString(it) }
        )

        val saved = jobPostingRepository.save(jobPosting)
        logger.info("공고 저장 완료 - 공고 ID: ${saved.id}, 회사: ${saved.companyName}")

        // 5. AI 질문 생성
        val questions = questionGeneratorService.generateQuestions(saved)

        // 6. GeneratedQuestion 엔티티 저장
        questions.forEach { generatedQuestionRepository.save(it) }
        logger.info("질문 생성 완료 - 공고 ID: ${saved.id}, 질문 개수: ${questions.size}")

        return saved
    }

    /**
     * 공고 상세 조회 (질문 포함)
     *
     * @param jobPostingId 공고 ID
     * @param userId 사용자 ID (소유권 검증)
     * @return 공고 ViewModel
     * @throws JobPostingNotFoundException 공고를 찾을 수 없거나 권한 없음
     */
    @Transactional(readOnly = true)
    fun getJobPostingWithQuestions(jobPostingId: Long, userId: Long): JobPostingViewModel {
        val jobPosting = jobPostingRepository.findById(jobPostingId).orElseThrow {
            JobPostingNotFoundException("공고를 찾을 수 없습니다: ID=$jobPostingId")
        }

        // 소유권 검증
        if (jobPosting.userId != userId) {
            throw JobPostingNotFoundException("이 공고에 대한 접근 권한이 없습니다")
        }

        // 질문 조회
        val questions = generatedQuestionRepository.findByJobPostingIdOrderByOrderIndexAsc(jobPostingId)

        return JobPostingViewModel(
            id = jobPosting.id,
            userId = jobPosting.userId,
            originalUrl = jobPosting.originalUrl,
            companyName = jobPosting.companyName,
            jobTitle = jobPosting.jobTitle,
            jobDescription = jobPosting.jobDescription,
            effectiveJobField = jobPosting.effectiveJobField,
            requiredSkills = jobPosting.requiredSkills?.let {
                objectMapper.readValue(it, List::class.java) as List<String>
            },
            preferredSkills = jobPosting.preferredSkills?.let {
                objectMapper.readValue(it, List::class.java) as List<String>
            },
            createdAt = jobPosting.createdAt,
            questions = questions.map { question ->
                GeneratedQuestionDto(
                    id = question.id,
                    content = question.content,
                    category = question.category,
                    difficulty = question.difficulty,
                    aiReasoning = question.aiReasoning,
                    orderIndex = question.orderIndex
                )
            }
        )
    }

    /**
     * 사용자의 공고 목록 조회
     *
     * @param userId 사용자 ID
     * @return 공고 목록 (요약)
     */
    @Transactional(readOnly = true)
    fun findByUserId(userId: Long): List<JobPostingSummaryDto> {
        val jobPostings = jobPostingRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId)

        return jobPostings.map { jobPosting ->
            val questionCount = generatedQuestionRepository.countByJobPostingId(jobPosting.id).toInt()

            JobPostingSummaryDto(
                id = jobPosting.id,
                companyName = jobPosting.companyName,
                jobTitle = jobPosting.jobTitle,
                effectiveJobField = jobPosting.effectiveJobField,
                questionCount = questionCount,
                createdAt = jobPosting.createdAt
            )
        }
    }
}
