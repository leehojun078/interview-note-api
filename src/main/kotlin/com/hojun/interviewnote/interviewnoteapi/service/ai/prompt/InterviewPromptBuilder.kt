package com.hojun.interviewnote.interviewnoteapi.service.ai.prompt

import org.springframework.stereotype.Service

/**
 * 면접 진행용 프롬프트 생성기
 *
 * Phase 2 리팩토링: PromptBuilder에서 분리
 * 역할: 실시간 대화형 면접 진행 프롬프트 생성
 */
@Service
class InterviewPromptBuilder {

    private companion object {
        private const val IT = "IT"
        private const val PLANNING = "PLANNING"
        private const val MARKETING = "MARKETING"
        private const val ACCOUNTING = "ACCOUNTING"
        private const val HR = "HR"
        private const val ADMIN = "ADMIN"
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
     * 면접 진행용 시스템 프롬프트 (직무 기반)
     *
     * Phase 7: 실시간 대화형 면접
     */
    fun buildInterviewSystemPrompt(jobField: String): String {
        val logicDesc = getLogicDescription(jobField)
        val specificityDesc = getSpecificityDescription(jobField)

        return """
            당신은 ${jobField} 분야의 면접관입니다.

            면접 진행 방식:
            1. 자연스러운 대화 형태로 질문 (반드시 200자 이내)
            2. 답변에 따라 꼬리 질문 (2-3번)
            3. 답변이 부족하면 힌트 제공
            4. 좋은 답변에는 긍정적 피드백 후 새 주제로 전환

            평가 기준:
            - 논리성(logic): $logicDesc (1-5점)
            - 구체성(specificity): $specificityDesc (1-5점)
            - 전달력(delivery): 명확하고 이해하기 쉬운 설명 (1-5점)

            중요:
            - 모든 질문은 반드시 200자 이내로 작성
            - 한국어로 대화
            - 면접관 역할에 충실 (과도한 친절함 금지)

            출력 형식 (JSON):
            {
              "evaluation": {
                "logicScore": 3,
                "specificityScore": 2,
                "deliveryScore": 4,
                "comment": "답변 평가 코멘트"
              },
              "nextAction": {
                "question": "다음 질문 (200자 이내)",
                "reasoning": "이 질문을 하는 이유",
                "isFollowUp": true
              }
            }
        """.trimIndent()
    }

    /**
     * 면접 진행용 시스템 프롬프트 (공고 기반)
     *
     * Phase 7: 채용 공고 맞춤형 면접
     */
    fun buildInterviewSystemPromptWithJobPosting(
        jobField: String,
        companyName: String,
        jobTitle: String,
        jobDescription: String,
        requiredSkills: String?,
        preferredSkills: String?
    ): String {
        val logicDesc = getLogicDescription(jobField)
        val specificityDesc = getSpecificityDescription(jobField)

        return """
            당신은 ${companyName}의 ${jobTitle} 면접관입니다.

            채용 공고 요구사항:
            - 필수 기술: ${requiredSkills ?: "명시되지 않음"}
            - 우대 기술: ${preferredSkills ?: "명시되지 않음"}
            - 직무 설명: ${jobDescription.take(500)}

            면접 진행 방식:
            1. 자연스러운 대화 형태로 질문 (반드시 200자 이내)
            2. 답변에 따라 꼬리 질문 (2-3번)
            3. **공고의 필수 기술을 중점적으로 질문**
            4. 좋은 답변에는 긍정적 피드백

            평가 기준:
            - 논리성(logic): $logicDesc (1-5점)
            - 구체성(specificity): $specificityDesc (1-5점)
            - 전달력(delivery): 명확하고 이해하기 쉬운 설명 (1-5점)

            중요:
            - 모든 질문은 반드시 200자 이내로 작성

            출력 형식 (JSON):
            {
              "evaluation": {
                "logicScore": 3,
                "specificityScore": 2,
                "deliveryScore": 4,
                "comment": "답변 평가 코멘트"
              },
              "nextAction": {
                "question": "다음 질문 (200자 이내)",
                "reasoning": "이 질문을 하는 이유",
                "isFollowUp": true
              }
            }
        """.trimIndent()
    }

    /**
     * 첫 질문 생성 프롬프트
     *
     * Phase 7: 면접 시작 시 자기소개 요청
     */
    fun buildFirstQuestionPrompt(
        jobField: String,
        companyName: String?,
        jobTitle: String?
    ): String {
        return if (companyName != null && jobTitle != null) {
            """
                ${companyName}의 ${jobTitle} 면접을 시작합니다.
                지원자에게 첫 질문으로 자기소개를 요청하세요.
                질문은 반드시 200자 이내로 작성하세요.

                참고: 지원자는 200자 이내로 답변하므로, 간결하고 핵심적인 자기소개를 요청하세요.
            """.trimIndent()
        } else {
            """
                ${jobField} 면접을 시작합니다.
                지원자에게 첫 질문으로 자기소개를 요청하세요.
                질문은 반드시 200자 이내로 작성하세요.

                참고: 지원자는 200자 이내로 답변하므로, 간결하고 핵심적인 자기소개를 요청하세요.
            """.trimIndent()
        }
    }

    /**
     * 꼬리 질문 생성 프롬프트
     *
     * Phase 7: 대화 히스토리 기반 다음 질문 생성
     */
    fun buildFollowUpPrompt(conversationHistory: String): String {
        return """
            지금까지의 대화:
            $conversationHistory

            지원자의 마지막 답변을 평가하고, 다음 질문을 생성하세요.
            - 답변이 부족하면 꼬리 질문
            - 답변이 충분하면 새로운 주제의 질문
            - 질문은 반드시 200자 이내

            출력 형식 (JSON):
            {
              "evaluation": {
                "logicScore": 3,
                "specificityScore": 2,
                "deliveryScore": 4,
                "comment": "답변 평가 코멘트"
              },
              "nextAction": {
                "question": "다음 질문 (200자 이내)",
                "reasoning": "이 질문을 하는 이유",
                "isFollowUp": true
              }
            }
        """.trimIndent()
    }

    /**
     * 직무별 논리성 평가 기준 설명
     */
    private fun getLogicDescription(jobField: String): String {
        return when (jobField) {
            IT -> "기술적 사고의 논리적 흐름과 일관성"
            PLANNING -> "전략적 사고와 분석적 접근"
            MARKETING -> "브랜드 이해와 캠페인 기획 논리"
            ACCOUNTING -> "재무/회계적 사고와 분석"
            HR -> "조직 이해와 인사 관리 논리"
            ADMIN -> "업무 효율성과 행정 절차 이해"
            DESIGN -> "디자인 의도와 창의적 사고"
            SALES -> "고객 이해와 영업 전략 논리"
            MD -> "상품 기획과 시장 분석 논리"
            SERVICE -> "고객 경험 이해와 문제 해결 논리"
            PRODUCTION -> "생산 효율과 품질 관리 논리"
            CONSTRUCTION -> "건설 기술과 안전 관리 논리"
            MEDICAL -> "의료 이해와 환자 케어 논리"
            EDUCATION -> "교육 철학과 학생 이해"
            MEDIA -> "콘텐츠 기획과 제작 논리"
            FINANCE -> "금융 상품 이해와 리스크 관리"
            PUBLIC -> "공공 서비스와 정책 이해"
            else -> "사고의 논리적 흐름과 일관성"
        }
    }

    /**
     * 직무별 구체성 평가 기준 설명
     */
    private fun getSpecificityDescription(jobField: String): String {
        return when (jobField) {
            IT -> "구체적 기술 스택, 사례, 수치 제시"
            PLANNING -> "시장 분석 데이터, 전략 수립 과정"
            MARKETING -> "캠페인 성과, 마케팅 지표"
            ACCOUNTING -> "재무제표 이해, 회계 규정 적용"
            HR -> "인사 제도, 채용 프로세스 경험"
            ADMIN -> "행정 업무, 문서 관리 사례"
            DESIGN -> "디자인 프로세스, 도구 활용"
            SALES -> "영업 실적, 고객 관리 사례"
            MD -> "상품 기획 사례, 트렌드 분석"
            SERVICE -> "고객 응대 사례, 클레임 해결"
            PRODUCTION -> "생산 공정, 품질 관리 지표"
            CONSTRUCTION -> "현장 관리, 안전 사고 예방"
            MEDICAL -> "의료 행위, 환자 케어 사례"
            EDUCATION -> "교수법, 학생 평가 방법"
            MEDIA -> "콘텐츠 제작 경험, 시청률 등"
            FINANCE -> "금융 상품 지식, 투자 전략"
            PUBLIC -> "공공 정책, 민원 처리 사례"
            else -> "구체적 사례와 수치 제시"
        }
    }
}
