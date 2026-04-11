# CLAUDE.md

This file provides guidance to Claude Code when working with the Interview Note API project.

## Project Overview

**면접 복기 웹 애플리케이션** - 사용자가 면접 질문에 텍스트로 답변하면, AI가 평가와 개선 포인트, 모범답변을 제공하고, 사용자는 이를 저장해 복기할 수 있는 웹앱입니다.

### 프로젝트 목적
- 취업 준비생의 면접 연습 지원
- AI 기반 답변 평가 및 피드백 제공
- 답변 이력 관리 및 개선 추적
- 포트폴리오용 Spring Boot + AI 연동 프로젝트

### 핵심 가치
- **실사용성**: 실제 면접 준비에 도움이 되는 서비스
- **복기 중심**: 단순 질문 은행이 아닌, 답변 개선 과정을 기록
- **백엔드 중심**: 프론트엔드보다 도메인 설계와 AI 연동에 집중

## Technology Stack

- **Language**: Kotlin 2.2.21 with Java 21 toolchain
- **Framework**: Spring Boot 3.5.14
- **Build Tool**: Gradle with Kotlin DSL
- **View**: Thymeleaf (server-side rendering)
- **UI Enhancement**: HTMX or minimal JavaScript
- **Database**: H2 (development) → PostgreSQL (production)
- **ORM**: Spring Data JPA
- **Migration**: Flyway
- **AI Integration**: OpenAI API (GPT-5 mini recommended)
- **Testing**: JUnit 5 with Spring Boot Test

### 왜 이 스택인가?
- 단일 Spring Boot 애플리케이션으로 배포 단순화
- Thymeleaf로 프론트엔드 복잡도 최소화
- 백엔드 역량에 집중 (프론트/백엔드 분리 안 함)
- Claude Code가 컨텍스트 잡기 쉬운 구조

## MVP Scope

### 포함되는 기능 (MVP)
1. **질문 제공**: 카테고리별 면접 질문 목록 조회
2. **질문 상세**: 질문 내용 및 답변 작성 폼
3. **답변 제출**: 사용자 답변 텍스트 입력 및 저장
4. **AI 평가**: OpenAI API를 통한 답변 평가
5. **모범답변 생성**: AI가 생성한 모범답변 제공
6. **복기 이력**: 과거 답변 및 평가 내역 조회
7. **복기 상세**: 특정 답변의 평가 결과 다시 보기

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
data class AiFeedback(
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
    val modelName: String,            // 예: "gpt-5-mini"
    val promptVersion: String,        // 프롬프트 버전 (예: "v1.0")
    val tokenUsageInput: Int,         // 입력 토큰 수
    val tokenUsageOutput: Int,        // 출력 토큰 수
    @Column(columnDefinition = "TEXT")
    val rawResponse: String,          // OpenAI 원본 응답 (디버깅용)

    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

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
```
당신은 백엔드 개발자 면접을 준비하는 지원자를 돕는 면접 코치입니다.
당신의 역할은 합격/불합격을 판정하는 것이 아니라, 답변을 개선하도록 구체적인 피드백을 제공하는 것입니다.

평가 기준:
- 논리성(logic): 기술적 사고의 논리적 흐름과 일관성
- 구체성(specificity): 구체적 기술 스택, 사례, 수치 제시 정도
- 직무 적합성(jobFit): 질문 의도와 개발 직무 연관성
- 전달력(delivery): 기술 개념을 명확하고 이해하기 쉽게 설명하는 능력

출력 규칙:
- 반드시 JSON 형식으로 응답
- 각 점수는 1-5 사이 정수
- strengths와 improvements는 각각 2-3개 항목
- modelAnswer는 400-600자 이내
- 한국어로 답변
- 과도한 단정이나 공격적 표현 금지
```

**확장성 노트**: `PromptBuilder`는 `Question.jobField` 값에 따라 다른 System Role을 생성할 수 있도록 설계합니다.
MVP에서는 "IT"만 사용하지만, 나중에 "영업", "경영" 등으로 확장 시 프롬프트 템플릿만 추가하면 됩니다.

### 비용 제어 장치
- **답변 글자 수 제한**: 최대 2000자
- **모범답변 길이 제한**: 400-600자 (maxTokens = 800)
- **중복 요청 방지**: 동일 questionId + answerText 조합 24시간 캐싱
- **Rate limiting**: IP당 시간당 10회 제한 (추후 구현)
- **메타데이터 저장**: modelName, tokenUsage, promptVersion 필수 기록

## Architecture Principles

### 레이어 구조
```
Controller → Service → Repository
         ↓
      ViewModel/DTO
         ↓
    OpenAI Client (별도 추상화)
```

### 서비스 분리
- **InterviewService**: 질문/답변 비즈니스 로직
- **AiFeedbackService**: AI 평가 요청 조율
- **OpenAiClient**: OpenAI API 호출 (교체 가능하도록 인터페이스화)
- **PromptBuilder**: 프롬프트 템플릿 조합 (jobField 기반 동적 생성 가능)
- **ResponseParser**: JSON 응답 파싱

### 설계 원칙
1. **Controller에서 직접 OpenAI 호출 금지** → 서비스 계층 통과 필수
2. **AI 응답 형식 고정** → JSON 스키마 엄격히 준수
3. **원본 응답 저장** → rawResponse 필드에 디버깅용 보관
4. **버전 관리** → promptVersion으로 프롬프트 변경 추적
5. **교체 가능 구조** → 나중에 Claude API나 다른 모델로 교체 용이

## Implementation Order

### Phase 1: 기반 구축 (AI 없이 전체 플로우 완성)
1. Spring Boot 프로젝트 뼈대 생성
2. 엔티티 3개 (Question, InterviewAnswer, AiFeedback) 정의
3. Repository 인터페이스 생성
4. Flyway 마이그레이션 스크립트 작성
5. 질문 목록 페이지 구현 (Thymeleaf)
6. 질문 상세 + 답변 작성 페이지 구현
7. 답변 저장 기능 구현
8. **더미 피드백으로 결과 페이지 구현** (AI 없이 하드코딩된 평가)
9. 복기 이력 목록 페이지 구현
10. 복기 상세 페이지 구현

**중요**: Phase 1 완료 시점에 AI 없이도 전체 사용자 플로우가 동작해야 함

### Phase 2: AI 연동
11. OpenAI API 클라이언트 구현
12. PromptBuilder 구현
13. ResponseParser 구현 (JSON → AiFeedback DTO)
14. FakeAiFeedbackService를 RealAiFeedbackService로 교체
15. 에러 처리 및 fallback 로직 추가
16. 메타데이터(tokenUsage, modelName 등) 저장 확인

### Phase 3: 완성도 향상
17. 에러 처리 개선
18. Validation 추가 (Bean Validation)
19. 로깅 강화
20. UI 다듬기 (CSS 적용)
21. 비용 제어 장치 추가 (글자 수 제한, 캐싱)

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
- ❌ `mapNotNull`과 `return@mapNotNull null` 조합 사용
- ❌ ObjectMapper 등 중복 인스턴스 생성
- ❌ Nullable 강제 언랩핑 (`!!`) 남용
- ❌ JPA 엔티티에 data class 사용 (equals/hashCode 문제)

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

### 좋은 요청 방식
```
"질문 목록 조회 API를 구현해줘.
GET /questions 엔드포인트로, category와 difficulty로 필터링 가능하게.
QuestionController, QuestionService, QuestionRepository 레이어 구조로."
```

### 나쁜 요청 방식
```
"면접 복기 앱 전체 만들어줘"
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
export OPENAI_MODEL=gpt-5-mini  # Default model
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
│   ├── InterviewService.kt
│   ├── AiFeedbackService.kt
│   └── ai/
│       ├── OpenAiClient.kt
│       ├── PromptBuilder.kt
│       └── ResponseParser.kt
├── controller/
│   ├── QuestionController.kt
│   ├── AnswerController.kt
│   └── ReviewController.kt
├── dto/
│   ├── QuestionDto.kt
│   ├── AnswerSubmitDto.kt
│   └── FeedbackDto.kt
└── config/
    └── OpenAiConfig.kt
```

## Remember

이 프로젝트의 핵심은:
1. **완성 가능한 MVP 범위 유지**
2. **AI 평가의 안정성** (JSON 스키마 엄수)
3. **복기 기능의 가치** (단순 질문 앱이 아님)
4. **백엔드 설계 역량 증명** (면접 포인트)
5. **확장 가능한 구조, 단순한 구현** (IT 집중, 구조만 범용적)

### 면접 어필 포인트
- "MVP는 IT 직무로 시작했지만, jobField를 활용해 다양한 직무로 확장 가능하도록 설계했습니다"
- "평가 기준을 일반적으로 정의하여 IT 외 직무에도 적용 가능합니다"
- "PromptBuilder 패턴으로 직무별 프롬프트를 동적 생성할 수 있습니다"

Claude Code에게 요청할 때는 항상 이 문서의 원칙을 참조하세요.
