# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

## [0.7.0] - 2026-05-02

### Added - Phase 7 (Real-time AI Chat Interview)

#### 실시간 AI 채팅 면접 시스템
- 💬 **AI 면접관과 실시간 대화**
  - 직무 기반 일반 면접: 17개 직무 중 선택
  - 공고 기반 맞춤 면접: 특정 채용 공고 맞춤형
  - 자연스러운 대화 형태의 모의 면접 진행
- 📡 **SSE 실시간 통신** (Server-Sent Events)
  - WebSocket 대신 SSE 사용 (구현 단순화)
  - 30분 타임아웃, 자동 재연결 지원
  - HTTP 기반 (방화벽 이슈 없음)
- ⚡ **비동기 AI 응답 생성**
  - @Async 비동기 처리로 사용자 대기 시간 제거
  - ThreadPoolTaskExecutor (core: 10, max: 50, queue: 100)
  - AI 응답 시간: 평균 3-5초
  - 사용자 체감 응답 시간: <100ms

#### 꼬리 질문 및 평가
- 🔗 **AI 꼬리 질문 자동 생성**
  - 답변 평가 기반 다음 질문 결정
  - 답변 부족 시 꼬리 질문 (`isFollowUp: true`)
  - 답변 충분 시 새 질문 (`isFollowUp: false`)
  - 질문 길이 200자 제한 (초과 시 자동 절단)
- 📊 **실시간 답변 평가**
  - 논리성, 구체성, 전달력 각 0-5점 (첫 질문 시 0점 허용)
  - 평가 코멘트
  - 사용자 메시지에 평가 점수 저장
- 🎓 **종합 평가 생성**
  - 평균 점수 (1-5점, 소수점 1자리)
  - 종합 피드백 (400-600자)
  - 주요 강점 3개, 개선점 3개
  - 채용 추천도 (추천/보류/비추천 + 근거)

#### 도메인 모델
- **MockInterview 엔티티**
  - 면접 세션 관리 (IN_PROGRESS, COMPLETED, ABORTED)
  - jobPostingId (nullable - 직무 기반 면접 시 null)
  - selectedJobField (필수)
  - 종합 평가 필드 (overallFeedback, keyStrengths, keyImprovements, averageScore, recommendation)
- **InterviewMessage 엔티티**
  - 채팅 메시지 저장 (AI, USER)
  - messageIndex (순서 보장, 0부터 시작)
  - aiReasoning (AI 전용)
  - 평가 점수 필드 (USER 전용)

#### API 엔드포인트
- POST `/mock-interviews/start` - 면접 시작
- GET `/mock-interviews/{id}/chat` - 채팅 UI 페이지
- POST `/mock-interviews/{id}/messages` - 사용자 메시지 전송
- GET `/mock-interviews/{id}/stream` - SSE 스트림 (실시간 수신) ⭐
- POST `/mock-interviews/{id}/end` - 면접 종료 및 종합 평가
- GET `/mock-interviews/{id}/result` - 결과 페이지

#### UI/UX
- 🎨 **직무 선택 모달** (`fragments/job-field-modal.html`)
  - 홈페이지 "AI 면접 연습" 버튼
  - 17개 직무 드롭다운
  - CSRF 토큰 JavaScript 처리
- 💬 **실시간 채팅 화면** (`mock-interviews/chat.html`)
  - SSE EventSource 연결
  - 메시지 자동 스크롤
  - 200자 입력 제한
  - 로딩 인디케이터
- 📊 **종합 평가 페이지** (`mock-interviews/result.html`)
  - 점수 프로그레스 바
  - 강점/개선점 카드
  - 대화 내역 (접기 가능)

#### 성능 최적화
- 🚀 **DB 인덱스**
  - idx_mock_interviews_user_id (사용자별 면접 조회)
  - idx_mock_interviews_status (상태별 필터링)
  - idx_interview_messages_order (복합 인덱스: mock_interview_id, message_index)
- ⚙️ **AsyncConfig**
  - ThreadPoolTaskExecutor 설정
  - graceful shutdown 지원
  - 동시 면접 세션 처리 (최대 50개)

#### 보안 및 제한
- 🛡️ **30턴 대화 제한**
  - MaxTurnExceededException
  - AI 비용 제어
- 🚦 **Rate Limiting**
  - 모의 면접 5회/일 (사용자별)
  - checkMockInterviewLimit()
- 🔒 **소유권 검증**
  - MockInterviewAccessDeniedException
  - 타 사용자 접근 차단
  - 종료된 면접 메시지 전송 차단

#### 서비스 계층
- **MockInterviewService**
  - 세션 관리, SSE Emitter 관리
  - 비동기 AI 응답 생성 (@Async)
  - 메시지 브로드캐스트
- **InterviewAiService**
  - 첫 질문 생성 (동기)
  - 꼬리 질문 생성 (비동기)
  - 종합 평가 생성
- **InterviewResponseParser**
  - JSON 파싱 및 검증
  - 질문 길이 200자 제한
  - 점수 범위 0-5 검증

### Changed
- Database: V12 migration (mock_interviews, interview_messages 테이블)
- PromptBuilder: 면접용 메서드 5개 추가 (기존 17개 직무 프롬프트 재활용)
- RateLimitService: 모의 면접 Rate Limit 추가
- GlobalExceptionHandler: MockInterviewException 처리
- SecurityConfig: SSE 엔드포인트 설정
- InterviewResponseParser: MIN_SCORE = 0 (첫 질문 시 평가 없음 허용)

### Fixed
- Phase 7D: Thymeleaf fragment script 실행 문제 (home.html로 이동)
- Phase 7D: CSRF 토큰 JavaScript 처리 (meta tags 사용)
- Phase 7E: 점수 범위 검증 예외 타입 수정
- Phase 7E: 30턴 제한 테스트 안정화 (비동기 AI 응답 고려)

### Tests
- ✅ **30개 Phase 7 테스트 모두 통과**
  - Phase7IntegrationTest.kt: 10개 통합 테스트
  - InterviewResponseParserTest.kt: 14개 단위 테스트
  - MockInterviewServiceTest.kt: 15개 단위 테스트
  - MockInterviewControllerTest.kt: 8개 컨트롤러 테스트
- 📊 **테스트 커버리지: Phase 7 코드 85%+**

### Documentation
- 📝 PHASE7_COMPLETION_REPORT.md 작성 (완료 보고서)
- 📝 README.md Phase 7 섹션 추가
- 📝 CHANGELOG.md 0.7.0 업데이트

---

## [0.6.0] - 2026-04-30

### Added - Phase 6 (Job Posting Based Questions)

#### 채용 공고 기반 질문 생성
- 🎯 **AI 맞춤형 질문 10개 자동 생성**
  - 원티드, 사람인, 잡코리아 등 채용 공고 URL 입력
  - 공고 내용 파싱 (회사명, 포지션, 직무 설명, 기술 스택)
  - OpenAI gpt-4o-mini로 맞춤형 질문 생성
  - 난이도 분포: EASY 3개, MEDIUM 4개, HARD 3개
- 📋 **JobPosting 도메인**
  - 채용 공고 저장 (URL, 회사명, 포지션, 직무 설명)
  - selectedJobField (사용자 선택) / inferredJobField (AI 추론)
  - 필수/우대 기술 스택 JSON 저장
- 💬 **GeneratedQuestion 엔티티**
  - AI 생성 질문 10개 저장
  - orderIndex (1-10) 순서 보장
  - aiReasoning (생성 근거) 저장

#### HTML 파싱 및 최적화
- 🔧 **Jsoup 기반 파싱**
  - 원티드 URL → AI Fallback 전략
  - cleanHtml() Jsoup text() 방식 (HTML 태그 완전 제거)
  - 크기 감소: 143,933자 → 3,181자 (97.8% 감소)
- ⚡ **토큰 효율성**
  - 8000자 제한 충족 (60% 여유)
  - AI 입력 토큰 대폭 감소 (~2,000 → ~800)
  - 비용 절감 및 응답 속도 향상

#### UI/UX
- 📝 **공고 등록 화면** (`/job-postings/create`)
  - URL 입력, 직무 선택 (선택 사항)
  - 질문 생성하기 버튼
- 📋 **질문 목록 화면** (`/job-postings/{id}/questions`)
  - 공고 정보 카드 (회사명, 포지션, 원본 링크)
  - 질문 카드 10개 (번호, 카테고리, 난이도, AI 근거)
  - 답변하기 버튼 → 기존 답변 플로우 연결

### Changed
- Database: V9 migration (job_postings, generated_questions 테이블)
- Database: V10 migration (interview_answers.generated_question_id 추가)
- OpenAI maxTokens: 800 → 3000 (질문 생성용)
- PromptBuilder: 질문 생성 프롬프트 추가 (난이도 분포 필수화)
- InterviewAnswer: generatedQuestionId 필드 추가 (nullable)

### Fixed
- **JSON 잘림 문제** (Phase 6D)
  - maxTokens 설정 불일치 수정 (application.properties vs 코드)
  - 800 → 3000으로 일치시켜 질문 10개 안정적 생성
- **난이도 분포 문제** (Phase 6E)
  - EASY 난이도 질문 생성 안 되는 문제 수정
  - 프롬프트 "권장" → "필수 - 정확히 지켜야 함"으로 강화
  - Phase6CParsingTest에 엄격한 검증 추가 (3-4-3 분포)
- **GeneratedQuestion 답변 제출 버그** (Phase 6E - Critical)
  - ✅ **외래키 제약 조건 위반 수정**
    - 문제: `interview_answers.question_id` NOT NULL + FK 제약, GeneratedQuestion 답변 시 `questionId=0`으로 설정 → 500 Error
    - 해결: V11 migration으로 `question_id` nullable 변경, `questionId=null` 설정
    - InterviewAnswer 엔티티 수정: `questionId: Long?`
    - ReviewService 수정: GeneratedQuestion 지원 추가 (when 표현식)
    - Phase6EGeneratedQuestionAnswerBugTest 추가 (3개 테스트)
  - 질문 ID 매칭 오류 수정
  - Form action 동적 처리 (isGenerated 플래그 활용)
  - GeneratedQuestionController POST 엔드포인트 추가
  - InterviewService.submitAnswerForGeneratedQuestion() 추가
  - 피드백에 올바른 질문 내용 표시

### Technical Details
- **Domain 추가**: JobPosting, GeneratedQuestion
- **Repository 추가**: JobPostingRepository, GeneratedQuestionRepository
- **Service 추가**: JobPostingParserService, QuestionGeneratorService, JobPostingService
- **Controller 추가**: JobPostingController, GeneratedQuestionController
- **DTO 추가**: ParsedJobPosting, GeneratedQuestionDto, JobPostingViewModel
- **테스트**: Phase6AIntegrationTest, Phase6BIntegrationTest, Phase6CIntegrationTest, Phase6CParsingTest, Phase6DHtmlAnalysisTest
- **의존성**: Jsoup 1.17.2 추가

---

## [0.5.0] - 2026-04-23

### Added - Phase 5 (Multi-Job Field Support)

#### 직무 확장 (IT → 17개 직무)
- 🎯 **17개 직무 분야 지원**
  - IT개발, 기획·전략, 마케팅·홍보·조사, 회계·세무·재무
  - 인사·노무·HRD, 총무·법무·사무, 디자인, 영업·판매·무역
  - 상품기획·MD, 서비스, 생산, 건설·건축
  - 의료, 교육, 미디어·문화·스포츠, 금융·보험, 공공·복지
- 📝 **340개 면접 질문 데이터**
  - 각 직무별 20개 질문 (EASY 5개, MEDIUM 10개, HARD 5개)
  - 직무별 맞춤 카테고리 (IT: 기술역량/문제해결/협업, 영업: 고객관리/실적달성/협상 등)
- 🎨 **사용자 프로필 관리**
  - 직무 분야 선택 (JobField enum)
  - 경력 수준 선택 (신입, 주니어, 시니어, 시니어+)
  - `/profile` 페이지에서 설정 가능

#### AI 평가 개인화
- 🤖 **17개 직무별 프롬프트**
  - PromptBuilder에 각 직무별 평가 기준 구현
  - 직무 특성에 맞는 논리성/구체성 평가
  - buildBasePrompt로 중복 제거한 설계
- 🔍 **동적 카테고리 필터링**
  - 직무 선택 시 해당 직무의 카테고리만 표시
  - JavaScript 기반 동적 UI (HTMX 대신)
  - `getCategoriesByAllJobFields()` 메서드로 서버에서 전체 맵 전달

#### 사용자 경험 개선
- 🏠 **홈페이지 개인화**
  - 사용자 직무 기반 추천 질문 (5개 랜덤)
  - 직무 미설정 시 안내 배너 표시
  - IT 기본값 자동 적용 (직무 미설정 사용자)
- 📋 **질문 목록 필터링**
  - 직무 선택 드롭다운 추가
  - 로그인 사용자 기본 직무 자동 적용
  - 카테고리/난이도 조합 필터링

### Changed
- Database: V6 migration (job_field, career_level 컬럼 추가)
- Database: V7 migration (340개 질문 데이터 INSERT)
- QuestionService: jobField 파라미터 추가, IT 기본값 처리
- UserService: getProfile(), updateProfile() 메서드 추가
- QuestionController: jobField 필터링 및 동적 카테고리 지원
- HomeController: 개인화된 질문 추천 로직

### Technical Details
- **Domain 추가**: JobField enum (17개), CareerLevel enum (4개)
- **Controller 추가**: ProfileController (프로필 조회/수정)
- **DTO 추가**: UserProfileDto, UpdateProfileRequest
- **템플릿 추가**: profile/settings.html
- **테스트**: 245개 통과 (+23개 Phase 5 통합 테스트)
- **성능**: jobField 인덱스로 필터링 < 200ms

---

## [0.4.1] - 2026-04-20

### Added - Phase 4B (User-Specific Features)

#### 사용자별 데이터 분리
- 👤 **답변-사용자 연결**
  - InterviewAnswer에 userId 외래키 추가
  - 로그인한 사용자만 자신의 답변 조회/생성
  - V5 migration으로 스키마 업데이트
- 🔒 **권한 기반 접근 제어**
  - 타 사용자 답변 조회 차단 (403 Forbidden)
  - HomeController, ReviewController에 사용자 필터링 적용
  - AnswerController에 userId 자동 주입

#### UI/UX 개선
- 🏠 **개인화된 홈페이지**
  - "최근 리뷰 3개" 섹션 (사용자별)
  - 로그인/미로그인 상태별 다른 화면
  - Tailwind CSS 적용 완료
- 📊 **리뷰 이력 필터링**
  - 사용자별 리뷰만 표시
  - 빈 상태 안내 메시지

### Changed
- InterviewAnswer: userId 컬럼 추가 (nullable → not null)
- ReviewService: getUserReviews(userId) 메서드로 전환
- 기존 getReviewList() deprecated 처리

### Fixed
- 타 사용자 답변 조회 보안 이슈 해결
- 로그인 없이 답변 작성 방지

---

## [0.4.0] - 2026-04-18

### Added - Phase 4A (User Management)

#### 회원가입 및 인증
- 🔐 **Spring Security 통합**
  - 이메일 기반 회원가입/로그인
  - BCrypt 비밀번호 암호화
  - 세션 기반 인증 (remember-me 지원)
- 👤 **User 엔티티**
  - 이메일 (unique), 비밀번호 해시, 이름
  - 역할 기반 접근 제어 (USER, ADMIN)
  - 계정 활성화 상태 (isActive)
  - 마지막 로그인 일시 추적
- 🎨 **인증 UI**
  - 회원가입 페이지 (/auth/register)
  - 로그인 페이지 (/auth/login)
  - Tailwind CSS 스타일링

#### 보안 강화
- 🛡️ **비밀번호 검증**
  - 최소 8자, 최대 100자
  - Spring Validation 적용
- 🔒 **CSRF 보호**
  - Spring Security CSRF 토큰
  - 모든 POST 요청 보호
- 🚪 **접근 제어**
  - 인증 필요 경로: /questions, /answers, /reviews
  - 공개 경로: /, /auth/**, /h2-console (dev)

### Changed
- Database: V4 migration (users 테이블 생성)
- SecurityConfig: formLogin, logout, remember-me 설정
- 모든 Controller에 @AuthenticationPrincipal 적용
- 네비게이션 바에 로그인/로그아웃 버튼 추가

### Technical Details
- **비밀번호 암호화**: BCryptPasswordEncoder (strength 12)
- **세션 관리**: 서버 사이드 세션 (remember-me: 14일)
- **역할**: USER (기본), ADMIN (확장용)

---

## [0.3.0] - 2026-04-14

### Added - Phase 3 (Production Ready)

#### 3A: UI/UX 개선
- 🎨 **Tailwind CSS 도입**
  - 일관된 디자인 시스템
  - 반응형 레이아웃
  - 유틸리티 우선 스타일링
- ⚡ **HTMX 적용**
  - 페이지 새로고침 없는 인터랙션
  - 실시간 로딩 인디케이터
  - 부드러운 사용자 경험
- 🚨 **에러 페이지 개선**
  - 사용자 친화적 404 페이지
  - 사용자 친화적 500 페이지
  - 명확한 에러 메시지

#### 3B: 로깅 및 모니터링
- 📊 **구조화된 로깅**
  - JSON 형식 로그 (Logback + Logstash Encoder)
  - 환경별 로그 레벨 (dev: DEBUG, prod: INFO)
  - 요청 ID 추적 (MDC)
- 📈 **메트릭 수집** (Micrometer + Prometheus)
  - HTTP 요청 메트릭
  - AI API 호출 메트릭 (횟수, 지연시간, 토큰)
  - 캐시 히트율
  - Rate Limit 거부 횟수
- ❤️ **헬스 체크**
  - Spring Boot Actuator 활성화
  - Liveness/Readiness probe
  - OpenAI API 연결 상태 체크

#### 3C: Docker 컨테이너화
- 🐳 **Multi-stage Dockerfile**
  - Builder 단계: Gradle 빌드
  - Runtime 단계: Alpine Linux + JRE 21
  - 이미지 크기: ~180MB
  - 비루트 유저 실행 (보안)
  - Health check 내장
- 🐙 **Docker Compose**
  - PostgreSQL 15-alpine 컨테이너
  - Spring Boot 애플리케이션 컨테이너
  - 네트워크 격리 (interview-network)
  - 볼륨 영구화 (postgres_data)
  - Health check 기반 의존성 관리
- ⚙️ **환경별 설정 분리**
  - application.properties (공통)
  - application-dev.properties (H2)
  - application-prod.properties (PostgreSQL)
  - .env.example (템플릿)

### Changed
- Frontend: 기본 HTML/CSS → Tailwind CSS + HTMX
- Logging: 텍스트 로그 → JSON 구조화 로그
- Database: H2 (dev) / PostgreSQL (prod) 환경 분리

### Technical Details
- Docker 이미지: ~180MB (Multi-stage 빌드)
- PostgreSQL: 15-alpine
- Monitoring: Prometheus 메트릭 수집 가능
- Logging: JSON 형식 (ELK Stack 호환)

---

## [0.2.0] - 2026-04-13

### Added - Phase 2 (AI Integration)
- 🤖 **OpenAI API 연동** (gpt-4o-mini)
  - RestTemplate 기반 직접 HTTP 호출
  - JSON Mode 강제로 구조화된 응답 보장
- 📈 **4가지 평가 기준**
  - 논리성 (logic)
  - 구체성 (specificity)
  - 직무적합성 (jobFit)
  - 전달력 (delivery)
- 💡 **AI 생성 모범답변** (400-600자)
- ⚡ **중복 요청 방지**
  - SHA-256 해시 기반 24시간 캐싱
  - 속도: 1,700배+ 향상 (5초 → 3ms)
  - 비용: 캐시 히트 시 100% 절감
- 🛡️ **Rate Limiting**
  - IP당 33회/시간 제한
  - Caffeine Cache 기반 in-memory 저장
  - 최대 월 비용: $4.75 (단일 IP)
- 🔄 **Fallback 메커니즘**
  - API 오류 시 자동으로 더미 피드백 제공
  - 서비스 중단 없는 안정성 보장
- 📝 **메타데이터 저장**
  - 모델명, 프롬프트 버전, 토큰 사용량
  - 원본 응답 저장 (디버깅용)
  - answerTextHash (중복 방지용)

### Changed
- 용어 변경: "면접 복기" → "면접 리뷰"
- AiFeedbackService 리팩토링
  - generateDummyFeedback() 유지 (fallback용)
  - generateFeedback() 추가 (실제 AI 호출)

### Technical Details
- OpenAI 모델: gpt-4o-mini
- 평균 응답 시간: 5-6초
- 1회 평가 비용: ~$0.0002 (약 0.2원)
- 평가 품질: 평균 4.0-4.5/5.0

## [0.1.0] - 2026-04-11

### Added - Phase 1 (MVP Foundation)
- 📝 면접 질문 조회
  - 카테고리별 필터링
  - 난이도별 필터링
- ✍️ 답변 작성 및 저장
  - Validation (50-2000자)
- 📊 리뷰 이력 조회
- 🎯 더미 AI 피드백
  - 답변 길이 기반 점수
  - 고정 템플릿 피드백

### Infrastructure
- Spring Boot 3.5.14
- Kotlin 1.9.25
- H2 Database (in-memory)
- Flyway Migration
- Thymeleaf Templates
- JUnit 5 + Mockito

---

## Version History

- **0.5.0** (2026-04-23): Phase 5 - 17개 직무 확장, 사용자 프로필, AI 개인화 ✨
- **0.4.1** (2026-04-20): Phase 4B - 사용자별 데이터 분리, 권한 기반 접근 제어
- **0.4.0** (2026-04-18): Phase 4A - 회원가입/로그인, Spring Security 통합
- **0.3.0** (2026-04-14): Phase 3 - 프로덕션 준비 완료 (UI/UX, 모니터링, Docker)
- **0.2.0** (2026-04-13): Phase 2 - AI 연동 완료
- **0.1.0** (2026-04-11): Phase 1 - MVP 기반 완료
- **0.0.1** (2026-04-10): 프로젝트 초기화
