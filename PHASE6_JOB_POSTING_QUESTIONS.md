# PRD: 채용 공고 기반 맞춤형 질문 생성

**프로젝트**: Interview Note API - Phase 6
**작성일**: 2026-04-27
**버전**: 1.0
**현재 상태**: Phase 1-5 완료 (17개 직무, 340개 정적 질문 지원)
**핵심 기능**: 채용 공고 URL 기반 AI 맞춤형 질문 생성

---

## 📋 Executive Summary

### 배경
현재 Interview Note API는 정적으로 등록된 340개의 면접 질문을 제공하고 있습니다. 사용자는 질문을 선택하고 답변을 작성하면 AI가 평가를 제공하는 구조입니다. 그러나 **실제 지원하는 채용 공고에 맞춤화된 질문**이 없어 실전 준비에 한계가 있습니다.

### 목표
**채용 공고 URL 기반 맞춤형 질문 생성**: 사용자가 관심 있는 채용 공고 URL을 입력하면, AI가 공고 내용을 분석하여 10개의 맞춤형 면접 질문을 자동 생성합니다.

### 기대 효과
- **실전 준비 향상**: 실제 지원하는 공고에 맞춘 질문으로 연습 가능
- **차별화된 피드백**: 공고 요구사항과 사용자 답변의 적합도를 정확히 평가
- **사용자 참여 증가**: 340개 정적 질문보다 개인화된 경험 제공
- **채용 공고 중심 학습**: 특정 회사/포지션에 최적화된 면접 준비

---

## 🎯 기능 요구사항

### 1.1 공고 URL 입력 및 파싱

**사용자 스토리**:
> "백엔드 개발자로 지원하려는 원티드 공고가 있습니다. 이 공고에 맞는 면접 질문을 추천받고 싶습니다."

**기능 명세**:
- 사용자는 채용 공고 URL을 입력합니다 (원티드, 사람인, 잡코리아 등)
- 시스템은 URL에서 채용 공고를 파싱합니다:
  - 회사명
  - 포지션명
  - 공고 내용 (직무 설명, 자격 요건)
  - 필수 기술 스택
  - 우대 기술 스택
- 파싱 실패 시 사용자가 직접 입력할 수 있는 폼을 제공합니다.

**파싱 전략**:
- **주요 사이트 (원티드, 사람인, 잡코리아)**: Jsoup 라이브러리를 사용한 웹 스크래핑
- **기타 사이트**: OpenAI API를 사용한 HTML 파싱 (Fallback)
- **파싱 불가**: 사용자가 공고 내용을 직접 복사/붙여넣기

**입력 제약**:
- URL 길이: 최대 200자
- 공고 내용: 최대 6,000자 (AI 토큰 제한 고려)

---

### 1.2 직무 선택 및 확정

**기능 명세**:
- 사용자는 채용 공고 등록 시 **직무를 직접 선택**할 수 있습니다 (17개 직무 중 선택):
  - IT, SALES, ACCOUNTING, MARKETING, PLANNING, HR, ADMIN, DESIGN, MD, SERVICE, PRODUCTION, CONSTRUCTION, MEDICAL, EDUCATION, MEDIA, FINANCE, PUBLIC
- **미선택 시**: AI가 공고 내용을 분석하여 직무를 추론합니다 (`inferredJobField`).
- 추론된 직무는 사용자에게 표시되며, 수정 가능합니다.

**UI**:
- 공고 등록 폼에 **직무 선택 드롭다운** 추가 (선택 사항)
- 미선택 시: "AI가 자동으로 직무를 분석합니다" 안내 문구

---

### 1.3 AI 기반 질문 생성

**기능 명세**:
- AI가 채용 공고를 분석하여 **10개의 맞춤형 면접 질문**을 생성합니다.
- 질문 카테고리는 **선택된 직무에 따라 동적으로 결정**됩니다 (기존 질문 목록과 동일한 구조):

**직무별 카테고리 예시**:
| 직무 | 카테고리 1 | 카테고리 2 | 카테고리 3 |
|------|-----------|-----------|-----------|
| IT | 기술역량 | 문제해결 | 협업경험 |
| SALES | 고객관리 | 실적달성 | 협상스킬 |
| ACCOUNTING | 재무분석 | 세무지식 | 리스크관리 |
| MARKETING | 캠페인기획 | 데이터분석 | 콘텐츠전략 |
| PLANNING | 전략수립 | 시장분석 | 프로젝트관리 |

- 카테고리별 질문 개수는 AI가 공고 내용에 따라 유연하게 배분합니다 (예: 기술 중심 공고면 카테고리 1이 6개, 카테고리 2가 3개, 카테고리 3이 1개).
- 난이도 분포:
  - EASY: 3문항 (기본 개념, 경험 유무)
  - MEDIUM: 4문항 (심화 기술, 프로젝트 경험)
  - HARD: 3문항 (트레이드오프, 설계 결정)

**AI 프롬프트 예시** (IT 직무):
```
채용 공고 정보:
회사명: 카카오
포지션: 백엔드 개발자
직무: IT

필수 기술: Kotlin, Spring Boot, MySQL, Docker
우대 기술: Kubernetes, Redis, Kafka

위 공고를 기반으로 실전 면접 질문 10개를 생성하세요.
- 카테고리는 반드시 다음 중 선택: 기술역량, 문제해결, 협업경험
- 필수 기술에 대한 깊이 있는 질문
- STAR 기법으로 답변 가능한 경험 질문
- 기술 선택의 근거를 묻는 질문
```

**참고**: 각 직무마다 사용 가능한 카테고리가 다릅니다 (db/migration/V2, V7 참조).

**출력 형식** (JSON):
```json
{
  "inferredJobField": "IT",  // 사용자가 직무 미선택 시에만 사용
  "questions": [
    {
      "content": "Kotlin의 코루틴을 실무에서 어떻게 활용했나요?",
      "category": "기술역량",
      "difficulty": "MEDIUM",
      "reasoning": "필수 기술인 Kotlin의 핵심 기능 이해도를 확인하기 위한 질문"
    },
    {
      "content": "동시성 이슈를 경험하고 해결한 사례는?",
      "category": "문제해결",
      "difficulty": "HARD",
      "reasoning": "실무 문제 해결 능력 검증"
    },
    {
      "content": "Git 브랜치 전략은 무엇을 사용했나요?",
      "category": "협업경험",
      "difficulty": "EASY",
      "reasoning": "팀 협업 경험 확인"
    }
    // ... 7개 더 (총 10개)
  ]
}
```

---

### 1.4 생성된 질문 목록 표시

**UI 요구사항**:
- 공고 정보 카드:
  - 회사명, 포지션명
  - 원본 공고 링크 (새 탭에서 열기)
- 질문 카드 (10개):
  - 질문 내용
  - 카테고리 태그 (기술역량, 경험검증 등)
  - 난이도 배지 (EASY: 초록, MEDIUM: 노랑, HARD: 빨강)
  - "AI 생성 근거" 접기 가능 (details 태그)
  - "답변하기" 버튼 → 기존 답변 작성 플로우로 연결

---

### 1.5 답변 및 피드백 (기존 플로우 재활용)

**기능 명세**:
- 생성된 질문에 대한 답변은 기존 `InterviewAnswer` 플로우를 재사용합니다.
- 답변 작성 → AI 평가 → 피드백 표시
- 단, 질문 출처를 `GeneratedQuestion`으로 구분하여 리뷰 이력에 표시합니다.

**제약사항**:
- 질문 생성은 **1일 10회로 제한** (Rate Limiting)
- 동일 URL의 공고는 **7일간 캐싱** (중복 생성 방지)

---

## 🏗️ 도메인 모델

### JobPosting (채용 공고)
```kotlin
@Entity
@Table(name = "job_postings")
class JobPosting(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val userId: Long,                   // 공고 등록자
    val originalUrl: String,            // 원본 URL
    val companyName: String,            // 회사명
    val jobTitle: String,               // 포지션명
    val jobDescription: String,         // 공고 전문 (TEXT)
    var selectedJobField: JobField?,    // 사용자 선택 직무 (우선순위 높음)
    var inferredJobField: JobField?,    // AI 추론 직무 (사용자 미선택 시 사용)
    val requiredSkills: String?,        // 필수 기술 (JSON array)
    val preferredSkills: String?,       // 우대 기술 (JSON array)
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

### GeneratedQuestion (생성된 질문)
```kotlin
@Entity
@Table(name = "generated_questions")
class GeneratedQuestion(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val jobPostingId: Long,             // 소속 채용 공고
    val content: String,                // 질문 내용 (TEXT)
    val category: String,               // 기술역량, 경험검증 등
    val difficulty: String,             // EASY, MEDIUM, HARD
    val aiReasoning: String,            // AI 생성 근거 (TEXT)
    val orderIndex: Int,                // 순서 (1-10)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

---

## 🏗️ 기술 아키텍처

### 시스템 아키텍처
```
[사용자 브라우저]
    ↓ (HTTP)
[Spring Boot Application]
    ├─ JobPostingController (공고 등록/조회)
    └─ ...
    ↓
[Service Layer]
    ├─ JobPostingParserService (Jsoup + AI Fallback)
    ├─ QuestionGeneratorService (AI 질문 생성)
    └─ ...
    ↓
[Support Services]
    ├─ OpenAiClientImpl (AI API 호출)
    ├─ PromptBuilder (17개 직무 프롬프트)
    ├─ RateLimitService (IP/User ID 기반)
    └─ DuplicateRequestCache (SHA-256 해싱)
    ↓
[Database (PostgreSQL)]
    ├─ job_postings (채용 공고)
    └─ generated_questions (생성된 질문)
    ↓
[External API]
    └─ OpenAI gpt-4o-mini
```

---

## 💾 데이터베이스 스키마

### V9__create_job_postings_table.sql
```sql
CREATE TABLE job_postings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    original_url TEXT NOT NULL,
    company_name VARCHAR(200) NOT NULL,
    job_title VARCHAR(200) NOT NULL,
    job_description TEXT NOT NULL,
    selected_job_field VARCHAR(50),    -- 사용자 선택 직무 (우선)
    inferred_job_field VARCHAR(50),    -- AI 추론 직무 (대체)
    required_skills TEXT,
    preferred_skills TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE generated_questions (
    id BIGSERIAL PRIMARY KEY,
    job_posting_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(100) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    ai_reasoning TEXT NOT NULL,
    order_index INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (job_posting_id) REFERENCES job_postings(id) ON DELETE CASCADE
);

CREATE INDEX idx_job_postings_user ON job_postings(user_id);
CREATE INDEX idx_job_postings_active ON job_postings(is_active);
CREATE INDEX idx_generated_questions_posting ON generated_questions(job_posting_id);
```

---

## 🔌 API 명세

### POST /job-postings
**설명**: 채용 공고 URL 제출 (파싱 + 질문 생성)

**요청**:
```http
POST /job-postings
Content-Type: application/x-www-form-urlencoded

url=https://www.wanted.co.kr/wd/123456
selectedJobField=IT  (선택 사항)
```

**응답** (성공 시 redirect):
```http
HTTP/1.1 302 Found
Location: /job-postings/{id}/questions
```

---

### GET /job-postings/{id}/questions
**설명**: 생성된 질문 목록 조회

**응답** (HTML):
```html
<!-- 공고 정보 카드 + 질문 카드 10개 -->
```

---

## 🎨 UI/UX 설계

### 채용 공고 등록 화면
```
┌─────────────────────────────────────┐
│ 채용 공고 등록                      │
├─────────────────────────────────────┤
│ 채용 공고 URL:                      │
│ [____________________________]      │
│                                     │
│ 직무 선택 (선택 사항):              │
│ [▼ IT개발          ]                │
│ (미선택 시 AI가 자동 분석)          │
│                                     │
│ [질문 생성]                         │
└─────────────────────────────────────┘
```

### 생성된 질문 목록 화면
```
┌─────────────────────────────────────┐
│ 백엔드 개발자 - 카카오              │
│ 원본 공고: [링크 🔗]                │
├─────────────────────────────────────┤
│ [질문 1] 기술역량 🟡 MEDIUM         │
│ Kotlin의 코루틴을 실무에서 어떻게   │
│ 활용했나요?                         │
│ [AI 생성 근거 ▼]                    │
│ [답변하기]                          │
├─────────────────────────────────────┤
│ [질문 2] 문제해결 🔴 HARD           │
│ 동시성 이슈를 경험하고 해결한       │
│ 사례는?                             │
│ [답변하기]                          │
└─────────────────────────────────────┘
```

---

## 💰 비용 분석

### AI API 비용 추정 (OpenAI gpt-4o-mini)

**질문 생성 (1회)**:
- 입력: 공고 내용 (평균 2,000자) + 프롬프트 (500자) ≈ 1,250 토큰
- 출력: 10개 질문 + 근거 ≈ 1,500 토큰
- 비용: $0.00375 (입력) + $0.00225 (출력) = **$0.006/회**

**월간 비용** (100명 사용자, 각 10회):
- 100명 × 10회 × $0.006 = **$6/월**

**절감 전략**:
- 7일 캐싱: 동일 URL 재생성 방지 → 약 30% 절감
- Rate Limiting: 1일 10회 제한 → 남용 방지
- **예상 실제 비용**: **$4.2/월** (100명 기준)

---

## ⚠️ 리스크 및 대응 방안

### 1. 웹 스크래핑 실패
**리스크**: 채용 사이트 HTML 구조 변경 시 파싱 실패

**대응**:
- 주요 3개 사이트 (원티드, 사람인, 잡코리아) 전용 파서 구현
- Fallback: OpenAI API로 HTML → 구조화 데이터 변환
- 최종 Fallback: 사용자가 직접 복사/붙여넣기

---

### 2. AI 질문 품질 저하
**리스크**: 공고 내용이 불충분하거나 모호한 경우

**대응**:
- 최소 필수 정보 검증 (회사명, 포지션명, 직무 설명 200자 이상)
- AI 프롬프트에 "구체적이고 실전 중심 질문 생성" 명확히 지시
- 사용자 피드백: "질문 품질이 낮음" 신고 기능 (향후)

---

### 3. Rate Limiting 우회
**리스크**: 사용자가 여러 계정 생성하여 제한 우회

**대응**:
- IP 기반 + 사용자 ID 기반 이중 제한
- 비정상 패턴 감지 (1시간 내 5회 이상 계정 생성 시 차단)
- 향후: Captcha 추가

---

## 📅 구현 일정 (상세 계획)

### Phase 6A: 채용 공고 파싱 (Week 1 - 5-7일)

**목표**: 데이터 기반 구축 및 웹 스크래핑/AI fallback 파싱 구현

#### 1. JobPosting 엔티티
- **파일**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/domain/JobPosting.kt`
- **필드**: id, userId, originalUrl, companyName, jobTitle, jobDescription, selectedJobField, inferredJobField, requiredSkills, preferredSkills, isActive, createdAt
- **메서드**: effectiveJobField (프로퍼티), deactivate(), updateInferredJobField()
- **패턴**: Regular class (not data class), custom equals/hashCode

#### 2. GeneratedQuestion 엔티티
- **파일**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/domain/GeneratedQuestion.kt`
- **필드**: id, jobPostingId, content, category, difficulty, aiReasoning, orderIndex, createdAt
- **제약**: orderIndex 1-10 (CHECK), difficulty EASY/MEDIUM/HARD (CHECK)

#### 3. V9 마이그레이션
- **파일**: `src/main/resources/db/migration/V9__create_job_postings_table.sql`
- **내용**: CREATE TABLE job_postings, generated_questions + indexes + FK constraints

#### 4. Repositories
- **JobPostingRepository**: findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(), findFirstByOriginalUrlAndCreatedAtAfterOrderByCreatedAtDesc()
- **GeneratedQuestionRepository**: findByJobPostingIdOrderByOrderIndexAsc(), countByJobPostingId()

#### 5. JobPostingParserService
- **전략**: Jsoup (원티드/사람인/잡코리아) → AI Fallback → Manual Input
- **메서드**: parseFromUrl(), parseWanted(), parseSaramin(), parseJobKorea(), parseWithAi()
- **의존성**: Jsoup 1.17.2 추가 (build.gradle.kts)

#### 6. DTOs & 예외
- **DTOs**: ParsedJobPosting, CreateJobPostingRequest, JobPostingViewModel
- **예외**: JobPostingException (sealed), JobPostingParseException, JobPostingNotFoundException

**완료 기준**:
- [ ] JobPosting 엔티티 생성 (모든 필드 + equals/hashCode)
- [ ] GeneratedQuestion 엔티티 생성
- [ ] V9 마이그레이션 실행 성공
- [ ] Repositories (custom queries)
- [ ] JobPostingParserService (Jsoup + AI fallback)
- [ ] DTOs 검증
- [ ] 예외 처리 추가
- [ ] 단위 테스트: 파싱 성공/실패 시나리오

---

### Phase 6B: AI 질문 생성 (Week 2 - 6-8일)

**목표**: OpenAI API로 공고 기반 맞춤형 질문 10개 생성

#### 1. QuestionGeneratorService
- **파일**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/service/QuestionGeneratorService.kt`
- **핵심 메서드**: generateQuestions(jobPosting): List<GeneratedQuestion>
- **흐름**: PromptBuilder → AiClient → QuestionResponseParser → 엔티티 생성
- **기능**: parseSkills(), generateFallbackQuestions(), Micrometer 메트릭 (counter, timer)

#### 2. PromptBuilder 확장
- **파일**: 기존 `src/main/kotlin/.../service/ai/PromptBuilder.kt` 업데이트
- **새 메서드**:
  - buildQuestionGenerationSystemPrompt(jobField, companyName, jobTitle): String
  - buildQuestionGenerationUserPrompt(jobDescription, requiredSkills, preferredSkills): String
  - getCategoriesForJobField(jobField): List<String> - 17개 직무 카테고리 맵핑
- **출력**: JSON 형식 (10개 질문, 난이도 3-4-3 분포)

#### 3. QuestionResponseParser
- **파일**: `src/main/kotlin/.../service/ai/QuestionResponseParser.kt`
- **메서드**: parseQuestionResponse(rawResponse): ParsedQuestions
- **검증**: 질문 개수, difficulty 유효성, content/category 비어있지 않음, 난이도 분포 (로그)
- **DTOs**: ParsedQuestions, GeneratedQuestionDto

#### 4. Rate Limiting & 캐싱
- **RateLimitService**: checkQuestionGenerationLimit(userId) 추가 - 10회/24시간
- **JobPostingCache**: findCachedByUrl(url) - 7일 캐싱

**완료 기준**:
- [ ] QuestionGeneratorService (AI 통합)
- [ ] PromptBuilder 확장 (17개 직무 프롬프트)
- [ ] QuestionResponseParser (JSON 검증)
- [ ] Rate Limiting (10회/일)
- [ ] 7일 캐싱 (URL 기반)
- [ ] Micrometer 메트릭 (counter, timer)
- [ ] 단위 테스트: 생성, 파싱, 검증
- [ ] 통합 테스트: End-to-end 질문 생성

---

### Phase 6C: UI 및 통합 (Week 3 - 6-7일)

**목표**: 사용자 인터페이스 구축 및 기존 답변 플로우 연결

#### 1. JobPostingController
- **파일**: `src/main/kotlin/.../controller/JobPostingController.kt`
- **엔드포인트**:
  - GET /job-postings/create - 공고 등록 폼
  - POST /job-postings - 공고 등록 + 질문 생성 + redirect
  - GET /job-postings/{id}/questions - 질문 목록 조회
  - GET /job-postings - 내 공고 목록
- **보안**: @AuthenticationPrincipal, User ownership 검증

#### 2. JobPostingService (Orchestration)
- **파일**: `src/main/kotlin/.../service/JobPostingService.kt`
- **메서드**:
  - createJobPosting(userId, url, selectedJobField): JobPosting
  - getJobPostingWithQuestions(jobPostingId, userId): JobPostingViewModel
  - findByUserId(userId): List<JobPostingViewModel>
- **흐름**: Rate limit → 7일 캐시 체크 → 파싱 → 엔티티 저장 → 질문 생성

#### 3. UI 템플릿
- **create.html**: URL 입력, 직무 선택 드롭다운, 제한 사항 배너, Tailwind CSS + Dark mode
- **questions.html**: 공고 정보 카드, 질문 카드 10개 (번호, 난이도 배지, 카테고리, AI 근거, 답변하기 버튼)
- **list.html** (optional): 내 공고 목록
- **패턴**: fragments/layout.html 재사용

#### 4. 기존 플로우 연결
- **InterviewAnswer**: generatedQuestionId: Long? 필드 추가
- **V10 마이그레이션**: ALTER TABLE interview_answers ADD COLUMN generated_question_id
- **GeneratedQuestionController**: GET /generated-questions/{id}/answer (답변 작성 폼)

**완료 기준**:
- [ ] JobPostingController (CRUD)
- [ ] JobPostingService (orchestration)
- [ ] UI 템플릿: create.html, questions.html
- [ ] Thymeleaf + Tailwind CSS + Dark mode
- [ ] 기존 답변 플로우 연결 (generatedQuestionId FK)
- [ ] 보안: 소유권 검증
- [ ] Responsive mobile 디자인
- [ ] 에러 핸들링 (flash messages)

---

### Phase 6D: 테스트 및 최적화 (Week 4 - 7-8일)

**목표**: 종합 테스트, 성능 튜닝, 문서화

#### 1. 통합 테스트
- **파일**: `src/test/kotlin/.../Phase6IntegrationTest.kt`
- **시나리오**:
  - 공고 등록 + 질문 10개 생성 + redirect 검증
  - 7일 캐시 히트 (동일 URL 재등록)
  - Rate limit 검증 (10회 초과 시 에러)
  - Wanted URL 파싱 성공
  - 알 수 없는 사이트 → AI fallback
  - 난이도 분포 검증 (3-4-3)
  - 생성된 질문 답변 작성 (generatedQuestionId 연결)

#### 2. Unit 테스트
- **RateLimitServiceTest**: checkQuestionGenerationLimit() 10회 성공, 11번째 실패
- **JobPostingCacheTest**: 3일 전 캐시 히트, 8일 전 캐시 미스
- **JobPostingParserServiceTest**: Wanted URL 파싱, Invalid URL → null

#### 3. 성능 최적화
- **목표**: 질문 생성 < 10초, 캐시 히트율 > 30%, Rate limit 정확도 100%
- **최적화**: 비동기 질문 생성 (@Async), DB 인덱스 검증, Batch saving

#### 4. 문서화
- **PHASE6_COMPLETION_REPORT.md**: Overview, Features, Architecture, API endpoints, DB schema, Test coverage (90%+), Limitations, Future improvements
- **README.md**: Phase 6 섹션 추가
- **CHANGELOG.md**: [0.6.0] 버전 추가

**완료 기준**:
- [ ] 90%+ 테스트 커버리지 (Phase 6 코드)
- [ ] 통합 테스트: 전체 플로우 (등록 → 파싱 → 생성 → 답변)
- [ ] Rate Limiting 검증 (10회 제한)
- [ ] 캐싱 검증 (7일 만료)
- [ ] 성능: <10초 질문 생성
- [ ] 문서화: README, CHANGELOG, PHASE6_COMPLETION_REPORT.md
- [ ] API 레퍼런스 업데이트

---

**총 소요 기간**: 약 4주 (24-30일)

---

## 📊 성공 지표 (KPI)

### Phase 6: 질문 생성
- [ ] 파싱 성공률: **> 80%** (원티드/사람인/잡코리아)
- [ ] AI 질문 생성 성공률: **> 95%**
- [ ] 질문 품질 만족도: **평균 4.0/5점** (사용자 설문)
- [ ] 월 질문 생성 횟수: **500회 이상** (100명 × 5회)

---

## 🚀 다음 단계 (Phase 7)

Phase 6 완료 후:
- **Phase 7: 실시간 AI 채팅 면접** (별도 PRD)
  - 채용 공고 기반 또는 직무 기반 모의 면접
  - SSE 실시간 통신
  - 꼬리 질문 생성
  - 종합 평가 제공

---

**작성자**: Claude Code
**승인일**: 2026-04-27
**핵심 기능**: 채용 공고 URL 기반 AI 맞춤형 질문 생성
