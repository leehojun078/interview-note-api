# Phase 1 리팩토링 계획

## 개요

**목적**: Phase 1 코드의 품질 개선 및 Google Kotlin Style Guide 준수
**범위**: 9개 리팩토링 항목
**원칙**: 기능 변경 없이 코드 구조 및 품질만 개선

---

## 리팩토링 항목 상세

### 1. mapNotNull 제거 (필수 - 사용자 요구사항)

#### 📍 위치
- `ReviewService.kt:23`

#### 🔴 현재 코드
```kotlin
fun getReviewList(): List<ReviewSummaryDto> {
    val answers = interviewAnswerRepository.findAllByOrderByCreatedAtDesc()

    return answers.mapNotNull { answer ->
        val question = questionRepository.findById(answer.questionId).orElse(null) ?: return@mapNotNull null
        val feedback = aiFeedbackRepository.findByInterviewAnswerId(answer.id) ?: return@mapNotNull null

        val averageScore = (feedback.logicScore + feedback.specificityScore +
                feedback.jobFitScore + feedback.deliveryScore) / 4.0

        ReviewSummaryDto(
            answerId = answer.id,
            questionContent = question.content,
            category = question.category,
            answeredAt = answer.createdAt,
            averageScore = averageScore
        )
    }
}
```

#### ❌ 문제점
- `mapNotNull`과 `return@mapNotNull null` 조합은 가독성이 떨어짐
- Google Kotlin Style Guide에서는 명시적인 변환을 권장

#### ✅ 수정 방안
```kotlin
fun getReviewList(): List<ReviewSummaryDto> {
    val answers = interviewAnswerRepository.findAllByOrderByCreatedAtDesc()

    return answers.mapNotNull { answer ->
        val question = questionRepository.findById(answer.questionId).orElse(null)
        val feedback = aiFeedbackRepository.findByInterviewAnswerId(answer.id)

        if (question != null && feedback != null) {
            ReviewSummaryDto(
                answerId = answer.id,
                questionContent = question.content,
                category = question.category,
                answeredAt = answer.createdAt,
                averageScore = feedback.averageScore  // 항목2에서 추가될 확장 프로퍼티 사용
            )
        } else {
            null
        }
    }
}
```

#### 📦 영향 범위
- `ReviewService.kt` 1개 파일
- 외부 API 변경 없음

---

### 2. 평균 점수 계산 로직 중복 제거 (필수)

#### 📍 위치
- `AnswerController.kt:49-50`
- `ReviewController.kt:42-43`
- `ReviewService.kt:27-28`

#### 🔴 현재 코드
```kotlin
// AnswerController.kt:49-50
val averageScore = (feedback.logicScore + feedback.specificityScore +
        feedback.jobFitScore + feedback.deliveryScore) / 4.0

// ReviewController.kt:42-43
val averageScore = (feedback.logicScore + feedback.specificityScore +
        feedback.jobFitScore + feedback.deliveryScore) / 4.0

// ReviewService.kt:27-28
val averageScore = (feedback.logicScore + feedback.specificityScore +
        feedback.jobFitScore + feedback.deliveryScore) / 4.0
```

#### ❌ 문제점
- 동일한 계산 로직이 3곳에 중복
- 계산 방식 변경 시 3곳 모두 수정 필요 (유지보수성 저하)
- DRY(Don't Repeat Yourself) 원칙 위반

#### ✅ 수정 방안

**FeedbackDto.kt에 확장 프로퍼티 추가**:
```kotlin
// FeedbackDto.kt
data class FeedbackDto(
    val logicScore: Int,
    val specificityScore: Int,
    val jobFitScore: Int,
    val deliveryScore: Int,
    val strengths: List<String>,
    val improvements: List<String>,
    val modelAnswer: String,
    val overallComment: String
) {
    /**
     * 4가지 평가 점수의 평균
     */
    val averageScore: Double
        get() = (logicScore + specificityScore + jobFitScore + deliveryScore) / 4.0

    companion object {
        // ... 기존 코드
    }
}
```

**AiFeedback 엔티티에도 추가 (ReviewService에서 직접 사용)**:
```kotlin
// AiFeedback.kt
@Entity
@Table(name = "ai_feedbacks")
class AiFeedback(
    // ... 기존 필드들
) {
    /**
     * 4가지 평가 점수의 평균
     */
    val averageScore: Double
        get() = (logicScore + specificityScore + jobFitScore + deliveryScore) / 4.0
}
```

**사용처 수정**:
```kotlin
// AnswerController.kt
model.addAttribute("averageScore", answerWithFeedback.feedback.averageScore)

// ReviewController.kt
model.addAttribute("averageScore", answerWithFeedback.feedback.averageScore)

// ReviewService.kt
ReviewSummaryDto(
    // ...
    averageScore = feedback.averageScore
)
```

#### 📦 영향 범위
- `FeedbackDto.kt` - 확장 프로퍼티 추가
- `AiFeedback.kt` - 확장 프로퍼티 추가
- `AnswerController.kt` - 계산 로직 제거
- `ReviewController.kt` - 계산 로직 제거
- `ReviewService.kt` - 계산 로직 제거

---

### 3. ObjectMapper 중복 생성 → ObjectMapperConfig로 중앙화 (필수)

#### 📍 위치
- `AiFeedbackService.kt:17`
- `FeedbackDto.kt:17`

#### 🔴 현재 코드
```kotlin
// AiFeedbackService.kt:17
private val objectMapper = jacksonObjectMapper()

// FeedbackDto.kt:17
private val objectMapper = jacksonObjectMapper()
```

#### ❌ 문제점
- `ObjectMapper` 인스턴스를 각각 생성하여 메모리 낭비
- 중앙화된 설정 관리 불가
- ObjectMapper 설정 변경 시 여러 곳 수정 필요

#### ✅ 수정 방안

**1) ObjectMapperConfig.kt 생성 (신규 파일)**:
```kotlin
// config/ObjectMapperConfig.kt
package com.hojun.interviewnote.interviewnoteapi.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ObjectMapperConfig {

    companion object {
        /**
         * 중앙화된 ObjectMapper 인스턴스
         * - Spring Bean으로도 사용 (DI)
         * - Companion object로도 사용 (DTO static 메서드)
         */
        val objectMapper: ObjectMapper =
            jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    @Bean
    fun objectMapper(): ObjectMapper = objectMapper
}
```

**2) AiFeedbackService.kt - Bean 주입**:
```kotlin
@Service
@Transactional
class AiFeedbackService(
    private val aiFeedbackRepository: AiFeedbackRepository,
    private val objectMapper: ObjectMapper  // Spring Bean 주입
) {
    // private val objectMapper = jacksonObjectMapper() 제거

    fun generateDummyFeedback(answer: InterviewAnswer, question: Question): AiFeedback {
        // ... 기존 로직 동일
        strengths = objectMapper.writeValueAsString(strengths.take(2)),
        improvements = objectMapper.writeValueAsString(improvements.take(2)),
        // ...
    }
}
```

**3) FeedbackDto.kt - companion object에서 ObjectMapperConfig 직접 사용**:
```kotlin
// FeedbackDto.kt
package com.hojun.interviewnote.interviewnoteapi.dto

import com.hojun.interviewnote.interviewnoteapi.config.ObjectMapperConfig
import com.hojun.interviewnote.interviewnoteapi.domain.AiFeedback
import org.slf4j.LoggerFactory

data class FeedbackDto(
    val logicScore: Int,
    val specificityScore: Int,
    val jobFitScore: Int,
    val deliveryScore: Int,
    val strengths: List<String>,
    val improvements: List<String>,
    val modelAnswer: String,
    val overallComment: String
) {
    val averageScore: Double
        get() = (logicScore + specificityScore + jobFitScore + deliveryScore) / 4.0

    companion object {
        private val logger = LoggerFactory.getLogger(FeedbackDto::class.java)

        fun from(aiFeedback: AiFeedback): FeedbackDto {
            return FeedbackDto(
                logicScore = aiFeedback.logicScore,
                specificityScore = aiFeedback.specificityScore,
                jobFitScore = aiFeedback.jobFitScore,
                deliveryScore = aiFeedback.deliveryScore,
                strengths = parseJsonArray(aiFeedback.strengths),
                improvements = parseJsonArray(aiFeedback.improvements),
                modelAnswer = aiFeedback.modelAnswer,
                overallComment = aiFeedback.overallComment
            )
        }

        private fun parseJsonArray(json: String): List<String> {
            return try {
                ObjectMapperConfig.objectMapper.readValue(json, List::class.java) as List<String>
            } catch (e: Exception) {
                logger.warn("JSON 파싱 실패 - 원본: $json", e)
                emptyList()
            }
        }
    }
}
```

**4) InterviewService.kt 수정 (변경 없음 - FeedbackDto.from() 시그니처 동일)**:
```kotlin
@Service
@Transactional
class InterviewService(
    private val interviewAnswerRepository: InterviewAnswerRepository,
    private val questionService: QuestionService,
    private val aiFeedbackService: AiFeedbackService
    // JsonUtil 주입 불필요
) {
    fun submitAnswer(dto: AnswerSubmitDto): AnswerWithFeedbackDto {
        // ...
        return AnswerWithFeedbackDto(
            // ...
            feedback = FeedbackDto.from(aiFeedback)  // 파라미터 변경 없음
        )
    }

    fun getAnswerWithFeedback(answerId: Long): AnswerWithFeedbackDto {
        // ...
        return AnswerWithFeedbackDto(
            // ...
            feedback = FeedbackDto.from(aiFeedback)  // 파라미터 변경 없음
        )
    }
}
```

#### 💡 이 방식의 장점
1. **싱글톤 보장**: 전역적으로 하나의 ObjectMapper 인스턴스만 사용
2. **양쪽 지원**:
   - Spring DI 필요한 곳 → Bean 주입 (`AiFeedbackService`)
   - Static 접근 필요한 곳 → companion object 직접 참조 (`FeedbackDto`)
3. **불변성**: `val`로 선언되어 thread-safe
4. **설정 중앙화**: ObjectMapper 설정을 한 곳에서 관리
5. **코드 간결성**: JsonUtil 같은 중간 레이어 불필요

#### 📦 영향 범위
- `config/ObjectMapperConfig.kt` - 신규 파일 생성
- `AiFeedbackService.kt` - ObjectMapper Bean 주입
- `FeedbackDto.kt` - companion object에서 ObjectMapperConfig 사용
- `InterviewService.kt` - 변경 없음 (기존 코드 유지)

---

### 4. Magic Number 상수화 (필수)

#### 📍 위치
- `AiFeedbackService.kt:28-31, 78-79`

#### 🔴 현재 코드
```kotlin
val baseScore = when {
    answerLength >= 500 -> 4
    answerLength >= 300 -> 3
    else -> 2
}

// ...

tokenUsageInput = answerLength / 4,
tokenUsageOutput = modelAnswer.length / 4,
```

#### ❌ 문제점
- Magic Number (500, 300, 4, 3, 2, 4) 사용
- 의미가 명확하지 않고 변경 시 일일이 찾아야 함

#### ✅ 수정 방안
```kotlin
@Service
@Transactional
class AiFeedbackService(
    private val aiFeedbackRepository: AiFeedbackRepository,
    private val objectMapper: ObjectMapper
) {
    companion object {
        // 답변 길이 기준
        private const val ANSWER_LENGTH_THRESHOLD_HIGH = 500
        private const val ANSWER_LENGTH_THRESHOLD_MEDIUM = 300

        // 더미 점수
        private const val DUMMY_SCORE_HIGH = 4
        private const val DUMMY_SCORE_MEDIUM = 3
        private const val DUMMY_SCORE_LOW = 2

        // 토큰 추정 계수 (대략 4글자 = 1토큰)
        private const val TOKEN_ESTIMATION_FACTOR = 4
    }

    fun generateDummyFeedback(answer: InterviewAnswer, question: Question): AiFeedback {
        val answerLength = answer.answerText.length

        val baseScore = when {
            answerLength >= ANSWER_LENGTH_THRESHOLD_HIGH -> DUMMY_SCORE_HIGH
            answerLength >= ANSWER_LENGTH_THRESHOLD_MEDIUM -> DUMMY_SCORE_MEDIUM
            else -> DUMMY_SCORE_LOW
        }

        // ... 중간 생략 ...

        val aiFeedback = AiFeedback(
            // ...
            logicScore = baseScore,
            specificityScore = baseScore - 1,
            jobFitScore = baseScore,
            deliveryScore = baseScore - 1,
            // ...
            tokenUsageInput = answerLength / TOKEN_ESTIMATION_FACTOR,
            tokenUsageOutput = modelAnswer.length / TOKEN_ESTIMATION_FACTOR,
            // ...
        )

        return aiFeedbackRepository.save(aiFeedback)
    }
}
```

#### 📦 영향 범위
- `AiFeedbackService.kt` 1개 파일
- 외부 API 변경 없음

---

### 5. CLAUDE.md에 Google Kotlin Style Guide 추가 (필수)

#### 📍 위치
- `CLAUDE.md`

#### 🔴 현재 상태
- 코딩 스타일 가이드 명시 없음

#### ✅ 수정 방안

**CLAUDE.md의 "Coding Guidelines" 섹션에 추가**:

```markdown
## Coding Guidelines

### Code Style
- **Google Kotlin Style Guide 준수**: 모든 Kotlin 코드는 [Google Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)를 따릅니다
  - 들여쓰기: 4 spaces
  - 최대 줄 길이: 100자 (단, URL이나 긴 문자열은 예외)
  - Import 순서: 알파벳 순, 와일드카드 import 금지
  - 함수/변수명: camelCase
  - 상수명: UPPER_SNAKE_CASE
  - 클래스명: PascalCase
  - Nullable 타입: 명시적으로 `?` 사용, `!!` 최소화
  - 확장 함수/프로퍼티 적극 활용

### DO
- ✅ 작은 단위로 기능 분할
- ✅ 각 레이어의 책임 명확히 분리
- ✅ AI 응답은 항상 JSON 스키마 검증
- ✅ 원본 응답(rawResponse) 반드시 저장
- ✅ 프롬프트 버전 관리
- ✅ 테스트 작성 (최소한 Service 레이어)
- ✅ Magic Number 대신 상수 사용
- ✅ DRY 원칙 준수 (중복 코드 제거)
- ✅ Spring Bean 주입 활용 (ObjectMapper 등)

### DON'T
- ❌ Controller에서 직접 OpenAI 호출
- ❌ AI 응답을 자유 텍스트로 받기
- ❌ MVP 범위 밖 기능 추가
- ❌ 처음부터 복잡한 구조 설계
- ❌ 프론트엔드/백엔드 분리 (MVP 단계)
- ❌ 과도한 추상화나 미래 확장성 고려
- ❌ `mapNotNull`과 `return@mapNotNull null` 조합 사용
- ❌ ObjectMapper 등 중복 인스턴스 생성
- ❌ Nullable 강제 언랩핑 (`!!`) 남용
```

#### 📦 영향 범위
- `CLAUDE.md` 1개 파일

---

### 6. Nullable 강제 언랩핑 개선 (권장)

#### 📍 위치
- `InterviewService.kt:24, 29`

#### 🔴 현재 코드
```kotlin
fun submitAnswer(dto: AnswerSubmitDto): AnswerWithFeedbackDto {
    // 1. 질문 존재 여부 확인
    val question = questionService.findById(dto.questionId!!)  // !! 사용

    // 2. 답변 저장
    val answer = InterviewAnswer(
        questionId = dto.questionId,
        answerText = dto.answerText!!,  // !! 사용
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
    // ...
}
```

#### ❌ 문제점
- `!!` (강제 언랩핑) 사용은 NPE 위험
- Validation으로 이미 null 체크되었지만, 코드만 보면 위험해 보임
- Google Kotlin Style Guide에서는 `!!` 최소화 권장

#### ✅ 수정 방안
```kotlin
fun submitAnswer(dto: AnswerSubmitDto): AnswerWithFeedbackDto {
    // Elvis 연산자로 명시적 예외 처리
    val questionId = dto.questionId
        ?: throw IllegalArgumentException("질문 ID는 필수입니다")
    val answerText = dto.answerText
        ?: throw IllegalArgumentException("답변은 필수입니다")

    // 1. 질문 존재 여부 확인
    val question = questionService.findById(questionId)

    // 2. 답변 저장
    val answer = InterviewAnswer(
        questionId = questionId,
        answerText = answerText,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
    val savedAnswer = interviewAnswerRepository.save(answer)

    // 3. AI 피드백 생성 (Phase 1: 더미)
    val aiFeedback = aiFeedbackService.generateDummyFeedback(savedAnswer, question)

    // 4. 결합된 DTO 반환
    return AnswerWithFeedbackDto(
        answerId = savedAnswer.id,
        questionId = question.id,
        questionContent = question.content,
        answerText = savedAnswer.answerText,
        answeredAt = savedAnswer.createdAt,
        feedback = FeedbackDto.from(aiFeedback, jsonUtil)
    )
}
```

#### 📝 참고
- `@Valid` Validation이 먼저 실행되므로 실제로는 null이 올 수 없음
- 하지만 명시적 처리로 코드 안전성 향상

#### 📦 영향 범위
- `InterviewService.kt` 1개 파일
- 외부 API 변경 없음

---

### 7. JPA 엔티티 data class → class 변경 (권장)

#### 📍 위치
- `Question.kt`
- `InterviewAnswer.kt`
- `AiFeedback.kt`

#### 🔴 현재 코드
```kotlin
@Entity
@Table(name = "questions")
data class Question(
    // ...
)

@Entity
@Table(name = "interview_answers")
data class InterviewAnswer(
    // ...
)

@Entity
@Table(name = "ai_feedbacks")
data class AiFeedback(
    // ...
)
```

#### ❌ 문제점
- **JPA 엔티티에 data class 사용 시 문제점**:
  1. `equals()`/`hashCode()`가 모든 필드 기반 → id만 사용해야 함
  2. Lazy Loading 프록시와 충돌 가능
  3. `copy()` 메서드가 불변 객체를 가정하지만 JPA는 가변 객체
  4. 양방향 연관관계 시 `toString()` 무한 재귀 위험

#### ✅ 수정 방안

**Question.kt**:
```kotlin
@Entity
@Table(name = "questions")
class Question(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 50)
    val jobField: String = "IT",

    @Column(nullable = false, length = 100)
    val targetJob: String,

    @Column(nullable = false, length = 100)
    val category: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(nullable = false, length = 20)
    val difficulty: String,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Question) return false
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String {
        return "Question(id=$id, category='$category', difficulty='$difficulty')"
    }
}
```

**InterviewAnswer.kt**:
```kotlin
@Entity
@Table(name = "interview_answers")
class InterviewAnswer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val questionId: Long,

    @Column(nullable = false, columnDefinition = "TEXT")
    val answerText: String,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InterviewAnswer) return false
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String {
        return "InterviewAnswer(id=$id, questionId=$questionId)"
    }
}
```

**AiFeedback.kt**:
```kotlin
@Entity
@Table(name = "ai_feedbacks")
class AiFeedback(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val interviewAnswerId: Long,

    @Column(nullable = false)
    val logicScore: Int,

    @Column(nullable = false)
    val specificityScore: Int,

    @Column(nullable = false)
    val jobFitScore: Int,

    @Column(nullable = false)
    val deliveryScore: Int,

    @Column(nullable = false, columnDefinition = "TEXT")
    val strengths: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val improvements: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val modelAnswer: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val overallComment: String,

    @Column(nullable = false, length = 50)
    val jobField: String = "IT",

    @Column(nullable = false, length = 50)
    val modelName: String,

    @Column(nullable = false, length = 20)
    val promptVersion: String,

    @Column(nullable = false)
    val tokenUsageInput: Int,

    @Column(nullable = false)
    val tokenUsageOutput: Int,

    @Column(nullable = false, columnDefinition = "TEXT")
    val rawResponse: String,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * 4가지 평가 점수의 평균
     */
    val averageScore: Double
        get() = (logicScore + specificityScore + jobFitScore + deliveryScore) / 4.0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AiFeedback) return false
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String {
        return "AiFeedback(id=$id, interviewAnswerId=$interviewAnswerId, averageScore=$averageScore)"
    }
}
```

#### 📦 영향 범위
- `Question.kt` - data class → class
- `InterviewAnswer.kt` - data class → class
- `AiFeedback.kt` - data class → class
- **DB 마이그레이션 불필요** (테이블 구조 변경 없음)

---

### 8. 커스텀 예외 클래스 도입 (권장)

#### 📍 위치
- `QuestionService.kt:31`
- `InterviewService.kt:54, 59`

#### 🔴 현재 코드
```kotlin
// QuestionService.kt
return questionRepository.findById(id)
    .orElseThrow { IllegalArgumentException("질문을 찾을 수 없습니다: $id") }

// InterviewService.kt
val answer = interviewAnswerRepository.findById(answerId)
    .orElseThrow { IllegalArgumentException("답변을 찾을 수 없습니다: $answerId") }

val aiFeedback = aiFeedbackService.findByInterviewAnswerId(answerId)
    ?: throw IllegalStateException("평가 결과를 찾을 수 없습니다: $answerId")
```

#### ❌ 문제점
- 표준 예외(`IllegalArgumentException`, `IllegalStateException`) 사용
- 예외 타입만으로 어떤 도메인 오류인지 구분 어려움
- 통일된 예외 처리 전략 부재

#### ✅ 수정 방안

**1) 커스텀 예외 클래스 생성**:

```kotlin
// exception/NotFoundException.kt
package com.hojun.interviewnote.interviewnoteapi.exception

abstract class NotFoundException(message: String) : RuntimeException(message)

class QuestionNotFoundException(id: Long) :
    NotFoundException("질문을 찾을 수 없습니다: $id")

class AnswerNotFoundException(id: Long) :
    NotFoundException("답변을 찾을 수 없습니다: $id")

class FeedbackNotFoundException(answerId: Long) :
    NotFoundException("평가 결과를 찾을 수 없습니다 (답변 ID: $answerId)")
```

**2) 서비스 레이어 수정**:

```kotlin
// QuestionService.kt
import com.hojun.interviewnote.interviewnoteapi.exception.QuestionNotFoundException

fun findById(id: Long): Question {
    return questionRepository.findById(id)
        .orElseThrow { QuestionNotFoundException(id) }
}

// InterviewService.kt
import com.hojun.interviewnote.interviewnoteapi.exception.AnswerNotFoundException
import com.hojun.interviewnote.interviewnoteapi.exception.FeedbackNotFoundException

fun getAnswerWithFeedback(answerId: Long): AnswerWithFeedbackDto {
    val answer = interviewAnswerRepository.findById(answerId)
        .orElseThrow { AnswerNotFoundException(answerId) }

    val question = questionService.findById(answer.questionId)

    val aiFeedback = aiFeedbackService.findByInterviewAnswerId(answerId)
        ?: throw FeedbackNotFoundException(answerId)

    // ...
}
```

**3) 전역 예외 핸들러 추가 (향후 REST API 전환 대비)**:

```kotlin
// exception/GlobalExceptionHandler.kt
package com.hojun.interviewnote.interviewnoteapi.exception

import org.slf4j.LoggerFactory
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(e: NotFoundException, model: Model): String {
        logger.warn("Resource not found: ${e.message}")
        model.addAttribute("errorMessage", e.message)
        return "error/404"  // 향후 에러 페이지 생성
    }
}
```

#### 📦 영향 범위
- `exception/NotFoundException.kt` - 신규 파일
- `exception/GlobalExceptionHandler.kt` - 신규 파일
- `QuestionService.kt` - 예외 변경
- `InterviewService.kt` - 예외 변경

---

### 9. JSON 파싱 에러 로깅 (권장) ✅ 항목 3에 통합됨

#### 📍 위치
- `FeedbackDto.kt:33-37`

#### 🔴 현재 코드
```kotlin
// FeedbackDto.kt
private fun parseJsonArray(json: String): List<String> {
    return try {
        objectMapper.readValue(json, List::class.java) as List<String>
    } catch (e: Exception) {
        emptyList()  // 조용히 실패
    }
}
```

#### ❌ 문제점
- 예외 발생 시 조용히 빈 리스트 반환
- 디버깅 어려움 (로그 없음)
- 데이터 무결성 문제 발견 불가

#### ✅ 수정 방안

**항목 3에서 이미 처리됨 - FeedbackDto.kt에 로깅 추가**:
```kotlin
// FeedbackDto.kt
companion object {
    private val logger = LoggerFactory.getLogger(FeedbackDto::class.java)

    private fun parseJsonArray(json: String): List<String> {
        return try {
            ObjectMapperConfig.objectMapper.readValue(json, List::class.java) as List<String>
        } catch (e: Exception) {
            logger.warn("JSON 파싱 실패 - 원본: $json", e)  // ✅ 로깅 추가
            emptyList()
        }
    }
}
```

#### 📦 영향 범위
- `FeedbackDto.kt` - **항목 3에서 함께 수정됨**
- 외부 API 변경 없음

#### 📝 참고
- 이 항목은 항목 3 (ObjectMapper 중앙화)와 함께 처리됩니다
- 별도 파일 생성 불필요

---

## 리팩토링 순서

### Phase A: 기반 작업 (의존성 없음)
1. ✅ **항목5**: CLAUDE.md 업데이트
2. ✅ **항목4**: Magic Number 상수화 (`AiFeedbackService.kt`)
3. ✅ **항목7**: JPA 엔티티 data class → class 변경

### Phase B: 공통 컴포넌트 생성
4. ✅ **항목3 + 항목9**: ObjectMapperConfig 생성 + FeedbackDto 로깅 추가
5. ✅ **항목8**: 커스텀 예외 클래스 생성
6. ✅ **항목2**: 평균 점수 계산 로직 공통화 (확장 프로퍼티)

### Phase C: 서비스/컨트롤러 리팩토링
7. ✅ **항목6**: Nullable 강제 언랩핑 개선 (`InterviewService.kt`)
8. ✅ **항목1**: mapNotNull 제거 (`ReviewService.kt`)

---

## 체크리스트

### 코드 수정
- [ ] `CLAUDE.md` - Google Kotlin Style Guide 추가
- [ ] `config/ObjectMapperConfig.kt` - 신규 생성 (companion object + Bean)
- [ ] `AiFeedbackService.kt` - Magic Number 상수화 + ObjectMapper Bean 주입
- [ ] `Question.kt` - data class → class
- [ ] `InterviewAnswer.kt` - data class → class
- [ ] `AiFeedback.kt` - data class → class + averageScore 프로퍼티
- [ ] `FeedbackDto.kt` - averageScore 프로퍼티 + ObjectMapperConfig 사용 + 로깅
- [ ] `exception/NotFoundException.kt` - 신규 생성
- [ ] `exception/GlobalExceptionHandler.kt` - 신규 생성
- [ ] `QuestionService.kt` - 커스텀 예외 사용
- [ ] `InterviewService.kt` - Nullable 개선 + 커스텀 예외
- [ ] `ReviewService.kt` - mapNotNull 제거 + averageScore 프로퍼티 사용
- [ ] `AnswerController.kt` - averageScore 프로퍼티 사용
- [ ] `ReviewController.kt` - averageScore 프로퍼티 사용

### 테스트
- [ ] 기존 테스트 통과 확인 (`./gradlew test`)
- [ ] 애플리케이션 실행 확인 (`./gradlew bootRun`)
- [ ] 전체 사용자 플로우 수동 테스트
  - [ ] 질문 목록 조회
  - [ ] 답변 작성 및 제출
  - [ ] 피드백 확인
  - [ ] 복기 이력 조회

### 문서
- [ ] CLAUDE.md 업데이트 완료
- [ ] REFACTORING_PLAN.md 체크리스트 완료 표시

---

## 예상 파일 변경 목록

### 수정 파일 (11개)
1. `CLAUDE.md`
2. `src/main/kotlin/.../domain/Question.kt`
3. `src/main/kotlin/.../domain/InterviewAnswer.kt`
4. `src/main/kotlin/.../domain/AiFeedback.kt`
5. `src/main/kotlin/.../dto/FeedbackDto.kt`
6. `src/main/kotlin/.../service/AiFeedbackService.kt`
7. `src/main/kotlin/.../service/QuestionService.kt`
8. `src/main/kotlin/.../service/InterviewService.kt`
9. `src/main/kotlin/.../service/ReviewService.kt`
10. `src/main/kotlin/.../controller/AnswerController.kt`
11. `src/main/kotlin/.../controller/ReviewController.kt`

### 신규 파일 (3개)
1. `src/main/kotlin/.../config/ObjectMapperConfig.kt`
2. `src/main/kotlin/.../exception/NotFoundException.kt`
3. `src/main/kotlin/.../exception/GlobalExceptionHandler.kt`

### 총 14개 파일 변경

---

## 참고 자료
- [Google Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)
- [Effective Java - Item 10: equals/hashCode](https://github.com/jbloch/effective-java-3e-source-code)
- [Hibernate Best Practices - Entities](https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#entity)
- [Spring Boot - ObjectMapper Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.spring-mvc.customize-jackson-objectmapper)
