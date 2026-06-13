# Interview Note API - Refactoring Guide

**Version**: 1.0
**Last Updated**: 2026-05-18
**Target**: 코드 품질 8.0 → 9.0+ 달성

---

## Executive Summary

| 항목 | 현재 | 목표 | 방법 |
|------|------|------|------|
| **코드 품질** | 8.0/10 | 9.0+/10 | 아래 리팩토링 수행 |
| **Technical Debt** | 6개 | 0개 | 우선순위별 해결 |
| **예상 소요** | - | ~16시간 | 3주 분산 작업 |

---

## Priority Matrix

```
                     ┌─────────────────────────────────────────┐
                     │              HIGH IMPACT                │
                     │                                         │
        ┌────────────┼──────────────┬──────────────────────────┤
        │            │ P1           │ P2                       │
        │  URGENT    │ ReviewService│ MockInterviewService     │
        │            │ PromptBuilder│ N+1 Query                │
        ├────────────┼──────────────┼──────────────────────────┤
        │            │ P3           │ P4                       │
        │  NOT       │ REST API     │ Test Pyramid             │
        │  URGENT    │ Domain Model │ Documentation            │
        └────────────┴──────────────┴──────────────────────────┘
                                   LOW IMPACT
```

---

## Priority 1: Critical (즉시 수정)

### 1.1 ReviewService 코드 중복 제거

**현재 상태**
- 파일: `src/main/kotlin/.../service/ReviewService.kt`
- 문제: 3개 메서드에 80% 동일 코드 (lines 40-67, 78-105, 116-143)
- 영향: DRY 위반, 수정 시 3곳 변경 필요

**Before (문제 코드)**
```kotlin
// getReviewList() - lines 40-67
val (questionContent, category) = when {
    answer.questionId != null -> {
        val question = questionRepository.findById(answer.questionId).orElse(null)
        question?.let { it.content to it.category }
    }
    answer.generatedQuestionId != null -> {
        val genQuestion = generatedQuestionRepository.findById(answer.generatedQuestionId).orElse(null)
        genQuestion?.let { it.content to it.category }
    }
    else -> null
} ?: return@mapNotNull null

// getUserReviews() - lines 78-105 (거의 동일)
// getUserReviewsPage() - lines 116-143 (거의 동일)
```

**After (리팩토링)**
```kotlin
/**
 * 질문 정보 조회 (공통 메서드)
 *
 * InterviewAnswer에서 questionId 또는 generatedQuestionId를 사용하여
 * 질문 내용과 카테고리를 조회합니다.
 *
 * @param answer 답변 엔티티
 * @return Pair(질문 내용, 카테고리) 또는 null
 */
private fun resolveQuestion(answer: InterviewAnswer): Pair<String, String>? {
    return when {
        answer.questionId != null -> {
            questionRepository.findById(answer.questionId).orElse(null)
                ?.let { it.content to it.category }
        }
        answer.generatedQuestionId != null -> {
            generatedQuestionRepository.findById(answer.generatedQuestionId).orElse(null)
                ?.let { it.content to it.category }
        }
        else -> null
    }
}

/**
 * ReviewSummaryDto 생성 (공통 메서드)
 */
private fun buildReviewSummary(answer: InterviewAnswer): ReviewSummaryDto? {
    val (questionContent, category) = resolveQuestion(answer) ?: return null
    val feedback = aiFeedbackRepository.findByInterviewAnswerId(answer.id) ?: return null

    return ReviewSummaryDto(
        answerId = answer.id,
        questionContent = questionContent,
        category = category,
        answeredAt = answer.createdAt,
        averageScore = feedback.averageScore
    )
}

// 기존 메서드 단순화
fun getUserReviews(userId: Long): List<ReviewSummaryDto> {
    return interviewAnswerRepository.findByUserIdOrderByCreatedAtDesc(userId)
        .mapNotNull { buildReviewSummary(it) }
}

fun getUserReviewsPage(userId: Long, pageable: Pageable): Page<ReviewSummaryDto> {
    val answersPage = interviewAnswerRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
    val reviewDtos = answersPage.content.mapNotNull { buildReviewSummary(it) }
    return PageImpl(reviewDtos, pageable, answersPage.totalElements)
}
```

**작업 단계**
1. [ ] `resolveQuestion()` private 메서드 추가
2. [ ] `buildReviewSummary()` private 메서드 추가
3. [ ] `getReviewList()`, `getUserReviews()`, `getUserReviewsPage()` 단순화
4. [ ] 기존 테스트 실행하여 동작 확인
5. [ ] @Deprecated `getReviewList()` 제거 검토

**예상 소요**: 1시간
**난이도**: Easy
**위험도**: Low (기존 테스트로 검증 가능)

---

### 1.2 PromptBuilder 분리

**현재 상태**
- 파일: `src/main/kotlin/.../service/ai/PromptBuilder.kt`
- 문제: 950줄, 4가지 역할 혼재 (피드백/질문생성/면접/평가 프롬프트)
- 영향: SRP 위반, 유지보수 어려움

**Phase 1: Config Enum 도입 (2시간)**

```kotlin
// Before: 17개의 개별 메서드
private fun buildItSystemPrompt(targetJob: String) =
    buildBasePrompt(targetJob, "기술적 사고...", "구체적 기술 스택...", listOf(...))

private fun buildSalesSystemPrompt(targetJob: String) =
    buildBasePrompt(targetJob, "영업 전략...", "구체적 실적...", listOf(...))
// ... 15개 더

// After: 데이터 클래스 + Map
data class JobFieldPromptConfig(
    val logicDescription: String,
    val specificityDescription: String,
    val badExamples: List<String>
)

private val feedbackPromptConfigs = mapOf(
    "IT" to JobFieldPromptConfig(
        logicDescription = "기술적 사고의 논리적 흐름과 일관성",
        specificityDescription = "구체적 기술 스택, 사례, 수치 제시 정도",
        badExamples = listOf(
            "추상적 답변: \"저는 열심히 노력했습니다\"",
            "구체성 부족: \"Spring을 사용했습니다\" (어떻게? 왜? 무엇을?)"
        )
    ),
    "SALES" to JobFieldPromptConfig(
        logicDescription = "영업 전략과 설득의 논리적 흐름",
        specificityDescription = "구체적 실적 수치, 고객 사례, 영업 방법 제시 정도",
        badExamples = listOf(
            "추상적 답변: \"열심히 영업했습니다\"",
            "구체성 부족: \"고객이 만족했습니다\" (매출은? 어떤 방법으로?)"
        )
    ),
    // ... 나머지 15개 직무
)

fun buildSystemPrompt(jobField: String, targetJob: String): String {
    val config = feedbackPromptConfigs[jobField]
        ?: throw IllegalArgumentException("지원하지 않는 직무 분야입니다: $jobField")
    return buildBasePrompt(targetJob, config)
}

private fun buildBasePrompt(targetJob: String, config: JobFieldPromptConfig): String {
    val badExamplesText = config.badExamples.joinToString("\n            - ")
    return """
        당신은 ${targetJob} 면접을 준비하는 지원자를 돕는 면접 코치입니다.
        ...
        평가 기준:
        - 논리성(logic): ${config.logicDescription} (1-5점)
        - 구체성(specificity): ${config.specificityDescription} (1-5점)
        ...
    """.trimIndent()
}
```

**Phase 2: 클래스 분리 (2시간)**

```
service/ai/
├── PromptBuilder.kt (삭제)
└── prompt/
    ├── FeedbackPromptBuilder.kt      # 답변 평가 프롬프트 (lines 44-398)
    ├── QuestionPromptBuilder.kt      # 질문 생성 프롬프트 (lines 399-598)
    ├── InterviewPromptBuilder.kt     # 면접 진행 프롬프트 (lines 600-763)
    ├── EvaluationPromptBuilder.kt    # 종합 평가 프롬프트 (lines 771-907)
    └── JobFieldPromptConfig.kt       # 직무별 설정 데이터
```

**FeedbackPromptBuilder.kt 예시**
```kotlin
@Service
class FeedbackPromptBuilder {

    private val promptConfigs = mapOf(
        "IT" to JobFieldPromptConfig(...),
        "SALES" to JobFieldPromptConfig(...),
        // ...
    )

    fun buildSystemPrompt(jobField: String, targetJob: String): String {
        val config = promptConfigs[jobField]
            ?: throw IllegalArgumentException("지원하지 않는 직무: $jobField")
        return buildBasePrompt(targetJob, config)
    }

    fun buildUserPrompt(question: Question, answer: String): String {
        return """
            면접 질문:
            ${question.content}

            지원자 답변:
            $answer

            위 답변을 평가하고, JSON 형식으로 피드백을 제공해주세요.
        """.trimIndent()
    }

    private fun buildBasePrompt(targetJob: String, config: JobFieldPromptConfig): String {
        // 공통 템플릿
    }
}
```

**작업 단계**
1. [ ] `JobFieldPromptConfig` 데이터 클래스 생성
2. [ ] `feedbackPromptConfigs` Map으로 17개 직무 정의
3. [ ] `buildSystemPrompt()` 메서드 단순화
4. [ ] prompt 패키지 생성
5. [ ] FeedbackPromptBuilder, QuestionPromptBuilder, InterviewPromptBuilder, EvaluationPromptBuilder 분리
6. [ ] 기존 PromptBuilder 사용처 수정 (AiFeedbackService, InterviewAiService 등)
7. [ ] 테스트 실행 및 검증

**예상 소요**: 4시간 (Phase 1: 2시간, Phase 2: 2시간)
**난이도**: Medium
**위험도**: Medium (여러 Service에서 사용)

---

## Priority 2: High (1주일 내 수정)

### 2.1 MockInterviewService SSE 분리

**현재 상태**
- 파일: `src/main/kotlin/.../service/MockInterviewService.kt`
- 문제: 431줄, SSE 관리 + 비즈니스 로직 혼재
- 영향: 테스트 어려움, 책임 불명확

**Before**
```kotlin
@Service
class MockInterviewService(...) {
    // SSE 관리 (lines 277-335)
    private val emitters = ConcurrentHashMap<Long, SseEmitter>()
    fun registerEmitter(interviewId: Long, emitter: SseEmitter) { ... }
    fun removeEmitter(interviewId: Long) { ... }
    fun broadcastMessage(interviewId: Long, message: InterviewMessage): Boolean { ... }

    // 비즈니스 로직 (lines 54-275)
    fun startInterview(...): MockInterview { ... }
    fun sendUserMessage(...): InterviewMessage { ... }
    fun endInterview(...): MockInterview { ... }

    // AI 응답 (lines 337-412)
    @Async fun generateAndBroadcastAiResponseAsync(...) { ... }
}
```

**After**
```kotlin
// SseEmitterService.kt (신규)
@Service
class SseEmitterService(
    private val meterRegistry: MeterRegistry
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val emitters = ConcurrentHashMap<Long, SseEmitter>()

    fun register(id: Long, emitter: SseEmitter) {
        emitters[id] = emitter
        logger.info("SSE emitter 등록 - id: $id")
        meterRegistry.gauge("sse.active_connections", emitters.size)
    }

    fun remove(id: Long) {
        emitters.remove(id)
        logger.info("SSE emitter 제거 - id: $id")
        meterRegistry.gauge("sse.active_connections", emitters.size)
    }

    fun broadcast(id: Long, event: String, data: Any): Boolean {
        val emitter = emitters[id]
        if (emitter == null) {
            logger.warn("SSE emitter가 없음 - id: $id")
            return false
        }

        return try {
            emitter.send(SseEmitter.event().name(event).data(data))
            meterRegistry.counter("sse.messages_sent").increment()
            true
        } catch (e: IOException) {
            logger.error("SSE 전송 실패 - id: $id", e)
            remove(id)
            meterRegistry.counter("sse.errors").increment()
            false
        }
    }

    fun getEmitter(id: Long): SseEmitter? = emitters[id]
}

// MockInterviewService.kt (리팩토링)
@Service
class MockInterviewService(
    private val sseEmitterService: SseEmitterService,  // SSE 위임
    private val mockInterviewRepository: MockInterviewRepository,
    private val interviewMessageRepository: InterviewMessageRepository,
    private val interviewAiService: InterviewAiService,
    // ...
) {
    // SSE 관련 코드 제거, sseEmitterService 호출로 대체

    fun broadcastMessage(interviewId: Long, message: InterviewMessage): Boolean {
        val messageDto = mapToMessageDto(message)
        return sseEmitterService.broadcast(interviewId, "message", messageDto)
    }

    // 비즈니스 로직만 유지
    fun startInterview(...): MockInterview { ... }
    fun sendUserMessage(...): InterviewMessage { ... }
    fun endInterview(...): MockInterview { ... }
}
```

**작업 단계**
1. [ ] `SseEmitterService.kt` 생성
2. [ ] SSE 관련 메서드 이동 (registerEmitter, removeEmitter, broadcast)
3. [ ] MockInterviewService에서 SseEmitterService 주입
4. [ ] MockInterviewController 수정 (SseEmitter 등록 부분)
5. [ ] 테스트 추가 (SseEmitterService 단위 테스트)

**예상 소요**: 3시간
**난이도**: Medium
**위험도**: Medium (SSE 동작 검증 필요)

---

### 2.2 N+1 쿼리 최적화

**현재 상태**
- 위치: `ReviewService`, `MockInterviewService`
- 문제: 리스트 조회 후 개별 엔티티 추가 조회

**위험 코드**
```kotlin
// ReviewService - 잠재적 N+1
val answers = interviewAnswerRepository.findByUserIdOrderByCreatedAtDesc(userId)
return answers.mapNotNull { answer ->
    // 각 answer마다 question 조회 (N번)
    val question = questionRepository.findById(answer.questionId).orElse(null)
    // 각 answer마다 feedback 조회 (N번)
    val feedback = aiFeedbackRepository.findByInterviewAnswerId(answer.id)
    // ...
}
```

**해결책 1: JOIN FETCH 쿼리**
```kotlin
// InterviewAnswerRepository.kt
@Query("""
    SELECT a FROM InterviewAnswer a
    LEFT JOIN FETCH a.question
    WHERE a.userId = :userId
    ORDER BY a.createdAt DESC
""")
fun findByUserIdWithQuestion(@Param("userId") userId: Long): List<InterviewAnswer>
```

**해결책 2: @EntityGraph**
```kotlin
// InterviewAnswerRepository.kt
@EntityGraph(attributePaths = ["question"])
fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<InterviewAnswer>
```

**해결책 3: 배치 조회 (권장)**
```kotlin
// ReviewService.kt
fun getUserReviews(userId: Long): List<ReviewSummaryDto> {
    val answers = interviewAnswerRepository.findByUserIdOrderByCreatedAtDesc(userId)

    // 배치 조회
    val questionIds = answers.mapNotNull { it.questionId }
    val questions = questionRepository.findAllById(questionIds).associateBy { it.id }

    val generatedQuestionIds = answers.mapNotNull { it.generatedQuestionId }
    val generatedQuestions = generatedQuestionRepository.findAllById(generatedQuestionIds).associateBy { it.id }

    val answerIds = answers.map { it.id }
    val feedbacks = aiFeedbackRepository.findAllByInterviewAnswerIdIn(answerIds).associateBy { it.interviewAnswerId }

    return answers.mapNotNull { answer ->
        val (content, category) = when {
            answer.questionId != null -> questions[answer.questionId]?.let { it.content to it.category }
            answer.generatedQuestionId != null -> generatedQuestions[answer.generatedQuestionId]?.let { it.content to it.category }
            else -> null
        } ?: return@mapNotNull null

        val feedback = feedbacks[answer.id] ?: return@mapNotNull null

        ReviewSummaryDto(
            answerId = answer.id,
            questionContent = content,
            category = category,
            answeredAt = answer.createdAt,
            averageScore = feedback.averageScore
        )
    }
}
```

**작업 단계**
1. [ ] AiFeedbackRepository에 `findAllByInterviewAnswerIdIn()` 추가
2. [ ] ReviewService 배치 조회 방식으로 수정
3. [ ] 쿼리 로그 활성화하여 N+1 해결 확인
4. [ ] 성능 테스트 추가

**예상 소요**: 2시간
**난이도**: Medium
**위험도**: Low (기능 변경 없음)

---

## Priority 3: Medium (시간 여유시)

### 3.1 REST API 일관성 개선

**현재 문제**
```
POST /questions/{id}/answer     → 자원 중심 아님
GET  /answers/{id}/feedback     → 중첩 자원
POST /questions/{id}/draft      → 명확하지 않음
```

**권장 변경**
```
POST /answers                   (body: { questionId, answerText })
GET  /answers/{id}?include=feedback
POST /drafts  또는  PATCH /answers/{id}/draft
```

**주의**: Breaking Change이므로 점진적 마이그레이션 필요

**예상 소요**: 6시간
**난이도**: High (영향 범위 큼)

---

### 3.2 Domain Model 풍부화

**현재 상태**: Anemic Domain Model

```kotlin
// Before: 단순 데이터 홀더
@Entity
class Question(
    val jobField: String,
    val category: String,
    val difficulty: String,
    val content: String,
    val isActive: Boolean = true
)

// After: Rich Domain Model
@Entity
class Question(
    @Column(nullable = false, length = 50)
    val jobField: String,

    @Column(nullable = false, length = 50)
    val category: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val difficulty: Difficulty,  // String → Enum

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    val isActive: Boolean = true
) {
    init {
        require(content.isNotBlank()) { "질문 내용은 비어있을 수 없습니다" }
        require(content.length <= 1000) { "질문 내용은 1000자를 초과할 수 없습니다" }
    }

    fun isEasy(): Boolean = difficulty == Difficulty.EASY
    fun isMedium(): Boolean = difficulty == Difficulty.MEDIUM
    fun isHard(): Boolean = difficulty == Difficulty.HARD

    fun matchesFilter(filterJobField: String?, filterCategory: String?, filterDifficulty: Difficulty?): Boolean {
        return (filterJobField == null || jobField == filterJobField) &&
               (filterCategory == null || category == filterCategory) &&
               (filterDifficulty == null || difficulty == filterDifficulty)
    }
}
```

**예상 소요**: 2시간
**난이도**: Low

---

### 3.3 테스트 피라미드 정상화

**현재**: Unit 40%, Integration 50%
**목표**: Unit 60%, Integration 30%

**필요 작업**
1. Service 단위 테스트 추가
    - `AiFeedbackServiceTest` 확장
    - `MockInterviewServiceTest` 신규
    - `QuestionServiceTest` 확장

2. Integration 테스트 분리
    - Phase1~8 IntegrationTest 중 일부를 Unit으로 변환

**예상 소요**: 4시간
**난이도**: Medium

---

## Refactoring Checklist

### Week 1: Critical Fixes
```
[ ] ReviewService 중복 제거 (1시간)
    [ ] resolveQuestion() 메서드 추출
    [ ] buildReviewSummary() 메서드 추출
    [ ] 기존 테스트 통과 확인

[ ] PromptBuilder Phase 1 (2시간)
    [ ] JobFieldPromptConfig 데이터 클래스 생성
    [ ] feedbackPromptConfigs Map 정의 (17개 직무)
    [ ] buildSystemPrompt() 단순화
    [ ] 테스트 통과 확인
```

### Week 2: High Priority
```
[ ] PromptBuilder Phase 2 (2시간)
    [ ] prompt 패키지 생성
    [ ] FeedbackPromptBuilder 분리
    [ ] QuestionPromptBuilder 분리
    [ ] InterviewPromptBuilder 분리
    [ ] EvaluationPromptBuilder 분리
    [ ] 사용처 수정 (AiFeedbackService 등)

[ ] MockInterviewService SSE 분리 (3시간)
    [ ] SseEmitterService 생성
    [ ] SSE 로직 이동
    [ ] MockInterviewService 수정
    [ ] Controller 수정
    [ ] 테스트 추가
```

### Week 3: Optimization
```
[ ] N+1 쿼리 최적화 (2시간)
    [ ] Repository 메서드 추가
    [ ] 배치 조회 방식 적용
    [ ] 쿼리 로그로 검증

[ ] 코드 정리 (1시간)
    [ ] println() → logger.debug()
    [ ] @Deprecated 메서드 제거
    [ ] 불필요한 주석 정리
```

---

## Verification

### 리팩토링 후 확인사항

1. **빌드 확인**
   ```bash
   ./gradlew clean build
   ```

2. **테스트 실행**
   ```bash
   ./gradlew test
   ```

3. **수동 테스트**
    - 답변 제출 → AI 피드백 확인
    - 모의 면접 시작 → SSE 메시지 확인
    - 리뷰 이력 조회 → 페이지네이션 확인

4. **성능 확인 (N+1)**
   ```properties
   # application-dev.properties
   spring.jpa.show-sql=true
   logging.level.org.hibernate.SQL=DEBUG
   logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
   ```

---

## Risk Mitigation

| 리팩토링 | 위험 | 대응 |
|----------|------|------|
| ReviewService | 낮음 | 기존 테스트 실행 |
| PromptBuilder | 중간 | AI 응답 수동 테스트 |
| SSE 분리 | 중간 | 브라우저 SSE 연결 테스트 |
| N+1 최적화 | 낮음 | 쿼리 로그 확인 |

---

## Expected Results

| 항목 | Before | After |
|------|--------|-------|
| 코드 품질 점수 | 8.0/10 | 9.0+/10 |
| PromptBuilder 줄 수 | 950줄 | ~200줄 × 4 |
| ReviewService 중복률 | 80% | 0% |
| MockInterviewService | 431줄 | ~300줄 |
| N+1 쿼리 | 잠재적 위험 | 해결 |
| 테스트 비율 (Unit) | 40% | 60% |

---

## Appendix: File Locations

### 수정 대상 파일
```
src/main/kotlin/.../service/
├── ReviewService.kt                    # P1: 중복 제거
├── MockInterviewService.kt             # P2: SSE 분리
└── ai/
    └── PromptBuilder.kt                # P1: 클래스 분리

src/main/kotlin/.../repository/
└── AiFeedbackRepository.kt             # P2: 배치 조회 메서드 추가
```

### 신규 생성 파일
```
src/main/kotlin/.../service/
├── SseEmitterService.kt                # P2: SSE 관리
└── ai/prompt/
    ├── FeedbackPromptBuilder.kt
    ├── QuestionPromptBuilder.kt
    ├── InterviewPromptBuilder.kt
    ├── EvaluationPromptBuilder.kt
    └── JobFieldPromptConfig.kt
```

---

**Document Version**: 1.0
**Generated**: 2026-05-18
