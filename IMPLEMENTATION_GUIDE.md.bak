# Implementation Guide

면접 복기 웹 애플리케이션 구현 가이드

## 목차
1. [화면 흐름](#1-화면-흐름)
2. [API 엔드포인트](#2-api-엔드포인트)
3. [사용자 시나리오](#3-사용자-시나리오)
4. [DTO 정의](#4-dto-정의)
5. [비기능적 요구사항](#5-비기능적-요구사항)
6. [UI/UX 가이드라인](#6-uiux-가이드라인)

---

## 1. 화면 흐름

### 1.1 질문 목록 페이지

**URL**: `/questions`

**입력 파라미터**:
- `category` (optional): String - 필터링할 카테고리 (예: "기술역량", "문제해결")
- `difficulty` (optional): String - EASY, MEDIUM, HARD
- `targetJob` (optional): String - 대상 직무 (예: "백엔드 개발자")

**출력**: Question 목록 (카드 형태)

**UI 요소**:
- 필터 드롭다운 (카테고리, 난이도, 직무)
- 질문 카드 목록 (제목, 카테고리, 난이도 표시)
- 각 카드는 클릭 가능

**액션**:
- 질문 카드 클릭 → `/questions/{id}/answer`로 이동

---

### 1.2 질문 상세 + 답변 작성 페이지

**URL**: `/questions/{id}/answer`

**입력**: `questionId` (path variable)

**출력**:
- Question 상세 정보 (내용, 카테고리, 난이도, 직무)
- 답변 작성 폼 (textarea, 최대 2000자)

**UI 요소**:
- 질문 표시 영역 (카드 형태)
- 답변 입력 textarea
- 글자 수 카운터 (예: "500 / 2000자")
- 제출 버튼 (primary)
- 목록으로 돌아가기 버튼 (secondary)

**액션**:
- 제출 버튼 클릭 → POST `/questions/{id}/answer` → `/answers/{answerId}/feedback`로 리다이렉트
- 목록으로 돌아가기 → `/questions`로 이동

**Validation**:
- 답변 최소 길이: 50자
- 답변 최대 길이: 2000자
- 빈 답변 제출 방지

---

### 1.3 평가 결과 페이지

**URL**: `/answers/{answerId}/feedback`

**입력**: `answerId` (path variable)

**출력**:
- 작성한 질문 (읽기 전용)
- 작성한 답변 (읽기 전용)
- AI 평가 결과:
  - 4가지 점수 (1-5): 논리성, 구체성, 직무적합성, 전달력
  - 강점 리스트 (2-3개)
  - 개선점 리스트 (2-3개)
  - 모범답변
  - 종합 코멘트

**UI 요소**:
- 질문 표시 영역
- 답변 표시 영역
- 점수 시각화 (막대 그래프 또는 별점)
  - 각 항목별 점수 표시
  - 평균 점수 강조
- 강점 섹션 (그린 계열 아이콘)
- 개선점 섹션 (오렌지 계열 아이콘)
- 모범답변 섹션 (접기/펼치기 토글 가능)
- 종합 코멘트
- 액션 버튼들

**액션**:
- 다른 질문 연습하기 → `/questions`로 이동
- 복기 이력 보기 → `/reviews`로 이동

---

### 1.4 복기 이력 목록 페이지

**URL**: `/reviews`

**입력**: 없음

**출력**: InterviewAnswer + AiFeedback 결합 목록

**UI 요소**:
- 이력 카드 목록
  - 질문 제목 (첫 50자)
  - 카테고리 뱃지
  - 작성일시
  - 평균 점수 (별점 또는 숫자)
- 최신순 정렬 (createdAt DESC)
- 빈 상태: "아직 답변한 질문이 없습니다" 메시지

**액션**:
- 이력 카드 클릭 → `/reviews/{answerId}`로 이동
- 질문 연습하기 버튼 → `/questions`로 이동

---

### 1.5 복기 상세 페이지

**URL**: `/reviews/{answerId}`

**입력**: `answerId` (path variable)

**출력**: 평가 결과 페이지와 동일 (과거 답변 재확인)

**UI 요소**:
- 평가 결과 페이지(1.3)와 동일
- 추가 요소:
  - "다시 답변하기" 버튼 (같은 질문 재도전)
  - 작성일시 표시

**액션**:
- 다시 답변하기 → `/questions/{questionId}/answer`로 이동
- 복기 목록으로 → `/reviews`로 이동

---

### 1.6 홈 페이지 (Optional)

**URL**: `/` 또는 `/home`

**출력**:
- 프로젝트 소개 (간단한 설명)
- 최근 답변 3개 (미리보기)
- 주요 기능 안내
- CTA 버튼들

**UI 요소**:
- 히어로 섹션 (제목 + 부제목)
- 최근 답변 카드 3개
- CTA 버튼들 (primary, secondary)

**액션**:
- "질문 연습 시작" (primary) → `/questions`
- "내 복기 이력" (secondary) → `/reviews`

---

### 1.7 에러 페이지

**URL**: `/error`

**출력**:
- 에러 메시지
- 에러 코드 (optional)
- 홈으로 돌아가기 버튼

**UI 요소**:
- 에러 아이콘
- 사용자 친화적 메시지
- 홈 버튼

---

## 2. API 엔드포인트

### Question 관련

#### 2.1 질문 목록 조회

```
GET /api/questions
```

**파라미터**:
- `category` (optional): String
- `difficulty` (optional): String
- `targetJob` (optional): String

**응답**: `200 OK`
```json
[
  {
    "id": 1,
    "jobField": "IT",
    "targetJob": "백엔드 개발자",
    "category": "기술역량",
    "content": "Spring Bean의 생명주기를 설명해주세요.",
    "difficulty": "MEDIUM"
  },
  {
    "id": 2,
    "jobField": "IT",
    "targetJob": "백엔드 개발자",
    "category": "문제해결",
    "content": "대용량 트래픽을 처리한 경험이 있나요?",
    "difficulty": "HARD"
  }
]
```

---

#### 2.2 질문 상세 조회

```
GET /api/questions/{id}
```

**파라미터**: `id` (path) - Long

**응답**: `200 OK`
```json
{
  "id": 1,
  "jobField": "IT",
  "targetJob": "백엔드 개발자",
  "category": "기술역량",
  "content": "Spring Bean의 생명주기를 설명해주세요.",
  "difficulty": "MEDIUM"
}
```

**에러**:
- `404 NOT FOUND` - 질문을 찾을 수 없음

---

### Answer 관련

#### 2.3 답변 제출 및 AI 평가

```
POST /api/answers
```

**요청 Body**:
```json
{
  "questionId": 1,
  "answerText": "Spring Bean은 IoC 컨테이너에 의해 관리되는 객체입니다..."
}
```

**처리 흐름**:
1. InterviewAnswer 엔티티 저장
2. OpenAI API 호출하여 AI 평가 요청
3. AiFeedback 엔티티 저장
4. 결합된 결과 반환

**응답**: `201 CREATED`
```json
{
  "answerId": 10,
  "questionId": 1,
  "questionContent": "Spring Bean의 생명주기를 설명해주세요.",
  "answerText": "Spring Bean은 IoC 컨테이너에 의해 관리되는 객체입니다...",
  "answeredAt": "2026-04-11T14:30:00",
  "feedback": {
    "logicScore": 4,
    "specificityScore": 3,
    "jobFitScore": 4,
    "deliveryScore": 3,
    "strengths": [
      "IoC 개념을 정확히 이해하고 있음",
      "STAR 기법을 활용한 구조적 답변"
    ],
    "improvements": [
      "Bean 생명주기의 각 단계를 더 구체적으로 설명 필요",
      "소멸(Destruction) 단계에 대한 언급 부족"
    ],
    "modelAnswer": "Spring Bean의 생명주기는 다음과 같습니다. 1) 인스턴스화...",
    "overallComment": "전반적으로 개념 이해도가 높으나, 생명주기 단계별 설명을 보완하면 더 좋겠습니다."
  }
}
```

**에러**:
- `400 BAD REQUEST` - Validation 실패 (빈 답변, 너무 긴 답변 등)
- `404 NOT FOUND` - 질문을 찾을 수 없음
- `500 INTERNAL SERVER ERROR` - AI 평가 실패 (fallback 처리 필요)

---

#### 2.4 답변 상세 조회

```
GET /api/answers/{id}
```

**파라미터**: `id` (path) - Long (answerId)

**응답**: `200 OK` (위의 AnswerWithFeedbackDto와 동일)

**에러**:
- `404 NOT FOUND` - 답변을 찾을 수 없음

---

### Review 관련

#### 2.5 복기 이력 목록 조회

```
GET /api/reviews
```

**응답**: `200 OK`
```json
[
  {
    "answerId": 10,
    "questionContent": "Spring Bean의 생명주기를 설명해주세요.",
    "category": "기술역량",
    "answeredAt": "2026-04-11T14:30:00",
    "averageScore": 3.5
  },
  {
    "answerId": 9,
    "questionContent": "RESTful API 설계 원칙은 무엇인가요?",
    "category": "기술역량",
    "answeredAt": "2026-04-11T13:15:00",
    "averageScore": 4.0
  }
]
```

**정렬**: `answeredAt` DESC (최신순)

---

#### 2.6 복기 상세 조회

```
GET /api/reviews/{answerId}
```

**파라미터**: `answerId` (path) - Long

**응답**: `200 OK` (AnswerWithFeedbackDto - 2.3과 동일)

**에러**:
- `404 NOT FOUND` - 답변을 찾을 수 없음

---

### 페이지 Controller (Thymeleaf)

#### 2.7 질문 목록 페이지

```
GET /questions
```

**Controller**: `QuestionController`
- Model: `List<Question>`
- View: `questions/list.html`

---

#### 2.8 질문 상세 + 답변 작성 페이지

```
GET /questions/{id}/answer
```

**Controller**: `AnswerController`
- Model: `Question`
- View: `questions/answer.html`

---

#### 2.9 답변 제출 처리

```
POST /questions/{id}/answer
```

**Controller**: `AnswerController`
- Form Data: `answerText`
- 처리:
  1. `POST /api/answers` 호출
  2. AnswerId 받기
- Redirect: `/answers/{answerId}/feedback`

---

#### 2.10 평가 결과 페이지

```
GET /answers/{answerId}/feedback
```

**Controller**: `FeedbackController`
- Model: `AnswerWithFeedback`
- View: `answers/feedback.html`

---

#### 2.11 복기 이력 목록 페이지

```
GET /reviews
```

**Controller**: `ReviewController`
- Model: `List<ReviewSummary>`
- View: `reviews/list.html`

---

#### 2.12 복기 상세 페이지

```
GET /reviews/{answerId}
```

**Controller**: `ReviewController`
- Model: `AnswerWithFeedback`
- View: `reviews/detail.html`

---

#### 2.13 홈 페이지

```
GET / 또는 GET /home
```

**Controller**: `HomeController`
- Model: `RecentAnswers` (최근 3개)
- View: `home.html`

---

## 3. 사용자 시나리오

### 시나리오 1: 첫 면접 연습

**목표**: 처음 앱을 사용하는 사용자가 질문을 선택하고 답변을 제출하여 AI 평가를 받는다.

**단계**:
1. 사용자가 홈페이지(`/`) 접속
2. "질문 연습 시작" 버튼 클릭
3. 질문 목록 페이지(`/questions`)로 이동
4. 필터 적용: "기술역량" 카테고리, "MEDIUM" 난이도 선택
5. "Spring Bean의 생명주기를 설명해주세요" 질문 카드 클릭
6. 질문 상세 + 답변 작성 페이지(`/questions/1/answer`)로 이동
7. Textarea에 500자 답변 입력
   ```
   Spring Bean은 IoC 컨테이너에 의해 관리되는 객체입니다.
   생명주기는 크게 4단계로 나뉩니다...
   ```
8. 글자 수 카운터 확인: "500 / 2000자"
9. "제출" 버튼 클릭
10. 로딩 스피너 표시 (AI 평가 진행 중)
11. 평가 완료 후 평가 결과 페이지(`/answers/10/feedback`)로 리다이렉트
12. 평가 결과 확인:
    - 논리성: ⭐⭐⭐⭐ (4점)
    - 구체성: ⭐⭐⭐ (3점)
    - 직무적합성: ⭐⭐⭐⭐ (4점)
    - 전달력: ⭐⭐⭐ (3점)
    - **평균: 3.5점**
13. 강점 확인:
    - ✅ "IoC 개념을 정확히 이해하고 있음"
    - ✅ "STAR 기법을 활용한 구조적 답변"
14. 개선점 확인:
    - ⚠️ "Bean 생명주기의 각 단계를 더 구체적으로 설명 필요"
    - ⚠️ "소멸(Destruction) 단계에 대한 언급 부족"
15. "모범답변 보기" 토글 클릭하여 모범답변 확인
16. "다른 질문 연습하기" 버튼 클릭 → 질문 목록으로 복귀

**예상 소요 시간**: 10-15분

---

### 시나리오 2: 답변 복기 및 재도전

**목표**: 과거에 작성한 답변을 다시 확인하고, 같은 질문에 개선된 답변을 제출한다.

**단계**:
1. 사용자가 네비게이션 바에서 "복기 이력" 클릭
2. 복기 이력 목록 페이지(`/reviews`)로 이동
3. 이력 카드 목록 확인 (최신순):
   - "Spring Bean의 생명주기" - 평균 3.5점 - 2026-04-11 14:30
   - **"RESTful API 설계 원칙" - 평균 3.0점 - 2026-04-11 13:15** ← 선택
   - "대용량 트래픽 처리 경험" - 평균 4.0점 - 2026-04-10 16:00
4. "RESTful API 설계 원칙" 카드 클릭
5. 복기 상세 페이지(`/reviews/9`)로 이동
6. 이전 답변 확인:
   ```
   RESTful API는 HTTP 메서드를 사용하여...
   ```
7. 평가 결과 확인: 평균 3.0점 (낮음)
8. 개선점 확인:
   - ⚠️ **"구체적인 HTTP 메서드별 용도 설명 부족"**
   - ⚠️ "상태 코드 활용 언급 필요"
9. 모범답변 확인하여 부족한 부분 파악
10. "다시 답변하기" 버튼 클릭
11. 동일 질문의 답변 작성 페이지(`/questions/2/answer`)로 이동
12. 이전 피드백을 참고하여 개선된 답변 작성:
    ```
    RESTful API는 HTTP 메서드를 사용하여 리소스를 조작합니다.
    - GET: 리소스 조회 (예: GET /users/1)
    - POST: 리소스 생성 (예: POST /users)
    - PUT: 리소스 전체 수정
    - DELETE: 리소스 삭제
    또한 상태 코드를 활용하여...
    ```
13. 제출 후 새로운 평가 확인
14. 개선된 점수 확인:
    - 논리성: 4점
    - 구체성: **4점** (이전 2점에서 향상)
    - 직무적합성: 4점
    - 전달력: 4점
    - **평균: 4.0점** (이전 3.0점에서 향상)
15. 개선 성과 확인 및 만족

**예상 소요 시간**: 15-20분

---

### 시나리오 3: 카테고리별 집중 연습

**목표**: 특정 카테고리와 난이도의 질문들을 집중적으로 연습한다.

**단계**:
1. 사용자가 질문 목록 페이지(`/questions`) 접속
2. 필터 적용:
   - 카테고리: **"문제해결"** 선택
   - 난이도: **"HARD"** 선택
3. 필터링된 고난이도 문제해결 질문 목록 확인 (5개)
4. "대용량 트래픽을 처리한 경험이 있나요?" 질문 선택
5. 답변 작성 페이지로 이동
6. 1500자 분량의 상세한 답변 작성 (실제 프로젝트 경험 기반)
   ```
   네, 이전 프로젝트에서 대용량 트래픽을 처리한 경험이 있습니다.

   상황:
   - 특정 이벤트 기간에 평소 대비 10배 이상의 트래픽 발생
   - 기존 단일 서버 구조로는 부하 감당 불가

   조치:
   - Redis 캐싱 도입하여 DB 부하 90% 감소
   - 로드 밸런서 구성으로 수평 확장
   - CDN 활용하여 정적 리소스 처리

   결과:
   - 초당 1만 요청 처리 가능 (기존 1천 요청 대비 10배 향상)
   - 응답 시간 200ms 이하 유지
   - 무중단 서비스 제공 성공
   ```
7. 제출 후 AI 평가 대기
8. 평가 결과 확인:
   - 논리성: 5점
   - 구체성: 4점
   - 직무적합성: 5점
   - 전달력: 4점
   - 평균: 4.5점
9. 강점 확인:
   - ✅ "STAR 기법을 체계적으로 활용"
   - ✅ "기술적 깊이 있는 솔루션 제시"
10. 개선점 확인:
    - ⚠️ **"수치화된 성과를 더 강조하면 좋겠음"**
    - ⚠️ "모니터링 및 장애 대응 언급 추가 권장"
11. 모범답변 확인:
    - **"초당 10만 요청 처리"** 같은 더 구체적인 수치 예시 확인
    - "Prometheus + Grafana 모니터링" 같은 도구 언급 확인
12. 다음 답변에서 반영하기 위해 개선점 메모
13. 같은 카테고리의 다른 HARD 질문으로 연습 계속

**예상 소요 시간**: 20-30분 (카테고리별 3-5개 질문 연습)

---

## 4. DTO 정의

### 4.1 QuestionDto

```kotlin
data class QuestionDto(
    val id: Long,
    val jobField: String,          // "IT", "영업", "경영" 등
    val targetJob: String,          // "백엔드 개발자", "프론트엔드 개발자"
    val category: String,           // "기술역량", "문제해결", "협업경험"
    val content: String,            // 질문 내용
    val difficulty: String          // "EASY", "MEDIUM", "HARD"
)
```

**사용처**:
- `GET /api/questions` 응답
- `GET /api/questions/{id}` 응답

---

### 4.2 AnswerSubmitDto

```kotlin
data class AnswerSubmitDto(
    val questionId: Long,
    val answerText: String
)
```

**Validation**:
- `questionId`: required, must exist
- `answerText`: required, length 50-2000

**사용처**:
- `POST /api/answers` 요청 Body

---

### 4.3 FeedbackDto

```kotlin
data class FeedbackDto(
    val logicScore: Int,            // 1-5
    val specificityScore: Int,      // 1-5
    val jobFitScore: Int,           // 1-5
    val deliveryScore: Int,         // 1-5
    val strengths: List<String>,    // 2-3개 항목
    val improvements: List<String>, // 2-3개 항목
    val modelAnswer: String,        // 400-600자
    val overallComment: String      // 종합 코멘트
)
```

**사용처**:
- `AnswerWithFeedbackDto`의 nested object

---

### 4.4 AnswerWithFeedbackDto

```kotlin
data class AnswerWithFeedbackDto(
    val answerId: Long,
    val questionId: Long,
    val questionContent: String,     // 질문 내용 (조인)
    val answerText: String,
    val answeredAt: LocalDateTime,
    val feedback: FeedbackDto
)
```

**사용처**:
- `POST /api/answers` 응답
- `GET /api/answers/{id}` 응답
- `GET /api/reviews/{answerId}` 응답

---

### 4.5 ReviewSummaryDto

```kotlin
data class ReviewSummaryDto(
    val answerId: Long,
    val questionContent: String,     // 질문 내용 (조인)
    val category: String,            // 카테고리 (조인)
    val answeredAt: LocalDateTime,
    val averageScore: Double         // (4개 점수 합) / 4
)
```

**계산식**:
```kotlin
averageScore = (logicScore + specificityScore + jobFitScore + deliveryScore) / 4.0
```

**사용처**:
- `GET /api/reviews` 응답

---

## 5. 비기능적 요구사항

### 5.1 성능

| 항목 | 요구사항 |
|------|----------|
| API 응답 시간 | 200ms 이하 (AI 호출 제외) |
| AI 평가 응답 시간 | 10초 이내 |
| 동시 사용자 | MVP: 1명 (단일 사용자 모드) |
| DB 쿼리 최적화 | N+1 문제 방지 (Fetch Join 활용) |

---

### 5.2 보안

| 항목 | 요구사항 |
|------|----------|
| 인증/인가 | MVP: 없음 (단일 사용자 모드) |
| OpenAI API Key | 환경변수로 관리 (`OPENAI_API_KEY`) |
| 코드 내 비밀정보 | 절대 하드코딩 금지 |
| SQL Injection | JPA Parameterized Query 사용 |
| XSS 방지 | Thymeleaf의 자동 escaping 활용 |

---

### 5.3 제약사항

| 항목 | 제약 |
|------|------|
| 답변 최소 길이 | 50자 |
| 답변 최대 길이 | 2000자 |
| 모범답변 길이 | 400-600자 (maxTokens=800) |
| AI 평가 실패 | Graceful fallback (에러 메시지 표시, 저장은 유지) |
| 요청 제한 | IP당 시간당 10회 (추후 구현) |

---

### 5.4 데이터

| 항목 | 요구사항 |
|------|----------|
| 초기 질문 데이터 | 최소 20개 (카테고리/난이도 다양) |
| 답변 이력 | 무제한 저장 |
| 데이터베이스 | H2 (개발) → PostgreSQL (프로덕션) |
| 마이그레이션 | Flyway 사용 |

---

### 5.5 에러 처리

**AI 평가 실패 시**:
1. InterviewAnswer는 정상 저장
2. AiFeedback은 저장 실패 (또는 fallback 데이터)
3. 사용자에게 에러 메시지 표시:
   ```
   "AI 평가에 일시적으로 실패했습니다.
   답변은 저장되었으며, 나중에 다시 확인해주세요."
   ```
4. 로그 기록 (디버깅용)

**일반 에러**:
- 400: Validation 실패 → 사용자 친화적 메시지
- 404: 리소스 없음 → "요청하신 내용을 찾을 수 없습니다"
- 500: 서버 에러 → "일시적인 오류가 발생했습니다"

---

## 6. UI/UX 가이드라인

### 6.1 레이아웃

**네비게이션 바** (모든 페이지 공통):
```
[로고] [홈] [질문 연습] [복기 이력]
```

**컨테이너**:
- 최대 너비: 1200px
- 중앙 정렬
- 좌우 패딩: 20px

**반응형**:
- 데스크톱 우선 (1024px 이상)
- 모바일은 선택적 (MVP에서는 제외 가능)

---

### 6.2 색상 팔레트

| 용도 | 색상 | Hex |
|------|------|-----|
| Primary (주요 버튼) | 프로페셔널 블루 | #2563EB |
| Secondary (보조 버튼) | 그레이 | #6B7280 |
| 강점 (Strengths) | 그린 | #10B981 |
| 개선점 (Improvements) | 오렌지 | #F59E0B |
| 에러 | 레드 | #EF4444 |
| 배경 | 라이트 그레이 | #F9FAFB |
| 텍스트 | 다크 그레이 | #111827 |

---

### 6.3 타이포그래피

| 요소 | 크기 | 굵기 |
|------|------|------|
| 페이지 제목 (h1) | 32px | Bold |
| 섹션 제목 (h2) | 24px | Semibold |
| 질문 내용 | 18px | Semibold |
| 답변 입력/표시 | 16px | Regular |
| 피드백 내용 | 14px | Regular |
| 캡션/라벨 | 12px | Regular |

**폰트 패밀리**:
- 한글: Pretendard, -apple-system, sans-serif
- 영문: Inter, system-ui, sans-serif

---

### 6.4 컴포넌트

**버튼**:
- Primary: 파란색 배경, 흰색 텍스트, 호버 시 어두워짐
- Secondary: 회색 테두리, 투명 배경, 호버 시 배경 생김
- 크기: 높이 44px, 패딩 16px 24px
- 둥근 모서리: 6px

**카드**:
- 배경: 흰색
- 그림자: subtle (0 1px 3px rgba(0,0,0,0.1))
- 둥근 모서리: 8px
- 패딩: 20px
- 호버 시 살짝 올라감 (transform: translateY(-2px))

**입력 필드**:
- 테두리: 1px solid #D1D5DB
- 포커스 시: 테두리 파란색
- 높이: 44px (input), auto (textarea)
- 둥근 모서리: 6px

---

### 6.5 인터랙션

**로딩 상태**:
- 제출 버튼 클릭 시 스피너 표시
- 버튼 비활성화 (disabled)
- 메시지: "평가 중입니다..."

**Toast 메시지**:
- 성공: 그린 배경, 체크 아이콘
- 에러: 레드 배경, X 아이콘
- 위치: 화면 상단 중앙
- 자동 사라짐: 3초

**애니메이션**:
- 페이지 전환: 부드러운 fade-in
- 카드 호버: transform + shadow 변화
- 토글 (모범답변): slide-down

---

### 6.6 아이콘

사용 권장 아이콘 라이브러리: **Heroicons** 또는 **Lucide Icons**

| 용도 | 아이콘 |
|------|--------|
| 강점 | ✅ CheckCircle (green) |
| 개선점 | ⚠️ AlertCircle (orange) |
| 에러 | ❌ XCircle (red) |
| 로딩 | 🔄 Loader (spinning) |
| 정보 | ℹ️ InfoCircle (blue) |

---

## 구현 순서 (Phase 1: AI 없는 플로우)

MVP 구현은 3단계로 나뉩니다. **Phase 1**에서는 AI 없이 전체 플로우를 완성합니다.

1. ✅ Spring Boot 프로젝트 생성
2. ✅ 엔티티 3개 생성 (Question, InterviewAnswer, AiFeedback)
3. ✅ Repository 인터페이스 생성
4. ✅ Flyway 마이그레이션 스크립트 작성
5. ✅ 초기 질문 데이터 20개 삽입 (seed data)
6. ✅ 질문 목록 페이지 구현 (Controller + Thymeleaf)
7. ✅ 질문 상세 + 답변 작성 페이지 구현
8. ✅ 답변 저장 기능 구현
9. ✅ **더미 피드백**으로 평가 결과 페이지 구현
10. ✅ 복기 이력 목록 페이지 구현
11. ✅ 복기 상세 페이지 구현

**Phase 1 완료 조건**: AI 없이도 전체 사용자 플로우가 동작해야 함

---

## 참고 문서

- [CLAUDE.md](./CLAUDE.md) - 프로젝트 전체 가이드
- [README.md](./README.md) - 프로젝트 개요 (추후 작성)
- Spring Boot 공식 문서: https://spring.io/projects/spring-boot
- Thymeleaf 공식 문서: https://www.thymeleaf.org/

---

**작성일**: 2026-04-11
**버전**: 1.0
**작성자**: Claude Code with 호준
