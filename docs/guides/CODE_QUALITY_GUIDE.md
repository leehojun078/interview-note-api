# Interview Note API - Code Quality Report

**Version**: 1.0
**Analysis Date**: 2026-05-18
**Analyzer**: Claude Code Architecture Review

---

## Executive Summary

| Metric | Score | Status |
|--------|-------|--------|
| **Overall Architecture** | 8.0 / 10 | Good |
| **SOLID Principles** | 8.5 / 10 | Very Good |
| **Clean Code** | 7.0 / 10 | Needs Improvement |
| **Test Coverage** | 7.0 / 10 | Acceptable |
| **Security** | 8.5 / 10 | Very Good |

**Verdict**: 프로덕션 준비 완료, 리팩토링으로 **9.0+** 달성 가능

---

## 1. SOLID Principles Assessment

### 1.1 Single Responsibility Principle (SRP)

**Grade: B+ (85/100)**

| 파일 | 줄 수 | 책임 수 | 평가 |
|------|-------|---------|------|
| `QuestionService.kt` | 73줄 | 1 (질문 조회) | ✅ 우수 |
| `ReviewService.kt` | 180줄 | 1 (리뷰 조회) | ✅ 우수 |
| `AiFeedbackService.kt` | 243줄 | 1 (AI 평가) | ✅ 우수 |
| `PromptBuilder.kt` | 950줄 | 4+ (피드백/질문/면접/평가 프롬프트) | ❌ 위반 |
| `MockInterviewService.kt` | 431줄 | 2 (세션 관리 + SSE) | ⚠️ 경고 |
| `AnswerController.kt` | 234줄 | 3 (HTTP + 검증 조율 + 캐시) | ⚠️ 경고 |

**위반 사례:**

```kotlin
// PromptBuilder.kt - 4가지 역할 혼재
class PromptBuilder {
    // 1. 피드백 프롬프트 (lines 44-382)
    fun buildSystemPrompt(jobField, targetJob)

    // 2. 질문 생성 프롬프트 (lines 399-598)
    fun buildQuestionGenerationSystemPrompt(...)

    // 3. 면접 진행 프롬프트 (lines 600-763)
    fun buildInterviewSystemPrompt(...)

    // 4. 종합 평가 프롬프트 (lines 771-907)
    fun buildFinalEvaluationPrompt(...)
}
```

**권장 분리:**
- `FeedbackPromptBuilder` - 답변 평가용
- `QuestionPromptBuilder` - 질문 생성용
- `InterviewPromptBuilder` - 면접 진행용

### 1.2 Open/Closed Principle (OCP)

**Grade: A- (88/100)**

**강점:**
```kotlin
// 인터페이스 기반 확장 가능
interface AiClient {
    fun requestFeedback(systemPrompt: String, userPrompt: String): String
}

// 구현체 교체 용이 (OpenAI → Claude/Gemini)
@Service
class OpenAiClientImpl : AiClient { ... }
class ClaudeClientImpl : AiClient { ... }  // 추가 가능
```

**약점:**
```kotlin
// 직무 추가 시 코드 수정 필요
fun buildSystemPrompt(jobField: String, targetJob: String): String {
    return when (jobField) {
        IT -> buildItSystemPrompt(targetJob)
        PLANNING -> buildPlanningSystemPrompt(targetJob)
        // 새 직무 추가 시 여기 수정 필요
    }
}
```

**개선안:** 직무별 프롬프트를 설정 파일로 외부화

### 1.3 Liskov Substitution Principle (LSP)

**Grade: A (92/100)**

```kotlin
// sealed class 활용 - 완벽한 대체 가능
sealed class AiException(message: String, cause: Throwable?) : RuntimeException(message, cause)
class AiApiException(msg: String, cause: Throwable?) : AiException(msg, cause)
class AiResponseParseException(msg: String, val rawResponse: String, cause: Throwable?) : AiException(msg, cause)

// GlobalExceptionHandler에서 부모 타입으로 일관 처리
@ExceptionHandler(AiException::class)
fun handleAiException(e: AiException): String { ... }
```

### 1.4 Interface Segregation Principle (ISP)

**Grade: A- (90/100)**

```kotlin
// AiClient - 최소 인터페이스
interface AiClient {
    fun requestFeedback(systemPrompt: String, userPrompt: String): String
}
// ✅ 단일 메서드, 명확한 계약
```

**개선 가능:**
```kotlin
// 현재: 하나의 메서드에 모든 AI 호출 담당
// 권장: 용도별 분리
interface FeedbackAiClient {
    fun requestFeedback(...): FeedbackResponse
}

interface InterviewAiClient {
    fun requestQuestion(...): QuestionResponse
}
```

### 1.5 Dependency Inversion Principle (DIP)

**Grade: A (92/100)**

```kotlin
// ✅ 인터페이스에 의존
@Service
class AiFeedbackService(
    private val aiClient: AiClient,           // 인터페이스
    private val promptBuilder: PromptBuilder,
    private val responseParser: ResponseParser,
    private val repository: AiFeedbackRepository  // Spring Data JPA 인터페이스
) { ... }

// ✅ 생성자 주입 일관 적용
// ✅ @Autowired 필드 주입 없음
```

---

## 2. Clean Code Metrics

### 2.1 File Size Distribution

| 범위 | 파일 수 | 비율 | 상태 |
|------|---------|------|------|
| 0-100줄 | 35 | 65% | ✅ 이상적 |
| 100-200줄 | 12 | 22% | ✅ 허용 |
| 200-300줄 | 4 | 7% | ⚠️ 주의 |
| 300줄+ | 3 | 6% | ❌ 분리 필요 |

**문제 파일 (300줄 이상):**

| 파일 | 줄 수 | 권장 조치 |
|------|-------|----------|
| `PromptBuilder.kt` | 950줄 | 3-4개 파일로 분리 |
| `MockInterviewService.kt` | 431줄 | SSE 로직 분리 |
| `MockInterviewController.kt` | 358줄 | 헬퍼 메서드 추출 |

### 2.2 Method Complexity

**평균 메서드 길이:** 15줄 (권장: 20줄 이하) ✅

**복잡한 메서드:**

| 메서드 | 줄 수 | 순환복잡도 | 권장 |
|--------|-------|-----------|------|
| `PromptBuilder.buildSystemPrompt()` | 25줄 | 17 (when 분기) | 테이블 기반으로 변경 |
| `AiFeedbackService.generateFeedback()` | 45줄 | 8 | 책임 분리 |
| `MockInterviewService.generateAndBroadcastAiResponseAsync()` | 65줄 | 5 | 적정 |

### 2.3 Cyclomatic Complexity

```
buildSystemPrompt()           : CC = 17 (when 분기 17개) ❌ 너무 높음
generateFeedback()            : CC = 8  ⚠️ 경고
submitAnswer()                : CC = 6  ✅ 적정
parseOpenAiResponse()         : CC = 3  ✅ 우수
```

**권장:** CC > 10인 메서드는 분리 필요

### 2.4 Naming Conventions

**일관성 분석:**

| 패턴 | 예시 | 상태 |
|------|------|------|
| Service 메서드 | `generateFeedback()`, `submitAnswer()` | ✅ 동사 시작 |
| Repository 메서드 | `findByUserId()`, `findAllByOrderByCreatedAtDesc()` | ✅ Spring Data 규칙 |
| DTO 변환 | `FeedbackDto.from(entity)` | ✅ Factory 패턴 |
| Boolean 메서드 | `isGenerated()`, `isActive()` | ✅ is/has 접두사 |

**불일치 발견:**

```kotlin
// 반환 타입 명명 불일치
fun generateFeedback() : AiFeedback              // 엔티티 반환
fun generateFinalEvaluation() : FinalEvaluationResult  // DTO 반환

// 권장: 일관된 접미사 규칙 적용
// - Entity 반환: generate*()
// - DTO 반환: get*Dto() 또는 create*Response()
```

---

## 3. Code Duplication Analysis

### 3.1 High Duplication (80%+)

**ReviewService.kt (lines 40-67, 78-105, 116-143)**

```kotlin
// 3개 메서드에 거의 동일한 코드
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
```

**해결책:**
```kotlin
private fun resolveQuestion(answer: InterviewAnswer): Pair<String, String>? {
    return when {
        answer.questionId != null ->
            questionRepository.findById(answer.questionId).orElse(null)
                ?.let { it.content to it.category }
        answer.generatedQuestionId != null ->
            generatedQuestionRepository.findById(answer.generatedQuestionId).orElse(null)
                ?.let { it.content to it.category }
        else -> null
    }
}
```

### 3.2 Medium Duplication (60%+)

**PromptBuilder.kt - 17개 직무별 메서드**

```kotlin
// 패턴 반복 (17회)
private fun buildItSystemPrompt(targetJob: String) =
    buildBasePrompt(targetJob, "기술적 사고...", "구체적 기술 스택...", listOf(...))

private fun buildSalesSystemPrompt(targetJob: String) =
    buildBasePrompt(targetJob, "영업 전략...", "구체적 실적...", listOf(...))
// ... 15개 더
```

**해결책:**
```kotlin
data class JobFieldConfig(
    val logicDescription: String,
    val specificityDescription: String,
    val badExamples: List<String>
)

private val jobFieldConfigs = mapOf(
    "IT" to JobFieldConfig("기술적 사고...", "구체적 기술 스택...", listOf(...)),
    "SALES" to JobFieldConfig("영업 전략...", "구체적 실적...", listOf(...)),
    // ...
)

fun buildSystemPrompt(jobField: String, targetJob: String): String {
    val config = jobFieldConfigs[jobField] ?: throw IllegalArgumentException()
    return buildBasePrompt(targetJob, config)
}
```

### 3.3 Duplication Summary

| 영역 | 중복률 | 영향 | 수정 난이도 |
|------|--------|------|------------|
| ReviewService | 80% | Medium | Easy (1h) |
| PromptBuilder | 60% | High | Medium (4h) |
| Controller 검증 | 30% | Low | Low (2h) |

---

## 4. Test Coverage Analysis

### 4.1 Coverage Summary

| Layer | 파일 수 | 테스트 파일 수 | 커버리지 추정 |
|-------|---------|---------------|--------------|
| Service | 12 | 8 | ~75% |
| Controller | 11 | 6 | ~60% |
| Repository | 9 | 3 | ~100% (Spring Data) |
| Domain | 13 | 1 | ~40% |
| AI Integration | 6 | 4 | ~80% |

### 4.2 Test Pyramid

```
Current State:                    Ideal State:

      ┌────────┐                        ┌────────┐
      │  E2E   │ 10%                    │  E2E   │ 10%
      │        │                        │        │
    ┌─┴────────┴─┐                    ┌─┴────────┴─┐
    │ Integration│ 50% ❌ 너무 많음    │ Integration│ 30%
    │            │                    │            │
  ┌─┴────────────┴─┐                ┌─┴────────────┴─┐
  │     Unit       │ 40%            │     Unit       │ 60%
  │                │                │                │
  └────────────────┘                └────────────────┘
```

**문제:** 테스트 피라미드 역전 (Integration > Unit)

### 4.3 Test Quality

**Good Practices:**
```kotlin
// ✅ 명확한 테스트명 (Kotlin backtick)
@Test
fun `정상적인 답변 제출시 피드백 페이지로 리다이렉트한다`() { ... }

// ✅ AAA 패턴 (Arrange-Act-Assert)
@Test
fun `캐시된 피드백이 있으면 AI 호출하지 않는다`() {
    // Arrange
    val cached = createCachedFeedback()
    whenever(cache.find(...)).thenReturn(cached)

    // Act
    val result = service.generateFeedback(answer, question)

    // Assert
    verify(aiClient, never()).requestFeedback(any(), any())
    assertThat(result.id).isEqualTo(cached.id)
}
```

**Missing Tests:**

| 영역 | 누락된 테스트 | 우선순위 |
|------|--------------|----------|
| MockInterviewService | SSE 스트림 테스트 | High |
| ReviewService | 페이지네이션 경계 테스트 | Medium |
| PromptBuilder | 17개 직무 프롬프트 테스트 | Medium |
| Domain | Validation 테스트 | Low |

---

## 5. Architecture Score Card

| 항목 | 점수 | 상세 |
|------|------|------|
| **Layer Separation** | 9/10 | Controller → Service → Repository 명확 |
| **Dependency Direction** | 9/10 | 순환 참조 없음, 단방향 |
| **Domain Independence** | 8/10 | Domain이 외부 의존 없음, 일부 Anemic |
| **Interface Abstraction** | 9/10 | AiClient, Repository 인터페이스 |
| **Exception Handling** | 9/10 | sealed class, GlobalExceptionHandler |
| **Logging/Monitoring** | 8/10 | MDC, Micrometer, Prometheus |
| **Security** | 8.5/10 | BCrypt, CSRF, Rate Limiting |
| **Caching Strategy** | 7/10 | 중복 방지만, Redis 미도입 |
| **API Consistency** | 6/10 | REST 규칙 일부 위반 |

**종합: 8.0/10**

---

## 6. Technical Debt Inventory

### TD-001: PromptBuilder 거대 파일

| 항목 | 내용 |
|------|------|
| **위치** | `src/.../service/ai/PromptBuilder.kt` |
| **현황** | 950줄, 4가지 역할 혼재 |
| **영향** | 유지보수 어려움, SRP 위반 |
| **수정 방안** | 3개 클래스로 분리 + Config Enum 도입 |
| **예상 소요** | 4시간 |
| **우선순위** | P1 (Critical) |

### TD-002: ReviewService 코드 중복

| 항목 | 내용 |
|------|------|
| **위치** | `src/.../service/ReviewService.kt:40-143` |
| **현황** | 3개 메서드에 80% 동일 코드 |
| **영향** | DRY 위반, 수정 시 3곳 변경 필요 |
| **수정 방안** | `resolveQuestion()` 공통 메서드 추출 |
| **예상 소요** | 1시간 |
| **우선순위** | P1 (Critical) |

### TD-003: MockInterviewService 혼재

| 항목 | 내용 |
|------|------|
| **위치** | `src/.../service/MockInterviewService.kt` |
| **현황** | 431줄, SSE 관리 + 비즈니스 로직 |
| **영향** | 테스트 어려움, 책임 불명확 |
| **수정 방안** | `SseEmitterService` 분리 |
| **예상 소요** | 3시간 |
| **우선순위** | P2 (High) |

### TD-004: N+1 쿼리 잠재적 위험

| 항목 | 내용 |
|------|------|
| **위치** | `ReviewService`, `MockInterviewRepository` |
| **현황** | 리스트 조회 후 개별 엔티티 fetch |
| **영향** | 대량 데이터 시 성능 저하 |
| **수정 방안** | @EntityGraph, JOIN FETCH 적용 |
| **예상 소요** | 2시간 |
| **우선순위** | P2 (High) |

### TD-005: Anemic Domain Model

| 항목 | 내용 |
|------|------|
| **위치** | `domain/Question.kt`, `domain/InterviewAnswer.kt` |
| **현황** | 데이터만 보유, 비즈니스 로직 없음 |
| **영향** | 로직이 Service에 분산 |
| **수정 방안** | 도메인 메서드 추가 (validate, isXxx) |
| **예상 소요** | 2시간 |
| **우선순위** | P3 (Medium) |

### TD-006: 테스트 피라미드 역전

| 항목 | 내용 |
|------|------|
| **위치** | `src/test/kotlin/...` |
| **현황** | Unit 40%, Integration 50% |
| **영향** | 테스트 실행 시간 증가, 디버깅 어려움 |
| **수정 방안** | Service 단위 테스트 추가 |
| **예상 소요** | 4시간 |
| **우선순위** | P3 (Medium) |

---

## 7. Code Smells Summary

### 7.1 Critical (즉시 수정)

| Code Smell | 위치 | 해결책 |
|------------|------|--------|
| **God Class** | PromptBuilder.kt | 3개 클래스로 분리 |
| **Duplicated Code** | ReviewService.kt | 공통 메서드 추출 |
| **Large Method** | buildSystemPrompt() | 테이블 기반으로 변경 |

### 7.2 Warning (1주일 내 수정)

| Code Smell | 위치 | 해결책 |
|------------|------|--------|
| **Feature Envy** | MockInterviewService SSE | 별도 서비스 분리 |
| **Primitive Obsession** | difficulty: String | Enum으로 변경 |
| **Dead Code** | @Deprecated getReviewList() | 제거 검토 |

### 7.3 Info (시간 여유시)

| Code Smell | 위치 | 해결책 |
|------------|------|--------|
| **Comments** | println() 디버그 | logger.debug() 변경 |
| **Magic Numbers** | MAX_TURNS = 30 | 설정 파일로 이동 |
| **Long Parameter List** | buildFinalEvaluationPrompt(4 params) | Builder 패턴 |

---

## 8. Recommendations

### 8.1 Immediate Actions (This Week)

1. **ReviewService 중복 제거** (1시간)
    - `resolveQuestion()` 메서드 추출
    - 테스트 추가

2. **PromptBuilder 분리 Phase 1** (2시간)
    - JobFieldConfig Enum 도입
    - when 분기 → Map 조회로 변경

3. **디버그 코드 정리** (30분)
    - `println()` → `logger.debug()`
    - 불필요한 로그 제거

### 8.2 Short-term Actions (This Month)

1. **PromptBuilder 분리 Phase 2** (2시간)
    - FeedbackPromptBuilder, QuestionPromptBuilder, InterviewPromptBuilder 분리

2. **MockInterviewService SSE 분리** (3시간)
    - SseEmitterService 생성
    - 비즈니스 로직과 분리

3. **N+1 쿼리 최적화** (2시간)
    - @EntityGraph 적용
    - 성능 테스트 추가

### 8.3 Long-term Improvements

1. **테스트 피라미드 정상화** (4시간)
    - Service 단위 테스트 추가
    - Integration 테스트 비율 감소

2. **REST API 일관성** (6시간)
    - 엔드포인트 리팩토링
    - 점진적 마이그레이션

3. **Domain Model 풍부화** (2시간)
    - 비즈니스 메서드 추가
    - Validation 도메인 이동

---

## 9. Conclusion

### 강점

1. ✅ **명확한 레이어 분리** - Controller → Service → Repository 패턴 준수
2. ✅ **인터페이스 추상화** - AiClient로 AI 구현체 교체 용이
3. ✅ **예외 처리 체계** - sealed class 기반 타입 안전한 예외
4. ✅ **보안 설계** - BCrypt, CSRF, Rate Limiting 적용
5. ✅ **모니터링 인프라** - Prometheus 메트릭, 구조화 로깅

### 개선 필요

1. ⚠️ **거대 파일** - PromptBuilder 950줄 분리 필요
2. ⚠️ **코드 중복** - ReviewService 80% 중복 제거
3. ⚠️ **테스트 피라미드** - Unit/Integration 비율 조정
4. ⚠️ **REST 일관성** - 엔드포인트 규칙 통일

### 최종 평가

**현재 점수: 8.0/10**

리팩토링 후 예상 점수: **9.0+/10**

이 프로젝트는 **시니어 개발자 포트폴리오 수준**에 부합하며, 제안된 리팩토링을 수행하면 **업계 표준 이상의 코드 품질**을 달성할 수 있습니다.

---

**Report Generated**: 2026-05-18
**Reviewed By**: Claude Code Architecture Analysis
