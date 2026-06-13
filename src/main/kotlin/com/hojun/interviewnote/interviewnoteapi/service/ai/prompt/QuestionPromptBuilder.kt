package com.hojun.interviewnote.interviewnoteapi.service.ai.prompt

import org.springframework.stereotype.Service

/**
 * 질문 생성용 프롬프트 생성기
 *
 * Phase 2 리팩토링: PromptBuilder에서 분리
 * 역할: 채용 공고 기반 맞춤형 면접 질문 생성 프롬프트
 */
@Service
class QuestionPromptBuilder {

    private companion object {
        private const val IT = "IT"
        private const val SALES = "SALES"
        private const val MARKETING = "MARKETING"
        private const val PLANNING = "PLANNING"
        private const val ACCOUNTING = "ACCOUNTING"
        private const val HR = "HR"
        private const val ADMIN = "ADMIN"
        private const val DESIGN = "DESIGN"
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
     * 질문 생성용 시스템 프롬프트
     *
     * Phase 6B: 채용 공고 기반 맞춤형 질문 10개 생성
     *
     * @param jobField 직무 분야 (17개 중 하나)
     * @param companyName 회사명
     * @param jobTitle 포지션명
     * @return 시스템 프롬프트
     */
    fun buildQuestionGenerationSystemPrompt(
        jobField: String,
        companyName: String,
        jobTitle: String
    ): String {
        val categories = getCategoriesForJobField(jobField)
        val categoriesText = categories.joinToString(", ")

        return """
            당신은 ${companyName}의 ${jobTitle} 포지션 면접을 위한 질문을 생성하는 전문가입니다.

            채용 공고를 분석하여 실전 면접에서 나올 수 있는 **10개의 맞춤형 면접 질문**을 생성하세요.

            질문 생성 원칙:
            1. **공고 기반**: 필수 기술, 우대 기술, 직무 설명을 면밀히 반영
            2. **STAR 기법 유도**: 상황(Situation), 과제(Task), 행동(Action), 결과(Result)로 답변 가능한 질문
            3. **깊이 있는 기술 질문**: "사용해봤나요?"가 아닌 "어떻게 활용했고, 왜 선택했나요?" 수준
            4. **실무 중심**: 이론보다는 실제 경험과 문제 해결 능력 확인
            5. **트레이드오프 질문**: "왜 A가 아닌 B를 선택했나요?" 같은 판단력 검증

            카테고리 (반드시 다음 중 선택):
            - $categoriesText

            난이도 분포 (필수 - 정확히 지켜야 함):
            - EASY: 정확히 3문항 (기본 개념, 경험 유무, 간단한 기술 설명)
            - MEDIUM: 정확히 4문항 (심화 기술, 프로젝트 경험, 구체적 활용 사례)
            - HARD: 정확히 3문항 (트레이드오프, 설계 결정, 복잡한 문제 해결, 기술 선택 근거)

            중요: 반드시 EASY 3개 + MEDIUM 4개 + HARD 3개 = 총 10개를 생성하세요.

            출력 형식 (JSON):
            {
              "inferredJobField": "${jobField}",
              "questions": [
                {
                  "content": "Git을 사용한 경험이 있나요? 어떤 브랜치 전략을 사용했나요?",
                  "category": "기술역량",
                  "difficulty": "EASY",
                  "reasoning": "기본적인 협업 도구 사용 경험 확인"
                },
                {
                  "content": "RESTful API 설계 원칙을 설명해주세요.",
                  "category": "기술역량",
                  "difficulty": "EASY",
                  "reasoning": "기본 개념 이해도 확인"
                },
                {
                  "content": "팀 프로젝트에서 코드 리뷰를 진행한 경험이 있나요?",
                  "category": "협업경험",
                  "difficulty": "EASY",
                  "reasoning": "협업 프로세스 경험 유무 확인"
                },
                {
                  "content": "Kotlin의 코루틴을 실무에서 어떻게 활용했나요? 동기 방식 대비 어떤 이점이 있었나요?",
                  "category": "기술역량",
                  "difficulty": "MEDIUM",
                  "reasoning": "필수 기술의 실무 활용 경험과 이해도 확인"
                },
                {
                  "content": "데이터베이스 성능 최적화를 위해 어떤 방법들을 사용했나요?",
                  "category": "문제해결",
                  "difficulty": "MEDIUM",
                  "reasoning": "실무 문제 해결 경험 검증"
                },
                {
                  "content": "프로젝트에서 발생한 기술 부채를 어떻게 관리했나요?",
                  "category": "문제해결",
                  "difficulty": "MEDIUM",
                  "reasoning": "장기적 관점의 코드 품질 관리 능력 확인"
                },
                {
                  "content": "마이크로서비스 간 통신 방식을 어떻게 설계했나요?",
                  "category": "기술역량",
                  "difficulty": "MEDIUM",
                  "reasoning": "아키텍처 설계 경험 검증"
                },
                {
                  "content": "Spring Boot와 다른 프레임워크를 비교했을 때, 왜 Spring Boot를 선택했나요?",
                  "category": "기술역량",
                  "difficulty": "HARD",
                  "reasoning": "기술 선택의 근거와 트레이드오프 이해도 확인"
                },
                {
                  "content": "대용량 트래픽 상황에서 발생한 문제와 해결 과정을 설명해주세요.",
                  "category": "문제해결",
                  "difficulty": "HARD",
                  "reasoning": "복잡한 실전 문제 해결 능력 검증"
                },
                {
                  "content": "분산 시스템 환경에서 데이터 일관성을 어떻게 보장했나요?",
                  "category": "기술역량",
                  "difficulty": "HARD",
                  "reasoning": "고급 기술 개념과 설계 능력 검증"
                }
              ]
            }

            중요:
            - 반드시 10개 질문 생성
            - **난이도 분포 필수 준수**: EASY 3개, MEDIUM 4개, HARD 3개 (정확히 이 개수대로)
            - 각 질문은 100-200자 내외
            - reasoning은 50-200자 내외로 명확하게
            - category는 위 목록에서만 선택
            - difficulty는 EASY, MEDIUM, HARD만 사용 (대소문자 정확히)
        """.trimIndent()
    }

    /**
     * 질문 생성용 사용자 프롬프트
     *
     * Phase 6B: 채용 공고 내용을 전달
     *
     * @param jobDescription 공고 전문 (직무 설명, 자격 요건)
     * @param requiredSkills 필수 기술 스택 (JSON 배열)
     * @param preferredSkills 우대 기술 스택 (JSON 배열)
     * @return 사용자 프롬프트
     */
    fun buildQuestionGenerationUserPrompt(
        jobDescription: String,
        requiredSkills: String?,
        preferredSkills: String?
    ): String {
        val skillsSection = buildSkillsSection(requiredSkills, preferredSkills)

        return """
            다음 채용 공고를 기반으로 면접 질문 10개를 생성해주세요:

            $skillsSection

            직무 설명 및 자격 요건:
            $jobDescription

            위 내용을 반영하여 실전 면접 질문 10개를 JSON 형식으로 생성해주세요.
        """.trimIndent()
    }

    /**
     * 직무별 카테고리 목록 반환
     *
     * Phase 6B: 질문 생성 시 카테고리 제약 조건
     *
     * @param jobField 직무 분야 (예: "IT", "SALES")
     * @return 해당 직무의 카테고리 목록
     */
    fun getCategoriesForJobField(jobField: String): List<String> {
        return when (jobField) {
            IT -> listOf("기술역량", "문제해결", "협업경험")
            SALES -> listOf("고객관리", "실적달성", "협상스킬")
            MARKETING -> listOf("캠페인기획", "데이터분석", "콘텐츠전략")
            PLANNING -> listOf("전략수립", "시장분석", "프로젝트관리")
            ACCOUNTING -> listOf("재무분석", "세무지식", "리스크관리")
            HR -> listOf("채용관리", "인사제도", "조직문화")
            ADMIN -> listOf("업무관리", "문서작성", "커뮤니케이션")
            DESIGN -> listOf("디자인역량", "프로젝트경험", "협업스킬")
            MD -> listOf("상품기획", "트렌드분석", "협상력")
            SERVICE -> listOf("고객응대", "문제해결", "서비스마인드")
            PRODUCTION -> listOf("공정관리", "품질관리", "안전관리")
            CONSTRUCTION -> listOf("현장관리", "기술이해", "안전의식")
            MEDICAL -> listOf("전문지식", "환자케어", "윤리의식")
            EDUCATION -> listOf("교육역량", "학생이해", "교육열정")
            MEDIA -> listOf("콘텐츠제작", "창의성", "협업능력")
            FINANCE -> listOf("상품지식", "영업력", "리스크관리")
            PUBLIC -> listOf("공공서비스", "정책이해", "민원대응")
            else -> listOf("직무역량", "문제해결", "협업능력")
        }
    }

    /**
     * 기술 스택 섹션 생성 (있는 경우에만)
     */
    private fun buildSkillsSection(requiredSkills: String?, preferredSkills: String?): String {
        val sections = mutableListOf<String>()

        if (!requiredSkills.isNullOrBlank()) {
            sections.add("필수 기술: $requiredSkills")
        }
        if (!preferredSkills.isNullOrBlank()) {
            sections.add("우대 기술: $preferredSkills")
        }

        return if (sections.isNotEmpty()) {
            sections.joinToString("\n")
        } else {
            "기술 스택: 명시되지 않음"
        }
    }
}
