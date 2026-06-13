# Phase 3 구현 계획서

면접 리뷰 웹 애플리케이션 - 완성도 향상 및 프로덕션 준비

## 목차
1. [개요](#1-개요)
2. [현재 상태 (Phase 2 완료)](#2-현재-상태-phase-2-완료)
3. [Phase 3 목표](#3-phase-3-목표)
4. [상세 구현 계획](#4-상세-구현-계획)
5. [구현 순서](#5-구현-순서)
6. [검증 계획](#6-검증-계획)

---

## 1. 개요

### 1.1 목표

Phase 2에서 구현한 AI 연동을 바탕으로, **실제 사용자에게 배포 가능한 수준**으로 애플리케이션을 완성합니다.

**핵심 가치**:
- ✅ **사용자 경험**: 직관적이고 반응성 있는 UI
- ✅ **운영 가능성**: 로깅, 모니터링, 에러 추적
- ✅ **안정성**: 개선된 에러 처리, 사용자 친화적 에러 메시지
- ✅ **배포 준비**: Docker, 환경별 설정 분리

### 1.2 Phase 3의 범위

**포함되는 기능**:
- UI/UX 개선 (CSS, 레이아웃, 반응형)
- HTMX를 활용한 인터랙티브 요소
- 구조화된 로깅 및 모니터링
- 에러 페이지 및 사용자 피드백 개선
- Docker 컨테이너화
- 프로덕션 환경 설정 분리

**제외되는 기능** (MVP 범위 밖):
- ❌ 로그인/회원가입 (여전히 단일 사용자)
- ❌ 관리자 대시보드
- ❌ 소셜 공유 기능
- ❌ 음성/STT 기능
- ❌ 벡터DB/RAG

---

## 2. 현재 상태 (Phase 2 완료)

### 2.1 구현 완료된 기능

✅ **백엔드**:
- OpenAI API 연동 (gpt-4o-mini)
- 4가지 평가 기준 (논리성, 구체성, 직무적합성, 전달력)
- AI 생성 모범답변
- 중복 요청 방지 (SHA-256 해싱, 24시간 캐싱)
- Rate Limiting (IP당 33회/시간)
- Fallback 메커니즘 (AI 오류 시 더미 피드백)
- 메타데이터 저장 (모델명, 토큰 사용량, 원본 응답)

✅ **테스트**:
- 136개 테스트 (단위, 통합, E2E)
- 주요 컴포넌트 단위 테스트 완료
- Rate Limit, 캐싱, AI 클라이언트 테스트

✅ **문서**:
- README.md (프로젝트 개요, 설치)
- CHANGELOG.md (변경 이력)
- SETUP_GUIDE.md (환경 설정)
- CLAUDE.md (개발 가이드)

### 2.2 개선 필요 영역

🔸 **UI/UX**:
- 기본 Thymeleaf 템플릿만 존재
- CSS 스타일링 부족
- 사용자 피드백 (로딩, 성공/실패 메시지) 부족
- 모바일 반응형 미지원

🔸 **운영**:
- 로그가 구조화되지 않음
- 메트릭/모니터링 없음
- 에러 추적 어려움

🔸 **에러 처리**:
- 일부 에러 페이지 미구현
- 사용자 친화적인 에러 메시지 부족

🔸 **배포**:
- 환경별 설정 분리 미흡
- Docker 미지원
- 프로덕션 준비 부족

---

## 3. Phase 3 목표

### 3.1 사용자 경험 개선

**목표**: 직관적이고 반응성 있는 인터페이스

**구현 항목**:
1. **CSS 프레임워크 도입** (Tailwind CSS 또는 Bootstrap)
   - 일관된 디자인 시스템
   - 반응형 레이아웃
   - 접근성 (a11y) 고려

2. **HTMX 적용**
   - 페이지 새로고침 없는 답변 제출
   - 실시간 로딩 인디케이터
   - 부드러운 트랜지션

3. **사용자 피드백 강화**
   - 성공/실패 토스트 메시지
   - 진행 상태 표시 (AI 평가 중...)
   - Validation 오류 인라인 표시

4. **에러 페이지 개선**
   - 사용자 친화적 404, 500 페이지
   - Rate Limit 초과 시 명확한 안내
   - AI 오류 시 fallback 안내

### 3.2 운영 가능성 확보

**목표**: 프로덕션 환경에서 안정적으로 운영 가능

**구현 항목**:
1. **구조화된 로깅**
   - JSON 형식 로그 (Logback + Logstash Encoder)
   - 요청 ID 추적 (MDC)
   - 중요 이벤트 로깅 (AI 호출, 캐시 히트, Rate Limit)

2. **메트릭 수집** (Micrometer + Prometheus)
   - HTTP 요청 메트릭
   - AI API 호출 횟수, 지연 시간, 토큰 사용량
   - 캐시 히트율
   - Rate Limit 거부 횟수

3. **헬스 체크**
   - Actuator 엔드포인트 활성화
   - Liveness / Readiness probe
   - OpenAI API 연결 상태 체크

### 3.3 배포 준비

**목표**: 컨테이너 기반 배포 환경 구축

**구현 항목**:
1. **Docker 컨테이너화**
   - Multi-stage 빌드
   - 최적화된 이미지 크기
   - 환경변수 기반 설정

2. **환경별 설정 분리**
   - application-dev.properties
   - application-prod.properties
   - 민감 정보 외부화

3. **Docker Compose**
   - PostgreSQL 컨테이너
   - 애플리케이션 컨테이너
   - 로컬 개발 환경 일관성

---

## 4. 상세 구현 계획

### 4.1 UI/UX 개선

#### 4.1.1 CSS 프레임워크 선택: Tailwind CSS

**선택 이유**:
- ✅ 빠른 프로토타이핑 (유틸리티 우선)
- ✅ 번들 크기 최적화 (PurgeCSS)
- ✅ Thymeleaf와 호환성 좋음
- ✅ 커스터마이징 용이

**대안 (채택하지 않음)**:
- ❌ Bootstrap: 과도한 기본 스타일, 커스터마이징 어려움
- ❌ Pure CSS: 개발 시간 오래 걸림

**적용 범위**:
- 전체 레이아웃 (헤더, 푸터, 네비게이션)
- 질문 목록 카드
- 답변 작성 폼
- 피드백 결과 페이지
- 에러 페이지

**구현 예시**:
```html
<!-- 질문 카드 -->
<div class="max-w-4xl mx-auto p-6">
  <div class="bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition">
    <div class="flex items-center justify-between mb-4">
      <span class="px-3 py-1 bg-blue-100 text-blue-800 rounded-full text-sm">
        기술역량
      </span>
      <span class="text-gray-500 text-sm">난이도: MEDIUM</span>
    </div>
    <h3 class="text-xl font-semibold text-gray-800 mb-2">
      Spring Boot의 장점을 설명하세요.
    </h3>
    <a href="/questions/1/answer"
       class="mt-4 inline-block bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700">
      답변하기
    </a>
  </div>
</div>
```

#### 4.1.2 HTMX 도입

**목표**: 페이지 새로고침 없는 부드러운 UX

**적용 시나리오**:
1. **답변 제출**
   - 폼 제출 시 전체 페이지 새로고침 대신 HTMX 사용
   - 제출 버튼 → 로딩 스피너 → 성공 메시지

2. **실시간 피드백**
   - AI 평가 진행 중 폴링 (polling)으로 진행 상태 표시
   - "AI가 답변을 평가하는 중입니다... (예상 5-10초)"

3. **필터링**
   - 카테고리/난이도 선택 시 페이지 새로고침 없이 목록 업데이트

**구현 예시**:
```html
<!-- 답변 제출 폼 -->
<form hx-post="/questions/1/answer"
      hx-target="#feedback-result"
      hx-indicator="#loading-spinner">
  <textarea name="answerText" class="w-full p-4 border rounded-lg"></textarea>
  <button type="submit"
          class="bg-blue-600 text-white px-6 py-2 rounded-lg">
    제출하기
  </button>
  <div id="loading-spinner" class="htmx-indicator">
    <svg class="animate-spin h-5 w-5 text-blue-600">...</svg>
    AI 평가 중...
  </div>
</form>
<div id="feedback-result"></div>
```

**컨트롤러 변경**:
```kotlin
@PostMapping("/questions/{questionId}/answer")
fun submitAnswer(
    @PathVariable questionId: Long,
    @Valid @ModelAttribute dto: AnswerSubmitDto,
    bindingResult: BindingResult,
    request: HttpServletRequest,
    @RequestHeader("HX-Request", required = false) hxRequest: String?
): String {
    // ... 기존 로직 ...

    val result = interviewService.submitAnswer(dto)

    // HTMX 요청이면 partial 템플릿 반환
    if (hxRequest == "true") {
        model.addAttribute("answer", result)
        return "fragments/feedback :: feedback-content"
    }

    // 일반 요청이면 리다이렉트
    return "redirect:/answers/${result.answerId}/feedback"
}
```

#### 4.1.3 사용자 피드백 컴포넌트

**1. 토스트 메시지**
```html
<!-- Toast 컴포넌트 (Tailwind + Alpine.js) -->
<div x-data="{ show: false, message: '' }"
     @show-toast.window="show = true; message = $event.detail; setTimeout(() => show = false, 3000)"
     x-show="show"
     class="fixed top-4 right-4 bg-green-500 text-white px-6 py-3 rounded-lg shadow-lg">
  <span x-text="message"></span>
</div>
```

**2. 로딩 인디케이터**
```html
<div class="flex items-center justify-center p-8">
  <svg class="animate-spin h-8 w-8 text-blue-600" viewBox="0 0 24 24">
    <!-- spinner SVG -->
  </svg>
  <span class="ml-3 text-gray-600">AI가 답변을 평가하는 중입니다...</span>
</div>
```

**3. Validation 오류 표시**
```html
<div th:if="${#fields.hasErrors('answerText')}"
     class="text-red-600 text-sm mt-1">
  <span th:errors="*{answerText}"></span>
</div>
```

#### 4.1.4 에러 페이지 개선

**구현 파일**:
- `src/main/resources/templates/error/404.html`
- `src/main/resources/templates/error/500.html`
- `src/main/resources/templates/error/rate-limit.html`

**404 페이지 예시**:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>404 - 페이지를 찾을 수 없습니다</title>
    <link href="/css/tailwind.css" rel="stylesheet">
</head>
<body class="bg-gray-50">
    <div class="min-h-screen flex items-center justify-center">
        <div class="text-center">
            <h1 class="text-9xl font-bold text-gray-200">404</h1>
            <p class="text-2xl font-semibold text-gray-800 mt-4">
                페이지를 찾을 수 없습니다
            </p>
            <p class="text-gray-600 mt-2">
                요청하신 페이지가 존재하지 않거나 이동되었습니다.
            </p>
            <a href="/"
               class="mt-6 inline-block bg-blue-600 text-white px-6 py-3 rounded-lg">
                홈으로 돌아가기
            </a>
        </div>
    </div>
</body>
</html>
```

**GlobalExceptionHandler 개선**:
```kotlin
@ExceptionHandler(NotFoundException::class)
fun handleNotFound(e: NotFoundException, model: Model): String {
    logger.warn("리소스를 찾을 수 없음: ${e.message}")
    model.addAttribute("message", e.message)
    return "error/404"
}

@ExceptionHandler(Exception::class)
fun handleGenericException(e: Exception, model: Model): String {
    logger.error("예상치 못한 오류 발생", e)
    model.addAttribute("message", "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
    model.addAttribute("errorId", UUID.randomUUID().toString()) // 에러 추적용
    return "error/500"
}
```

---

### 4.2 로깅 및 모니터링

#### 4.2.1 구조화된 로깅 (JSON)

**의존성 추가**:
```kotlin
// build.gradle.kts
implementation("net.logstash.logback:logstash-logback-encoder:7.4")
```

**Logback 설정** (`src/main/resources/logback-spring.xml`):
```xml
<configuration>
    <springProfile name="prod">
        <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                <includeMdcKeyName>requestId</includeMdcKeyName>
                <includeMdcKeyName>userId</includeMdcKeyName>
                <includeMdcKeyName>ip</includeMdcKeyName>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="JSON"/>
        </root>
    </springProfile>

    <springProfile name="dev">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="DEBUG">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>
</configuration>
```

**요청 ID 추적 필터**:
```kotlin
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestId = UUID.randomUUID().toString()
        MDC.put("requestId", requestId)
        MDC.put("ip", getClientIp(request))

        response.addHeader("X-Request-ID", requestId)

        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.clear()
        }
    }
}
```

**로깅 개선 예시**:
```kotlin
// Before
logger.info("OpenAI 피드백 요청 - 모델: ${properties.model}")

// After
logger.info(
    "OpenAI 피드백 요청",
    kv("model", properties.model),
    kv("questionId", questionId),
    kv("answerLength", answerText.length)
)
```

#### 4.2.2 메트릭 수집 (Micrometer)

**의존성 추가**:
```kotlin
// build.gradle.kts
implementation("org.springframework.boot:spring-boot-starter-actuator")
implementation("io.micrometer:micrometer-registry-prometheus")
```

**application.properties**:
```properties
# Actuator 설정
management.endpoints.web.exposure.include=health,info,prometheus,metrics
management.endpoint.health.show-details=when-authorized
management.metrics.tags.application=interview-note-api
management.metrics.tags.environment=${SPRING_PROFILES_ACTIVE:dev}
```

**커스텀 메트릭**:
```kotlin
@Service
class AiFeedbackService(
    // ...
    private val meterRegistry: MeterRegistry
) {

    private val aiCallCounter = meterRegistry.counter("ai.calls.total")
    private val aiCallTimer = meterRegistry.timer("ai.calls.duration")
    private val cacheHitCounter = meterRegistry.counter("cache.hits", "type", "duplicate_request")
    private val cacheMissCounter = meterRegistry.counter("cache.misses", "type", "duplicate_request")
    private val tokenUsageGauge = meterRegistry.gauge("ai.tokens.usage", AtomicLong(0))

    fun generateFeedback(answer: InterviewAnswer, question: Question): AiFeedback {
        // 캐시 확인
        val cached = duplicateRequestCache.findCached(question.id, answer.answerText)
        if (cached != null) {
            cacheHitCounter.increment()
            logger.info("캐시된 피드백 반환", kv("feedbackId", cached.id))
            return cached
        }
        cacheMissCounter.increment()

        // AI 호출
        return aiCallTimer.recordCallable {
            aiCallCounter.increment()
            // ... AI 호출 로직 ...
            tokenUsageGauge?.set((tokenUsageInput + tokenUsageOutput).toLong())
            feedback
        }!!
    }
}
```

**Prometheus 엔드포인트**:
- `/actuator/prometheus` - 메트릭 수집 엔드포인트

**주요 메트릭**:
- `ai_calls_total` - AI API 호출 횟수
- `ai_calls_duration` - AI API 호출 지연 시간 (평균, P95, P99)
- `cache_hits_total` - 캐시 히트 횟수
- `cache_misses_total` - 캐시 미스 횟수
- `ai_tokens_usage` - 실시간 토큰 사용량
- `http_server_requests_seconds` - HTTP 요청 메트릭 (Spring Boot 기본)

#### 4.2.3 헬스 체크

**커스텀 헬스 인디케이터**:
```kotlin
@Component
class OpenAiHealthIndicator(
    private val aiClient: AiClient
) : HealthIndicator {

    override fun health(): Health {
        return try {
            // 간단한 연결 테스트 (실제 호출 안 함)
            Health.up()
                .withDetail("provider", "OpenAI")
                .withDetail("model", "gpt-4o-mini")
                .build()
        } catch (e: Exception) {
            Health.down()
                .withDetail("error", e.message)
                .build()
        }
    }
}
```

**헬스 체크 엔드포인트**:
- `/actuator/health` - 전체 헬스 상태
- `/actuator/health/liveness` - Kubernetes liveness probe
- `/actuator/health/readiness` - Kubernetes readiness probe

---

### 4.3 Docker 컨테이너화

#### 4.3.1 Multi-stage Dockerfile

**목표**: 최적화된 프로덕션 이미지 (< 200MB)

**Dockerfile**:
```dockerfile
# Stage 1: Build
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

RUN gradle build -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 타임존 설정
RUN apk add --no-cache tzdata
ENV TZ=Asia/Seoul

# 비루트 유저 생성
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}", \
    "-jar", \
    "app.jar"]
```

**빌드 및 실행**:
```bash
# 이미지 빌드
docker build -t interview-note-api:latest .

# 컨테이너 실행
docker run -d \
  --name interview-note-api \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e OPENAI_API_KEY=sk-proj-... \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/interviewdb \
  interview-note-api:latest
```

#### 4.3.2 Docker Compose

**목표**: 로컬 개발 환경 일관성

**docker-compose.yml**:
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: interview-postgres
    environment:
      POSTGRES_DB: interviewdb
      POSTGRES_USER: interviewuser
      POSTGRES_PASSWORD: interviewpass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U interviewuser"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    build: .
    container_name: interview-note-api
    depends_on:
      postgres:
        condition: service_healthy
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      OPENAI_API_KEY: ${OPENAI_API_KEY}
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/interviewdb
      SPRING_DATASOURCE_USERNAME: interviewuser
      SPRING_DATASOURCE_PASSWORD: interviewpass
    restart: unless-stopped

volumes:
  postgres_data:
```

**실행**:
```bash
# 환경변수 로드
export $(cat .env | grep -v '^#' | xargs)

# 전체 스택 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f app

# 종료
docker-compose down
```

#### 4.3.3 환경별 설정 분리

**application-dev.properties**:
```properties
# 개발 환경
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.com.hojun.interviewnote=DEBUG

# H2 사용
spring.datasource.url=jdbc:h2:mem:interviewdb
spring.h2.console.enabled=true
```

**application-prod.properties**:
```properties
# 프로덕션 환경
spring.jpa.show-sql=false
logging.level.com.hojun.interviewnote=INFO

# PostgreSQL 사용
spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect

# Flyway 설정
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
```

---

## 5. 구현 순서

### Phase 3A: UI/UX 개선 (3-4일)

#### Step 1: Tailwind CSS 설정
- [ ] Tailwind CSS CDN 또는 빌드 설정
- [ ] 기본 레이아웃 템플릿 작성 (`layout/base.html`)
- [ ] 헤더, 푸터, 네비게이션 컴포넌트

#### Step 2: 기존 페이지 스타일링
- [ ] 질문 목록 페이지 (`questions/list.html`)
- [ ] 질문 상세 + 답변 작성 페이지 (`questions/answer-form.html`)
- [ ] 피드백 결과 페이지 (`answers/feedback.html`)
- [ ] 리뷰 이력 페이지 (`reviews/list.html`)

#### Step 3: HTMX 도입
- [ ] HTMX CDN 추가
- [ ] 답변 제출 폼 HTMX 적용
- [ ] 로딩 인디케이터 구현
- [ ] 필터링 기능 HTMX 적용 (선택사항)

#### Step 4: 사용자 피드백 컴포넌트
- [ ] 토스트 메시지 컴포넌트
- [ ] Validation 오류 인라인 표시
- [ ] 성공/실패 알림 개선

#### Step 5: 에러 페이지
- [ ] 404 페이지 (`error/404.html`)
- [ ] 500 페이지 (`error/500.html`)
- [ ] Rate Limit 페이지 (`error/rate-limit.html`)
- [ ] GlobalExceptionHandler 개선

**체크포인트**:
- ✅ 모든 페이지가 일관된 디자인 시스템 적용
- ✅ 모바일 반응형 동작
- ✅ 에러 시나리오 사용자 친화적 처리

---

### Phase 3B: 로깅 및 모니터링 (2-3일)

#### Step 6: 구조화된 로깅
- [ ] Logstash Encoder 의존성 추가
- [ ] `logback-spring.xml` 설정
- [ ] RequestIdFilter 구현
- [ ] 주요 이벤트 로깅 개선
  - AI 호출 시작/완료
  - 캐시 히트/미스
  - Rate Limit 거부
  - 예외 발생

#### Step 7: 메트릭 수집
- [ ] Actuator + Micrometer 의존성 추가
- [ ] `application.properties` 설정
- [ ] 커스텀 메트릭 구현
  - `ai_calls_total`
  - `ai_calls_duration`
  - `cache_hits_total`
  - `ai_tokens_usage`
- [ ] Prometheus 엔드포인트 테스트

#### Step 8: 헬스 체크
- [ ] OpenAiHealthIndicator 구현
- [ ] Liveness / Readiness probe 설정
- [ ] 헬스 체크 엔드포인트 테스트

**체크포인트**:
- ✅ `/actuator/prometheus` 정상 동작
- ✅ `/actuator/health` 상태 확인
- ✅ 로그가 JSON 형식으로 출력 (프로덕션)
- ✅ 요청 ID가 모든 로그에 포함

---

### Phase 3C: Docker 및 배포 준비 (2일)

#### Step 9: Dockerfile 작성
- [ ] Multi-stage Dockerfile 작성
- [ ] 이미지 빌드 테스트
- [ ] 이미지 크기 확인 (< 200MB)
- [ ] 컨테이너 실행 테스트

#### Step 10: Docker Compose
- [ ] `docker-compose.yml` 작성
- [ ] PostgreSQL 서비스 추가
- [ ] 환경변수 설정
- [ ] 전체 스택 실행 테스트

#### Step 11: 환경별 설정 분리
- [ ] `application-dev.properties` 작성
- [ ] `application-prod.properties` 작성
- [ ] Flyway 마이그레이션 테스트 (PostgreSQL)
- [ ] 환경변수 문서화 (`SETUP_GUIDE.md` 업데이트)

**체크포인트**:
- ✅ `docker-compose up` 한 번에 전체 스택 실행
- ✅ PostgreSQL에서 Flyway 마이그레이션 성공
- ✅ 프로덕션 프로필로 정상 동작
- ✅ 환경변수로 모든 민감 정보 외부화

---

### Phase 3D: 문서화 및 테스트 (1일)

#### Step 12: 문서 업데이트
- [ ] `README.md` 업데이트
  - Docker 실행 방법
  - 환경변수 목록
  - 메트릭 엔드포인트
- [ ] `SETUP_GUIDE.md` 업데이트
  - Docker 설치 가이드
  - Docker Compose 사용법
  - 프로덕션 배포 가이드
- [ ] `CHANGELOG.md` 업데이트

#### Step 13: E2E 테스트
- [ ] 전체 사용자 플로우 테스트
  - 질문 조회 → 답변 작성 → AI 평가 → 리뷰 이력
- [ ] 에러 시나리오 테스트
  - Validation 오류
  - Rate Limit 초과
  - AI API 오류 (fallback)
- [ ] Docker 환경 테스트
  - 로컬 Docker Compose 실행
  - 메트릭 수집 확인

**체크포인트**:
- ✅ Phase 3 문서 완성
- ✅ 전체 E2E 테스트 통과
- ✅ 프로덕션 준비 완료

---

## 6. 검증 계획

### 6.1 UI/UX 검증

**수동 테스트 체크리스트**:
- [ ] 질문 목록 페이지 렌더링 (데스크톱, 모바일)
- [ ] 답변 작성 폼 제출 (HTMX)
- [ ] 로딩 인디케이터 표시
- [ ] 성공 토스트 메시지 표시
- [ ] Validation 오류 인라인 표시
- [ ] 404 페이지 접근 (존재하지 않는 질문 ID)
- [ ] 500 페이지 트리거 (의도적 예외 발생)
- [ ] Rate Limit 페이지 (34회 요청 후)

**브라우저 테스트**:
- [ ] Chrome (최신)
- [ ] Safari (최신)
- [ ] Firefox (최신)
- [ ] 모바일 Safari (iOS)
- [ ] 모바일 Chrome (Android)

### 6.2 로깅 및 모니터링 검증

**로그 검증**:
```bash
# 프로덕션 프로필로 실행
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun

# JSON 로그 확인
curl localhost:8080/questions | jq '.requestId'
```

**메트릭 검증**:
```bash
# Prometheus 메트릭 확인
curl localhost:8080/actuator/prometheus | grep ai_calls

# 헬스 체크
curl localhost:8080/actuator/health | jq '.'
```

**기대 결과**:
- ✅ 모든 로그가 JSON 형식 (프로덕션)
- ✅ 요청 ID가 모든 로그에 포함
- ✅ AI 호출 메트릭이 증가
- ✅ 캐시 히트율이 측정됨

### 6.3 Docker 검증

**Docker 빌드**:
```bash
# 이미지 빌드
docker build -t interview-note-api:latest .

# 이미지 크기 확인
docker images interview-note-api
# 기대: < 200MB
```

**Docker Compose 검증**:
```bash
# 전체 스택 실행
export OPENAI_API_KEY=sk-proj-...
docker-compose up -d

# 헬스 체크
curl localhost:8080/actuator/health

# 질문 조회
curl localhost:8080/questions

# PostgreSQL 연결 확인
docker-compose exec postgres psql -U interviewuser -d interviewdb -c "\dt"
# 기대: questions, interview_answers, ai_feedbacks 테이블 존재
```

---

## 7. 성공 기준

Phase 3 완료 시 다음 기준을 만족해야 합니다:

### 7.1 사용자 경험
- ✅ 모든 페이지가 일관된 디자인 적용 (Tailwind CSS)
- ✅ 모바일 반응형 동작
- ✅ HTMX로 부드러운 인터랙션
- ✅ 사용자 친화적 에러 페이지 (404, 500, Rate Limit)
- ✅ 실시간 피드백 (로딩, 성공/실패 메시지)

### 7.2 운영 가능성
- ✅ JSON 형식 구조화된 로그
- ✅ Prometheus 메트릭 수집 (AI 호출, 캐시, Rate Limit)
- ✅ 헬스 체크 엔드포인트 동작
- ✅ 요청 ID 추적 가능

### 7.3 배포 준비
- ✅ Docker 이미지 빌드 성공 (< 200MB)
- ✅ Docker Compose로 전체 스택 실행
- ✅ PostgreSQL 환경에서 Flyway 마이그레이션 성공
- ✅ 환경별 설정 분리 (dev, prod)

### 7.4 문서화
- ✅ README.md - Docker 실행 방법 추가
- ✅ SETUP_GUIDE.md - 프로덕션 배포 가이드 추가
- ✅ CHANGELOG.md - Phase 3 변경 이력 추가
- ✅ `phase3_implementation_plan.md` 완성

---

## 8. 향후 확장 (Phase 4 이후)

Phase 3 완료 후 고려할 수 있는 확장 기능:

### 8.1 사용자 관리
- 로그인/회원가입 (Spring Security)
- 사용자별 답변 이력 분리
- 프로필 관리

### 8.2 고급 기능
- 질문 검색 (Elasticsearch)
- 답변 음성 녹음 (Web Speech API)
- AI 음성 피드백 (TTS)
- 답변 비교 기능 (이전 답변과 비교)

### 8.3 분석 기능
- 사용자 대시보드 (평균 점수, 개선 추이)
- 카테고리별 강점/약점 분석
- 학습 추천 시스템

### 8.4 확장성
- Redis 캐싱 (중복 요청 방지)
- Kafka/RabbitMQ (비동기 AI 평가)
- 다중 AI 모델 지원 (Claude, Gemini)

---

**작성일**: 2026-04-13
**버전**: 1.0
**작성자**: Claude Code with 호준

**다음 단계**: Phase 3A부터 시작 (UI/UX 개선)
