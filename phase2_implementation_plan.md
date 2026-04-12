# Phase 2 구현 계획서

면접 복기 웹 애플리케이션 - AI 연동 구현 가이드

## 목차
1. [개요](#1-개요)
2. [사용자 결정사항](#2-사용자-결정사항)
3. [아키텍처 설계](#3-아키텍처-설계)
4. [신규 컴포넌트 상세 설계](#4-신규-컴포넌트-상세-설계)
5. [구현 순서](#5-구현-순서)
6. [비용 제어 전략](#6-비용-제어-전략)
7. [테스트 전략](#7-테스트-전략)
8. [검증 계획](#8-검증-계획)
9. [파일 목록](#9-파일-목록)

---

## 1. 개요

### 1.1 목표

Phase 1에서 구현한 더미 피드백 시스템을 실제 OpenAI API 기반 AI 평가 시스템으로 전환합니다.

**핵심 가치**:
- ✅ **안정성**: 모든 AI 오류 시 더미 피드백으로 폴백
- ✅ **비용 최적화**: 중복 요청 방지 + Rate Limiting
- ✅ **확장성**: 인터페이스 기반 설계로 향후 다른 AI 모델 교체 용이
- ✅ **테스트 용이성**: Mock을 통한 단위/통합 테스트 가능

### 1.2 현재 상태 (Phase 1)

```kotlin
// AiFeedbackService.kt (현재)
fun generateDummyFeedback(answer: InterviewAnswer, question: Question): AiFeedback {
    // 답변 길이 기반 더미 점수 생성
    val baseScore = when {
        answerLength >= 500 -> 4
        answerLength >= 300 -> 3
        else -> 2
    }
    // 고정 문구 반환
}
```

**문제점**:
- 실제 답변 내용을 평가하지 않음
- 모든 답변에 동일한 템플릿 피드백 제공
- 실사용성 없음

### 1.3 Phase 2 완료 후 상태

```kotlin
// AiFeedbackService.kt (Phase 2 완료 후)
fun generateFeedback(answer: InterviewAnswer, question: Question): AiFeedback {
    try {
        // 1. 캐시 확인 (중복 방지)
        val cached = duplicateRequestCache.findCached(question.id, answer.answerText)
        if (cached != null) return cached

        // 2. 프롬프트 생성
        val systemPrompt = promptBuilder.buildSystemPrompt(question.jobField, question.targetJob)
        val userPrompt = promptBuilder.buildUserPrompt(question, answer.answerText)

        // 3. OpenAI API 호출
        val rawResponse = aiClient.requestFeedback(systemPrompt, userPrompt)

        // 4. 응답 파싱 및 검증
        val parsed = responseParser.parseOpenAiResponse(rawResponse)

        // 5. 엔티티 생성 및 저장
        val feedback = buildAiFeedbackEntity(answer, question, parsed, rawResponse)
        return aiFeedbackRepository.save(feedback)

    } catch (e: AiException) {
        logger.warn("AI 평가 실패, 더미 피드백으로 폴백: ${e.message}")
        return generateDummyFeedback(answer, question)
    }
}
```

---

## 2. 사용자 결정사항

### 2.1 OpenAI API 통신 방식 선택

**선택**: RestTemplate을 사용한 직접 HTTP 호출

**선택 이유**:
- ✅ 외부 의존성 최소화 (Spring Boot 기본 제공)
- ✅ 가볍고 명시적인 API 호출 제어
- ✅ JSON 요청/응답 직접 관리로 디버깅 용이
- ✅ OpenAI API 스펙 변경 시 빠른 대응 가능
- ✅ 불필요한 추상화 레이어 제거

**대안 (채택하지 않음)**:
- ❌ OpenAI Java SDK : 추가 의존성, MVP에는 과도한 기능
- ❌ Spring AI 1.1+ : 불필요한 추상화 레이어, MVP 단계에서는 과도한 복잡도
- ❌ LangChain4j : Java 중심, Kotlin DSL 지원 부족

**구현 방식**:
- `RestTemplate`으로 `https://api.openai.com/v1/chat/completions` 직접 호출
- Jackson `ObjectMapper`로 요청/응답 직렬화
- `response_format: json_object`로 구조화된 JSON 응답 강제

### 2.2 비용 제어 전략

**선택**: MVP에 포함

| 기능 | 구현 방법 | 기대 효과 |
|------|----------|----------|
| **중복 요청 방지** | (questionId, answerText) 해시 기반 24시간 캐싱 | 동일 답변 재평가 방지, 비용 절감 |
| **Rate Limiting** | IP 기반 시간당 33회 제한 | 악의적 사용 방지, 예산 초과 방지 |

**비용 추정**:
```
1회 평가 비용 (gpt-4o-mini 기준):
- Input: ~500 tokens (시스템 프롬프트 + 사용자 답변)
- Output: ~200 tokens (평가 JSON)
- 총 비용: $0.00015 / 평가

시나리오별 비용:
- 100 평가/일: $0.015/일 = $0.45/월
- 1,000 평가/일: $0.15/일 = $4.5/월

Rate Limit 적용 시 최대 비용:
- 33회/시간 × 24시간 = 792회/일
- 792회 × $0.00015 = $0.12/일 = $3.6/월 (단일 IP 기준)
```

### 2.3 문서 관리 방식

**선택**: `phase2_implementation_plan.md` 별도 생성

**이유**:
- `implementation_guide.md`는 이미 900줄 이상으로 방대
- Phase 2 전용 문서로 분리하여 관리 용이성 확보
- 향후 Phase 3, Phase 4도 별도 문서로 관리 가능

---

## 3. 아키텍처 설계

### 3.1 전체 흐름도

```
사용자 요청
    ↓
AnswerController
    ├─→ RateLimitService (IP 기반 요청 제한 확인)
    └─→ InterviewService
            ↓
        AiFeedbackService
            ├─→ DuplicateRequestCache.findCached() (캐시 확인)
            ├─→ PromptBuilder.buildSystemPrompt()
            ├─→ PromptBuilder.buildUserPrompt()
            ├─→ OpenAiClient.requestFeedback()
            ├─→ ResponseParser.parseOpenAiResponse()
            ├─→ DuplicateRequestCache.storeCached() (캐시 저장)
            └─→ AiFeedbackRepository.save()
            ↓
        AnswerWithFeedbackDto
            ↓
        Thymeleaf 템플릿 (answers/feedback.html)
```

**폴백 경로**:
- 어떤 `AiException`이든 발생 시 → `generateDummyFeedback()` 호출 → 경고 로그 기록

### 3.2 레이어 구조

```
┌─────────────────────────────────────────────────┐
│  Presentation Layer (Controller)                 │
│  - AnswerController (Rate Limit 체크)            │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│  Business Layer (Service)                        │
│  - InterviewService                              │
│  - AiFeedbackService (orchestration)             │
│  - QuestionService                               │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│  AI Integration Layer (service/ai)               │
│  - OpenAiClient (interface)                      │
│  - OpenAiClientImpl (implementation)             │
│  - PromptBuilder (prompt 생성)                   │
│  - ResponseParser (응답 검증 및 파싱)            │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│  Infrastructure Layer                            │
│  - DuplicateRequestCache (중복 방지)             │
│  - RateLimitService (요청 제한)                  │
│  - OpenAI API (외부 서비스)                      │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│  Data Layer (Repository)                         │
│  - AiFeedbackRepository                          │
│  - InterviewAnswerRepository                     │
│  - QuestionRepository                            │
└─────────────────────────────────────────────────┘
```

### 3.3 패키지 구조

```
src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/
├── config/
│   ├── ObjectMapperConfig.kt          (기존)
│   └── OpenAiConfig.kt                (신규)
├── controller/
│   └── AnswerController.kt            (수정 - Rate Limit 체크 추가)
├── service/
│   ├── AiFeedbackService.kt           (수정 - generateFeedback() 추가)
│   ├── InterviewService.kt            (수정 - 1줄 변경)
│   ├── ai/                            (신규 패키지)
│   │   ├── AiClient.kt                (신규 - 인터페이스)
│   │   ├── OpenAiClientImpl.kt        (신규)
│   │   ├── PromptBuilder.kt           (신규)
│   │   └── ResponseParser.kt          (신규)
│   ├── cache/                         (신규 패키지)
│   │   └── DuplicateRequestCache.kt   (신규)
│   └── ratelimit/                     (신규 패키지)
│       └── RateLimitService.kt        (신규)
├── exception/
│   ├── GlobalExceptionHandler.kt      (수정 - AI 예외 핸들러 추가)
│   ├── NotFoundException.kt           (기존)
│   ├── AiExceptions.kt                (신규)
│   └── RateLimitExceededException.kt  (신규)
└── domain/
    └── AiFeedback.kt                  (수정 - answerTextHash 필드 추가)
```

---

## 4. 신규 컴포넌트 상세 설계

### 4.1 OpenAiConfig - OpenAI 설정 클래스

**파일 위치**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/config/OpenAiConfig.kt`

**목적**:
- OpenAI API 키 관리
- 타임아웃, 모델명 등 설정 중앙화
- RestTemplate은 별도 Bean 없이 OpenAiClientImpl에서 직접 생성

**구현**:
```kotlin
@Configuration
@ConfigurationProperties(prefix = "openai")
class OpenAiProperties {
    lateinit var apiKey: String
    var model: String = "gpt-4o-mini"          // 기본 모델
    var promptVersion: String = "v1.0"
    var maxTokens: Int = 800
    var temperature: Double = 0.7
    var timeout: Long = 30000                   // 30초 (미사용, 향후 확장 가능)
}
```

**참고**: RestTemplate은 Spring Boot의 기본 의존성에 포함되어 있어 별도 Bean 설정이 불필요합니다.

**application.properties 추가 설정**:
```properties
# OpenAI Configuration (Phase 2)
openai.api-key=${OPENAI_API_KEY}
openai.model=gpt-4o-mini
openai.prompt-version=v1.0
openai.max-tokens=800
openai.temperature=0.7
openai.timeout=30000
```

**환경 변수 설정**:
```bash
# .env 파일 (로컬 개발용, .gitignore에 추가 필수)
export OPENAI_API_KEY=sk-proj-...

# IntelliJ Run Configuration
# Edit Configurations → Environment Variables → OPENAI_API_KEY=sk-proj-...
```

---

### 4.2 AiExceptions - AI 관련 예외 클래스

**파일 위치**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/exception/AiExceptions.kt`

**목적**:
- AI 작업 관련 예외를 세분화하여 적절한 처리 가능
- 에러 원인 추적 용이

**구현**:
```kotlin
/**
 * AI 관련 예외의 기본 클래스
 */
sealed class AiException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * OpenAI API 호출 실패 (네트워크, 인증, Rate Limit 등)
 */
class AiApiException(message: String, cause: Throwable? = null) : AiException(message, cause)

/**
 * OpenAI 응답 파싱 실패 (잘못된 JSON 형식)
 */
class AiResponseParseException(
    message: String,
    val rawResponse: String,        // 디버깅용 원본 응답 저장
    cause: Throwable? = null
) : AiException(message, cause)

/**
 * AI 요청 중 일반 오류 (타임아웃, 알 수 없는 오류)
 */
class AiRequestException(message: String, cause: Throwable? = null) : AiException(message, cause)

/**
 * AI 응답이 비어있음
 */
class AiResponseException(message: String) : AiException(message)
```

**GlobalExceptionHandler 수정**:
```kotlin
@ControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(AiException::class)
    fun handleAiException(e: AiException, model: Model): String {
        logger.error("AI 작업 실패: ${e.message}", e)

        // 파싱 오류 시 원본 응답 로깅
        if (e is AiResponseParseException) {
            logger.error("원본 AI 응답: ${e.rawResponse}")
        }

        model.addAttribute("errorMessage", "AI 평가 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
        model.addAttribute("errorDetail", e.message)
        return "error/ai-error"
    }
}
```

---

### 4.3 PromptBuilder - 프롬프트 생성기

**파일 위치**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/service/ai/PromptBuilder.kt`

**목적**:
- 직무(jobField)별 시스템 프롬프트 생성
- 질문과 답변을 포함한 사용자 프롬프트 생성
- 확장 가능한 구조 (IT → 영업, 경영 등)

**구현**:
```kotlin
@Service
class PromptBuilder(
    private val properties: OpenAiProperties
) {

    companion object {
        private const val IT_JOB_FIELD = "IT"
    }

    /**
     * 직무에 따른 시스템 프롬프트 생성
     * MVP: IT 직무만 구현, 향후 확장 가능
     */
    fun buildSystemPrompt(jobField: String, targetJob: String): String {
        return when (jobField) {
            IT_JOB_FIELD -> buildItSystemPrompt(targetJob)
            else -> throw IllegalArgumentException("지원하지 않는 직무 분야입니다: $jobField")
        }
    }

    /**
     * IT 직무용 시스템 프롬프트 (CLAUDE.md 스펙 준수)
     */
    private fun buildItSystemPrompt(targetJob: String): String {
        return """
            당신은 ${targetJob} 면접을 준비하는 지원자를 돕는 면접 코치입니다.
            당신의 역할은 합격/불합격을 판정하는 것이 아니라, 답변을 개선하도록 구체적인 피드백을 제공하는 것입니다.

            평가 기준:
            - 논리성(logic): 기술적 사고의 논리적 흐름과 일관성 (1-5점)
            - 구체성(specificity): 구체적 기술 스택, 사례, 수치 제시 정도 (1-5점)
            - 직무 적합성(jobFit): 질문 의도와 개발 직무 연관성 (1-5점)
            - 전달력(delivery): 기술 개념을 명확하고 이해하기 쉽게 설명하는 능력 (1-5점)

            출력 규칙:
            - 반드시 JSON 형식으로 응답
            - 각 점수는 1-5 사이 정수
            - strengths와 improvements는 각각 2-3개 항목
            - modelAnswer는 400-600자 이내
            - 한국어로 답변
            - 과도한 단정이나 공격적 표현 금지

            JSON 형식:
            {
              "scores": {
                "logic": 4,
                "specificity": 3,
                "jobFit": 4,
                "delivery": 3
              },
              "strengths": ["강점1", "강점2"],
              "improvements": ["개선점1", "개선점2"],
              "modelAnswer": "모범답변 내용...",
              "overallComment": "종합 코멘트"
            }
        """.trimIndent()
    }

    /**
     * 질문과 답변을 포함한 사용자 프롬프트 생성
     */
    fun buildUserPrompt(question: Question, answer: String): String {
        return """
            면접 질문:
            ${question.content}

            지원자 답변:
            $answer

            위 답변을 평가하고, JSON 형식으로 피드백을 제공해주세요.
        """.trimIndent()
    }
}
```

**확장 전략** (향후 영업 직무 추가 예시):
```kotlin
private fun buildSalesSystemPrompt(targetJob: String): String {
    return """
        당신은 ${targetJob} 면접을 준비하는 지원자를 돕는 면접 코치입니다.

        평가 기준:
        - 논리성(logic): 설득의 논리적 흐름과 일관성
        - 구체성(specificity): 구체적 실적, 수치, 사례 제시 정도
        - 직무 적합성(jobFit): 질문 의도와 영업 직무 연관성
        - 전달력(delivery): 고객/면접관에게 명확히 전달하는 능력

        ...
    """.trimIndent()
}
```

---

### 4.4 AiClient & OpenAiClientImpl - OpenAI API 추상화

**파일 위치**:
- `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/service/ai/AiClient.kt`
- `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/service/ai/OpenAiClientImpl.kt`

**목적**:
- OpenAI API 호출을 인터페이스로 추상화
- Mock 테스트 가능
- 향후 다른 AI 모델(Claude, Gemini)로 교체 용이

**인터페이스**:
```kotlin
/**
 * AI 클라이언트 인터페이스 - Mock 및 Provider 교체 가능
 */
interface AiClient {
    fun requestFeedback(systemPrompt: String, userPrompt: String): String
}
```

**구현체** (RestTemplate 기반):
```kotlin
/**
 * OpenAI API 구현체 - RestTemplate을 사용한 직접 HTTP 호출
 */
@Service
class OpenAiClientImpl(
    private val properties: OpenAiProperties,
    private val objectMapper: ObjectMapper
) : AiClient {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val restTemplate = RestTemplate()

    companion object {
        private const val OPENAI_API_URL = "https://api.openai.com/v1/chat/completions"
    }

    override fun requestFeedback(systemPrompt: String, userPrompt: String): String {
        try {
            logger.info("OpenAI 피드백 요청 - 모델: ${properties.model}")

            // 1. 요청 본문 생성
            val requestBody = OpenAiRequest(
                model = properties.model,
                messages = listOf(
                    Message(role = "system", content = systemPrompt),
                    Message(role = "user", content = userPrompt)
                ),
                temperature = properties.temperature,
                maxTokens = properties.maxTokens,
                responseFormat = ResponseFormat(type = "json_object")
            )

            // 2. HTTP 헤더 설정
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                setBearerAuth(properties.apiKey)
            }

            // 3. HTTP 요청 전송
            val request = HttpEntity(requestBody, headers)
            val response = restTemplate.exchange(
                OPENAI_API_URL,
                HttpMethod.POST,
                request,
                String::class.java
            )

            // 4. 응답 파싱
            val responseBody = response.body
                ?: throw AiResponseException("OpenAI 응답이 비어있습니다")

            val openAiResponse = objectMapper.readValue(responseBody, OpenAiResponse::class.java)
            val content = openAiResponse.choices.firstOrNull()?.message?.content
                ?: throw AiResponseException("OpenAI 응답에 내용이 없습니다")

            logger.info(
                "OpenAI 피드백 수신 - 토큰: ${openAiResponse.usage?.totalTokens ?: 0}, " +
                "응답 길이: ${content.length}자"
            )

            return content

        } catch (e: HttpClientErrorException) {
            logger.error("OpenAI API 클라이언트 오류 (${e.statusCode}): ${e.responseBodyAsString}", e)
            throw AiApiException("OpenAI API 호출 실패 (${e.statusCode}): ${e.message}", e)
        } catch (e: HttpServerErrorException) {
            logger.error("OpenAI API 서버 오류 (${e.statusCode}): ${e.responseBodyAsString}", e)
            throw AiApiException("OpenAI 서버 오류 (${e.statusCode}): ${e.message}", e)
        } catch (e: SocketTimeoutException) {
            logger.error("OpenAI API 타임아웃: ${e.message}", e)
            throw AiRequestException("OpenAI API 타임아웃: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("예상치 못한 오류: ${e.message}", e)
            throw AiRequestException("AI 피드백 요청 실패: ${e.message}", e)
        }
    }

    // 요청/응답 데이터 클래스들
    private data class OpenAiRequest(
        val model: String,
        val messages: List<Message>,
        val temperature: Double,
        @JsonProperty("max_tokens") val maxTokens: Int,
        @JsonProperty("response_format") val responseFormat: ResponseFormat
    )

    private data class Message(val role: String, val content: String)
    private data class ResponseFormat(val type: String)

    private data class OpenAiResponse(
        val id: String?,
        val choices: List<Choice>,
        val usage: Usage?
    )

    private data class Choice(
        val message: Message?,
        @JsonProperty("finish_reason") val finishReason: String?
    )

    private data class Usage(
        @JsonProperty("prompt_tokens") val promptTokens: Int?,
        @JsonProperty("completion_tokens") val completionTokens: Int?,
        @JsonProperty("total_tokens") val totalTokens: Int?
    )
}
```

**핵심 설계 결정**:
- **RestTemplate 직접 사용**: 외부 SDK 의존성 제거, 명시적 HTTP 호출
- **인터페이스 추상화**: 향후 Claude API 등으로 교체 가능
- **JSON Mode 강제**: `response_format: json_object`로 구조화된 응답 보장
- **에러 분류**: HTTP 상태 코드 기반 세분화된 예외 처리
- **로깅**: 모델명, 토큰 사용량, 응답 길이, 에러 상세 로깅

---

### 4.5 ResponseParser - 응답 파싱 및 검증

**파일 위치**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/service/ai/ResponseParser.kt`

**목적**:
- OpenAI JSON 응답을 파싱
- 응답 데이터 검증 (점수 범위, 피드백 개수 등)
- 검증 실패 시 예외 발생

**데이터 클래스**:
```kotlin
/**
 * 파싱된 AI 피드백 데이터
 */
data class ParsedFeedback(
    val logicScore: Int,
    val specificityScore: Int,
    val jobFitScore: Int,
    val deliveryScore: Int,
    val strengths: List<String>,
    val improvements: List<String>,
    val modelAnswer: String,
    val overallComment: String
)

/**
 * OpenAI JSON 응답 구조 (내부용)
 */
private data class OpenAiResponse(
    val scores: Scores,
    val strengths: List<String>,
    val improvements: List<String>,
    val modelAnswer: String,
    val overallComment: String
) {
    data class Scores(
        val logic: Int,
        val specificity: Int,
        val jobFit: Int,
        val delivery: Int
    )
}
```

**구현**:
```kotlin
@Service
class ResponseParser(
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MIN_SCORE = 1
        private const val MAX_SCORE = 5
        private const val MIN_FEEDBACK_ITEMS = 2
        private const val MAX_FEEDBACK_ITEMS = 3
        private const val MIN_MODEL_ANSWER_LENGTH = 100
        private const val MAX_MODEL_ANSWER_LENGTH = 1000
    }

    /**
     * OpenAI JSON 응답을 ParsedFeedback으로 파싱
     * 파싱 실패 또는 검증 실패 시 AiResponseParseException 발생
     */
    fun parseOpenAiResponse(jsonString: String, rawResponse: String = jsonString): ParsedFeedback {
        try {
            val response = objectMapper.readValue(jsonString, OpenAiResponse::class.java)

            // 검증
            validateScores(response.scores)
            validateFeedbackItems(response.strengths, "strengths")
            validateFeedbackItems(response.improvements, "improvements")
            validateModelAnswer(response.modelAnswer)

            logger.info("OpenAI 응답 파싱 성공")

            return ParsedFeedback(
                logicScore = response.scores.logic,
                specificityScore = response.scores.specificity,
                jobFitScore = response.scores.jobFit,
                deliveryScore = response.scores.delivery,
                strengths = response.strengths,
                improvements = response.improvements,
                modelAnswer = response.modelAnswer,
                overallComment = response.overallComment
            )

        } catch (e: JsonProcessingException) {
            logger.error("JSON 파싱 실패: ${e.message}", e)
            throw AiResponseParseException("AI 응답의 JSON 형식이 잘못되었습니다: ${e.message}", rawResponse, e)
        } catch (e: IllegalArgumentException) {
            logger.error("응답 검증 실패: ${e.message}", e)
            throw AiResponseParseException("AI 응답 내용이 유효하지 않습니다: ${e.message}", rawResponse, e)
        }
    }

    private fun validateScores(scores: OpenAiResponse.Scores) {
        listOf(
            "logic" to scores.logic,
            "specificity" to scores.specificity,
            "jobFit" to scores.jobFit,
            "delivery" to scores.delivery
        ).forEach { (name, score) ->
            require(score in MIN_SCORE..MAX_SCORE) {
                "$name 점수는 $MIN_SCORE-$MAX_SCORE 사이여야 합니다 (현재: $score)"
            }
        }
    }

    private fun validateFeedbackItems(items: List<String>, fieldName: String) {
        require(items.size in MIN_FEEDBACK_ITEMS..MAX_FEEDBACK_ITEMS) {
            "$fieldName는 $MIN_FEEDBACK_ITEMS-$MAX_FEEDBACK_ITEMS개여야 합니다 (현재: ${items.size}개)"
        }
        require(items.all { it.isNotBlank() }) {
            "$fieldName에 빈 항목이 포함되어 있습니다"
        }
    }

    private fun validateModelAnswer(answer: String) {
        require(answer.length in MIN_MODEL_ANSWER_LENGTH..MAX_MODEL_ANSWER_LENGTH) {
            "모범답변 길이는 $MIN_MODEL_ANSWER_LENGTH-$MAX_MODEL_ANSWER_LENGTH자 사이여야 합니다 (현재: ${answer.length}자)"
        }
    }
}
```

**검증 전략**:
- ✅ **점수 범위**: 1-5 사이 정수 확인
- ✅ **피드백 개수**: 강점/개선점 각각 2-3개
- ✅ **모범답변 길이**: 100-1000자 (400-600 권장, 여유 허용)
- ✅ **빈 문자열 방지**: 모든 피드백 항목이 빈 값이 아닌지 확인

---

### 4.6 DuplicateRequestCache - 중복 요청 방지

**파일 위치**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/service/cache/DuplicateRequestCache.kt`

**목적**:
- 동일한 질문에 동일한 답변을 24시간 내 재평가하지 않음
- API 비용 절감

**구현 전략**:
1. `(questionId, answerText)` → SHA-256 해시 생성
2. `AiFeedback` 테이블에 `answerTextHash` 컬럼 추가
3. 피드백 요청 전 24시간 내 동일 해시 존재 여부 확인
4. 존재하면 캐시된 피드백 반환, 없으면 새로 생성

**Flyway 마이그레이션**:
```sql
-- V3__add_answer_text_hash.sql
ALTER TABLE ai_feedbacks
ADD COLUMN answer_text_hash VARCHAR(64);

CREATE INDEX idx_ai_feedbacks_hash_created
ON ai_feedbacks(answer_text_hash, created_at);
```

**AiFeedback 엔티티 수정**:
```kotlin
@Entity
@Table(name = "ai_feedbacks")
class AiFeedback(
    // ... 기존 필드들 ...

    @Column(name = "answer_text_hash", length = 64)
    val answerTextHash: String? = null,     // SHA-256 해시 (64자)

    // ...
)
```

**구현**:
```kotlin
@Service
class DuplicateRequestCache(
    private val aiFeedbackRepository: AiFeedbackRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val CACHE_DURATION_HOURS = 24L
    }

    /**
     * 캐시된 피드백 조회 (24시간 이내)
     */
    fun findCached(questionId: Long, answerText: String): AiFeedback? {
        val hash = generateHash(questionId, answerText)
        val cutoffTime = LocalDateTime.now().minusHours(CACHE_DURATION_HOURS)

        val cached = aiFeedbackRepository.findByAnswerTextHashAndCreatedAtAfter(hash, cutoffTime)

        if (cached != null) {
            logger.info("캐시된 피드백 사용 - 해시: $hash")
        }

        return cached
    }

    /**
     * 질문 ID와 답변 텍스트로부터 해시 생성
     */
    fun generateHash(questionId: Long, answerText: String): String {
        val input = "$questionId:$answerText"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
```

**Repository 메서드 추가**:
```kotlin
interface AiFeedbackRepository : JpaRepository<AiFeedback, Long> {
    fun findByInterviewAnswerId(interviewAnswerId: Long): AiFeedback?

    // 신규 메서드
    fun findByAnswerTextHashAndCreatedAtAfter(
        answerTextHash: String,
        createdAt: LocalDateTime
    ): AiFeedback?
}
```

---

### 4.7 RateLimitService - 요청 제한

**파일 위치**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/service/ratelimit/RateLimitService.kt`

**목적**:
- IP 주소당 시간당 33회 제한
- 악의적 사용 방지
- 예산 초과 방지

**구현 전략**:
- Caffeine Cache를 사용한 in-memory 저장
- 1시간 윈도우 방식
- IP 주소를 키로 사용

**의존성 추가**:
```kotlin
// build.gradle.kts
implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
```

**예외 클래스**:
```kotlin
// exception/RateLimitExceededException.kt
class RateLimitExceededException(
    val ip: String,
    val limit: Int,
    val resetTime: LocalDateTime
) : RuntimeException("IP $ip의 요청 한도($limit 회/시간)를 초과했습니다. 재설정 시간: $resetTime")
```

**구현**:
```kotlin
@Service
class RateLimitService {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MAX_REQUESTS_PER_HOUR = 33
        private const val WINDOW_DURATION_MINUTES = 60L
    }

    // IP별 요청 기록 (1시간 자동 만료)
    private val requestCache: Cache<String, MutableList<LocalDateTime>> = Caffeine.newBuilder()
        .expireAfterWrite(WINDOW_DURATION_MINUTES, TimeUnit.MINUTES)
        .build()

    /**
     * 요청 허용 여부 확인 및 기록
     * @throws RateLimitExceededException 한도 초과 시
     */
    fun checkAndRecordRequest(ip: String) {
        val now = LocalDateTime.now()
        val cutoffTime = now.minusMinutes(WINDOW_DURATION_MINUTES)

        // 현재 IP의 요청 기록 가져오기
        val requests = requestCache.get(ip) { mutableListOf() }!!

        // 1시간 이내 요청만 필터링
        requests.removeIf { it.isBefore(cutoffTime) }

        // 한도 확인
        if (requests.size >= MAX_REQUESTS_PER_HOUR) {
            val resetTime = requests.first().plusMinutes(WINDOW_DURATION_MINUTES)
            logger.warn("Rate limit 초과 - IP: $ip, 현재 요청 수: ${requests.size}")
            throw RateLimitExceededException(ip, MAX_REQUESTS_PER_HOUR, resetTime)
        }

        // 요청 기록
        requests.add(now)
        logger.debug("요청 기록 - IP: $ip, 현재 요청 수: ${requests.size}/$MAX_REQUESTS_PER_HOUR")
    }
}
```

**AnswerController 수정**:
```kotlin
@Controller
class AnswerController(
    private val interviewService: InterviewService,
    private val rateLimitService: RateLimitService        // 신규 추가
) {

    @PostMapping("/questions/{questionId}/answer")
    fun submitAnswer(
        @PathVariable questionId: Long,
        @RequestParam answerText: String,
        request: HttpServletRequest                        // 신규 추가
    ): String {
        // Rate limit 체크
        val clientIp = getClientIp(request)
        rateLimitService.checkAndRecordRequest(clientIp)

        // 기존 로직
        val dto = AnswerSubmitDto(questionId, answerText)
        val result = interviewService.submitAnswer(dto)
        return "redirect:/answers/${result.answerId}/feedback"
    }

    private fun getClientIp(request: HttpServletRequest): String {
        // X-Forwarded-For 헤더 우선 (프록시 환경 대응)
        val forwardedFor = request.getHeader("X-Forwarded-For")
        return if (!forwardedFor.isNullOrBlank()) {
            forwardedFor.split(",").first().trim()
        } else {
            request.remoteAddr
        }
    }
}
```

**GlobalExceptionHandler에 추가**:
```kotlin
@ExceptionHandler(RateLimitExceededException::class)
fun handleRateLimitExceeded(e: RateLimitExceededException, model: Model): String {
    logger.warn("Rate limit 초과: ${e.message}")

    model.addAttribute("errorMessage", "요청 한도를 초과했습니다")
    model.addAttribute("errorDetail", "1시간에 최대 ${e.limit}회까지 평가를 요청할 수 있습니다. ${e.resetTime}에 재설정됩니다.")
    return "error/rate-limit"
}
```

---

### 4.8 AiFeedbackService 리팩토링

**파일 위치**: `src/main/kotlin/com/hojun/interviewnote/interviewnoteapi/service/AiFeedbackService.kt`

**수정 전략**:
1. `generateDummyFeedback()` 유지 (테스트 및 폴백용)
2. 새로운 `generateFeedback()` 메서드 추가 (실제 AI 호출)
3. 모든 `AiException` catch하여 더미 피드백으로 폴백

**수정 후 코드**:
```kotlin
@Service
@Transactional
class AiFeedbackService(
    private val aiFeedbackRepository: AiFeedbackRepository,
    private val objectMapper: ObjectMapper,
    private val aiClient: AiClient,                              // 신규 의존성
    private val promptBuilder: PromptBuilder,                    // 신규 의존성
    private val responseParser: ResponseParser,                  // 신규 의존성
    private val duplicateRequestCache: DuplicateRequestCache,    // 신규 의존성
    private val openAiProperties: OpenAiProperties               // 신규 의존성
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        // 더미 피드백용 상수들 (기존 유지)
        private const val ANSWER_LENGTH_THRESHOLD_HIGH = 500
        private const val ANSWER_LENGTH_THRESHOLD_MEDIUM = 300
        private const val DUMMY_SCORE_HIGH = 4
        private const val DUMMY_SCORE_MEDIUM = 3
        private const val DUMMY_SCORE_LOW = 2
        private const val TOKEN_ESTIMATION_FACTOR = 4
    }

    /**
     * Phase 2: 실제 AI 피드백 생성
     */
    fun generateFeedback(answer: InterviewAnswer, question: Question): AiFeedback {
        logger.info("AI 피드백 생성 시작 - 답변 ID: ${answer.id}, 질문 ID: ${question.id}")

        try {
            // 1. 캐시 확인 (중복 방지)
            val cached = duplicateRequestCache.findCached(question.id, answer.answerText)
            if (cached != null) {
                logger.info("캐시된 피드백 반환 - 피드백 ID: ${cached.id}")
                return cached
            }

            // 2. 프롬프트 생성
            val systemPrompt = promptBuilder.buildSystemPrompt(question.jobField, question.targetJob)
            val userPrompt = promptBuilder.buildUserPrompt(question, answer.answerText)

            // 3. AI 피드백 요청
            val startTime = System.currentTimeMillis()
            val rawResponse = aiClient.requestFeedback(systemPrompt, userPrompt)
            val duration = System.currentTimeMillis() - startTime

            logger.info("AI 응답 수신 완료 - 소요 시간: ${duration}ms")

            // 4. 응답 파싱
            val parsed = responseParser.parseOpenAiResponse(rawResponse)

            // 5. AiFeedback 엔티티 생성
            val answerTextHash = duplicateRequestCache.generateHash(question.id, answer.answerText)
            val aiFeedback = buildAiFeedbackEntity(
                answer = answer,
                question = question,
                parsed = parsed,
                rawResponse = rawResponse,
                answerTextHash = answerTextHash
            )

            // 6. 저장 및 반환
            return aiFeedbackRepository.save(aiFeedback)

        } catch (e: AiException) {
            logger.error("AI 피드백 생성 실패 (답변 ID: ${answer.id}): ${e.message}", e)
            logger.warn("더미 피드백으로 폴백합니다")
            return generateDummyFeedback(answer, question)
        }
    }

    private fun buildAiFeedbackEntity(
        answer: InterviewAnswer,
        question: Question,
        parsed: ParsedFeedback,
        rawResponse: String,
        answerTextHash: String
    ): AiFeedback {
        return AiFeedback(
            interviewAnswerId = answer.id,
            logicScore = parsed.logicScore,
            specificityScore = parsed.specificityScore,
            jobFitScore = parsed.jobFitScore,
            deliveryScore = parsed.deliveryScore,
            strengths = objectMapper.writeValueAsString(parsed.strengths),
            improvements = objectMapper.writeValueAsString(parsed.improvements),
            modelAnswer = parsed.modelAnswer,
            overallComment = parsed.overallComment,
            jobField = question.jobField,
            modelName = openAiProperties.model,
            promptVersion = openAiProperties.promptVersion,
            tokenUsageInput = estimateTokens(answer.answerText),
            tokenUsageOutput = estimateTokens(parsed.modelAnswer),
            rawResponse = rawResponse,
            answerTextHash = answerTextHash,
            createdAt = LocalDateTime.now()
        )
    }

    private fun estimateTokens(text: String): Int {
        // 간단한 추정: 4글자 ≈ 1토큰
        return text.length / TOKEN_ESTIMATION_FACTOR
    }

    /**
     * Phase 1: 더미 피드백 생성 (테스트 및 폴백용 유지)
     */
    fun generateDummyFeedback(answer: InterviewAnswer, question: Question): AiFeedback {
        // 기존 구현 그대로 유지
        // ...

        // modelName을 "fallback-dummy-v1"로 설정하여 폴백 여부 추적 가능
        val modelName = "fallback-dummy-v1"

        // ...
    }

    fun findByInterviewAnswerId(interviewAnswerId: Long): AiFeedback? {
        return aiFeedbackRepository.findByInterviewAnswerId(interviewAnswerId)
    }
}
```

**InterviewService 수정** (1줄 변경):
```kotlin
@Service
@Transactional
class InterviewService(
    // ...
) {

    fun submitAnswer(dto: AnswerSubmitDto): AnswerWithFeedbackDto {
        // ...

        // 변경 전:
        // val aiFeedback = aiFeedbackService.generateDummyFeedback(savedAnswer, question)

        // 변경 후:
        val aiFeedback = aiFeedbackService.generateFeedback(savedAnswer, question)

        // ...
    }
}
```

---

## 5. 구현 순서

### Phase 2A: 기반 구축 (1일)

**Step 1: 의존성 추가**
```kotlin
// build.gradle.kts
dependencies {
    // 기존 의존성...
    // RestTemplate은 spring-boot-starter-web에 포함되어 있어 별도 추가 불필요

    // Caffeine Cache (Rate Limiting용)
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
}
```

- [ ] `build.gradle.kts` 수정
- [ ] `./gradlew build` 실행하여 의존성 다운로드 확인
- [ ] 빌드 오류 없는지 확인

**Step 2: 설정 클래스 생성**
- [ ] `config/OpenAiConfig.kt` 생성
- [ ] `application.properties`에 OpenAI 설정 추가
- [ ] 로컬 환경변수 `OPENAI_API_KEY` 설정
- [ ] 애플리케이션 실행하여 Bean 로드 확인

**Step 3: 예외 클래스 생성**
- [ ] `exception/AiExceptions.kt` 생성 (5개 클래스)
- [ ] `exception/RateLimitExceededException.kt` 생성
- [ ] `GlobalExceptionHandler.kt`에 AI 예외 핸들러 추가
- [ ] (선택) `error/ai-error.html` 템플릿 생성

**체크포인트**:
- ✅ 애플리케이션이 정상 실행됨
- ✅ OpenAI 설정 Bean이 로드됨 (로그 확인)
- ✅ 빌드 및 기존 테스트 통과

---

### Phase 2B: AI 클라이언트 구현 (1-2일)

**Step 4: PromptBuilder 구현**
- [ ] `service/ai/PromptBuilder.kt` 생성
- [ ] `buildSystemPrompt()` - IT 직무용 프롬프트 구현
- [ ] `buildUserPrompt()` 구현
- [ ] 단위 테스트 작성: `PromptBuilderTest.kt`
  - IT 직무 시스템 프롬프트 생성 테스트
  - 지원되지 않는 직무 예외 테스트
  - 사용자 프롬프트 생성 테스트

**Step 5: ResponseParser 구현**
- [ ] `service/ai/ResponseParser.kt` 생성
- [ ] `ParsedFeedback` 데이터 클래스 정의
- [ ] `parseOpenAiResponse()` 구현 (검증 포함)
- [ ] 단위 테스트 작성: `ResponseParserTest.kt`
  - 유효한 JSON 파싱 테스트
  - 범위 밖 점수 예외 테스트
  - 피드백 항목 개수 검증 테스트
  - 잘못된 JSON 예외 테스트

**Step 6: OpenAI 클라이언트 구현**
- [ ] `service/ai/AiClient.kt` 인터페이스 정의
- [ ] `service/ai/OpenAiClientImpl.kt` 구현
- [ ] Mock 테스트 작성: `OpenAiClientTest.kt`
  - 정상 응답 반환 테스트
  - OpenAI API 오류 시 AiApiException 테스트
  - 빈 응답 시 AiResponseException 테스트

**체크포인트**:
- ✅ 모든 단위 테스트 통과
- ✅ `./gradlew test` 실행 시 오류 없음

---

### Phase 2C: 서비스 통합 (1일)

**Step 7: AiFeedbackService 리팩토링**
- [ ] 의존성 추가: `AiClient`, `PromptBuilder`, `ResponseParser`, `DuplicateRequestCache`, `OpenAiProperties`
- [ ] `generateFeedback()` 메서드 구현
- [ ] `buildAiFeedbackEntity()` 헬퍼 메서드 구현
- [ ] 폴백 로직 추가 (catch `AiException`)
- [ ] `generateDummyFeedback()`의 modelName을 "fallback-dummy-v1"로 변경
- [ ] 통합 테스트 작성: `AiFeedbackServiceIntegrationTest.kt`
  - Mock AI 클라이언트로 피드백 생성 테스트
  - AI 오류 시 더미 피드백 폴백 테스트

**Step 8: InterviewService 수정**
- [ ] 43번 줄: `generateDummyFeedback()` → `generateFeedback()`로 변경
- [ ] `InterviewServiceTest.kt` 업데이트 (AiClient Mock 추가)

**체크포인트**:
- ✅ 전체 서비스 레이어가 Mock AI로 동작
- ✅ 기존 테스트 + 신규 테스트 모두 통과

---

### Phase 2D: 비용 제어 구현 (1-2일)

**Step 9: 중복 요청 방지**
- [ ] Flyway 마이그레이션 작성: `V3__add_answer_text_hash.sql`
- [ ] `AiFeedback` 엔티티에 `answerTextHash` 필드 추가
- [ ] `AiFeedbackRepository`에 `findByAnswerTextHashAndCreatedAtAfter()` 메서드 추가
- [ ] `service/cache/DuplicateRequestCache.kt` 구현
  - `findCached()` - 24시간 내 캐시 조회
  - `generateHash()` - SHA-256 해시 생성
- [ ] `AiFeedbackService.generateFeedback()`에 캐시 확인 로직 추가
- [ ] 마이그레이션 테스트: `./gradlew flywayMigrate` 실행

**Step 10: Rate Limiting**
- [ ] `build.gradle.kts`에 Caffeine 의존성 추가 (Step 1에서 완료)
- [ ] `service/ratelimit/RateLimitService.kt` 구현
  - Caffeine Cache 기반 IP 추적
  - `checkAndRecordRequest()` 메서드
- [ ] `AnswerController`에 Rate Limit 체크 추가
  - `getClientIp()` 헬퍼 메서드
  - `rateLimitService.checkAndRecordRequest()` 호출
- [ ] `GlobalExceptionHandler`에 Rate Limit 예외 핸들러 추가

**체크포인트**:
- ✅ 중복 요청 시 캐시된 피드백 반환
- ✅ 34번째 요청에서 Rate Limit 예외 발생
- ✅ 1시간 후 Rate Limit 리셋

---

### Phase 2E: 실제 API 테스트 (0.5일)

**Step 11: 실제 OpenAI API 테스트**
- [ ] 환경변수 `OPENAI_API_KEY` 설정
- [ ] `Phase2ManualTest.kt` 작성 (`@Disabled` 포함)
- [ ] `@Disabled` 제거 후 테스트 실행
- [ ] 검증 항목:
  - [ ] 점수가 1-5 범위
  - [ ] 강점/개선점이 각각 2-3개
  - [ ] 모범답변이 합리적인 길이
  - [ ] rawResponse가 유효한 JSON
  - [ ] 로그에서 토큰 사용량, 지연 시간 확인

**Step 12: 오류 시나리오 테스트**
- [ ] 잘못된 API 키로 테스트 → 더미 피드백 폴백 확인
- [ ] 네트워크 차단 상태 테스트 → 폴백 확인
- [ ] Rate Limit 초과 테스트 → 429 오류 확인
- [ ] 중복 요청 테스트 → 캐시된 응답 반환 확인

**Step 13: 메타데이터 검증**
- [ ] H2 콘솔 접속 (`http://localhost:8080/h2-console`)
- [ ] `ai_feedbacks` 테이블 조회
- [ ] 검증 항목:
  - [ ] `modelName`: "gpt-4o-mini"
  - [ ] `promptVersion`: "v1.0"
  - [ ] `tokenUsageInput`, `tokenUsageOutput`: 합리적인 값
  - [ ] `rawResponse`: 유효한 JSON
  - [ ] `answerTextHash`: 64자 SHA-256 해시

**체크포인트**:
- ✅ 실제 OpenAI 연동 E2E 동작 확인
- ✅ 모든 오류 시나리오 정상 처리

---

### Phase 2F: UI 검증 및 문서화 (0.5일)

**Step 14: 웹 인터페이스 테스트**
- [ ] 애플리케이션 실행: `./gradlew bootRun`
- [ ] 브라우저에서 전체 플로우 테스트:
  - [ ] 질문 목록 → 질문 선택 → 답변 작성 (300자 이상)
  - [ ] 제출 후 피드백 페이지 확인
  - [ ] 실제 AI 점수 표시 (더미 아님)
  - [ ] 의미 있는 강점/개선점 표시
  - [ ] 모범답변 관련성 확인
  - [ ] 응답 시간 < 10초

**Step 15: 폴백 UI 테스트**
- [ ] 애플리케이션 중지
- [ ] `application.properties`에서 API 키 임시 제거
- [ ] 재시작 후 답변 제출
- [ ] 더미 피드백 표시 확인
- [ ] DB에서 `modelName = "fallback-dummy-v1"` 확인
- [ ] API 키 복구

**Step 16: Rate Limit UI 테스트**
- [ ] 동일 IP에서 34개 답변 제출 (1시간 이내)
- [ ] 34번째 요청에서 Rate Limit 오류 페이지 확인
- [ ] 1시간 대기 또는 캐시 클리어
- [ ] 다시 요청 가능한지 확인

**Step 17: 중복 요청 UI 테스트**
- [ ] 동일 질문에 동일 답변 2회 제출 (24시간 이내)
- [ ] 2번째 요청이 즉시 반환되는지 확인 (< 1초)
- [ ] 로그에서 "캐시된 피드백 사용" 메시지 확인

**Step 18: 문서 업데이트**
- [ ] `README.md` 업데이트:
  - OpenAI API 키 설정 방법
  - 환경 변수 설정 예시
  - 비용 추정 정보
  - Rate Limit 정보
- [ ] 코드 주석 검토 및 보완

**체크포인트**:
- ✅ Phase 2 완전 완료
- ✅ Production 배포 준비 완료

---

## 6. 비용 제어 전략

### 6.1 MVP에 포함되는 비용 제어

| 메커니즘 | 구현 방법 | 기대 효과 |
|---------|----------|----------|
| **답변 길이 제한** | `AnswerSubmitDto`에서 최대 2000자 검증 | 입력 토큰 제한 |
| **토큰 제한** | OpenAI API 호출 시 `maxTokens=800` | 출력 토큰 제한 |
| **모델 선택** | `gpt-4o-mini` 사용 (최저가 모델) | 토큰당 비용 최소화 |
| **JSON Mode** | `responseFormat(JSON_OBJECT)` 사용 | 자유 텍스트 대비 토큰 절약 |
| **중복 방지** | 24시간 캐싱 | 불필요한 재평가 방지 |
| **Rate Limiting** | IP당 33회/시간 | 악의적 사용 방지 |

### 6.2 비용 추정

**1회 평가 비용 (gpt-4o-mini 기준)**:
```
모델 요금 (2026년 기준):
- 입력: $0.15 / 1M 토큰
- 출력: $0.60 / 1M 토큰

1회 평가:
- 입력 토큰: ~500 (시스템 프롬프트 + 질문 + 답변)
- 출력 토큰: ~200 (평가 JSON)
- 입력 비용: 500 × $0.15 / 1M = $0.000075
- 출력 비용: 200 × $0.60 / 1M = $0.00012
- 총 비용: $0.000195 ≈ $0.0002 (0.2원)
```

**시나리오별 월간 비용**:
| 일일 평가 수 | 월간 평가 수 | 월간 비용 | 비고 |
|------------|------------|----------|------|
| 10 | 300 | $0.06 | 개인 사용자 |
| 100 | 3,000 | $0.60 | 중간 사용자 |
| 1,000 | 30,000 | $6.00 | 다수 사용자 |

**Rate Limit 적용 시 최대 비용**:
```
단일 IP 최대 요청: 33회/시간 × 24시간 = 792회/일
월간 최대 요청: 792 × 30 = 23,760회
월간 최대 비용: 23,760 × $0.0002 ≈ $4.75
```

### 6.3 모니터링 지표 (추후 구현)

Phase 3에서 Micrometer + Prometheus/Grafana로 구현 예정:

1. **요청 메트릭**:
   - 일일 총 요청 수
   - 성공 / 실패 비율
   - 폴백 비율
   - 평균 지연 시간

2. **토큰 메트릭**:
   - 일일 총 토큰 사용량 (입력/출력 분리)
   - 평가당 평균 토큰
   - 일일 추정 비용

3. **캐시 메트릭**:
   - 캐시 히트율
   - Rate Limit 초과 횟수

---

## 7. 테스트 전략

### 7.1 단위 테스트

#### PromptBuilderTest
```kotlin
@ExtendWith(MockitoExtension::class)
class PromptBuilderTest {

    @Mock
    private lateinit var properties: OpenAiProperties

    @InjectMocks
    private lateinit var promptBuilder: PromptBuilder

    @Test
    fun `IT 직무 시스템 프롬프트를 생성한다`() {
        val prompt = promptBuilder.buildSystemPrompt("IT", "백엔드 개발자")

        assertThat(prompt).contains("백엔드 개발자")
        assertThat(prompt).contains("논리성")
        assertThat(prompt).contains("JSON 형식")
    }

    @Test
    fun `지원되지 않는 직무는 예외를 발생시킨다`() {
        assertThrows<IllegalArgumentException> {
            promptBuilder.buildSystemPrompt("SALES", "영업관리자")
        }
    }
}
```

#### ResponseParserTest
```kotlin
class ResponseParserTest {

    private lateinit var responseParser: ResponseParser

    @BeforeEach
    fun setup() {
        responseParser = ResponseParser(ObjectMapperConfig.objectMapper)
    }

    @Test
    fun `유효한 JSON 응답을 파싱한다`() {
        val json = """
            {
              "scores": {"logic": 4, "specificity": 3, "jobFit": 4, "delivery": 3},
              "strengths": ["강점1", "강점2"],
              "improvements": ["개선점1", "개선점2"],
              "modelAnswer": "${"모범답변 ".repeat(20)}",
              "overallComment": "좋습니다"
            }
        """.trimIndent()

        val result = responseParser.parseOpenAiResponse(json)

        assertThat(result.logicScore).isEqualTo(4)
        assertThat(result.strengths).hasSize(2)
    }

    @Test
    fun `범위 밖 점수는 예외를 발생시킨다`() {
        val json = """
            {
              "scores": {"logic": 6, ...},
              ...
            }
        """.trimIndent()

        assertThrows<AiResponseParseException> {
            responseParser.parseOpenAiResponse(json)
        }
    }
}
```

### 7.2 통합 테스트

#### AiFeedbackServiceIntegrationTest
```kotlin
@SpringBootTest
@Transactional
class AiFeedbackServiceIntegrationTest {

    @Autowired
    private lateinit var aiFeedbackService: AiFeedbackService

    @MockBean
    private lateinit var aiClient: AiClient

    @Test
    fun `AI 피드백을 생성하고 저장한다`() {
        val answer = InterviewAnswer(...)
        val question = Question(...)

        val mockResponse = """
            {
              "scores": {"logic": 4, "specificity": 3, "jobFit": 4, "delivery": 3},
              "strengths": ["강점1", "강점2"],
              "improvements": ["개선점1", "개선점2"],
              "modelAnswer": "${"모범답변 ".repeat(50)}",
              "overallComment": "좋습니다"
            }
        """.trimIndent()

        whenever(aiClient.requestFeedback(any(), any())).thenReturn(mockResponse)

        val result = aiFeedbackService.generateFeedback(answer, question)

        assertThat(result.id).isNotZero
        assertThat(result.logicScore).isEqualTo(4)
    }

    @Test
    fun `AI 오류 시 더미 피드백으로 폴백한다`() {
        whenever(aiClient.requestFeedback(any(), any())).thenThrow(AiApiException("API Error"))

        val result = aiFeedbackService.generateFeedback(answer, question)

        assertThat(result.modelName).contains("fallback-dummy")
    }
}
```

### 7.3 E2E 테스트 (수동)

#### Phase2ManualTest
```kotlin
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Phase2ManualTest {

    @Autowired
    private lateinit var aiFeedbackService: AiFeedbackService

    @Disabled("수동 테스트 - 실제 OpenAI API 호출")
    @Test
    fun `실제 OpenAI API로 피드백을 생성한다`() {
        val answer = InterviewAnswer(
            id = 999L,
            questionId = 999L,
            answerText = """
                Spring Boot의 가장 큰 장점은 개발 생산성입니다.
                자동 설정 기능으로 인해 복잡한 XML 설정 없이 빠르게 프로젝트를 시작할 수 있습니다.
                ...
            """.trimIndent(),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val question = Question(...)

        val result = aiFeedbackService.generateFeedback(answer, question)

        println("=== AI 피드백 결과 ===")
        println("논리성: ${result.logicScore}")
        println("구체성: ${result.specificityScore}")
        println("직무적합성: ${result.jobFitScore}")
        println("전달력: ${result.deliveryScore}")
        println("평균: ${result.averageScore}")
        println("\n강점: ${result.strengths}")
        println("개선점: ${result.improvements}")
        println("\n모델: ${result.modelName}")
        println("토큰 사용량: ${result.tokenUsageInput} + ${result.tokenUsageOutput}")

        assertThat(result.modelName).isEqualTo("gpt-4o-mini")
        assertThat(result.logicScore).isBetween(1, 5)
    }
}
```

---

## 8. 검증 계획

### 8.1 자동화된 테스트 검증

```bash
# 전체 테스트 실행
./gradlew test

# 커버리지 리포트 생성
./gradlew test jacocoTestReport

# 빌드 확인
./gradlew build
```

**기대 결과**:
- ✅ 모든 기존 테스트 통과
- ✅ 모든 신규 단위 테스트 통과 (PromptBuilder, ResponseParser, OpenAiClient)
- ✅ 통합 테스트 통과 (AiFeedbackService with mocked AI)
- ✅ 빌드 성공, 경고 없음

### 8.2 수동 검증 체크리스트

#### OpenAI 연동 검증
- [ ] 실제 AI 피드백 생성 동작
- [ ] 점수가 1-5 범위
- [ ] 강점/개선점이 각각 2-3개
- [ ] 모범답변이 400-600자 내외
- [ ] 응답 시간 < 10초
- [ ] 메타데이터 올바르게 저장 (modelName, promptVersion, tokenUsage, rawResponse)

#### 에러 처리 검증
- [ ] 잘못된 API 키 → 더미 피드백 폴백
- [ ] 네트워크 오류 → 더미 피드백 폴백
- [ ] 잘못된 JSON 응답 → 더미 피드백 폴백
- [ ] 타임아웃 → 더미 피드백 폴백

#### 비용 제어 검증
- [ ] 중복 요청 시 캐시된 결과 반환 (API 호출 안 함)
- [ ] 캐시 24시간 후 만료
- [ ] Rate Limit 강제 (34번째 요청 차단)
- [ ] Rate Limit 1시간 후 리셋

#### UI 검증
- [ ] 피드백 페이지에 실제 AI 결과 표시
- [ ] 폴백 시 더미 피드백 정상 표시
- [ ] Rate Limit 오류 시 사용자 친화적 메시지 표시
- [ ] 로딩 인디케이터 표시 (AI 호출 중)

### 8.3 데이터베이스 검증

```sql
-- AI 피드백 메타데이터 확인
SELECT
  id,
  modelName,
  promptVersion,
  tokenUsageInput,
  tokenUsageOutput,
  answerTextHash,
  LENGTH(rawResponse) as rawResponseLength,
  createdAt
FROM ai_feedbacks
ORDER BY createdAt DESC
LIMIT 10;

-- 중복 해시 확인 (24시간 내 중복 없어야 함)
SELECT
  answerTextHash,
  COUNT(*) as count
FROM ai_feedbacks
WHERE createdAt > NOW() - INTERVAL '24 HOURS'
GROUP BY answerTextHash
HAVING COUNT(*) > 1;

-- 폴백 비율 확인
SELECT
  CASE
    WHEN modelName LIKE '%dummy%' THEN '폴백'
    ELSE '정상'
  END as status,
  COUNT(*) as count
FROM ai_feedbacks
GROUP BY status;
```

**기대 결과**:
- ✅ `modelName`: "gpt-4o-mini" (폴백 아닌 경우)
- ✅ `promptVersion`: "v1.0"
- ✅ `tokenUsageInput`, `tokenUsageOutput`: 합리적인 값 (0 아님)
- ✅ `rawResponse`: 유효한 JSON (LENGTH > 100)
- ✅ `answerTextHash`: 64자 SHA-256 해시
- ✅ 24시간 내 중복 해시 없음

---

## 9. 파일 목록

### 9.1 신규 파일 (12개)

| 번호 | 파일 경로 | 용도 |
|-----|----------|------|
| 1 | `src/main/kotlin/.../exception/AiExceptions.kt` | AI 관련 예외 클래스 (5개) |
| 2 | `src/main/kotlin/.../config/OpenAiConfig.kt` | OpenAI 설정 및 Bean |
| 3 | `src/main/kotlin/.../service/ai/AiClient.kt` | AI 클라이언트 인터페이스 |
| 4 | `src/main/kotlin/.../service/ai/OpenAiClientImpl.kt` | OpenAI API 구현체 |
| 5 | `src/main/kotlin/.../service/ai/PromptBuilder.kt` | 프롬프트 생성기 |
| 6 | `src/main/kotlin/.../service/ai/ResponseParser.kt` | 응답 파싱 및 검증 |
| 7 | `src/main/kotlin/.../service/cache/DuplicateRequestCache.kt` | 중복 요청 방지 캐시 |
| 8 | `src/main/kotlin/.../service/ratelimit/RateLimitService.kt` | Rate Limiting |
| 9 | `src/main/kotlin/.../exception/RateLimitExceededException.kt` | Rate Limit 예외 |
| 10 | `src/test/kotlin/.../service/ai/PromptBuilderTest.kt` | PromptBuilder 단위 테스트 |
| 11 | `src/test/kotlin/.../service/ai/ResponseParserTest.kt` | ResponseParser 단위 테스트 |
| 12 | `src/test/kotlin/.../Phase2ManualTest.kt` | 수동 E2E 테스트 |

### 9.2 수정 파일 (7개)

| 번호 | 파일 경로 | 수정 내용 |
|-----|----------|----------|
| 1 | `build.gradle.kts` | OpenAI SDK, Caffeine 의존성 추가 |
| 2 | `src/main/resources/application.properties` | OpenAI 설정 추가 |
| 3 | `src/main/kotlin/.../service/AiFeedbackService.kt` | `generateFeedback()` 메서드 추가, 의존성 추가 |
| 4 | `src/main/kotlin/.../service/InterviewService.kt` | 43번 줄 1줄 변경 (더미 → 실제 AI) |
| 5 | `src/main/kotlin/.../controller/AnswerController.kt` | Rate Limit 체크 추가 |
| 6 | `src/main/kotlin/.../exception/GlobalExceptionHandler.kt` | AI 예외 핸들러 추가 |
| 7 | `src/main/kotlin/.../domain/AiFeedback.kt` | `answerTextHash` 필드 추가 |

### 9.3 신규 Flyway 마이그레이션 (1개)

| 파일 경로 | 용도 |
|----------|------|
| `src/main/resources/db/migration/V3__add_answer_text_hash.sql` | `ai_feedbacks` 테이블에 `answer_text_hash` 컬럼 추가 |

### 9.4 문서 파일

| 파일 경로 | 용도 |
|----------|------|
| `/phase2_implementation_plan.md` | 이 문서 (Phase 2 상세 설계) |
| `/README.md` | Phase 2 설정 방법 추가 필요 |

**총 변경 파일 수**: 21개
- 신규: 13개 (코드 12개 + 마이그레이션 1개)
- 수정: 7개
- 문서: 1개 (이 파일)

---

## 10. 참고 자료

### 10.1 공식 문서

- [OpenAI Java SDK - GitHub](https://github.com/openai/openai-java)
- [OpenAI API Reference](https://platform.openai.com/docs/api-reference)
- [Spring Boot Configuration Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Caffeine Cache](https://github.com/ben-manes/caffeine)

### 10.2 관련 프로젝트 문서

- `CLAUDE.md` - 프로젝트 전체 가이드
- `implementation_guide.md` - Phase 1 구현 가이드
- `phase1_refactoring.md` - Phase 1 리팩토링 계획

### 10.3 코딩 스타일

- [Google Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)
- [Effective Java - equals/hashCode](https://github.com/jbloch/effective-java-3e-source-code)

---

**작성일**: 2026-04-12
**버전**: 1.0
**작성자**: Claude Code with 호준
