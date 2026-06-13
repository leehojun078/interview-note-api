# Phase 8: AI 채팅 면접 개선 사항

**작성일**: 2026-05-04
**대상 브랜치**: `feat/interview-improvement`
**관련 문서**: [PRD](/Users/hojun/.claude/plans/linear-drifting-hummingbird.md)

---

## 개요

Phase 7에서 구현된 AI 채팅 면접 기능의 **평가 정확도**, **피드백 품질**, **사용자 경험**을 대폭 개선합니다.

### 핵심 문제

현재 AI 채팅 면접은 다음과 같은 문제가 있습니다:

1. **부정확한 점수**: "안녕하세요" (30자) → 3.8/5.0점
2. **피드백 부족**: 400-600자로는 구체적인 개선 방향 제시 어려움
3. **경직된 구조**: 강점/개선점 항상 3개씩 (답변 품질 무관)
4. **사용성 저하**: AI 면접 결과를 나중에 찾기 어려움
5. **제한된 기능**: 경력 수준 미지원, 채용 공고 기반 면접 미지원

---

## 10가지 개선 사항

### 1. 종합 평가 점수 계산 로직 개선 ⭐⭐⭐

**문제**: 짧은 답변도 3-4점을 받음 (AI Hallucination)

**개선**:
- 첫 답변(자기소개) 제외한 가중 평균 계산
- 저품질 답변 50% 이상 시 전체 평균 * 0.8 패널티
- AI 프롬프트 엄격화 (명확한 점수 기준 제시)

**효과**:
- 짧은 답변(50자 미만) 평균 점수: **3.8점 → < 2.0점**

---

### 2. 종합 피드백 길이 증가 ⭐⭐

**문제**: 400-600자로는 구체적인 분석 부족

**개선**:
- 피드백 길이: **400-600자 → 800-1200자**
- 4단락 구조 (전반적 인상 200자 + 강점 300자 + 개선점 300자 + 격려 200자)

**효과**:
- 평균 피드백 길이: **500자 → 900자 이상**

---

### 3. 강점/개선점 개수 유연화 ⭐⭐

**문제**: 답변 품질과 무관하게 각 3개 고정

**개선**:
- 강점: **0-5개** (품질에 따라)
- 개선점: **1-5개** (최소 1개)
- 평균 점수에 따른 개수 가이드:
  - 4.0+ → 강점 4-5개, 개선 1-2개
  - 3.0-3.9 → 강점 2-3개, 개선 2-3개
  - 2.0-2.9 → 강점 1-2개, 개선 3-4개
  - < 2.0 → 강점 0-1개, 개선 4-5개

**효과**:
- 저품질 답변에 억지로 강점 만들지 않음
- 고품질 답변에 더 많은 개선점 제시 가능

---

### 4. URL 구조 검토 ⭐

**결정**: `/mock-interviews` 유지 (변경 안함)

**이유**:
- 업계 표준 용어 (Pramp, Interviewing.io)
- Breaking change 비용 > 개선 효과

**대안**:
- UI 레이블 개선 ("AI 면접 연습", "AI 면접 평가 결과")

---

### 5. 리뷰 이력 페이지 통합 ⭐⭐⭐

**문제**: AI 면접 결과를 나중에 찾기 어려움

**개선**:
- `/reviews` 페이지에 2개 탭 추가:
  - 탭 1: **질문 연습** (기존 InterviewAnswer + AiFeedback)
  - 탭 2: **AI 면접** (MockInterview 종합 평가)
- ReviewService에 `getUserMockInterviewReviews()` 추가

**효과**:
- AI 면접 결과 재조회율: **0% → 40% 이상**

---

### 6. "다시 연습하기" vs "새로 연습하기" 구분 ⭐⭐

**문제**: 버튼명 "다시 연습하기"인데 실제로는 새 세션 시작

**개선**:
- 2개 버튼 제공:
  - **이어서 연습하기**: COMPLETED → IN_PROGRESS 전환, 기존 대화 이어감
  - **새로 연습하기**: 새로운 면접 세션 시작
- `MockInterview.resume()` 메서드 추가
- POST `/mock-interviews/{id}/resume` 엔드포인트 추가

**효과**:
- 사용자 혼란 감소
- "이어서 연습하기" 사용률: **0% → 20%**

---

### 7. 채용 공고 기반 AI 채팅 면접 ⭐⭐

**문제**: `MockInterview.jobPostingId`는 있지만 UI 없음

**개선**:
- `job-postings/questions.html`에 "이 공고로 AI 면접 연습" 버튼 추가
- POST `/mock-interviews/start?jobPostingId={id}` 지원
- AI 프롬프트에 채용 공고 정보 포함

**효과**:
- 채용 공고 기반 AI 면접 비율: **0% → 30%**

---

### 8. 사용 방법 안내 UI 추가 ⭐

**문제**: AI 면접 시작 모달에 사용 방법 안내 없음

**개선**:
- `job-field-modal.html`에 4단계 안내 추가:
  1. 면접관 AI가 자기소개부터 시작해 2-5개 질문
  2. 각 질문마다 200자 이내로 답변
  3. AI가 즉시 평가하고 다음 질문 생성
  4. 종합 평가와 개선 포인트 확인

**효과**:
- 첫 사용자 이탈률 감소
- 사용 방법 문의 감소

---

### 9. 경력 수준 선택 및 난이도 조정 ⭐⭐⭐

**문제**: 모든 사용자가 신입 수준 질문만 받음

**개선**:
- 경력 수준 4단계 선택 가능:
  - **ENTRY**: 신입 (경험 1년 미만)
  - **JUNIOR**: 주니어 (1-3년)
  - **SENIOR**: 시니어 (3-7년)
  - **SENIOR_PLUS**: 시니어+ (7년 이상)
- AI 프롬프트에서 경력 수준별 질문 난이도 조정:
  - ENTRY: 기본 개념, 학습 경험
  - JUNIOR: 실무 경험, 기술 활용 사례
  - SENIOR: 아키텍처 설계, 기술 리드
  - SENIOR_PLUS: 기술 전략, 조직 리더십
- `MockInterview.careerLevel` 필드 추가 (Migration V13)

**효과**:
- 경력 수준 선택 비율: **0% → 60%**
- 질문 난이도 만족도 증가

---

### 10. 짧은 답변 품질 검증 강화

**포함**: 요구사항 #1 (종합 평가 점수 개선)에 포함

**개선**:
- AI 프롬프트에 명시적 예시 추가:
  - "ㅎㅎ" → 모든 점수 1점
  - "열심히 노력했습니다" → logic 2, specificity 1, delivery 2
- Fallback 검증: 답변 50자 미만 && 점수 > 2 → 강제로 1-2점

---

## 구현 우선순위

### Phase 8A: 점수 계산 및 피드백 개선 (최우선)
- **기간**: 3-4일
- **개선**: #1, #2, #3, #10
- **핵심**: `calculateWeightedScore()`, 엄격한 AI 프롬프트

### Phase 8B: 경력 수준 및 UI 개선
- **기간**: 2-3일
- **개선**: #7, #8, #9
- **핵심**: 경력 수준 선택, 난이도 조정, 안내 UI

### Phase 8C: 리뷰 통합 및 재개 기능
- **기간**: 2-3일
- **개선**: #5, #6
- **핵심**: 2개 탭, `resume()` 메서드

### Phase 8D: 테스트 및 문서화
- **기간**: 1-2일
- **개선**: -
- **핵심**: Integration Test, 문서 업데이트

---

## 성공 지표

### 기술적 지표

| 지표 | 현재 | 목표 | 측정 방법 |
|-----|-----|-----|---------|
| 짧은 답변(50자 미만) 평균 점수 | 3.8점 | < 2.0점 | Integration Test |
| 종합 피드백 평균 길이 | 500자 | 900자 이상 | `overallFeedback.length` 평균 |
| 강점/개선점 개수 표준편차 | 0 (고정) | > 1.0 | `keyStrengths.size` 표준편차 |

### 사용자 경험 지표

| 지표 | 현재 | 목표 | 측정 방법 |
|-----|-----|-----|---------|
| AI 면접 결과 재조회율 | 0% | 40% | `/reviews` 탭 전환율 |
| 경력 수준 선택 비율 | 0% | 60% | `MockInterview.careerLevel != null` 비율 |
| "이어서 연습하기" 사용률 | 0% | 20% | POST `/resume` 호출 횟수 |
| 채용 공고 기반 AI 면접 비율 | 0% | 30% | `MockInterview.jobPostingId != null` 비율 |

---

## 핵심 파일 (12개)

### 1. Database Migration
- `V13__enhance_mock_interview_scoring.sql` (신규)
  - `mock_interviews.career_level` 추가
  - `mock_interviews.weighted_average_score` 추가

### 2. Domain Layer
- `domain/MockInterview.kt`
  - `careerLevel: CareerLevel?` 필드 추가
  - `weightedAverageScore: Double?` 필드 추가
  - `resume()` 메서드 추가

### 3. Service Layer
- `service/InterviewAiService.kt`
  - `calculateWeightedScore()` 메서드 추가
- `service/ai/PromptBuilder.kt`
  - 엄격한 평가 프롬프트 (5점 기준 명확화)
  - 경력 수준별 난이도 조정 (`getCareerLevelGuidance()`)
- `service/ai/InterviewResponseParser.kt`
  - 800자 검증, 0-5개 검증
- `service/ReviewService.kt`
  - `getUserMockInterviewReviews()` 추가

### 4. Controller Layer
- `controller/ReviewController.kt`
  - 2개 탭 데이터 제공 (`mockInterviewReviews`)
- `controller/MockInterviewController.kt`
  - `careerLevel` 파라미터 추가
  - `resumeInterview()` 엔드포인트 추가

### 5. Template Layer
- `templates/fragments/job-field-modal.html`
  - 경력 수준 선택 UI
  - 사용 방법 안내 UI
- `templates/reviews/list.html`
  - 탭 구조 (질문 연습 / AI 면접)
- `templates/mock-interviews/result.html`
  - "이어서 연습하기" vs "새로 연습하기" 버튼
- `templates/job-postings/questions.html`
  - "이 공고로 AI 면접 연습" 버튼

---

## 기대 효과

### 1. 평가 정확도 향상
- 사용자가 실제 답변 품질에 맞는 점수를 받음
- 무의미한 답변에 낮은 점수 부여 → 학습 동기 부여

### 2. 피드백 품질 개선
- 더 길고 구체적인 피드백 (800-1200자)
- 답변 품질에 맞는 강점/개선점 개수
- 실질적인 개선 방향 제시

### 3. 사용자 경험 향상
- AI 면접 결과를 쉽게 찾아볼 수 있음 (리뷰 이력 통합)
- "이어서 연습하기"로 연속 학습 가능
- 경력 수준에 맞는 질문으로 만족도 증가

### 4. 기능 확장
- 채용 공고 기반 AI 면접으로 실전 연습
- 사용 방법 안내로 첫 사용자 이탈 방지

---

## 위험 요소 및 완화 방안

### 1. AI Hallucination 지속
**위험**: 엄격한 프롬프트에도 불구하고 AI가 관대하게 평가

**완화**:
- Fallback 검증: 답변 50자 미만 && 점수 > 2 → 강제 1-2점
- 명시적 예시: "ㅎㅎ → 1점", "열심히 → 1점"

### 2. 가중 평균 계산 엣지 케이스
**위험**: 자기소개만 답변한 경우 계산 오류

**완화**:
- 최소 2개 답변 필요 (자기소개 제외)
- 1개만 있으면 첫 답변 점수 그대로 사용

### 3. 경력 수준 선택 혼란
**위험**: 사용자가 자신의 경력 수준을 모를 수 있음

**완화**:
- 도움말 텍스트: "경험 1년 미만", "1-3년"
- Optional 필드 (기본값: ENTRY)

---

## 다음 단계

1. ✅ PHASE8_AI_CHAT_INTERVIEW_IMPROVEMENTS.md 작성 (완료)
2. ✅ phase8_ai_chat_interview_plan.md 작성 (완료)
3. ⏳ Phase 8A 구현 시작
4. ⏳ Phase 8B, 8C, 8D 순차 구현
5. ⏳ PHASE8_COMPLETION_REPORT.md 작성

---

**문서 종료**
