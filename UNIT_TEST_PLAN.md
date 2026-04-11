# Phase 1 단위 테스트 계획

## 목표
- 각 레이어(Controller, Service, Repository)별 기본 단위 테스트 작성
- 핵심 비즈니스 로직 및 예외 처리 검증
- 100% coverage가 아닌 주요 기능 위주

---

## 1. Repository Layer 테스트

### QuestionRepositoryTest
**테스트 대상**: 커스텀 쿼리 메서드
- ✅ `findByIsActiveTrue()` - 활성화된 질문 조회
- ✅ `findByCategoryAndIsActiveTrue()` - 카테고리별 필터링
- ✅ `findByDifficultyAndIsActiveTrue()` - 난이도별 필터링
- ✅ `findByCategoryAndDifficultyAndIsActiveTrue()` - 복합 필터링

**테스트 전략**:
- `@DataJpaTest` 사용
- 테스트 데이터 직접 생성 (Flyway 마이그레이션 데이터 사용 가능)

### InterviewAnswerRepositoryTest
**테스트 대상**:
- ✅ `findByQuestionId()` - 특정 질문에 대한 답변 조회
- ✅ `findAllByOrderByCreatedAtDesc()` - 최신 답변 순 정렬

**테스트 전략**:
- `@DataJpaTest` 사용
- 테스트 데이터 직접 생성

### AiFeedbackRepositoryTest
**테스트 대상**:
- ✅ `findByInterviewAnswerId()` - 답변에 대한 피드백 조회
- ✅ 존재하지 않는 경우 null 반환

**테스트 전략**:
- `@DataJpaTest` 사용
- 테스트 데이터 직접 생성

---

## 2. Service Layer 테스트

### QuestionServiceTest
**테스트 대상**:
- ✅ `findAll()` - 필터 없이 전체 조회
- ✅ `findAll(category)` - 카테고리 필터링
- ✅ `findAll(difficulty)` - 난이도 필터링
- ✅ `findAll(category, difficulty)` - 복합 필터링
- ✅ `findById()` - 정상 조회
- ✅ `findById()` - 존재하지 않는 ID → QuestionNotFoundException
- ✅ `findDtoById()` - DTO 변환

**테스트 전략**:
- `@ExtendWith(MockitoExtension::class)` 사용
- Repository를 Mock으로 주입
- given-when-then 패턴

### AiFeedbackServiceTest
**테스트 대상**:
- ✅ `generateDummyFeedback()` - 답변 길이별 점수 계산
  - 500자 이상 → SCORE_HIGH (4)
  - 300자 이상 → SCORE_MEDIUM (3)
  - 300자 미만 → SCORE_LOW (2)
- ✅ `generateDummyFeedback()` - JSON 직렬화 검증
- ✅ `findByInterviewAnswerId()` - 피드백 조회

**테스트 전략**:
- `@ExtendWith(MockitoExtension::class)` 사용
- Repository, ObjectMapper Mock 주입

### InterviewServiceTest
**테스트 대상**:
- ✅ `submitAnswer()` - 정상 답변 제출 플로우
- ✅ `submitAnswer()` - questionId null → IllegalArgumentException
- ✅ `submitAnswer()` - answerText null → IllegalArgumentException
- ✅ `getAnswerWithFeedback()` - 정상 조회
- ✅ `getAnswerWithFeedback()` - 답변 없음 → AnswerNotFoundException
- ✅ `getAnswerWithFeedback()` - 피드백 없음 → FeedbackNotFoundException

**테스트 전략**:
- `@ExtendWith(MockitoExtension::class)` 사용
- 의존성 모두 Mock 주입
- 예외 시나리오 중점 테스트

### ReviewServiceTest
**테스트 대상**:
- ✅ `getReviewList()` - 정상 조회
- ✅ `getReviewList()` - Question 없는 경우 필터링
- ✅ `getReviewList()` - Feedback 없는 경우 필터링
- ✅ `getReviewList()` - averageScore 계산 검증

**테스트 전략**:
- `@ExtendWith(MockitoExtension::class)` 사용
- Repository Mock 주입

---

## 3. Controller Layer 테스트

### HomeControllerTest
**테스트 대상**:
- ✅ `GET /` - 홈 페이지 렌더링
- ✅ `GET /home` - 홈 페이지 렌더링
- ✅ 최근 답변 3개 model에 추가 검증

**테스트 전략**:
- `@WebMvcTest(HomeController::class)` 사용
- MockMvc로 HTTP 요청 테스트
- Service Mock 주입

### QuestionControllerTest
**테스트 대상**:
- ✅ `GET /questions` - 질문 목록 (필터 없음)
- ✅ `GET /questions?category=기술역량` - 카테고리 필터
- ✅ `GET /questions?difficulty=EASY` - 난이도 필터
- ✅ `GET /questions?category=기술역량&difficulty=EASY` - 복합 필터
- ✅ `GET /questions/{id}/answer` - 답변 작성 폼

**테스트 전략**:
- `@WebMvcTest(QuestionController::class)` 사용
- MockMvc + Service Mock

### AnswerControllerTest
**테스트 대상**:
- ✅ `POST /questions/{id}/answer` - 정상 답변 제출 → 리다이렉트
- ✅ `POST /questions/{id}/answer` - Validation 실패 → 에러
- ✅ `GET /answers/{id}/feedback` - 피드백 페이지

**테스트 전략**:
- `@WebMvcTest(AnswerController::class)` 사용
- MockMvc + Service Mock
- Validation 시나리오 테스트

### ReviewControllerTest
**테스트 대상**:
- ✅ `GET /reviews` - 복기 이력 목록
- ✅ `GET /reviews/{id}` - 복기 상세

**테스트 전략**:
- `@WebMvcTest(ReviewController::class)` 사용
- MockMvc + Service Mock

---

## 4. DTO 테스트 (선택적)

### FeedbackDtoTest
**테스트 대상**:
- ✅ `from()` - AiFeedback → FeedbackDto 변환
- ✅ `averageScore` - 평균 점수 계산
- ✅ `parseJsonArray()` - JSON 파싱 성공
- ✅ `parseJsonArray()` - JSON 파싱 실패 → 빈 리스트

**테스트 전략**:
- 단순 POJO 테스트
- Mock 없이 직접 테스트

---

## 테스트 파일 구조

```
src/test/kotlin/com/hojun/interviewnote/interviewnoteapi/
├── repository/
│   ├── QuestionRepositoryTest.kt
│   ├── InterviewAnswerRepositoryTest.kt
│   └── AiFeedbackRepositoryTest.kt
├── service/
│   ├── QuestionServiceTest.kt
│   ├── AiFeedbackServiceTest.kt
│   ├── InterviewServiceTest.kt
│   └── ReviewServiceTest.kt
├── controller/
│   ├── HomeControllerTest.kt
│   ├── QuestionControllerTest.kt
│   ├── AnswerControllerTest.kt
│   └── ReviewControllerTest.kt
└── dto/
    └── FeedbackDtoTest.kt
```

---

## 테스트 라이브러리

### 이미 포함된 것
- JUnit 5
- Spring Boot Test
- Mockito (Kotlin)
- AssertJ (optional)

### 추가 필요 (선택적)
```kotlin
// build.gradle.kts
testImplementation("io.mockk:mockk:1.13.8")  // Kotlin용 Mock 라이브러리 (선택)
testImplementation("org.assertj:assertj-core:3.24.2")  // AssertJ (선택)
```

**결정**: 기본 Mockito + JUnit 5 사용 (추가 라이브러리 불필요)

---

## 예상 테스트 개수

- **Repository**: 3개 클래스 × 평균 3개 테스트 = 약 9개
- **Service**: 4개 클래스 × 평균 5개 테스트 = 약 20개
- **Controller**: 4개 클래스 × 평균 4개 테스트 = 약 16개
- **DTO**: 1개 클래스 × 4개 테스트 = 약 4개

**총 약 49개 테스트 케이스**

---

## 작성 순서

1. **Repository Layer** (가장 간단, 의존성 없음)
2. **Service Layer** (Repository Mock 사용)
3. **Controller Layer** (Service Mock 사용)
4. **DTO Layer** (선택적)

---

## 참고 사항

- **given-when-then 패턴** 사용
- **테스트 메서드명**: 한글 또는 영문 (일관성 유지)
  - 예: `fun submitAnswer_shouldReturnAnswerWithFeedback_whenValidInput()`
  - 예: `fun 정상_답변_제출시_피드백과_함께_반환된다()`
- **Mock 검증**: `verify()` 사용 (필요 시)
- **예외 테스트**: `assertThrows<ExceptionType>` 사용
