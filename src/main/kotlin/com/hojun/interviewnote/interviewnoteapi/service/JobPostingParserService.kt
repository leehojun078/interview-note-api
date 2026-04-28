package com.hojun.interviewnote.interviewnoteapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.hojun.interviewnote.interviewnoteapi.domain.JobField
import com.hojun.interviewnote.interviewnoteapi.dto.ParsedJobPosting
import com.hojun.interviewnote.interviewnoteapi.exception.JobPostingParseException
import com.hojun.interviewnote.interviewnoteapi.service.ai.AiClient
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URL

/**
 * 채용 공고 파싱 서비스
 *
 * Phase 6A에서 추가됨
 * - 전략: Jsoup (원티드/사람인/잡코리아) → AI Fallback → 수동 입력
 * - MVP에서는 AI Fallback을 메인으로 사용
 */
@Service
class JobPostingParserService(
    private val aiClient: AiClient,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val DEFAULT_TIMEOUT_MS = 10000  // 10초
    }

    /**
     * 채용 공고 URL 파싱
     *
     * 전략:
     * 1. URL에서 사이트 판별 (wanted, saramin, jobkorea)
     * 2. 사이트별 전용 파서 시도 (Jsoup)
     * 3. 실패 시 AI Fallback (OpenAI HTML 파싱)
     * 4. 모두 실패 시 null 반환 (수동 입력 유도)
     *
     * @param url 채용 공고 URL
     * @return 파싱된 공고 데이터 (실패 시 null)
     */
    fun parseFromUrl(url: String): ParsedJobPosting? {
        try {
            logger.info("채용 공고 파싱 시작 - URL: $url")

            // 1. URL 유효성 검증
            val normalizedUrl = normalizeUrl(url)
            val host = extractHost(normalizedUrl)

            // 2. HTML 다운로드
            val document = fetchHtml(normalizedUrl)

            // 3. 사이트별 파서 시도
            val parsed = when {
                host.contains("wanted.co.kr") -> parseWanted(document)
                host.contains("saramin.co.kr") -> parseSaramin(document)
                host.contains("jobkorea.co.kr") -> parseJobKorea(document)
                else -> null
            }

            if (parsed != null) {
                logger.info("사이트별 파서 성공 - 회사: ${parsed.companyName}")
                return parsed
            }

            // 4. AI Fallback
            logger.info("사이트별 파서 실패 - AI Fallback 시도")
            val htmlText = document.html()
            return parseWithAi(htmlText)

        } catch (e: Exception) {
            logger.error("채용 공고 파싱 실패 - URL: $url", e)
            throw JobPostingParseException("채용 공고 파싱 실패: ${e.message}", e)
        }
    }

    /**
     * 원티드 파싱 (Jsoup)
     *
     * TODO: Phase 6B에서 실제 구현
     * MVP에서는 null 반환하여 AI Fallback으로 넘김
     */
    private fun parseWanted(document: Document): ParsedJobPosting? {
        logger.debug("원티드 파싱 시도 (미구현)")
        return null
    }

    /**
     * 사람인 파싱 (Jsoup)
     *
     * TODO: Phase 6B에서 실제 구현
     */
    private fun parseSaramin(document: Document): ParsedJobPosting? {
        logger.debug("사람인 파싱 시도 (미구현)")
        return null
    }

    /**
     * 잡코리아 파싱 (Jsoup)
     *
     * TODO: Phase 6B에서 실제 구현
     */
    private fun parseJobKorea(document: Document): ParsedJobPosting? {
        logger.debug("잡코리아 파싱 시도 (미구현)")
        return null
    }

    /**
     * AI 기반 HTML 파싱 (Fallback)
     *
     * OpenAI API를 사용하여 HTML에서 채용 공고 정보 추출
     *
     * @param html HTML 문자열 (최대 6,000자로 제한)
     * @return 파싱된 공고 데이터 (실패 시 null)
     */
    private fun parseWithAi(html: String): ParsedJobPosting? {
        try {
            // HTML 길이 제한 (AI 토큰 제한 고려)
            val truncatedHtml = if (html.length > 6000) {
                logger.warn("HTML 길이 제한 - 원본: ${html.length}자 → 6000자로 축소")
                html.substring(0, 6000)
            } else {
                html
            }

            // AI 프롬프트 구성
            val systemPrompt = buildParsingSystemPrompt()
            val userPrompt = buildParsingUserPrompt(truncatedHtml)

            // AI 호출
            val rawResponse = aiClient.requestFeedback(systemPrompt, userPrompt)

            // JSON 파싱
            val response = objectMapper.readValue(rawResponse, AiParseResponse::class.java)

            logger.info("AI 파싱 성공 - 회사: ${response.companyName}, 직무: ${response.inferredJobField}")

            return ParsedJobPosting(
                companyName = response.companyName,
                jobTitle = response.jobTitle,
                jobDescription = response.jobDescription,
                inferredJobField = response.inferredJobField?.let { JobField.fromCode(it) },
                requiredSkills = response.requiredSkills,
                preferredSkills = response.preferredSkills
            )

        } catch (e: Exception) {
            logger.error("AI 파싱 실패: ${e.message}", e)
            return null
        }
    }

    /**
     * AI 파싱용 System Prompt
     */
    private fun buildParsingSystemPrompt(): String {
        return """
            당신은 채용 공고 HTML을 분석하여 구조화된 데이터로 변환하는 전문가입니다.

            추출해야 할 정보:
            - 회사명 (companyName)
            - 포지션명 (jobTitle)
            - 직무 설명 (jobDescription): 주요 업무, 자격 요건 등을 포함한 상세 설명
            - 추론된 직무 분야 (inferredJobField): PLANNING, MARKETING, ACCOUNTING, HR, ADMIN, IT, DESIGN, SALES, MD, SERVICE, PRODUCTION, CONSTRUCTION, MEDICAL, EDUCATION, MEDIA, FINANCE, PUBLIC 중 선택
            - 필수 기술 (requiredSkills): 배열 형태
            - 우대 기술 (preferredSkills): 배열 형태

            출력 형식: JSON
            {
              "companyName": "회사명",
              "jobTitle": "포지션명",
              "jobDescription": "직무 설명 (200-2000자)",
              "inferredJobField": "IT",
              "requiredSkills": ["기술1", "기술2"],
              "preferredSkills": ["기술3", "기술4"]
            }

            중요:
            - 정보가 없으면 null 반환
            - jobDescription은 최소 200자 이상
            - inferredJobField는 반드시 위 17개 중 하나 선택
        """.trimIndent()
    }

    /**
     * AI 파싱용 User Prompt
     */
    private fun buildParsingUserPrompt(html: String): String {
        return """
            다음 HTML에서 채용 공고 정보를 추출해주세요:

            $html
        """.trimIndent()
    }

    /**
     * URL 정규화
     */
    private fun normalizeUrl(url: String): String {
        var normalized = url.trim()
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://$normalized"
        }
        return normalized
    }

    /**
     * URL에서 호스트 추출
     */
    private fun extractHost(url: String): String {
        return try {
            URL(url).host.lowercase()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * HTML 다운로드 (Jsoup)
     */
    private fun fetchHtml(url: String): Document {
        return Jsoup.connect(url)
            .timeout(DEFAULT_TIMEOUT_MS)
            .userAgent("Mozilla/5.0 (compatible; InterviewNoteBot/1.0)")
            .get()
    }
}

/**
 * AI 파싱 응답 DTO
 */
private data class AiParseResponse(
    val companyName: String,
    val jobTitle: String,
    val jobDescription: String,
    val inferredJobField: String?,
    val requiredSkills: List<String>?,
    val preferredSkills: List<String>?
)
