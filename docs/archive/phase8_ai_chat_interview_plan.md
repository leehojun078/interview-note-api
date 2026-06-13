# Phase 8: AI 채팅 면접 개선 - 상세 실행 계획

**작성일**: 2026-05-04
**대상 버전**: 0.8.0
**대상 브랜치**: `feat/interview-improvement`
**관련 문서**:
- [PRD](/Users/hojun/.claude/plans/linear-drifting-hummingbird.md)
- [PHASE8_AI_CHAT_INTERVIEW_IMPROVEMENTS.md](PHASE8_AI_CHAT_INTERVIEW_IMPROVEMENTS.md)

---

## 목차

1. [Phase 8A: 점수 계산 및 피드백 개선](#phase-8a-점수-계산-및-피드백-개선)
2. [Phase 8B: 경력 수준 및 UI 개선](#phase-8b-경력-수준-및-ui-개선)
3. [Phase 8C: 리뷰 통합 및 재개 기능](#phase-8c-리뷰-통합-및-재개-기능)
4. [Phase 8D: 테스트 및 문서화](#phase-8d-테스트-및-문서화)

---

## Phase 8A: 점수 계산 및 피드백 개선

**예상 기간**: 3-4일
**우선순위**: 최우선 🔴

### 목표

- ✅ 짧은 답변(50자 미만) 평균 점수 < 2.0점
- ✅ 종합 피드백 평균 900자 이상
- ✅ 강점/개선점 개수 0-5개 유연화

---

### Step 1: Database Migration 작성

**파일**: `src/main/resources/db/migration/V13__enhance_mock_interview_scoring.sql`

```sql
-- Phase 8A: 종합 평가 점수 계산 개선
-- Author: Claude + Hojun
-- Date: 2026-05-04

-- 경력 수준 컬럼 추가 (nullable, 기본값 ENTRY)
ALTER TABLE mock_interviews
ADD COLUMN career_level VARCHAR(20);

-- 기존 레코드는 ENTRY로 설정
UPDATE mock_interviews
SET career_level = 'ENTRY'
WHERE career_level IS NULL;

COMMENT ON COLUMN mock_interviews.career_level IS
'경력 수준: ENTRY(신입), JUNIOR(주니어), SENIOR(시니어), SENIOR_PLUS(시니어+)';

-- 가중 평균 점수 컬럼 추가
ALTER TABLE mock_interviews
ADD COLUMN weighted_average_score DOUBLE PRECISION;

COMMENT ON COLUMN mock_interviews.weighted_average_score IS
'가중 평균 점수 (첫 답변 제외, 저품질 답변 50% 이상 시 패널티)';

-- 점수 계산 방법 컬럼 추가
ALTER TABLE mock_interviews
ADD COLUMN score_calculation_method VARCHAR(50) DEFAULT 'WEIGHTED_AVERAGE';

COMMENT ON COLUMN mock_interviews.score_calculation_method IS
'점수 계산 방법: SIMPLE_AVERAGE(기존), WEIGHTED_AVERAGE(신규)';
```

**검증**:
```bash
./gradlew flywayMigrate
./gradlew flywayValidate
```

---

### Step 2: MockInterview 엔티티 수정

**파일**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/domain/MockInterview.kt`

**변경 사항**:

```kotlin
@Entity
class MockInterview(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val userId: Long,

    val jobPostingId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val selectedJobField: JobField,

    // ===== Phase 8A: 경력 수준 추가 =====
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var careerLevel: CareerLevel? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: MockInterviewStatus = MockInterviewStatus.IN_PROGRESS,

    @Column(nullable = false)
    val startedAt: LocalDateTime = LocalDateTime.now(),
    var endedAt: LocalDateTime? = null,

    // ===== 종합 평가 필드 =====
    @Column(columnDefinition = "TEXT")
    var overallFeedback: String? = null,

    @Column(columnDefinition = "TEXT")
    var keyStrengths: String? = null,

    @Column(columnDefinition = "TEXT")
    var keyImprovements: String? = null,

    var averageScore: Double? = null,

    // ===== Phase 8A: 가중 평균 점수 추가 =====
    var weightedAverageScore: Double? = null,

    @Column(length = 50)
    var scoreCalculationMethod: String = "WEIGHTED_AVERAGE",

    @Column(columnDefinition = "TEXT")
    var recommendation: String? = null,

    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    // ===== Phase 8A: 종합 평가 complete() 메서드 수정 =====
    fun complete(
        overallFeedback: String,
        keyStrengths: String,
        keyImprovements: String,
        averageScore: Double,
        weightedAverageScore: Double,  // 새로 추가
        recommendation: String
    ) {
        require(status == MockInterviewStatus.IN_PROGRESS) {
            "진행 중인 면접만 완료할 수 있습니다: status=$status"
        }

        this.status = MockInterviewStatus.COMPLETED
        this.endedAt = LocalDateTime.now()
        this.overallFeedback = overallFeedback
        this.keyStrengths = keyStrengths
        this.keyImprovements = keyImprovements
        this.averageScore = averageScore
        this.weightedAverageScore = weightedAverageScore  // 새로 추가
        this.recommendation = recommendation
    }

    // ===== Phase 8C: 면접 재개 메서드 (나중에 추가) =====
    fun resume() {
        require(status == MockInterviewStatus.COMPLETED) {
            "완료된 면접만 재개 가능합니다: status=$status"
        }
        this.status = MockInterviewStatus.IN_PROGRESS
        this.endedAt = null
    }

    fun abort() {
        require(status == MockInterviewStatus.IN_PROGRESS) {
            "진행 중인 면접만 중단할 수 있습니다: status=$status"
        }
        this.status = MockInterviewStatus.ABORTED
        this.endedAt = LocalDateTime.now()
    }
}
```

---

### Step 3: InterviewAiService - 가중 평균 점수 계산 로직 추가

**파일**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/service/InterviewAiService.kt`

**새로운 메서드 추가**:

```kotlin
/**
 * 가중 평균 점수 계산
 *
 * Phase 8A: 첫 답변(자기소개) 제외, 저품질 답변 비율 패널티
 *
 * @param messages 전체 메시지 목록 (AI + USER)
 * @return 가중 평균 점수 (1.0-5.0)
 */
private fun calculateWeightedScore(messages: List<InterviewMessage>): Double {
    val userAnswers = messages
        .filter { it.sender == MessageSender.USER && it.averageScore != null }
        .sortedBy { it.messageIndex }

    // 엣지 케이스: 자기소개만 있는 경우
    if (userAnswers.size < 2) {
        return userAnswers.firstOrNull()?.averageScore ?: 1.0
    }

    // 첫 답변(자기소개) 제외
    val answersExceptFirst = userAnswers.drop(1)

    if (answersExceptFirst.isEmpty()) {
        return userAnswers.first().averageScore ?: 1.0
    }

    // 평균 점수 계산
    val avg = answersExceptFirst.mapNotNull { it.averageScore }.average()

    // 저품질 답변 비율 체크
    val lowQualityRatio = answersExceptFirst.count {
        (it.averageScore ?: 5.0) < 2.0
    }.toDouble() / answersExceptFirst.size

    // 저품질 답변 50% 이상 시 패널티
    return if (lowQualityRatio >= 0.5) {
        (avg * 0.8).coerceAtLeast(1.0)
    } else {
        avg
    }
}
```

**기존 메서드 수정**: `generateFinalEvaluation()`

```kotlin
fun generateFinalEvaluation(
    interview: MockInterview,
    conversation: List<InterviewMessage>,
    jobPosting: JobPosting?
): FinalEvaluationResult {
    logger.info("종합 평가 생성 시작 - interviewId: ${interview.id}")

    // 1. 프롬프트 구성
    val systemPrompt = if (jobPosting != null) {
        promptBuilder.buildFinalEvaluationPromptWithJobPosting(
            jobField = interview.selectedJobField.name,
            careerLevel = interview.careerLevel,  // Phase 8A: 경력 수준 추가
            jobPosting = jobPosting
        )
    } else {
        promptBuilder.buildFinalEvaluationPrompt(
            jobField = interview.selectedJobField.name,
            careerLevel = interview.careerLevel  // Phase 8A: 경력 수준 추가
        )
    }

    // 2. 대화 히스토리 포맷
    val conversationHistory = conversation
        .sortedBy { it.createdAt }
        .joinToString("\n\n") { msg ->
            val sender = if (msg.sender == MessageSender.AI) "면접관" else "지원자"
            "$sender: ${msg.content}"
        }

    val allUserAnswers = conversation
        .filter { it.sender == MessageSender.USER }
        .mapIndexed { idx, msg ->
            "답변 ${idx + 1}: ${msg.content}"
        }
        .joinToString("\n")

    val userPrompt = """
        === 면접 대화 히스토리 ===
        $conversationHistory

        === 지원자 답변 요약 ===
        $allUserAnswers

        위 면접 내용을 바탕으로 종합 평가를 작성해주세요.
    """.trimIndent()

    // 3. AI 호출
    val rawResponse = aiClient.requestFeedback(systemPrompt, userPrompt)

    // 4. 응답 파싱
    val result = interviewResponseParser.parseFinalEvaluation(rawResponse)

    logger.info("종합 평가 생성 완료 - interviewId: ${interview.id}, " +
                "averageScore: ${result.averageScore}, " +
                "strengths: ${result.keyStrengths.split(",").size}, " +
                "improvements: ${result.keyImprovements.split(",").size}")

    return result
}
```

---

### Step 4: PromptBuilder - 엄격한 평가 프롬프트 수정

**파일**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/service/ai/PromptBuilder.kt`

**메서드 수정**: `buildFinalEvaluationPrompt()`

```kotlin
/**
 * 종합 평가 프롬프트 생성
 * Phase 8A: 엄격한 평가 기준, 800-1200자 피드백, 0-5개 유연한 강점/개선점
 */
fun buildFinalEvaluationPrompt(
    jobField: String,
    careerLevel: CareerLevel? = null
): String {
    val jobFieldDisplayName = JobField.valueOf(jobField).displayName
    val careerGuidance = getCareerLevelGuidance(careerLevel)

    return """
        당신은 ${jobFieldDisplayName} 분야의 면접 평가 전문가입니다.

        경력 수준 고려:
        $careerGuidance

        === 종합 평가 작성 지침 ===

        1. **overallFeedback** (800-1200자):
           - 첫 단락(200자): 전반적 인상 및 핵심 평가
           - 둘째 단락(300자): 구체적 강점 및 사례 분석
           - 셋째 단락(300자): 개선이 필요한 부분 및 구체적 제안
           - 마지막 단락(200자): 격려 및 다음 단계 조언

        2. **keyStrengths** (0-5개, 품질에 따라):
           - 평균 점수 4.0 이상: 4-5개
           - 평균 점수 3.0-3.9: 2-3개
           - 평균 점수 2.0-2.9: 1-2개
           - 평균 점수 2.0 미만: 0-1개
           - **중요**: 억지로 강점을 만들지 마세요. 실제로 두드러진 강점만 작성

        3. **keyImprovements** (1-5개, 최소 1개):
           - 평균 점수 4.0 이상: 1-2개
           - 평균 점수 3.0-3.9: 2-3개
           - 평균 점수 2.0-2.9: 3-4개
           - 평균 점수 2.0 미만: 4-5개
           - **중요**: 구체적이고 실질적인 개선 방향 제시

        4. **averageScore** (1.0-5.0):
           - 각 답변의 평균 점수를 전체 평균
           - **주의**: 첫 답변(자기소개)는 관대하게 평가, 나머지는 엄격하게

        5. **recommendation**:
           - "합격 추천 + 근거"
           - "보류 + 근거"
           - "불합격 + 근거"

        === 엄격한 평가 기준 (중요!) ===

        **짧은 답변 처리**:
        - 20자 미만: "답변이 너무 짧습니다" 코멘트, 모든 점수 1점
        - 20-50자: "좀 더 구체적으로 설명해주세요" 코멘트, 구체성 1-2점
        - 50자 이상: 정상 평가

        **자기소개 예외** (첫 답변만):
        - 첫 답변(자기소개)은 50자 이상이면 관대하게 평가 (3-4점)
        - 이름, 경력, 관심사만 언급해도 괜찮음

        **나쁜 답변 예시와 점수**:
        - "안녕하세요" (10자) → logic: 1, specificity: 1, delivery: 1
        - "열심히 노력했습니다" (추상적) → logic: 2, specificity: 1, delivery: 2
        - "여기는 여기는 여기는..." (반복) → logic: 1, specificity: 1, delivery: 1
        - "ㅎㅎ", "모르겠어요" → logic: 1, specificity: 1, delivery: 1

        **좋은 답변 예시와 점수**:
        - 구체적 사례 + 수치 + 기술 스택 언급 → logic: 4-5, specificity: 4-5
        - STAR 기법 활용 → logic: 4-5, delivery: 4-5
        - 명확한 구조 + 깊이 있는 분석 → 모든 항목 4-5점

        === 출력 형식 (JSON) ===
        {
          "overallFeedback": "종합 피드백 (800-1200자)",
          "keyStrengths": ["강점1", "강점2", ...],  // 0-5개
          "keyImprovements": ["개선점1", "개선점2", ...],  // 1-5개
          "averageScore": 3.2,
          "recommendation": "보류 - 기술적 깊이를 더하면 합격 가능"
        }

        **중요**: 반드시 JSON 형식으로만 응답하세요. 다른 텍스트는 포함하지 마세요.
    """.trimIndent()
}
```

**새로운 메서드 추가**: `getCareerLevelGuidance()`

```kotlin
/**
 * 경력 수준별 평가 가이드
 * Phase 8A: 경력 수준에 따른 기대 수준 명시
 */
private fun getCareerLevelGuidance(careerLevel: CareerLevel?): String {
    return when (careerLevel) {
        CareerLevel.ENTRY -> """
            - 신입 수준 (경험 1년 미만)
            - 기대: 기본 개념 이해, 학습 경험, 프로젝트 경험
            - 평가: 이론보다는 "경험해봤는지", "어떻게 학습했는지" 중심
            - 실무 경험 없어도 괜찮으나, 구체적 학습 사례는 필요
        """.trimIndent()

        CareerLevel.JUNIOR -> """
            - 주니어 수준 (경험 1-3년)
            - 기대: 실무 경험, 기술 활용 사례, 협업 경험
            - 평가: "어떻게 사용했는지", "왜 선택했는지" 중심
            - 1-2년 실무 경험 가정, 기본 기술 스택 활용 사례 필요
        """.trimIndent()

        CareerLevel.SENIOR -> """
            - 시니어 수준 (경험 3-7년)
            - 기대: 아키텍처 설계, 기술 리드, 문제 해결
            - 평가: "어떻게 설계했는지", "트레이드오프는?" 중심
            - 3-5년 실무 경험 가정, 심화 기술 질문에 답변 필요
        """.trimIndent()

        CareerLevel.SENIOR_PLUS -> """
            - 시니어+ 수준 (경험 7년 이상)
            - 기대: 기술 전략, 조직 리더십, 복잡한 설계
            - 평가: "어떻게 기술 결정을 내렸는지", "팀을 어떻게 이끌었는지" 중심
            - 7년 이상 경험 가정, 매우 심화된 기술 및 리더십 질문
        """.trimIndent()

        null -> """
            - 기본 수준 (경력 수준 미지정)
            - 기대: 중간 난이도 질문, 일반적 실무 경험
        """.trimIndent()
    }
}
```

---

### Step 5: InterviewResponseParser - 검증 로직 수정

**파일**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/service/ai/InterviewResponseParser.kt`

**메서드 수정**: `parseFinalEvaluation()`

```kotlin
/**
 * 종합 평가 응답 파싱
 * Phase 8A: 800자 검증, 0-5개 유연한 검증
 */
fun parseFinalEvaluation(rawResponse: String): FinalEvaluationResult {
    try {
        val json = objectMapper.readTree(rawResponse)

        // 1. overallFeedback 파싱 및 검증
        val overallFeedback = json["overallFeedback"]?.asText()
            ?: throw IllegalArgumentException("overallFeedback 필드 누락")

        // Phase 8A: 최소 500자 요구 (권장 800-1200자)
        if (overallFeedback.length < 500) {
            logger.warn("종합 피드백 길이 부족: ${overallFeedback.length}자 (권장: 800-1200자)")
        }

        if (overallFeedback.length < 300) {
            throw IllegalArgumentException(
                "종합 피드백이 너무 짧습니다: ${overallFeedback.length}자 (최소 300자)"
            )
        }

        // 2. keyStrengths 파싱 및 검증
        val keyStrengths = json["keyStrengths"]?.map { it.asText() }
            ?: throw IllegalArgumentException("keyStrengths 필드 누락")

        // Phase 8A: 0-5개 허용
        require(keyStrengths.size in 0..5) {
            "keyStrengths 개수 오류: ${keyStrengths.size}개 (0-5개 허용)"
        }

        // 3. keyImprovements 파싱 및 검증
        val keyImprovements = json["keyImprovements"]?.map { it.asText() }
            ?: throw IllegalArgumentException("keyImprovements 필드 누락")

        // Phase 8A: 1-5개 허용 (최소 1개)
        require(keyImprovements.size in 1..5) {
            "keyImprovements 개수 오류: ${keyImprovements.size}개 (1-5개 필요)"
        }

        // 4. averageScore 파싱 및 검증
        val averageScore = json["averageScore"]?.asDouble()
            ?: throw IllegalArgumentException("averageScore 필드 누락")

        require(averageScore in 1.0..5.0) {
            "averageScore 범위 오류: $averageScore (1.0-5.0 허용)"
        }

        // 5. recommendation 파싱
        val recommendation = json["recommendation"]?.asText()
            ?: throw IllegalArgumentException("recommendation 필드 누락")

        return FinalEvaluationResult(
            overallFeedback = overallFeedback,
            keyStrengths = objectMapper.writeValueAsString(keyStrengths),
            keyImprovements = objectMapper.writeValueAsString(keyImprovements),
            averageScore = averageScore,
            recommendation = recommendation
        )

    } catch (e: Exception) {
        logger.error("종합 평가 응답 파싱 실패: ${e.message}", e)
        logger.debug("원본 응답: $rawResponse")
        throw IllegalArgumentException("AI 응답 파싱 실패: ${e.message}", e)
    }
}
```

---

### Step 6: MockInterviewService - complete() 호출 수정

**파일**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/service/MockInterviewService.kt`

**메서드 수정**: `endInterview()`

```kotlin
fun endInterview(interviewId: Long, userId: Long): MockInterview {
    val interview = validateAndGetInterview(interviewId, userId)
    val messages = interviewMessageRepository
        .findByMockInterviewIdOrderByCreatedAtAsc(interviewId)

    val jobPosting = interview.jobPostingId?.let { id ->
        jobPostingRepository.findById(id).orElse(null)
    }

    // Phase 8A: 종합 평가 생성 (AI 호출)
    val evaluation = interviewAiService.generateFinalEvaluation(
        interview = interview,
        conversation = messages,
        jobPosting = jobPosting
    )

    // Phase 8A: 가중 평균 점수 계산 (별도 로직)
    val weightedScore = interviewAiService.calculateWeightedScore(messages)

    // Phase 8A: complete() 메서드에 weightedAverageScore 전달
    interview.complete(
        overallFeedback = evaluation.overallFeedback,
        keyStrengths = evaluation.keyStrengths,
        keyImprovements = evaluation.keyImprovements,
        averageScore = evaluation.averageScore,
        weightedAverageScore = weightedScore,  // 새로 추가
        recommendation = evaluation.recommendation
    )

    val saved = mockInterviewRepository.save(interview)

    logger.info("면접 종료 완료 - interviewId: $interviewId, " +
                "averageScore: ${evaluation.averageScore}, " +
                "weightedScore: $weightedScore")

    meterRegistry.counter("mock_interview.ended").increment()

    return saved
}
```

---

### Step 7: Integration Test 작성

**파일**: `src/test/kotlin/com/hojun/interviewnote/interviewnoteapi/Phase8AIntegrationTest.kt`

```kotlin
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Phase8AIntegrationTest {

    @Autowired
    private lateinit var interviewAiService: InterviewAiService

    @Autowired
    private lateinit var mockInterviewRepository: MockInterviewRepository

    @Autowired
    private lateinit var interviewMessageRepository: InterviewMessageRepository

    @Test
    fun `가중 평균 점수 계산 - 첫 답변 제외`() {
        // Given: 5개 답변 (첫 답변 4.0점, 나머지 1.5점)
        val messages = listOf(
            InterviewMessage(
                mockInterviewId = 1L,
                sender = MessageSender.USER,
                content = "안녕하세요. 저는 백엔드 개발자입니다.",
                messageIndex = 1,
                logicScore = 4,
                specificityScore = 4,
                deliveryScore = 4
            ),
            InterviewMessage(
                mockInterviewId = 1L,
                sender = MessageSender.USER,
                content = "ㅎㅎ",
                messageIndex = 3,
                logicScore = 1,
                specificityScore = 1,
                deliveryScore = 1
            ),
            InterviewMessage(
                mockInterviewId = 1L,
                sender = MessageSender.USER,
                content = "노력했습니다",
                messageIndex = 5,
                logicScore = 2,
                specificityScore = 1,
                deliveryScore = 2
            )
        )

        // When: 가중 평균 계산
        val weightedScore = interviewAiService.calculateWeightedScore(messages)

        // Then: 첫 답변 제외 평균 = (1.0 + 1.67) / 2 = 1.34
        // 저품질 비율 50% 이상 → 패널티 * 0.8
        assertThat(weightedScore).isLessThan(2.0)
        assertThat(weightedScore).isGreaterThan(1.0)
    }

    @Test
    fun `종합 피드백 길이 800자 이상`() {
        // Given: 완료된 면접
        val interview = mockInterviewRepository.findById(1L).get()
        val messages = interviewMessageRepository
            .findByMockInterviewIdOrderByCreatedAtAsc(1L)

        // When: 종합 평가 생성
        val evaluation = interviewAiService.generateFinalEvaluation(
            interview = interview,
            conversation = messages,
            jobPosting = null
        )

        // Then: 피드백 길이 800자 이상
        assertThat(evaluation.overallFeedback.length).isGreaterThanOrEqualTo(800)
        assertThat(evaluation.overallFeedback.length).isLessThanOrEqualTo(1500)
    }

    @Test
    fun `강점_개선점 개수 유연화 - 낮은 점수`() {
        // Given: 평균 점수 1.5 (매우 낮음)
        // When: 종합 평가 생성
        // Then: strengths 0-1개, improvements 4-5개
        val evaluation = interviewAiService.generateFinalEvaluation(...)

        val strengths = objectMapper.readValue(
            evaluation.keyStrengths,
            object : TypeReference<List<String>>() {}
        )
        val improvements = objectMapper.readValue(
            evaluation.keyImprovements,
            object : TypeReference<List<String>>() {}
        )

        assertThat(strengths.size).isLessThanOrEqualTo(1)
        assertThat(improvements.size).isGreaterThanOrEqualTo(4)
    }
}
```

---

### Step 8: 검증 체크리스트

- [ ] V13 migration 실행 성공
- [ ] MockInterview 엔티티 컴파일 성공
- [ ] calculateWeightedScore() 단위 테스트 통과
- [ ] PromptBuilder 엄격한 프롬프트 검증
- [ ] InterviewResponseParser 800자 검증
- [ ] Phase8AIntegrationTest 3개 테스트 통과
- [ ] 기존 테스트 회귀 방지 (모두 통과)

---

## Phase 8B: 경력 수준 및 UI 개선

**예상 기간**: 2-3일
**우선순위**: 높음 🟡

### 목표

- ✅ 경력 수준 4단계 선택 가능
- ✅ 경력 수준별 질문 난이도 조정
- ✅ 사용 방법 안내 UI 추가
- ✅ 채용 공고 기반 AI 면접 UI 추가

---

### Step 1: job-field-modal.html - 경력 수준 선택 UI 추가

**파일**: `src/main/resources/templates/fragments/job-field-modal.html`

**변경 사항**:

```html
<!-- AI 면접 연습 시작 모달 -->
<div id="jobFieldModal" class="modal hidden">
    <div class="modal-content max-w-2xl">
        <div class="modal-header">
            <h3>AI 면접 연습 시작</h3>
            <button onclick="closeJobFieldModal()" class="modal-close">&times;</button>
        </div>

        <div class="modal-body">
            <!-- Phase 8B: 사용 방법 안내 추가 -->
            <div class="bg-gradient-to-r from-blue-50 to-purple-50 dark:from-blue-900/20 dark:to-purple-900/20
                        border-2 border-blue-200 dark:border-blue-800 rounded-xl p-6 mb-6">
                <h4 class="text-lg font-bold text-gray-900 dark:text-dark-text mb-4 flex items-center gap-2">
                    <svg class="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                              d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                    </svg>
                    AI 면접 연습 방법
                </h4>
                <ol class="space-y-3 text-sm text-gray-700 dark:text-dark-muted">
                    <li class="flex items-start gap-3">
                        <span class="flex-shrink-0 w-7 h-7 rounded-full bg-blue-600 text-white
                                     flex items-center justify-center text-xs font-bold">1</span>
                        <span>면접관 AI가 <strong>자기소개</strong>부터 시작해 <strong>2-5개 질문</strong>을 드립니다</span>
                    </li>
                    <li class="flex items-start gap-3">
                        <span class="flex-shrink-0 w-7 h-7 rounded-full bg-blue-600 text-white
                                     flex items-center justify-center text-xs font-bold">2</span>
                        <span>각 질문마다 <strong>200자 이내</strong>로 답변을 작성하세요</span>
                    </li>
                    <li class="flex items-start gap-3">
                        <span class="flex-shrink-0 w-7 h-7 rounded-full bg-blue-600 text-white
                                     flex items-center justify-center text-xs font-bold">3</span>
                        <span>답변 제출 시 AI가 <strong>즉시 평가</strong>하고 다음 질문을 생성합니다 (2-3초 소요)</span>
                    </li>
                    <li class="flex items-start gap-3">
                        <span class="flex-shrink-0 w-7 h-7 rounded-full bg-blue-600 text-white
                                     flex items-center justify-center text-xs font-bold">4</span>
                        <span>면접 종료 후 <strong>종합 평가</strong>와 <strong>개선 포인트</strong>를 확인하세요</span>
                    </li>
                </ol>

                <div class="mt-4 pt-4 border-t border-blue-300 dark:border-blue-700">
                    <p class="text-xs text-blue-800 dark:text-blue-300 font-medium">
                        💡 팁: 구체적인 사례와 수치를 포함하면 더 높은 점수를 받을 수 있습니다!
                    </p>
                </div>
            </div>

            <!-- 면접 직무 선택 -->
            <form id="jobFieldForm" th:action="@{/mock-interviews/start}" method="post">
                <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}" />

                <div class="space-y-4">
                    <!-- 직무 분야 선택 -->
                    <div>
                        <label for="selectedJobField" class="block text-sm font-semibold text-gray-700 dark:text-dark-text mb-2">
                            직무 분야 <span class="text-red-500">*</span>
                        </label>
                        <select id="selectedJobField"
                                name="selectedJobField"
                                required
                                class="block w-full px-4 py-3 border border-gray-300 dark:border-dark-border rounded-lg
                                       bg-white dark:bg-dark-bg text-gray-900 dark:text-dark-text
                                       focus:ring-2 focus:ring-primary focus:border-primary
                                       transition-all duration-200 shadow-sm">
                            <option value="">선택해주세요</option>
                            <option value="IT">IT개발</option>
                            <option value="PLANNING">기획·전략</option>
                            <option value="MARKETING">마케팅·홍보·조사</option>
                            <option value="SALES">영업·유통·무역</option>
                            <option value="SERVICE">고객서비스·리테일</option>
                            <option value="LOGISTICS">구매·자재·물류</option>
                            <option value="PRODUCTION">생산·제조</option>
                            <option value="RESEARCH">연구개발·설계</option>
                            <option value="HR">인사·노무·HRD</option>
                            <option value="ACCOUNTING">회계·세무·재무</option>
                            <option value="DESIGN">디자인</option>
                            <option value="CONSTRUCTION">건설·건축</option>
                            <option value="EDUCATION">교육</option>
                            <option value="LEGAL">법무·사무·총무</option>
                            <option value="FINANCE">금융·보험</option>
                            <option value="MEDIA">미디어·문화·스포츠</option>
                            <option value="PUBLIC">공공·복지</option>
                        </select>
                    </div>

                    <!-- Phase 8B: 경력 수준 선택 추가 -->
                    <div>
                        <label for="careerLevel" class="block text-sm font-semibold text-gray-700 dark:text-dark-text mb-2">
                            경력 수준 <span class="text-red-500">*</span>
                        </label>
                        <select id="careerLevel"
                                name="careerLevel"
                                required
                                class="block w-full px-4 py-3 border border-gray-300 dark:border-dark-border rounded-lg
                                       bg-white dark:bg-dark-bg text-gray-900 dark:text-dark-text
                                       focus:ring-2 focus:ring-primary focus:border-primary
                                       transition-all duration-200 shadow-sm">
                            <option value="">선택해주세요</option>
                            <option value="ENTRY">신입 (경험 1년 미만)</option>
                            <option value="JUNIOR">주니어 (1-3년)</option>
                            <option value="SENIOR">시니어 (3-7년)</option>
                            <option value="SENIOR_PLUS">시니어+ (7년 이상)</option>
                        </select>
                        <p class="mt-2 text-xs text-gray-500 dark:text-dark-muted">
                            경력 수준에 따라 질문 난이도가 조정됩니다
                        </p>
                    </div>
                </div>

                <div class="flex gap-3 mt-6">
                    <button type="button"
                            onclick="closeJobFieldModal()"
                            class="flex-1 px-6 py-3 border border-gray-300 dark:border-dark-border rounded-lg
                                   text-gray-700 dark:text-dark-text bg-white dark:bg-dark-bg
                                   hover:bg-gray-50 dark:hover:bg-gray-800
                                   transition-colors duration-200">
                        취소
                    </button>
                    <button type="submit"
                            class="flex-1 px-6 py-3 bg-gradient-to-r from-primary to-purple-600
                                   text-white font-semibold rounded-lg
                                   hover:from-primary-dark hover:to-purple-700
                                   transition-all duration-200 shadow-lg">
                        면접 시작
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>
```

---

### Step 2: MockInterviewController - careerLevel 파라미터 추가

**파일**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/controller/MockInterviewController.kt`

**메서드 수정**: `startInterview()`

```kotlin
/**
 * POST /mock-interviews/start
 *
 * Phase 8B: careerLevel 파라미터 추가
 */
@PostMapping("/start")
fun startInterview(
    @AuthenticationPrincipal userDetails: UserDetails,
    @RequestParam(required = false) jobPostingId: Long?,
    @RequestParam selectedJobField: String,
    @RequestParam(required = false) careerLevel: String?,  // Phase 8B: 추가
    redirectAttributes: RedirectAttributes
): String {
    val user = userRepository.findByEmail(userDetails.username)
        ?: throw IllegalStateException("사용자를 찾을 수 없습니다")

    return try {
        val jobField = JobField.valueOf(selectedJobField)
        val career = careerLevel?.let { CareerLevel.valueOf(it) }  // Phase 8B: 추가

        val interview = mockInterviewService.startInterview(
            userId = user.id,
            jobPostingId = jobPostingId,
            selectedJobField = jobField,
            careerLevel = career  // Phase 8B: 추가
        )

        logger.info("면접 시작 - userId: ${user.id}, jobField: $selectedJobField, " +
                    "careerLevel: $careerLevel, jobPostingId: $jobPostingId")

        "redirect:/mock-interviews/${interview.id}/chat"
    } catch (e: IllegalArgumentException) {
        logger.error("면접 시작 실패 - 잘못된 파라미터: ${e.message}")
        redirectAttributes.addFlashAttribute("error", "잘못된 직무 또는 경력 수준입니다")
        "redirect:/home"
    } catch (e: Exception) {
        logger.error("면접 시작 실패", e)
        redirectAttributes.addFlashAttribute("error", "면접 시작 중 오류가 발생했습니다")
        "redirect:/home"
    }
}
```

---

### Step 3: MockInterviewService - careerLevel 저장

**파일**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/service/MockInterviewService.kt`

**메서드 수정**: `startInterview()`

```kotlin
fun startInterview(
    userId: Long,
    jobPostingId: Long?,
    selectedJobField: JobField,
    careerLevel: CareerLevel? = null  // Phase 8B: 추가
): MockInterview {
    // ... (jobPosting 검증 로직)

    // Phase 8B: careerLevel 포함하여 면접 생성
    val interview = MockInterview(
        userId = userId,
        jobPostingId = jobPostingId,
        selectedJobField = effectiveJobField,
        careerLevel = careerLevel  // Phase 8B: 추가
    )
    val saved = mockInterviewRepository.save(interview)

    logger.info("면접 생성 - interviewId: ${saved.id}, userId: $userId, " +
                "jobField: ${effectiveJobField.name}, careerLevel: ${careerLevel?.name}")

    // ... (첫 질문 생성)

    return saved
}
```

---

### Step 4: job-postings/questions.html - AI 면접 버튼 추가 ✅

**파일**: `src/main/resources/templates/job-postings/questions.html`

**추가 위치**: "10개 질문으로 연습" 버튼 옆

**구현 결정**: **옵션 A 적용** - User 프로필에서 careerLevel 가져오기

```html
<!-- 액션 버튼 영역 -->
<div class="flex flex-col sm:flex-row gap-4 mb-8">
    <!-- 기존: 10개 질문으로 연습 -->
    <a th:href="@{/generated-questions/{id}/answer(id=${jobPosting.questions[0].id})}"
       class="flex-1 btn-neon text-center py-4 text-lg">
        📝 10개 질문으로 연습
    </a>

    <!-- Phase 8B: 채용 공고 기반 AI 면접 추가 -->
    <form th:action="@{/mock-interviews/start}" method="post" class="flex-1">
        <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}" />
        <input type="hidden" name="jobPostingId" th:value="${jobPosting.id}" />
        <input type="hidden" name="selectedJobField" th:value="${jobPosting.effectiveJobField.name}" />

        <button type="submit"
                class="w-full btn-neon py-4 text-lg flex items-center justify-center gap-2">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/>
            </svg>
            이 공고로 AI 면접 연습
        </button>
    </form>
</div>

<!-- 안내 메시지 -->
<div class="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-4 mb-6">
    <p class="text-sm text-blue-800 dark:text-blue-300">
        💡 <strong>AI 면접 연습</strong>: 이 채용 공고를 바탕으로 실전 같은 모의 면접을 진행합니다 (2-5개 질문, 실시간 평가).
        경력 수준은 프로필 설정을 따릅니다 (기본값: 신입).
    </p>
</div>
```

**경력 수준 처리 로직** (MockInterviewController.kt):
1. URL 파라미터 `careerLevel`이 있으면 사용
2. 없으면 `user.careerLevel` (프로필 설정) 사용
3. 둘 다 없으면 기본값 `CareerLevel.ENTRY` (신입) 사용

```kotlin
val career = when {
    !careerLevel.isNullOrBlank() -> CareerLevel.valueOf(careerLevel)
    user.careerLevel != null -> user.careerLevel
    else -> CareerLevel.ENTRY
}
```

**장점**:
- 사용자가 프로필에서 한 번만 경력 수준 설정
- 채용 공고 기반 면접 시작이 간편함 (클릭 한 번)
- 일반 AI 면접 시작 모달에서는 여전히 선택 가능 (우선순위 높음)

---

### Step 5: 검증 체크리스트

- [ ] job-field-modal.html 사용 방법 안내 UI 표시
- [ ] 경력 수준 4단계 선택 가능
- [ ] careerLevel 파라미터 전달 확인
- [ ] MockInterview에 careerLevel 저장 확인
- [ ] job-postings/questions.html AI 면접 버튼 표시
- [ ] 채용 공고 기반 AI 면접 시작 성공

---

## Phase 8C: 리뷰 통합 및 재개 기능

**예상 기간**: 2-3일
**우선순위**: 중간 🟢

### 목표

- ✅ 리뷰 이력 페이지 2개 탭 (질문 연습 / AI 면접)
- ✅ "이어서 연습하기" vs "새로 연습하기" 구분

---

### Step 1: DTO 생성

**파일**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/dto/MockInterviewReviewDto.kt` (신규)

```kotlin
package com.hojun.interviewnote.interviewnoteapi.dto

import java.time.LocalDateTime

/**
 * AI 면접 리뷰 목록 DTO
 * Phase 8C: 리뷰 이력 통합
 */
data class MockInterviewReviewDto(
    val interviewId: Long,
    val jobField: String,               // "IT개발"
    val careerLevel: String?,           // "신입", "주니어" 등
    val startedAt: LocalDateTime,
    val averageScore: Double?,          // weightedAverageScore 우선, 없으면 averageScore
    val messageCount: Int,              // AI 질문 개수
    val jobPostingInfo: JobPostingInfoDto?  // 공고 기반인 경우
)

/**
 * 채용 공고 요약 DTO
 */
data class JobPostingInfoDto(
    val companyName: String,
    val jobTitle: String
)
```

---

### Step 2: ReviewService - getUserMockInterviewReviews() 추가

**파일**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/service/ReviewService.kt`

**새로운 메서드 추가**:

```kotlin
/**
 * 사용자의 AI 면접 이력 조회
 * Phase 8C: 리뷰 이력 통합
 */
fun getUserMockInterviewReviews(userId: Long): List<MockInterviewReviewDto> {
    val interviews = mockInterviewRepository
        .findByUserIdAndStatusOrderByStartedAtDesc(userId, MockInterviewStatus.COMPLETED)

    return interviews.map { interview ->
        val messageCount = interviewMessageRepository
            .countByMockInterviewIdAndSender(interview.id, MessageSender.AI)

        val jobPostingInfo = interview.jobPostingId?.let { id ->
            jobPostingRepository.findById(id).orElse(null)?.let {
                JobPostingInfoDto(
                    companyName = it.companyName,
                    jobTitle = it.jobTitle
                )
            }
        }

        MockInterviewReviewDto(
            interviewId = interview.id,
            jobField = interview.selectedJobField.displayName,
            careerLevel = interview.careerLevel?.displayName,
            startedAt = interview.startedAt,
            averageScore = interview.weightedAverageScore ?: interview.averageScore,
            messageCount = messageCount.toInt(),
            jobPostingInfo = jobPostingInfo
        )
    }
}
```

**Repository 메서드 추가 필요**:

```kotlin
// MockInterviewRepository.kt
fun findByUserIdAndStatusOrderByStartedAtDesc(
    userId: Long,
    status: MockInterviewStatus
): List<MockInterview>
```

---

### Step 3: ReviewController - 2개 탭 데이터 제공

**파일**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/controller/ReviewController.kt`

**메서드 수정**: `list()`

```kotlin
@GetMapping
fun list(
    @AuthenticationPrincipal userDetails: UserDetails,
    model: Model
): String {
    val user = userRepository.findByEmail(userDetails.username)
        ?: throw IllegalStateException("사용자를 찾을 수 없습니다")

    // 질문 연습 이력
    val questionReviews = reviewService.getUserReviews(user.id)

    // Phase 8C: AI 면접 이력 추가
    val mockInterviewReviews = reviewService.getUserMockInterviewReviews(user.id)

    model.addAttribute("questionReviews", questionReviews)
    model.addAttribute("mockInterviewReviews", mockInterviewReviews)
    model.addAttribute("currentUser", user)

    logger.info("리뷰 목록 조회 - userId: ${user.id}, " +
                "questionReviews: ${questionReviews.size}, " +
                "mockInterviewReviews: ${mockInterviewReviews.size}")

    return "reviews/list"
}
```

---

### Step 4: reviews/list.html - 탭 구조 추가

**파일**: `src/main/resources/templates/reviews/list.html`

**전체 구조 변경**:

```html
<!-- 탭 네비게이션 -->
<div class="flex border-b border-gray-200 dark:border-dark-border mb-6">
    <button onclick="switchTab('question')"
            id="questionTab"
            class="px-6 py-3 font-semibold text-primary border-b-2 border-primary">
        질문 연습 (<span th:text="${#lists.size(questionReviews)}">5</span>)
    </button>
    <button onclick="switchTab('mock')"
            id="mockTab"
            class="px-6 py-3 font-semibold text-gray-500 hover:text-gray-700">
        AI 면접 (<span th:text="${#lists.size(mockInterviewReviews)}">3</span>)
    </button>
</div>

<!-- 질문 연습 탭 내용 (기존 코드) -->
<div id="questionTabContent">
    <!-- 기존 reviews 목록 코드 -->
</div>

<!-- AI 면접 탭 내용 (신규) -->
<div id="mockTabContent" class="hidden">
    <div class="space-y-4" th:if="${not #lists.isEmpty(mockInterviewReviews)}">
        <div th:each="interview : ${mockInterviewReviews}"
             class="card card-interactive"
             th:onclick="|location.href='@{/mock-interviews/{id}/result(id=${interview.interviewId})}'|">
            <div class="flex justify-between items-start">
                <div class="flex-1">
                    <h3 class="text-xl font-semibold mb-2">
                        <span th:text="${interview.jobField}">IT개발</span> 면접
                        <span th:if="${interview.careerLevel != null}"
                              class="text-sm text-gray-500 ml-2"
                              th:text="'(' + ${interview.careerLevel} + ')'">
                            (신입)
                        </span>
                    </h3>

                    <!-- 채용 공고 정보 (있는 경우) -->
                    <div th:if="${interview.jobPostingInfo != null}"
                         class="text-sm text-gray-600 dark:text-dark-muted mb-2">
                        <span th:text="${interview.jobPostingInfo.companyName}">회사명</span> ·
                        <span th:text="${interview.jobPostingInfo.jobTitle}">포지션</span>
                    </div>

                    <div class="flex gap-2 items-center">
                        <span class="badge badge-purple"
                              th:text="${interview.messageCount} + '개 질문'">
                            5개 질문
                        </span>
                        <span class="text-sm text-gray-500"
                              th:text="${#temporals.format(interview.startedAt, 'yyyy-MM-dd HH:mm')}">
                            2026-05-03 14:30
                        </span>
                    </div>
                </div>

                <!-- 점수 표시 -->
                <div class="text-center min-w-[100px]">
                    <div class="text-4xl font-bold mb-1"
                         th:classappend="${interview.averageScore >= 4.0} ? 'text-green-500' :
                                        (${interview.averageScore >= 3.0} ? 'text-primary' : 'text-yellow-500')"
                         th:text="${#numbers.formatDecimal(interview.averageScore, 1, 1)}">
                        3.8
                    </div>
                    <div class="text-sm text-gray-500">/ 5.0</div>
                </div>
            </div>
        </div>
    </div>

    <!-- 빈 상태 -->
    <div th:if="${#lists.isEmpty(mockInterviewReviews)}" class="text-center py-16">
        <svg class="w-20 h-20 mx-auto mb-4 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/>
        </svg>
        <p class="text-gray-500 mb-4">아직 완료된 AI 면접이 없습니다.</p>
        <button onclick="openJobFieldModal()" class="btn-neon">
            첫 AI 면접 시작하기
        </button>
    </div>
</div>

<script>
function switchTab(tabName) {
    // Show/hide content
    const questionContent = document.getElementById('questionTabContent');
    const mockContent = document.getElementById('mockTabContent');
    const questionTab = document.getElementById('questionTab');
    const mockTab = document.getElementById('mockTab');

    if (tabName === 'question') {
        questionContent.classList.remove('hidden');
        mockContent.classList.add('hidden');

        questionTab.classList.add('text-primary', 'border-b-2', 'border-primary');
        questionTab.classList.remove('text-gray-500');

        mockTab.classList.remove('text-primary', 'border-b-2', 'border-primary');
        mockTab.classList.add('text-gray-500');
    } else {
        questionContent.classList.add('hidden');
        mockContent.classList.remove('hidden');

        mockTab.classList.add('text-primary', 'border-b-2', 'border-primary');
        mockTab.classList.remove('text-gray-500');

        questionTab.classList.remove('text-primary', 'border-b-2', 'border-primary');
        questionTab.classList.add('text-gray-500');
    }
}
</script>
```

---

### Step 5: MockInterviewController - resumeInterview() 엔드포인트 추가

**파일**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/controller/MockInterviewController.kt`

**새로운 엔드포인트 추가**:

```kotlin
/**
 * POST /mock-interviews/{id}/resume
 *
 * Phase 8C: 완료된 면접 재개
 * 응답: redirect to /mock-interviews/{id}/chat
 */
@PostMapping("/{id}/resume")
fun resumeInterview(
    @PathVariable id: Long,
    @AuthenticationPrincipal userDetails: UserDetails,
    redirectAttributes: RedirectAttributes
): String {
    val user = userRepository.findByEmail(userDetails.username)
        ?: throw IllegalStateException("사용자를 찾을 수 없습니다")

    return try {
        mockInterviewService.resumeInterview(id, user.id)

        redirectAttributes.addFlashAttribute("info",
            "면접이 재개되었습니다. 이어서 연습해보세요!")

        logger.info("면접 재개 - interviewId: $id, userId: ${user.id}")

        "redirect:/mock-interviews/$id/chat"
    } catch (e: IllegalArgumentException) {
        logger.error("면접 재개 실패 - interviewId: $id: ${e.message}")
        redirectAttributes.addFlashAttribute("error", e.message)
        "redirect:/mock-interviews/$id/result"
    } catch (e: Exception) {
        logger.error("면접 재개 실패 - interviewId: $id", e)
        redirectAttributes.addFlashAttribute("error", "면접 재개 중 오류가 발생했습니다")
        "redirect:/mock-interviews/$id/result"
    }
}
```

---

### Step 6: MockInterviewService - resumeInterview() 메서드 추가

**파일**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/service/MockInterviewService.kt`

**새로운 메서드 추가**:

```kotlin
/**
 * 면접 재개
 * Phase 8C: 완료된 면접을 다시 시작
 */
fun resumeInterview(interviewId: Long, userId: Long): MockInterview {
    val interview = getInterview(interviewId, userId)

    require(interview.status == MockInterviewStatus.COMPLETED) {
        "완료된 면접만 재개할 수 있습니다"
    }

    interview.resume()
    val resumed = mockInterviewRepository.save(interview)

    logger.info("면접 재개 - interviewId: $interviewId, userId: $userId")
    meterRegistry.counter("mock_interview.resumed").increment()

    return resumed
}
```

---

### Step 7: result.html - 버튼 수정

**파일**: `src/main/resources/templates/mock-interviews/result.html`

**버튼 영역 변경**:

```html
<!-- 액션 버튼 -->
<div class="flex flex-col sm:flex-row gap-4 justify-center mt-8">
    <!-- Phase 8C: 이어서 연습하기 -->
    <form th:action="@{/mock-interviews/{id}/resume(id=${interview.id})}" method="post">
        <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}" />
        <button type="submit" class="btn-neon w-full sm:w-auto">
            ▶️ 이어서 연습하기
        </button>
    </form>

    <!-- Phase 8C: 새로 연습하기 -->
    <button onclick="openJobFieldModal()" class="btn-secondary w-full sm:w-auto">
        🔄 새로 연습하기
    </button>

    <a th:href="@{/reviews}" class="btn-secondary text-center w-full sm:w-auto">
        📋 리뷰 목록 보기
    </a>
</div>
```

---

### Step 8: 검증 체크리스트

- [ ] MockInterviewReviewDto 생성
- [ ] ReviewService.getUserMockInterviewReviews() 구현
- [ ] ReviewController.list() 2개 탭 데이터 전달
- [ ] reviews/list.html 탭 구조 표시
- [ ] POST /mock-interviews/{id}/resume 엔드포인트 동작
- [ ] MockInterview.resume() 메서드 동작
- [ ] result.html 버튼 2개 표시

---

## Phase 8D: 테스트 및 문서화

**예상 기간**: 1-2일
**우선순위**: 필수 🔴

### Step 1: Phase8IntegrationTest.kt 작성

**파일**: `src/test/kotlin/com/hojun/interviewnote/interviewnoteapi/Phase8IntegrationTest.kt`

**테스트 목록** (10+):

1. ✅ 가중 평균 점수 계산 - 첫 답변 제외
2. ✅ 저품질 답변 50% 이상 시 패널티
3. ✅ 종합 피드백 길이 800-1200자
4. ✅ 강점/개선점 개수 유연화 (낮은 점수)
5. ✅ 강점/개선점 개수 유연화 (높은 점수)
6. ✅ 경력 수준 선택 및 저장
7. ✅ 리뷰 이력 2개 탭 조회
8. ✅ 면접 재개 기능
9. ✅ 채용 공고 기반 AI 면접 시작
10. ✅ 사용 방법 안내 UI 표시

### Step 2: 문서 업데이트

**파일 목록**:

1. `CLAUDE.md` - Phase 8 완료 상태 반영
2. `CHANGELOG.md` - 0.8.0 추가
3. `PHASE8_COMPLETION_REPORT.md` - 완료 보고서 작성

### Step 3: 배포 준비

- [ ] V13 migration dev 환경 검증
- [ ] Docker 빌드 성공
- [ ] Smoke Test 통과
- [ ] V13 migration prod 환경 적용

---

## 종료 조건 (Acceptance Criteria)

Phase 8은 다음 조건을 모두 만족할 때 완료됩니다:

### 기능 완성도

- [ ] 짧은 답변(50자 미만) 평균 점수 < 2.0점
- [ ] 종합 피드백 길이 800-1200자
- [ ] 강점/개선점 개수 0-5개 (유연)
- [ ] 경력 수준 선택 가능 (4단계)
- [ ] 경력 수준별 질문 난이도 조정
- [ ] 리뷰 이력 페이지 2개 탭
- [ ] "이어서 연습하기" vs "새로 연습하기" 구분
- [ ] 채용 공고 기반 AI 면접 UI
- [ ] 사용 방법 안내 UI

### 테스트

- [ ] Phase8IntegrationTest 10+ 테스트 통과
- [ ] 모든 기존 테스트 통과 (회귀 방지)
- [ ] 수동 E2E 테스트 완료

### 문서화

- [ ] CLAUDE.md 업데이트
- [ ] CHANGELOG.md 업데이트 (0.8.0)
- [ ] PHASE8_COMPLETION_REPORT.md 작성

### 배포 준비

- [ ] V13 migration 검증 (dev, prod)
- [ ] Docker 빌드 성공
- [ ] Smoke Test 통과

---

**문서 종료**
