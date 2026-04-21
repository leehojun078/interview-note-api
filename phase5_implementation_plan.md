# Phase 5: Multi-Job Field Support Implementation Plan

**프로젝트**: Interview Note API
**목표**: IT 단일 직무에서 17개 직무로 확장 + 사용자 직무 선호도 저장
**작성일**: 2026-04-20

---

## Executive Summary

현재 Interview Note API는 IT 직무 면접 질문만 지원합니다. Phase 5에서는 **17개 직무로 확장**하고, 사용자가 자신의 **직무와 경력을 프로필에 저장**하여 **맞춤형 질문**을 받을 수 있도록 개선합니다.

**핵심 변경사항**:
- User 엔티티에 `jobField`, `careerLevel` 필드 추가 (nullable)
- 17개 직무별 20개 질문 데이터 생성 (총 340개 질문)
- 프로필 설정 페이지 구현 (직무/경력 선택)
- QuestionService jobField 필터링 지원
- PromptBuilder 17개 직무 프롬프트 구현
- 홈페이지 개인화 (사용자 직무 기반 질문 추천)

**예상 작업량**: 약 37시간 (18단계)

---

## User Decisions (확정된 설계 결정)

다음 설계 결정사항은 사용자 확인을 받았습니다:

1. **직무 선택 모델**: ✅ **단일 선택** (사용자당 1개 직무만)
   - User 테이블에 `job_field VARCHAR(50)` 컬럼 추가
   - Many-to-Many 관계 불필요
   - 구현 복잡도 최소화

2. **경력 입력 방식**: ✅ **드롭다운** (4가지 선택지)
   - `CareerLevel` enum: 신입, 주니어(1-3년), 시니어(3-7년), 시니어+(7년 이상)
   - User 테이블에 `career_level VARCHAR(20)` 컬럼 추가
   - 데이터 일관성 보장

3. **회원가입 시 수집 여부**: ✅ **가입 후 프로필에서만 설정**
   - 회원가입 폼은 현재 그대로 (이메일, 비밀번호, 이름)
   - `/profile` 페이지에서 직무/경력 설정
   - 가입 장벽 최소화

4. **질문 데이터 생성**: ✅ **AI로 자동 생성**
   - Claude/GPT로 각 직무별 20개 질문 생성
   - V2 migration 형식 준수 (EASY 5개, MEDIUM 10개, HARD 5개)
   - 총 340개 질문 (17 직무 × 20개)

---

## 17개 직무 목록

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
    PUBLIC("공공·복지", "PUBLIC")
}
```

---

## High-Level Architecture Changes

### 1. Database Schema

**V6__add_user_job_preferences.sql**:
```sql
ALTER TABLE users ADD COLUMN job_field VARCHAR(50);
ALTER TABLE users ADD COLUMN career_level VARCHAR(20);
CREATE INDEX idx_users_job_field ON users(job_field);
```

**V7__insert_multi_job_field_questions.sql**:
- INSERT 340 questions (17 job fields × 20 questions)
- Each job field:
  - 5 EASY questions
  - 10 MEDIUM questions
  - 5 HARD questions
- Categories vary by job field (e.g., IT: 기술역량/문제해결/협업, 영업: 고객관리/실적달성/협상)

### 2. Domain Layer

**New Enums**:
- `JobField.kt`: 17개 직무 정의
- `CareerLevel.kt`: 4개 경력 수준 정의

**User Entity Update**:
```kotlin
// 추가 필드
var jobField: JobField? = null
var careerLevel: CareerLevel? = null

// 추가 메서드
fun updateJobPreferences(jobField: JobField?, careerLevel: CareerLevel?)
```

### 3. Service Layer

**QuestionService**:
- `findAll(jobField: String?, category: String?, difficulty: String?)`
  - jobField 파라미터 추가
  - null이면 사용자 직무 기본값 사용 (로그인 시)
  - 미로그인 or 직무 미설정 시 "IT" 기본값

**UserService**:
- `getProfile(userId: Long): UserProfileDto`
- `updateProfile(userId: Long, request: UpdateProfileRequest): User`

**PromptBuilder**:
- 17개 직무별 `buildXxxSystemPrompt()` 메서드 구현
- `when (jobField)` 분기에 모든 직무 추가

### 4. Controller Layer

**New Controller**:
- `ProfileController`: GET /profile, POST /profile/update

**Updated Controllers**:
- `QuestionController`: jobField 파라미터 추가
- `HomeController`: 개인화된 질문 추천 (사용자 직무 기반)

### 5. View Layer

**New Templates**:
- `profile/settings.html`: 프로필 설정 페이지

**Updated Templates**:
- `questions/list.html`: 직무 필터 드롭다운 추가
- `home.html`: 직무 미설정 시 배너, 추천 질문 섹션 추가
- `fragments/layout.html`: 네비게이션에 "프로필 설정" 링크 추가

---

## Implementation Steps (18단계)

### **Step 1: Domain Models for Job Fields**
**복잡도**: Simple | **예상 시간**: 1시간

**생성할 파일**:
- `/src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/domain/JobField.kt`
- `/src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/domain/CareerLevel.kt`

**수정할 파일**:
- `/src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/domain/User.kt`

**작업 내용**:
1. JobField enum 생성 (17개 값 + displayName, code)
2. CareerLevel enum 생성 (4개 값 + displayName)
3. User 엔티티에 필드 추가:
   ```kotlin
   @Enumerated(EnumType.STRING)
   var jobField: JobField? = null

   @Enumerated(EnumType.STRING)
   var careerLevel: CareerLevel? = null
   ```
4. User에 메서드 추가:
   ```kotlin
   fun updateJobPreferences(jobField: JobField?, careerLevel: CareerLevel?) {
       this.jobField = jobField
       this.careerLevel = careerLevel
   }
   ```

**테스트**:
- User 엔티티 단위 테스트 (`UserTest.kt`)
- `updateJobPreferences()` 메서드 검증

**의존성**: 없음

---

### **Step 2: Database Migration - User Preferences**
**복잡도**: Simple | **예상 시간**: 30분

**생성할 파일**:
- `/src/main/resources/db/migration/V6__add_user_job_preferences.sql`

**작업 내용**:
```sql
-- Add job field and career level columns
ALTER TABLE users ADD COLUMN job_field VARCHAR(50);
ALTER TABLE users ADD COLUMN career_level VARCHAR(20);

-- Create index for filtering
CREATE INDEX idx_users_job_field ON users(job_field);

-- Update existing test users (optional)
UPDATE users SET job_field = 'IT', career_level = 'ENTRY'
WHERE email IN ('test@example.com', 'admin@example.com');
```

**테스트**:
- Flyway migration 자동 실행 확인 (bootRun)
- H2 Console에서 컬럼 추가 검증

**의존성**: Step 1 완료

---

### **Step 3: Generate Multi-Job Field Question Data**
**복잡도**: Medium | **예상 시간**: 4시간

**생성할 파일**:
- `/src/main/resources/db/migration/V7__insert_multi_job_field_questions.sql`

**작업 내용**:
1. AI(Claude/GPT)로 17개 직무별 20개 질문 생성
2. 각 직무별 특성에 맞는 카테고리 정의:
   - IT: 기술역량, 문제해결, 협업경험
   - 영업: 고객관리, 실적달성, 협상스킬
   - 회계: 재무분석, 세무지식, 리스크관리
   - (나머지 14개 직무도 유사하게)
3. 난이도 분포: EASY 5개, MEDIUM 10개, HARD 5개
4. V2 migration 형식 준수:
   ```sql
   INSERT INTO questions (job_field, target_job, category, content, difficulty) VALUES
   ('PLANNING', '경영기획자', '전략수립', '회사의 3개년 중장기 전략을 수립한 경험이 있나요?', 'MEDIUM'),
   ('MARKETING', '마케팅매니저', '캠페인기획', 'SNS 마케팅 캠페인을 성공시킨 사례를 설명해주세요.', 'MEDIUM'),
   ...
   ```

**검증**:
- 총 340개 질문 삽입 확인 (20개 × 17개)
- 중복 content 체크 쿼리 실행
- 각 직무별 카테고리 분포 확인

**의존성**: Step 2 완료

**Note**: AI 생성 시 각 직무의 일반적인 면접 질문 패턴 연구 필요

---

### **Step 4: Update QuestionRepository**
**복잡도**: Simple | **예상 시간**: 1시간

**수정할 파일**:
- `/src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/repository/QuestionRepository.kt`

**작업 내용**:
```kotlin
interface QuestionRepository : JpaRepository<Question, Long> {
    // 기존 메서드들...

    // 새로운 jobField 필터링 메서드
    fun findByJobFieldAndIsActiveTrue(jobField: String): List<Question>
    fun findByJobFieldAndCategoryAndIsActiveTrue(jobField: String, category: String): List<Question>
    fun findByJobFieldAndDifficultyAndIsActiveTrue(jobField: String, difficulty: String): List<Question>
    fun findByJobFieldAndCategoryAndDifficultyAndIsActiveTrue(
        jobField: String,
        category: String,
        difficulty: String
    ): List<Question>
}
```

**테스트**:
- Repository 통합 테스트 (`QuestionRepositoryTest.kt`)
- 각 메서드별 jobField 필터링 검증
- 빈 결과 및 다중 결과 처리 확인

**의존성**: Step 3 완료

---

### **Step 5: Update QuestionService**
**복잡도**: Simple | **예상 시간**: 1시간

**수정할 파일**:
- `/src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/service/QuestionService.kt`

**작업 내용**:
```kotlin
fun findAll(
    jobField: String? = null,
    category: String? = null,
    difficulty: String? = null
): List<QuestionDto> {
    val effectiveJobField = jobField?.takeIf { it.isNotBlank() } ?: "IT"

    val questions = when {
        category != null && difficulty != null ->
            questionRepository.findByJobFieldAndCategoryAndDifficultyAndIsActiveTrue(
                effectiveJobField, category, difficulty
            )
        category != null ->
            questionRepository.findByJobFieldAndCategoryAndIsActiveTrue(
                effectiveJobField, category
            )
        difficulty != null ->
            questionRepository.findByJobFieldAndDifficultyAndIsActiveTrue(
                effectiveJobField, difficulty
            )
        else ->
            questionRepository.findByJobFieldAndIsActiveTrue(effectiveJobField)
    }

    return questions.map { it.toDto() }
}
```

**테스트**:
- Service 단위 테스트 (`QuestionServiceTest.kt`)
- jobField null 처리 (기본값 "IT")
- 모든 필터 조합 검증

**의존성**: Step 4 완료

---

### **Step 6: Create UserProfileDto and Update UserService**
**복잡도**: Simple | **예상 시간**: 2시간

**생성할 파일**:
- `/src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/dto/UserProfileDto.kt`

**수정할 파일**:
- `/src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/service/UserService.kt`

**작업 내용**:

**UserProfileDto.kt**:
```kotlin
data class UserProfileDto(
    val id: Long,
    val email: String,
    val name: String,
    val jobField: JobField?,
    val careerLevel: CareerLevel?,
    val createdAt: LocalDateTime
)

data class UpdateProfileRequest(
    @field:NotBlank(message = "이름을 입력해주세요")
    @field:Size(min = 2, max = 50, message = "이름은 2-50자 이내여야 합니다")
    val name: String,

    val jobField: JobField?,
    val careerLevel: CareerLevel?
)
```

**UserService 추가 메서드**:
```kotlin
fun getProfile(userId: Long): UserProfileDto {
    val user = userRepository.findById(userId)
        .orElseThrow { UserNotFoundException("사용자를 찾을 수 없습니다: $userId") }

    return UserProfileDto(
        id = user.id,
        email = user.email,
        name = user.name,
        jobField = user.jobField,
        careerLevel = user.careerLevel,
        createdAt = user.createdAt
    )
}

fun updateProfile(userId: Long, request: UpdateProfileRequest): User {
    val user = userRepository.findById(userId)
        .orElseThrow { UserNotFoundException("사용자를 찾을 수 없습니다: $userId") }

    user.changeName(request.name)
    user.updateJobPreferences(request.jobField, request.careerLevel)

    return userRepository.save(user)
}
```

**테스트**:
- UserService 단위 테스트
- getProfile() 검증
- updateProfile() 검증 (정상 케이스 + 예외)

**의존성**: Step 1 완료

---

### **Step 7: Create ProfileController**
**복잡도**: Medium | **예상 시간**: 2시간

**생성할 파일**:
- `/src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/controller/ProfileController.kt`

**작업 내용**:
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
    ): String {
        val user = userService.findByEmail(userDetails.username)
            ?: throw UserNotFoundException("사용자를 찾을 수 없습니다")

        val profile = userService.getProfile(user.id)

        model.addAttribute("profile", profile)
        model.addAttribute("jobFields", JobField.values())
        model.addAttribute("careerLevels", CareerLevel.values())

        return "profile/settings"
    }

    @PostMapping("/update")
    fun updateProfile(
        @AuthenticationPrincipal userDetails: UserDetails,
        @Valid @ModelAttribute request: UpdateProfileRequest,
        bindingResult: BindingResult,
        redirectAttributes: RedirectAttributes
    ): String {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "입력 값을 확인해주세요")
            return "redirect:/profile"
        }

        val user = userService.findByEmail(userDetails.username)
            ?: throw UserNotFoundException("사용자를 찾을 수 없습니다")

        userService.updateProfile(user.id, request)

        redirectAttributes.addFlashAttribute("success", "프로필이 업데이트되었습니다")
        return "redirect:/profile"
    }
}
```

**테스트**:
- Controller 통합 테스트 (`ProfileControllerTest.kt`)
- GET /profile 인증된 사용자
- POST /profile/update 성공 케이스
- POST /profile/update 검증 실패 케이스

**의존성**: Step 6 완료

---

### **Step 8: Create Profile Settings UI**
**복잡도**: Medium | **예상 시간**: 3시간

**생성할 파일**:
- `/src/main/resources/templates/profile/settings.html`

**작업 내용**:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>프로필 설정 - 면접 리뷰</title>
    <link href="https://cdn.jsdelivr.net/npm/tailwindcss@2.2.19/dist/tailwind.min.css" rel="stylesheet">
</head>
<body>
    <div th:replace="~{fragments/layout :: navbar}"></div>

    <div class="container mx-auto px-4 py-8">
        <h1 class="text-3xl font-bold mb-6">프로필 설정</h1>

        <!-- Success/Error Messages -->
        <div th:if="${success}" class="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded mb-4">
            <span th:text="${success}"></span>
        </div>
        <div th:if="${error}" class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
            <span th:text="${error}"></span>
        </div>

        <!-- Profile Form -->
        <form method="post" th:action="@{/profile/update}" class="bg-white shadow-md rounded px-8 pt-6 pb-8 mb-4">
            <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>

            <!-- Name -->
            <div class="mb-4">
                <label class="block text-gray-700 font-bold mb-2">이름</label>
                <input type="text" name="name" th:value="${profile.name}"
                       class="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700"
                       required minlength="2" maxlength="50"/>
            </div>

            <!-- Job Field -->
            <div class="mb-4">
                <label class="block text-gray-700 font-bold mb-2">직무 분야</label>
                <select name="jobField" class="shadow border rounded w-full py-2 px-3 text-gray-700">
                    <option value="">선택 안 함 (기본: IT)</option>
                    <option th:each="job : ${jobFields}"
                            th:value="${job.name()}"
                            th:text="${job.displayName}"
                            th:selected="${profile.jobField == job}"></option>
                </select>
            </div>

            <!-- Career Level -->
            <div class="mb-6">
                <label class="block text-gray-700 font-bold mb-2">경력 수준</label>
                <select name="careerLevel" class="shadow border rounded w-full py-2 px-3 text-gray-700">
                    <option value="">선택 안 함</option>
                    <option th:each="level : ${careerLevels}"
                            th:value="${level.name()}"
                            th:text="${level.displayName}"
                            th:selected="${profile.careerLevel == level}"></option>
                </select>
            </div>

            <!-- Buttons -->
            <div class="flex items-center justify-between">
                <button type="submit" class="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded">
                    저장
                </button>
                <a th:href="@{/home}" class="text-blue-500 hover:text-blue-800">취소</a>
            </div>
        </form>

        <!-- Account Info -->
        <div class="bg-gray-100 rounded p-4 mt-6">
            <h2 class="text-xl font-bold mb-2">계정 정보</h2>
            <p><strong>이메일:</strong> <span th:text="${profile.email}"></span></p>
            <p><strong>가입일:</strong> <span th:text="${#temporals.format(profile.createdAt, 'yyyy-MM-dd HH:mm')}"></span></p>
        </div>
    </div>
</body>
</html>
```

**테스트**:
- 수동 UI 테스트 (브라우저)
- 직무 드롭다운 17개 옵션 확인
- 경력 드롭다운 4개 옵션 확인
- 폼 제출 및 flash 메시지 확인

**의존성**: Step 7 완료

---

### **Step 9: Update QuestionController with JobField Filter**
**복잡도**: Simple | **예상 시간**: 1시간

**수정할 파일**:
- `/src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/controller/QuestionController.kt`

**작업 내용**:
```kotlin
@GetMapping
fun list(
    @AuthenticationPrincipal userDetails: UserDetails?,
    @RequestParam(required = false) jobField: String?,
    @RequestParam(required = false) category: String?,
    @RequestParam(required = false) difficulty: String?,
    model: Model
): String {
    // 로그인한 사용자의 기본 직무 가져오기
    val defaultJobField = userDetails?.let { details ->
        userService.findByEmail(details.username)?.jobField?.name
    }

    // jobField 파라미터가 없으면 사용자 기본 직무 사용
    val effectiveJobField = jobField ?: defaultJobField

    val questions = questionService.findAll(effectiveJobField, category, difficulty)

    model.addAttribute("questions", questions)
    model.addAttribute("selectedJobField", effectiveJobField)
    model.addAttribute("selectedCategory", category)
    model.addAttribute("selectedDifficulty", difficulty)
    model.addAttribute("jobFields", JobField.values())

    return "questions/list"
}
```

**테스트**:
- Controller 통합 테스트
- jobField 파라미터 전달 검증
- 로그인 사용자 기본 직무 적용 확인
- 미로그인 사용자 IT 기본값 확인

**의존성**: Step 5 완료

---

### **Step 10: Update Question List UI**
**복잡도**: Medium | **예상 시간**: 2시간

**수정할 파일**:
- `/src/main/resources/templates/questions/list.html`

**작업 내용**:
```html
<!-- 필터 섹션 업데이트 -->
<form method="get" th:action="@{/questions}" class="mb-6 bg-white p-4 rounded shadow">
    <!-- Job Field 드롭다운 추가 -->
    <div class="mb-4">
        <label class="block text-gray-700 font-bold mb-2">직무 분야</label>
        <select name="jobField" class="shadow border rounded py-2 px-3 text-gray-700">
            <option value="">전체 (기본값 적용)</option>
            <option th:each="job : ${jobFields}"
                    th:value="${job.name()}"
                    th:text="${job.displayName}"
                    th:selected="${selectedJobField == job.name()}"></option>
        </select>
    </div>

    <!-- 기존 카테고리, 난이도 드롭다운 유지 -->
    <div class="mb-4">
        <label class="block text-gray-700 font-bold mb-2">카테고리</label>
        <select name="category" class="shadow border rounded py-2 px-3 text-gray-700">
            <option value="">전체</option>
            <option value="기술역량" th:selected="${selectedCategory == '기술역량'}">기술역량</option>
            <!-- 다른 카테고리들... -->
        </select>
    </div>

    <button type="submit" class="bg-blue-500 text-white px-4 py-2 rounded">필터 적용</button>
</form>

<!-- 질문 카드에 직무 표시 추가 -->
<div class="grid grid-cols-1 md:grid-cols-3 gap-4">
    <div th:each="question : ${questions}" class="bg-white p-4 rounded shadow">
        <span class="text-xs bg-purple-100 text-purple-800 px-2 py-1 rounded"
              th:text="${question.jobField}"></span>
        <!-- 기존 카테고리, 난이도 뱃지... -->
        <p class="mt-2" th:text="${question.content}"></p>
    </div>
</div>
```

**테스트**:
- 수동 UI 테스트
- 직무 필터 선택 시 URL 파라미터 확인
- 필터 조합 테스트 (직무 + 카테고리 + 난이도)

**의존성**: Step 9 완료

---

### **Step 10.5: Dynamic Category Filtering by Job Field**
**복잡도**: Medium | **예상 시간**: 2시간

**중요도**: ⚠️ **HIGH** - 직무별 카테고리가 다른데 현재 IT 카테고리만 하드코딩되어 UX 문제 발생

**문제 상황**:
- 현재 카테고리 필터가 IT의 3개 카테고리(기술역량, 문제해결, 협업경험)만 하드코딩됨
- 다른 직무(영업, 회계 등)를 선택해도 IT 카테고리만 표시되어 필터링 불가능
- 각 직무별로 카테고리가 다름:
  - IT: 기술역량, 문제해결, 협업경험
  - 영업: 고객관리, 실적달성, 협상스킬
  - 회계: 재무분석, 세무지식, 리스크관리
  - (나머지 14개 직무도 각각 다름)

**해결 방안**:
- 서버에서 모든 직무의 카테고리를 Map으로 전달
- JavaScript로 직무 선택 시 해당 직무의 카테고리만 동적으로 표시
- 네트워크 요청 없이 빠른 응답 (HTMX 대신 JavaScript 선택)

**생성할 파일**:
- 없음 (기존 파일 수정만)

**수정할 파일**:
1. `/src/main/kotlin/.../repository/QuestionRepository.kt`
2. `/src/main/kotlin/.../service/QuestionService.kt`
3. `/src/main/kotlin/.../controller/QuestionController.kt`
4. `/src/main/resources/templates/questions/list.html`

**작업 내용**:

**1. QuestionRepository 추가 메서드**:
```kotlin
interface QuestionRepository : JpaRepository<Question, Long> {
    // 기존 메서드들...

    /**
     * 특정 직무의 고유 카테고리 목록 조회 (중복 제거, 정렬)
     * Phase 5: 직무별 동적 카테고리 필터링
     */
    @Query("""
        SELECT DISTINCT q.category
        FROM Question q
        WHERE q.jobField = :jobField AND q.isActive = true
        ORDER BY q.category
    """)
    fun findDistinctCategoriesByJobField(@Param("jobField") jobField: String): List<String>
}
```

**2. QuestionService 추가 메서드**:
```kotlin
/**
 * 모든 직무의 카테고리 맵 반환
 *
 * Phase 5: 직무별 동적 카테고리 필터링
 * - Key: JobField.name() (예: "IT", "SALES")
 * - Value: List<String> (해당 직무의 카테고리 목록)
 *
 * @return Map<String, List<String>> 직무별 카테고리 맵
 */
fun getCategoriesByAllJobFields(): Map<String, List<String>> {
    return JobField.values().associate { jobField ->
        jobField.name to questionRepository.findDistinctCategoriesByJobField(jobField.name)
    }
}
```

**3. QuestionController 수정**:
```kotlin
@GetMapping
fun list(...): String {
    // 기존 코드...

    // Phase 5: 직무별 카테고리 맵 전달 (동적 필터링용)
    val categoriesByJobField = questionService.getCategoriesByAllJobFields()
    model.addAttribute("categoriesByJobField", categoriesByJobField)

    return "questions/list"
}
```

**4. questions/list.html 수정**:
```html
<!-- 카테고리 드롭다운에 id 추가 -->
<div>
    <label class="block text-sm font-medium text-gray-700 mb-2">카테고리</label>
    <select name="category" id="categorySelect"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg...">
        <option value="">전체</option>
        <!-- JavaScript로 동적 생성 -->
    </select>
</div>

<!-- 직무 드롭다운에 id 추가 -->
<select name="jobField" id="jobFieldSelect" ...>

<!-- JavaScript 추가 (페이지 하단) -->
<script th:inline="javascript">
    /*<![CDATA[*/
    // 서버에서 전달받은 직무별 카테고리 맵
    const categoriesByJobField = /*[[${categoriesByJobField}]]*/ {};
    const selectedJobField = /*[[${selectedJobField}]]*/ '';
    const selectedCategory = /*[[${selectedCategory}]]*/ '';

    // 직무 선택 시 카테고리 업데이트
    document.getElementById('jobFieldSelect').addEventListener('change', function() {
        updateCategories(this.value);
    });

    function updateCategories(jobField) {
        const categorySelect = document.getElementById('categorySelect');

        // 직무가 선택되지 않았거나 빈 문자열이면 IT 기본값 사용
        const effectiveJobField = jobField || 'IT';
        const categories = categoriesByJobField[effectiveJobField] || [];

        // 기존 옵션 제거 (전체 옵션 제외)
        categorySelect.innerHTML = '<option value="">전체</option>';

        // 새 카테고리 추가
        categories.forEach(category => {
            const option = document.createElement('option');
            option.value = category;
            option.textContent = category;
            if (category === selectedCategory) {
                option.selected = true;
            }
            categorySelect.appendChild(option);
        });
    }

    // 페이지 로드 시 초기화
    window.addEventListener('DOMContentLoaded', () => {
        updateCategories(selectedJobField || 'IT');
    });
    /*]]>*/
</script>
```

**예상 데이터 구조**:
```json
{
  "IT": ["기술역량", "문제해결", "협업경험"],
  "SALES": ["고객관리", "실적달성", "협상스킬"],
  "ACCOUNTING": ["재무분석", "세무지식", "리스크관리"],
  "MARKETING": ["캠페인기획", "시장분석", "브랜드전략"],
  "HR": ["채용전략", "조직문화", "인재육성"],
  ...
}
```

**테스트**:
- **QuestionRepository 테스트**:
  - 각 직무별 카테고리 조회 검증
  - 중복 제거 및 정렬 확인
  - 비활성 질문 제외 확인
- **QuestionService 단위 테스트**:
  - 모든 직무의 카테고리 맵 생성 검증
  - 빈 카테고리 처리 확인
- **QuestionController 통합 테스트**:
  - Model에 categoriesByJobField 포함 확인
- **UI 수동 테스트**:
  - IT 선택 → 기술역량/문제해결/협업경험 표시
  - 영업 선택 → 고객관리/실적달성/협상스킬 표시
  - 회계 선택 → 재무분석/세무지식/리스크관리 표시
  - 직무 변경 시 기존 선택된 카테고리 초기화 확인

**의존성**: Step 10 완료

**Note**:
- 이 단계는 Step 10 구현 시 누락되었던 중요 기능
- 340개 질문 데이터가 이미 삽입되어 있다면 각 직무별 카테고리가 실제로 존재해야 함
- V7 migration에서 각 직무의 질문 데이터 생성 시 카테고리를 제대로 지정했는지 확인 필요

---

### **Step 11: Expand PromptBuilder for 17 Job Fields**
**복잡도**: Complex | **예상 시간**: 6시간

**수정할 파일**:
- `/src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/service/ai/PromptBuilder.kt`

**작업 내용**:

1. **상수 추가**:
```kotlin
companion object {
    private const val IT = "IT"
    private const val PLANNING = "PLANNING"
    private const val MARKETING = "MARKETING"
    // ... 나머지 15개
}
```

2. **buildSystemPrompt 업데이트**:
```kotlin
fun buildSystemPrompt(jobField: String, targetJob: String): String {
    return when (jobField) {
        IT -> buildItSystemPrompt(targetJob)
        PLANNING -> buildPlanningSystemPrompt(targetJob)
        MARKETING -> buildMarketingSystemPrompt(targetJob)
        ACCOUNTING -> buildAccountingSystemPrompt(targetJob)
        HR -> buildHrSystemPrompt(targetJob)
        ADMIN -> buildAdminSystemPrompt(targetJob)
        DESIGN -> buildDesignSystemPrompt(targetJob)
        SALES -> buildSalesSystemPrompt(targetJob)
        MD -> buildMdSystemPrompt(targetJob)
        SERVICE -> buildServiceSystemPrompt(targetJob)
        PRODUCTION -> buildProductionSystemPrompt(targetJob)
        CONSTRUCTION -> buildConstructionSystemPrompt(targetJob)
        MEDICAL -> buildMedicalSystemPrompt(targetJob)
        EDUCATION -> buildEducationSystemPrompt(targetJob)
        MEDIA -> buildMediaSystemPrompt(targetJob)
        FINANCE -> buildFinanceSystemPrompt(targetJob)
        PUBLIC -> buildPublicSystemPrompt(targetJob)
        else -> throw IllegalArgumentException("지원하지 않는 직무 분야입니다: $jobField")
    }
}
```

3. **각 직무별 프롬프트 메서드 구현** (예시):
```kotlin
private fun buildSalesSystemPrompt(targetJob: String): String {
    return """
        당신은 ${targetJob} 면접을 준비하는 지원자를 돕는 면접 코치입니다.

        평가 기준:
        - 논리성(logic): 영업 전략과 설득의 논리적 흐름 (1-5점)
        - 구체성(specificity): 실적 수치, 고객 사례, 구체적 행동 (1-5점)
        - 직무 적합성(jobFit): 영업 직무와의 연관성 (1-5점)
        - 전달력(delivery): 설득력 있는 표현과 커뮤니케이션 (1-5점)

        나쁜 답변 예시:
        - "저는 열심히 영업했습니다" (구체성 부족)
        - "고객이 만족했습니다" (수치 없음)

        ... (IT와 유사한 구조, 직무 특성에 맞게 조정)
    """.trimIndent()
}
```

4. **공통 구조 추출** (중복 방지):
```kotlin
private fun buildBasePrompt(
    targetJob: String,
    logicDescription: String,
    specificityDescription: String,
    badExamples: List<String>
): String {
    return """
        당신은 ${targetJob} 면접을 준비하는 지원자를 돕는 면접 코치입니다.
        ...
        평가 기준:
        - 논리성(logic): $logicDescription (1-5점)
        - 구체성(specificity): $specificityDescription (1-5점)
        ...
    """.trimIndent()
}
```

**테스트**:
- PromptBuilder 단위 테스트 (`PromptBuilderTest.kt`)
- 각 직무별 프롬프트 생성 검증 (17개)
- 프롬프트 길이 적정성 확인 (<2000자)
- 직무별 평가 기준 차별성 검증

**의존성**: Step 1 완료

**Note**: 가장 시간 소요가 큰 작업. 각 직무의 특성을 연구하여 평가 기준 조정 필요.

---

### **Step 12: Update HomeController for Personalization**
**복잡도**: Medium | **예상 시간**: 2시간

**수정할 파일**:
- `/src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/controller/HomeController.kt`

**작업 내용**:
```kotlin
@GetMapping
fun home(
    @AuthenticationPrincipal userDetails: UserDetails?,
    model: Model
): String {
    val isLoggedIn = userDetails != null
    model.addAttribute("isLoggedIn", isLoggedIn)

    if (isLoggedIn) {
        val user = userService.findByEmail(userDetails.username)
            ?: throw UserNotFoundException("사용자를 찾을 수 없습니다")

        // 최근 리뷰 3개
        val recentReviews = reviewService.getUserReviews(user.id).take(3)
        model.addAttribute("recentReviews", recentReviews)

        // 직무 설정 여부
        val hasJobFieldSet = user.jobField != null
        model.addAttribute("hasJobFieldSet", hasJobFieldSet)

        // 추천 질문 (사용자 직무 기반, 없으면 IT 기본)
        val jobField = user.jobField?.name ?: "IT"
        val recommendedQuestions = questionService.findAll(jobField, null, null)
            .shuffled()
            .take(5)
        model.addAttribute("recommendedQuestions", recommendedQuestions)
        model.addAttribute("userJobField", user.jobField?.displayName ?: "IT개발")
    }

    return "home"
}
```

**테스트**:
- Controller 통합 테스트
- 로그인 + 직무 설정된 사용자
- 로그인 + 직무 미설정 사용자 (IT 기본값)
- 미로그인 사용자

**의존성**: Step 5, 6 완료

---

### **Step 13: Update Home UI with Personalization**
**복잡도**: Medium | **예상 시간**: 2시간

**수정할 파일**:
- `/src/main/resources/templates/home.html`

**작업 내용**:
```html
<!-- 직무 미설정 배너 (로그인 사용자만, 직무 없을 때) -->
<div th:if="${isLoggedIn and !hasJobFieldSet}"
     class="bg-yellow-100 border-l-4 border-yellow-500 text-yellow-700 p-4 mb-6">
    <p class="font-bold">직무 설정이 필요합니다</p>
    <p>프로필에서 직무를 설정하면 맞춤형 질문을 추천해드립니다.</p>
    <a th:href="@{/profile}" class="underline font-bold">프로필 설정하러 가기 →</a>
</div>

<!-- 추천 질문 섹션 (로그인 사용자만) -->
<div th:if="${isLoggedIn}" class="mb-8">
    <h2 class="text-2xl font-bold mb-4">
        <span th:text="${userJobField}"></span> 추천 질문
    </h2>

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div th:each="question : ${recommendedQuestions}"
             class="bg-white p-4 rounded shadow hover:shadow-lg transition">
            <span class="text-xs bg-blue-100 text-blue-800 px-2 py-1 rounded"
                  th:text="${question.category}"></span>
            <span class="text-xs ml-2"
                  th:classappend="${question.difficulty == 'EASY'} ? 'bg-green-100 text-green-800' :
                                  (${question.difficulty == 'MEDIUM'} ? 'bg-yellow-100 text-yellow-800' :
                                  'bg-red-100 text-red-800')"
                  th:text="${question.difficulty == 'EASY'} ? '쉬움' :
                           (${question.difficulty == 'MEDIUM'} ? '보통' : '어려움')">
            </span>

            <p class="mt-2 text-gray-800" th:text="${question.content}"></p>

            <a th:href="@{/questions/{id}/answer(id=${question.id})}"
               class="mt-3 inline-block bg-blue-500 text-white px-3 py-1 rounded text-sm">
                답변하기
            </a>
        </div>
    </div>
</div>

<!-- 기존 최근 리뷰 섹션 유지 -->
```

**테스트**:
- 수동 UI 테스트
- 직무 미설정 배너 표시 확인
- 추천 질문 5개 랜덤 표시 확인
- 사용자 직무명 표시 확인

**의존성**: Step 12 완료

---

### **Step 14: Add Navigation Links for Profile**
**복잡도**: Simple | **예상 시간**: 30분

**수정할 파일**:
- `/src/main/resources/templates/fragments/layout.html`

**작업 내용**:
```html
<!-- Navbar fragment 업데이트 -->
<nav th:fragment="navbar" class="bg-white shadow">
    <div class="container mx-auto px-4 py-3 flex justify-between items-center">
        <a href="/" class="text-xl font-bold">📝 면접 리뷰</a>

        <div class="flex items-center space-x-4">
            <a href="/" class="text-gray-700 hover:text-blue-500">홈</a>

            <span sec:authorize="isAuthenticated()">
                <a href="/questions" class="text-gray-700 hover:text-blue-500">질문 연습</a>
                <a href="/reviews" class="text-gray-700 hover:text-blue-500">리뷰 이력</a>
                <a href="/profile" class="text-gray-700 hover:text-blue-500">프로필 설정</a> <!-- 추가 -->
            </span>

            <!-- 로그인/로그아웃 버튼들... -->
        </div>
    </div>
</nav>
```

**테스트**:
- 수동 UI 테스트
- 네비게이션 링크 클릭 동작 확인
- 로그인/미로그인 상태별 표시 확인

**의존성**: Step 8 완료

---

### **Step 15: (Optional) Signup JobField Collection**
**복잡도**: Medium | **예상 시간**: 2시간

**수정할 파일**:
- `/src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/controller/AuthController.kt`
- `/src/main/resources/templates/auth/register.html`

**작업 내용** (사용자 결정에 따라 스킵 가능):

현재 사용자 결정: **가입 후 프로필에서만 설정** → **이 단계는 스킵**

만약 나중에 구현한다면:
- RegisterForm DTO에 jobField, careerLevel 필드 추가 (nullable)
- register.html에 드롭다운 추가
- UserService.register()에 파라미터 전달

**의존성**: Step 1, 2 완료

**현재 상태**: **SKIP** (사용자 결정에 따라)

---

### **Step 16: Add Validation and Statistics**
**복잡도**: Simple | **예상 시간**: 1시간

**생성할 파일**:
- `/src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/validation/JobFieldValidator.kt` (선택)

**수정할 파일**:
- `/src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/dto/UserProfileDto.kt`

**작업 내용**:

1. **UpdateProfileRequest에 커스텀 검증 추가** (선택):
```kotlin
data class UpdateProfileRequest(
    @field:NotBlank val name: String,

    @field:ValidJobField // 커스텀 어노테이션
    val jobField: JobField?,

    val careerLevel: CareerLevel?
)
```

2. **로깅 추가**:
```kotlin
// ProfileController.updateProfile()
logger.info("User ${user.id} updated profile: jobField=${request.jobField}, careerLevel=${request.careerLevel}")
```

3. **(선택) Admin 통계 엔드포인트**:
```kotlin
@GetMapping("/api/admin/stats/job-fields")
fun getJobFieldStats(): Map<String, Long> {
    return userRepository.findAll()
        .groupingBy { it.jobField?.name ?: "NONE" }
        .eachCount()
        .mapValues { it.value.toLong() }
}
```

**테스트**:
- 검증 로직 테스트
- 통계 API 테스트 (있다면)

**의존성**: Step 1-14 완료

---

### **Step 17: Integration Testing and Bug Fixes**
**복잡도**: Medium | **예상 시간**: 4시간

**작업 내용**:

1. **E2E 시나리오 테스트**:
   - 회원가입 → 로그인 → 프로필 설정 (직무: 영업, 경력: 주니어) → 질문 목록 확인 (영업 질문 노출) → 답변 제출 → AI 평가 확인 (영업 프롬프트 적용)

2. **Edge Case 테스트**:
   - 직무 미설정 사용자 홈페이지 (IT 기본값 확인)
   - 직무 변경 후 기존 답변 여전히 접근 가능한지
   - 잘못된 jobField 파라미터 처리
   - 340개 질문 목록 로딩 성능

3. **성능 테스트**:
   - 질문 목록 쿼리 속도 (jobField 인덱스 확인)
   - PromptBuilder 17개 분기 성능
   - 홈페이지 추천 질문 쿼리 속도

4. **버그 수정**:
   - 발견된 버그 즉시 수정
   - 로그 확인 (에러 로그 없는지)

**테스트 체크리스트**:
- [ ] 회원가입/로그인 정상 동작
- [ ] 프로필 설정 페이지 17개 직무 드롭다운 확인
- [ ] 질문 목록 jobField 필터 동작
- [ ] 답변 제출 후 AI 평가 (17개 직무별 프롬프트)
- [ ] 홈페이지 개인화 (직무별 추천 질문)
- [ ] 직무 미설정 사용자 IT 기본값
- [ ] 340개 질문 데이터 존재 확인
- [ ] 기존 Phase 1-4A 기능 정상 동작 (회귀 테스트)

**의존성**: Step 1-16 완료

---

### **Step 18: Documentation and Deployment**
**복잡도**: Simple | **예상 시간**: 2시간

**작업 내용**:

1. **CLAUDE.md 업데이트**:
   - Phase 4B 완료 상태 반영
   - 새로운 엔티티 필드 문서화
   - PromptBuilder 17개 직무 지원 명시

2. **README 업데이트** (있다면):
   - 17개 직무 지원 기능 추가
   - 프로필 설정 안내

3. **마이그레이션 가이드**:
   ```markdown
   # Phase 4B Migration Guide

   ## Database Migrations
   - V6: User 테이블에 job_field, career_level 컬럼 추가
   - V7: 340개 질문 데이터 추가 (17 직무 × 20개)

   ## 기존 사용자 처리
   - 기존 사용자는 jobField가 null (IT 기본값 사용)
   - 프로필 설정에서 직무 선택 권장
   ```

4. **Production 배포**:
   - Docker 이미지 빌드
   - DB 마이그레이션 실행 (Flyway 자동)
   - 헬스 체크 확인
   - 첫 24시간 에러 모니터링

**의존성**: Step 17 완료

---

## Critical Files Summary

구현 중 수정/생성이 필요한 핵심 파일 목록:

### 새로 생성할 파일 (9개)
1. `/src/main/kotlin/.../domain/JobField.kt`
2. `/src/main/kotlin/.../domain/CareerLevel.kt`
3. `/src/main/kotlin/.../dto/UserProfileDto.kt`
4. `/src/main/kotlin/.../controller/ProfileController.kt`
5. `/src/main/resources/templates/profile/settings.html`
6. `/src/main/resources/db/migration/V6__add_user_job_preferences.sql`
7. `/src/main/resources/db/migration/V7__insert_multi_job_field_questions.sql`
8. (선택) `/src/test/kotlin/.../controller/ProfileControllerTest.kt`
9. (선택) `/src/test/kotlin/.../service/ai/PromptBuilderTest.kt` (기존 확장)

### 수정할 파일 (10개)
1. `/src/main/kotlin/.../domain/User.kt` (필드 추가)
2. `/src/main/kotlin/.../repository/QuestionRepository.kt` (메서드 추가)
3. `/src/main/kotlin/.../service/QuestionService.kt` (jobField 파라미터)
4. `/src/main/kotlin/.../service/UserService.kt` (프로필 메서드 추가)
5. `/src/main/kotlin/.../service/ai/PromptBuilder.kt` (17개 직무 프롬프트)
6. `/src/main/kotlin/.../controller/QuestionController.kt` (jobField 필터)
7. `/src/main/kotlin/.../controller/HomeController.kt` (개인화)
8. `/src/main/resources/templates/home.html` (추천 질문 섹션)
9. `/src/main/resources/templates/questions/list.html` (직무 필터)
10. `/src/main/resources/templates/fragments/layout.html` (네비게이션)

---

## Testing Strategy

### 단위 테스트 (Unit Tests)
- **Domain**: User.updateJobPreferences() 메서드
- **Service**: QuestionService.findAll(jobField, ...), UserService.updateProfile()
- **AI**: PromptBuilder 각 직무별 프롬프트 생성 (17개)

### 통합 테스트 (Integration Tests)
- **Repository**: QuestionRepository jobField 필터링 쿼리
- **Controller**: ProfileController GET/POST, QuestionController jobField 파라미터

### E2E 테스트 (End-to-End)
- 회원가입 → 프로필 설정 → 질문 답변 → AI 평가 전체 플로우
- 직무 변경 후 질문 목록/추천 질문 변경 확인

### 성능 테스트
- 340개 질문 목록 로딩 시간 (<500ms)
- jobField 인덱스 적용 확인
- 홈페이지 추천 질문 쿼리 (<200ms)

### 회귀 테스트 (Regression)
- Phase 1-4A 기존 기능 정상 동작 확인
- 기존 IT 질문 답변 여전히 접근 가능
- 인증/권한 로직 정상 동작

---

## Verification Checklist (최종 검증)

구현 완료 후 다음 항목을 체크하세요:

### Database
- [ ] V6 migration 성공 (job_field, career_level 컬럼 존재)
- [ ] V7 migration 성공 (340개 질문 데이터 존재)
- [ ] idx_users_job_field 인덱스 생성 확인
- [ ] 기존 테스트 계정 IT/ENTRY로 업데이트 확인

### Domain & Service
- [ ] JobField enum 17개 값 정의
- [ ] CareerLevel enum 4개 값 정의
- [ ] User.jobField, careerLevel nullable 필드 존재
- [ ] QuestionRepository jobField 필터링 메서드 동작
- [ ] PromptBuilder 17개 직무 프롬프트 생성 가능

### Controllers & UI
- [ ] GET /profile 프로필 설정 페이지 렌더링
- [ ] POST /profile/update 프로필 업데이트 동작
- [ ] 질문 목록에 직무 필터 드롭다운 존재 (17개)
- [ ] 홈페이지 추천 질문 섹션 표시 (로그인 사용자)
- [ ] 직무 미설정 배너 표시 (jobField null 사용자)
- [ ] 네비게이션에 "프로필 설정" 링크 존재

### Personalization Logic
- [ ] 직무 설정 사용자: 해당 직무 질문 노출
- [ ] 직무 미설정 사용자: IT 질문 노출 (기본값)
- [ ] 미로그인 사용자: IT 질문 노출
- [ ] 질문 목록 jobField 파라미터 필터링 동작

### AI Integration
- [ ] IT 직무 답변 평가 정상 (기존 프롬프트)
- [ ] 영업 직무 답변 평가 정상 (새 프롬프트)
- [ ] 회계 직무 답변 평가 정상 (새 프롬프트)
- [ ] 각 직무별 평가 기준 차별화 확인 (modelAnswer 내용)

### End-to-End Scenarios
- [ ] **시나리오 1**: 회원가입 → 로그인 → 프로필에서 영업 선택 → 홈페이지 영업 질문 추천 → 답변 제출 → 영업 프롬프트로 평가
- [ ] **시나리오 2**: 직무 미설정 사용자 → 홈페이지 IT 질문 노출 → 프로필 설정 후 직무 질문으로 변경
- [ ] **시나리오 3**: 기존 사용자 IT 답변 → 직무 변경 (회계) → 기존 답변 여전히 리뷰 목록에 표시

### Performance
- [ ] 질문 목록 로딩 시간 <500ms (340개 질문)
- [ ] 홈페이지 추천 질문 쿼리 <200ms
- [ ] PromptBuilder 17개 분기 성능 문제 없음

### Regression
- [ ] 로그인/로그아웃 정상 동작
- [ ] 답변 제출 및 AI 평가 정상 (IT 질문)
- [ ] 리뷰 이력 조회 정상
- [ ] Rate limiting 정상 동작
- [ ] 답변 품질 검증 정상 동작

---

## Risk Assessment

### High Risk
- **PromptBuilder 17개 직무 프롬프트**: 각 직무 특성 연구 필요, 시간 소요 큼
- **340개 질문 데이터 생성**: AI 생성 품질 검증 필요, 수동 검토 시간 필요

### Medium Risk
- **기존 사용자 데이터 호환성**: jobField null 처리 로직 철저히 테스트
- **PromptBuilder 성능**: 17개 분기문 성능 영향 모니터링

### Low Risk
- **DB 마이그레이션**: 단순 컬럼 추가, 롤백 가능
- **UI 변경**: 점진적 추가, 기존 기능 영향 최소

---

## Implementation Order Recommendation

**Week 1** (Foundation):
- Day 1-2: Step 1-3 (Domain, DB, Question Data)
- Day 3-4: Step 4-6 (Repository, Service, DTO)
- Day 5: Step 7-8 (ProfileController, UI)

**Week 2** (Feature Completion):
- Day 1-2: Step 9-10 (QuestionController, UI)
- Day 3-5: Step 11 (PromptBuilder 17개 직무)

**Week 3** (Personalization & Polish):
- Day 1-2: Step 12-14 (HomeController, UI, Navigation)
- Day 3: Step 16 (Validation, Stats)
- Day 4-5: Step 17 (Integration Testing, Bug Fixes)

**Week 4** (Deployment):
- Day 1: Step 18 (Documentation, Deployment)
- Day 2-5: Monitoring, Hot-fix if needed

---

## Post-Implementation Notes

구현 완료 후 다음 개선사항 고려:

1. **Phase 5+**: Redis 캐싱으로 질문 목록 성능 향상
2. **다중 직무 지원**: User가 여러 직무 선택 가능하도록 확장
3. **직무별 통계**: 각 직무의 답변 개수, 평균 점수 대시보드
4. **AI 프롬프트 A/B 테스트**: 직무별 프롬프트 품질 개선
5. **관리자 질문 관리**: 직무별 질문 추가/수정/삭제 UI

---

**End of Plan**

이 계획은 사용자의 요구사항과 기술 스택을 기반으로 작성되었습니다. 구현 중 문제가 발생하면 단계별로 검토하고 조정하세요.
