package com.hojun.interviewnote.interviewnoteapi.service.ai.prompt

/**
 * 직무별 프롬프트 설정 데이터 클래스
 *
 * Phase 2 리팩토링: PromptBuilder에서 분리
 */
data class JobFieldPromptConfig(
    val logicDescription: String,
    val specificityDescription: String,
    val badExamples: List<String>
)

/**
 * 직무별 피드백 프롬프트 설정 Map
 */
object JobFieldConfigs {
    private const val PLANNING = "PLANNING"
    private const val MARKETING = "MARKETING"
    private const val ACCOUNTING = "ACCOUNTING"
    private const val HR = "HR"
    private const val ADMIN = "ADMIN"
    private const val IT = "IT"
    private const val DESIGN = "DESIGN"
    private const val SALES = "SALES"
    private const val MD = "MD"
    private const val SERVICE = "SERVICE"
    private const val PRODUCTION = "PRODUCTION"
    private const val CONSTRUCTION = "CONSTRUCTION"
    private const val MEDICAL = "MEDICAL"
    private const val EDUCATION = "EDUCATION"
    private const val MEDIA = "MEDIA"
    private const val FINANCE = "FINANCE"
    private const val PUBLIC = "PUBLIC"

    val feedbackConfigs = mapOf(
        IT to JobFieldPromptConfig(
            logicDescription = "기술적 사고의 논리적 흐름과 일관성",
            specificityDescription = "구체적 기술 스택, 사례, 수치 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"저는 열심히 노력했습니다\"",
                "구체성 부족: \"Spring을 사용했습니다\" (어떻게? 왜? 무엇을?)"
            )
        ),
        PLANNING to JobFieldPromptConfig(
            logicDescription = "전략적 사고와 기획 프로세스의 논리적 흐름",
            specificityDescription = "구체적 기획안, 성과 지표, 데이터 근거 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"전략을 수립했습니다\"",
                "구체성 부족: \"시장 조사를 했습니다\" (어떤 방법으로? 결과는?)"
            )
        ),
        MARKETING to JobFieldPromptConfig(
            logicDescription = "마케팅 전략과 캠페인 기획의 논리적 타당성",
            specificityDescription = "구체적 캠페인 성과, KPI, 타겟 분석 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"캠페인이 성공했습니다\"",
                "구체성 부족: \"SNS 마케팅을 했습니다\" (어떤 채널? 성과는?)"
            )
        ),
        ACCOUNTING to JobFieldPromptConfig(
            logicDescription = "재무 분석과 회계 처리의 논리적 정확성",
            specificityDescription = "구체적 재무 지표, 세무 지식, 실무 경험 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"재무제표를 분석했습니다\"",
                "구체성 부족: \"원가를 절감했습니다\" (얼마나? 어떤 방법으로?)"
            )
        ),
        HR to JobFieldPromptConfig(
            logicDescription = "인사 전략과 조직 관리의 논리적 체계성",
            specificityDescription = "구체적 채용 프로세스, 교육 프로그램, 성과 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"인재를 육성했습니다\"",
                "구체성 부족: \"채용을 진행했습니다\" (몇 명? 어떤 직무? 결과는?)"
            )
        ),
        ADMIN to JobFieldPromptConfig(
            logicDescription = "업무 프로세스와 규정 준수의 논리적 체계성",
            specificityDescription = "구체적 사례, 절차, 문제 해결 경험 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"효율적으로 처리했습니다\"",
                "구체성 부족: \"계약서를 검토했습니다\" (어떤 내용? 결과는?)"
            )
        ),
        DESIGN to JobFieldPromptConfig(
            logicDescription = "디자인 컨셉과 사용자 경험의 논리적 근거",
            specificityDescription = "구체적 프로젝트, 툴 사용 경험, 성과 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"디자인을 개선했습니다\"",
                "구체성 부족: \"Figma를 사용했습니다\" (어떤 작업? 결과는?)"
            )
        ),
        SALES to JobFieldPromptConfig(
            logicDescription = "영업 전략과 설득의 논리적 흐름",
            specificityDescription = "구체적 실적 수치, 고객 사례, 영업 방법 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"열심히 영업했습니다\"",
                "구체성 부족: \"고객이 만족했습니다\" (매출은? 어떤 방법으로?)"
            )
        ),
        MD to JobFieldPromptConfig(
            logicDescription = "상품 기획과 시장 분석의 논리적 타당성",
            specificityDescription = "구체적 상품 사례, 매출 데이터, 트렌드 분석 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"상품을 기획했습니다\"",
                "구체성 부족: \"트렌드를 분석했습니다\" (어떤 데이터? 결과는?)"
            )
        ),
        SERVICE to JobFieldPromptConfig(
            logicDescription = "고객 서비스와 문제 해결의 논리적 프로세스",
            specificityDescription = "구체적 고객 사례, 만족도 지표, 개선 경험 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"고객을 응대했습니다\"",
                "구체성 부족: \"불만을 처리했습니다\" (어떻게? 결과는?)"
            )
        ),
        PRODUCTION to JobFieldPromptConfig(
            logicDescription = "생산 프로세스와 품질 관리의 논리적 체계성",
            specificityDescription = "구체적 생산 지표, 공정 개선, 불량률 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"생산성을 향상시켰습니다\"",
                "구체성 부족: \"공정을 개선했습니다\" (어떻게? 얼마나?)"
            )
        ),
        CONSTRUCTION to JobFieldPromptConfig(
            logicDescription = "건설 프로젝트와 안전 관리의 논리적 체계성",
            specificityDescription = "구체적 프로젝트 규모, 공사 기간, 안전 성과 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"현장을 관리했습니다\"",
                "구체성 부족: \"공사를 진행했습니다\" (어떤 규모? 기간은?)"
            )
        ),
        MEDICAL to JobFieldPromptConfig(
            logicDescription = "의료 지식과 환자 케어의 논리적 정확성",
            specificityDescription = "구체적 진료 경험, 전문 지식, 환자 만족도 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"환자를 치료했습니다\"",
                "구체성 부족: \"의료 서비스를 제공했습니다\" (어떤 분야? 성과는?)"
            )
        ),
        EDUCATION to JobFieldPromptConfig(
            logicDescription = "교육 방법론과 커리큘럼의 논리적 체계성",
            specificityDescription = "구체적 교육 프로그램, 학습 성과, 교수법 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"학생들을 가르쳤습니다\"",
                "구체성 부족: \"교육을 진행했습니다\" (무엇을? 결과는?)"
            )
        ),
        MEDIA to JobFieldPromptConfig(
            logicDescription = "콘텐츠 기획과 제작의 논리적 창의성",
            specificityDescription = "구체적 프로젝트, 조회수/반응, 제작 과정 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"콘텐츠를 제작했습니다\"",
                "구체성 부족: \"기획을 했습니다\" (어떤 내용? 성과는?)"
            )
        ),
        FINANCE to JobFieldPromptConfig(
            logicDescription = "금융 상품과 리스크 분석의 논리적 정확성",
            specificityDescription = "구체적 상품 지식, 실적, 고객 사례 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"상품을 판매했습니다\"",
                "구체성 부족: \"고객을 상담했습니다\" (어떤 상품? 결과는?)"
            )
        ),
        PUBLIC to JobFieldPromptConfig(
            logicDescription = "공공 서비스와 정책 집행의 논리적 체계성",
            specificityDescription = "구체적 사업 사례, 수혜자 수, 성과 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"업무를 처리했습니다\"",
                "구체성 부족: \"민원을 해결했습니다\" (어떤 방법? 결과는?)"
            )
        )
    )
}
