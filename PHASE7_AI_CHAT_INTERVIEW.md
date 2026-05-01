# PRD: 실시간 AI 채팅 면접 시스템

**프로젝트**: Interview Note API - Phase 7
**작성일**: 2026-04-27
**최종 검토일**: 2026-04-30
**버전**: 1.1
**현재 상태**: Phase 1-6 완료 (17개 직무, 340개 정적 질문, 채용 공고 기반 질문 생성 지원)
**핵심 기능**: 🎯 **실시간 AI 채팅 면접** (직무 기반 또는 채용 공고 기반)

---

## 📋 Executive Summary

### 배경
현재 Interview Note API는 정적 질문 기반 답변 작성 방식으로, **실시간 대화형 면접 연습**이 불가능하여 실전 준비에 한계가 있습니다. 실제 면접은 일방향 답변이 아닌, 면접관과의 대화 형태로 진행되며, 답변에 따라 꼬리 질문이 이어집니다.

### 목표
**실시간 채팅 기반 모의 면접**: 사용자가 선택한 **직무** 또는 **채용 공고**를 기반으로 AI 면접관과 실시간 대화 형태의 모의 면접을 진행합니다. AI는 답변에 따라 꼬리 질문을 하고, 면접 종료 시 종합 평가를 제공합니다.

### 핵심 변경사항 (Phase 6 대비)
- **채용 공고는 선택 사항**: 직무만 선택해도 면접 시작 가능
- **직무 선택 모달**: 홈페이지에서 "AI 면접 연습" 버튼 클릭 시 17개 직무 선택
- **2가지 면접 모드**:
  1. **직무 기반 일반 면접**: 공고 없이 직무 역량 평가
  2. **공고 기반 맞춤 면접**: 특정 회사/포지션 맞춤 질문

### 기대 효과
- **실전 준비 향상**: 실제 면접과 유사한 대화 연습
- **대화 능력 훈련**: 일방향 답변이 아닌 실시간 대화로 면접 흐름 익히기
- **유연성 증가**: 채용 공고가 없어도 직무 기반으로 연습 가능
- **차별화된 피드백**: 답변 흐름에 따른 동적 평가 및 꼬리 질문

---

## 🎯 기능 요구사항

### 2.1 모의 면접 시작

**사용자 스토리 1 (직무 기반)**:
> "채용 공고 없이 백엔드 개발자 직무로만 일반 면접 연습을 하고 싶습니다."

**사용자 스토리 2 (공고 기반)**:
> "특정 회사 채용 공고에 맞춘 맞춤형 면접 연습을 하고 싶습니다."

**기능 명세**:

#### 시나리오 1: 직무 기반 일반 면접
1. 홈페이지에서 "AI 면접 연습" 버튼 클릭
2. **직무 선택 모달** 표시 (17개 직무 드롭다운)
3. 직무 선택 후 "면접 시작" 클릭
4. 새로운 `MockInterview` 세션 생성 (`jobPostingId` = null)
5. 채팅 화면으로 이동
6. AI가 첫 질문 자동 생성 (예: "자기소개를 부탁드립니다")

#### 시나리오 2: 공고 기반 맞춤 면접
1. 채용 공고 목록 → 공고 선택 → 공고 상세 페이지
2. "이 공고로 면접 연습" 버튼 클릭
3. 직무는 공고의 `selectedJobField` 또는 `inferredJobField` 자동 사용
4. 새로운 `MockInterview` 세션 생성 (`jobPostingId` = 123)
5. 채팅 화면으로 이동
6. AI가 공고 내용 기반 첫 질문 생성

**세션 관리**:
- 세션 ID: `MockInterview.id`
- 상태: `IN_PROGRESS` (진행 중) / `COMPLETED` (완료) / `ABORTED` (중단)
- 메타데이터:
  - 시작 시간 (`startedAt`)
  - 종료 시간 (`endedAt`)
  - 연관 채용 공고 (`jobPostingId` - nullable)
  - 선택 직무 (`selectedJobField` - 필수)

---

### 2.2 실시간 채팅 인터페이스

**UI 요구사항**:
- **헤더**:
  - **공고 기반**: 공고 정보 (회사명, 포지션명)
  - **직무 기반**: 직무명 표시 (예: "IT개발 면접")
  - 면접 시작 시간
  - "면접 종료" 버튼 (빨간색)
- **채팅 메시지 영역** (높이 400px, 스크롤 가능):
  - AI 메시지: 왼쪽 정렬, 파란색 배경
  - 사용자 메시지: 오른쪽 정렬, 초록색 배경
  - 각 메시지에 타임스탬프 표시
  - 자동 스크롤 (최신 메시지로)
- **입력 폼**:
  - 여러 줄 텍스트 입력 (textarea, 3줄)
  - "전송" 버튼
  - 엔터키로 전송 (Shift+엔터는 줄바꿈)

**실시간 통신 방식: Server-Sent Events (SSE)**
- 사용자 → 서버: HTTP POST 요청 (`/mock-interviews/{id}/messages`)
- 서버 → 사용자: SSE 스트림 (`/mock-interviews/{id}/stream`)
- 장점:
  - WebSocket보다 구현 간단
  - HTTP 기반이라 방화벽 이슈 없음
  - 브라우저 재연결 자동 처리

**대안 (미채택)**:
- WebSocket: 복잡도 높음, STOMP 설정 필요
- Long Polling: 실시간성 낮음, 서버 부하 높음

---

### 2.3 AI 면접 진행 로직

**글자수 제한**:
- **AI 질문**: 200자 이내 (초과 시 2개 질문으로 분할)
- **사용자 답변**: 200자 이내로 제한

**질문 분할 예시**:
- ❌ "Kotlin의 코루틴을 실무 서비스에 적용했던 경험이 있다면, 어떤 API나 비즈니스 로직에 사용했는지 설명해주시고, 기존의 동기 방식이나 CompletableFuture 기반 구현과 비교했을 때 처리량, 응답 속도, 코드 복잡도 측면에서 어떤 차이가 있었는지..." (210자)
- ✅ 질문 1: "Kotlin의 코루틴을 실무 서비스에서 어떤 API나 로직에 적용했나요?" (40자)
- ✅ 질문 2 (꼬리질문): "기존 방식과 비교했을 때 성능·복잡도 측면의 차이는?" (30자)

**대화 흐름**:
1. **첫 질문** (AI, 200자 이내): "자기소개를 부탁드립니다."
2. **사용자 답변**: (텍스트 입력)
3. **AI 평가 + 다음 질문**:
   - 답변을 평가 (논리성, 구체성, 전달력 각 1-5점)
   - 평가 결과를 `InterviewMessage`에 저장
   - 다음 행동 결정:
     - **꼬리 질문**: 답변이 부족하거나 더 깊이 파고들 필요
     - **긍정 피드백 + 새 질문**: 답변이 충분함
     - **힌트 제공 + 재질문**: 답변이 방향을 벗어남
4. 3번 반복 (평균 5-10턴 대화)

**AI 프롬프트 구조 (직무 기반 vs 공고 기반)**:

#### 직무 기반 프롬프트 (jobPostingId = null)
```
[System Prompt]
당신은 {직무} 분야의 면접관입니다.

면접 진행 방식:
1. 자연스러운 대화 형태로 질문 (200자 이내)
2. 답변에 따라 꼬리 질문 (2-3번)
3. 답변이 부족하면 힌트 제공
4. 좋은 답변에는 긍정적 피드백

평가 기준:
- 논리성: {직무별 논리 기준}
- 구체성: {직무별 구체성 기준}
- 전달력: 명확하고 이해하기 쉬운 설명

일반적인 {직무} 역량을 평가하는 질문을 진행하세요.
예: IT → 기술역량, 문제해결, 협업경험
```

#### 공고 기반 프롬프트 (jobPostingId = 123)
```
[System Prompt]
당신은 {회사명}의 {포지션} 면접관입니다.

채용 공고 요구사항:
- 필수 기술: {requiredSkills}
- 우대 기술: {preferredSkills}
- 직무 설명: {jobDescription 요약}

면접 진행 방식:
1. 자연스러운 대화 형태로 질문 (200자 이내)
2. 답변에 따라 꼬리 질문 (2-3번)
3. **공고의 필수 기술을 중점적으로 질문**
4. 좋은 답변에는 긍정적 피드백

중요: 모든 질문은 200자를 초과하지 않도록 하세요.
```

**AI 응답 형식**:
```json
{
  "evaluation": {
    "logicScore": 3,
    "specificityScore": 2,
    "deliveryScore": 4,
    "comment": "답변이 간결하나, 구체적인 기술 스택 언급이 부족합니다."
  },
  "nextAction": {
    "question": "주로 사용한 백엔드 프레임워크는 무엇인가요?",
    "reasoning": "구체적인 기술 경험을 확인하기 위한 꼬리 질문",
    "isFollowUp": true,
    "questionLength": 28
  }
}
```

**글자수 검증**:
- AI 응답에서 `question` 필드의 길이가 200자를 초과하면 에러 로그 기록
- 프로덕션에서는 자동으로 200자로 절단 (긴급 대응)

---

### 2.4 면접 종료 및 종합 평가

**종료 트리거**:
- 사용자가 "면접 종료" 버튼 클릭
- 세션이 5분 이상 비활성 (자동 종료, 향후 구현)

**종합 평가 생성**:
- 전체 대화 내역을 AI에게 전달하여 종합 평가 요청
- 출력:
  - 평균 점수 (1-5점, 소수점 1자리)
  - 종합 피드백 (400-600자)
  - 주요 강점 (3개)
  - 개선점 (3개)
  - 채용 추천도 (추천/보류/비추천 + 근거)

**종합 평가 프롬프트**:
```
면접 종료 - 전체 대화를 기반으로 종합 평가를 작성하세요.

면접 유형: {직무 기반 또는 공고 기반}
직무: {selectedJobField}
[공고 기반인 경우]
채용 공고: {포지션} at {회사명}

지원자의 모든 답변:
답변 1: 안녕하세요, 저는...
답변 2: Spring Boot를 사용해서...
...

[출력 형식]
{
  "overallFeedback": "지원자는 Spring Boot 경험이 풍부하나...",
  "keyStrengths": ["Kotlin 숙련도", "명확한 커뮤니케이션", "실무 경험 풍부"],
  "keyImprovements": ["분산 시스템 학습", "대규모 트래픽 처리 경험", "아키텍처 설계 역량"],
  "averageScore": 3.8,
  "recommendation": "보류 - 기술 역량은 우수하나, 요구사항인 Kafka 경험이 부족"
}
```

---

### 2.5 결과 페이지

**UI 요구사항**:
- **점수 요약 카드**:
  - 평균 점수 (큰 숫자로 표시, 예: 3.8 / 5.0)
  - 프로그레스 바 (파란색)
- **종합 피드백 카드**:
  - 전체 피드백 텍스트 (whitespace-pre-wrap)
- **강점 카드** (초록색 배경):
  - 강점 목록 (bullet points)
- **개선점 카드** (노란색 배경):
  - 개선점 목록 (bullet points)
- **대화 내역** (접기 가능):
  - 전체 메시지 히스토리
  - 각 답변에 대한 AI 평가 점수 표시
- **액션 버튼**:
  - "다시 연습하기" (새 면접 시작)
  - "리뷰 목록" (과거 연습 기록 보기)

**제약사항**:
- 모의 면접은 **1일 5회로 제한** (Rate Limiting)
- 세션당 최대 **30턴 대화** (AI 비용 제어)

---

## 🏗️ 도메인 모델

### MockInterview (모의 면접 세션)
```kotlin
@Entity
@Table(name = "mock_interviews")
class MockInterview(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val userId: Long,
    val jobPostingId: Long?,            // ✨ nullable (직무 기반 면접 시 null)
    @Enumerated(EnumType.STRING)
    val selectedJobField: JobField,     // ✨ 필수 (공고 또는 사용자 선택)
    val status: MockInterviewStatus = MockInterviewStatus.IN_PROGRESS,
    val startedAt: LocalDateTime = LocalDateTime.now(),
    var endedAt: LocalDateTime? = null,

    // 종합 평가 (종료 시 생성)
    var overallFeedback: String? = null,
    var keyStrengths: String? = null,      // JSON array
    var keyImprovements: String? = null,   // JSON array
    var averageScore: Double? = null,
    var recommendation: String? = null
)

enum class MockInterviewStatus {
    IN_PROGRESS, COMPLETED, ABORTED
}
```

### InterviewMessage (채팅 메시지)
```kotlin
@Entity
@Table(name = "interview_messages")
class InterviewMessage(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val mockInterviewId: Long,
    val sender: MessageSender,          // AI or USER
    val content: String,                // 메시지 내용 (TEXT)
    val messageIndex: Int,              // 순서 (0, 1, 2...)

    // AI 메시지 전용
    val aiReasoning: String? = null,

    // USER 메시지 전용 (평가)
    var logicScore: Int? = null,
    var specificityScore: Int? = null,
    var deliveryScore: Int? = null,
    var feedbackComment: String? = null,

    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class MessageSender {
    AI, USER
}
```

---

## 💾 데이터베이스 스키마

### V12__create_mock_interview_tables.sql
```sql
CREATE TABLE mock_interviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    job_posting_id BIGINT,                    -- ✨ nullable (NOT NULL 제거)
    selected_job_field VARCHAR(50) NOT NULL,  -- ✨ 추가 (필수)
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,
    overall_feedback TEXT,
    key_strengths TEXT,
    key_improvements TEXT,
    average_score DOUBLE PRECISION,
    recommendation TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (job_posting_id) REFERENCES job_postings(id)  -- nullable FK
);

CREATE TABLE interview_messages (
    id BIGSERIAL PRIMARY KEY,
    mock_interview_id BIGINT NOT NULL,
    sender VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    message_index INT NOT NULL,
    ai_reasoning TEXT,
    logic_score INT,
    specificity_score INT,
    delivery_score INT,
    feedback_comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (mock_interview_id) REFERENCES mock_interviews(id) ON DELETE CASCADE
);

CREATE INDEX idx_mock_interviews_user ON mock_interviews(user_id);
CREATE INDEX idx_mock_interviews_status ON mock_interviews(status);
CREATE INDEX idx_interview_messages_session ON interview_messages(mock_interview_id);
CREATE INDEX idx_interview_messages_order ON interview_messages(mock_interview_id, message_index);
```

---

## 🏗️ 기술 아키텍처

### 시스템 아키텍처
```
[사용자 브라우저]
    ↓ (HTTP + SSE)
[Spring Boot Application]
    ├─ MockInterviewController (채팅 면접 + SSE) ⭐ 핵심
    └─ ...
    ↓
[Service Layer]
    ├─ MockInterviewService (대화 관리, SSE 브로드캐스트) ⭐ 핵심
    ├─ InterviewAiService (AI 로직 조율)
    └─ ...
    ↓
[Support Services]
    ├─ OpenAiClientImpl (AI API 호출) - 기존 재사용
    ├─ PromptBuilder (직무 기반 / 공고 기반 프롬프트 분기) - 확장
    ├─ InterviewResponseParser (면접 응답 파싱) - 신규
    ├─ RateLimitService (IP/User ID 기반) - 확장
    └─ DuplicateRequestCache
    ↓
[Database (PostgreSQL)]
    ├─ mock_interviews (면접 세션)
    └─ interview_messages (채팅 메시지)
    ↓
[External API]
    └─ OpenAI gpt-4o-mini
```

### 의존성 다이어그램

```
MockInterviewController
    │
    ├── MockInterviewService
    │       ├── MockInterviewRepository
    │       ├── InterviewMessageRepository
    │       ├── InterviewAiService
    │       │       ├── AiClient (기존 인터페이스)
    │       │       ├── PromptBuilder (확장)
    │       │       └── InterviewResponseParser (신규)
    │       ├── RateLimitService (확장)
    │       └── SseEmitter 관리 (내부 ConcurrentHashMap)
    │
    ├── UserRepository (기존)
    └── JobPostingRepository (기존)
```

---

## 🔌 API 명세

### POST /mock-interviews/start
**설명**: 모의 면접 시작

**요청**:
```http
POST /mock-interviews/start
Content-Type: application/x-www-form-urlencoded

jobPostingId=123          (선택 사항, 공고 기반 면접 시)
selectedJobField=IT       (필수)
```

**응답** (redirect):
```http
HTTP/1.1 302 Found
Location: /mock-interviews/{id}/chat
```

---

### GET /mock-interviews/{id}/chat
**설명**: 채팅 UI 페이지

**응답** (HTML):
```html
<!-- 채팅 인터페이스 + JavaScript SSE 연결 -->
```

---

### POST /mock-interviews/{id}/messages
**설명**: 사용자 메시지 전송

**요청**:
```http
POST /mock-interviews/123/messages
Content-Type: application/x-www-form-urlencoded

content=안녕하세요, 저는 3년차 백엔드 개발자입니다...
```

**응답**:
```json
{
  "success": true
}
```

---

### GET /mock-interviews/{id}/stream (SSE 실시간 통신)
**설명**: SSE 스트림 (AI 응답 실시간 수신)

**응답** (Server-Sent Events):
```
event: message
data: {"id": 456, "sender": "AI", "content": "다음 질문...", "timestamp": "2026-04-27T10:30:00"}

event: message
data: {"id": 457, "sender": "USER", "content": "제 답변은...", "timestamp": "2026-04-27T10:31:00"}
```

---

### POST /mock-interviews/{id}/end
**설명**: 면접 종료 및 종합 평가 생성

**응답** (redirect):
```http
HTTP/1.1 302 Found
Location: /mock-interviews/{id}/result
```

---

### GET /mock-interviews/{id}/result
**설명**: 종합 평가 결과 페이지

**응답** (HTML):
```html
<!-- 점수 + 피드백 + 강점/개선점 + 대화 내역 -->
```

---

## 🎨 UI/UX 설계

### 직무 선택 모달 (새 기능)
```
┌─────────────────────────────────────┐
│ AI 면접 연습 시작                   │
├─────────────────────────────────────┤
│ 면접 직무를 선택해주세요:           │
│                                     │
│ [▼ IT개발          ]                │
│                                     │
│ 💡 채용 공고가 있다면 공고 페이지에서│
│    "이 공고로 면접 연습" 버튼을 이용│
│    하세요.                          │
│                                     │
│ [취소]          [면접 시작]         │
└─────────────────────────────────────┘
```

### 채팅 인터페이스 (핵심 화면)
```
┌─────────────────────────────────────┐
│ IT개발 면접 (또는: 백엔드 개발자-카카오)│
│ 시작: 10:25  [면접 종료] 버튼       │
├─────────────────────────────────────┤
│ [채팅 메시지 영역]                  │
│                                     │
│ ┌─ AI (10:25:10)                   │
│ │ 자기소개를 부탁드립니다.          │
│ └───────────────────────────────    │
│                                     │
│      ┌─ 나 (10:25:45) ───────┐     │
│      │ 안녕하세요, 저는...    │     │
│      └────────────────────────┘     │
│                                     │
│ ┌─ AI (10:26:00)                   │
│ │ Spring Boot를 사용한 경험은?     │
│ └───────────────────────────────    │
│                                     │
├─────────────────────────────────────┤
│ [답변 입력창 (3줄)]                 │
│                                     │
│ [전송] 버튼                         │
└─────────────────────────────────────┘
```

**JavaScript 핵심 기능**:
```javascript
// SSE 연결
const eventSource = new EventSource(`/mock-interviews/${id}/stream`);
eventSource.onmessage = (event) => {
    const message = JSON.parse(event.data);
    appendMessage(message);
};

// 메시지 전송
async function sendMessage(content) {
    await fetch(`/mock-interviews/${id}/messages`, {
        method: 'POST',
        body: new URLSearchParams({ content })
    });
}

// 자동 스크롤
function appendMessage(msg) {
    container.innerHTML += renderMessage(msg);
    container.scrollTop = container.scrollHeight;
}
```

---

## 💰 비용 분석

### AI API 비용 추정 (OpenAI gpt-4o-mini)

**단일 세션 비용**:
- 평균 10턴 대화 (질문-답변 5회 왕복)
- 턴당 800 토큰 × 10턴 = 8,000 토큰 ≈ $0.012
- 종합 평가 1회: 1,500 토큰 ≈ $0.002
- **총**: **$0.014/세션**

**월간 비용** (100명 사용자):
- 사용자당 5회/월 × $0.014 = $0.07
- 100명 × $0.07 = **$7/월**

**절감 전략**:
- Rate Limiting: 1일 5회 제한
- 최대 30턴 대화 제한
- 질문 200자 제한 (토큰 절감)

---

## ⚠️ 리스크 및 대응 방안

### 1. SSE 연결 끊김
**리스크**: 네트워크 불안정 시 메시지 손실

**대응**:
- 재연결 로직 (EventSource 자동 재연결)
- 메시지 DB 저장 (복구 가능)
- 타임아웃 설정 (30분)

---

### 2. AI 응답 지연
**리스크**: 채팅 중 AI 응답이 5초 이상 걸리면 사용자 이탈

**대응**:
- 로딩 인디케이터 ("AI가 답변 생성 중...")
- 타임아웃 10초 설정 (초과 시 재시도)

---

### 3. 동시 세션 과부하
**리스크**: 100명 동시 채팅 시 서버 부하

**대응**:
- SseEmitter 메모리 관리 (30분 타임아웃)
- 최대 동시 세션 제한 (향후)

---

## 🔧 기술적 결정 사항 (2026-04-30 검토 결과)

### 1. 마이그레이션 버전 수정
- **문제**: PRD 초안에서 V11 명시, 하지만 현재 V11까지 이미 사용됨 (Phase 6E)
- **수정**: `V12__create_mock_interview_tables.sql`로 변경

### 2. 기존 코드 재사용 결정

| 항목 | 결정 | 이유 |
|------|------|------|
| PromptBuilder | **기존 확장** | 17개 직무별 프롬프트 이미 구현됨, 메서드 추가로 대응 |
| ResponseParser | **별도 생성** | 응답 구조가 다름 (evaluation + nextAction) |
| ConversationManager | **서비스 내부** | 복잡도 낮음, 별도 클래스 불필요 |
| AiClient | **그대로 사용** | 인터페이스 기반 설계로 교체 가능 |
| RateLimitService | **확장** | 기존 IP 기반 + User ID 기반 추가 |

### 3. 기술적 결정

| 항목 | 결정 | 이유 |
|------|------|------|
| 첫 질문 생성 | **동기** | 채팅 화면 진입 전 완료 필요, UX 명확성 |
| AI 응답 생성 | **비동기 (@Async)** | 사용자 대기 시간 단축 |
| SSE 타임아웃 | **30분** | 일반적인 면접 시간 고려 |
| 에러 복구 | **DB 기반** | 메시지 모두 DB 저장, 재연결 시 복구 |
| SSE 테스트 | **WebTestClient** | MockMvc는 SSE 지원 제한적 |

### 4. 수정할 기존 파일 목록

1. `service/ai/PromptBuilder.kt` - 면접용 메서드 5개 추가
2. `service/ratelimit/RateLimitService.kt` - 모의 면접 Rate Limit 추가
3. `exception/GlobalExceptionHandler.kt` - MockInterviewException 처리
4. `config/SecurityConfig.kt` - SSE 엔드포인트 설정
5. `templates/home.html` - AI 면접 연습 버튼

---

## 📅 구현 일정 (상세 계획)

### Phase 7A: 모의 면접 세션 (Week 1 - 5-7일)

**목표**: 면접 세션 데이터 모델 구축 및 기본 서비스 로직 구현

#### 1. MockInterview 엔티티
- **파일**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/domain/MockInterview.kt`
- **필드**:
  - id, userId, jobPostingId (nullable), selectedJobField (필수)
  - status (IN_PROGRESS, COMPLETED, ABORTED)
  - startedAt, endedAt (nullable)
  - overallFeedback, keyStrengths, keyImprovements, averageScore, recommendation (종합 평가, nullable)
- **메서드**: complete(), abort(), updateEvaluation()
- **패턴**: Regular class, custom equals/hashCode

#### 2. InterviewMessage 엔티티
- **파일**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/domain/InterviewMessage.kt`
- **필드**:
  - id, mockInterviewId, sender (AI/USER), content, messageIndex
  - aiReasoning (AI 전용)
  - logicScore, specificityScore, deliveryScore, feedbackComment (USER 전용)
  - createdAt
- **제약**: messageIndex 순서 보장, sender enum (AI, USER)

#### 3. V12 마이그레이션
- **파일**: `src/main/resources/db/migration/V12__create_mock_interview_tables.sql`
- **내용**:
  - CREATE TABLE mock_interviews (jobPostingId nullable, selectedJobField NOT NULL)
  - CREATE TABLE interview_messages (FK to mock_interviews ON DELETE CASCADE)
  - CREATE INDEX (user_id, status, mock_interview_id, message_index)

#### 4. Repositories
- **MockInterviewRepository**:
  - findByUserIdAndStatusOrderByStartedAtDesc(userId, status)
  - findByIdAndUserId(id, userId) - 소유권 검증용
- **InterviewMessageRepository**:
  - findByMockInterviewIdOrderByMessageIndexAsc(mockInterviewId)
  - countByMockInterviewIdAndSender(mockInterviewId, sender) - 턴 수 계산

#### 5. MockInterviewService (기본 로직)
- **파일**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/service/MockInterviewService.kt`
- **메서드**:
  - startInterview(userId, jobPostingId?, selectedJobField): MockInterview
  - sendUserMessage(interviewId, content): InterviewMessage
  - endInterview(interviewId): MockInterview (종합 평가 생성)
  - getInterviewHistory(interviewId): List<InterviewMessage>
- **검증**: 최대 30턴 대화 제한, 세션 상태 검증

#### 6. DTOs & 예외
- **DTOs**:
  - StartInterviewRequest (jobPostingId?, selectedJobField)
  - SendMessageRequest (content)
  - InterviewMessageDto (id, sender, content, timestamp, scores?)
  - MockInterviewViewModel (id, status, startedAt, messageCount, averageScore?)
- **예외**:
  - MockInterviewException (sealed)
  - MockInterviewNotFoundException(id)
  - MockInterviewAccessDeniedException(interviewId, userId)
  - MaxTurnExceededException(interviewId, maxTurns = 30)

**완료 기준**:
- [ ] MockInterview 엔티티 생성 (모든 필드 + equals/hashCode)
- [ ] InterviewMessage 엔티티 생성
- [ ] V12 마이그레이션 실행 성공
- [ ] Repositories (custom queries)
- [ ] MockInterviewService 기본 CRUD 동작
- [ ] DTOs 검증
- [ ] 예외 처리 추가 (GlobalExceptionHandler)
- [ ] 단위 테스트: 세션 생성, 메시지 저장, 상태 전환

---

### Phase 7B: 채팅 AI 로직 (Week 2-3 - 8-10일)

**목표**: 직무/공고 기반 AI 프롬프트, 꼬리 질문 생성, 종합 평가 로직 구현

#### 1. PromptBuilder 확장 (기존 파일 수정)
- **파일**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/service/ai/PromptBuilder.kt`
- **추가 메서드** (기존 17개 직무 프롬프트 재활용):
  - `buildInterviewSystemPrompt(jobField: JobField): String` - 직무 기반 면접 프롬프트
  - `buildInterviewSystemPromptWithJobPosting(jobField, jobPosting): String` - 공고 기반 면접 프롬프트
  - `buildFirstQuestionPrompt(jobField, jobPosting?): String` - 첫 질문 생성
  - `buildFollowUpPrompt(conversation: List<InterviewMessage>): String` - 꼬리 질문
  - `buildFinalEvaluationPrompt(interview, conversation, jobPosting?): String` - 종합 평가
- **장점**: 기존 `getLogicDescription()`, `getSpecificityDescription()` 등 17개 직무별 헬퍼 메서드 재활용

**시스템 프롬프트 분기**:
```kotlin
// 공고 기반
if (jobPosting != null) {
    """
    당신은 ${jobPosting.companyName}의 ${jobPosting.jobTitle} 면접관입니다.
    필수 기술: ${jobPosting.requiredSkills}
    우대 기술: ${jobPosting.preferredSkills}
    공고 요구사항을 중점적으로 질문하세요.
    """
} else {
    // 직무 기반
    """
    당신은 ${jobField.displayName} 분야의 면접관입니다.
    일반적인 ${jobField.displayName} 역량을 평가하는 질문을 진행하세요.
    """
}
```

**출력 형식**: JSON (evaluation + nextAction)
- evaluation: {logicScore, specificityScore, deliveryScore, comment}
- nextAction: {question (200자 이내), reasoning, isFollowUp, questionLength}

#### 2. InterviewAiService
- **파일**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/service/InterviewAiService.kt`
- **메서드**:
  - generateFirstQuestion(interview): InterviewMessage - 첫 질문 생성
  - generateFollowUpQuestion(interview, conversation): InterviewMessage - 꼬리 질문
  - evaluateAnswer(interview, userMessage, conversation): EvaluationResult
  - generateFinalEvaluation(interview, conversation): FinalEvaluationResult
- **흐름**: PromptBuilder → AiClient → ResponseParser → 엔티티 생성
- **Micrometer**: 메트릭 추가 (interview.ai.calls, interview.ai.duration)

#### 3. InterviewResponseParser
- **파일**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/service/ai/InterviewResponseParser.kt`
- **메서드**:
  - parseInterviewResponse(rawResponse): ParsedInterviewResponse
  - parseFinalEvaluation(rawResponse): ParsedFinalEvaluation
- **검증**:
  - question 길이 <= 200자 (초과 시 경고 + 절단)
  - scores 1-5 범위
  - 필수 필드 존재 (evaluation, nextAction)
- **DTOs**:
  - ParsedInterviewResponse (evaluation, nextAction)
  - EvaluationDto (logicScore, specificityScore, deliveryScore, comment)
  - NextActionDto (question, reasoning, isFollowUp, questionLength)
  - ParsedFinalEvaluation (overallFeedback, keyStrengths, keyImprovements, averageScore, recommendation)

#### 4. 대화 흐름 관리
- **ConversationManager** (MockInterviewService 내부):
  - buildConversationContext(messages): String - AI에 전달할 대화 히스토리
  - shouldAskFollowUp(evaluation): Boolean - 평가 기반 판단
  - hasReachedMaxTurns(interview): Boolean - 30턴 체크
  - formatMessageForAi(message): String - AI용 메시지 포맷

#### 5. Rate Limiting (면접 시작)
- **RateLimitService**: checkMockInterviewLimit(userId) 추가
  - 키: "mock_interview:{userId}"
  - 제한: 5회/24시간
  - RateLimitExceededException 발생

**완료 기준**:
- [ ] PromptBuilder에 면접용 메서드 5개 추가 (기존 파일 확장)
- [ ] InterviewAiService (AI 통합)
- [ ] InterviewResponseParser (JSON 검증, 200자 제한)
- [ ] 대화 흐름 관리 (MockInterviewService 내부)
- [ ] RateLimitService에 모의 면접 제한 추가 (5회/일)
- [ ] Micrometer 메트릭 (interview.ai.calls, interview.ai.duration)
- [ ] 단위 테스트: 프롬프트 생성, AI 응답 파싱, 꼬리 질문 로직
- [ ] 통합 테스트: 첫 질문 → 답변 → 꼬리 질문 → 종합 평가

---

### Phase 7C: 실시간 통신 (SSE) (Week 4-5 - 8-10일)

**목표**: Server-Sent Events 구현 및 실시간 메시지 브로드캐스트

#### 1. MockInterviewController (SSE 엔드포인트)
- **파일**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/controller/MockInterviewController.kt`
- **엔드포인트**:
  - POST /mock-interviews/start - 면접 시작 (직무 선택 모달에서 호출)
  - GET /mock-interviews/{id}/chat - 채팅 UI 페이지
  - POST /mock-interviews/{id}/messages - 사용자 메시지 전송
  - **GET /mock-interviews/{id}/stream (SSE)** - 실시간 메시지 수신 ⭐ 핵심
  - POST /mock-interviews/{id}/end - 면접 종료 + 종합 평가
  - GET /mock-interviews/{id}/result - 결과 페이지

**SSE 엔드포인트 구현**:
```kotlin
@GetMapping("/{id}/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
fun streamMessages(
    @PathVariable id: Long,
    @AuthenticationPrincipal userDetails: UserDetails
): SseEmitter {
    val emitter = SseEmitter(30 * 60 * 1000L) // 30분 타임아웃
    mockInterviewService.registerEmitter(id, emitter)

    emitter.onCompletion { mockInterviewService.removeEmitter(id) }
    emitter.onTimeout { mockInterviewService.removeEmitter(id) }
    emitter.onError { mockInterviewService.removeEmitter(id) }

    return emitter
}
```

#### 2. SseEmitter 관리
- **MockInterviewService에 추가**:
  - private val emitters: ConcurrentHashMap<Long, SseEmitter> = ConcurrentHashMap()
  - registerEmitter(interviewId, emitter)
  - removeEmitter(interviewId)
  - broadcastMessage(interviewId, message): Boolean - 메시지 전송
  - sendEventToEmitter(emitter, message) - SseEmitter.send() 래퍼

**메시지 브로드캐스트 흐름**:
1. 사용자 메시지 POST → DB 저장 → SSE broadcast (USER 메시지)
2. AI 응답 생성 → DB 저장 → SSE broadcast (AI 메시지)
3. 클라이언트 EventSource가 실시간 수신

#### 3. AsyncConfig 설정 (새로 생성)
- **파일**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/config/AsyncConfig.kt`
- **내용**:
```kotlin
@Configuration
@EnableAsync
class AsyncConfig {
    @Bean(name = ["taskExecutor"])
    fun taskExecutor(): ThreadPoolTaskExecutor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 10
            maxPoolSize = 50
            queueCapacity = 100
            setThreadNamePrefix("async-")
            initialize()
        }
    }
}
```
- **목적**: @Async 어노테이션 활성화 및 스레드 풀 설정
- **스레드 풀 전략**:
  - core: 10 (기본 유지 스레드)
  - max: 50 (최대 동시 AI 응답 생성)
  - queue: 100 (대기 큐)

#### 4. 비동기 AI 응답 생성
- **@Async 처리**:
  - sendUserMessage() → 사용자 메시지 저장 후 즉시 반환
  - 백그라운드: AI 응답 생성 → 저장 → SSE broadcast
  - TaskExecutor 설정 (AsyncConfig)

```kotlin
@Async
fun generateAndBroadcastAiResponse(interview: MockInterview, conversation: List<InterviewMessage>) {
    val aiMessage = interviewAiService.generateFollowUpQuestion(interview, conversation)
    interviewMessageRepository.save(aiMessage)
    broadcastMessage(interview.id, aiMessage)
}
```

#### 5. 에러 처리 (SSE)
- **연결 끊김**: onError 핸들러에서 로깅, emitter 제거
- **타임아웃**: 30분 후 자동 종료, 재연결 안내
- **재연결 로직**: EventSource 자동 재연결 + 마지막 메시지 ID 기반 복구

#### 6. 보안 검증
- **소유권 검증**: 모든 엔드포인트에서 userId 확인
- **세션 상태 검증**: IN_PROGRESS 상태에서만 메시지 전송 가능
- **CSRF**: POST 요청 CSRF 토큰 검증

**완료 기준**:
- [ ] AsyncConfig 설정 완료 (ThreadPoolTaskExecutor)
- [ ] MockInterviewController (6개 엔드포인트)
- [ ] SSE 엔드포인트 구현 (GET /stream)
- [ ] SseEmitter 관리 (register, remove, broadcast)
- [ ] 비동기 AI 응답 생성 (@Async)
- [ ] 에러 처리 (연결 끊김, 타임아웃, 재연결)
- [ ] 보안 검증 (소유권, 세션 상태, CSRF)
- [ ] 단위 테스트: SSE 전송, emitter 관리
- [ ] 통합 테스트: SSE 연결 → 메시지 수신 → 재연결

---

### Phase 7D: 채팅 UI (Week 6 - 6-8일)

**목표**: 채팅 인터페이스 및 직무 선택 모달 구현

#### 1. 직무 선택 모달
- **파일**: `src/main/resources/templates/fragments/job-field-modal.html`
- **구조**:
  - Tailwind modal (fixed overlay, centered dialog)
  - 17개 JobField 드롭다운
  - "AI 면접 연습 시작" 버튼 → POST /mock-interviews/start
  - 채용 공고 안내: "채용 공고가 있다면 공고 페이지에서 시작하세요"
- **JavaScript**:
  - openJobFieldModal(), closeJobFieldModal()
  - submitJobFieldForm() - AJAX POST

#### 2. 채팅 화면
- **파일**: `src/main/resources/templates/mock-interviews/chat.html`
- **구조**:
  - **헤더**: 직무명 (또는 "회사명 - 포지션"), 시작 시간, [면접 종료] 버튼
  - **메시지 영역**:
    - 높이 400px, 스크롤 가능, auto-scroll to bottom
    - AI 메시지: 왼쪽, 파란색 배경
    - USER 메시지: 오른쪽, 초록색 배경
    - 타임스탬프 표시
  - **입력 폼**:
    - textarea (3줄, maxlength=200)
    - [전송] 버튼
    - Enter 전송, Shift+Enter 줄바꿈

**JavaScript SSE 연결** (핵심):
```javascript
const interviewId = /* from Thymeleaf */;
const eventSource = new EventSource(`/mock-interviews/${interviewId}/stream`);

eventSource.onmessage = (event) => {
    const message = JSON.parse(event.data);
    appendMessage(message);
    scrollToBottom();
};

eventSource.onerror = (error) => {
    console.error('SSE connection error:', error);
    showReconnectingMessage();
};

// 메시지 전송
async function sendMessage(content) {
    const response = await fetch(`/mock-interviews/${interviewId}/messages`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({ content })
    });

    if (response.ok) {
        clearInput();
        showLoadingIndicator('AI가 답변 생성 중...');
    }
}

// DOM 업데이트
function appendMessage(msg) {
    const msgDiv = createMessageElement(msg);
    messagesContainer.appendChild(msgDiv);
}

function scrollToBottom() {
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
}
```

#### 3. 종합 평가 결과 페이지
- **파일**: `src/main/resources/templates/mock-interviews/result.html`
- **구조**:
  - **점수 카드**: 평균 점수 (큰 숫자), 프로그레스 바
  - **종합 피드백 카드**: overallFeedback (whitespace-pre-wrap)
  - **강점 카드** (초록색): keyStrengths (bullet list)
  - **개선점 카드** (노란색): keyImprovements (bullet list)
  - **채용 추천도**: recommendation
  - **대화 내역** (접기 가능):
    - details 태그
    - 모든 메시지 표시 + USER 메시지의 평가 점수
  - **액션 버튼**:
    - [다시 연습하기] → /mock-interviews/start (모달 열기)
    - [리뷰 목록] → /reviews

#### 4. 홈페이지 통합
- **파일**: `src/main/resources/templates/index.html` 또는 `home.html`
- **추가 버튼**: "AI 면접 연습" (상단 CTA)
  - 클릭 → 직무 선택 모달 open
  - 직무 선택 → POST /mock-interviews/start → 채팅 화면

#### 5. 로딩 & 에러 UI
- **로딩 인디케이터**: "AI가 답변 생성 중..." (spinner)
- **재연결 메시지**: "연결이 끊어졌습니다. 재연결 중..." (노란색 배너)
- **에러 메시지**: "면접을 불러올 수 없습니다" (빨간색 배너)

**완료 기준**:
- [ ] 직무 선택 모달 (job-field-modal.html)
- [ ] 채팅 화면 (chat.html)
- [ ] JavaScript SSE 연결 (EventSource)
- [ ] 메시지 전송/수신 (AJAX POST, SSE onmessage)
- [ ] 자동 스크롤 (scrollToBottom)
- [ ] 종합 평가 페이지 (result.html)
- [ ] 홈페이지 통합 (AI 면접 연습 버튼)
- [ ] 로딩/에러 UI (인디케이터, 재연결, 에러 배너)
- [ ] Responsive 디자인, Dark mode 지원
- [ ] 수동 테스트: 면접 시작 → 대화 → 종료 → 결과 확인

---

### Phase 7E: 테스트 및 최적화 (Week 7 - 7-9일)

**목표**: 종합 테스트, SSE 부하 테스트, 성능 최적화, 문서화

#### 1. 통합 테스트
- **파일**: `src/test/kotlin/.../Phase7IntegrationTest.kt`
- **시나리오**:
  - 직무 기반 면접 시작 (jobPostingId = null)
  - 공고 기반 면접 시작 (jobPostingId = 123)
  - 사용자 메시지 전송 → AI 응답 수신
  - 꼬리 질문 생성 (평가 기반)
  - 30턴 제한 검증
  - 면접 종료 → 종합 평가 생성
  - Rate limit 검증 (5회/일)
  - 소유권 검증 (타 사용자 접근 차단)

**MockMvc + TestRestTemplate**:
- SSE 연결 테스트는 어렵지만, 메시지 저장 및 브로드캐스트 로직 테스트
- WebTestClient (Spring WebFlux) 사용 고려 (SSE 테스트)

#### 2. SSE 부하 테스트
- **도구**: Apache JMeter 또는 Gatling
- **시나리오**: 100명 동시 SSE 연결
  - 100개 EventSource 연결 유지
  - 각각 10개 메시지 전송/수신
  - 연결 안정성 > 95% (끊김률 < 5%)
  - 메모리 사용량 모니터링 (SseEmitter 누수 확인)

**성능 목표**:
- AI 응답 시간: < 5초 (OpenAI 호출 포함)
- SSE 메시지 전송 지연: < 500ms
- 동시 연결: 100개 이상 안정적 처리
- 메모리: SseEmitter당 < 1MB

#### 3. 단위 테스트
- **MockInterviewPromptBuilderTest**:
  - 직무 기반 프롬프트 생성
  - 공고 기반 프롬프트 생성
  - 질문 길이 200자 제한
- **InterviewAiServiceTest**:
  - 첫 질문 생성
  - 꼬리 질문 생성
  - 종합 평가 생성
  - AI 오류 시 fallback
- **SseEmitterManagementTest**:
  - emitter 등록/제거
  - 메시지 브로드캐스트
  - 타임아웃 처리

#### 4. 성능 최적화
- **DB 쿼리 최적화**:
  - InterviewMessage 조회 시 인덱스 활용 (mock_interview_id, message_index)
  - N+1 방지 (Eager loading 또는 batch fetch)
- **캐싱**:
  - 면접 세션 정보 캐싱 (Caffeine, 5분 TTL)
  - 대화 히스토리 캐싱 (빈번한 조회 방지)
- **비동기 처리**:
  - AI 응답 생성 @Async (사용자 대기 시간 단축)
  - ThreadPoolTaskExecutor 설정 (core: 10, max: 50)

#### 5. 문서화
- **PHASE7_COMPLETION_REPORT.md**:
  1. Overview (실시간 채팅 면접 시스템)
  2. Features (SSE, 꼬리 질문, 종합 평가, 직무/공고 분기)
  3. Architecture (SSE 흐름도, 비동기 처리)
  4. API endpoints (6개)
  5. Database schema (V11 migration)
  6. SSE 구현 상세 (EventSource, SseEmitter)
  7. Performance metrics (응답 시간, 동시 연결)
  8. Test coverage (목표: 85%+)
  9. Known limitations (SSE 브라우저 제한, 재연결 지연)
  10. Future improvements (WebSocket 고려, AI 스트리밍)

- **README.md 업데이트**:
```markdown
### Phase 7: 실시간 AI 채팅 면접

AI 면접관과 실시간 대화 형태의 모의 면접을 진행합니다.

**Features**:
- 직무 기반 또는 채용 공고 기반 면접
- Server-Sent Events (SSE) 실시간 통신
- AI 꼬리 질문 생성 (답변 평가 기반)
- 종합 평가 (평균 점수, 강점/개선점, 채용 추천도)
- 5회/일 Rate Limiting
- 최대 30턴 대화
```

- **CHANGELOG.md 업데이트**:
```markdown
## [0.7.0] - 2026-06-XX

### Added
- 실시간 AI 채팅 면접 (Phase 7)
- Server-Sent Events (SSE) 통신
- 직무 기반 / 채용 공고 기반 면접 모드
- AI 꼬리 질문 생성
- 종합 평가 (평균 점수, 강점, 개선점, 추천도)
- MockInterview, InterviewMessage 엔티티
- 직무 선택 모달 UI
```

**완료 기준**:
- [ ] 85%+ 테스트 커버리지 (Phase 7 코드)
- [ ] 통합 테스트: 전체 플로우 (시작 → 대화 → 종료 → 평가)
- [ ] SSE 부하 테스트: 100명 동시 연결, 안정성 > 95%
- [ ] Rate Limiting 검증 (5회/일)
- [ ] 성능: AI 응답 < 5초, SSE 지연 < 500ms
- [ ] 문서화: README, CHANGELOG, PHASE7_COMPLETION_REPORT.md
- [ ] API 레퍼런스 업데이트
- [ ] SSE 연결 재연결 로직 검증

---

**총 소요 기간**: 약 6주 (40-50일)

---

## 📊 성공 지표 (KPI)

### Phase 7: 채팅 면접
- [ ] SSE 연결 안정성: **> 95%** (끊김률 < 5%)
- [ ] 평균 세션 길이: **8-12턴** (대화)
- [ ] 사용자 만족도: **평균 4.0/5점** (설문)
- [ ] AI 응답 시간: **< 5초**
- [ ] 직무 기반 면접 비율: **> 40%** (공고 없이도 활용)

---

## 🚀 다음 단계 (Phase 8+)

### Phase 8: 채팅 면접 고도화
- 음성 녹음 및 STT 연동
- 실시간 AI 스트리밍 (토큰 단위 응답)
- 다중 언어 지원 (영어 면접)
- 면접 리플레이 기능 (과거 면접 재생)

---

**작성자**: Claude Code
**승인일**: 2026-04-27
**핵심 기능**: 🎯 **실시간 AI 채팅 면접** (직무 기반 또는 채용 공고 기반)
