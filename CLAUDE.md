# CLAUDE.md

This file provides guidance to Claude Code when working with the Interview Note API project.

**최종 업데이트**: 2026-04-30 (Phase 1-6 완료 상태 반영)

## Project Overview

**면접 리뷰 웹 애플리케이션** - 사용자가 면접 질문에 텍스트로 답변하면, AI가 평가와 개선 포인트, 모범답변을 제공하고, 사용자는 이를 저장해 리뷰할 수 있는 웹앱입니다.

### 프로젝트 목적
- 취업 준비생의 면접 연습 지원
- AI 기반 답변 평가 및 피드백 제공
- 답변 이력 관리 및 개선 추적
- 포트폴리오용 Spring Boot + AI 연동 프로젝트

### 핵심 가치
- **실사용성**: 실제 면접 준비에 도움이 되는 서비스
- **리뷰 중심**: 단순 질문 은행이 아닌, 답변 개선 과정을 기록
- **백엔드 중심**: 프론트엔드보다 도메인 설계와 AI 연동에 집중

## Technology Stack

### Backend
- **Language**: Kotlin 2.2.21 with Java 21 toolchain
- **Framework**: Spring Boot 3.5.14
- **Build Tool**: Gradle with Kotlin DSL
- **Database**: H2 (development) / PostgreSQL 15 (production)
- **ORM**: Spring Data JPA + Hibernate
- **Migration**: Flyway
- **Testing**: JUnit 5 with Spring Boot Test

### AI Integration
- **AI Model**: OpenAI gpt-4o-mini
- **HTTP Client**: RestTemplate (직접 구현)
- **Response Format**: JSON Mode (구조화된 응답)
- **Cache**: Caffeine Cache (중복 방지, Rate Limiting)

### Frontend (Phase 3에서 추가)
- **Template Engine**: Thymeleaf (server-side rendering)
- **UI Framework**: Tailwind CSS
- **UI Enhancement**: HTMX (페이지 새로고침 없는 인터랙션)

### Monitoring & Logging (Phase 3에서 추가)
- **Logging**: Logback + Logstash Encoder (JSON 로깅)
- **Metrics**: Micrometer + Prometheus
- **Health Check**: Spring Boot Actuator (Liveness/Readiness probes)
- **Request Tracing**: MDC (Mapped Diagnostic Context)

### DevOps (Phase 3에서 추가)
- **Container**: Docker (Multi-stage build, ~180MB)
- **Orchestration**: Docker Compose
- **Environments**: dev (H2) / prod (PostgreSQL)

### 왜 이 스택인가?
- 단일 Spring Boot 애플리케이션으로 배포 단순화
- Thymeleaf + HTMX로 프론트엔드 복잡도 최소화 (React/Vue 없이)
- 백엔드 역량에 집중 (프론트/백엔드 분리 안 함)
- Docker로 환경 일관성 보장
- Prometheus 메트릭으로 프로덕션 모니터링 가능
- Claude Code가 컨텍스트 잡기 쉬운 구조

## MVP Scope

### 포함되는 기능 (MVP)
1. **질문 제공**: 카테고리별 면접 질문 목록 조회
2. **질문 상세**: 질문 내용 및 답변 작성 폼
3. **답변 제출**: 사용자 답변 텍스트 입력 및 저장
4. **AI 평가**: OpenAI API를 통한 답변 평가
5. **모범답변 생성**: AI가 생성한 모범답변 제공
6. **리뷰 이력**: 과거 답변 및 평가 내역 조회
7. **리뷰 상세**: 특정 답변의 평가 결과 다시 보기

### 제외되는 기능 (Not in MVP)
- ❌ 로그인/회원가입 (비회원 단일 사용자 모드로 시작)
- ❌ 음성 녹음/재생
- ❌ STT (Speech-to-Text)
- ❌ 회사별 맞춤 질문 추천
- ❌ 소셜 로그인
- ❌ 결제 기능
- ❌ 랭킹/통계 대시보드 고도화
- ❌ 관리자 고급 기능
- ❌ 프론트엔드 프레임워크 분리 (React, Vue 등)
- ❌ 벡터DB/RAG (추후 고려)

## Domain Model

### 핵심 엔티티

#### 1. Question (질문)
```kotlin
@Entity
data class Question(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val jobField: String = "IT",     // MVP: "IT" 고정, 추후 "영업", "경영", "회계" 등 확장 가능
    val targetJob: String,           // 예: "백엔드 개발자", "프론트엔드 개발자"
    val category: String,            // 예: "기술역량", "문제해결", "협업경험"
    @Column(columnDefinition = "TEXT")
    val content: String,             // 질문 내용
    val difficulty: String,          // 예: "EASY", "MEDIUM", "HARD"
    val isActive: Boolean = true,    // 활성화 여부

    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
```

#### 2. InterviewAnswer (사용자 답변)
```kotlin
@Entity
data class InterviewAnswer(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val questionId: Long,
    @Column(columnDefinition = "TEXT")
    val answerText: String,          // 사용자가 작성한 답변

    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
```

#### 3. AiFeedback (AI 평가 결과)
```kotlin
@Entity
class AiFeedback(  // data class → class (JPA 최적화, equals/hashCode 커스터마이징)
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val interviewAnswerId: Long,

    // 평가 점수 (1-5) - 모든 직무에 공통 적용 가능한 기준
    val logicScore: Int,              // 논리성 (IT: 기술적 논리, 영업: 설득 논리)
    val specificityScore: Int,        // 구체성 (IT: 기술 스택, 영업: 실적 수치)
    val jobFitScore: Int,             // 직무 적합성 (모든 직무 공통)
    val deliveryScore: Int,           // 전달력 (모든 직무 공통)

    // 피드백 내용
    @Column(columnDefinition = "TEXT")
    val strengths: String,            // JSON array: ["강점1", "강점2"]
    @Column(columnDefinition = "TEXT")
    val improvements: String,         // JSON array: ["개선점1", "개선점2"]
    @Column(columnDefinition = "TEXT")
    val modelAnswer: String,          // AI가 생성한 모범답변
    @Column(columnDefinition = "TEXT")
    val overallComment: String,       // 종합 코멘트

    // 메타데이터
    val jobField: String = "IT",      // Question의 jobField 복사 (확장성)
    val modelName: String,            // 예: "gpt-4o-mini"
    val promptVersion: String,        // 프롬프트 버전 (예: "v1.0")
    val tokenUsageInput: Int,         // 입력 토큰 수
    val tokenUsageOutput: Int,        // 출력 토큰 수
    @Column(columnDefinition = "TEXT")
    val rawResponse: String,          // OpenAI 원본 응답 (디버깅용)

    val answerTextHash: String?,      // SHA-256 해시 (중복 방지용, Phase 2B에서 추가)

    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    // 평균 점수 계산 프로퍼티 (Phase 2에서 추가)
    val averageScore: Double
        get() = (logicScore + specificityScore + jobFitScore + deliveryScore) / 4.0
}
```

## Answer Quality Validation (답변 품질 검증)

### 검증 정책

사용자가 무의미한 답변(예: "aaaa...", "123123...")을 제출하는 것을 방지하기 위해 **2단계 검증**을 수행합니다.

#### Phase 1: 사전 검증 (AI 호출 전)

답변 제출 시 기본적인 품질 체크를 수행하여 **AI API 비용을 절감**합니다.

**검증 기준**:
1. **반복 문자 체크**: 동일한 문자가 70% 이상이면 거부
   - 예: "aaaaaaa...", "bbbbbbb..."
2. **반복 단어 체크** (Phase 2B+에서 추가): 동일한 단어가 40% 이상이면 거부
   - 예: "여기는 여기는 여기는 여기는..." → 거부
   - 구현: `hasExcessiveRepeatedWords()` in `AnswerValidator.kt`
   - AI Hallucination 방지 효과 (AI가 없는 내용을 창작하는 것 방지)
3. **고유 문자 개수**: 최소 5개 이상의 서로 다른 문자 필요
   - 예: "aaabbbcccddd" (4개) → 거부
4. **최소 단어 수**: 공백으로 구분된 최소 10개 단어 필요
   - 예: "한 두 세 네" (4개) → 거부
5. **의미 있는 문자 비율**: 한글/영어/기본 문장 부호가 50% 이상
   - 예: "1234!@#$%^&*" (숫자/특수문자만) → 거부

**거부 시 동작**:
- 답변 작성 페이지로 redirect
- 빨간색 경고 배너 표시
- 구체적인 에러 메시지 제공

**구현 위치**:
- `AnswerValidator.kt` - 검증 로직
- `AnswerController.kt` - 제출 시 검증 호출

#### Phase 2: AI 평가 후 저품질 경고

AI 평가 후 평균 점수가 **1.5점 미만**이면 저품질 경고를 표시합니다.

**경고 기준**:
- 평균 점수 < 1.5점 (4개 항목 평균)

**경고 시 동작**:
- 피드백 페이지 상단에 노란색 경고 배너 표시
- "답변 품질이 낮습니다" 메시지
- "다시 답변하기" 버튼 제공

**목적**:
- AI API는 호출되어 평가는 받되, 사용자에게 개선 권장
- 무의미한 답변으로 학습하는 것 방지

### 검증 흐름도

```
사용자 답변 제출
    ↓
Rate Limit 체크
    ↓
Bean Validation (50-2000자)
    ↓
[Phase 1] 사전 검증
    ├─ 통과 → AI 평가 진행
    └─ 실패 → 에러 메시지 + redirect
         ↓
    AI 평가 완료
         ↓
[Phase 2] 점수 체크
    ├─ ≥ 1.5점 → 정상 피드백 표시
    └─ < 1.5점 → 저품질 경고 배너 + 피드백 표시
```

### 검증 로직 예외 처리

**검증을 우회하지 않는 경우**:
- 기술 면접에서는 답변 품질이 중요하므로 예외 없이 모두 검증
- 관리자나 테스트 계정 예외 없음 (MVP에서는 단일 사용자)

**완화된 검증**:
- 프롬프트에서는 "2-3개 항목" 권장했지만, 실제로는 **1-5개**까지 허용
- AI가 상황에 따라 유연하게 개수를 조정 가능
- 답변 품질이 낮으면 1개, 높으면 5개까지 반환 가능

---

## AI Integration Design

### OpenAI API 응답 형식 (고정된 JSON 스키마)

**중요**: AI 응답은 반드시 아래 JSON 형식을 따라야 합니다. 자유 텍스트 응답은 파싱 지옥으로 이어집니다.

```json
{
  "scores": {
    "logic": 4,
    "specificity": 3,
    "jobFit": 4,
    "delivery": 3
  },
  "strengths": [
    "STAR 기법을 활용한 구조적 답변",
    "구체적인 수치와 성과 제시"
  ],
  "improvements": [
    "기술적 깊이를 더할 수 있는 부분",
    "팀 협업 경험을 더 강조하면 좋을 것"
  ],
  "modelAnswer": "저는 프로젝트 초기 설계 단계에서... (400-600자)",
  "overallComment": "전반적으로 답변 구조가 명확하며, 성과를 수치로 제시한 점이 좋습니다."
}
```

### 프롬프트 설계 원칙

#### System Role (MVP: IT 직무용)

**개선된 프롬프트** (Phase 2B+에서 Hallucination Prevention 적용):
```
당신은 백엔드 개발자 면접을 준비하는 지원자를 돕는 면접 코치입니다.
당신의 역할은 합격/불합격을 판정하는 것이 아니라, 답변을 개선하도록 구체적인 피드백을 제공하는 것입니다.

평가 기준:
- 논리성(logic): 기술적 사고의 논리적 흐름과 일관성
- 구체성(specificity): 구체적 기술 스택, 사례, 수치 제시 정도
- 직무 적합성(jobFit): 질문 의도와 개발 직무 연관성
- 전달력(delivery): 기술 개념을 명확하고 이해하기 쉽게 설명하는 능력

중요한 평가 지침 (Hallucination Prevention):
1. **정직한 평가**: 답변이 반복적이거나 무의미하면 솔직하게 지적하세요
2. **사실 기반**: 답변에 없는 내용을 추측하거나 창작하지 마세요
3. **강점 검증**: 실제로 답변에 나타난 강점만 언급하세요
4. **반복 표현 감지**: 같은 단어/문구가 반복되면 improvements에 지적
5. **내용 부족 시**: 답변이 짧거나 구체성이 부족하면 strengths를 억지로 만들지 말고, improvements를 더 구체적으로 작성
6. **엄격한 기준**: 형식적이거나 추상적인 답변은 낮은 점수를 주세요

나쁜 답변 예시:
- 반복 표현: "저는 중요하게 여기는 여기는 여기는..."
- 추상적 답변: "저는 열심히 노력했습니다"
- 구체성 부족: "Spring을 사용했습니다" (어떻게? 왜? 무엇을?)

이런 경우:
- strengths: 가능한 한 적게 (또는 0개도 가능)
- improvements: 구체적이고 실질적인 개선 방향 제시
- 점수: 1-2점 (매우 낮게)

출력 규칙:
- 반드시 JSON 형식으로 응답
- 각 점수는 1-5 사이 정수
- **strengths는 0-5개** (답변 품질에 따라 유연하게, 낮은 품질은 0개 가능)
- **improvements는 1-5개** (최소 1개, 많을수록 구체적)
- modelAnswer는 400-600자 이내
- 한국어로 답변
- 과도한 단정이나 공격적 표현 금지
```

**확장성 노트**: `PromptBuilder`는 `Question.jobField` 값에 따라 다른 System Role을 생성할 수 있도록 설계합니다.
MVP에서는 "IT"만 사용하지만, 나중에 "영업", "경영" 등으로 확장 시 프롬프트 템플릿만 추가하면 됩니다.

### 비용 제어 장치
- ✅ **답변 글자 수 제한**: 최대 2000자 (Bean Validation)
- ✅ **모범답변 길이 제한**: 400-600자 (maxTokens = 800)
- ✅ **중복 요청 방지**: 동일 questionId + answerText 조합 24시간 캐싱 (Phase 2B에서 구현 완료)
  - SHA-256 해싱으로 중복 감지 (`answerTextHash` 필드)
  - 속도 향상: 1,700배+ (5초 → 3ms)
  - 비용: 캐시 히트 시 100% 절감
- ✅ **Rate limiting**: IP당 33회/시간 (Caffeine Cache 기반, Phase 2B에서 구현 완료)
  - 최대 월 비용: $4.75 (단일 IP)
  - X-Forwarded-For 헤더 지원 (프록시 환경)
- ✅ **메타데이터 저장**: modelName, tokenUsage, promptVersion, answerTextHash 필수 기록
- ✅ **Fallback 메커니즘**: AI 오류 시 더미 피드백 제공 (서비스 중단 방지)

## Architecture Principles

### 레이어 구조
```
Controller → Service → Repository
         ↓
   Filter (RequestIdFilter)
         ↓
      ViewModel/DTO
         ↓
    AI Integration Layer
    (OpenAI Client, PromptBuilder, ResponseParser)
         ↓
   Support Services
   (DuplicateRequestCache, RateLimitService, AnswerValidator)
```

### 서비스 분리
- **InterviewService**: 질문/답변 비즈니스 로직
- **AiFeedbackService**: AI 평가 요청 조율 (Fallback 포함)
- **QuestionService**: 질문 조회 로직
- **ReviewService**: 리뷰 이력 조회 (Phase 1에서 추가)
- **OpenAiClient**: OpenAI API 호출 (인터페이스 기반, 교체 가능)
- **PromptBuilder**: 프롬프트 템플릿 조합 (jobField 기반 동적 생성 가능)
- **ResponseParser**: JSON 응답 파싱 및 검증
- **DuplicateRequestCache**: 중복 요청 캐싱 (24시간, Phase 2B에서 추가)
- **RateLimitService**: IP 기반 Rate Limiting (Phase 2B에서 추가)
- **AnswerValidator**: 답변 품질 사전 검증 (Phase 2B에서 추가)

### 설계 원칙
1. **Controller에서 직접 OpenAI 호출 금지** → 서비스 계층 통과 필수
2. **AI 응답 형식 고정** → JSON 스키마 엄격히 준수
3. **원본 응답 저장** → rawResponse 필드에 디버깅용 보관
4. **버전 관리** → promptVersion으로 프롬프트 변경 추적
5. **교체 가능 구조** → 나중에 Claude API나 다른 모델로 교체 용이

## Implementation Status

**프로젝트 현재 상태** (2026-04-30 기준): **Phase 1-6 모두 완료**, 17개 직무 지원, 채용 공고 기반 질문 생성, 프로덕션 배포 준비 완료

### ✅ Phase 1: 기반 구축 (완료)
AI 없이 전체 플로우 완성
1. ✅ Spring Boot 프로젝트 뼈대 생성
2. ✅ 엔티티 3개 (Question, InterviewAnswer, AiFeedback) 정의
3. ✅ Repository 인터페이스 생성
4. ✅ Flyway 마이그레이션 스크립트 작성
5. ✅ 질문 목록 페이지 구현 (Thymeleaf)
6. ✅ 질문 상세 + 답변 작성 페이지 구현
7. ✅ 답변 저장 기능 구현
8. ✅ 더미 피드백으로 결과 페이지 구현 (AI 없이 하드코딩된 평가)
9. ✅ 리뷰 이력 목록 페이지 구현
10. ✅ 리뷰 상세 페이지 구현

### ✅ Phase 2: AI 연동 (완료)
실제 OpenAI API 통합 및 비용 최적화
11. ✅ OpenAI API 클라이언트 구현 (RestTemplate 기반)
12. ✅ PromptBuilder 구현 (Hallucination Prevention 포함)
13. ✅ ResponseParser 구현 (JSON → AiFeedback DTO, 엄격한 검증)
14. ✅ RealAiFeedbackService 구현 (Fallback 메커니즘 포함)
15. ✅ 중복 요청 방지 (SHA-256 해싱, 24시간 캐싱)
16. ✅ Rate Limiting (IP당 33회/시간, Caffeine Cache)
17. ✅ 답변 품질 사전 검증 (AnswerValidator - 문자/단어 반복 체크)
18. ✅ 메타데이터 저장 (tokenUsage, modelName, answerTextHash)

### ✅ Phase 3: 완성도 향상 (완료)
프로덕션 준비 및 사용자 경험 개선
19. ✅ Tailwind CSS + HTMX 적용
20. ✅ 에러 페이지 개선 (404, 500, Rate Limit, AI Error)
21. ✅ JSON 구조화 로깅 (Logback + Logstash Encoder)
22. ✅ Prometheus 메트릭 수집 (AI 호출, 캐시, HTTP)
23. ✅ Health Check (Liveness/Readiness probes)
24. ✅ Docker 컨테이너화 (Multi-stage build, ~180MB)
25. ✅ Docker Compose 설정 (PostgreSQL + App)
26. ✅ 환경별 설정 분리 (dev/prod profiles)
27. ✅ RequestIdFilter (요청 추적, MDC)
28. ✅ OpenAiHealthIndicator (AI 연결 상태 체크)

### ✅ Phase 4: 사용자 관리 (완료)
회원가입/로그인 및 사용자별 데이터 분리

#### Phase 4A: 인증 및 권한 (완료)
29. ✅ Spring Security 통합 (세션 기반 인증)
30. ✅ 회원가입/로그인 UI (Thymeleaf + Tailwind)
31. ✅ User 엔티티 및 Repository
32. ✅ BCrypt 비밀번호 암호화
33. ✅ 역할 기반 접근 제어 (USER, ADMIN)
34. ✅ V4 migration (users 테이블 생성)

#### Phase 4B: 사용자별 데이터 분리 (완료)
35. ✅ InterviewAnswer에 userId 외래키 추가
36. ✅ V5 migration (user_id 컬럼 추가)
37. ✅ 사용자별 답변 조회/생성 제한
38. ✅ 홈페이지 개인화 (최근 리뷰 3개)
39. ✅ 타 사용자 답변 접근 차단 (403 Forbidden)

### ✅ Phase 5: 다중 직무 지원 (완료) ✨
IT 단일 직무에서 17개 직무로 확장 + 사용자 프로필

#### Phase 5A: 직무 확장 기반 (완료)
40. ✅ JobField enum (17개 직무) 정의
41. ✅ CareerLevel enum (4개 경력 수준) 정의
42. ✅ User 엔티티에 jobField, careerLevel 필드 추가
43. ✅ V6 migration (job_field, career_level 컬럼 추가)
44. ✅ V7 migration (340개 질문 데이터 INSERT)

#### Phase 5B: 프로필 및 필터링 (완료)
45. ✅ QuestionRepository jobField 필터링 메서드 추가
46. ✅ QuestionService jobField 파라미터 추가 (IT 기본값)
47. ✅ getCategoriesByAllJobFields() 메서드 (동적 카테고리)
48. ✅ UserProfileDto, UpdateProfileRequest DTO
49. ✅ ProfileController (GET /profile, POST /profile/update)
50. ✅ profile/settings.html 템플릿

#### Phase 5C: AI 개인화 (완료)
51. ✅ PromptBuilder 17개 직무 프롬프트 구현
52. ✅ buildBasePrompt() 공통 구조 (중복 제거)
53. ✅ 직무별 평가 기준 맞춤화 (논리성, 구체성)
54. ✅ QuestionController jobField 필터 + 동적 카테고리
55. ✅ HomeController 개인화 (직무 기반 추천 질문)
56. ✅ questions/list.html 직무 필터 UI + JavaScript 동적 카테고리

#### Phase 5D: 테스트 및 문서화 (완료)
57. ✅ Phase5IntegrationTest (23개 통합 테스트)
58. ✅ 전체 테스트 통과 (245개)
59. ✅ PHASE5_STEP17_TEST_REPORT.md 작성
60. ✅ CHANGELOG.md 업데이트 (0.4.0, 0.4.1, 0.5.0)
61. ✅ README.md 업데이트 (17개 직무 반영)
62. ✅ CLAUDE.md 업데이트 (Phase 5 완료 상태)
63. ✅ PHASE5_MIGRATION_GUIDE.md 작성

### ✅ Phase 6: 채용 공고 기반 질문 생성 (완료) 🚀

#### Phase 6A: 채용 공고 파싱 (완료)
64. ✅ JobPosting 엔티티 생성 (originalUrl, companyName, jobTitle, selectedJobField, inferredJobField)
65. ✅ GeneratedQuestion 엔티티 생성 (content, category, difficulty, aiReasoning, orderIndex)
66. ✅ V9 migration (job_postings, generated_questions 테이블)
67. ✅ JobPostingParserService 구현 (Jsoup + AI Fallback)
68. ✅ Jsoup 1.17.2 의존성 추가

#### Phase 6B: AI 질문 생성 (완료)
69. ✅ QuestionGeneratorService 구현 (AI 통합)
70. ✅ PromptBuilder 확장 (질문 생성 프롬프트)
71. ✅ QuestionResponseParser 구현 (JSON 검증)
72. ✅ Fallback 질문 10개 정의

#### Phase 6C: UI 및 통합 (완료)
73. ✅ JobPostingController 구현 (CRUD)
74. ✅ JobPostingService 구현 (orchestration)
75. ✅ UI 템플릿: create.html, questions.html
76. ✅ V10 migration (interview_answers.generated_question_id)

#### Phase 6D: 버그 수정 - HTML 파싱 개선 (완료)
77. ✅ cleanHtml() Regex → Jsoup text() (97.8% 크기 감소)
78. ✅ maxTokens 설정 불일치 수정 (800 → 3000)
79. ✅ Phase6DHtmlAnalysisTest 작성 및 검증

#### Phase 6E: 추가 버그 수정 (완료)
80. ✅ 난이도 분포 강제 (EASY 3, MEDIUM 4, HARD 3)
81. ✅ GeneratedQuestion 답변 제출 버그 수정 (질문 ID 매칭 오류)
82. ✅ InterviewService.submitAnswerForGeneratedQuestion() 추가
83. ✅ GeneratedQuestionController POST 엔드포인트 추가
84. ✅ questions/answer.html Form action 동적 처리

### ✅ Phase 7: AI 채팅 면접 (완료) 💬

**브랜치**: `feat/chat-interview`
**완료일**: 2026-05-03
**문서**: PHASE7_AI_CHAT_INTERVIEW.md, PHASE7_COMPLETION_REPORT.md

85. ✅ MockInterview 엔티티 (면접 세션 관리)
86. ✅ InterviewMessage 엔티티 (대화 메시지)
87. ✅ V11, V12 migration (mock_interviews, interview_messages 테이블)
88. ✅ SSE (Server-Sent Events) 실시간 스트리밍
89. ✅ 채용 공고 기반 맞춤 면접
90. ✅ 실시간 답변 평가 (개별 점수 + 피드백)
91. ✅ 종합 평가 생성 (강점, 개선점, 모범답변)
92. ✅ UI/UX (채팅 인터페이스, 타이핑 애니메이션)
93. ✅ Nginx 프록시 설정 (SSE 버퍼링 방지)

### ⏳ Phase 8: AI 채팅 면접 개선 (진행 중) 🔧

**브랜치**: `feat/interview-improvement`
**시작일**: 2026-05-04
**문서**:
- PHASE8_AI_CHAT_INTERVIEW_IMPROVEMENTS.md (개선 사항 요약)
- phase8_ai_chat_interview_plan.md (상세 실행 계획)

**핵심 개선 사항** (10가지):
1. ⏳ 종합 평가 점수 계산 로직 개선 (가중 평균, AI 프롬프트 엄격화)
2. ⏳ 종합 피드백 길이 증가 (400-600자 → 800-1200자)
3. ⏳ 강점/개선점 개수 유연화 (고정 3개 → 0-5개)
4. ✅ URL 구조 검토 (변경 안함)
5. ⏳ 리뷰 이력 페이지 통합 (질문 연습 + AI 면접 2개 탭)
6. ⏳ "이어서 연습하기" vs "새로 연습하기" 구분
7. ⏳ 채용 공고 기반 AI 면접 UI 추가
8. ⏳ 사용 방법 안내 UI 추가
9. ⏳ 경력 수준 선택 및 난이도 조정 (4단계)
10. ⏳ 짧은 답변 품질 검증 강화

**구현 계획**:
- Phase 8A: 점수 계산 및 피드백 개선 (최우선) 🔴
- Phase 8B: 경력 수준 및 UI 개선 🟡
- Phase 8C: 리뷰 통합 및 재개 기능 🟢
- Phase 8D: 테스트 및 문서화 🔴

### 📝 향후 작업 제안 (Phase 9+)

1. **Phase 9: 성능 최적화**
   - Redis 캐싱으로 질문 목록 성능 향상
   - DB 쿼리 최적화 (N+1 해결)
   - CDN 정적 리소스 제공

2. **Phase 10: AI 고도화**
   - 벡터DB + RAG 도입 (질문 유사도 검색)
   - 다중 직무 선택 (User가 여러 직무 관심 가능)
   - 직무별 통계 대시보드 (답변 개수, 평균 점수)

## Coding Guidelines

### Code Style
- **Google Kotlin Style Guide 준수**: 모든 Kotlin 코드는 [Google Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)를 따릅니다
  - 들여쓰기: 4 spaces
  - 최대 줄 길이: 100자 (단, URL이나 긴 문자열은 예외)
  - Import 순서: 알파벳 순, 와일드카드 import 금지
  - 함수/변수명: camelCase
  - 상수명: UPPER_SNAKE_CASE
  - 클래스명: PascalCase
  - Nullable 타입: 명시적으로 `?` 사용, `!!` 최소화
  - 확장 함수/프로퍼티 적극 활용

### DO
- ✅ 작은 단위로 기능 분할
- ✅ 각 레이어의 책임 명확히 분리
- ✅ AI 응답은 항상 JSON 스키마 검증
- ✅ 원본 응답(rawResponse) 반드시 저장
- ✅ 프롬프트 버전 관리
- ✅ 테스트 작성 (최소한 Service 레이어)
- ✅ Magic Number 대신 상수 사용
- ✅ DRY 원칙 준수 (중복 코드 제거)
- ✅ Spring Bean 주입 활용 (ObjectMapper 등)
- ✅ 명시적 예외 처리 (커스텀 예외 클래스)

### DON'T
- ❌ Controller에서 직접 OpenAI 호출
- ❌ AI 응답을 자유 텍스트로 받기
- ❌ MVP 범위 밖 기능 추가
- ❌ 처음부터 복잡한 구조 설계
- ❌ 프론트엔드/백엔드 분리 (MVP 단계)
- ❌ 과도한 추상화나 미래 확장성 고려
- ~~❌ `mapNotNull`과 `return@mapNotNull null` 조합 사용~~ (해결됨 - ReviewService 리팩토링 완료)
- ❌ ObjectMapper 등 중복 인스턴스 생성 → ObjectMapperConfig로 중앙화
- ❌ Nullable 강제 언랩핑 (`!!`) 남용
- ❌ JPA 엔티티에 data class 사용 → 일반 class 사용 (equals/hashCode 커스터마이징 필요)

## Scalability Considerations

### MVP 전략: IT 직무 집중, 구조는 확장 가능
MVP에서는 **IT 직무 면접에만 집중**하지만, 데이터 모델은 다양한 직무로 확장 가능하도록 설계되었습니다.

#### 현재 구조의 확장 포인트
1. **jobField**: Question과 AiFeedback에 추가됨 (기본값 "IT")
2. **평가 기준**: 모든 직무에 적용 가능한 일반적 기준 사용
   - 논리성: IT는 기술적 논리, 영업은 설득 논리
   - 구체성: IT는 기술 스택, 영업은 실적 수치
   - 직무 적합성: 모든 직무 공통
   - 전달력: 모든 직무 공통
3. **PromptBuilder**: jobField 기반 동적 프롬프트 생성 가능

#### 추후 확장 방법 (V2)
```kotlin
// 1. 새로운 직무 질문 데이터 추가
Question(jobField = "영업", targetJob = "영업관리자", ...)

// 2. PromptBuilder에 분기 추가
class PromptBuilder {
    fun buildSystemPrompt(jobField: String, targetJob: String): String {
        return when (jobField) {
            "IT" -> buildItPrompt(targetJob)
            "영업" -> buildSalesPrompt(targetJob)
            "경영" -> buildManagementPrompt(targetJob)
            else -> buildDefaultPrompt(targetJob)
        }
    }
}
```

#### MVP에서 해야 할 것
- ✅ jobField는 모든 질문에 "IT"로 설정
- ✅ 평가 기준은 IT 개발자 맥락으로 해석
- ✅ PromptBuilder는 IT용 프롬프트만 생성

#### MVP에서 하지 말아야 할 것
- ❌ 다른 직무 질문 데이터 추가
- ❌ jobField별 분기 로직 구현
- ❌ 동적 평가 기준 테이블 설계

**핵심**: 구조는 확장 가능하지만, 구현은 IT에만 집중하여 MVP 복잡도를 최소화합니다.

## Working with Claude Code

### 프로젝트 현재 상태 (2026-04-19 기준)
- ✅ **Phase 1-3 모두 완료**: MVP 기반, AI 연동, 프로덕션 준비 모두 완료
- ✅ **프로덕션 배포 준비 완료**: Docker, 모니터링, 로깅 인프라 구축 완료
- ✅ **총 코드**: 약 2,000줄 (main) + 3,951줄 (test)
- ✅ **테스트 커버리지**: 포괄적 (12+ 테스트 파일)
- 📝 **향후 작업**: Phase 4 (사용자 관리) 또는 성능 최적화

### 좋은 요청 방식
```
"질문 목록 조회 API를 구현해줘.
GET /questions 엔드포인트로, category와 difficulty로 필터링 가능하게.
QuestionController, QuestionService, QuestionRepository 레이어 구조로."
```

### 나쁜 요청 방식
```
"면접 리뷰 앱 전체 만들어줘"
```

### 작업 단위 분할 예시
1. "Question 엔티티와 Repository 생성"
2. "질문 목록 조회 화면 구현 (Controller + Thymeleaf)"
3. "답변 저장 기능 구현"
4. "OpenAI 클라이언트 인터페이스 구현"

## Build and Development Commands

### Building the project
```bash
./gradlew build
```

### Running the application
```bash
./gradlew bootRun
```

### Running tests
```bash
./gradlew test
```

### Database Migration
```bash
# Flyway migrations are applied automatically on startup
# Migration files: src/main/resources/db/migration/
```

## Environment Variables

### Required (for AI integration)
```bash
export OPENAI_API_KEY=sk-...
```

### Optional
```bash
export OPENAI_MODEL=gpt-4o-mini  # Default model (실제 사용 모델)
export PROMPT_VERSION=v1.0       # Prompt version for tracking
```

## Package Structure

```
com.hojun.interviewnote.interviewnoteapi/
├── domain/
│   ├── Question.kt
│   ├── InterviewAnswer.kt
│   └── AiFeedback.kt
├── repository/
│   ├── QuestionRepository.kt
│   ├── InterviewAnswerRepository.kt
│   └── AiFeedbackRepository.kt
├── service/
│   ├── ai/
│   │   ├── AiClient.kt (인터페이스)
│   │   ├── OpenAiClientImpl.kt
│   │   ├── PromptBuilder.kt
│   │   └── ResponseParser.kt
│   ├── cache/                      (Phase 2B에서 추가)
│   │   └── DuplicateRequestCache.kt
│   ├── ratelimit/                  (Phase 2B에서 추가)
│   │   └── RateLimitService.kt
│   ├── validation/                 (Phase 2B에서 추가)
│   │   └── AnswerValidator.kt
│   ├── InterviewService.kt
│   ├── AiFeedbackService.kt
│   ├── QuestionService.kt
│   └── ReviewService.kt            (Phase 1에서 추가)
├── controller/
│   ├── QuestionController.kt
│   ├── AnswerController.kt
│   ├── ReviewController.kt
│   └── HomeController.kt           (Phase 1에서 추가)
├── dto/
│   ├── QuestionDto.kt
│   ├── AnswerSubmitDto.kt
│   ├── FeedbackDto.kt
│   ├── AnswerWithFeedbackDto.kt
│   └── ReviewSummaryDto.kt
├── exception/                       (Phase 2에서 추가)
│   ├── AiExceptions.kt (sealed class)
│   ├── NotFoundExceptions.kt
│   ├── RateLimitExceededException.kt
│   └── GlobalExceptionHandler.kt
├── filter/                          (Phase 3B에서 추가)
│   └── RequestIdFilter.kt
├── health/                          (Phase 3B에서 추가)
│   └── OpenAiHealthIndicator.kt
└── config/
    ├── OpenAiConfig.kt
    └── ObjectMapperConfig.kt       (Phase 2에서 추가)
```

## Remember

이 프로젝트의 핵심은:
1. **완성 가능한 MVP 범위 유지**
2. **AI 평가의 안정성** (JSON 스키마 엄수)
3. **리뷰 기능의 가치** (단순 질문 앱이 아님)
4. **백엔드 설계 역량 증명** (면접 포인트)
5. **확장 가능한 구조, 단순한 구현** (IT 집중, 구조만 범용적)

### 면접 어필 포인트
- "MVP는 IT 직무로 시작했지만, jobField를 활용해 다양한 직무로 확장 가능하도록 설계했습니다"
- "평가 기준을 일반적으로 정의하여 IT 외 직무에도 적용 가능합니다"
- "PromptBuilder 패턴으로 직무별 프롬프트를 동적 생성할 수 있습니다"

Claude Code에게 요청할 때는 항상 이 문서의 원칙을 참조하세요.
