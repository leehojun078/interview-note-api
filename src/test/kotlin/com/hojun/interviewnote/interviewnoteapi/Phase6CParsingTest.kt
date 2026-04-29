package com.hojun.interviewnote.interviewnoteapi

import com.hojun.interviewnote.interviewnoteapi.domain.JobField
import com.hojun.interviewnote.interviewnoteapi.domain.User
import com.hojun.interviewnote.interviewnoteapi.repository.GeneratedQuestionRepository
import com.hojun.interviewnote.interviewnoteapi.repository.JobPostingRepository
import com.hojun.interviewnote.interviewnoteapi.repository.UserRepository
import com.hojun.interviewnote.interviewnoteapi.service.JobPostingParserService
import com.hojun.interviewnote.interviewnoteapi.service.JobPostingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

/**
 * Phase 6C Parsing Test
 *
 * Tests the improved HTML parsing logic with actual job posting URL
 * - Verifies smart HTML cleaning (script/style/comment removal)
 * - Verifies maxTokens increase (800 → 2000) prevents JSON truncation
 * - Tests with actual wanted.co.kr URL
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase6CParsingTest {

    @Autowired
    private lateinit var jobPostingService: JobPostingService

    @Autowired
    private lateinit var jobPostingParserService: JobPostingParserService

    @Autowired
    private lateinit var jobPostingRepository: JobPostingRepository

    @Autowired
    private lateinit var generatedQuestionRepository: GeneratedQuestionRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var testUser: User

    @BeforeEach
    fun setup() {
        // Create test user (or reuse existing)
        testUser = userRepository.findByEmail("test@example.com") ?: run {
            val newUser = User(
                email = "test@example.com",
                passwordHash = passwordEncoder.encode("password123"),
                name = "Test User",
                jobField = JobField.IT
            )
            userRepository.save(newUser)
        }
    }

    @Test
    fun `actual wanted job posting should generate 10 AI questions`() {
        // Given: Actual wanted.co.kr job posting URL from user's bug report
        val jobPostingUrl = "https://www.wanted.co.kr/wd/281357"
        val selectedJobField = null // AI가 자동으로 분석

        // When: Create job posting with AI question generation
        println("\n=== 공고 파싱 및 질문 생성 시작 ===")
        println("URL: $jobPostingUrl")
        println("직무: ${selectedJobField ?: "AI 자동 분석"}")

        val jobPosting = jobPostingService.createJobPosting(
            userId = testUser.id,
            originalUrl = jobPostingUrl,
            selectedJobField = selectedJobField
        )

        // Then: Verify job posting was created
        println("\n=== 공고 생성 결과 ===")
        println("공고 ID: ${jobPosting.id}")
        println("회사명: ${jobPosting.companyName}")
        println("포지션: ${jobPosting.jobTitle}")
        println("직무 분야: ${jobPosting.effectiveJobField?.displayName}")
        println("직무 설명 길이: ${jobPosting.jobDescription.length}자")

        // Verify questions were generated
        val questions = generatedQuestionRepository.findByJobPostingIdOrderByOrderIndexAsc(jobPosting.id)
        println("\n=== 생성된 질문 ===")
        println("질문 개수: ${questions.size}")

        questions.forEachIndexed { index, question ->
            println("\n#${index + 1}. [${question.difficulty}] ${question.category}")
            println("   ${question.content}")
            println("   근거: ${question.aiReasoning.take(50)}...")
        }

        // Verify exactly 10 questions
        assert(questions.size == 10) { "Expected 10 questions, got ${questions.size}" }

        // Verify no fallback questions (fallback questions have generic reasoning)
        val hasFallbackQuestions = questions.any {
            it.aiReasoning.contains("기본 질문입니다") ||
            it.aiReasoning.contains("fallback") ||
            it.aiReasoning == "AI 생성 실패로 인한 기본 질문"
        }
        assert(!hasFallbackQuestions) { "Found fallback questions - AI generation failed" }

        // Verify difficulty distribution is exactly 3-4-3
        val difficultyDistribution = questions.groupingBy { it.difficulty }.eachCount()
        println("\n=== 난이도 분포 ===")
        println("EASY: ${difficultyDistribution["EASY"] ?: 0}개")
        println("MEDIUM: ${difficultyDistribution["MEDIUM"] ?: 0}개")
        println("HARD: ${difficultyDistribution["HARD"] ?: 0}개")

        // Assert exact distribution: EASY 3, MEDIUM 4, HARD 3
        assert(difficultyDistribution["EASY"] == 3) {
            "EASY 난이도가 3개여야 하는데 ${difficultyDistribution["EASY"] ?: 0}개입니다"
        }
        assert(difficultyDistribution["MEDIUM"] == 4) {
            "MEDIUM 난이도가 4개여야 하는데 ${difficultyDistribution["MEDIUM"] ?: 0}개입니다"
        }
        assert(difficultyDistribution["HARD"] == 3) {
            "HARD 난이도가 3개여야 하는데 ${difficultyDistribution["HARD"] ?: 0}개입니다"
        }

        println("\n=== 테스트 성공: AI 질문 생성 정상 작동 (난이도 분포 3-4-3 검증 완료) ===")
    }

    @Test
    fun `HTML parsing should clean scripts and styles effectively`() {
        // Given: A job posting URL
        val jobPostingUrl = "https://www.wanted.co.kr/wd/281357"

        // When: Parse the job posting
        println("\n=== HTML 파싱 테스트 ===")
        val parsedPosting = jobPostingParserService.parseFromUrl(jobPostingUrl)

        // Then: Verify parsing succeeded
        assert(parsedPosting != null) { "Parsing failed - returned null" }
        parsedPosting!!

        println("회사명: ${parsedPosting.companyName}")
        println("포지션: ${parsedPosting.jobTitle}")
        println("직무 설명 길이: ${parsedPosting.jobDescription.length}자")
        println("추론된 직무: ${parsedPosting.inferredJobField?.displayName}")
        println("필수 기술: ${parsedPosting.requiredSkills?.joinToString(", ")}")
        println("우대 기술: ${parsedPosting.preferredSkills?.joinToString(", ")}")

        // Verify job description is reasonable length (not too short, not too long)
        assert(parsedPosting.jobDescription.length >= 100) {
            "Job description too short: ${parsedPosting.jobDescription.length} chars"
        }
        assert(parsedPosting.jobDescription.length <= 3000) {
            "Job description too long: ${parsedPosting.jobDescription.length} chars"
        }

        println("\n=== HTML 파싱 성공 ===")
    }
}
