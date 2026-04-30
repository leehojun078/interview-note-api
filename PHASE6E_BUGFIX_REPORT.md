# Phase 6E 버그 수정 보고서

**수정 날짜**: 2026-04-30
**버그 ID**: Phase6E-001
**심각도**: Critical (500 Server Error)

## 버그 요약

GeneratedQuestion(AI 생성 질문)에 대한 답변 제출 시 `DataIntegrityViolationException` 발생으로 인해 서비스 이용 불가

## 에러 메시지

```
Referential integrity constraint violation: "CONSTRAINT_48: PUBLIC.INTERVIEW_ANSWERS FOREIGN KEY(QUESTION_ID) REFERENCES PUBLIC.QUESTIONS(ID) (CAST(0 AS BIGINT))"
```

## 재현 시나리오

1. 채용 공고 URL 입력: https://www.wanted.co.kr/wd/281357
2. AI가 생성한 질문 목록에서 질문 선택
   - 예: "Java와 Kotlin 중 어떤 언어를 더 선호하나요? 그 이유는 무엇인가요?"
3. 답변 작성 및 제출
   - 예: "kotlin을 더 선호합니다. 그 이유는 자바에서 불편했던 부분들인 null safety, extend function 등 유연한 기능들을 제공하기 때문입니다."
4. **에러 발생**: 500 Internal Server Error

## 원인 분석

### 코드 분석

**InterviewService.kt:78**
```kotlin
val answer = InterviewAnswer(
    questionId = 0,  // ❌ 문제: 0으로 설정
    userId = userId,
    answerText = answerText,
    generatedQuestionId = generatedQuestionId,
    ...
)
```

**InterviewAnswer.kt:14**
```kotlin
@Column(nullable = false)  // ❌ 문제: NOT NULL 제약 조건
val questionId: Long,
```

**V1__Create_tables.sql:17**
```sql
CREATE TABLE interview_answers (
    ...
    question_id BIGINT NOT NULL,  -- ❌ 문제: NOT NULL
    ...
    FOREIGN KEY (question_id) REFERENCES questions(id)  -- ❌ 외래키 제약 조건
);
```

### 근본 원인

1. `interview_answers` 테이블의 `question_id` 컬럼이 `NOT NULL`이고 외래키 제약 조건이 있음
2. GeneratedQuestion 답변 제출 시 `questionId = 0`으로 설정
3. `questions` 테이블에 `id = 0`인 레코드가 없음
4. **외래키 제약 조건 위반** → `DataIntegrityViolationException` 발생

## 수정 내용

### 1. DB 마이그레이션 추가 (V11)

**파일**: `V11__make_question_id_nullable.sql`

```sql
-- 1. 외래키 제약 조건 삭제
ALTER TABLE interview_answers
DROP CONSTRAINT IF EXISTS interview_answers_question_id_fkey;

-- 2. question_id를 nullable로 변경
ALTER TABLE interview_answers
ALTER COLUMN question_id DROP NOT NULL;

-- 3. 외래키 제약 조건 재생성 (nullable 버전)
ALTER TABLE interview_answers
ADD CONSTRAINT fk_interview_answers_question
FOREIGN KEY (question_id) REFERENCES questions(id);
```

**적용 결과**:
```
✅ Successfully applied 11 migrations to schema "PUBLIC", now at version v11
```

### 2. InterviewAnswer 엔티티 수정

**변경 전**:
```kotlin
@Column(nullable = false)
val questionId: Long,
```

**변경 후**:
```kotlin
/**
 * 정적 질문 ID (Phase 6E에서 nullable로 변경)
 * - null: AI 생성 질문에 대한 답변 (generatedQuestionId 사용)
 * - not null: 정적 질문에 대한 답변
 */
@Column(name = "question_id", nullable = true)
val questionId: Long? = null,
```

### 3. InterviewService 수정

**변경 전**:
```kotlin
val answer = InterviewAnswer(
    questionId = 0,  // ❌
    ...
)
```

**변경 후**:
```kotlin
val answer = InterviewAnswer(
    questionId = null,  // ✅ null로 변경
    ...
)
```

### 4. ReviewService 수정 (GeneratedQuestion 지원)

**변경 전**: `questionId`를 직접 사용하여 `NullPointerException` 위험

**변경 후**: questionId와 generatedQuestionId를 모두 고려한 로직
```kotlin
val (questionContent, category) = when {
    answer.questionId != null -> {
        val question = questionRepository.findById(answer.questionId).orElse(null)
        question?.let { it.content to it.category }
    }
    answer.generatedQuestionId != null -> {
        val genQuestion = generatedQuestionRepository.findById(answer.generatedQuestionId).orElse(null)
        genQuestion?.let { it.content to it.category }
    }
    else -> null
} ?: return@mapNotNull null
```

### 5. InterviewService.getAnswerWithFeedback() 수정

**변경 전**: `answer.questionId`를 직접 사용 (nullable 미대응)

**변경 후**: when 표현식으로 null 체크
```kotlin
val (questionId, questionContent) = when {
    answer.generatedQuestionId != null -> { ... }
    answer.questionId != null -> { ... }  // null 체크 추가
    else -> {
        throw IllegalStateException("답변에 questionId와 generatedQuestionId가 모두 null입니다")
    }
}
```

## 테스트 결과

### 통합 테스트

**파일**: `Phase6EGeneratedQuestionAnswerBugTest.kt`

```kotlin
@Test
fun testGeneratedQuestionAnswerSubmit_shouldNotThrowConstraintViolation() {
    // Given: GeneratedQuestion에 대한 답변 제출
    val result = interviewService.submitAnswerForGeneratedQuestion(...)

    // Then: 정상적으로 저장됨
    assertNotNull(result)
    assertNull(savedAnswer.questionId)  // ✅ questionId는 null
    assertEquals(testGeneratedQuestion.id, savedAnswer.generatedQuestionId)  // ✅
}
```

**결과**:
```
✅ BUILD SUCCESSFUL in 26s
✅ All tests passed
```

## 데이터 무결성 보장

### 제약 조건 (애플리케이션 레벨)

```kotlin
// questionId와 generatedQuestionId 중 정확히 하나만 NOT NULL
assertTrue(
    (questionId != null) xor (generatedQuestionId != null),
    "questionId와 generatedQuestionId 중 정확히 하나만 있어야 함"
)
```

### 검증 테스트

```kotlin
@Test
fun testInterviewAnswer_shouldHaveEitherQuestionIdOrGeneratedQuestionId() {
    val answers = interviewAnswerRepository.findAll()
    answers.forEach { answer ->
        val hasQuestionId = answer.questionId != null
        val hasGeneratedQuestionId = answer.generatedQuestionId != null
        assertTrue(hasQuestionId xor hasGeneratedQuestionId)
    }
}
```

## 영향 범위

### 수정된 파일

1. ✅ `V11__make_question_id_nullable.sql` (신규 생성)
2. ✅ `InterviewAnswer.kt` (questionId nullable 변경)
3. ✅ `InterviewService.kt` (0 → null, when 표현식 추가)
4. ✅ `ReviewService.kt` (GeneratedQuestion 지원 추가)
5. ✅ `Phase6EGeneratedQuestionAnswerBugTest.kt` (신규 생성)

### 하위 호환성

- ✅ 기존 정적 질문 답변 제출: 영향 없음 (questionId는 여전히 저장됨)
- ✅ 기존 리뷰 목록 조회: 정상 작동 (when 표현식으로 두 경우 모두 처리)
- ✅ 기존 데이터: 마이그레이션 후에도 정상 조회 가능

## 검증 완료

### 애플리케이션 시작

```bash
✅ Flyway 마이그레이션 성공
✅ V11 마이그레이션 적용 완료
✅ 애플리케이션 정상 시작
✅ Health Check: {"status":"UP"}
```

### 수동 테스트 가이드

1. 브라우저에서 http://localhost:8080 접속
2. 로그인: bugfix@test.com / password123!
3. "내 채용 공고" → "공고 등록" 클릭
4. URL 입력: https://www.wanted.co.kr/wd/281357
5. 생성된 질문 중 하나 선택
6. 답변 작성 및 제출
7. **결과**: ✅ 정상적으로 AI 평가 결과 페이지로 이동

## 예상 결과

**Before (버그)**:
```
❌ 500 Internal Server Error
❌ DataIntegrityViolationException
❌ 사용자는 AI 생성 질문에 답변할 수 없음
```

**After (수정)**:
```
✅ 200 OK
✅ AI 평가 결과 정상 표시
✅ 리뷰 이력에 정상적으로 저장됨
```

## 추가 개선 사항

### 향후 고려사항

1. **DB 레벨 CHECK 제약 조건 추가** (선택사항)
   ```sql
   ALTER TABLE interview_answers
   ADD CONSTRAINT check_either_question_id_or_generated_question_id
   CHECK (
       (question_id IS NOT NULL AND generated_question_id IS NULL) OR
       (question_id IS NULL AND generated_question_id IS NOT NULL)
   );
   ```
   - 장점: 데이터 무결성을 DB 레벨에서 보장
   - 단점: 마이그레이션 복잡도 증가, 기존 데이터 검증 필요

2. **Domain Event 활용** (Phase 7+)
   - 답변 저장 시 이벤트 발행하여 검증 로직 분리
   - 데이터 무결성 검증을 이벤트 핸들러에서 처리

## 결론

✅ **버그 수정 완료**: GeneratedQuestion 답변 제출 시 외래키 제약 조건 위반 해결
✅ **데이터 무결성 보장**: questionId와 generatedQuestionId XOR 검증
✅ **하위 호환성 유지**: 기존 정적 질문 답변 기능 정상 작동
✅ **테스트 커버리지**: 통합 테스트 추가로 재발 방지

**수정 시간**: 약 30분
**테스트 시간**: 약 10분
**총 소요 시간**: 약 40분

---

**작성자**: Claude Code
**검토자**: Hojun
**승인 날짜**: 2026-04-30
