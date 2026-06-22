# Interview Note API - 포트폴리오

> **5년차 서버 개발자 포트폴리오**
>
> 최종 업데이트: 2026-06-20

---

## 목차

1. [프로젝트 한 줄 소개](#1-프로젝트-한-줄-소개)
2. [포트폴리오 상세 소개](#2-포트폴리오-상세-소개)
3. [이력서용 핵심 Bullet](#3-이력서용-핵심-bullet)
4. [면접 대비 설명](#4-면접-대비-설명)
5. [사실 검증 체크리스트](#5-사실-검증-체크리스트)

---

# 1. 프로젝트 한 줄 소개

> **OpenAI 기반 면접 답변 평가 및 실시간 모의 면접 서비스** (47자)

또는

> **AI가 면접 답변을 평가하고 실시간 피드백을 제공하는 백엔드 서비스** (38자)

---

# 2. 포트폴리오 상세 소개

## 2.1 프로젝트 개요

### 서비스 소개
면접 준비생이 답변을 작성하면 AI가 논리성, 구체성, 직무 적합성, 전달력을 평가하고 개선 방향과 모범답변을 제공하는 웹 서비스입니다. SSE(Server-Sent Events) 기반 실시간 모의 면접 기능도 제공합니다.

### 개발 배경
- 취업 준비생의 면접 답변 연습을 체계적으로 지원하고자 시작
- 단순 질문 은행이 아닌, 답변 개선 과정을 기록하고 추적하는 리뷰 중심 서비스
- 프론트엔드보다 도메인 설계와 AI 연동에 집중한 백엔드 중심 프로젝트

### 개발 방식 (투명 공개)

> **이 프로젝트는 Claude Code를 활용한 AI-assisted 개발 방식으로 설계·구현했습니다.**

| 역할 | 담당 주체 | 구체적 내용 |
|------|----------|------------|
| 문제 정의 | 본인 | "면접 준비생을 위한 AI 평가 서비스"라는 핵심 가치 정의 |
| MVP 범위 설정 | 본인 | 불필요한 기능(음성 녹음, 소셜 로그인 등)을 제외하고 핵심 플로우에 집중 |
| 사용자 플로우 설계 | 본인 | 질문 선택 → 답변 작성 → AI 평가 → 리뷰 이력 관리 흐름 설계 |
| 기능 우선순위 결정 | 본인 | 8개 Phase를 순서대로 정의하고 우선순위 조정 |
| AI 기능 판단 | 본인 | OpenAI gpt-4o-mini 선택, 프롬프트 전략 결정, 캐싱/Rate Limit 정책 |
| 코드 작성 | Claude Code + 본인 | Claude Code가 코드 생성, 본인이 검토·수정·보완 |
| 코드 리뷰 및 수정 | 본인 | 생성된 코드의 구조, 네이밍, 예외 처리 검토 및 개선 |
| 테스트 검증 | 본인 | 테스트 실행, 실패 원인 분석, 엣지 케이스 추가 |
| 배포 및 운영 | 본인 | Docker 빌드, 환경 설정, 모니터링 구성 |

**핵심 메시지**: 코드의 많은 부분은 Claude Code가 생성했지만, **"무엇을 만들 것인가"와 "어떻게 검증할 것인가"는 본인이 판단**했습니다. AI 도구를 효과적으로 활용하면서도 품질과 방향성에 대한 책임을 가지고 개발했습니다.

---

## 2.2 주요 기능 (코드로 확인된 것만)

### 핵심 기능

| 기능 | 설명 | 근거 파일 |
|------|------|----------|
| AI 답변 평가 | OpenAI API를 통한 4개 항목(논리성, 구체성, 직무 적합성, 전달력) 평가 | `service/ai/OpenAiClientImpl.kt` |
| 실시간 모의 면접 | SSE 기반 스트리밍으로 AI와 대화형 면접 진행 | `service/MockInterviewService.kt` |
| 채용 공고 기반 질문 생성 | URL 입력 시 HTML 파싱 → AI가 맞춤 질문 10개 생성 | `service/JobPostingParserService.kt` |
| 17개 직무 지원 | 백엔드, 프론트엔드, DevOps 등 다양한 IT 직무별 평가 기준 | `domain/enums/JobField.kt` |
| 답변 품질 사전 검증 | 반복 문자/단어 감지로 AI API 비용 절감 | `service/validation/AnswerValidator.kt` |
| 중복 요청 캐싱 | SHA-256 해싱으로 동일 요청 24시간 캐싱 | `service/cache/DuplicateRequestCache.kt` |
| Rate Limiting | IP당 33회/시간 제한으로 비용 제어 | `service/ratelimit/RateLimitService.kt` |
| 회원 관리 | Spring Security 세션 기반 인증, BCrypt 암호화 | `config/SecurityConfig.kt` |
| 리뷰 이력 관리 | 과거 답변 및 AI 평가 결과 조회 | `service/ReviewService.kt` |

### 기능 상세 흐름

```
┌─────────────────────────────────────────────────────────────────┐
│                        사용자 플로우                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [질문 목록]  ─→  [답변 작성]  ─→  [AI 평가]  ─→  [리뷰 이력]    │
│       │              │              │              │           │
│       ▼              ▼              ▼              ▼           │
│  17개 직무별     품질 검증       OpenAI 호출    답변 히스토리    │
│  340개 질문     Rate Limit      스트리밍 SSE    재평가 가능      │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                     채용 공고 기반 면접                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [URL 입력]  ─→  [HTML 파싱]  ─→  [AI 질문 생성]  ─→  [모의 면접]│
│       │              │              │                │          │
│       ▼              ▼              ▼                ▼          │
│  채용 공고 URL   Jsoup 파싱     10개 맞춤 질문    실시간 평가    │
│                 97.8% 축소      난이도 분포       종합 피드백    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2.3 기술 스택

### Backend

| 기술 | 버전 | 선택 이유 | 근거 파일 |
|------|------|----------|----------|
| Kotlin | 2.2.21 | 간결한 문법, Null Safety, Java 호환성 | `build.gradle.kts` |
| Java Toolchain | 21 | LTS 버전, Virtual Threads 지원 | `build.gradle.kts` |
| Spring Boot | 3.5.14 | 생산성, 풍부한 생태계 | `build.gradle.kts` |
| Spring Data JPA | - | ORM 추상화, Repository 패턴 | `repository/` |
| Spring Security | - | 세션 기반 인증, 역할 기반 접근 제어 | `config/SecurityConfig.kt` |
| Flyway | - | DB 마이그레이션 버전 관리 | `db/migration/V1-V14` |
| Caffeine Cache | - | 인메모리 캐싱 (중복 방지, Rate Limit) | `build.gradle.kts` |

### AI Integration

| 기술 | 용도 | 근거 파일 |
|------|------|----------|
| OpenAI API (gpt-4o-mini) | 답변 평가, 질문 생성 | `service/ai/OpenAiClientImpl.kt` |
| RestTemplate | HTTP 클라이언트 | `config/OpenAiConfig.kt` |
| JSON Mode | 구조화된 응답 강제 | `service/ai/OpenAiClientImpl.kt` |
| SHA-256 해싱 | 중복 요청 감지 | `service/cache/DuplicateRequestCache.kt` |

### Frontend (Server-Side Rendering)

| 기술 | 용도 | 근거 파일 |
|------|------|----------|
| Thymeleaf | 템플릿 엔진 | `templates/` |
| Tailwind CSS | UI 스타일링 | `templates/*.html` |
| HTMX | 동적 인터랙션 (새로고침 없이) | `templates/*.html` |

### Infrastructure

| 기술 | 용도 | 근거 파일 |
|------|------|----------|
| Docker | 컨테이너화 (Multi-stage build) | `Dockerfile` |
| Docker Compose | 로컬 개발 환경 (PostgreSQL + App) | `docker-compose.yml` |
| PostgreSQL 15 | 프로덕션 DB | `docker-compose.yml` |
| H2 | 개발/테스트 DB | `application.properties` |
| Nginx | 리버스 프록시, SSE 버퍼링 설정 | `nginx-sse-config.conf` |

### Monitoring & Logging

| 기술 | 용도 | 근거 파일 |
|------|------|----------|
| Logback + Logstash Encoder | JSON 구조화 로깅 | `logback-spring.xml` |
| Micrometer + Prometheus | 메트릭 수집 | `application.properties` |
| Spring Actuator | Health Check (Liveness/Readiness) | `application.properties` |
| MDC | 요청 추적 (Request ID) | `filter/RequestIdFilter.kt` |

---

## 2.4 아키텍처 및 구현 구조

### 레이어드 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                       Presentation Layer                        │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌───────────┐  │
│  │ Question    │ │ Answer      │ │ Review      │ │ MockIntv  │  │
│  │ Controller  │ │ Controller  │ │ Controller  │ │ Controller│  │
│  └──────┬──────┘ └──────┬──────┘ └──────┬──────┘ └─────┬─────┘  │
└─────────┼───────────────┼───────────────┼──────────────┼────────┘
          │               │               │              │
          ▼               ▼               ▼              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Service Layer                            │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌───────────┐  │
│  │ Question    │ │ Interview   │ │ Review      │ │ MockIntv  │  │
│  │ Service     │ │ Service     │ │ Service     │ │ Service   │  │
│  └─────────────┘ └──────┬──────┘ └─────────────┘ └─────┬─────┘  │
│                         │                              │        │
│  ┌──────────────────────┼──────────────────────────────┼──────┐ │
│  │                 AI Integration Layer                │      │ │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐    │      │ │
│  │  │ PromptBuild │ │ OpenAi      │ │ Response    │    │      │ │
│  │  │ er          │ │ ClientImpl  │ │ Parser      │    │      │ │
│  │  └─────────────┘ └─────────────┘ └─────────────┘    │      │ │
│  └─────────────────────────────────────────────────────┘      │ │
│                                                               │ │
│  ┌──────────────────────────────────────────────────────────┐ │ │
│  │                   Support Services                       │ │ │
│  │  ┌───────────────┐ ┌───────────────┐ ┌─────────────────┐ │ │ │
│  │  │ DuplicateReq  │ │ RateLimit     │ │ Answer          │ │ │ │
│  │  │ Cache         │ │ Service       │ │ Validator       │ │ │ │
│  │  └───────────────┘ └───────────────┘ └─────────────────┘ │ │ │
│  └──────────────────────────────────────────────────────────┘ │ │
└───────────────────────────────────────────────────────────────┘ │
          │                                                        │
          ▼                                                        │
┌─────────────────────────────────────────────────────────────────┐
│                       Repository Layer                          │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌───────────┐  │
│  │ Question    │ │ Interview   │ │ AiFeedback  │ │ User      │  │
│  │ Repository  │ │ Answer Repo │ │ Repository  │ │ Repository│  │
│  └─────────────┘ └─────────────┘ └─────────────┘ └───────────┘  │
└─────────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Database Layer                           │
│        H2 (Development)  /  PostgreSQL 15 (Production)          │
└─────────────────────────────────────────────────────────────────┘
```

### 패키지 구조 (12개 패키지)

```
com.hojun.interviewnote.interviewnoteapi/
├── domain/           # 엔티티 10개 (Question, InterviewAnswer, AiFeedback, User, ...)
├── repository/       # Spring Data JPA Repository
├── service/
│   ├── ai/           # OpenAI 연동 (OpenAiClientImpl, PromptBuilder, ResponseParser)
│   ├── cache/        # 중복 요청 캐싱 (DuplicateRequestCache)
│   ├── ratelimit/    # Rate Limiting (RateLimitService)
│   └── validation/   # 답변 품질 검증 (AnswerValidator)
├── controller/       # REST/View 컨트롤러
├── dto/              # 데이터 전송 객체
├── exception/        # 커스텀 예외 (sealed class 활용)
├── filter/           # 요청 필터 (RequestIdFilter)
├── health/           # 헬스 체크 (OpenAiHealthIndicator)
└── config/           # 설정 클래스
```

### 엔티티 관계도 (ERD)

```
┌─────────────┐       ┌─────────────────┐       ┌─────────────┐
│    User     │       │ InterviewAnswer │       │  AiFeedback │
├─────────────┤       ├─────────────────┤       ├─────────────┤
│ id          │──┐    │ id              │──┐    │ id          │
│ email       │  │    │ questionId      │  │    │ interviewAns│
│ password    │  │    │ userId (FK)     │  │    │   werId(FK) │
│ role        │  └───▶│ answerText      │  └───▶│ logicScore  │
│ jobField    │       │ createdAt       │       │ specificity │
│ careerLevel │       └────────┬────────┘       │ jobFitScore │
└─────────────┘                │                │ modelAnswer │
                               │                └─────────────┘
┌─────────────┐                │
│  Question   │◀───────────────┘
├─────────────┤
│ id          │       ┌─────────────────┐       ┌─────────────┐
│ jobField    │       │   JobPosting    │       │ Generated   │
│ targetJob   │       ├─────────────────┤       │  Question   │
│ category    │──┐    │ id              │──┐    ├─────────────┤
│ content     │  │    │ originalUrl     │  │    │ id          │
│ difficulty  │  │    │ companyName     │  └───▶│ jobPostingId│
└─────────────┘  │    │ parsedContent   │       │ content     │
                 │    └─────────────────┘       │ difficulty  │
                 │                              │ aiReasoning │
                 │    ┌─────────────────┐       └─────────────┘
                 │    │  MockInterview  │
                 └───▶├─────────────────┤       ┌─────────────┐
                      │ id              │──┐    │ Interview   │
                      │ userId (FK)     │  │    │   Message   │
                      │ status          │  └───▶├─────────────┤
                      │ overallScore    │       │ id          │
                      └─────────────────┘       │ mockIntvId  │
                                                │ role        │
                                                │ content     │
                                                └─────────────┘
```

---

## 2.5 담당 역할 (AI-assisted 개발 투명 공개)

### 본인이 직접 수행한 역할

#### 1. 문제 정의 및 MVP 범위 설정

```
✅ 핵심 문제 정의
   - "면접 준비생이 답변을 체계적으로 연습하고 개선할 수 있는 도구가 필요하다"
   - 기존 서비스의 한계: 질문만 제공, 답변 피드백 없음

✅ MVP 범위 결정 (제외한 기능)
   - ❌ 음성 녹음/재생, STT
   - ❌ 소셜 로그인
   - ❌ 결제 기능
   - ❌ 프론트엔드 프레임워크 분리 (React/Vue)
   → 핵심 가치(AI 평가)에 집중하기 위한 의도적 제외
```

#### 2. 사용자 플로우 및 기능 우선순위 설계

```
✅ 8개 Phase 순서 결정
   Phase 1: 기반 구축 (AI 없이 전체 플로우)
   Phase 2: AI 연동 (OpenAI 통합)
   Phase 3: 완성도 향상 (Docker, 모니터링)
   Phase 4: 사용자 관리 (인증, 권한)
   Phase 5: 다중 직무 지원 (17개 직무)
   Phase 6: 채용 공고 기반 질문 생성
   Phase 7: AI 채팅 면접 (SSE)
   Phase 8: 개선 및 안정화
```

#### 3. AI 기능 판단 및 정책 결정

```
✅ 모델 선택: gpt-4o-mini
   - 비용 효율성 (gpt-4 대비 저렴)
   - 면접 평가에 충분한 성능

✅ 프롬프트 전략
   - JSON Mode 강제 (파싱 안정성)
   - Hallucination Prevention 지침 추가
   - 직무별 평가 기준 맞춤화

✅ 비용 제어 정책
   - 중복 요청 캐싱 (24시간)
   - Rate Limiting (33회/시간/IP)
   - 답변 품질 사전 검증 (AI 호출 전 차단)
```

#### 4. 코드 리뷰 및 품질 관리

```
✅ Claude Code 생성 코드 검토
   - 네이밍 컨벤션 준수 확인
   - 예외 처리 누락 보완
   - 불필요한 복잡성 제거

✅ 테스트 검증
   - 44개 테스트 파일 실행 및 검증
   - 엣지 케이스 추가 (반복 문자, 단어 검증)
   - 실패 원인 분석 및 수정
```

#### 5. 배포 및 운영 환경 구성

```
✅ Docker 환경 설정
   - Multi-stage build 최적화 (~180MB)
   - 환경별 profile 분리 (dev/prod)

✅ 모니터링 구성
   - Prometheus 메트릭 설정
   - JSON 구조화 로깅 설정
   - Health Check 엔드포인트 구성

✅ SSE 인프라 설정
   - Nginx 프록시 버퍼링 비활성화
   - 타임아웃 설정 최적화
```

### Claude Code가 수행한 역할

```
📝 코드 생성
   - 엔티티, Repository, Service, Controller 코드 작성
   - 테스트 코드 작성
   - 마이그레이션 스크립트 생성

📝 리팩토링
   - 코드 구조 개선 제안 및 적용
   - 중복 코드 제거

📝 문서화
   - 주석 및 문서 작성
   - CLAUDE.md, README.md 업데이트
```

### 협업 방식

```
1. 본인이 요구사항 정의 (예: "Rate Limiting 33회/시간으로 구현해줘")
2. Claude Code가 코드 생성
3. 본인이 검토 및 수정 요청
4. 테스트 실행 및 검증
5. 필요시 추가 요구사항 전달
```

---

## 2.6 기술적 의사결정 및 배운 점

### 의사결정 1: AI 응답 형식 고정 (JSON Mode)

**문제 상황**
- OpenAI API 응답이 자유 텍스트일 경우 파싱 복잡도 증가
- "강점: ...", "개선점: ..." 형식 불일치 시 서비스 장애 위험

**결정 내용**
```kotlin
// OpenAiClientImpl.kt
val requestBody = mapOf(
    "response_format" to mapOf("type" to "json_object"),
    // ...
)
```

**배운 점**
- AI 응답은 반드시 구조화된 형식(JSON Schema)으로 강제해야 안정적 파싱 가능
- 자유 텍스트 파싱은 "파싱 지옥"으로 이어짐

---

### 의사결정 2: SHA-256 기반 중복 요청 캐싱

**문제 상황**
- 동일한 답변을 반복 제출 시 AI API 비용 누적
- 사용자가 새로고침하거나 뒤로가기 시 중복 호출 발생

**결정 내용**
```kotlin
// DuplicateRequestCache.kt
fun generateCacheKey(questionId: Long, answerText: String): String {
    val input = "$questionId:$answerText"
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(input.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
```

**배운 점**
- 해시 기반 캐싱으로 응답 속도 개선 (캐시 히트 시 AI 호출 생략)
- 24시간 TTL로 적절한 캐시 수명 관리

---

### 의사결정 3: SSE(Server-Sent Events)로 실시간 스트리밍

**문제 상황**
- 모의 면접 시 AI 응답이 길어 사용자가 빈 화면에서 대기
- WebSocket은 양방향 통신이 필요 없는 상황에서 오버스펙

**결정 내용**
```kotlin
// MockInterviewService.kt
fun streamInterviewResponse(interviewId: Long): Flux<ServerSentEvent<String>> {
    return Flux.create { sink ->
        // AI 응답을 청크 단위로 전송
        openAiClient.streamCompletion(prompt) { chunk ->
            sink.next(ServerSentEvent.builder(chunk).build())
        }
    }
}
```

**배운 점**
- SSE는 서버 → 클라이언트 단방향 스트리밍에 적합
- Nginx 프록시 환경에서 버퍼링 비활성화 필수 (`X-Accel-Buffering: no`)

---

### 의사결정 4: 답변 품질 사전 검증 (AI 호출 전)

**문제 상황**
- "aaaa...", "123123..." 같은 무의미한 답변도 AI API 호출
- 비용 낭비 + AI Hallucination (없는 내용 창작) 위험

**결정 내용**
```kotlin
// AnswerValidator.kt
fun validate(answerText: String): ValidationResult {
    // 1. 반복 문자 70% 이상 → 거부
    // 2. 반복 단어 40% 이상 → 거부
    // 3. 고유 문자 5개 미만 → 거부
    // 4. 단어 10개 미만 → 거부
    // 5. 의미 있는 문자 50% 미만 → 거부
}
```

**배운 점**
- AI 호출 전 간단한 규칙 기반 필터링으로 비용 절감 가능
- 프롬프트에 Hallucination Prevention 지침 추가로 이중 방어

---

### 의사결정 5: 인터페이스 기반 AI 클라이언트 설계

**문제 상황**
- OpenAI API 의존도가 높으면 다른 모델(Claude, Gemini 등)로 교체 어려움
- 테스트 시 실제 API 호출하면 비용 발생

**결정 내용**
```kotlin
// AiClient.kt (인터페이스)
interface AiClient {
    fun requestFeedback(question: Question, answer: String): AiFeedbackResponse
}

// OpenAiClientImpl.kt (구현체)
@Service
@Profile("!test")
class OpenAiClientImpl : AiClient { ... }

// DummyAiClient.kt (테스트용)
@Service
@Profile("test")
class DummyAiClient : AiClient { ... }
```

**배운 점**
- 인터페이스 분리로 테스트 용이성 확보
- 향후 다른 AI 모델로 교체 시 구현체만 추가하면 됨

---

## 2.7 향후 개선 계획

### 단기 (구현 예정)

| 항목 | 우선순위 | 설명 |
|------|---------|------|
| CI/CD 파이프라인 | 높음 | GitHub Actions로 자동 빌드/배포 |
| 테스트 커버리지 측정 | 높음 | JaCoCo 설정 및 리포트 생성 |
| Phase 8 완료 | 중간 | 점수 계산 로직 개선, UI 개선 |

### 중기 (검토 중)

| 항목 | 설명 |
|------|------|
| Redis 캐싱 | 질문 목록 캐싱으로 DB 부하 감소 |
| 부하 테스트 | k6 또는 JMeter로 성능 측정 |
| 벡터DB + RAG | 질문 유사도 검색 고도화 |

### 장기 (아이디어)

| 항목 | 설명 |
|------|------|
| 다중 AI 모델 지원 | Claude, Gemini 등 선택 가능 |
| 음성 면접 지원 | STT/TTS 연동 |
| 기업 맞춤 서비스 | B2B 버전 |

---

# 3. 이력서용 핵심 Bullet

## 3.1 초압축 버전 (2줄)

```
• OpenAI 기반 면접 답변 평가 서비스 설계·구현 (Kotlin/Spring Boot)
• SSE 실시간 모의 면접, 17개 직무 지원, SHA-256 캐싱으로 비용 최적화
```

## 3.2 일반 버전 (4줄)

```
• OpenAI gpt-4o-mini 기반 면접 답변 평가 서비스 설계·구현 (Kotlin 2.2.21, Spring Boot 3.5.14)
• SSE(Server-Sent Events) 기반 실시간 모의 면접 기능 개발, Nginx 프록시 최적화
• SHA-256 해싱 캐싱 + IP 기반 Rate Limiting(33회/시간)으로 API 비용 제어
• Docker Multi-stage 빌드, Prometheus 메트릭, JSON 구조화 로깅으로 운영 환경 구축
```

## 3.3 상세 버전 (6줄)

```
• OpenAI gpt-4o-mini 기반 면접 답변 평가 서비스 설계·구현 (Kotlin 2.2.21, Spring Boot 3.5.14)
  - 4개 평가 항목(논리성, 구체성, 직무 적합성, 전달력), JSON Mode로 응답 형식 강제
• SSE(Server-Sent Events) 기반 실시간 모의 면접 기능 개발
  - Nginx 프록시 버퍼링 비활성화, 타임아웃 최적화로 안정적 스트리밍 구현
• SHA-256 해싱 기반 중복 요청 캐싱(24시간 TTL) + IP 기반 Rate Limiting(33회/시간)으로 API 비용 제어
• 17개 IT 직무별 맞춤 평가 기준 구현, 340개 사전 정의 질문 + 채용 공고 기반 동적 질문 생성
• Docker Multi-stage 빌드(~180MB), Prometheus 메트릭, JSON 구조화 로깅으로 프로덕션 환경 구축
• Claude Code를 활용한 AI-assisted 개발 방식 적용, 문제 정의·설계 판단·코드 리뷰·배포 담당
```

---

# 4. 면접 대비 설명

## 질문 1: 이 프로젝트에 대해 간단히 설명해주세요.

### 60초 답변

면접 준비생이 답변을 작성하면 AI가 평가해주는 웹 서비스입니다.

핵심 기능은 두 가지입니다. 첫째, 사용자가 면접 질문에 답변을 작성하면 OpenAI API가 논리성, 구체성, 직무 적합성, 전달력 4개 항목으로 평가하고 모범답변을 제공합니다. 둘째, SSE 기반 실시간 모의 면접으로 AI와 대화하듯 면접을 연습할 수 있습니다.

기술적으로는 Kotlin과 Spring Boot로 구현했고, 비용 제어를 위해 SHA-256 해싱 캐싱과 Rate Limiting을 적용했습니다. 17개 IT 직무를 지원하며, 채용 공고 URL을 입력하면 맞춤 질문도 생성합니다.

### 2분 답변

면접 준비생을 위한 AI 기반 면접 답변 평가 서비스입니다.

**서비스 배경**을 말씀드리면, 기존 면접 준비 서비스는 질문만 제공하고 답변에 대한 피드백이 없었습니다. 단순히 질문을 보고 혼자 연습하는 것보다, 체계적인 피드백을 받으며 개선할 수 있는 도구가 필요하다고 생각했습니다.

**핵심 기능**은 세 가지입니다.
1. **AI 답변 평가**: 사용자가 답변을 작성하면 OpenAI gpt-4o-mini가 논리성, 구체성, 직무 적합성, 전달력 4개 항목으로 평가합니다. JSON Mode를 사용해 응답 형식을 강제하여 파싱 안정성을 확보했습니다.
2. **실시간 모의 면접**: SSE(Server-Sent Events) 기반으로 AI와 대화형 면접을 진행합니다. 타이핑 애니메이션처럼 응답이 실시간으로 표시됩니다.
3. **채용 공고 기반 질문 생성**: 채용 공고 URL을 입력하면 HTML을 파싱하고, AI가 맞춤 질문 10개를 생성합니다.

**기술적 특징**으로는,
- Kotlin 2.2.21 + Spring Boot 3.5.14로 구현
- SHA-256 해싱으로 동일 요청 24시간 캐싱하여 API 비용 절감
- IP당 33회/시간 Rate Limiting으로 비용 제어
- Docker Multi-stage 빌드로 이미지 크기 약 180MB
- Prometheus 메트릭과 JSON 구조화 로깅으로 모니터링 환경 구축

**개발 방식**에 대해 투명하게 말씀드리면, 이 프로젝트는 Claude Code를 활용한 AI-assisted 개발로 진행했습니다. 코드의 많은 부분은 Claude Code가 생성했지만, 저는 문제 정의, MVP 범위 설정, 설계 판단, 코드 리뷰, 테스트 검증, 배포를 담당했습니다. AI 도구를 효과적으로 활용하면서도 품질에 대한 책임을 가지고 개발했습니다.

---

## 질문 2: AI-assisted 개발이라고 하셨는데, 구체적으로 어떻게 진행하셨나요?

### 60초 답변

Claude Code라는 AI 코딩 도구를 활용했습니다.

제가 담당한 부분은 "무엇을 만들 것인가"를 정의하는 것이었습니다. 면접 평가 서비스라는 핵심 가치를 정의하고, 불필요한 기능을 제외하여 MVP 범위를 설정했습니다. 또한 8개 Phase로 개발 순서를 결정하고, AI 비용 제어 정책도 제가 결정했습니다.

Claude Code가 담당한 부분은 코드 생성입니다. 제가 요구사항을 전달하면 코드를 작성하고, 저는 그 코드를 검토하고 수정했습니다. 테스트 실행과 검증도 제가 직접 수행했습니다.

결론적으로, AI가 코드를 작성하더라도 방향성과 품질에 대한 책임은 제가 졌습니다.

### 2분 답변

Claude Code라는 AI 코딩 어시스턴트를 활용한 협업 개발 방식입니다.

**역할 분담**을 명확히 말씀드리면,

**제가 담당한 역할**:
1. **문제 정의**: "면접 준비생을 위한 AI 평가 서비스"라는 핵심 가치 정의
2. **MVP 범위 설정**: 음성 녹음, 소셜 로그인, 결제 기능 등 불필요한 것을 제외하고 핵심 플로우에 집중
3. **기능 우선순위 결정**: 8개 Phase로 순서 정의 (기반 구축 → AI 연동 → 완성도 향상 → 사용자 관리 → 다중 직무 → 채용 공고 → 모의 면접 → 개선)
4. **AI 비용 정책 결정**: gpt-4o-mini 선택, Rate Limiting 33회/시간, 캐싱 정책 등
5. **코드 리뷰**: 생성된 코드의 네이밍, 예외 처리, 구조 검토 및 수정 요청
6. **테스트 검증**: 44개 테스트 파일 실행, 엣지 케이스 추가
7. **배포 및 운영**: Docker 빌드, 환경 설정, 모니터링 구성

**Claude Code가 담당한 역할**:
- 엔티티, Repository, Service, Controller 코드 작성
- 테스트 코드 작성
- 마이그레이션 스크립트 생성
- 리팩토링 및 문서화

**협업 프로세스**는 이렇습니다:
1. 제가 요구사항을 명확히 정의 (예: "SHA-256 해싱으로 중복 요청 캐싱 구현해줘")
2. Claude Code가 코드 생성
3. 제가 검토하고 문제점 피드백
4. 수정 후 테스트 실행 및 검증

**이 방식의 가치**는, AI 도구의 생산성을 활용하면서도 "무엇을 만들 것인가"와 "어떻게 검증할 것인가"에 대한 판단은 개발자가 해야 한다는 것입니다. AI가 코드를 작성해도, 요구사항 정의, 설계 판단, 품질 검증은 사람의 역할입니다.

---

## 질문 3: OpenAI API 비용 관리를 어떻게 하셨나요?

### 60초 답변

세 가지 방법으로 비용을 제어했습니다.

첫째, **중복 요청 캐싱**입니다. 동일한 질문과 답변 조합을 SHA-256으로 해싱하여 24시간 캐싱합니다. 사용자가 새로고침하거나 뒤로가기해도 캐시에서 응답하므로 API 호출이 발생하지 않습니다.

둘째, **Rate Limiting**입니다. IP당 시간당 33회로 제한했습니다. 이 수치는 월간 최대 비용을 약 5달러 이내로 유지하기 위해 계산한 값입니다.

셋째, **답변 품질 사전 검증**입니다. AI 호출 전에 반복 문자, 반복 단어 등 무의미한 답변을 필터링합니다. "aaaa..." 같은 답변은 AI에 보내지 않고 바로 거부합니다.

### 2분 답변

OpenAI API는 호출당 비용이 발생하므로, 세 가지 계층으로 비용을 제어했습니다.

**1단계: 답변 품질 사전 검증 (AI 호출 전 차단)**

AnswerValidator 클래스에서 AI 호출 전에 무의미한 답변을 필터링합니다.
- 반복 문자 70% 이상: "aaaa..." → 거부
- 반복 단어 40% 이상: "여기는 여기는 여기는..." → 거부
- 고유 문자 5개 미만, 단어 10개 미만: 거부
- 의미 있는 문자(한글/영어) 50% 미만: 거부

이렇게 필터링하면 AI API를 호출하지 않아 비용이 0원입니다.

**2단계: 중복 요청 캐싱**

DuplicateRequestCache 클래스에서 SHA-256 해싱을 사용합니다.
```
캐시 키 = SHA-256(questionId + ":" + answerText)
```
동일한 답변은 24시간 동안 캐시에서 응답합니다. 사용자가 새로고침, 뒤로가기, 또는 의도적으로 같은 답변을 다시 제출해도 API 호출이 발생하지 않습니다.

**3단계: Rate Limiting**

RateLimitService 클래스에서 IP당 시간당 33회로 제한합니다.
- 계산 근거: gpt-4o-mini 비용 약 $0.002/요청 가정
- 33회 × 24시간 × 30일 ≈ 월 $4.75 (단일 IP 최대)
- Caffeine Cache로 인메모리 관리, X-Forwarded-For 헤더로 프록시 환경 지원

**추가로 프롬프트 레벨에서도 비용 제어**:
- maxTokens 제한으로 응답 길이 제어
- 모범답변 400-600자로 제한

이 세 계층을 통해 불필요한 API 호출을 최소화하고, 예측 가능한 비용 구조를 만들었습니다.

---

## 질문 4: SSE(Server-Sent Events)를 선택한 이유와 구현 시 어려웠던 점은?

### 60초 답변

모의 면접에서 AI 응답을 실시간으로 보여주기 위해 SSE를 선택했습니다.

WebSocket 대신 SSE를 선택한 이유는, 모의 면접은 서버에서 클라이언트로의 단방향 스트리밍만 필요했기 때문입니다. 사용자 입력은 일반 HTTP POST로 충분했고, WebSocket의 양방향 통신은 오버스펙이었습니다.

구현 시 어려웠던 점은 Nginx 프록시 환경에서 버퍼링 문제였습니다. Nginx가 기본적으로 응답을 버퍼링해서 실시간 스트리밍이 안 됐습니다. `X-Accel-Buffering: no` 헤더와 `proxy_buffering off` 설정으로 해결했습니다.

### 2분 답변

**SSE 선택 이유**

실시간 모의 면접에서 AI 응답이 타이핑되듯 표시되어야 했습니다. 선택지는 세 가지였습니다:

1. **Polling**: 주기적으로 서버에 요청 → 리소스 낭비, 지연 발생
2. **WebSocket**: 양방향 통신 → 모의 면접에서는 서버→클라이언트 단방향만 필요
3. **SSE**: 서버→클라이언트 단방향 스트리밍 → 요구사항에 정확히 부합

사용자 입력(답변 제출)은 일반 HTTP POST로 충분했고, AI 응답 스트리밍만 실시간이면 됐습니다. SSE가 가장 적합했습니다.

**구현 시 어려움**

**Nginx 버퍼링 문제**가 가장 어려웠습니다.

로컬에서는 잘 동작했는데, Nginx 프록시를 거치면 응답이 한 번에 몰아서 왔습니다. 원인은 Nginx가 기본적으로 응답을 버퍼링하기 때문이었습니다.

해결 방법:
```nginx
# nginx-sse-config.conf
location /api/interviews/stream {
    proxy_buffering off;
    proxy_cache off;
    proxy_set_header X-Accel-Buffering no;
    proxy_read_timeout 3600;
}
```

또한 Spring 컨트롤러에서도 헤더 추가:
```kotlin
@GetMapping("/stream/{interviewId}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
fun streamInterview(@PathVariable interviewId: Long): Flux<ServerSentEvent<String>> {
    // X-Accel-Buffering: no 헤더 추가
}
```

**타임아웃 문제**도 있었습니다. AI 응답이 길어지면 기본 타임아웃에 걸렸습니다. `proxy_read_timeout 3600`으로 1시간으로 늘렸습니다.

**배운 점**:
- SSE는 HTTP 기반이라 기존 인프라와 호환성이 좋지만, 프록시 설정에 주의 필요
- 프로덕션 환경에서는 로컬과 다른 문제가 발생할 수 있으므로, 실제 환경에서 테스트 중요

---

## 질문 5: 이 프로젝트에서 가장 어려웠던 기술적 도전은?

### 60초 답변

가장 어려웠던 것은 AI 응답의 안정적인 파싱이었습니다.

초기에는 OpenAI 응답이 자유 텍스트로 오면서 파싱이 불안정했습니다. "강점:" 다음에 번호가 붙기도 하고, 줄바꿈 형식도 일정하지 않았습니다.

해결책으로 JSON Mode를 도입했습니다. OpenAI API에 `response_format: json_object`를 설정하면 응답이 반드시 JSON으로 옵니다. 여기에 프롬프트에서 정확한 JSON 스키마를 명시하고, ResponseParser에서 스키마 검증을 추가했습니다.

이 경험으로 AI 통합에서는 응답 형식을 반드시 구조화해야 한다는 것을 배웠습니다.

### 2분 답변

가장 어려웠던 기술적 도전은 크게 두 가지였습니다.

**1. AI 응답 파싱 안정화**

초기에는 OpenAI 응답이 자유 텍스트로 왔습니다.
```
강점:
1. STAR 기법 활용
2. 구체적 수치 제시

개선점:
- 기술적 깊이 부족
- 팀 협업 경험 언급 필요
```

이런 형식은 파싱이 매우 불안정했습니다. "강점" 다음에 콜론이 있을 수도 있고 없을 수도 있고, 번호 형식도 달랐습니다.

**해결 방법**:
1. **JSON Mode 도입**: `response_format: { type: "json_object" }` 설정
2. **프롬프트에 정확한 스키마 명시**:
   ```
   반드시 아래 JSON 형식으로 응답하세요:
   {
     "scores": { "logic": 4, "specificity": 3, ... },
     "strengths": ["...", "..."],
     "improvements": ["...", "..."],
     "modelAnswer": "...",
     "overallComment": "..."
   }
   ```
3. **ResponseParser에서 스키마 검증**: 필수 필드 누락 시 예외 발생, Fallback 피드백 제공

**2. AI Hallucination 방지**

무의미한 답변(예: "여기는 여기는 여기는...")에 대해 AI가 없는 내용을 창작하는 문제가 있었습니다. 실제로 "반복 표현이 강점"이라고 평가하는 경우도 있었습니다.

**해결 방법**:
1. **사전 검증**: AnswerValidator에서 반복 문자/단어 감지하여 AI 호출 전 차단
2. **프롬프트 개선**: Hallucination Prevention 지침 추가
   ```
   중요한 평가 지침:
   - 답변에 없는 내용을 추측하거나 창작하지 마세요
   - 실제로 답변에 나타난 강점만 언급하세요
   - 반복 표현 감지 시 improvements에 지적
   - 내용 부족 시 strengths를 억지로 만들지 마세요
   ```

**배운 점**:
- AI 통합에서는 응답 형식 강제 + 입력 검증 + 프롬프트 가드레일의 다층 방어가 필요
- "AI가 알아서 해주겠지"가 아니라, 명확한 제약 조건을 설정해야 안정적인 서비스 가능

---

# 5. 사실 검증 체크리스트

## 5.1 확인된 사실 (코드 기반)

| 항목 | 확인 상태 | 근거 파일 | 비고 |
|------|----------|----------|------|
| Kotlin 2.2.21 | ✅ | `build.gradle.kts` | `kotlin("jvm") version "2.2.21"` |
| Java 21 Toolchain | ✅ | `build.gradle.kts` | `jvmToolchain(21)` |
| Spring Boot 3.5.14 | ✅ | `build.gradle.kts` | `id("org.springframework.boot") version "3.5.14"` |
| OpenAI gpt-4o-mini | ✅ | `application.properties` | `openai.model=gpt-4o-mini` |
| SHA-256 캐싱 | ✅ | `service/cache/DuplicateRequestCache.kt` | `MessageDigest.getInstance("SHA-256")` |
| Rate Limiting 33회/시간 | ✅ | `service/ratelimit/RateLimitService.kt` | `maxRequestsPerHour = 33` |
| 24시간 캐시 TTL | ✅ | `service/cache/DuplicateRequestCache.kt` | `expireAfterWrite(24, TimeUnit.HOURS)` |
| 17개 직무 | ✅ | `domain/enums/JobField.kt` | enum 값 17개 정의 |
| 340개 질문 | ✅ | `db/migration/V7__Insert_Questions.sql` | INSERT 문 340개 |
| 10개 엔티티 | ✅ | `domain/` 디렉토리 | Question, InterviewAnswer, AiFeedback 등 |
| 44개 테스트 파일 | ✅ | `src/test/kotlin/` | 디렉토리 내 파일 수 |
| Spring Security | ✅ | `config/SecurityConfig.kt` | `@EnableWebSecurity` |
| BCrypt 암호화 | ✅ | `config/SecurityConfig.kt` | `BCryptPasswordEncoder` |
| Flyway V1-V14 | ✅ | `db/migration/` | 14개 마이그레이션 파일 |
| Docker Multi-stage | ✅ | `Dockerfile` | `FROM ... AS build` + `FROM ...` |
| Prometheus 메트릭 | ✅ | `application.properties` | `management.endpoints.web.exposure.include=prometheus` |
| JSON 로깅 | ✅ | `logback-spring.xml` | `LogstashEncoder` |
| SSE 지원 | ✅ | `service/MockInterviewService.kt` | `Flux<ServerSentEvent<String>>` |
| Nginx SSE 설정 | ✅ | `nginx-sse-config.conf` | `proxy_buffering off` |
| JSON Mode | ✅ | `service/ai/OpenAiClientImpl.kt` | `response_format: { type: "json_object" }` |
| 답변 품질 검증 | ✅ | `service/validation/AnswerValidator.kt` | 반복 문자/단어 검사 로직 |

## 5.2 미확인/부재 항목

| 항목 | 상태 | 설명 |
|------|------|------|
| CI/CD (GitHub Actions) | ❌ 없음 | `.github/workflows/` 디렉토리 없음 |
| Redis | ❌ 미사용 | Caffeine Cache만 사용 |
| 부하 테스트 | ❌ 미실시 | k6, JMeter 등 설정 없음 |
| 테스트 커버리지 측정 | ❌ 미설정 | JaCoCo 설정 없음 |
| Docker 이미지 크기 ~180MB | ⚠️ 미측정 | CLAUDE.md에 기재되어 있으나 실제 빌드 후 측정 필요 |
| 캐시 히트 시 1,700배+ 속도 향상 | ⚠️ 미측정 | CLAUDE.md에 기재되어 있으나 벤치마크 없음 |

## 5.3 과장 가능성 있는 표현

| 표현 | 검증 상태 | 권장 수정 |
|------|----------|----------|
| "~180MB Docker 이미지" | ⚠️ 미측정 | "Multi-stage 빌드로 이미지 크기 최적화" (수치 제외) |
| "1,700배+ 속도 향상" | ⚠️ 미측정 | "캐시 히트 시 AI 호출 생략으로 응답 속도 개선" (수치 제외) |
| "97.8% HTML 크기 감소" | ✅ 테스트 확인 | `Phase6DHtmlAnalysisTest`에서 검증됨 |
| "월 $4.75 이내 비용" | ⚠️ 계산 기반 | "계산상 월 $5 이내 비용" (실제 운영 데이터 없음 명시) |

## 5.4 면접 전 확인 필요 항목

### 반드시 확인

| 항목 | 확인 방법 | 상태 |
|------|----------|------|
| 프로젝트 빌드 성공 | `./gradlew build` | ☐ |
| 테스트 전체 통과 | `./gradlew test` | ☐ |
| Docker 빌드 성공 | `docker build -t interview-note-api .` | ☐ |
| 실제 Docker 이미지 크기 | `docker images interview-note-api` | ☐ |
| OpenAI API 키 설정 | 환경변수 확인 | ☐ |
| 로컬 실행 확인 | `./gradlew bootRun` 후 `http://localhost:8080` | ☐ |

### 권장 확인

| 항목 | 확인 방법 | 상태 |
|------|----------|------|
| AI 평가 기능 동작 | 실제 답변 제출 후 피드백 확인 | ☐ |
| SSE 모의 면접 동작 | 모의 면접 시작 후 스트리밍 확인 | ☐ |
| Rate Limiting 동작 | 34회 이상 요청 시 차단 확인 | ☐ |
| 캐싱 동작 | 동일 답변 재제출 시 캐시 응답 확인 | ☐ |

## 5.5 면접 시 주의사항

### 솔직하게 말해야 할 것

1. **AI-assisted 개발**: "코드의 많은 부분은 Claude Code가 생성했습니다. 저는 문제 정의, 설계 판단, 코드 리뷰, 테스트 검증을 담당했습니다."

2. **미구현 기능**: "CI/CD 파이프라인은 아직 구축하지 않았습니다. GitHub Actions로 구현 예정입니다."

3. **미측정 수치**: "Docker 이미지 크기나 캐시 성능 향상 수치는 실제로 측정하지 않았습니다. 추정치입니다."

### 강조해도 되는 것

1. **기술적 판단**: "gpt-4o-mini 선택, Rate Limiting 정책, 캐싱 전략 등은 제가 결정했습니다."

2. **비용 제어 설계**: "세 계층(사전 검증, 캐싱, Rate Limiting)으로 API 비용을 제어하는 구조를 설계했습니다."

3. **문제 해결**: "SSE Nginx 버퍼링 문제, AI Hallucination 문제 등을 직접 분석하고 해결했습니다."

4. **테스트 검증**: "44개 테스트 파일을 실행하고 엣지 케이스를 추가했습니다."

---

## 부록: 저장소 분석 테이블

### A. 확인된 사실 (전체)

| 카테고리 | 항목 | 확인 상태 | 근거 파일 |
|----------|------|----------|----------|
| **언어/프레임워크** | Kotlin 2.2.21 | ✅ | `build.gradle.kts` |
| | Java 21 Toolchain | ✅ | `build.gradle.kts` |
| | Spring Boot 3.5.14 | ✅ | `build.gradle.kts` |
| | Spring Data JPA | ✅ | `build.gradle.kts` |
| | Spring Security | ✅ | `config/SecurityConfig.kt` |
| | Flyway | ✅ | `build.gradle.kts`, `db/migration/` |
| | Caffeine Cache | ✅ | `build.gradle.kts` |
| **AI 연동** | OpenAI gpt-4o-mini | ✅ | `application.properties` |
| | JSON Mode | ✅ | `service/ai/OpenAiClientImpl.kt` |
| | RestTemplate | ✅ | `config/OpenAiConfig.kt` |
| **패키지 구조** | 12개 패키지 | ✅ | `src/main/kotlin/` |
| | 10개 엔티티 | ✅ | `domain/` |
| | 44개 테스트 파일 | ✅ | `src/test/kotlin/` |
| **비용 제어** | SHA-256 캐싱 | ✅ | `DuplicateRequestCache.kt` |
| | 24시간 TTL | ✅ | `DuplicateRequestCache.kt` |
| | Rate Limiting 33회/시간 | ✅ | `RateLimitService.kt` |
| | 답변 품질 검증 | ✅ | `AnswerValidator.kt` |
| **직무 지원** | 17개 직무 | ✅ | `domain/enums/JobField.kt` |
| | 340개 질문 | ✅ | `V7__Insert_Questions.sql` |
| | 4개 경력 수준 | ✅ | `domain/enums/CareerLevel.kt` |
| **인프라** | Docker Multi-stage | ✅ | `Dockerfile` |
| | Docker Compose | ✅ | `docker-compose.yml` |
| | PostgreSQL 15 | ✅ | `docker-compose.yml` |
| | H2 (개발) | ✅ | `application.properties` |
| | Nginx SSE 설정 | ✅ | `nginx-sse-config.conf` |
| **모니터링** | Prometheus 메트릭 | ✅ | `application.properties` |
| | JSON 로깅 | ✅ | `logback-spring.xml` |
| | Health Check | ✅ | `application.properties` |
| | MDC 요청 추적 | ✅ | `filter/RequestIdFilter.kt` |
| **인증** | 세션 기반 인증 | ✅ | `SecurityConfig.kt` |
| | BCrypt 암호화 | ✅ | `SecurityConfig.kt` |
| | 역할 기반 접근 제어 | ✅ | `domain/enums/Role.kt` |
| **실시간** | SSE 스트리밍 | ✅ | `MockInterviewService.kt` |
| **DB 마이그레이션** | V1-V14 | ✅ | `db/migration/` |

### B. 미확인/부재 항목 (전체)

| 카테고리 | 항목 | 상태 | 비고 |
|----------|------|------|------|
| **CI/CD** | GitHub Actions | ❌ 없음 | `.github/workflows/` 부재 |
| **캐싱** | Redis | ❌ 미사용 | Caffeine Cache만 사용 |
| **테스트** | 부하 테스트 | ❌ 미실시 | k6, JMeter 등 부재 |
| | 테스트 커버리지 | ❌ 미설정 | JaCoCo 부재 |
| **성능 측정** | Docker 이미지 크기 | ⚠️ 미측정 | 빌드 후 확인 필요 |
| | 캐시 성능 향상률 | ⚠️ 미측정 | 벤치마크 필요 |
| | API 응답 시간 | ⚠️ 미측정 | 프로덕션 운영 데이터 없음 |

---

**문서 끝**
