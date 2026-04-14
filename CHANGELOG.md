# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

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

- **0.3.0** (2026-04-14): Phase 3 - 프로덕션 준비 완료 (UI/UX, 모니터링, Docker)
- **0.2.0** (2026-04-13): Phase 2 - AI 연동 완료
- **0.1.0** (2026-04-11): Phase 1 - MVP 기반 완료
- **0.0.1** (2026-04-10): 프로젝트 초기화
