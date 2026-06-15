# 면접 리뷰 웹 애플리케이션 (Interview Review API)

취업 준비생을 위한 AI 기반 면접 답변 평가 및 리뷰 서비스

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-blue.svg)](https://kotlinlang.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.14-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![OpenAI](https://img.shields.io/badge/OpenAI-gpt--4o--mini-orange.svg)](https://openai.com)
[![Job Fields](https://img.shields.io/badge/Job%20Fields-17-purple.svg)](#주요-기능)
[![Tests](https://img.shields.io/badge/Tests-43%20Files-success.svg)](#테스트)
[![Live Demo](https://img.shields.io/badge/Live-interviewmock.xyz-blue.svg)](https://interviewmock.xyz)

## 📋 목차

- [라이브 데모](#라이브-데모)
- [프로젝트 소개](#프로젝트-소개)
- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [시작하기](#시작하기)
- [OpenAI API 설정](#openai-api-설정)
- [실행 방법](#실행-방법)
- [비용 정보](#비용-정보)
- [프로젝트 구조](#프로젝트-구조)
- [개발 문서](#개발-문서)

---

## 라이브 데모

**프로덕션 URL**: https://interviewmock.xyz

- **호스팅**: AWS EC2
- **도메인**: interviewmock.xyz
- **HTTPS**: SSL 인증서 적용 완료

> 실제 서비스를 체험해보세요!

---

## 프로젝트 소개

면접 질문에 텍스트로 답변하면, **AI가 평가와 개선 포인트, 모범답변을 제공**하는 웹 애플리케이션입니다.

**✨ 17개 직무 분야 지원** - IT개발, 기획·전략, 마케팅, 회계, 인사, 영업, 디자인, 금융 등 다양한 직무별 맞춤 질문 및 AI 평가를 제공합니다.

### 핵심 가치

- **실사용성**: 실제 면접 준비에 도움이 되는 서비스
- **리뷰 중심**: 단순 질문 은행이 아닌, 답변 개선 과정을 기록
- **AI 평가**: OpenAI GPT-4o-mini를 활용한 직무별 맞춤형 피드백
- **비용 최적화**: 중복 방지 및 Rate Limiting으로 API 비용 절감
- **개인화**: 사용자 직무/경력에 따른 맞춤형 질문 추천

---

## 주요 기능

### ✅ Phase 1 (완료)
- 📝 면접 질문 조회 (카테고리별, 난이도별)
- ✍️ 답변 작성 및 저장
- 📊 리뷰 이력 조회
- 🎯 더미 AI 피드백 (초기 개발용)

### ✅ Phase 2 (완료)
- 🤖 **실제 OpenAI API 연동** (gpt-4o-mini)
- 📈 **4가지 평가 기준**: 논리성, 구체성, 직무적합성, 전달력
- 💡 **AI 생성 모범답변** (400-600자)
- ⚡ **중복 요청 방지** (24시간 캐싱, 1,700배+ 속도 향상)
- 🛡️ **Rate Limiting** (IP당 33회/시간)
- 🔄 **Fallback 메커니즘** (API 오류 시 자동 대체)

### ✅ Phase 3 (완료)
- 🎨 **UI/UX 개선**: Tailwind CSS, HTMX, 에러 페이지
- 📊 **로깅 & 모니터링**: JSON 로그, Prometheus 메트릭, Health Check
- 🐳 **Docker 컨테이너화**: Multi-stage 빌드, Docker Compose, 환경별 설정

### ✅ Phase 4 (완료)
- 🔐 **회원가입/로그인** (Phase 4A)
  - Spring Security 기반 이메일 인증
  - BCrypt 비밀번호 암호화
  - 세션 기반 인증 (remember-me 지원)
- 👤 **사용자별 데이터 분리** (Phase 4B)
  - 답변-사용자 연결 (userId 외래키)
  - 권한 기반 접근 제어 (타 사용자 답변 차단)
  - 개인화된 홈페이지 및 리뷰 이력

### ✅ Phase 5 (완료)
- 🎯 **17개 직무 분야 지원**
  - IT개발, 기획·전략, 마케팅·홍보, 회계·세무·재무
  - 인사·노무, 총무·법무, 디자인, 영업·판매·무역
  - 상품기획·MD, 서비스, 생산, 건설·건축
  - 의료, 교육, 미디어·문화·스포츠, 금융·보험, 공공·복지
- 📝 **340개 면접 질문** (각 직무별 20개)
- 🤖 **직무별 맞춤 AI 평가** (17개 직무 프롬프트)
- 🎨 **사용자 프로필 관리**
  - 직무 분야 선택 (JobField)
  - 경력 수준 선택 (신입/주니어/시니어/시니어+)
  - 프로필 기반 질문 추천
- 🏠 **개인화된 홈페이지**
  - 사용자 직무 기반 추천 질문
  - 직무 미설정 시 안내 배너

### ✅ Phase 6 (완료)
- 🎯 **채용 공고 기반 맞춤형 질문 생성**
  - 원티드, 사람인, 잡코리아 URL 입력
  - AI가 공고 분석하여 **10개 맞춤 질문** 자동 생성
  - 난이도 분포: EASY 3개, MEDIUM 4개, HARD 3개
- 📋 **AI 질문 생성 시스템**
  - 공고 파싱: 회사명, 포지션, 직무 설명, 기술 스택
  - OpenAI 기반 질문 생성 (실무 중심, STAR 기법 유도)
  - 생성 근거(reasoning) 함께 저장
- 🔧 **HTML 파싱 최적화**
  - Jsoup 기반 순수 텍스트 추출
  - 크기 97.8% 감소 (143KB → 3KB)
  - 8000자 제한 충족, 토큰 효율 60% 향상
- ✍️ **생성된 질문 답변 연동**
  - 기존 답변 작성 플로우 재사용
  - GeneratedQuestion ↔ InterviewAnswer 연결
  - 올바른 질문 매칭 (질문 ID 분리)

### ✅ Phase 7 (완료)
- 💬 **실시간 AI 채팅 면접 시스템**
  - AI 면접관과 실시간 대화 형태의 모의 면접
  - **직무 기반 면접**: 17개 직무 중 선택하여 일반 면접 연습
  - **공고 기반 면접**: 특정 채용 공고 맞춤형 면접 연습
- 📡 **SSE 실시간 통신** (Server-Sent Events)
  - WebSocket 대신 SSE 사용 (구현 단순, HTTP 기반)
  - 30분 타임아웃, 자동 재연결 지원
  - 브라우저 EventSource API 활용
- ⚡ **비동기 AI 응답 생성**
  - @Async 비동기 처리 (사용자 대기 시간 제거)
  - ThreadPoolTaskExecutor (core: 10, max: 50)
  - AI 응답 3-5초 → 즉시 응답 (<100ms)
- 🔗 **꼬리 질문 자동 생성**
  - 답변 평가 기반 다음 질문 결정
  - 답변 부족 시 꼬리 질문 (`isFollowUp: true`)
  - 답변 충분 시 새 질문 (`isFollowUp: false`)
- 📊 **종합 평가 시스템**
  - 평균 점수 (1-5점, 소수점 1자리)
  - 종합 피드백 (400-600자)
  - 주요 강점 3개, 개선점 3개
  - 채용 추천도 (추천/보류/비추천 + 근거)
- 🛡️ **보안 및 제한**
  - 30턴 대화 제한 (AI 비용 제어)
  - Rate Limiting: 5회/일 (모의 면접)
  - 소유권 검증 (타 사용자 접근 차단)
- ✅ **30개 테스트 모두 통과**
  - 통합 테스트 10개 (전체 플로우)
  - 단위 테스트 20개 (Parser, Service, Controller)

### ⏳ Phase 8 (완료)
- ✅ **8C: 리뷰 통합 및 재개 기능** (완료)
  - 2개 탭 리뷰 시스템 (질문 연습 / AI 면접)
  - 면접 재개 기능 ("이어서 연습하기" vs "새로 연습하기")
  - MockInterviewReviewDto (채용 공고, 경력 수준 정보 포함)
  - 8개 통합 테스트 추가
- ✅ **8A: 점수 계산 개선** (완료)
  - 가중 평균 점수 (첫 답변 제외, 저품질 패널티)
  - 종합 피드백 길이 증가 (400-600자 → 800-1200자)
  - 강점/개선점 유연화 (0-5개)
- ✅ **8B: 경력 수준 및 UI 개선** (완료)
  - 경력 수준 선택 UI (신입/주니어/시니어/시니어+)
  - 사용 방법 안내 모달
  - 채용 공고 기반 AI 면접 버튼

---

## 기술 스택

### Backend
- **Language**: Kotlin 2.2.21 (Java 21)
- **Framework**: Spring Boot 3.5.14
- **Security**: Spring Security (세션 기반 인증, BCrypt)
- **ORM**: Spring Data JPA + Hibernate
- **Database**: H2 (개발) / PostgreSQL 15 (프로덕션)
- **Migration**: Flyway (14개 마이그레이션: V1-V14)
- **Real-time**: SSE (Server-Sent Events)
- **Async**: @Async + ThreadPoolTaskExecutor
- **Build Tool**: Gradle (Kotlin DSL)

### AI Integration
- **AI Model**: OpenAI GPT-4o-mini
- **HTTP Client**: RestTemplate (직접 구현)
- **Response Format**: JSON Mode (구조화된 응답)
- **Cache**: SHA-256 해시 기반 중복 방지
- **Rate Limiting**: Caffeine Cache
- **HTML Parsing**: Jsoup 1.17.2 (채용 공고 파싱)

### Frontend
- **Template Engine**: Thymeleaf
- **UI Framework**: Tailwind CSS
- **UI Enhancement**: HTMX

### Monitoring & Logging
- **Logging**: Logback + Logstash Encoder (JSON)
- **Metrics**: Micrometer + Prometheus
- **Health Check**: Spring Boot Actuator

### DevOps
- **Container**: Docker (Multi-stage build)
- **Orchestration**: Docker Compose
- **Environments**: dev (H2) / prod (PostgreSQL)
- **Hosting**: AWS EC2
- **Domain**: interviewmock.xyz (HTTPS)

### Testing
- **Framework**: JUnit 5, Spring Boot Test
- **Mocking**: Mockito Kotlin

---

## 시작하기

### 사전 요구사항

- **Java 21** 이상
- **Gradle** (래퍼 포함)
- **OpenAI API 키** (무료 또는 유료 계정)

### 클론 및 빌드

```bash
# 저장소 클론
git clone <repository-url>
cd interview-note-api

# 빌드
./gradlew build

# 테스트
./gradlew test
```

---

## OpenAI API 설정

### 1. API 키 발급

1. [OpenAI Platform](https://platform.openai.com/signup) 가입
2. [API Keys](https://platform.openai.com/api-keys) 페이지에서 새 키 생성
3. 생성된 키를 안전하게 보관

### 2. 환경변수 설정

#### 방법 A: `.env` 파일 (권장)

프로젝트 루트에 `.env` 파일 생성:

```bash
# .env
OPENAI_API_KEY=sk-proj-your-api-key-here
```

**⚠️ 주의**: `.env` 파일은 `.gitignore`에 추가되어 있어 Git에 커밋되지 않습니다.

#### 방법 B: 시스템 환경변수

**macOS/Linux**:
```bash
export OPENAI_API_KEY=sk-proj-your-api-key-here
```

**Windows (PowerShell)**:
```powershell
$env:OPENAI_API_KEY="sk-proj-your-api-key-here"
```

### 3. Spring Boot 설정 확인

`src/main/resources/application.properties`에서 설정 확인:

```properties
# OpenAI Configuration
openai.api-key=${OPENAI_API_KEY}
openai.model=gpt-4o-mini
openai.prompt-version=v1.0
openai.max-tokens=800
openai.temperature=0.7
openai.timeout=30000
```

---

## 실행 방법

### 터미널에서 실행

```bash
# .env 파일 로드 후 실행
export $(cat .env | grep -v '^#' | xargs)
./gradlew bootRun
```

애플리케이션이 `http://localhost:8080`에서 실행됩니다.

### IntelliJ IDEA에서 실행

1. `InterviewNoteApiApplication.kt` 우클릭
2. `Modify Run Configuration...` 선택
3. **Environment variables** 섹션에 추가:
   ```
   OPENAI_API_KEY=sk-proj-your-api-key-here
   ```
4. `Run` 클릭

### H2 콘솔 접근

개발 중 데이터베이스 확인:

```
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:interviewdb
Username: sa
Password: (비어있음)
```

---

## Docker로 실행 (권장)

### 사전 요구사항

- Docker 20.10 이상
- Docker Compose v2.0 이상

### 빠른 시작

**1. 환경변수 설정**

```bash
# .env 파일 생성
cp .env.example .env

# .env 파일 편집
vim .env
```

`.env` 파일 내용:
```bash
OPENAI_API_KEY=sk-proj-your-actual-api-key-here
SPRING_PROFILES_ACTIVE=prod
```

**2. Docker Compose로 실행**

```bash
# 전체 스택 실행 (PostgreSQL + 애플리케이션)
docker-compose up -d

# 로그 확인
docker-compose logs -f app

# 상태 확인
docker-compose ps
```

**3. 애플리케이션 접속**

- **웹**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **Prometheus 메트릭**: http://localhost:8080/actuator/prometheus

**4. 중지**

```bash
# 중지
docker-compose down

# 중지 및 데이터 삭제
docker-compose down -v
```

### 개발 환경으로 실행

```bash
# .env 파일에서 프로파일 변경
SPRING_PROFILES_ACTIVE=dev

# 재시작
docker-compose restart app
```

### 이미지 크기

- **빌드 이미지**: ~180MB (Alpine Linux + JRE 21)
- **최적화**: Multi-stage 빌드, Gradle 캐싱

---

## 비용 정보

### OpenAI API 비용 (gpt-4o-mini 기준)

**요금** (2026년 1월 기준):
- 입력: $0.15 / 1M 토큰
- 출력: $0.60 / 1M 토큰

**1회 평가 비용**:
```
입력 토큰: ~300 (프롬프트 + 답변)
출력 토큰: ~200 (평가 JSON)

입력 비용: 300 × $0.15 / 1M = $0.000045
출력 비용: 200 × $0.60 / 1M = $0.00012
──────────────────────────────────────
총 비용: ~$0.0002 (약 0.2원)
```

### 월간 비용 추정

| 일일 평가 수 | 월간 평가 수 | 비용 (캐시 없음) | 비용 (캐시 50%) |
|------------|------------|----------------|----------------|
| 10회 | 300회 | $0.06 | $0.03 |
| 100회 | 3,000회 | $0.60 | $0.30 |
| 1,000회 | 30,000회 | $6.00 | $3.00 |

### Rate Limit 보호

**최대 비용 (단일 IP)**:
```
33회/시간 × 24시간 = 792회/일
792회/일 × 30일 = 23,760회/월
23,760회 × $0.0002 = $4.75/월
```

### 비용 절감 기능

1. **중복 요청 방지** (24시간 캐싱)
  - 동일 질문 + 답변 재평가 차단
  - API 호출 0회 (캐시 히트 시)
  - 속도: 1,700배+ 빠름

2. **Rate Limiting** (IP당 33회/시간)
  - 악의적 사용 방지
  - 예산 초과 방지

3. **Fallback 메커니즘**
  - API 오류 시 더미 피드백 제공
  - 사용자 경험 보장

---

## 프로젝트 구조

```
src/
├── main/
│   ├── kotlin/.../interviewnoteapi/
│   │   ├── config/
│   │   │   ├── AsyncConfig.kt             # Phase 7 - 비동기 설정
│   │   │   ├── ObjectMapperConfig.kt
│   │   │   ├── OpenAiConfig.kt
│   │   │   ├── RestTemplateConfig.kt
│   │   │   └── SecurityConfig.kt           # Phase 4A
│   │   ├── controller/
│   │   │   ├── AdminController.kt
│   │   │   ├── AnswerController.kt
│   │   │   ├── AuthController.kt          # Phase 4A - 회원가입/로그인
│   │   │   ├── GeneratedQuestionController.kt  # Phase 6
│   │   │   ├── HomeController.kt
│   │   │   ├── JobPostingController.kt    # Phase 6 - 채용 공고
│   │   │   ├── MockInterviewController.kt # Phase 7 - AI 채팅 면접
│   │   │   ├── ProfileController.kt       # Phase 5 - 프로필 설정
│   │   │   ├── QuestionController.kt
│   │   │   └── ReviewController.kt
│   │   ├── domain/
│   │   │   ├── AiFeedback.kt
│   │   │   ├── CareerLevel.kt            # Phase 5 - 경력 수준 Enum
│   │   │   ├── GeneratedQuestion.kt      # Phase 6
│   │   │   ├── InterviewAnswer.kt
│   │   │   ├── InterviewDraft.kt         # Phase 6/8
│   │   │   ├── InterviewMessage.kt       # Phase 7 - 채팅 메시지
│   │   │   ├── JobField.kt               # Phase 5 - 17개 직무 Enum
│   │   │   ├── JobPosting.kt             # Phase 6 - 채용 공고
│   │   │   ├── MockInterview.kt          # Phase 7 - 모의 면접 세션
│   │   │   ├── MockInterviewStatus.kt    # Phase 7
│   │   │   ├── Question.kt
│   │   │   ├── User.kt                   # Phase 4A
│   │   │   └── UserRole.kt               # Phase 4A
│   │   ├── dto/
│   │   │   ├── JobPostingDto.kt          # Phase 6
│   │   │   ├── MockInterviewDto.kt       # Phase 7
│   │   │   ├── MockInterviewReviewDto.kt # Phase 8C
│   │   │   ├── UpdateProfileRequest.kt   # Phase 5
│   │   │   └── UserProfileDto.kt         # Phase 5
│   │   ├── exception/
│   │   │   ├── AiExceptions.kt
│   │   │   ├── GlobalExceptionHandler.kt
│   │   │   └── RateLimitExceededException.kt
│   │   ├── filter/
│   │   │   └── RequestIdFilter.kt        # Phase 3B
│   │   ├── health/
│   │   │   └── OpenAiHealthIndicator.kt  # Phase 3B
│   │   ├── repository/
│   │   │   ├── AiFeedbackRepository.kt
│   │   │   ├── GeneratedQuestionRepository.kt  # Phase 6
│   │   │   ├── InterviewAnswerRepository.kt
│   │   │   ├── InterviewDraftRepository.kt     # Phase 6/8
│   │   │   ├── InterviewMessageRepository.kt   # Phase 7
│   │   │   ├── JobPostingRepository.kt         # Phase 6
│   │   │   ├── MockInterviewRepository.kt      # Phase 7
│   │   │   ├── QuestionRepository.kt
│   │   │   └── UserRepository.kt         # Phase 4A
│   │   ├── security/
│   │   │   └── CustomUserDetailsService.kt # Phase 4A
│   │   ├── service/
│   │   │   ├── ai/
│   │   │   │   ├── AiClient.kt
│   │   │   │   ├── OpenAiClientImpl.kt
│   │   │   │   ├── InterviewResponseParser.kt  # Phase 7
│   │   │   │   ├── QuestionResponseParser.kt   # Phase 6
│   │   │   │   ├── ResponseParser.kt
│   │   │   │   └── prompt/               # Phase 7 - 프롬프트 빌더
│   │   │   │       ├── EvaluationPromptBuilder.kt
│   │   │   │       ├── FeedbackPromptBuilder.kt
│   │   │   │       ├── InterviewPromptBuilder.kt
│   │   │   │       ├── JobFieldPromptConfig.kt
│   │   │   │       └── QuestionPromptBuilder.kt
│   │   │   ├── cache/
│   │   │   │   ├── DuplicateRequestCache.kt
│   │   │   │   ├── JobPostingCache.kt    # Phase 6
│   │   │   │   └── QuestionCache.kt
│   │   │   ├── ratelimit/
│   │   │   │   └── RateLimitService.kt
│   │   │   ├── validation/
│   │   │   │   └── AnswerValidator.kt    # Phase 2B
│   │   │   ├── AiFeedbackService.kt
│   │   │   ├── InterviewAiService.kt     # Phase 7 - AI 면접 서비스
│   │   │   ├── InterviewService.kt
│   │   │   ├── JobPostingParserService.kt # Phase 6
│   │   │   ├── JobPostingService.kt      # Phase 6
│   │   │   ├── MockInterviewService.kt   # Phase 7 - 모의 면접 관리
│   │   │   ├── QuestionGeneratorService.kt # Phase 6
│   │   │   ├── QuestionService.kt        # Phase 5: jobField 필터링
│   │   │   ├── ReviewService.kt
│   │   │   ├── SseEmitterService.kt      # Phase 7 - SSE 관리
│   │   │   └── UserService.kt            # Phase 4A, 5
│   │   └── InterviewNoteApiApplication.kt
│   └── resources/
│       ├── db/migration/
│       │   ├── V1__Create_tables.sql
│       │   ├── V2__Insert_initial_questions.sql
│       │   ├── V3__add_answer_text_hash.sql
│       │   ├── V4__Create_users_table.sql       # Phase 4A
│       │   ├── V5__add_user_id_to_interview_answers.sql # Phase 4B
│       │   ├── V6__add_user_job_preferences.sql # Phase 5
│       │   ├── V7__insert_multi_job_field_questions.sql # Phase 5 (340개 질문)
│       │   ├── V8__create_interview_drafts_table.sql    # Phase 6
│       │   ├── V9__create_job_postings_table.sql        # Phase 6
│       │   ├── V10__add_generated_question_id.sql       # Phase 6
│       │   ├── V11__make_question_id_nullable.sql       # Phase 6
│       │   ├── V12__create_mock_interview_tables.sql    # Phase 7
│       │   ├── V13__enhance_mock_interview_scoring.sql  # Phase 8
│       │   └── V14__add_answer_text_hash.sql            # Phase 8
│       ├── templates/
│       │   ├── answers/
│       │   ├── auth/                     # Phase 4A - 로그인/회원가입
│       │   ├── error/
│       │   ├── fragments/
│       │   ├── job-postings/             # Phase 6 - 채용 공고
│       │   ├── mock-interviews/          # Phase 7 - AI 채팅 면접
│       │   ├── profile/                  # Phase 5 - 프로필 설정
│       │   ├── questions/
│       │   ├── reviews/
│       │   └── home.html
│       ├── application.properties
│       ├── application-dev.properties
│       ├── application-prod.properties
│       └── logback-spring.xml
└── test/
    └── kotlin/.../interviewnoteapi/
        ├── bugfix/                       # 버그 수정 테스트
        ├── controller/                   # 7개 컨트롤러 테스트
        ├── integration/                  # 수동 통합 테스트
        ├── repository/                   # 3개 저장소 테스트
        ├── service/                      # 9개 서비스 테스트
        │   ├── ai/                       # AI 관련 테스트 (5개)
        │   ├── cache/
        │   ├── ratelimit/
        │   └── validation/
        ├── Phase1IntegrationTest.kt
        ├── Phase5IntegrationTest.kt
        ├── Phase6*IntegrationTest.kt    # Phase 6: 5개 통합 테스트
        ├── Phase7IntegrationTest.kt     # Phase 7: AI 채팅 면접
        ├── Phase8*IntegrationTest.kt    # Phase 8: 5개 통합 테스트
        └── Week1CriticalFixIntegrationTest.kt
```

---

## 개발 문서

프로젝트 세부 사항은 다음 문서를 참조하세요. 전체 문서 목록은 **[docs/README.md](./docs/README.md)**를 참조하세요.

### 핵심 가이드
- **[CLAUDE.md](./CLAUDE.md)** - 프로젝트 전체 가이드 (아키텍처, 도메인 모델, 코딩 규칙)
- **[CHANGELOG.md](./CHANGELOG.md)** - 버전별 변경 이력 (0.1.0 ~ 0.8.0)
- **[docs/guides/SETUP_GUIDE.md](./docs/guides/SETUP_GUIDE.md)** - 환경 설정 가이드

### Phase별 구현 계획 (docs/archive/)
- **[PHASE8_AI_CHAT_INTERVIEW_IMPROVEMENTS.md](./docs/archive/PHASE8_AI_CHAT_INTERVIEW_IMPROVEMENTS.md)** - Phase 8: AI 면접 개선 🔧
- **[PHASE7_AI_CHAT_INTERVIEW.md](./docs/archive/PHASE7_AI_CHAT_INTERVIEW.md)** - Phase 7: 실시간 AI 채팅 면접 PRD
- **[PHASE6_JOB_POSTING_QUESTIONS.md](./docs/archive/PHASE6_JOB_POSTING_QUESTIONS.md)** - Phase 6: 채용 공고 기반 질문 생성
- **[phase5_implementation_plan.md](./docs/archive/phase5_implementation_plan.md)** - Phase 5: 17개 직무 확장

### Phase 완료 보고서 (docs/archive/)
- **[PHASE8C_COMPLETION_REPORT.md](./docs/archive/PHASE8C_COMPLETION_REPORT.md)** - Phase 8C 완료 보고서 (리뷰 통합, 재개 기능) 🎉
- **[PHASE7_COMPLETION_REPORT.md](./docs/archive/PHASE7_COMPLETION_REPORT.md)** - Phase 7 완료 보고서 (SSE, 비동기 처리)
- **[PHASE6D_COMPLETION_REPORT.md](./docs/archive/PHASE6D_COMPLETION_REPORT.md)** - Phase 6D 완료 보고서 (HTML 파싱 최적화)

### 가이드 문서 (docs/guides/)
- **[NGINX_SSE_SETUP.md](./docs/guides/NGINX_SSE_SETUP.md)** - Nginx SSE 프록시 설정
- **[CODE_QUALITY_GUIDE.md](./docs/guides/CODE_QUALITY_GUIDE.md)** - 코드 품질 가이드
- **[PHASE5_MIGRATION_GUIDE.md](./docs/guides/PHASE5_MIGRATION_GUIDE.md)** - Phase 5 마이그레이션 가이드

---

## 테스트

### 단위 테스트

```bash
./gradlew test
```

### 통합 테스트 (실제 OpenAI API)

```bash
# Phase 2E 테스트 (수동)
export $(cat .env | grep -v '^#' | xargs)
./gradlew test --tests "*Phase2EManualTest"
```

**⚠️ 주의**: 실제 OpenAI API를 호출하므로 비용이 발생합니다.

---

## 라이선스

이 프로젝트는 개인 포트폴리오용으로 제작되었습니다.

---

## 문의

프로젝트 관련 문의사항이 있으시면 이슈를 등록해주세요.

---

**Built with ❤️ using Kotlin & Spring Boot**
