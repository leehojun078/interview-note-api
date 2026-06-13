# Phase 5 Migration Guide

**버전**: 0.5.0
**마이그레이션 날짜**: 2026-04-23
**작성자**: Development Team

---

## Executive Summary

Phase 5에서는 **IT 단일 직무에서 17개 직무로 확장**되며, 사용자 프로필 관리 기능이 추가됩니다.

**주요 변경사항**:
- ✅ 17개 직무 분야 지원 (JobField enum)
- ✅ 340개 질문 데이터 (각 직무별 20개)
- ✅ 사용자 프로필 (직무, 경력 선택)
- ✅ 직무별 AI 평가 프롬프트 (17개)
- ✅ 동적 카테고리 필터링

**영향 범위**:
- Database: 2개 마이그레이션 (V6, V7)
- Domain: 2개 enum 추가, User 필드 추가
- Service: QuestionService, UserService, PromptBuilder 변경
- Controller: ProfileController 추가, QuestionController 변경
- Templates: 1개 추가, 2개 수정

**마이그레이션 소요 시간**: 약 5분 (Flyway 자동 실행)

---

## Breaking Changes

### ⚠️ 주의: 하위 호환성 유지

Phase 5는 **하위 호환성을 유지**하도록 설계되었습니다:

1. **기존 사용자 데이터**: jobField, careerLevel은 **nullable**이므로 기존 사용자에게 영향 없음
2. **기존 질문 데이터**: V2 migration의 IT 질문 20개는 그대로 유지 (V7에서 320개 추가)
3. **API 호환성**: QuestionService.findAll()의 jobField 파라미터는 **nullable**이며 기본값 "IT" 사용

### 🔄 변경 필요 사항

**없음** - Flyway migration이 자동으로 처리합니다.

---

## Database Migrations

### V6: Add User Job Preferences

**파일**: `src/main/resources/db/migration/V6__add_user_job_preferences.sql`

**변경사항**:
```sql
-- 1. job_field 컬럼 추가 (nullable)
ALTER TABLE users ADD COLUMN job_field VARCHAR(50);

-- 2. career_level 컬럼 추가 (nullable)
ALTER TABLE users ADD COLUMN career_level VARCHAR(20);

-- 3. job_field 인덱스 생성 (필터링 성능 향상)
CREATE INDEX idx_users_job_field ON users(job_field);

-- 4. 테스트 사용자 기본값 설정 (선택사항)
UPDATE users SET job_field = 'IT', career_level = 'ENTRY'
WHERE email IN ('test@example.com', 'admin@example.com');
```

**영향**:
- ✅ 기존 사용자: jobField = null, careerLevel = null (IT 기본값 사용)
- ✅ 인덱스 추가: jobField 필터링 성능 향상 (< 200ms)
- ✅ 테스트 사용자: IT/ENTRY로 자동 설정

**롤백 방법**:
```sql
DROP INDEX idx_users_job_field;
ALTER TABLE users DROP COLUMN career_level;
ALTER TABLE users DROP COLUMN job_field;
```

---

### V7: Insert Multi-Job Field Questions

**파일**: `src/main/resources/db/migration/V7__insert_multi_job_field_questions.sql`

**변경사항**:
```sql
-- 17개 직무 × 20개 질문 = 340개 INSERT
-- 각 직무별 분포:
--   EASY: 5개
--   MEDIUM: 10개
--   HARD: 5개

INSERT INTO questions (job_field, target_job, category, content, difficulty, is_active) VALUES
  ('PLANNING', '경영기획자', '전략수립', '...', 'MEDIUM', true),
  ('MARKETING', '마케팅매니저', '캠페인기획', '...', 'MEDIUM', true),
  ('ACCOUNTING', '회계담당자', '재무분석', '...', 'MEDIUM', true),
  -- ... (총 340개)
```

**영향**:
- ✅ 기존 IT 질문 20개 유지
- ✅ 신규 16개 직무 × 20개 = 320개 질문 추가
- ✅ 총 질문 수: 20개 → 340개

**검증 쿼리**:
```sql
-- 전체 질문 개수 확인
SELECT COUNT(*) FROM questions; -- 결과: 340

-- 직무별 개수 확인
SELECT job_field, COUNT(*)
FROM questions
GROUP BY job_field
ORDER BY job_field;
-- 각 직무별 20개씩

-- 난이도 분포 확인 (IT 직무)
SELECT difficulty, COUNT(*)
FROM questions
WHERE job_field = 'IT'
GROUP BY difficulty;
-- EASY: 5, MEDIUM: 10, HARD: 5
```

**롤백 방법**:
```sql
-- 신규 질문 삭제 (V7에서 추가된 것만)
DELETE FROM questions WHERE id > 20;
-- 또는 직무별 삭제
DELETE FROM questions WHERE job_field != 'IT';
```

---

## Code Changes

### 1. Domain Layer

#### JobField Enum (새로 추가)

**파일**: `src/main/kotlin/.../domain/JobField.kt`

```kotlin
enum class JobField(val displayName: String, val code: String) {
    PLANNING("기획·전략", "PLANNING"),
    MARKETING("마케팅·홍보·조사", "MARKETING"),
    ACCOUNTING("회계·세무·재무", "ACCOUNTING"),
    HR("인사·노무·HRD", "HR"),
    ADMIN("총무·법무·사무", "ADMIN"),
    IT("IT개발", "IT"),
    DESIGN("디자인", "DESIGN"),
    SALES("영업·판매·무역", "SALES"),
    MD("상품기획·MD", "MD"),
    SERVICE("서비스", "SERVICE"),
    PRODUCTION("생산", "PRODUCTION"),
    CONSTRUCTION("건설·건축", "CONSTRUCTION"),
    MEDICAL("의료", "MEDICAL"),
    EDUCATION("교육", "EDUCATION"),
    MEDIA("미디어·문화·스포츠", "MEDIA"),
    FINANCE("금융·보험", "FINANCE"),
    PUBLIC("공공·복지", "PUBLIC");
}
```

#### CareerLevel Enum (새로 추가)

**파일**: `src/main/kotlin/.../domain/CareerLevel.kt`

```kotlin
enum class CareerLevel(val displayName: String, val code: String) {
    ENTRY("신입", "ENTRY"),
    JUNIOR("주니어(1-3년)", "JUNIOR"),
    SENIOR("시니어(3-7년)", "SENIOR"),
    SENIOR_PLUS("시니어+(7년 이상)", "SENIOR_PLUS");
}
```

#### User Entity (필드 추가)

**파일**: `src/main/kotlin/.../domain/User.kt`

```kotlin
// 추가된 필드
@Column(length = 50)
@Enumerated(EnumType.STRING)
var jobField: JobField? = null  // Phase 5: 직무 분야

@Column(length = 20)
@Enumerated(EnumType.STRING)
var careerLevel: CareerLevel? = null  // Phase 5: 경력 수준

// 추가된 메서드
fun updateJobPreferences(jobField: JobField?, careerLevel: CareerLevel?) {
    this.jobField = jobField
    this.careerLevel = careerLevel
}
```

---

### 2. Service Layer

#### QuestionService (메서드 추가)

**파일**: `src/main/kotlin/.../service/QuestionService.kt`

**변경사항**:
```kotlin
// 기존: fun findAll(category: String?, difficulty: String?)
// 변경 후:
fun findAll(
    jobField: String? = null,  // NEW - 직무 필터
    category: String? = null,
    difficulty: String? = null
): List<QuestionDto> {
    val effectiveJobField = jobField?.takeIf { it.isNotBlank() } ?: "IT"
    // ...
}

// 새 메서드: 직무별 카테고리 맵
fun getCategoriesByAllJobFields(): Map<String, List<String>> {
    return JobField.values().associate { jobField ->
        jobField.name to questionRepository.findDistinctCategoriesByJobField(jobField.name)
    }
}
```

**호환성**: 기존 호출 코드는 그대로 작동 (jobField = null → "IT")

#### UserService (메서드 추가)

**파일**: `src/main/kotlin/.../service/UserService.kt`

**새 메서드**:
```kotlin
fun getProfile(userId: Long): UserProfileDto {
    val user = userRepository.findById(userId)
        .orElseThrow { UserNotFoundException("...") }
    return UserProfileDto(/* ... */)
}

fun updateProfile(userId: Long, request: UpdateProfileRequest): User {
    val user = userRepository.findById(userId)
        .orElseThrow { UserNotFoundException("...") }
    user.changeName(request.name)
    user.updateJobPreferences(request.jobField, request.careerLevel)
    return userRepository.save(user)
}
```

#### PromptBuilder (17개 직무 지원)

**파일**: `src/main/kotlin/.../service/ai/PromptBuilder.kt`

**변경사항**:
```kotlin
fun buildSystemPrompt(jobField: String, targetJob: String): String {
    return when (jobField) {
        "IT" -> buildItSystemPrompt(targetJob)
        "PLANNING" -> buildPlanningSystemPrompt(targetJob)
        "MARKETING" -> buildMarketingSystemPrompt(targetJob)
        // ... 17개 모두
        else -> throw IllegalArgumentException("지원하지 않는 직무: $jobField")
    }
}

// 각 직무별 프롬프트 메서드
private fun buildItSystemPrompt(targetJob: String): String { /* ... */ }
private fun buildPlanningSystemPrompt(targetJob: String): String { /* ... */ }
// ... (17개)
```

---

### 3. Controller Layer

#### ProfileController (새로 추가)

**파일**: `src/main/kotlin/.../controller/ProfileController.kt`

```kotlin
@Controller
@RequestMapping("/profile")
class ProfileController(
    private val userService: UserService
) {
    @GetMapping
    fun showProfileSettings(
        @AuthenticationPrincipal userDetails: UserDetails,
        model: Model
    ): String { /* ... */ }

    @PostMapping("/update")
    fun updateProfile(
        @AuthenticationPrincipal userDetails: UserDetails,
        @Valid @ModelAttribute request: UpdateProfileRequest,
        bindingResult: BindingResult,
        redirectAttributes: RedirectAttributes
    ): String { /* ... */ }
}
```

**새 경로**:
- GET `/profile` - 프로필 조회 및 설정 페이지
- POST `/profile/update` - 프로필 업데이트

#### QuestionController (변경)

**파일**: `src/main/kotlin/.../controller/QuestionController.kt`

**변경사항**:
```kotlin
@GetMapping
fun list(
    @AuthenticationPrincipal userDetails: UserDetails?,
    @RequestParam(required = false) jobField: String?,  // NEW
    @RequestParam(required = false) category: String?,
    @RequestParam(required = false) difficulty: String?,
    model: Model
): String {
    // 로그인 사용자의 기본 직무 가져오기
    val defaultJobField = userDetails?.let { details ->
        userService.findByEmail(details.username)?.jobField?.name
    }
    val effectiveJobField = jobField ?: defaultJobField

    val questions = questionService.findAll(effectiveJobField, category, difficulty)

    // 동적 카테고리 맵 전달
    val categoriesByJobField = questionService.getCategoriesByAllJobFields()
    model.addAttribute("categoriesByJobField", categoriesByJobField)

    // ...
}
```

---

### 4. Template Changes

#### profile/settings.html (새로 추가)

**파일**: `src/main/resources/templates/profile/settings.html`

**주요 기능**:
- JobField enum → `<select>` 드롭다운 (17개 옵션)
- CareerLevel enum → `<select>` 드롭다운 (4개 옵션)
- 계정 정보 표시 (이메일, 가입일)
- Flash 메시지 (성공/실패)

#### questions/list.html (수정)

**파일**: `src/main/resources/templates/questions/list.html`

**추가된 기능**:
- 직무 선택 드롭다운 (17개 옵션)
- JavaScript 동적 카테고리 필터링
  - 직무 선택 시 해당 직무의 카테고리만 표시
  - `categoriesByJobField` 맵 활용

**JavaScript 로직**:
```javascript
const categoriesByJobField = /*[[${categoriesByJobField}]]*/ {};
document.getElementById('jobFieldSelect').addEventListener('change', function() {
    const jobField = this.value || 'IT';
    const categories = categoriesByJobField[jobField] || [];
    // 카테고리 드롭다운 동적 업데이트
});
```

#### home.html (수정)

**파일**: `src/main/resources/templates/home.html`

**추가된 기능**:
- 직무 미설정 시 안내 배너
- 사용자 직무 기반 추천 질문 섹션 (5개 랜덤)
- 추천 질문 제목에 직무명 표시 ("IT개발 추천 질문")

---

## Migration Steps

### 1. 사전 준비

**백업 생성** (권장):
```bash
# PostgreSQL 백업
pg_dump -U postgres -d interviewdb > backup_before_phase5.sql

# H2 백업 (dev)
# H2는 파일 기반이므로 data/ 디렉토리 복사
cp -r data/ data_backup/
```

### 2. 코드 업데이트

```bash
# Git에서 Phase 5 브랜치 가져오기
git checkout main
git pull origin main

# 또는 특정 태그
git checkout v0.5.0
```

### 3. 빌드 및 테스트

```bash
# 빌드
./gradlew clean build

# 테스트 (245개 통과 확인)
./gradlew test

# 결과 확인
# Total Tests: 245
# Passed: 245 ✅
```

### 4. Flyway Migration 실행

**자동 실행** (권장):
```bash
# Spring Boot 실행 시 Flyway가 자동으로 V6, V7 실행
./gradlew bootRun
```

**수동 실행** (필요 시):
```bash
./gradlew flywayMigrate -Dflyway.url=jdbc:postgresql://localhost:5432/interviewdb
```

**로그 확인**:
```
INFO  o.f.c.i.command.DbMigrate - Successfully applied 2 migrations to schema "public", now at version v7
```

### 5. 검증

#### 데이터베이스 검증

**H2 Console** (dev):
```sql
-- 1. users 테이블 컬럼 확인
SELECT * FROM information_schema.columns
WHERE table_name = 'USERS'
AND column_name IN ('JOB_FIELD', 'CAREER_LEVEL');

-- 2. 질문 개수 확인
SELECT COUNT(*) FROM questions; -- 결과: 340

-- 3. 직무별 개수
SELECT job_field, COUNT(*)
FROM questions
GROUP BY job_field;
```

**PostgreSQL** (prod):
```bash
psql -U postgres -d interviewdb

-- 동일한 쿼리 실행
```

#### 애플리케이션 검증

**브라우저 테스트**:
1. http://localhost:8080 접속
2. 로그인
3. `/profile` 접속 → 17개 직무 드롭다운 확인
4. 직무 선택 (예: "영업·판매·무역")
5. 저장
6. `/questions` 접속 → 영업 질문 20개 표시 확인
7. 카테고리 필터: 영업 카테고리만 표시되는지 확인

**API 테스트**:
```bash
# 1. IT 질문 조회 (기본값)
curl -s http://localhost:8080/questions | grep "IT"

# 2. 영업 질문 조회
curl -s "http://localhost:8080/questions?jobField=SALES" | grep "영업"
```

### 6. 모니터링

**로그 확인**:
```bash
# 애플리케이션 로그
tail -f logs/application.log

# Flyway migration 성공 로그 확인
grep "Successfully applied" logs/application.log
```

**Health Check**:
```bash
curl http://localhost:8080/actuator/health
# 결과: {"status":"UP"}
```

**메트릭 확인**:
```bash
curl http://localhost:8080/actuator/prometheus | grep questions
```

---

## Rollback Plan

### Option 1: Flyway Rollback (권장)

```bash
# V7 롤백 (질문 데이터 삭제)
./gradlew flywayUndo -Dflyway.target=v6

# V6 롤백 (users 테이블 컬럼 삭제)
./gradlew flywayUndo -Dflyway.target=v5
```

### Option 2: 수동 롤백

```sql
-- V7 롤백: 신규 질문 삭제
DELETE FROM questions WHERE id > 20;

-- V6 롤백: 컬럼 삭제
DROP INDEX idx_users_job_field;
ALTER TABLE users DROP COLUMN career_level;
ALTER TABLE users DROP COLUMN job_field;
```

### Option 3: 백업 복원

```bash
# PostgreSQL 백업 복원
psql -U postgres -d interviewdb < backup_before_phase5.sql

# H2 백업 복원
rm -rf data/
cp -r data_backup/ data/
```

---

## Troubleshooting

### 문제 1: Flyway migration 실패

**증상**:
```
ERROR: relation "users" does not exist
```

**원인**: V4, V5 migration이 실행되지 않음 (Phase 4 미완료)

**해결**:
```bash
# Phase 4 migration 먼저 실행
git checkout v0.4.1
./gradlew bootRun
# V4, V5 실행 확인 후

# Phase 5로 이동
git checkout v0.5.0
./gradlew bootRun
```

### 문제 2: 340개 질문이 로드되지 않음

**증상**:
```sql
SELECT COUNT(*) FROM questions; -- 결과: 20
```

**원인**: V7 migration이 실행되지 않음

**해결**:
```bash
# Flyway 상태 확인
./gradlew flywayInfo

# V7이 Pending 상태면 수동 실행
./gradlew flywayMigrate
```

### 문제 3: jobField 필터링이 작동하지 않음

**증상**: `/questions?jobField=SALES` 접속 시 IT 질문 표시

**원인**: QuestionService.findAll() 파라미터 전달 오류

**해결**:
```kotlin
// QuestionController.kt 확인
val questions = questionService.findAll(
    jobField = effectiveJobField,  // ← 이 부분 확인
    category = category,
    difficulty = difficulty
)
```

### 문제 4: PromptBuilder IllegalArgumentException

**증상**:
```
IllegalArgumentException: 지원하지 않는 직무 분야입니다: null
```

**원인**: Question.jobField가 null

**해결**:
```sql
-- 모든 질문에 jobField 설정 확인
SELECT id, job_field FROM questions WHERE job_field IS NULL;

-- NULL인 질문을 IT로 업데이트
UPDATE questions SET job_field = 'IT' WHERE job_field IS NULL;
```

### 문제 5: 동적 카테고리 필터링이 작동하지 않음

**증상**: 직무 선택 시 카테고리가 업데이트되지 않음

**원인**: JavaScript 오류 또는 `categoriesByJobField` 전달 실패

**해결**:
```html
<!-- questions/list.html 확인 -->
<!-- 브라우저 개발자 도구 Console 확인 -->
<script th:inline="javascript">
    const categoriesByJobField = /*[[${categoriesByJobField}]]*/ {};
    console.log(categoriesByJobField); // ← 디버깅
</script>
```

---

## Performance Impact

### Before Phase 5 (IT만)

| 작업 | 질문 수 | 시간 |
|------|--------|------|
| 전체 질문 조회 | 20개 | < 100ms |
| 카테고리 필터링 | ~7개 | < 50ms |

### After Phase 5 (17개 직무)

| 작업 | 질문 수 | 시간 | 인덱스 |
|------|--------|------|-------|
| 전체 질문 조회 | 340개 | < 1000ms | - |
| jobField 필터링 | 20개 | < 200ms | idx_users_job_field |
| 카테고리 필터링 | ~7개 | < 100ms | - |

**성능 개선**:
- ✅ jobField 인덱스로 필터링 성능 200ms 이내
- ✅ 전체 조회도 1초 이내 (340개)
- ✅ 캐시 없이도 충분한 성능

---

## Security Considerations

### 1. 데이터 검증

**jobField 검증**:
```kotlin
// JobField enum 사용으로 SQL Injection 방지
@Enumerated(EnumType.STRING)
var jobField: JobField? = null
```

**입력 검증**:
```kotlin
data class UpdateProfileRequest(
    @field:NotBlank val name: String,
    val jobField: JobField?,  // enum 타입 → 안전
    val careerLevel: CareerLevel?
)
```

### 2. 권한 제어

**프로필 접근**:
```kotlin
// 본인 프로필만 조회/수정 가능
@AuthenticationPrincipal userDetails: UserDetails
val user = userService.findByEmail(userDetails.username)
```

### 3. CSRF 보호

**모든 POST 요청**:
```html
<input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
```

---

## Support

**문제 발생 시**:
1. 로그 확인: `logs/application.log`
2. Health Check: `http://localhost:8080/actuator/health`
3. GitHub Issues: [프로젝트 저장소]/issues

**참고 문서**:
- [PHASE5_STEP17_TEST_REPORT.md](./PHASE5_STEP17_TEST_REPORT.md) - 테스트 보고서
- [CHANGELOG.md](./CHANGELOG.md) - 변경 이력
- [phase5_implementation_plan.md](./phase5_implementation_plan.md) - 상세 설계

---

**작성일**: 2026-04-23
**버전**: 0.5.0
**문서 버전**: 1.0
