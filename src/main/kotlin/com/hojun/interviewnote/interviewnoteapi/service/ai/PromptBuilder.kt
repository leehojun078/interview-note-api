package com.hojun.interviewnote.interviewnoteapi.service.ai

import com.hojun.interviewnote.interviewnoteapi.config.OpenAiProperties
import com.hojun.interviewnote.interviewnoteapi.domain.Question
import org.springframework.stereotype.Service

/**
 * AI 평가를 위한 프롬프트 생성기
 *
 * 직무(jobField)별로 다른 시스템 프롬프트를 생성하여
 * 평가 기준을 직무에 맞게 조정할 수 있습니다.
 *
 * MVP에서는 IT 직무만 지원하며, 향후 확장 가능한 구조로 설계되었습니다.
 */
@Service
class PromptBuilder(
    private val properties: OpenAiProperties
) {

    companion object {
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
    }

    /**
     * 직무에 따른 시스템 프롬프트 생성
     * Phase 5: 17개 직무 모두 지원
     */
    fun buildSystemPrompt(jobField: String, targetJob: String): String {
        return when (jobField) {
            IT -> buildItSystemPrompt(targetJob)
            PLANNING -> buildPlanningSystemPrompt(targetJob)
            MARKETING -> buildMarketingSystemPrompt(targetJob)
            ACCOUNTING -> buildAccountingSystemPrompt(targetJob)
            HR -> buildHrSystemPrompt(targetJob)
            ADMIN -> buildAdminSystemPrompt(targetJob)
            DESIGN -> buildDesignSystemPrompt(targetJob)
            SALES -> buildSalesSystemPrompt(targetJob)
            MD -> buildMdSystemPrompt(targetJob)
            SERVICE -> buildServiceSystemPrompt(targetJob)
            PRODUCTION -> buildProductionSystemPrompt(targetJob)
            CONSTRUCTION -> buildConstructionSystemPrompt(targetJob)
            MEDICAL -> buildMedicalSystemPrompt(targetJob)
            EDUCATION -> buildEducationSystemPrompt(targetJob)
            MEDIA -> buildMediaSystemPrompt(targetJob)
            FINANCE -> buildFinanceSystemPrompt(targetJob)
            PUBLIC -> buildPublicSystemPrompt(targetJob)
            else -> throw IllegalArgumentException("지원하지 않는 직무 분야입니다: $jobField")
        }
    }

    /**
     * 공통 프롬프트 구조 (중복 방지)
     */
    private fun buildBasePrompt(
        targetJob: String,
        logicDescription: String,
        specificityDescription: String,
        badExamples: List<String>
    ): String {
        val badExamplesText = badExamples.joinToString("\n            - ")
        return """
            당신은 ${targetJob} 면접을 준비하는 지원자를 돕는 면접 코치입니다.
            당신의 역할은 합격/불합격을 판정하는 것이 아니라, 답변을 개선하도록 구체적인 피드백을 제공하는 것입니다.

            평가 기준:
            - 논리성(logic): $logicDescription (1-5점)
            - 구체성(specificity): $specificityDescription (1-5점)
            - 직무 적합성(jobFit): 질문 의도와 직무 연관성 (1-5점)
            - 전달력(delivery): 명확하고 이해하기 쉽게 설명하는 능력 (1-5점)

            중요한 평가 지침:
            1. **정직한 평가**: 답변이 반복적이거나 무의미하면 솔직하게 지적하세요
            2. **사실 기반**: 답변에 없는 내용을 추측하거나 창작하지 마세요
            3. **강점 검증**: 실제로 답변에 나타난 강점만 언급하세요
            4. **반복 표현 감지**: 같은 단어/문구가 반복되면 improvements에 "반복 표현을 피하고 구체적인 경험과 예시를 들어 설명"을 포함하세요
            5. **내용 부족 시**: 답변이 짧거나 구체성이 부족하면 strengths를 억지로 만들지 말고, improvements를 더 구체적으로 작성하세요
            6. **엄격한 기준**: 형식적이거나 추상적인 답변은 낮은 점수를 주세요

            나쁜 답변 예시:
            - 반복 표현: "저는 중요하게 여기는 여기는 여기는..."
            - $badExamplesText

            이런 경우:
            - strengths: 가능한 한 적게 (또는 비어있어도 됨)
            - improvements: 구체적이고 실질적인 개선 방향 제시
            - 점수: 1-2점 (매우 낮게)

            출력 규칙:
            - 반드시 JSON 형식으로 응답
            - 각 점수는 1-5 사이 정수
            - strengths는 0-5개 (답변이 부실하면 0개도 가능)
            - improvements는 1-5개 필수 (최소 1개, 내용이 부실하면 많게)
            - modelAnswer는 400-600자 이내
            - 한국어로 답변
            - 과도한 단정이나 공격적 표현 금지

            JSON 형식:
            {
              "scores": {
                "logic": 4,
                "specificity": 3,
                "jobFit": 4,
                "delivery": 3
              },
              "strengths": ["강점1", "강점2"],
              "improvements": ["개선점1", "개선점2"],
              "modelAnswer": "모범답변 내용...",
              "overallComment": "종합 코멘트"
            }
        """.trimIndent()
    }

    /**
     * IT 직무용 시스템 프롬프트
     */
    private fun buildItSystemPrompt(targetJob: String): String {
        return buildBasePrompt(
            targetJob = targetJob,
            logicDescription = "기술적 사고의 논리적 흐름과 일관성",
            specificityDescription = "구체적 기술 스택, 사례, 수치 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"저는 열심히 노력했습니다\"",
                "구체성 부족: \"Spring을 사용했습니다\" (어떻게? 왜? 무엇을?)"
            )
        )
    }

    /**
     * 기획·전략 직무용 시스템 프롬프트
     */
    private fun buildPlanningSystemPrompt(targetJob: String): String {
        return buildBasePrompt(
            targetJob = targetJob,
            logicDescription = "전략적 사고와 기획 프로세스의 논리적 흐름",
            specificityDescription = "구체적 기획안, 성과 지표, 데이터 근거 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"전략을 수립했습니다\"",
                "구체성 부족: \"시장 조사를 했습니다\" (어떤 방법으로? 결과는?)"
            )
        )
    }

    /**
     * 마케팅·홍보·조사 직무용 시스템 프롬프트
     */
    private fun buildMarketingSystemPrompt(targetJob: String): String {
        return buildBasePrompt(
            targetJob = targetJob,
            logicDescription = "마케팅 전략과 캠페인 기획의 논리적 타당성",
            specificityDescription = "구체적 캠페인 성과, KPI, 타겟 분석 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"캠페인이 성공했습니다\"",
                "구체성 부족: \"SNS 마케팅을 했습니다\" (어떤 채널? 성과는?)"
            )
        )
    }

    /**
     * 회계·세무·재무 직무용 시스템 프롬프트
     */
    private fun buildAccountingSystemPrompt(targetJob: String): String {
        return buildBasePrompt(
            targetJob = targetJob,
            logicDescription = "재무 분석과 회계 처리의 논리적 정확성",
            specificityDescription = "구체적 재무 지표, 세무 지식, 실무 경험 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"재무제표를 분석했습니다\"",
                "구체성 부족: \"원가를 절감했습니다\" (얼마나? 어떤 방법으로?)"
            )
        )
    }

    /**
     * 인사·노무·HRD 직무용 시스템 프롬프트
     */
    private fun buildHrSystemPrompt(targetJob: String): String {
        return buildBasePrompt(
            targetJob = targetJob,
            logicDescription = "인사 전략과 조직 관리의 논리적 체계성",
            specificityDescription = "구체적 채용 프로세스, 교육 프로그램, 성과 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"인재를 육성했습니다\"",
                "구체성 부족: \"채용을 진행했습니다\" (몇 명? 어떤 직무? 결과는?)"
            )
        )
    }

    /**
     * 총무·법무·사무 직무용 시스템 프롬프트
     */
    private fun buildAdminSystemPrompt(targetJob: String): String {
        return buildBasePrompt(
            targetJob = targetJob,
            logicDescription = "업무 프로세스와 규정 준수의 논리적 체계성",
            specificityDescription = "구체적 사례, 절차, 문제 해결 경험 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"효율적으로 처리했습니다\"",
                "구체성 부족: \"계약서를 검토했습니다\" (어떤 내용? 결과는?)"
            )
        )
    }

    /**
     * 디자인 직무용 시스템 프롬프트
     */
    private fun buildDesignSystemPrompt(targetJob: String): String {
        return buildBasePrompt(
            targetJob = targetJob,
            logicDescription = "디자인 컨셉과 사용자 경험의 논리적 근거",
            specificityDescription = "구체적 프로젝트, 툴 사용 경험, 성과 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"디자인을 개선했습니다\"",
                "구체성 부족: \"Figma를 사용했습니다\" (어떤 작업? 결과는?)"
            )
        )
    }

    /**
     * 영업·판매·무역 직무용 시스템 프롬프트
     */
    private fun buildSalesSystemPrompt(targetJob: String): String {
        return buildBasePrompt(
            targetJob = targetJob,
            logicDescription = "영업 전략과 설득의 논리적 흐름",
            specificityDescription = "구체적 실적 수치, 고객 사례, 영업 방법 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"열심히 영업했습니다\"",
                "구체성 부족: \"고객이 만족했습니다\" (매출은? 어떤 방법으로?)"
            )
        )
    }

    /**
     * 상품기획·MD 직무용 시스템 프롬프트
     */
    private fun buildMdSystemPrompt(targetJob: String): String {
        return buildBasePrompt(
            targetJob = targetJob,
            logicDescription = "상품 기획과 시장 분석의 논리적 타당성",
            specificityDescription = "구체적 상품 사례, 매출 데이터, 트렌드 분석 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"상품을 기획했습니다\"",
                "구체성 부족: \"트렌드를 분석했습니다\" (어떤 데이터? 결과는?)"
            )
        )
    }

    /**
     * 서비스 직무용 시스템 프롬프트
     */
    private fun buildServiceSystemPrompt(targetJob: String): String {
        return buildBasePrompt(
            targetJob = targetJob,
            logicDescription = "고객 서비스와 문제 해결의 논리적 프로세스",
            specificityDescription = "구체적 고객 사례, 만족도 지표, 개선 경험 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"고객을 응대했습니다\"",
                "구체성 부족: \"불만을 처리했습니다\" (어떻게? 결과는?)"
            )
        )
    }

    /**
     * 생산 직무용 시스템 프롬프트
     */
    private fun buildProductionSystemPrompt(targetJob: String): String {
        return buildBasePrompt(
            targetJob = targetJob,
            logicDescription = "생산 프로세스와 품질 관리의 논리적 체계성",
            specificityDescription = "구체적 생산 지표, 공정 개선, 불량률 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"생산성을 향상시켰습니다\"",
                "구체성 부족: \"공정을 개선했습니다\" (어떻게? 얼마나?)"
            )
        )
    }

    /**
     * 건설·건축 직무용 시스템 프롬프트
     */
    private fun buildConstructionSystemPrompt(targetJob: String): String {
        return buildBasePrompt(
            targetJob = targetJob,
            logicDescription = "건설 프로젝트와 안전 관리의 논리적 체계성",
            specificityDescription = "구체적 프로젝트 규모, 공사 기간, 안전 성과 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"현장을 관리했습니다\"",
                "구체성 부족: \"공사를 진행했습니다\" (어떤 규모? 기간은?)"
            )
        )
    }

    /**
     * 의료 직무용 시스템 프롬프트
     */
    private fun buildMedicalSystemPrompt(targetJob: String): String {
        return buildBasePrompt(
            targetJob = targetJob,
            logicDescription = "의료 지식과 환자 케어의 논리적 정확성",
            specificityDescription = "구체적 진료 경험, 전문 지식, 환자 만족도 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"환자를 치료했습니다\"",
                "구체성 부족: \"의료 서비스를 제공했습니다\" (어떤 분야? 성과는?)"
            )
        )
    }

    /**
     * 교육 직무용 시스템 프롬프트
     */
    private fun buildEducationSystemPrompt(targetJob: String): String {
        return buildBasePrompt(
            targetJob = targetJob,
            logicDescription = "교육 방법론과 커리큘럼의 논리적 체계성",
            specificityDescription = "구체적 교육 프로그램, 학습 성과, 교수법 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"학생들을 가르쳤습니다\"",
                "구체성 부족: \"교육을 진행했습니다\" (무엇을? 결과는?)"
            )
        )
    }

    /**
     * 미디어·문화·스포츠 직무용 시스템 프롬프트
     */
    private fun buildMediaSystemPrompt(targetJob: String): String {
        return buildBasePrompt(
            targetJob = targetJob,
            logicDescription = "콘텐츠 기획과 제작의 논리적 창의성",
            specificityDescription = "구체적 프로젝트, 조회수/반응, 제작 과정 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"콘텐츠를 제작했습니다\"",
                "구체성 부족: \"기획을 했습니다\" (어떤 내용? 성과는?)"
            )
        )
    }

    /**
     * 금융·보험 직무용 시스템 프롬프트
     */
    private fun buildFinanceSystemPrompt(targetJob: String): String {
        return buildBasePrompt(
            targetJob = targetJob,
            logicDescription = "금융 상품과 리스크 분석의 논리적 정확성",
            specificityDescription = "구체적 상품 지식, 실적, 고객 사례 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"상품을 판매했습니다\"",
                "구체성 부족: \"고객을 상담했습니다\" (어떤 상품? 결과는?)"
            )
        )
    }

    /**
     * 공공·복지 직무용 시스템 프롬프트
     */
    private fun buildPublicSystemPrompt(targetJob: String): String {
        return buildBasePrompt(
            targetJob = targetJob,
            logicDescription = "공공 서비스와 정책 집행의 논리적 체계성",
            specificityDescription = "구체적 사업 사례, 수혜자 수, 성과 제시 정도",
            badExamples = listOf(
                "추상적 답변: \"업무를 처리했습니다\"",
                "구체성 부족: \"민원을 해결했습니다\" (어떤 방법? 결과는?)"
            )
        )
    }

    /**
     * 질문과 답변을 포함한 사용자 프롬프트 생성
     */
    fun buildUserPrompt(question: Question, answer: String): String {
        return """
            면접 질문:
            ${question.content}

            지원자 답변:
            $answer

            위 답변을 평가하고, JSON 형식으로 피드백을 제공해주세요.
        """.trimIndent()
    }
}
