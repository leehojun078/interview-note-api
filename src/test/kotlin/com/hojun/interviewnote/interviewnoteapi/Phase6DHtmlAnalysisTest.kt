package com.hojun.interviewnote.interviewnoteapi

import com.hojun.interviewnote.interviewnoteapi.service.JobPostingParserService
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * Phase 6D: HTML 파싱 개선 - cleanedHtml 분석 테스트
 *
 * 목적:
 * 1. 현재 cleanHtml() 메서드의 실제 출력 확인
 * 2. 불필요한 텍스트 패턴 및 비율 측정
 * 3. 3가지 개선 방안 비교
 */
@SpringBootTest
@ActiveProfiles("test")
class Phase6DHtmlAnalysisTest {

    @Autowired
    private lateinit var jobPostingParserService: JobPostingParserService

    @Test
    @Disabled("Requires external network access to wanted.co.kr")
    fun `analyze current cleanedHtml content and size`() {
        println("\n========================================")
        println("Phase 6D: cleanedHtml 분석 테스트")
        println("========================================\n")

        // 1. wanted.co.kr HTML 다운로드
        val url = "https://www.wanted.co.kr/wd/281357"
        println("URL: $url\n")

        val document = Jsoup.connect(url)
            .timeout(10000)
            .userAgent("Mozilla/5.0 (compatible; InterviewNoteBot/1.0)")
            .get()

        val rawHtml = document.html()

        // 2. 현재 cleanHtml() 메서드 적용 (리플렉션 사용)
        val cleanedHtml = cleanHtmlViaReflection(rawHtml)

        // 3. 크기 분석
        println("=== HTML 크기 분석 ===")
        println("원본 HTML: ${rawHtml.length}자")
        println("cleanedHtml: ${cleanedHtml.length}자")
        val reductionRate = ((rawHtml.length - cleanedHtml.length) * 100.0 / rawHtml.length)
        println("감소율: ${String.format("%.1f", reductionRate)}%")
        println()

        // 4. cleanedHtml 샘플 출력
        println("=== cleanedHtml 앞부분 2000자 ===")
        println(cleanedHtml.take(2000))
        println("... (생략) ...\n")

        if (cleanedHtml.length > 17000) {
            println("=== cleanedHtml 중간부분 2000자 (위치: 15000-17000) ===")
            println(cleanedHtml.substring(15000, 17000))
            println("... (생략) ...\n")
        }

        // 5. 불필요한 키워드 검색
        println("=== 불필요한 텍스트 패턴 분석 ===")
        val navKeywords = listOf(
            "로그인", "회원가입", "기업서비스", "이용약관",
            "더보기", "펼치기", "스크랩", "공유하기",
            "조회수", "지원자", "북마크", "관심기업"
        )

        var totalOccurrences = 0
        navKeywords.forEach { keyword ->
            val count = cleanedHtml.split(keyword).size - 1
            if (count > 0) {
                println("  '$keyword' 출현: ${count}회")
                totalOccurrences += count
            }
        }
        println("총 불필요한 키워드 출현 횟수: $totalOccurrences")
        println()

        // 6. 채용 공고 본문 키워드 검색
        println("=== 채용 공고 본문 키워드 분석 ===")
        val jobKeywords = listOf("주요업무", "자격요건", "우대사항", "복지", "기술스택", "포지션", "담당업무")
        jobKeywords.forEach { keyword ->
            val index = cleanedHtml.indexOf(keyword)
            if (index >= 0) {
                println("  '$keyword' 발견 위치: ${index}자")
            } else {
                println("  '$keyword' 발견 못함")
            }
        }
        println()
    }

    @Test
    @Disabled("Requires external network access to wanted.co.kr")
    fun `compare 3 approaches for HTML cleaning`() {
        println("\n========================================")
        println("Phase 6D: 3가지 방안 비교 테스트")
        println("========================================\n")

        val url = "https://www.wanted.co.kr/wd/281357"
        val document = Jsoup.connect(url)
            .timeout(10000)
            .userAgent("Mozilla/5.0 (compatible; InterviewNoteBot/1.0)")
            .get()
        val rawHtml = document.html()

        // 현재 방식
        val currentCleaned = cleanHtmlViaReflection(rawHtml)

        // 방안 1: Jsoup text()
        val approach1 = document.body().text()

        // 방안 2: CSS Selector (추정)
        val approach2Body = document.clone()
        approach2Body.select("nav, header, footer, aside, script, style").remove()
        val approach2 = approach2Body.select("main, article").firstOrNull()?.text()
            ?: approach2Body.body().text()

        // 방안 3: Structured (추정)
        val approach3Body = document.clone()
        approach3Body.select("nav, header, footer, aside, script, style").remove()
        approach3Body.select("[class*='nav'], [class*='footer'], [class*='sidebar']").remove()
        val approach3 = approach3Body.body().text()

        // 비교
        println("=== 3가지 방안 크기 비교 ===")
        println("원본 HTML: ${rawHtml.length}자")
        println("현재 cleanHtml (regex): ${currentCleaned.length}자")
        println("방안1 (Jsoup text): ${approach1.length}자")
        println("방안2 (CSS Selector): ${approach2.length}자")
        println("방안3 (Structured): ${approach3.length}자")
        println()

        // 감소율 계산
        println("=== 감소율 비교 (원본 대비) ===")
        println("현재: ${String.format("%.1f", (rawHtml.length - currentCleaned.length) * 100.0 / rawHtml.length)}%")
        println("방안1: ${String.format("%.1f", (rawHtml.length - approach1.length) * 100.0 / rawHtml.length)}%")
        println("방안2: ${String.format("%.1f", (rawHtml.length - approach2.length) * 100.0 / rawHtml.length)}%")
        println("방안3: ${String.format("%.1f", (rawHtml.length - approach3.length) * 100.0 / rawHtml.length)}%")
        println()

        println("=== 방안1 (Jsoup text) 샘플 (앞 500자) ===")
        println(approach1.take(500))
        println("... (생략) ...\n")

        println("=== 방안2 (CSS Selector) 샘플 (앞 500자) ===")
        println(approach2.take(500))
        println("... (생략) ...\n")

        println("=== 방안3 (Structured) 샘플 (앞 500자) ===")
        println(approach3.take(500))
        println("... (생략) ...\n")

        // 불필요한 키워드 비교
        println("=== 불필요한 키워드 출현 비교 ===")
        val navKeywords = listOf("로그인", "회원가입", "스크랩", "더보기")

        println("현재 cleanHtml:")
        navKeywords.forEach { keyword ->
            val count = currentCleaned.split(keyword).size - 1
            if (count > 0) println("  '$keyword': ${count}회")
        }

        println("\n방안1 (Jsoup text):")
        navKeywords.forEach { keyword ->
            val count = approach1.split(keyword).size - 1
            if (count > 0) println("  '$keyword': ${count}회")
        }

        println("\n방안2 (CSS Selector):")
        navKeywords.forEach { keyword ->
            val count = approach2.split(keyword).size - 1
            if (count > 0) println("  '$keyword': ${count}회")
        }

        println("\n방안3 (Structured):")
        navKeywords.forEach { keyword ->
            val count = approach3.split(keyword).size - 1
            if (count > 0) println("  '$keyword': ${count}회")
        }
        println()
    }

    /**
     * 리플렉션을 사용하여 private cleanHtml() 메서드 호출
     */
    private fun cleanHtmlViaReflection(html: String): String {
        val method = JobPostingParserService::class.java
            .getDeclaredMethod("cleanHtml", String::class.java)
        method.isAccessible = true
        return method.invoke(jobPostingParserService, html) as String
    }
}
