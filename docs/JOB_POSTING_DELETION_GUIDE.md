# 채용 공고 및 생성된 질문 삭제 가이드

**작성일**: 2026-06-21
**목적**: DB에서 수작업으로 채용 공고(JobPosting)와 해당 공고로 생성된 질문 10개(GeneratedQuestion) 삭제

---

## 1. 현재 프로젝트 구조 요약

### 1.1 엔티티 관계도

```
User (users)
  ↓ (FK: user_id)
JobPosting (job_postings)
  ├─ isActive: BOOLEAN (soft delete 플래그, 사용 안됨)
  ├─ effectiveJobField: selectedJobField || inferredJobField
  │
  └─→ GeneratedQuestion (generated_questions)
      ├─ FK: job_posting_id → job_postings(id) [ON DELETE CASCADE]
      ├─ orderIndex: 1-10
      ├─ difficulty: EASY/MEDIUM/HARD
      │
      └─→ InterviewAnswer (interview_answers)
          ├─ FK: generated_question_id → generated_questions(id) [NO CASCADE]
          ├─ userId: FK to users(id)
          │
          └─→ AiFeedback (ai_feedbacks)
              └─ interviewAnswerId: FK (no cascade)
```

### 1.2 주요 테이블 구조

#### `job_postings`
```sql
CREATE TABLE job_postings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    original_url TEXT,
    company_name VARCHAR(200),
    job_title VARCHAR(200),
    job_description TEXT,
    selected_job_field VARCHAR(50),
    inferred_job_field VARCHAR(50),
    required_skills TEXT,
    preferred_skills TEXT,
    is_active BOOLEAN DEFAULT TRUE,  -- Soft delete 플래그
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### `generated_questions`
```sql
CREATE TABLE generated_questions (
    id BIGSERIAL PRIMARY KEY,
    job_posting_id BIGINT NOT NULL REFERENCES job_postings(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    category VARCHAR(100),
    difficulty VARCHAR(20) CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    ai_reasoning TEXT,
    order_index INT NOT NULL CHECK (order_index BETWEEN 1 AND 10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### `interview_answers` (관련 부분만)
```sql
ALTER TABLE interview_answers
ADD COLUMN generated_question_id BIGINT;

ADD CONSTRAINT fk_interview_answers_generated_question
FOREIGN KEY (generated_question_id) REFERENCES generated_questions(id);
-- 주의: NO CASCADE DELETE!
```

### 1.3 Cascade 동작 요약

| 삭제 대상 | 자동 삭제됨 | 수동 처리 필요 |
|----------|-----------|-------------|
| `job_postings` 행 삭제 | `generated_questions` (ON DELETE CASCADE) | `interview_answers` (orphaned 방지) |
| `generated_questions` 행 삭제 | 없음 | `interview_answers` (orphaned 방지) |

**중요**: `job_postings`를 삭제하면 `generated_questions`는 자동 삭제되지만, `interview_answers`는 orphaned 상태가 됩니다!

---

## 2. 삭제 전 확인사항

### 2.1 삭제할 채용 공고 확인

```sql
-- 현재 등록된 모든 채용 공고 조회
SELECT
    jp.id,
    jp.company_name,
    jp.job_title,
    jp.selected_job_field,
    jp.inferred_job_field,
    jp.is_active,
    jp.created_at,
    COUNT(gq.id) AS question_count,
    u.email AS owner_email
FROM job_postings jp
LEFT JOIN generated_questions gq ON jp.id = gq.job_posting_id
LEFT JOIN users u ON jp.user_id = u.id
GROUP BY jp.id, u.email
ORDER BY jp.created_at DESC;
```

### 2.2 해당 공고의 답변 이력 확인

```sql
-- 특정 채용 공고(ID=1)에 대한 답변 이력 확인
SELECT
    ia.id AS answer_id,
    ia.user_id,
    u.email,
    gq.content AS question_content,
    ia.answer_text,
    ia.created_at
FROM interview_answers ia
JOIN generated_questions gq ON ia.generated_question_id = gq.id
JOIN users u ON ia.user_id = u.id
WHERE gq.job_posting_id = 1
ORDER BY ia.created_at DESC;
```

### 2.3 답변에 연결된 AI 피드백 확인

```sql
-- 특정 채용 공고(ID=1)의 답변에 대한 AI 피드백 확인
SELECT
    af.id AS feedback_id,
    af.interview_answer_id,
    af.logic_score,
    af.specificity_score,
    af.job_fit_score,
    af.delivery_score,
    af.average_score,
    af.created_at
FROM ai_feedbacks af
JOIN interview_answers ia ON af.interview_answer_id = ia.id
JOIN generated_questions gq ON ia.generated_question_id = gq.id
WHERE gq.job_posting_id = 1;
```

---

## 3. 삭제 방법

### 방법 1: Hard Delete (완전 삭제) - 권장

**적용 시나리오**: 테스트 데이터나 실수로 등록한 공고를 완전히 제거하고 싶을 때

#### Step 1: 트랜잭션 시작 (안전 장치)

```sql
BEGIN;
```

#### Step 2: 연관된 AI 피드백 삭제 (선택적)

```sql
-- 특정 채용 공고(ID=1)의 답변에 대한 AI 피드백 삭제
DELETE FROM ai_feedbacks
WHERE interview_answer_id IN (
    SELECT ia.id
    FROM interview_answers ia
    JOIN generated_questions gq ON ia.generated_question_id = gq.id
    WHERE gq.job_posting_id = 1
);
```

#### Step 3: 연관된 사용자 답변 삭제

```sql
-- 특정 채용 공고(ID=1)로 생성된 질문에 대한 답변 삭제
DELETE FROM interview_answers
WHERE generated_question_id IN (
    SELECT id FROM generated_questions WHERE job_posting_id = 1
);
```

#### Step 4: 생성된 질문 삭제 (선택적 - CASCADE로 자동 삭제됨)

```sql
-- 명시적으로 삭제하고 싶다면 (안 해도 다음 단계에서 CASCADE 됨)
DELETE FROM generated_questions WHERE job_posting_id = 1;
```

#### Step 5: 채용 공고 삭제

```sql
-- 채용 공고 삭제 (generated_questions는 자동 CASCADE 삭제)
DELETE FROM job_postings WHERE id = 1;
```

#### Step 6: 확인 및 커밋

```sql
-- 삭제 결과 확인
SELECT COUNT(*) FROM job_postings WHERE id = 1;  -- 0이어야 함
SELECT COUNT(*) FROM generated_questions WHERE job_posting_id = 1;  -- 0이어야 함
SELECT COUNT(*) FROM interview_answers WHERE generated_question_id IN (
    SELECT id FROM generated_questions WHERE job_posting_id = 1
);  -- 0이어야 함

-- 문제 없으면 커밋
COMMIT;

-- 문제 있으면 롤백
-- ROLLBACK;
```

### 방법 2: Soft Delete (논리 삭제) - 데이터 보존

**적용 시나리오**: 실제 서비스에서 삭제 기록을 남기고 복구 가능하게 하고 싶을 때

#### Step 1: is_active 플래그 비활성화

```sql
-- 채용 공고를 비활성화 (삭제한 것처럼 보이게)
UPDATE job_postings SET is_active = FALSE WHERE id = 1;
```

#### Step 2: 확인

```sql
-- 활성 공고 조회 (비활성화된 공고는 안 보임)
SELECT * FROM job_postings WHERE is_active = TRUE;

-- 비활성 공고 조회
SELECT * FROM job_postings WHERE is_active = FALSE;
```

**장점**:
- 데이터 복구 가능 (`UPDATE job_postings SET is_active = TRUE WHERE id = 1`)
- 답변 이력 보존 (사용자가 작성한 답변 유지)
- 삭제 기록 추적 가능

**단점**:
- DB 용량 계속 차지
- 질문 목록에서는 여전히 조회 가능 (UI에서 필터링 필요)

---

## 4. 안전한 삭제를 위한 One-Liner SQL

### 4.1 전체 삭제 (트랜잭션 포함)

```sql
-- 특정 채용 공고(ID=1) 및 관련 데이터 완전 삭제
BEGIN;

-- AI 피드백 삭제
DELETE FROM ai_feedbacks
WHERE interview_answer_id IN (
    SELECT ia.id FROM interview_answers ia
    JOIN generated_questions gq ON ia.generated_question_id = gq.id
    WHERE gq.job_posting_id = 1
);

-- 사용자 답변 삭제
DELETE FROM interview_answers
WHERE generated_question_id IN (
    SELECT id FROM generated_questions WHERE job_posting_id = 1
);

-- 채용 공고 삭제 (생성된 질문은 CASCADE 삭제)
DELETE FROM job_postings WHERE id = 1;

COMMIT;
```

### 4.2 특정 사용자의 모든 채용 공고 삭제

```sql
-- 특정 사용자(user_id=10)의 모든 채용 공고 삭제
BEGIN;

-- AI 피드백 삭제
DELETE FROM ai_feedbacks
WHERE interview_answer_id IN (
    SELECT ia.id FROM interview_answers ia
    JOIN generated_questions gq ON ia.generated_question_id = gq.id
    JOIN job_postings jp ON gq.job_posting_id = jp.id
    WHERE jp.user_id = 10
);

-- 사용자 답변 삭제
DELETE FROM interview_answers
WHERE generated_question_id IN (
    SELECT gq.id FROM generated_questions gq
    JOIN job_postings jp ON gq.job_posting_id = jp.id
    WHERE jp.user_id = 10
);

-- 채용 공고 삭제
DELETE FROM job_postings WHERE user_id = 10;

COMMIT;
```

### 4.3 모든 채용 공고 삭제 (테스트 환경 초기화)

```sql
-- 모든 채용 공고 및 관련 데이터 삭제 (위험!)
BEGIN;

-- AI 피드백 삭제
DELETE FROM ai_feedbacks
WHERE interview_answer_id IN (
    SELECT ia.id FROM interview_answers ia
    WHERE ia.generated_question_id IS NOT NULL
);

-- 사용자 답변 삭제
DELETE FROM interview_answers WHERE generated_question_id IS NOT NULL;

-- 모든 채용 공고 삭제
DELETE FROM job_postings;

COMMIT;
```

---

## 5. 삭제 후 검증 쿼리

### 5.1 Orphaned 레코드 확인

```sql
-- 채용 공고 없이 남은 생성 질문 확인 (0이어야 함)
SELECT gq.* FROM generated_questions gq
LEFT JOIN job_postings jp ON gq.job_posting_id = jp.id
WHERE jp.id IS NULL;

-- 생성 질문 없이 남은 답변 확인 (0이어야 함)
SELECT ia.* FROM interview_answers ia
WHERE ia.generated_question_id IS NOT NULL
AND ia.generated_question_id NOT IN (SELECT id FROM generated_questions);

-- AI 피드백 없이 남은 답변 확인 (정상적일 수 있음)
SELECT ia.* FROM interview_answers ia
LEFT JOIN ai_feedbacks af ON ia.id = af.interview_answer_id
WHERE ia.generated_question_id IS NOT NULL AND af.id IS NULL;
```

### 5.2 테이블 레코드 수 확인

```sql
-- 전체 레코드 수 확인
SELECT
    'job_postings' AS table_name, COUNT(*) AS count FROM job_postings
UNION ALL
SELECT 'generated_questions', COUNT(*) FROM generated_questions
UNION ALL
SELECT 'interview_answers (generated)', COUNT(*)
FROM interview_answers WHERE generated_question_id IS NOT NULL
UNION ALL
SELECT 'ai_feedbacks (generated)', COUNT(*)
FROM ai_feedbacks WHERE interview_answer_id IN (
    SELECT id FROM interview_answers WHERE generated_question_id IS NOT NULL
);
```

---

## 6. H2 vs PostgreSQL 차이점

### 6.1 H2 (개발 환경)

```bash
# H2 Console 접속 방법
# 1. application-dev.properties에서 h2-console.enabled=true 확인
# 2. 브라우저에서 http://localhost:8080/h2-console 접속
# 3. JDBC URL: jdbc:h2:mem:testdb
# 4. Username: sa
# 5. Password: (비어있음)
```

**특징**:
- In-memory DB (애플리케이션 재시작 시 데이터 초기화)
- CASCADE DELETE 동작 정상
- 트랜잭션 지원

### 6.2 PostgreSQL (프로덕션 환경)

```bash
# PostgreSQL 접속 방법
docker exec -it interview-note-postgres psql -U postgres -d interviewnote

# 또는 로컬 psql 사용
psql -h localhost -p 5432 -U postgres -d interviewnote
```

**특징**:
- 영구 스토리지 (데이터 영속성)
- CASCADE DELETE 동작 정상
- 트랜잭션 지원 강력

---

## 7. 권장 삭제 절차 (단계별)

### Case A: 테스트 데이터 1개 삭제 (초보자용)

```sql
-- 1. 삭제할 공고 확인
SELECT id, company_name, job_title FROM job_postings WHERE id = 1;

-- 2. 관련 답변 개수 확인
SELECT COUNT(*) AS answer_count
FROM interview_answers
WHERE generated_question_id IN (
    SELECT id FROM generated_questions WHERE job_posting_id = 1
);

-- 3. 트랜잭션 시작
BEGIN;

-- 4. AI 피드백 삭제
DELETE FROM ai_feedbacks
WHERE interview_answer_id IN (
    SELECT ia.id FROM interview_answers ia
    JOIN generated_questions gq ON ia.generated_question_id = gq.id
    WHERE gq.job_posting_id = 1
);

-- 5. 답변 삭제
DELETE FROM interview_answers
WHERE generated_question_id IN (
    SELECT id FROM generated_questions WHERE job_posting_id = 1
);

-- 6. 공고 삭제 (질문은 CASCADE)
DELETE FROM job_postings WHERE id = 1;

-- 7. 확인
SELECT COUNT(*) FROM job_postings WHERE id = 1;

-- 8. 커밋
COMMIT;
```

### Case B: 실수로 등록한 공고 여러 개 삭제 (중급자용)

```sql
-- 1. 최근 등록된 공고 3개 확인
SELECT id, company_name, job_title, created_at
FROM job_postings
ORDER BY created_at DESC
LIMIT 3;

-- 2. ID 목록 변수로 지정 (예: 5, 6, 7)
BEGIN;

DELETE FROM ai_feedbacks
WHERE interview_answer_id IN (
    SELECT ia.id FROM interview_answers ia
    JOIN generated_questions gq ON ia.generated_question_id = gq.id
    WHERE gq.job_posting_id IN (5, 6, 7)
);

DELETE FROM interview_answers
WHERE generated_question_id IN (
    SELECT id FROM generated_questions WHERE job_posting_id IN (5, 6, 7)
);

DELETE FROM job_postings WHERE id IN (5, 6, 7);

COMMIT;
```

### Case C: 특정 회사의 공고만 삭제 (고급)

```sql
-- 1. 특정 회사 공고 확인
SELECT id, company_name, job_title FROM job_postings
WHERE company_name LIKE '%토스%';

-- 2. 삭제 실행
BEGIN;

DELETE FROM ai_feedbacks
WHERE interview_answer_id IN (
    SELECT ia.id FROM interview_answers ia
    JOIN generated_questions gq ON ia.generated_question_id = gq.id
    JOIN job_postings jp ON gq.job_posting_id = jp.id
    WHERE jp.company_name LIKE '%토스%'
);

DELETE FROM interview_answers
WHERE generated_question_id IN (
    SELECT gq.id FROM generated_questions gq
    JOIN job_postings jp ON gq.job_posting_id = jp.id
    WHERE jp.company_name LIKE '%토스%'
);

DELETE FROM job_postings WHERE company_name LIKE '%토스%';

COMMIT;
```

---

## 8. 자주 묻는 질문 (FAQ)

### Q1: CASCADE DELETE가 작동하는데 왜 수동으로 답변을 삭제해야 하나요?

**A**: `generated_questions`는 `job_postings` 삭제 시 CASCADE로 자동 삭제되지만, `interview_answers`는 `generated_questions`와 FK만 있고 CASCADE 설정이 없어서 orphaned 상태가 됩니다. 따라서 수동 삭제가 필요합니다.

### Q2: Soft Delete를 사용하면 답변 삭제를 안 해도 되나요?

**A**: 네, Soft Delete(`is_active = FALSE`)는 데이터를 보존하므로 답변/피드백 삭제가 불필요합니다. 단, UI에서 `isActive = true` 필터링이 필요합니다.

### Q3: 실수로 삭제했을 때 복구 방법은?

**A**:
- **Hard Delete**: DB 백업에서 복구해야 합니다 (PostgreSQL의 경우 pg_dump 백업 필요)
- **Soft Delete**: `UPDATE job_postings SET is_active = TRUE WHERE id = 1`로 복구 가능

### Q4: 삭제 후 AI 토큰 사용량은 줄어드나요?

**A**: 아니요. 이미 사용한 토큰은 OpenAI에 과금되었으므로 삭제해도 환불되지 않습니다. 단, 향후 동일 공고 재등록 시 중복 요청 캐싱으로 비용 절감 가능합니다.

### Q5: 프로덕션 환경에서 안전하게 삭제하려면?

**A**:
1. DB 백업 먼저 수행 (`pg_dump`)
2. 삭제 전 `BEGIN` 트랜잭션 시작
3. 삭제 후 검증 쿼리로 확인
4. 문제 없으면 `COMMIT`, 문제 있으면 `ROLLBACK`

---

## 9. 향후 개선 제안

### 9.1 삭제 기능 구현 (Phase 6F 또는 Phase 9)

**추가할 기능**:
1. `JobPostingService.deleteJobPosting(id, userId)` 메서드
2. `DELETE /job-postings/{id}` 엔드포인트
3. UI에 "삭제" 버튼 추가 (확인 대화상자 포함)
4. Soft Delete 적용 (`isActive = false`)
5. 소유자 검증 (타 사용자 공고 삭제 방지)

**구현 예시**:
```kotlin
@Transactional
fun deleteJobPosting(jobPostingId: Long, userId: Long) {
    val jobPosting = jobPostingRepository.findById(jobPostingId)
        .orElseThrow { JobPostingNotFoundException(jobPostingId) }

    // 소유자 확인
    if (jobPosting.userId != userId) {
        throw UnauthorizedAccessException()
    }

    // Soft Delete
    jobPosting.deactivate()
    jobPostingRepository.save(jobPosting)
}
```

### 9.2 데이터베이스 마이그레이션 개선

**V11 Migration (제안)**:
```sql
-- interview_answers에 CASCADE DELETE 추가
ALTER TABLE interview_answers
DROP CONSTRAINT IF EXISTS fk_interview_answers_generated_question;

ALTER TABLE interview_answers
ADD CONSTRAINT fk_interview_answers_generated_question
FOREIGN KEY (generated_question_id)
REFERENCES generated_questions(id)
ON DELETE SET NULL;  -- 질문 삭제 시 답변은 유지하되 FK만 NULL 처리
```

**장점**: 질문 삭제 시 답변이 orphaned 되지 않고, `generated_question_id = NULL` 상태로 보존됨

---

## 10. 요약

### 현재 상황
- ✅ 채용 공고 생성 기능 완전 구현
- ✅ AI 질문 생성 (10개) 완전 구현
- ❌ 삭제 기능 미구현 (UI, Service, Controller 없음)
- ⚠️ Soft Delete 패턴 부분 구현 (`isActive` 플래그만 존재)

### 권장 삭제 방법
1. **Hard Delete**: AI 피드백 → 답변 → 공고 순서로 삭제 (CASCADE 활용)
2. **Soft Delete**: `is_active = FALSE`로 비활성화 (데이터 보존)

### 안전 장치
- `BEGIN` → 삭제 → 검증 → `COMMIT` 패턴 사용
- 삭제 전 반드시 백업 수행 (프로덕션)
- Orphaned 레코드 확인 쿼리 실행

### 다음 단계
- Phase 6F: 삭제 기능 구현 계획 (선택적)
- V11 Migration: FK CASCADE 설정 개선 (선택적)
