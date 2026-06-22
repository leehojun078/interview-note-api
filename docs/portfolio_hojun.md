# Interview Mock - AI 기반 면접 연습 플랫폼

> **Kotlin/Spring Boot 기반 AI 면접 연습 서비스** | SSE 실시간 채팅 면접, 17개 직무 지원, 채용 공고 기반 맞춤 질문 생성

- **운영 URL**: https://interviewmock.xyz
- **GitHub**: https://github.com/leehojun078/interview-note-api

---

## 목차

1. [설계](#1-설계)
2. [구현](#2-구현)
3. [배포 및 운영](#3-배포-및-운영)

---

# 1. 설계

## 1.1 문제 정의

면접 준비의 가장 큰 어려움은 **피드백 없이 혼자 연습**해야 한다는 점입니다. 단순히 질문을 보여주는 앱은 많지만, 작성한 답변에 대해 구체적인 평가와 개선 방향을 제시하는 서비스는 부족했습니다.

**해결하고자 한 문제**:
- 면접 답변에 대한 객관적이고 구체적인 피드백 부재
- 반복 연습 시 개선 추적의 어려움
- 직무별로 다른 평가 기준 적용 필요
- 실제 면접처럼 꼬리 질문을 받는 경험 부족

**핵심 가치**:
- 단순 질문 은행이 아닌, **답변 개선 과정을 기록하고 추적**하는 리뷰 중심 서비스
- AI가 논리성, 구체성, 직무적합성, 전달력 4가지 항목으로 평가
- 실시간 모의 면접으로 실전 감각 훈련

---

## 1.2 기술 스택 선정

| 분류 | 기술 | 선정 이유 |
|------|------|----------|
| **Language** | Kotlin 2.2.21 (Java 21) | Null Safety, 간결한 문법, Spring 공식 지원 |
| **Framework** | Spring Boot 3.5.14 | 풍부한 생태계, 빠른 개발 속도 |
| **Database** | PostgreSQL 15 | 안정성, JSON 타입 지원, 프로덕션 검증 |
| **AI** | OpenAI gpt-4o-mini | 비용 대비 성능, JSON Mode 지원 |
| **Frontend** | Thymeleaf + HTMX | 프론트엔드 복잡도 최소화, 서버 렌더링 |
| **Cache** | Caffeine | 인메모리 캐시로 단순화, 별도 인프라 불필요 |
| **Container** | Docker | 환경 일관성, Multi-stage 빌드로 이미지 최적화 |

**선정 원칙**: 백엔드 역량에 집중하기 위해 프론트엔드/백엔드 분리를 하지 않고, 단일 Spring Boot 애플리케이션으로 구성했습니다. React/Vue 없이 Thymeleaf + HTMX 조합으로 필요한 인터랙션을 구현했습니다.

---

## 1.3 아키텍처 설계

### 레이어드 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                      Presentation Layer                          │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌───────────┐  │
│  │ Question    │ │ Answer      │ │ Review      │ │ MockIntv  │  │
│  │ Controller  │ │ Controller  │ │ Controller  │ │ Controller│  │
│  └──────┬──────┘ └──────┬──────┘ └──────┬──────┘ └─────┬─────┘  │
└─────────┼───────────────┼───────────────┼──────────────┼────────┘
          │               │               │              │
          ▼               ▼               ▼              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Service Layer                             │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                   AI Integration Layer                      │ │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐            │ │
│  │  │ Prompt      │ │ OpenAi      │ │ Response    │            │ │
│  │  │ Builder     │ │ Client      │ │ Parser      │            │ │
│  │  └─────────────┘ └─────────────┘ └─────────────┘            │ │
│  └─────────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                   Support Services                          │ │
│  │  ┌───────────────┐ ┌───────────────┐ ┌─────────────────┐    │ │
│  │  │ Duplicate     │ │ RateLimit     │ │ Answer          │    │ │
│  │  │ RequestCache  │ │ Service       │ │ Validator       │    │ │
│  │  └───────────────┘ └───────────────┘ └─────────────────┘    │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Repository Layer                           │
│  Question / InterviewAnswer / AiFeedback / MockInterview / User  │
└─────────────────────────────────────────────────────────────────┘
```

**설계 원칙**:
- **AI 통합 레이어 분리**: PromptBuilder, OpenAiClient, ResponseParser를 별도 레이어로 분리하여 AI 공급자 교체 용이
- **Support Services**: 캐싱, Rate Limiting, 검증 로직을 독립 서비스로 분리
- **Controller에서 직접 AI 호출 금지**: 반드시 Service 레이어를 통과

![시스템 아키텍처](./images/architecture.png)

---

## 1.4 데이터 모델링

### 핵심 엔티티 관계

```
┌─────────┐     1:N     ┌─────────────────┐     1:1     ┌────────────┐
│  User   │────────────▶│ InterviewAnswer │────────────▶│ AiFeedback │
└─────────┘             └─────────────────┘             └────────────┘
     │                          │
     │                          │ N:1
     │                          ▼
     │                  ┌─────────────────┐
     │                  │    Question     │
     │                  └─────────────────┘
     │
     │ 1:N     ┌─────────────────┐     1:N     ┌──────────────────┐
     └────────▶│  MockInterview  │────────────▶│ InterviewMessage │
               └─────────────────┘             └──────────────────┘
                       │
                       │ N:1
                       ▼
               ┌─────────────────┐     1:N     ┌───────────────────┐
               │   JobPosting    │────────────▶│ GeneratedQuestion │
               └─────────────────┘             └───────────────────┘
```

**주요 설계 결정**:
- `AiFeedback.answerTextHash`: SHA-256 해시로 중복 요청 감지 (24시간 캐싱)
- `AiFeedback.rawResponse`: AI 원본 응답 저장 (디버깅, 프롬프트 개선에 활용)
- `AiFeedback.promptVersion`: 프롬프트 버전 관리로 A/B 테스트 가능
- `MockInterview.status`: IN_PROGRESS, COMPLETED 상태 관리로 면접 재개 지원

---

# 2. 구현

## 2.1 핵심 기능

### AI 답변 평가

사용자가 면접 질문에 답변을 제출하면 OpenAI가 4가지 항목으로 평가합니다.

- **평가 항목**: 논리성, 구체성, 직무적합성, 전달력 (각 1-5점)
- **피드백 내용**: 강점(0-5개), 개선점(1-5개), 모범답변(400-600자), 종합 코멘트

**JSON Mode 강제**: 자유 텍스트 응답의 파싱 불안정 문제를 해결하기 위해 `response_format: { type: "json_object" }`를 설정하여 구조화된 JSON 응답을 강제합니다.

```kotlin
// OpenAiClientImpl.kt
val requestBody = OpenAiRequest(
    model = properties.model,
    messages = listOf(
        Message(role = "system", content = systemPrompt),
        Message(role = "user", content = userPrompt)
    ),
    responseFormat = ResponseFormat(type = "json_object")
)
```

**Fallback 메커니즘**: AI 호출 실패 시 더미 피드백을 반환하여 서비스 연속성을 확보합니다.

![질문 목록 화면](./images/questions-list.png)

![답변 작성 화면](./images/answer-form.png)

![AI 평가 결과](./images/feedback-result.png)

---

### 실시간 모의 면접 (SSE)

AI와 채팅 형식으로 면접을 진행하며, 각 답변에 대해 실시간으로 평가와 꼬리 질문을 받습니다.

**SSE 선택 이유**:
| 방식 | 장점 | 단점 |
|------|------|------|
| Polling | 구현 간단 | 불필요한 요청, 실시간성 낮음 |
| WebSocket | 양방향 | 연결 관리 복잡 |
| **SSE** | 단방향 스트리밍, HTTP 기반 | 서버→클라이언트만 |

면접 시나리오는 AI→User 방향 스트리밍이 핵심이므로 SSE가 적합합니다.

```kotlin
// SseEmitterService.kt
private val emitters = ConcurrentHashMap<Long, SseEmitter>()

fun broadcast(id: Long, event: String, data: Any): Boolean {
    val emitter = emitters[id] ?: return false
    return try {
        emitter.send(SseEmitter.event().name(event).data(data))
        true
    } catch (e: IOException) {
        remove(id)
        false
    }
}
```

**비동기 처리**: `@Async`로 AI 응답 생성을 백그라운드에서 처리하여 사용자 대기 시간을 제거합니다.

![실시간 모의 면접](./images/mock-interview.png)

---

### 채용 공고 기반 질문 생성

채용 공고 URL을 입력하면 HTML을 파싱하여 해당 직무에 맞는 면접 질문 10개를 생성합니다.

**3단계 Fallback 전략**:
1. 사이트별 Jsoup 파서 (wanted, saramin, jobkorea)
2. AI Fallback (HTML 구조 인식 실패 시)
3. 수동 입력 (URL 파싱 완전 실패 시)

```kotlin
// JobPostingParserService.kt
fun parseFromUrl(url: String): ParsedJobPosting? {
    val document = fetchHtml(url)

    // 사이트별 파서 시도
    val parsed = when {
        host.contains("wanted.co.kr") -> parseWanted(document)
        host.contains("saramin.co.kr") -> parseSaramin(document)
        else -> null
    }
    if (parsed != null) return parsed

    // AI Fallback - HTML을 텍스트로 변환 후 AI 파싱
    val cleanedHtml = Jsoup.parse(document.html()).body().text()
    return parseWithAi(cleanedHtml)
}
```

**토큰 비용 절감**: HTML 원본 → 텍스트 변환으로 97.8% 크기 감소를 달성했습니다.

---

## 2.2 비용 최적화

### 중복 요청 캐싱

**문제**: 동일 답변 재제출 시 불필요한 AI API 호출 발생

**해결**: `(questionId + answerText)` 조합의 SHA-256 해시로 24시간 캐싱

```kotlin
// DuplicateRequestCache.kt
fun generateHash(questionId: Long, answerText: String): String {
    val input = "$questionId:$answerText"
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

fun findCached(questionId: Long, answerText: String): AiFeedback? {
    val hash = generateHash(questionId, answerText)
    return aiFeedbackRepository.findByAnswerTextHashAndCreatedAtAfter(
        hash, LocalDateTime.now().minusHours(24)
    )
}
```

**효과**: 캐시 히트 시 AI 호출 완전 생략, DB 조회로 구현하여 별도 Redis 불필요

---

### Rate Limiting

**정책**: IP당 33회/시간 (Caffeine Cache 기반)

```kotlin
// RateLimitService.kt
private val cache: Cache<String, AtomicInteger> = Caffeine.newBuilder()
    .expireAfterWrite(1, TimeUnit.HOURS)
    .build()

fun checkAndRecordRequest(ip: String): Boolean {
    val counter = cache.get(ip) { AtomicInteger(0) }
    return counter.incrementAndGet() <= MAX_REQUESTS_PER_HOUR
}
```

**효과**: 최대 월 비용 $4.75 (단일 IP 기준)로 제한

---

### 답변 품질 사전 검증

**문제**: 무의미한 답변("aaaa...", 반복 단어)이 AI 호출되어 비용 낭비

**해결**: AI 호출 전 5가지 사전 검증

```kotlin
// AnswerValidator.kt
fun validate(answerText: String): ValidationResult {
    // 1. 반복 문자 70% 이상 → 거부
    if (hasExcessiveRepeatedChars(trimmed)) return Invalid("...")

    // 2. 반복 단어 40% 이상 → 거부
    if (hasExcessiveRepeatedWords(trimmed)) return Invalid("...")

    // 3. 고유 문자 5개 미만 → 거부
    if (uniqueChars < 5) return Invalid("...")

    // 4. 최소 10단어 → 거부
    if (words.size < 10) return Invalid("...")

    // 5. 의미 있는 문자 50% 미만 → 거부
    if (!hasSufficientMeaningfulChars(trimmed)) return Invalid("...")

    return Valid
}
```

**효과**: 무의미한 답변 AI 호출 차단, AI Hallucination 방지

---

## 2.3 확장성 설계

### 17개 직무 지원 (Strategy Pattern)

`JobFieldPromptConfig`에서 직무별로 다른 평가 기준과 프롬프트를 동적으로 생성합니다.

```kotlin
// JobFieldPromptConfig.kt
fun getSystemPrompt(jobField: JobField): String {
    return when (jobField) {
        BACKEND -> "백엔드 개발자 면접 코치..."
        FRONTEND -> "프론트엔드 개발자 면접 코치..."
        DEVOPS -> "DevOps 엔지니어 면접 코치..."
        // 17개 직무 모두 지원
    }
}
```

**확장 포인트**: 새 직무 추가 시 enum 값과 프롬프트 템플릿만 추가하면 됩니다.

### AI 공급자 교체 가능 (Adapter Pattern)

`AiClient` 인터페이스로 추상화하여 OpenAI 외 다른 AI 공급자로 교체 가능합니다.

```kotlin
interface AiClient {
    fun requestFeedback(systemPrompt: String, userPrompt: String): AiResponse
}

// 현재: OpenAiClientImpl
// 추후: ClaudeClientImpl, GeminiClientImpl 등 추가 가능
```

---

# 3. 배포 및 운영

## 3.1 컨테이너화

### Docker Multi-stage 빌드

```dockerfile
# Stage 1: Build
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app
COPY . .
RUN ./gradlew build -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
RUN adduser -D -s /bin/sh appuser
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**최적화 포인트**:
- Alpine Linux 기반으로 이미지 크기 최소화
- 비루트 유저 실행으로 보안 강화
- HEALTHCHECK 설정으로 컨테이너 상태 모니터링

---

## 3.2 환경 분리

| 환경 | 데이터베이스 | 프로파일 |
|------|------------|---------|
| 개발 | H2 (인메모리) | `dev` |
| 운영 | PostgreSQL 15 | `prod` |

```yaml
# docker-compose.yml
services:
  app:
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - OPENAI_API_KEY=${OPENAI_API_KEY}
    depends_on:
      db:
        condition: service_healthy

  db:
    image: postgres:15-alpine
    volumes:
      - postgres_data:/var/lib/postgresql/data
```

---

## 3.3 모니터링

### Prometheus 메트릭

- `ai.calls.total`: AI 호출 횟수
- `ai.calls.duration`: AI 응답 시간
- `cache.hits`: 캐시 히트 횟수
- `sse.active_connections`: SSE 연결 수

### JSON 구조화 로깅

```xml
<!-- logback-spring.xml -->
<appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>requestId</includeMdcKeyName>
        <includeMdcKeyName>userId</includeMdcKeyName>
        <includeMdcKeyName>ip</includeMdcKeyName>
    </encoder>
</appender>
```

**MDC 추적**: `RequestIdFilter`에서 요청별 고유 ID를 부여하여 로그 추적

### Health Check

- `/actuator/health/liveness`: 컨테이너 생존 확인
- `/actuator/health/readiness`: 트래픽 수신 준비 확인
- `OpenAiHealthIndicator`: AI 서비스 연결 상태 확인

![모니터링 대시보드](./images/prometheus.png)

---

## 3.4 배포 인프라

| 항목 | 구성 |
|------|------|
| **호스팅** | AWS EC2 |
| **도메인** | https://interviewmock.xyz |
| **HTTPS** | SSL 인증서 적용 |
| **프록시** | Nginx (SSE 버퍼링 설정) |

### Nginx SSE 설정

SSE 스트리밍을 위해 버퍼링을 비활성화합니다.

```nginx
location /mock-interviews/stream {
    proxy_pass http://app:8080;
    proxy_http_version 1.1;
    proxy_set_header Connection "";
    proxy_buffering off;
    proxy_cache off;
    proxy_read_timeout 86400;
}
```

---

## 성과 요약

| 항목 | 수치 |
|------|------|
| 소스 코드 | 84개 파일 |
| 테스트 코드 | 42개 파일 |
| DB 마이그레이션 | V1-V14 (14개 버전) |
| 엔티티 | 10개 |
| 직무 분야 | 17개 |
| 질문 데이터 | 340+개 |

**핵심 성과**:
- SHA-256 캐싱으로 중복 요청 시 AI 호출 제거
- Rate Limiting + 사전 검증으로 API 비용 최적화
- SSE + @Async로 실시간 모의 면접 UX 구현
- 17개 직무 지원으로 확장성 확보
- Docker + Prometheus로 프로덕션 운영 환경 구축

---

## 이력서용 핵심 Bullet

- OpenAI gpt-4o-mini 연동, JSON Mode로 구조화된 평가 응답 구현 (4가지 점수 + 모범답변)
- SSE 기반 실시간 모의 면접, ConcurrentHashMap + @Async로 비동기 AI 응답 처리
- SHA-256 해싱 중복 요청 캐싱(24시간) + Caffeine Rate Limiting(33회/시간)으로 API 비용 최적화
- Docker Multi-stage 빌드, Prometheus 메트릭, Logstash JSON 로깅, AWS EC2 배포
