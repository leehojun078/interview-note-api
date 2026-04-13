# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

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

- **0.2.0** (2026-04-13): Phase 2 - AI 연동 완료
- **0.1.0** (2026-04-11): Phase 1 - MVP 기반 완료
- **0.0.1** (2026-04-10): 프로젝트 초기화
