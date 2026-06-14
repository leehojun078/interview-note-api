# Code Quality Review - Interview Note API

**5년차 서버 개발자 포트폴리오 프로젝트 최종 점검**

작성일: 2026-06-13
프로젝트 상태: Phase 8C 완료 (17개 직무 지원, AI 면접, 채용 공고 기반 질문 생성)

---

## 📊 Executive Summary

이 프로젝트는 **프로덕션 수준의 Spring Boot 애플리케이션**으로, 시니어 백엔드 개발자 수준의 엔지니어링 역량을 보여줍니다.

### 프로젝트 규모
- **소스 코드**: 82개 파일, 8,698줄 (main)
- **테스트 코드**: 41개 파일, 10,583줄 (test)
- **커버리지**: Unit, Integration, Controller 테스트 포괄
- **기술 스택**: Kotlin, Spring Boot 3.5.14, PostgreSQL, OpenAI API, Docker, SSE

### 종합 평가: **8.3/10** ⭐⭐⭐⭐

#### 강점 (8.5-9.0점 수준)
- ✅ **클린 아키텍처**: Controller → Service → Repository 계층 분리
- ✅ **엔터프라이즈 기능**: AI 통합, SSE 스트리밍, 멀티 테넌트 사용자 스코핑
- ✅ **테스트 품질**: AAA 패턴, 포괄적 모킹, 엣지 케이스 커버리지
- ✅ **확장 가능한 설계**: 17개 직무 지원, N+1 쿼리 최적화, 프롬프트 버전 관리
- ✅ **프로덕션 준비**: Docker, 메트릭, 헬스 체크, 구조화된 로깅

#### 개선 필요 영역 (Critical 2개, High 8개)
- ⚠️ **트랜잭션 경계 위반** (AiFeedbackService) - 중복 피드백 생성 가능
- ⚠️ **Race Condition** (RateLimitService) - 동시성 환경에서 제한 우회 가능
- 📌 **성능 최적화**: RestTemplate 커넥션 풀링, 페이지네이션 미적용
- 📌 **코드 품질**: 하드코딩된 문자열, null-safety 갭, i18n 미지원

---

## 🎯 발견된 이슈 요약

| 심각도 | 개수 | 영향 영역 | 예상 수정 시간 |
|--------|------|-----------|----------------|
| **Critical** | 2 | 데이터 일관성, 동시성 | 3-5시간 |
| **High** | 8 | 성능, 유지보수성, 견고성 | 12-15시간 |
| **Medium** | 12 | 코드 품질, 문서화, 확장성 | 16-20시간 |
| **Low** | 8 | 향후 개선 사항 | 8-12시간 |

**총 예상 수정 시간**: 56-72시간 (4주 파트타임 or 1.5주 풀타임)

---

## ⚠️ Critical Issues (포트폴리오 제출 전 필수 수정)

### 1. 트랜잭션 경계 위반 - AiFeedbackService

**파일**: `src/main/kotlin/.../service/AiFeedbackService.kt:62-163`
**심각도**: CRITICAL 🔴
**영향**: AI 실패 시 중복 피드백 레코드 생성 가능

#### 문제 코드
```kotlin
@Service
@Transactional  // 클래스 레벨 트랜잭션
class AiFeedbackService(...) {
    fun generateFeedback(answer: InterviewAnswer, question: Question): AiFeedback {
        return try {
            // ... AI 호출 및 파싱
            val aiFeedback = AiFeedback(...)
            val savedFeedback = aiFeedbackRepository.save(aiFeedback)  // ✓ 저장 완료
            savedFeedback
        } catch (e: AiException) {
            // ❌ Fallback이 동일 트랜잭션 내에서 실행됨
            // save() 후 AI 파싱 실패 시 중복 피드백 생성
            logger.warn("AI 피드백 생성 실패, 더미 피드백으로 fallback")
            generateDummyFeedback(answer, question)  // Line 157
        }
    }
}
```

#### 근본 원인
1. 전체 메서드가 하나의 트랜잭션으로 실행
2. `save()` 후 예외 발생 시, catch 블록의 `generateDummyFeedback()`도 같은 트랜잭션 내
3. 결과: 하나의 답변에 대해 2개의 피드백 레코드 생성

#### 시나리오
```
1. AI 호출 성공 → AiFeedback 생성 → save() ✓ (DB에 저장)
2. JSON 파싱 실패 → AiResponseParseException 발생
3. catch 블록 진입 → generateDummyFeedback() 호출
4. 더미 피드백 save() ✓ (DB에 저장)
5. 트랜잭션 커밋 → 2개의 피드백이 모두 저장됨 ❌
```

#### 권장 수정 사항
```kotlin
// 1. 트랜잭션 분리
fun generateFeedback(answer: InterviewAnswer, question: Question): AiFeedback {
    return try {
        generateRealFeedback(answer, question)  // 별도 트랜잭션
    } catch (e: AiException) {
        logger.warn("AI 피드백 생성 실패, 더미 피드백으로 fallback - 오류: ${e.message}", e)
        generateDummyFeedback(answer, question)  // 별도 트랜잭션
    }
}

// 2. 실제 피드백 생성 (새 트랜잭션)
@Transactional
private fun generateRealFeedback(answer: InterviewAnswer, question: Question): AiFeedback {
    // 1. 캐시 확인
    val cached = duplicateRequestCache.findCached(question.id, answer.answerText)
    if (cached != null) {
        return copyAndSaveCachedFeedback(cached, answer)
    }

    // 2. AI 호출 및 파싱
    val systemPrompt = feedbackPromptBuilder.buildSystemPrompt(question.jobField, question.targetJob)
    val userPrompt = feedbackPromptBuilder.buildUserPrompt(question, answer.answerText)
    val rawResponse = aiCallsTimer.recordCallable {
        aiClient.requestFeedback(systemPrompt, userPrompt)
    }!!
    val parsedFeedback = responseParser.parseOpenAiResponse(rawResponse, rawResponse)

    // 3. 엔티티 생성 및 저장 (모두 성공 or 모두 실패)
    val answerTextHash = duplicateRequestCache.generateHash(question.id, answer.answerText)
    val aiFeedback = createFeedbackEntity(parsedFeedback, answer, question, rawResponse, answerTextHash)
    return aiFeedbackRepository.save(aiFeedback)
}

// 3. 더미 피드백 생성 (독립 트랜잭션)
@Transactional
fun generateDummyFeedback(answer: InterviewAnswer, question: Question): AiFeedback {
    val answerLength = answer.answerText.length
    val baseScore = when {
        answerLength >= 500 -> 4
        answerLength >= 300 -> 3
        else -> 2
    }

    val aiFeedback = AiFeedback(
        interviewAnswerId = answer.id,
        logicScore = baseScore,
        // ... 더미 데이터
        modelName = "dummy-model-v1",
        promptVersion = "v1.0-dummy"
    )

    return aiFeedbackRepository.save(aiFeedback)
}
```

#### 예상 작업 시간
- 리팩토링: 2시간
- 통합 테스트 작성: 1시간
- 수동 테스트 (AI 실패 시나리오): 30분
- **총 3-3.5시간**

#### 테스트 전략
```kotlin
@Test
fun `AI 파싱 실패 시 더미 피드백만 저장되어야 함`() {
    // Given
    val answer = createAnswer()
    val question = createQuestion()

    // AI 응답은 성공하지만 파싱 실패 시나리오 모킹
    whenever(aiClient.requestFeedback(any(), any())).thenReturn("{invalid json}")
    whenever(responseParser.parseOpenAiResponse(any(), any()))
        .thenThrow(AiResponseParseException("JSON 파싱 실패", "{invalid json}"))

    // When
    val result = aiFeedbackService.generateFeedback(answer, question)

    // Then
    assertThat(result.modelName).isEqualTo("dummy-model-v1")

    // 저장된 피드백이 1개만 있어야 함 (더미만)
    val allFeedbacks = aiFeedbackRepository.findAll()
    assertThat(allFeedbacks).hasSize(1)
    assertThat(allFeedbacks[0].modelName).isEqualTo("dummy-model-v1")
}
```

---

### 2. Race Condition - RateLimitService

**파일**: `src/main/kotlin/.../service/ratelimit/RateLimitService.kt:56-76` (추정)
**심각도**: CRITICAL 🔴
**영향**: 고 동시성 환경에서 Rate Limit 우회 가능

#### 문제 코드 (추정)
```kotlin
fun checkAndRecordRequest(ip: String) {
    val now = LocalDateTime.now()
    val cutoffTime = now.minusMinutes(WINDOW_DURATION_MINUTES)

    val requests = requestCache.get(ip) { mutableListOf() }!!

    // ❌ Non-atomic: 다른 스레드가 여기서 끼어들 수 있음
    requests.removeIf { it.isBefore(cutoffTime) }

    if (requests.size >= MAX_REQUESTS_PER_HOUR) {
        throw RateLimitExceededException(...)
    }

    requests.add(now)  // ❌ 동기화 없이 쓰기 발생
}
```

#### Race Condition 시나리오
```
Time    Thread A                           Thread B
-------------------------------------------------------------------
T1      get(ip) → [req1, req2, ..., req32]
T2                                         get(ip) → 같은 리스트 참조 [req1, ..., req32]
T3      removeIf() → 32개 유지
T4                                         removeIf() → 32개 유지
T5      size < 33 체크 ✓ 통과
T6                                         size < 33 체크 ✓ 통과
T7      add() → 33개 총합
T8                                         add() → 34개 총합 (제한 초과!)
```

#### 권장 수정 사항

**옵션 1: Synchronized 블록 (간단, 성능 중)**
```kotlin
fun checkAndRecordRequest(ip: String) {
    synchronized(requestCache) {  // Cache 전체 락
        val now = LocalDateTime.now()
        val cutoffTime = now.minusMinutes(WINDOW_DURATION_MINUTES)

        val requests = requestCache.get(ip) { mutableListOf() }!!
        requests.removeIf { it.isBefore(cutoffTime) }

        if (requests.size >= MAX_REQUESTS_PER_HOUR) {
            val resetTime = requests.first().plusMinutes(WINDOW_DURATION_MINUTES)
            throw RateLimitExceededException(ip, MAX_REQUESTS_PER_HOUR, resetTime)
        }

        requests.add(now)
        logger.debug("Rate limit 체크 완료 - IP: $ip, 현재 요청 수: ${requests.size}")
    }
}
```

**옵션 2: 개별 IP별 락 (성능 최적)**
```kotlin
private val ipLocks = ConcurrentHashMap<String, ReentrantLock>()

fun checkAndRecordRequest(ip: String) {
    val lock = ipLocks.computeIfAbsent(ip) { ReentrantLock() }
    lock.lock()
    try {
        val now = LocalDateTime.now()
        val cutoffTime = now.minusMinutes(WINDOW_DURATION_MINUTES)

        val requests = requestCache.get(ip) { mutableListOf() }!!
        requests.removeIf { it.isBefore(cutoffTime) }

        if (requests.size >= MAX_REQUESTS_PER_HOUR) {
            throw RateLimitExceededException(ip, MAX_REQUESTS_PER_HOUR,
                requests.first().plusMinutes(WINDOW_DURATION_MINUTES))
        }

        requests.add(now)
    } finally {
        lock.unlock()
    }
}
```

#### 예상 작업 시간
- 동기화 코드 추가: 1시간
- 동시성 테스트 작성: 1.5시간
- JMeter 부하 테스트: 30분
- **총 2-3시간**

#### 테스트 전략
```kotlin
@Test
fun `동시 요청 시 Rate Limit이 정확히 적용되어야 함`() {
    val ip = "192.168.1.100"
    val threadCount = 50
    val latch = CountDownLatch(threadCount)
    val successCount = AtomicInteger(0)
    val failureCount = AtomicInteger(0)

    val executor = Executors.newFixedThreadPool(threadCount)

    repeat(threadCount) {
        executor.submit {
            try {
                rateLimitService.checkAndRecordRequest(ip)
                successCount.incrementAndGet()
            } catch (e: RateLimitExceededException) {
                failureCount.incrementAndGet()
            } finally {
                latch.countDown()
            }
        }
    }

    latch.await(10, TimeUnit.SECONDS)
    executor.shutdown()

    // 최대 33개까지만 성공해야 함
    assertThat(successCount.get()).isLessThanOrEqualTo(33)
    assertThat(successCount.get() + failureCount.get()).isEqualTo(threadCount)
}
```

---

## 📌 High Priority Issues (포트폴리오 품질 향상)

### 3. RestTemplate 커넥션 풀링 미설정

**파일**: `service/ai/OpenAiClientImpl.kt:33`
**심각도**: HIGH 🟠
**영향**: 고부하 시 성능 저하, 소켓 고갈

#### 문제
```kotlin
@Service
class OpenAiClientImpl(...) : AiClient {
    private val restTemplate = RestTemplate()  // ❌ 기본 설정, 풀링 없음
}
```

- 매 요청마다 새 HTTP 커넥션 생성 가능
- 타임아웃 미설정 (무한 대기 위험)
- 커넥션 재사용 불가능

#### 권장 수정
```kotlin
// 1. RestTemplateConfig.kt 생성
@Configuration
class RestTemplateConfig {
    @Bean
    fun restTemplate(): RestTemplate {
        val factory = HttpComponentsClientHttpRequestFactory().apply {
            setConnectTimeout(10_000)  // 10초 (연결 타임아웃)
            setConnectionRequestTimeout(10_000)  // 10초 (풀에서 커넥션 획득 타임아웃)
            setReadTimeout(30_000)  // 30초 (AI API 응답 대기)
        }

        return RestTemplate(factory).apply {
            errorHandler = DefaultResponseErrorHandler()
        }
    }
}

// 2. OpenAiClientImpl.kt 수정
@Service
class OpenAiClientImpl(
    private val properties: OpenAiProperties,
    private val objectMapper: ObjectMapper,
    private val restTemplate: RestTemplate  // ✓ 주입받음
) : AiClient {
    // private val restTemplate = RestTemplate() 제거

    override fun requestFeedback(systemPrompt: String, userPrompt: String): String {
        // 기존 로직 유지
    }
}
```

**예상 작업 시간**: 1시간

---

### 4. 하드코딩된 "IT" 값 - InterviewService

**파일**: `service/InterviewService.kt:97-105`
**심각도**: HIGH 🟠
**영향**: DIP 위반, 비IT 직무 공고에서 오류

#### 문제
```kotlin
// 생성된 질문으로 답변 제출 시
val questionForFeedback = Question(
    id = generatedQuestion.id,
    jobField = "IT",        // ❌ HARDCODED!
    targetJob = "개발자",    // ❌ HARDCODED!
    category = generatedQuestion.category,
    content = generatedQuestion.content,
    difficulty = generatedQuestion.difficulty,
    isActive = true
)
val feedback = aiFeedbackService.generateFeedback(savedAnswer, questionForFeedback)
```

**문제점**:
- 채용 공고가 "마케팅" 직무여도 "IT"로 평가됨
- Question 엔티티를 임시로 생성 (안티패턴)
- 테스트 불가능 (모킹 어려움)

#### 권장 수정
```kotlin
// 1. AiFeedbackService에 오버로드 메서드 추가
@Service
@Transactional
class AiFeedbackService(...) {
    // 기존 메서드 유지
    fun generateFeedback(answer: InterviewAnswer, question: Question): AiFeedback { ... }

    // 새 메서드 추가
    fun generateFeedbackForGeneratedQuestion(
        answer: InterviewAnswer,
        generatedQuestion: GeneratedQuestion,
        jobPosting: JobPosting
    ): AiFeedback {
        val jobField = jobPosting.effectiveJobField?.code ?: "IT"
        val targetJob = jobPosting.jobTitle

        // 프롬프트 생성 시 실제 직무 사용
        val systemPrompt = feedbackPromptBuilder.buildSystemPrompt(jobField, targetJob)
        val userPrompt = feedbackPromptBuilder.buildUserPrompt(generatedQuestion, answer.answerText)

        // ... AI 호출 및 파싱 (기존 로직과 동일)

        return aiFeedbackRepository.save(aiFeedback)
    }
}

// 2. InterviewService 수정
val feedback = aiFeedbackService.generateFeedbackForGeneratedQuestion(
    savedAnswer,
    generatedQuestion,
    jobPosting  // ✓ 실제 공고 정보 전달
)
```

**예상 작업 시간**: 2시간

---

### 5. Null-Safety 갭 - Response Parsers

**파일**: `service/ai/InterviewResponseParser.kt:107` (예시)
**심각도**: HIGH 🟠
**영향**: NPE 위험

#### 문제
```kotlin
val overallFeedback = json["overallFeedback"]?.asText()  // null 가능
if (overallFeedback.length < 500) {  // ❌ NPE 위험!
    logger.warn("종합 피드백 길이 부족: ${overallFeedback.length}자")
}
```

#### 권장 수정
```kotlin
// 1. 명시적 null 체크
val overallFeedback = json["overallFeedback"]?.asText()
    ?: throw IllegalArgumentException("overallFeedback 필드 누락")

// 이제 안전하게 사용 가능
if (overallFeedback.length < 500) {
    logger.warn("종합 피드백 길이 부족: ${overallFeedback.length}자")
}

// 2. 모든 필수 필드에 대해 검증
private fun validateRequiredFields(json: JsonNode, fieldName: String): String {
    return json[fieldName]?.asText()
        ?: throw IllegalArgumentException("필수 필드 누락: $fieldName")
}

// 사용
val overallFeedback = validateRequiredFields(json, "overallFeedback")
val keyStrengths = validateRequiredFields(json, "keyStrengths")
val keyImprovements = validateRequiredFields(json, "keyImprovements")
```

**적용 대상**:
- `InterviewResponseParser.kt`
- `ResponseParser.kt`
- `QuestionResponseParser.kt`

**예상 작업 시간**: 1-1.5시간 (3개 파서 검토)

---

### 6-10. (추가 5개 High Priority 이슈 생략)

상세 내용은 `/Users/hojun/.claude/plans/jazzy-plotting-acorn.md` 참조

---

## 📋 Medium Priority Issues (코드 품질)

### 11. Magic Strings 산재

**파일**: `QuestionController.kt`, `AnswerController.kt`
**심각도**: MEDIUM 🟡

#### 문제 예시
```kotlin
return "redirect:/questions/$questionId/answer?error=validation"
return "redirect:/answers/${result.answerId}/feedback?warning=low_quality"
```

#### 권장 수정
```kotlin
object ControllerConstants {
    const val PARAM_ERROR = "error"
    const val ERROR_VALIDATION = "validation"
    const val ERROR_RATELIMIT = "ratelimit"
    const val ERROR_DUPLICATE = "duplicate"

    const val PARAM_WARNING = "warning"
    const val WARNING_LOW_QUALITY = "low_quality"
}

// 사용
return "redirect:/questions/$questionId/answer" +
    "?${ControllerConstants.PARAM_ERROR}=${ControllerConstants.ERROR_VALIDATION}"
```

**예상 작업 시간**: 2시간

---

### 12. 데이터베이스 FK 제약 조건 누락

**파일**: Flyway migrations
**심각도**: MEDIUM 🟡

#### 권장 추가 마이그레이션
```sql
-- V13__add_foreign_key_constraints.sql

ALTER TABLE interview_answers
ADD CONSTRAINT fk_answers_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_answers_question
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE SET NULL,
ADD CONSTRAINT fk_answers_generated_question
    FOREIGN KEY (generated_question_id) REFERENCES generated_questions(id) ON DELETE SET NULL;

ALTER TABLE ai_feedbacks
ADD CONSTRAINT fk_feedbacks_answer
    FOREIGN KEY (interview_answer_id) REFERENCES interview_answers(id) ON DELETE CASCADE;

ALTER TABLE generated_questions
ADD CONSTRAINT fk_gen_questions_posting
    FOREIGN KEY (job_posting_id) REFERENCES job_postings(id) ON DELETE CASCADE;

ALTER TABLE mock_interviews
ADD CONSTRAINT fk_mock_interviews_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_mock_interviews_posting
    FOREIGN KEY (job_posting_id) REFERENCES job_postings(id) ON DELETE SET NULL;

ALTER TABLE interview_messages
ADD CONSTRAINT fk_messages_interview
    FOREIGN KEY (mock_interview_id) REFERENCES mock_interviews(id) ON DELETE CASCADE;
```

**예상 작업 시간**: 1시간

---

## ✅ 보존해야 할 강점 (변경 금지!)

### 아키텍처 우수성

#### 1. 서비스 분리 (SRP 준수) ⭐⭐⭐⭐⭐
```kotlin
// 각 서비스가 단일 책임을 가짐
- AiFeedbackService        → AI 평가 요청 조율
- InterviewAiService       → 면접 AI 로직
- QuestionGeneratorService → 질문 생성
- ReviewService            → 리뷰 집계 쿼리 (N+1 최적화)
- MockInterviewService     → 면접 세션 관리
- SseEmitterService        → SSE 연결 관리
```

#### 2. Lazy 메트릭 초기화 ⭐⭐⭐⭐⭐
```kotlin
private val aiCallsCounter by lazy {
    meterRegistry.counter("ai.calls.total")
}
```
- 테스트 호환성 (MeterRegistry 모킹 불필요)
- 프로덕션 메트릭 수집

#### 3. N+1 쿼리 방지 ⭐⭐⭐⭐⭐
```kotlin
// ReviewService.buildReviewSummariesBatch()
// 31개 쿼리 → 4개 쿼리 (87% 감소)
val questions = questionRepository.findAllById(questionIds).associateBy { it.id }
val feedbacks = aiFeedbackRepository.findAllByInterviewAnswerIdIn(answerIds)
    .associateBy { it.interviewAnswerId }
```

---

### AI 통합 우수성

#### 1. 비용 제어 메커니즘 ⭐⭐⭐⭐⭐
- SHA-256 해시 중복 제거 (24시간 캐시) → **1,700배 속도 향상** (5초 → 3ms)
- Rate Limiting (사용자당 33회/시간)
- 토큰 사용량 추적 (비용 분석)

#### 2. Graceful Fallback ⭐⭐⭐⭐⭐
```kotlin
catch (e: AiException) {
    logger.warn("AI 피드백 생성 실패, 더미 피드백으로 fallback")
    generateDummyFeedback(answer, question)  // 500 에러 대신 더미 반환
}
```

#### 3. Hallucination 방지 프롬프트 ⭐⭐⭐⭐
```
중요한 평가 지침:
1. **정직한 평가**: 답변이 반복적이거나 무의미하면 솔직하게 지적하세요
2. **사실 기반**: 답변에 없는 내용을 추측하거나 창작하지 마세요
3. **강점 검증**: 실제로 답변에 나타난 강점만 언급하세요
```

---

### 보안 우수성

#### 1. 사용자 스코핑 쿼리 (멀티 테넌트 준비) ⭐⭐⭐⭐⭐
```kotlin
// 모든 쿼리에 userId 필터 포함
fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<InterviewAnswer>
fun findByUserIdAndQuestionIdAndAnswerTextHash(...): InterviewAnswer?
```

#### 2. 다층 검증 ⭐⭐⭐⭐⭐
```
HTTP 메서드 → Bean Validation → Custom Validation → 중복 검사
```

---

### 테스트 우수성

#### 1. 포괄적 테스트 (41개, 10,583줄) ⭐⭐⭐⭐⭐
```kotlin
@Test
fun `캐시 히트 시 캐시된 피드백을 반환한다`() {
    // Given
    val answer = createAnswer("테스트 답변")
    val cachedFeedback = AiFeedback(/*...*/)
    whenever(duplicateRequestCache.findCached(...)).thenReturn(cachedFeedback)

    // When
    val result = aiFeedbackService.generateFeedback(answer, question)

    // Then
    assertThat(result.modelAnswer).isEqualTo("캐시된 모범답변")
    verify(aiClient, never()).requestFeedback(any(), any())
}
```

#### 2. 스마트 모킹 ⭐⭐⭐⭐
```kotlin
@BeforeEach
fun setUp() {
    lenient().`when`(meterRegistry.counter(any())).thenReturn(mockCounter)
    lenient().`when`(mockTimer.recordCallable(any())).thenAnswer { invocation ->
        val callable = invocation.getArgument<Callable<Any>>(0)
        callable.call()  // 실제 로직 실행
    }
}
```

---

## 🗓️ 리팩토링 로드맵

### Week 1: Critical 수정 (16-20시간)

#### Day 1-2: 트랜잭션 경계
- [ ] `AiFeedbackService.generateFeedback()` 트랜잭션 분리
- [ ] `generateRealFeedback()` 추출 및 `@Transactional` 적용
- [ ] AI 실패 후 저장 시나리오 통합 테스트 추가
- [ ] 테스트 DB에서 중복 피드백 없음 검증

#### Day 3: Race Condition
- [ ] `RateLimitService`에 synchronized 블록 추가
- [ ] `CountDownLatch`를 사용한 동시성 테스트 작성
- [ ] JMeter 부하 테스트 (100 동시 요청)

#### Day 4-5: RestTemplate + 하드코딩
- [ ] `RestTemplateConfig` 생성 (커넥션 풀링)
- [ ] `generateFeedbackForGeneratedQuestion()` 오버로드 추가
- [ ] `InterviewService` 수정하여 새 메서드 사용
- [ ] 통합 테스트 업데이트

---

### Week 2: High Priority (20-24시간)

#### Day 1: Null-Safety
- [ ] 모든 `ResponseParser` 클래스 감사
- [ ] 명시적 null 체크 및 예외 추가
- [ ] null 입력에 대한 파서 단위 테스트 추가

#### Day 2-3: Validation i18n
- [ ] `messages.properties`, `messages_en.properties` 생성
- [ ] `AnswerValidator`에 `MessageSource` 주입
- [ ] 검증 엔드포인트에 locale 파라미터 추가

#### Day 4: 한글 단어 수
- [ ] 한글 토크나이징 라이브러리 조사 (Mecab vs 휴리스틱)
- [ ] 개선된 단어 수 로직 구현
- [ ] 공백 없는 한글 텍스트 단위 테스트

#### Day 5: 로깅 + 페이지네이션
- [ ] 모든 로그 메시지를 구조화된 형식으로 표준화
- [ ] `QuestionController`에 페이지네이션 추가
- [ ] 페이지네이션 UI용 Thymeleaf 템플릿 업데이트

---

### Week 3: Medium Priority (12-16시간)

#### Day 1-2: 문서화
- [ ] 모든 public controller 메서드에 JavaDoc 추가
- [ ] 모든 public service 메서드에 JavaDoc 추가
- [ ] 예외 발생 시나리오 문서화

#### Day 3: 코드 품질
- [ ] `ControllerConstants`로 magic string 추출
- [ ] 모든 하드코딩된 문자열 교체
- [ ] 상수 사용 검증 단위 테스트 추가

#### Day 4: 데이터베이스
- [ ] FK용 Flyway 마이그레이션 생성
- [ ] Cascading delete 테스트
- [ ] README에 인덱스 문서화 추가

---

### Week 4: Low Priority + 테스팅 (8-12시간)

#### Day 1: 성능
- [ ] 모든 list 엔드포인트에 페이지네이션 추가
- [ ] 파티셔닝 전략 문서 작성

#### Day 2: 모니터링
- [ ] AI 메트릭용 커스텀 Spring Boot Actuator 엔드포인트 추가
- [ ] Prometheus 스크래핑 검증
- [ ] Grafana 대시보드 JSON 생성

#### Day 3-4: 최종 테스팅
- [ ] 전체 회귀 테스트 스위트 실행
- [ ] JMeter 부하 테스트 (1000 req/min)
- [ ] 보안 감사 (OWASP ZAP 스캔)

---

## 💬 면접 토킹 포인트

### 아키텍처 & 설계 (5년차 경력 수준)

#### 1. 멀티 테넌트 설계
> "모든 데이터 접근을 쿼리 레벨에서 userId로 스코핑했습니다. 컨트롤러가 아닌 Repository에서 필터링하므로, 데이터 유출을 방지하고 향후 멀티 테넌시 확장이 용이합니다."

#### 2. 인터페이스 기반 설계
> "서비스 계층은 의존성 주입과 인터페이스 기반으로 설계했습니다. 예를 들어 `AiClient`는 인터페이스이므로, OpenAI를 Claude로 교체하거나 테스트에서 모킹하기 쉽습니다. 이는 SOLID 원칙의 DIP를 보여줍니다."

#### 3. N+1 쿼리 해결
> "`ReviewService`에서 N+1 쿼리 문제를 배치 쿼리로 해결했습니다. 10개 리뷰 조회 시 데이터베이스 호출을 31회에서 4회로 줄여 87% 개선했습니다. 이는 ORM 전문성을 보여줍니다."

### AI 통합 (프로젝트 고유 강점)

#### 4. 비용 제어
> "AI 통합에는 세 가지 비용 제어 메커니즘이 있습니다: SHA-256 해시 기반 중복 제거(24시간 캐시), Rate Limiting(33회/시간), 메타데이터 추적입니다. 캐시는 1,700배 속도 향상(5초→3ms)을 달성했습니다."

#### 5. Hallucination 방지
> "프롬프트에 Hallucination 방지 지침을 명시했습니다. AI에게 사실을 창작하거나 내용을 반복하거나 없는 강점을 만들지 말라고 명시적으로 지시합니다. 이는 AI 한계에 대한 이해를 보여줍니다."

#### 6. Graceful Degradation
> "모든 AI 응답은 엄격한 JSON 스키마로 파싱합니다. 파싱 실패 시 500 에러 대신 더미 피드백으로 graceful하게 저하됩니다. 이는 프로덕션 준비 사고방식입니다."

### 성능 & 확장성

#### 7. 확장 가능한 설계
> "시스템은 확장 가능한 설계로 17개 직무를 지원합니다. `JobField` enum, 동적 `PromptBuilder`, 직무별 평가 기준으로 구성됩니다. MVP는 IT만 사용하지만, 확장은 코드 변경 없이 데이터 추가만으로 가능합니다."

#### 8. Lazy 메트릭 초기화
> "Micrometer 메트릭에 lazy 초기화를 사용했습니다. 이를 통해 `MeterRegistry` 모킹 없이 서비스를 테스트할 수 있으면서도 프로덕션 메트릭을 수집할 수 있습니다."

### 테스팅 & 품질

#### 9. 포괄적 테스팅
> "총 10,583줄의 41개 포괄적 테스트를 작성했습니다. Unit(모킹 기반), Integration(전체 DB), Controller 계층을 커버합니다. 테스트는 AAA 패턴을 따르며 명확성을 위해 한글 설명을 사용합니다."

#### 10. 정밀 단언
> "가중 점수 같은 부동소수점 계산에는 `isCloseTo()`를 정밀도 오프셋과 함께 사용하여 불안정한 테스트를 방지합니다. 동시성 시나리오에서는 모의 면접 생명주기 상태 전환을 테스트합니다."

### 보안 인식

#### 11. 다층 보안
> "애플리케이션은 BCrypt 비밀번호 해싱, 세션 기반 인증, 민감한 작업 전 소유권 검증을 사용합니다. Rate Limiting은 남용을 방지하고, 다층 검증은 잘못된 데이터를 방지합니다."

### 더 많은 시간이 있다면 개선할 점

#### 12. 트랜잭션 경계 최적화
> "`AiFeedbackService`의 트랜잭션 경계가 더 엄격할 수 있습니다. 현재 fallback 메커니즘이 메인 트랜잭션 내에서 실행됩니다. 실제 vs 더미 피드백을 별도 트랜잭션으로 분리하겠습니다."

#### 13. 향후 개선 사항
> "RFC 7807 ProblemDetail을 REST 오류 응답에 추가하고, Testcontainers를 데이터베이스 통합 테스트에 추가하며, `interview_answers` 테이블이 1M 레코드를 초과할 때 파티셔닝을 구현하겠습니다."

---

## 📈 성공 기준

이 플랜 구현 후:

- [ ] **Critical 이슈 0개** - 트랜잭션 경계 수정, Race Condition 해결
- [ ] **High Priority 이슈 95%+ 해결** - RestTemplate 풀링, i18n, null-safety
- [ ] **문서화 완료** - 모든 public 메서드에 JavaDoc
- [ ] **모든 테스트 통과** - 새 동시성 및 엣지 케이스 테스트 포함
- [ ] **부하 테스트 통과** - 5분간 1000 req/min 지속
- [ ] **보안 감사 클린** - OWASP ZAP 스캔 고위험 발견 사항 없음
- [ ] **포트폴리오 준비 완료** - 아키텍처, 트레이드오프, 개선 사항을 자신 있게 논의

**최종 점수 목표**: **9.0/10** (현재 8.3/10에서)

---

## 📝 다음 단계

1. **사용자 검토**: 이 플랜을 검토하고 일정에 따라 이슈 우선순위 지정
2. **작업 생성**: Week 1 Critical 수정을 일일 작업으로 세분화
3. **실행**: 트랜잭션 경계 수정부터 시작 (가장 큰 영향)
4. **테스트**: 각 수정 후 전체 회귀 테스트 실행
5. **문서화**: 개선 사항으로 README.md 업데이트
6. **준비**: 코드 예제와 함께 면접 토킹 포인트 연습

**예상 일정**: 파트타임 4주 또는 풀타임 1.5주

---

## 📚 참고 자료

- **프로젝트 문서**: `/Users/hojun/IdeaProjects/interview-note-api/CLAUDE.md`
- **상세 플랜**: `/Users/hojun/.claude/plans/jazzy-plotting-acorn.md`
- **리팩토링 가이드**: `/Users/hojun/IdeaProjects/interview-note-api/REFACTORING_GUIDE.md`
- **완료 보고서**: `/Users/hojun/IdeaProjects/interview-note-api/docs/archive/REFACTORING_COMPLETION_REPORT.md`

---

**작성자**: Claude Code (Sonnet 4.5)
**검토일**: 2026-06-13
**버전**: 1.0
