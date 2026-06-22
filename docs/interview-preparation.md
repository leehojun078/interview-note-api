# Interview Note API 면접 준비 문서

> **대상**: 5년차 백엔드 개발자 포트폴리오 면접
> **작성일**: 2026-06-20
> **원칙**: 실제 코드 기반, 약점 솔직 인정, 개선 방향 제시

---

## 목차

1. [프로젝트 소개](#1-프로젝트-소개-중요도-상)
2. [아키텍처](#2-아키텍처-중요도-상)
3. [Spring Boot](#3-spring-boot-중요도-상)
4. [JPA](#4-jpa-중요도-상)
5. [API 설계](#5-api-설계-중요도-중)
6. [데이터베이스](#6-데이터베이스-중요도-상)
7. [인증/인가](#7-인증인가-중요도-중)
8. [인프라](#8-인프라-중요도-중)
9. [성능](#9-성능-중요도-상)
10. [장애 대응](#10-장애-대응-중요도-상)
11. [테스트](#11-테스트-중요도-상)
12. [협업 관점](#12-협업-관점-중요도-중)
13. [시니어 레벨 검증](#13-시니어-레벨-검증-중요도-상)

---

## 1. 프로젝트 소개 (중요도: 상)

### Q1-1. 이 프로젝트를 만든 이유가 무엇인가요?

#### 질문 의도
지원자의 문제 인식 능력과 해결 동기를 파악. 단순 기술 연습인지, 실제 문제 해결을 위한 것인지 확인.

#### 답변 예시
```
취업 준비생들이 면접 연습 시 겪는 문제를 해결하고 싶었습니다.

기존 면접 준비 방법의 문제점:
1. 혼자 연습하면 객관적인 피드백을 받기 어려움
2. 모범 답변을 보더라도 자신의 답변과 비교가 어려움
3. 과거 답변 이력을 체계적으로 관리하기 어려움

이 프로젝트는 AI를 활용해 즉각적인 피드백과 모범 답변을 제공하고,
답변 이력을 저장해서 개선 과정을 추적할 수 있게 합니다.

특히 백엔드 개발자로서 AI API 통합, 비용 최적화, 실시간 스트리밍 같은
실무에서 마주칠 기술적 도전을 직접 경험하고 싶었습니다.
```

#### 추가 꼬리 질문
- "취업 준비생을 타겟으로 한 이유는?" → 제가 취업 준비 경험이 있어서 Pain Point를 잘 알고 있습니다.
- "비슷한 서비스와 차별점은?" → AI 평가에 집중하면서도 비용 최적화를 통해 무료/저가 서비스 가능성을 열었습니다.

#### 개선 방향
- 실제 사용자 테스트를 통한 UX 개선 필요
- 직무별 맞춤 프롬프트 고도화 (현재 17개 직무 지원)

---

### Q1-2. 프로젝트의 핵심 기능을 설명해주세요.

#### 질문 의도
프로젝트 전체 구조에 대한 이해도 확인. 핵심 가치와 기술적 구현의 연결 파악.

#### 답변 예시
```
핵심 기능은 크게 4가지입니다.

1. **질문 연습** (Phase 1)
   - 17개 직무, 340개 면접 질문 제공
   - 카테고리, 난이도별 필터링

2. **AI 평가** (Phase 2)
   - OpenAI gpt-4o-mini 모델로 답변 평가
   - 4가지 기준(논리성, 구체성, 직무적합성, 전달력) 점수화
   - 강점/개선점/모범답변 제공

3. **채용공고 기반 질문 생성** (Phase 6)
   - 채용 공고 URL 입력 시 AI가 맞춤 질문 10개 생성
   - Jsoup으로 HTML 파싱, 실패 시 AI Fallback

4. **AI 모의 면접** (Phase 7)
   - 실시간 채팅 형식의 모의 면접
   - SSE(Server-Sent Events)로 실시간 응답 스트리밍
   - 종합 평가 및 점수 제공

기술적으로는 비용 최적화가 핵심 과제였는데,
SHA-256 해싱으로 중복 요청을 캐싱해서 99% 비용 절감을 달성했습니다.
```

#### 추가 꼬리 질문
- "Phase별로 나눈 이유는?" → MVP 접근법으로 점진적 확장. 각 Phase마다 기능 완성 후 다음 단계로.
- "17개 직무는 어떻게 선정했나요?" → 채용 시장 분석 기반. 각 직무별 맞춤 프롬프트 구현.

---

### Q1-3. 왜 이 기술 스택을 선택했나요?

#### 질문 의도
기술 선택의 근거와 트레이드오프 이해 확인. 무작정 유행을 따르는지, 합리적 판단인지 파악.

#### 답변 예시
```
각 기술 선택에는 명확한 이유가 있습니다.

**Kotlin + Spring Boot 3.5**
- 널 안전성과 코루틴으로 안정성 향상
- Spring의 성숙한 생태계와 풍부한 문서
- JPA, Security 등 표준 솔루션 활용 가능

**Thymeleaf + HTMX (프론트엔드 분리 안 함)**
- MVP 단계에서 백엔드 로직에 집중하기 위한 선택
- React/Vue 분리 시 API 설계, CORS, 인증 복잡도 증가
- HTMX로 페이지 새로고침 없는 인터랙션 구현

**PostgreSQL (프로덕션)**
- H2는 개발용, 프로덕션은 PostgreSQL
- JSON 타입 지원으로 AI 응답 저장 용이
- Flyway로 마이그레이션 관리

**Caffeine Cache (Redis 미사용)**
- 단일 인스턴스 MVP에서 충분
- Redis 운영 복잡도 회피
- 분산 환경 확장 시 Redis로 전환 계획

대안으로 고려했던 것들:
- WebFlux: 학습 곡선 대비 이점 불명확, MVC 선택
- JWT: 세션 기반이 MVP에서 더 단순, 추후 전환 가능
```

#### 추가 꼬리 질문
- "WebFlux를 선택하지 않은 구체적 이유는?" → SSE만 필요해서 @Async로 충분. 전체 리액티브 전환의 이점이 불명확.
- "Kotlin의 어떤 기능을 주로 활용했나요?" → data class, sealed class, 확장 함수, null safety

#### 개선 방향
- 트래픽 증가 시 Redis 도입 필요
- API 분리 시 Spring Cloud Gateway 고려

---

## 2. 아키텍처 (중요도: 상)

### Q2-1. 프로젝트의 전체 아키텍처를 설명해주세요.

#### 질문 의도
시스템 설계 능력과 계층 분리에 대한 이해 확인.

#### 답변 예시
```
전통적인 레이어드 아키텍처를 기반으로 합니다.

┌─────────────────────────────────────────────┐
│  Presentation Layer (Controller)            │
│  - HTTP 요청 수신, 응답 반환               │
│  - Bean Validation (@Valid)                │
│  - 인증/인가 체크                          │
└─────────────────────────────────────────────┘
                    │
┌─────────────────────────────────────────────┐
│  Business Layer (Service)                   │
│  - 비즈니스 로직 처리                      │
│  - 트랜잭션 관리 (@Transactional)          │
│  - AI 서비스 통합                          │
└─────────────────────────────────────────────┘
                    │
┌─────────────────────────────────────────────┐
│  Data Access Layer (Repository)             │
│  - JPA 기반 데이터 접근                    │
│  - Spring Data JPA 활용                    │
└─────────────────────────────────────────────┘
                    │
┌─────────────────────────────────────────────┐
│  External Integration                       │
│  - OpenAI API (RestTemplate)               │
│  - Jsoup (HTML 파싱)                       │
└─────────────────────────────────────────────┘

각 계층의 책임이 명확히 분리되어 있어서:
- Controller는 HTTP만 처리, 비즈니스 로직 없음
- Service는 순수 비즈니스 로직, DB 접근은 Repository 위임
- Repository는 데이터 접근만 담당
```

#### 추가 꼬리 질문
- "Controller에서 비즈니스 로직이 있으면 안 되는 이유는?" → 테스트 어려움, 재사용 불가, 단일 책임 원칙 위반
- "Service 계층을 더 세분화한 부분이 있나요?" → AI 관련은 service/ai, 캐시는 service/cache로 서브패키지 분리

---

### Q2-2. DDD나 클린 아키텍처를 적용했나요?

#### 질문 의도
아키텍처 패턴에 대한 깊은 이해와 실제 적용 경험 확인.

#### 답변 예시
```
완전한 DDD나 클린 아키텍처는 적용하지 않았습니다.
MVP 규모에서 과도한 추상화는 오히려 복잡도만 증가시킬 수 있다고 판단했습니다.

다만 일부 개념은 차용했습니다:

**DDD 요소**
- Aggregate Root: User, Question이 각각의 Aggregate
- Value Object: JobField, CareerLevel Enum
- Repository Pattern: Spring Data JPA 활용

**클린 아키텍처 요소**
- 의존성 역전: AiClient 인터페이스로 구현체 분리
  → OpenAiClientImpl을 다른 구현체로 교체 가능
- Use Case 분리: InterviewService, ReviewService 등 기능별 서비스

적용하지 않은 이유:
- 도메인 복잡도가 높지 않음 (CRUD 중심)
- 팀 규모가 작아서 (1인 개발) 오버헤드
- MVP 속도 우선

만약 복잡한 비즈니스 로직이 추가되면 도입을 고려하겠습니다.
예를 들어 결제, 구독 기능이 추가되면 Bounded Context 분리가 필요할 것입니다.
```

#### 추가 꼬리 질문
- "AiClient 인터페이스를 만든 구체적 이유는?" → OpenAI 외에 Claude, Gemini 등 교체 가능성
- "Aggregate를 잘못 설계하면 어떤 문제가 생기나요?" → 트랜잭션 범위 확대, 동시성 문제, 성능 저하

#### 개선 방향
- 도메인 이벤트 도입으로 서비스 간 결합도 감소
- CQRS 패턴으로 읽기/쓰기 분리 고려

---

### Q2-3. 패키지 구조를 어떻게 설계했나요?

#### 질문 의도
코드 조직화 능력과 유지보수성에 대한 고려 확인.

#### 답변 예시
```
기능별 + 계층별 하이브리드 구조입니다.

com.hojun.interviewnote.interviewnoteapi/
├── config/           # 설정 클래스
├── controller/       # 10개 컨트롤러
├── domain/           # 12개 엔티티 + Enum
├── dto/              # 데이터 전송 객체
├── exception/        # 예외 클래스
├── filter/           # HTTP 필터
├── health/           # Actuator 확장
├── repository/       # 9개 리포지토리
├── security/         # Spring Security 확장
└── service/          # 비즈니스 로직
    ├── ai/           # AI 통합 (OpenAI 클라이언트, 파서)
    │   └── prompt/   # 프롬프트 빌더
    ├── cache/        # 캐싱 서비스
    ├── ratelimit/    # Rate Limiting
    └── validation/   # 입력 검증

이 구조를 선택한 이유:
1. 새로운 기능 추가 시 위치가 명확
2. 관련 코드가 가까이 있어서 탐색 용이
3. 패키지 단위 접근 제어 가능

service/ai처럼 복잡한 기능은 서브패키지로 더 분리했습니다.
prompt/ 안에 FeedbackPromptBuilder, InterviewPromptBuilder 등
역할별로 클래스를 분리해서 단일 책임 원칙을 지켰습니다.
```

#### 추가 꼬리 질문
- "feature 기반 패키지 구조와 비교하면?" → 현재 규모에서는 계층 기반이 적합. 마이크로서비스 전환 시 feature 기반 고려.

---

## 3. Spring Boot (중요도: 상)

### Q3-1. 트랜잭션을 어떻게 관리하고 있나요?

#### 질문 의도
트랜잭션의 원리 이해와 실제 적용 경험 확인.

#### 답변 예시
```
선언적 트랜잭션(@Transactional)을 사용합니다.

기본 전략:
- Service 클래스에 @Transactional(readOnly = true) 기본 적용
- 쓰기 작업 메서드에만 @Transactional 명시

예시 (AiFeedbackService):
@Service
@Transactional(readOnly = true)
class AiFeedbackService {

    // 읽기 전용 (클래스 기본값 적용)
    fun findByAnswerId(answerId: Long): AiFeedback?

    // 쓰기 작업
    @Transactional
    fun generateFeedback(answer: InterviewAnswer, question: Question): AiFeedback {
        // AI 호출 + 파싱 + 저장
        // 실패 시 전체 롤백
    }
}

readOnly = true의 이점:
- Hibernate flush 모드 MANUAL로 변경 → 성능 향상
- 레플리카 DB 라우팅 가능
- 의도 명시로 코드 가독성 향상

주의한 점:
- AI 호출은 외부 I/O라서 트랜잭션 범위 최소화
- 긴 트랜잭션은 DB 커넥션 점유 시간 증가
- 실패 시 Fallback 피드백 생성으로 사용자 경험 보호
```

#### 추가 꼬리 질문
- "트랜잭션 전파(propagation)를 어떻게 설정했나요?" → 기본 REQUIRED 사용. 중첩 트랜잭션은 현재 불필요.
- "Checked Exception에서 롤백이 안 되는 이유는?" → Spring 기본 정책. 필요시 rollbackFor 속성 사용.
- "@Transactional이 같은 클래스 내부 호출에서 안 되는 이유는?" → 프록시 기반이라 self-invocation 불가. 별도 빈 분리 필요.

---

### Q3-2. 예외 처리 전략을 설명해주세요.

#### 질문 의도
예외 처리 설계 능력과 사용자 경험 고려 확인.

#### 답변 예시
```
계층화된 예외 클래스와 전역 예외 핸들러를 사용합니다.

1. **예외 클래스 계층** (sealed class 활용)

sealed class AiException(message: String) : RuntimeException(message)
├── AiApiException        # 네트워크, 인증 오류
├── AiResponseParseException  # JSON 파싱 실패 (rawResponse 포함)
├── AiRequestException    # 타임아웃
└── AiResponseException   # 빈 응답

sealed class MockInterviewException : RuntimeException
├── MockInterviewNotFoundException
├── MockInterviewAccessDeniedException
├── MaxTurnExceededException
└── InterviewAlreadyEndedException

2. **GlobalExceptionHandler**

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitExceededException::class)
    fun handleRateLimit(e: RateLimitExceededException): String {
        // 429 응답 + rate-limit 에러 페이지
    }

    @ExceptionHandler(AiException::class)
    fun handleAiException(e: AiException): String {
        // 디버깅용 rawResponse 로깅
        // 사용자에게는 친절한 에러 메시지
    }
}

3. **Fallback 메커니즘**

AI 오류 시 더미 피드백 반환으로 서비스 중단 방지:
catch (e: AiException) {
    logger.warn("AI 실패, 더미 피드백으로 fallback", e)
    return generateDummyFeedback(answer, question)
}
```

#### 추가 꼬리 질문
- "sealed class를 사용한 이유는?" → when 표현식에서 모든 케이스 강제, 새 예외 추가 시 컴파일 에러
- "rawResponse를 저장하는 이유는?" → 프로덕션 디버깅용. AI 응답 변경 감지.

#### 개선 방향
- REST API용 JSON 에러 응답 표준화
- 에러 코드 체계 수립 (E001, E002 등)

---

### Q3-3. Bean 관리는 어떻게 하고 있나요?

#### 질문 의도
Spring IoC/DI 이해도 확인.

#### 답변 예시
```
생성자 주입을 기본으로 사용합니다.

@Service
class InterviewService(
    private val questionService: QuestionService,
    private val answerRepository: InterviewAnswerRepository,
    private val aiFeedbackService: AiFeedbackService
) {
    // 모든 의존성이 생성자에서 주입
}

생성자 주입을 선택한 이유:
1. 불변성 보장 (final 필드)
2. 테스트 시 Mock 주입 용이
3. 순환 참조 컴파일 타임 감지
4. Kotlin에서 val로 선언 가능

@Bean 직접 등록 케이스:
- ObjectMapperConfig: Jackson 설정 중앙화
- RestTemplateConfig: 타임아웃 설정
- AsyncConfig: 스레드 풀 설정

@Configuration
class ObjectMapperConfig {
    companion object {
        val objectMapper: ObjectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    @Bean
    fun objectMapper(): ObjectMapper = objectMapper
}

ObjectMapper를 중앙화한 이유:
- 여러 곳에서 new ObjectMapper() 하면 메모리 낭비
- 설정 불일치 방지
- 테스트에서도 동일 인스턴스 사용
```

#### 추가 꼬리 질문
- "필드 주입(@Autowired)을 사용하지 않은 이유는?" → 테스트 어려움, 숨겨진 의존성, final 불가
- "순환 참조가 발생하면 어떻게 해결하나요?" → 설계 재검토 우선. 불가피하면 @Lazy 사용.

---

## 4. JPA (중요도: 상)

### Q4-1. N+1 문제를 어떻게 해결했나요?

#### 질문 의도
JPA 성능 최적화 경험과 문제 해결 능력 확인.

#### 답변 예시
```
배치 조회 패턴으로 해결했습니다.

**문제 상황 (ReviewService)**

리뷰 목록 100개 조회 시:
- 1회: 전체 답변 조회
- 100회: 각 답변의 질문 조회  (N+1)
- 100회: 각 답변의 피드백 조회 (N+1)
→ 총 201회 쿼리

**해결 방법: 배치 조회**

fun buildReviewSummariesBatch(answers: List<InterviewAnswer>): List<ReviewSummaryDto> {
    // 1. 필요한 모든 ID 수집
    val questionIds = answers.mapNotNull { it.questionId }
    val answerIds = answers.map { it.id }

    // 2. 배치 조회 (IN 절 사용)
    val questions = questionRepository.findAllById(questionIds)
        .associateBy { it.id }
    val feedbacks = aiFeedbackRepository.findAllByInterviewAnswerIdIn(answerIds)
        .associateBy { it.interviewAnswerId }

    // 3. 메모리에서 결합
    return answers.mapNotNull { answer ->
        val question = questions[answer.questionId] ?: return@mapNotNull null
        val feedback = feedbacks[answer.id] ?: return@mapNotNull null
        ReviewSummaryDto(answer, question, feedback)
    }
}

**결과**
- 201회 → 4회 쿼리 (약 50배 감소)
- 실행 시간: ~500ms → ~50ms

Repository 메서드:
fun findAllByInterviewAnswerIdIn(ids: List<Long>): List<AiFeedback>
```

#### 추가 꼬리 질문
- "FETCH JOIN을 사용하지 않은 이유는?" → 연관관계가 ID만 저장하는 구조라서 배치 조회가 더 적합
- "IN 절에 너무 많은 ID가 들어가면?" → 1000개 단위로 청킹. Hibernate의 in_clause_parameter_padding 설정 활용.

---

### Q4-2. Entity 설계에서 data class를 사용하지 않은 이유는?

#### 질문 의도
JPA와 Kotlin 통합에 대한 깊은 이해 확인.

#### 답변 예시
```
JPA 프록시와의 호환성 문제 때문입니다.

문제점:
1. data class의 equals/hashCode는 모든 필드를 비교
   → 프록시 객체와 실제 엔티티 비교 시 문제
   → 영속성 컨텍스트에서 동일 엔티티 인식 실패

2. data class의 copy() 메서드
   → JPA 엔티티는 영속성 상태를 가짐
   → copy() 시 상태 복제 안 됨

해결: 일반 class + ID 기반 equals/hashCode

@Entity
class Question(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val jobField: String,
    val content: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Question) return false
        return id != 0L && id == other.id  // ID만 비교
    }

    override fun hashCode(): Int = javaClass.hashCode()
}

이 패턴의 장점:
- 프록시 객체도 올바르게 비교
- 영속화 전(id=0)은 equals가 false 반환
- HashSet/HashMap에서 안정적으로 작동
```

#### 추가 꼬리 질문
- "hashCode()가 상수인 이유는?" → ID가 0에서 실제 값으로 변경되어도 해시 변경 안 됨. HashSet 일관성 유지.
- "data class를 써도 되는 경우는?" → Embeddable Value Object, DTO

---

### Q4-3. 연관관계를 어떻게 설계했나요?

#### 질문 의도
엔티티 간 관계 설계 능력과 트레이드오프 이해 확인.

#### 답변 예시
```
단방향 관계 + ID 참조를 기본으로 사용했습니다.

현재 설계:
@Entity
class InterviewAnswer(
    val questionId: Long?,           // FK만 저장, 연관관계 X
    val generatedQuestionId: Long?,  // FK만 저장
    val userId: Long                 // FK만 저장
)

양방향 연관관계를 피한 이유:
1. 순환 참조 위험 (JSON 직렬화 시 무한 루프)
2. 영속성 전이 복잡도 증가
3. 단일 엔티티 테스트 어려움
4. N+1 문제 발생 가능성

ID 참조의 단점:
- 조인 쿼리 시 명시적 JOIN 필요
- 타입 안전성 부족 (Long vs Question)

이 트레이드오프를 선택한 이유:
- 현재 도메인이 단순 (CRUD 중심)
- 배치 조회로 성능 문제 해결 가능
- 서비스 계층에서 조합으로 충분

양방향이 필요했다면:
- Aggregate 내부에서만 사용
- @JsonIgnore로 순환 방지
- mappedBy로 주인 명확히
```

#### 추가 꼬리 질문
- "JPA Cascade를 사용한 곳은?" → GeneratedQuestion(ON DELETE CASCADE from JobPosting), InterviewMessage(from MockInterview)
- "Aggregate 경계를 어떻게 정했나요?" → 트랜잭션 단위 + 생명주기 공유 기준. User, Question, MockInterview가 각각 Root.

---

## 5. API 설계 (중요도: 중)

### Q5-1. RESTful하게 설계했나요?

#### 질문 의도
REST 원칙에 대한 이해와 실제 적용 확인.

#### 답변 예시
```
REST 원칙을 부분적으로 따르고 있습니다.

**적용한 부분**

1. 리소스 기반 URL
   GET  /questions           # 질문 목록
   GET  /questions/{id}      # 질문 상세
   POST /questions/{id}/answer  # 답변 제출

2. HTTP 메서드 의미 준수
   GET: 조회 (멱등성)
   POST: 생성/행동

3. 상태 코드 활용
   200: 성공
   302: 리다이렉트 (폼 제출 후)
   404: 리소스 없음
   429: Rate Limit 초과

**완전히 RESTful하지 않은 부분**

1. Thymeleaf 기반이라 JSON API 아님
   → HTML 응답 반환

2. POST로 "행동" 표현
   POST /mock-interviews/{id}/end  # 면접 종료
   → PUT/PATCH가 더 적절할 수 있음

3. HATEOAS 미적용
   → 링크 기반 네비게이션 없음

이유:
- MVP에서 Thymeleaf SSR 선택
- 완전한 REST보다 실용성 우선
- API 버전 분리 시 REST API 별도 구축 예정
```

#### 추가 꼬리 질문
- "REST API로 분리한다면 어떻게?" → /api/v1 prefix, JSON 응답, JWT 인증

---

### Q5-2. 요청/응답 DTO 구조를 설명해주세요.

#### 질문 의도
데이터 전송 객체 설계와 검증 로직 확인.

#### 답변 예시
```
요청/응답 DTO를 분리하고 Bean Validation을 활용합니다.

**요청 DTO (입력 검증)**

data class AnswerSubmitDto(
    @field:NotNull
    val questionId: Long?,

    @field:NotBlank
    @field:Size(min = 50, max = 2000, message = "50-2000자 사이로 작성해주세요")
    val answerText: String?
)

data class RegisterForm(
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    val email: String,

    @field:Size(min = 8, max = 100, message = "비밀번호는 8-100자입니다")
    val password: String,

    @field:NotBlank
    val passwordConfirm: String,

    @field:Size(min = 2, max = 50)
    val name: String
)

**응답 DTO (팩토리 메서드 패턴)**

data class FeedbackDto(
    val logicScore: Int,
    val specificityScore: Int,
    val strengths: List<String>,
    val improvements: List<String>,
    val modelAnswer: String,
    val averageScore: Double
) {
    companion object {
        fun from(feedback: AiFeedback): FeedbackDto {
            // 엔티티 → DTO 변환 로직
            // JSON 문자열 파싱 등
        }
    }
}

DTO 분리 이유:
- 엔티티 직접 노출 방지 (보안, 불필요한 필드)
- API 스펙과 DB 스키마 분리
- 버전별 응답 형식 변경 용이
```

---

## 6. 데이터베이스 (중요도: 상)

### Q6-1. 인덱스 전략을 어떻게 수립했나요?

#### 질문 의도
데이터베이스 성능 최적화 경험 확인.

#### 답변 예시
```
총 30개의 인덱스를 쿼리 패턴 기반으로 설계했습니다.

**1. 조회 패턴 분석**

가장 빈번한 쿼리:
- 사용자별 답변 목록 (user_id, created_at DESC)
- 직무별 질문 필터링 (job_field, category)
- 중복 답변 감지 (user_id, question_id, answer_text_hash)

**2. 인덱스 설계**

-- 사용자별 리뷰 목록 (가장 빈번)
CREATE INDEX idx_interview_answers_user_created
ON interview_answers(user_id, created_at DESC);

-- 중복 답변 방지 (복합 인덱스)
CREATE INDEX idx_interview_answers_user_question_hash
ON interview_answers(user_id, question_id, answer_text_hash);

-- 직무별 질문 필터링
CREATE INDEX idx_questions_job_field ON questions(job_field);
CREATE INDEX idx_questions_category ON questions(category);

-- 로그인 최적화
CREATE INDEX idx_users_active_email ON users(is_active, email);

**3. 복합 인덱스 순서**

(user_id, created_at DESC)
→ user_id로 먼저 필터링 후 created_at 정렬
→ user_id 단독 조회에도 활용 가능

(user_id, question_id, answer_text_hash)
→ 중복 체크는 세 컬럼 모두 필요
→ WHERE user_id = ? AND question_id = ? AND answer_text_hash = ?
```

#### 추가 꼬리 질문
- "인덱스가 너무 많으면 문제는?" → INSERT/UPDATE 성능 저하, 스토리지 증가
- "커버링 인덱스를 사용한 곳은?" → 현재는 없음. 조회 최적화 시 고려.

---

### Q6-2. 마이그레이션을 어떻게 관리하나요?

#### 질문 의도
스키마 버전 관리와 배포 전략 확인.

#### 답변 예시
```
Flyway를 사용해서 버전 관리합니다.

**마이그레이션 파일 구조**
src/main/resources/db/migration/
├── V1__Create_tables.sql           # 기본 테이블
├── V2__Insert_questions.sql        # 시드 데이터
├── V3__Add_answer_text_hash.sql    # 캐시용 해시
├── V4__Create_users_table.sql      # 회원 관리
├── V5__Add_user_id_to_answers.sql  # 사용자 분리
├── V6__Add_job_field_career.sql    # 다중 직무
├── V7__Insert_multi_job_questions.sql  # 340개 질문
...
└── V14__Add_answer_text_hash.sql   # 중복 방지

**설정**
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration

**명명 규칙**
V{번호}__{설명}.sql
- V는 버전 (반드시 대문자)
- 번호는 순차적
- __는 언더스코어 두 개
- 설명은 snake_case

**주의 사항**
- 한 번 적용된 마이그레이션은 수정 금지
- 롤백 스크립트는 별도 관리 (현재 미구현)
- 프로덕션 배포 전 테스트 환경에서 검증
```

#### 추가 꼬리 질문
- "롤백은 어떻게 하나요?" → Flyway 유료 기능. 무료는 수동 롤백 스크립트 작성.
- "스키마 변경 시 무중단 배포는?" → 컬럼 추가 → 코드 배포 → 이전 컬럼 삭제 (3단계)

---

## 7. 인증/인가 (중요도: 중)

### Q7-1. JWT 대신 세션을 선택한 이유는?

#### 질문 의도
인증 방식의 트레이드오프 이해 확인.

#### 답변 예시
```
MVP 단계에서 세션의 단순함을 선택했습니다.

**세션 선택 이유**

1. Spring Security 기본 지원
   - 추가 라이브러리 불필요
   - 설정만으로 바로 사용

2. 서버 사이드 렌더링과 궁합
   - Thymeleaf + HTMX 구조
   - CSRF 토큰 자동 처리

3. 상태 관리 단순
   - 로그아웃 시 서버에서 세션 삭제
   - 토큰 블랙리스트 관리 불필요

4. 단일 인스턴스 MVP
   - 스티키 세션 불필요
   - 세션 클러스터링 불필요

**JWT가 필요한 상황**

- 모바일 앱 지원 시
- 마이크로서비스 간 인증
- 수평 확장 (다중 인스턴스)
- 외부 API 제공 시

**전환 계획**

API 분리 시 JWT 도입:
1. Spring Security OAuth2 Resource Server
2. Access Token + Refresh Token
3. Redis 기반 토큰 블랙리스트
```

#### 추가 꼬리 질문
- "JWT의 단점은?" → 토큰 크기, 강제 만료 어려움, 페이로드 노출
- "세션 클러스터링이 필요해지면?" → Spring Session + Redis 도입

---

### Q7-2. Rate Limiting을 어떻게 구현했나요?

#### 질문 의도
보안과 비용 제어에 대한 실질적 구현 경험 확인.

#### 답변 예시
```
3단계 Rate Limiting을 Caffeine Cache로 구현했습니다.

**1. AI 평가 요청**
- 제한: 33회/시간
- 대상: 사용자 ID
- 목적: AI API 비용 제어

**2. 질문 생성 요청**
- 제한: 10회/24시간
- 대상: 사용자 ID
- 목적: 채용공고 파싱 비용 제어

**3. 모의 면접 시작**
- 제한: 5회/24시간
- 대상: 사용자 ID
- 목적: 대화형 AI 비용 제어

**구현 (RateLimitService.kt)**

@Service
class RateLimitService {
    private val requestCache: Cache<String, MutableList<LocalDateTime>> =
        Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .build()

    fun checkAndRecordRequest(userId: Long) {
        synchronized(requestCache) {  // 동시성 제어
            val key = userId.toString()
            val requests = requestCache.get(key) { mutableListOf() }!!

            // 윈도우 내 요청만 유지
            val cutoff = LocalDateTime.now().minusMinutes(60)
            requests.removeIf { it.isBefore(cutoff) }

            if (requests.size >= MAX_REQUESTS_PER_HOUR) {
                val resetTime = requests.first().plusMinutes(60)
                throw RateLimitExceededException(key, 33, resetTime)
            }

            requests.add(LocalDateTime.now())
        }
    }
}

**Sliding Window 알고리즘**
- 고정 윈도우보다 정확
- 경계 시점 폭발 방지
- 메모리 효율적 (자동 만료)
```

#### 추가 꼬리 질문
- "분산 환경에서는?" → Redis + Lua Script로 원자적 연산 필요
- "33회는 어떻게 산정했나요?" → 월 $30 예산 기준 역산

---

## 8. 인프라 (중요도: 중)

### Q8-1. Docker 빌드를 어떻게 최적화했나요?

#### 질문 의도
컨테이너화 경험과 최적화 능력 확인.

#### 답변 예시
```
멀티스테이지 빌드로 이미지 크기를 최소화했습니다.

**빌드 전략**

# Stage 1: 빌드 (gradle:8.5-jdk21, ~800MB)
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app

# 의존성 캐싱 (레이어 분리)
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true

# 소스 빌드
COPY src ./src
RUN gradle build -x test --no-daemon

# Stage 2: 런타임 (eclipse-temurin:21-jre-alpine, ~180MB)
FROM eclipse-temurin:21-jre-alpine
COPY --from=builder /app/build/libs/*.jar app.jar

**최적화 포인트**

1. 레이어 캐싱
   - build.gradle.kts 변경 시에만 의존성 재다운로드
   - 소스 변경은 빠른 증분 빌드

2. JRE만 사용
   - JDK 전체 불필요
   - Alpine Linux 경량 이미지

3. 보안
   - 비루트 유저 실행
   - spring:spring 그룹 생성

4. JVM 최적화
   -XX:+UseContainerSupport  # 컨테이너 메모리 인식
   -XX:MaxRAMPercentage=75.0 # 75% 메모리 사용

**결과**
- 최종 이미지: ~180MB
- 빌드 시간: 캐시 히트 시 ~30초
```

#### 추가 꼬리 질문
- "distroless 이미지는 고려했나요?" → 디버깅 어려움으로 Alpine 선택
- "이미지 스캔은?" → 현재 미구현. Trivy 도입 계획.

---

### Q8-2. 현재 AWS 배포 구성은 어떤가요?

#### 질문 의도
실제 운영 환경 경험 확인.

#### 답변 예시
```
현재는 Docker Compose 기반 로컬 배포만 구현되어 있습니다.
AWS 배포는 계획 단계입니다.

**현재 구성 (docker-compose.yml)**

services:
  postgres:
    image: postgres:15-alpine
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U interviewuser"]

  app:
    build: .
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: prod
      OPENAI_API_KEY: ${OPENAI_API_KEY}

**AWS 배포 계획**

1. 최소 구성 (MVP)
   - EC2 t3.medium
   - RDS PostgreSQL (db.t3.micro)
   - Route 53 + ALB
   - HTTPS (ACM 인증서)

2. 확장 구성
   - ECS Fargate (컨테이너 오케스트레이션)
   - ElastiCache Redis (세션, 캐시)
   - CloudWatch (모니터링)

**현재 미구현 사항**
- CI/CD 파이프라인 (GitHub Actions 계획)
- Nginx 리버스 프록시
- HTTPS 설정
```

#### 추가 꼬리 질문
- "ECS vs EKS 선택 기준은?" → 규모와 팀 역량. 소규모면 ECS, K8s 경험 있으면 EKS.

#### 개선 방향
- GitHub Actions CI/CD 구축
- Terraform으로 IaC 관리

---

## 9. 성능 (중요도: 상)

### Q9-1. 캐싱 전략을 설명해주세요.

#### 질문 의도
성능 최적화 경험과 캐시 설계 능력 확인.

#### 답변 예시
```
2가지 캐싱 전략을 사용합니다.

**1. 중복 요청 캐싱 (DuplicateRequestCache)**

목적: 동일 답변에 대한 AI 호출 비용 절감

fun findCached(questionId: Long, answerText: String): AiFeedback? {
    val hash = generateHash(questionId, answerText)  // SHA-256
    val cutoffTime = LocalDateTime.now().minusHours(24)

    return aiFeedbackRepository
        .findByAnswerTextHashAndCreatedAtAfter(hash, cutoffTime)
}

효과:
- 캐시 히트 시: 5초 → 3ms (1,700배 빠름)
- 비용 절감: 99% (반복 질문 환경)

**2. 질문 캐싱 (QuestionCache)**

목적: 동일 채용 공고 URL의 질문 재생성 방지

fun findCachedQuestions(originalUrl: String): List<GeneratedQuestion> {
    val cutoffTime = LocalDateTime.now().minusDays(7)

    val cachedPosting = jobPostingRepository
        .findFirstByOriginalUrlAndCreatedAtAfterOrderByCreatedAtDesc(
            originalUrl, cutoffTime
        )

    return generatedQuestionRepository
        .findByJobPostingIdOrderByOrderIndexAsc(cachedPosting.id)
}

효과:
- 동일 공고 7일간 재사용
- AI 질문 생성 비용 0원

**Caffeine 설정**

val cache: Cache<String, MutableList<LocalDateTime>> =
    Caffeine.newBuilder()
        .expireAfterWrite(24, TimeUnit.HOURS)  // 24시간 TTL
        .maximumSize(10_000)                    // 최대 1만 건
        .build()
```

#### 추가 꼬리 질문
- "캐시 무효화는 어떻게?" → TTL 기반 자동 만료. 수동 무효화 없음.
- "Cache-Aside vs Write-Through?" → Cache-Aside 사용. DB가 Source of Truth.

---

### Q9-2. 동시성 문제를 어떻게 처리했나요?

#### 질문 의도
멀티스레드 환경에서의 문제 해결 능력 확인.

#### 답변 예시
```
세 가지 동시성 제어 메커니즘을 사용합니다.

**1. synchronized (RateLimitService)**

fun checkAndRecordRequest(userId: Long) {
    synchronized(requestCache) {
        // 캐시 읽기 + 수정이 원자적으로 실행
        val requests = requestCache.get(userId) { mutableListOf() }!!
        requests.removeIf { it.isBefore(cutoff) }

        if (requests.size >= MAX) throw RateLimitExceededException(...)
        requests.add(LocalDateTime.now())
    }
}

목적: Rate Limit 체크 + 기록의 원자성 보장

**2. @Transactional (AiFeedbackService)**

@Transactional
fun generateFeedback(...): AiFeedback {
    // 1. 캐시 확인
    // 2. AI 호출
    // 3. 파싱
    // 4. 저장
    // 실패 시 전체 롤백
}

목적: 데이터베이스 일관성 보장

**3. @Async + ThreadPoolTaskExecutor**

@Configuration
@EnableAsync
class AsyncConfig {
    @Bean
    fun taskExecutor(): Executor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 10
            maxPoolSize = 50
            queueCapacity = 100
            setThreadNamePrefix("async-interview-")
        }
    }
}

@Async("taskExecutor")
fun handleUserMessage(interviewId: Long, message: String) {
    // 별도 스레드에서 AI 응답 생성
    // SSE로 클라이언트에 전송
}

목적: 블로킹 없이 AI 응답 스트리밍

**ConcurrentHashMap (SseEmitterService)**

private val emitters = ConcurrentHashMap<Long, SseEmitter>()

목적: 다중 SSE 연결 관리
```

#### 추가 꼬리 질문
- "데드락 방지는?" → 단일 락만 사용, 락 순서 고정
- "스레드 풀 크기는 어떻게 정했나요?" → CPU 코어 수 기반. I/O 바운드라 코어 × 2~5.

---

### Q9-3. 트래픽이 100배 증가하면 어떻게 대응하겠습니까?

#### 질문 의도
확장성에 대한 사고 능력 확인.

#### 답변 예시
```
단계별 확장 전략을 적용하겠습니다.

**1단계: 수직 확장 (즉시)**
- EC2 인스턴스 크기 증가 (t3.medium → c5.xlarge)
- RDS 스펙 업그레이드
- 비용 대비 효과 좋음

**2단계: 캐시 계층 강화 (1주)**
- Redis 도입
  - 세션 저장 (Spring Session)
  - Rate Limit 분산 처리
  - 질문 목록 캐싱
- 캐시 히트율 목표: 80%+

**3단계: 수평 확장 (2주)**
- ECS/EKS 기반 오토스케일링
- ALB 로드밸런싱
- 스테이트리스 서버 구조
  - JWT 전환 (세션 → 토큰)
  - 외부 세션 저장소 (Redis)

**4단계: 데이터베이스 확장 (1개월)**
- Read Replica 추가
- 읽기/쓰기 분리 (@Transactional(readOnly))
- 필요시 샤딩 고려

**5단계: 아키텍처 개선 (장기)**
- 이벤트 기반 비동기 처리 (Kafka)
- AI 요청 큐잉 (SQS)
- CDN 정적 리소스 배포

현재 프로젝트에서 준비된 것:
✅ readOnly 트랜잭션 분리
✅ 배치 조회로 N+1 해결
✅ Rate Limiting 구조
❌ Redis 미사용
❌ CI/CD 미구현
```

---

## 10. 장애 대응 (중요도: 상)

### Q10-1. AI API 장애 시 어떻게 대응하나요?

#### 질문 의도
장애 복구 설계 능력 확인.

#### 답변 예시
```
Fallback 메커니즘으로 서비스 중단을 방지합니다.

**AiFeedbackService 구현**

fun generateFeedback(answer: InterviewAnswer, question: Question): AiFeedback {
    return try {
        // 1. 캐시 확인
        duplicateRequestCache.findCached(...)?.let { return it }

        // 2. 실제 AI 호출
        generateRealFeedback(answer, question)

    } catch (e: AiException) {
        // 3. AI 장애 시 Fallback
        logger.warn("AI 실패, 더미 피드백으로 fallback", e)
        generateDummyFeedback(answer, question)

    } catch (e: Exception) {
        logger.error("예상치 못한 오류", e)
        generateDummyFeedback(answer, question)
    }
}

**더미 피드백 로직**

private fun generateDummyFeedback(...): AiFeedback {
    val score = when {
        answer.answerText.length >= 500 -> 4
        answer.answerText.length >= 200 -> 3
        else -> 2
    }

    return AiFeedback(
        logicScore = score,
        specificityScore = score,
        // ...
        modelName = "fallback",  // 추적 가능
        overallComment = "AI 평가 중 오류 발생. 기본 평가를 제공합니다."
    )
}

**사용자 경험**
- 에러 페이지 대신 기본 평가 제공
- "더 자세한 피드백을 위해 다시 시도해주세요" 안내
- 이후 재시도 시 정상 평가 가능
```

#### 추가 꼬리 질문
- "Circuit Breaker는?" → 현재 미구현. Resilience4j 도입 고려.
- "더미 피드백도 저장하나요?" → 예. modelName="fallback"으로 구분 가능.

---

### Q10-2. 로깅/모니터링 전략을 설명해주세요.

#### 질문 의도
운영 가시성에 대한 이해 확인.

#### 답변 예시
```
3단계 모니터링 체계를 구축했습니다.

**1. 구조화된 로깅 (logback-spring.xml)**

개발: 텍스트 기반 (가독성)
14:23:45.123 [main] DEBUG - AI 호출 시작

프로덕션: JSON 기반 (자동 수집)
{
    "timestamp": "2026-06-20T14:23:45.123Z",
    "message": "AI 호출 시작",
    "requestId": "a1b2c3d4-e5f6-...",  # MDC
    "ip": "192.168.1.100",              # MDC
    "userId": "user-123"                # MDC
}

**2. 요청 추적 (RequestIdFilter)**

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(...) {
        val requestId = request.getHeader("X-Request-ID")
            ?: UUID.randomUUID().toString()

        MDC.put("requestId", requestId)
        MDC.put("ip", getClientIp(request))

        response.addHeader("X-Request-ID", requestId)

        try { filterChain.doFilter(request, response) }
        finally { MDC.clear() }
    }
}

효과: 분산 환경에서 요청 전체 추적 가능

**3. 메트릭 수집 (Prometheus + Micrometer)**

// 커스텀 메트릭 (AiFeedbackService)
private val aiCallsCounter = meterRegistry.counter("ai.calls.total")
private val aiCallsTimer = meterRegistry.timer("ai.calls.duration")
private val cacheHitsCounter = meterRegistry.counter("cache.hits")
private val tokenUsageCounter = meterRegistry.counter("ai.tokens.total")

수집 항목:
- ai.calls.total: AI 호출 횟수
- ai.calls.duration: AI 응답 시간
- cache.hits / cache.misses: 캐시 효율
- ai.tokens.total: 토큰 사용량 (비용 추적)

**4. 헬스 체크 (OpenAiHealthIndicator)**

@Component
class OpenAiHealthIndicator : HealthIndicator {
    override fun health(): Health {
        if (openAiProperties.apiKey.isBlank()) {
            return Health.down()
                .withDetail("error", "API 키 미설정")
                .build()
        }
        return Health.up()
            .withDetail("model", openAiProperties.model)
            .build()
    }
}

Kubernetes Liveness/Readiness 프로브 지원
```

#### 추가 꼬리 질문
- "Grafana 대시보드는?" → 현재 미구현. 메트릭 수집은 준비됨.
- "알람은?" → 미구현. CloudWatch Alarms 또는 Prometheus AlertManager 계획.

---

## 11. 테스트 (중요도: 상)

### Q11-1. 테스트 전략을 설명해주세요.

#### 질문 의도
테스트 설계 능력과 품질에 대한 인식 확인.

#### 답변 예시
```
테스트 피라미드 원칙을 따릅니다.

**테스트 현황**
- 44개 테스트 파일
- 약 11,161줄 테스트 코드
- Phase 1-8 모두 테스트 포함

**1. 단위 테스트 (Unit Tests)**

대상: Service, Validator, Parser
도구: JUnit 5 + Mockito

@ExtendWith(MockitoExtension::class)
class ResponseParserTest {
    @Mock
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `점수가 1-5 범위를 벗어나면 예외 발생`() {
        val invalidJson = """{ "scores": { "logic": 0 } }"""

        assertThrows<AiResponseParseException> {
            responseParser.parseOpenAiResponse(invalidJson)
        }
    }
}

특징:
- 외부 의존성 Mock
- 경계값 테스트
- 예외 케이스 검증

**2. 통합 테스트 (Integration Tests)**

대상: 전체 플로우, 데이터베이스 연동
도구: @SpringBootTest + @Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase5IntegrationTest {
    @Autowired
    private lateinit var questionRepository: QuestionRepository

    @Test
    fun `340개 질문 데이터 로드 검증`() {
        val allQuestions = questionRepository.findAll()
        assertEquals(340, allQuestions.size)
    }
}

특징:
- 실제 DB 연동 (H2)
- Flyway 마이그레이션 검증
- 트랜잭션 롤백으로 격리

**3. 웹 계층 테스트 (WebMvcTest)**

대상: Controller, 요청/응답 검증
도구: MockMvc

@WebMvcTest(AnswerController::class)
class AnswerControllerTest {
    @MockitoBean
    private lateinit var interviewService: InterviewService

    @Test
    @WithMockUser
    fun `답변 제출 시 피드백 페이지로 리다이렉트`() {
        mockMvc.perform(post("/questions/1/answer")
            .param("answerText", validAnswer)
            .with(csrf()))
            .andExpect(status().is3xxRedirection())
    }
}
```

#### 추가 꼬리 질문
- "테스트 커버리지는?" → 측정 도구 미설정. Jacoco 도입 계획.
- "E2E 테스트는?" → Selenium 미사용. 통합 테스트로 대체.

---

### Q11-2. Mock을 사용한 이유는?

#### 질문 의도
테스트 격리와 의존성 관리 이해 확인.

#### 답변 예시
```
외부 의존성 격리와 테스트 속도를 위해 Mock을 사용합니다.

**Mock 대상과 이유**

1. AI Client (필수 Mock)
   - 실제 호출 시 비용 발생 ($0.04/요청)
   - 네트워크 의존성 → 테스트 불안정
   - 응답 시간 3-10초 → 테스트 느림

2. Repository (선택적 Mock)
   - 단위 테스트: Mock 사용
   - 통합 테스트: 실제 DB (H2)

3. MeterRegistry
   - 메트릭 수집 무시
   - 테스트에서 불필요

**Mock 사용 패턴**

@ExtendWith(MockitoExtension::class)
class AiFeedbackServiceTest {
    @Mock
    private lateinit var aiClient: AiClient

    @Mock
    private lateinit var aiFeedbackRepository: AiFeedbackRepository

    @InjectMocks
    private lateinit var aiFeedbackService: AiFeedbackService

    @BeforeEach
    fun setUp() {
        whenever(aiClient.requestFeedback(any(), any()))
            .thenReturn(validJsonResponse)

        whenever(aiFeedbackRepository.save(any()))
            .thenAnswer { it.arguments[0] }  // 저장된 객체 반환
    }

    @Test
    fun `AI 응답 파싱 성공 시 피드백 저장`() {
        val result = aiFeedbackService.generateFeedback(answer, question)

        assertNotNull(result)
        verify(aiFeedbackRepository).save(any())
    }
}

**실제 객체 사용 케이스**

- DuplicateRequestCache: 해싱 로직 검증 필요
- AnswerValidator: 순수 로직, Mock 불필요
- ResponseParser: JSON 파싱 정확성 검증
```

---

## 12. 협업 관점 (중요도: 중)

### Q12-1. 코드 리뷰 시 어떤 점을 중점적으로 봐야 할까요?

#### 질문 의도
코드 품질에 대한 인식과 리뷰 역량 확인.

#### 답변 예시
```
이 프로젝트의 핵심 리뷰 포인트는 다음과 같습니다.

**1. AI 통합 코드**

파일: service/ai/OpenAiClientImpl.kt
체크:
- 타임아웃 설정 (30초)
- 에러 응답 처리
- rawResponse 로깅 여부

파일: service/ai/ResponseParser.kt
체크:
- 점수 범위 검증 (1-5)
- JSON 파싱 실패 처리
- 경계값 테스트 존재 여부

**2. 캐싱 코드**

파일: service/cache/DuplicateRequestCache.kt
체크:
- SHA-256 해싱 정확성
- TTL 설정 적절성 (24시간)
- 동시성 이슈 (synchronized)

**3. Rate Limiting**

파일: service/ratelimit/RateLimitService.kt
체크:
- synchronized 범위 적절성
- 슬라이딩 윈도우 구현 정확성
- 에러 메시지 명확성

**4. 트랜잭션**

파일: service/InterviewService.kt
체크:
- @Transactional 범위
- 외부 호출(AI)과 DB 작업 분리
- 롤백 케이스 고려

**5. N+1 쿼리**

파일: service/ReviewService.kt
체크:
- 배치 조회 사용 여부
- findAllById 활용
- 메모리 조합 로직 정확성
```

---

### Q12-2. 이 프로젝트의 컨벤션은 어떻게 정했나요?

#### 질문 의도
코딩 표준에 대한 인식 확인.

#### 답변 예시
```
Google Kotlin Style Guide를 기반으로 합니다.

**코드 스타일**
- 들여쓰기: 4 spaces
- 최대 줄 길이: 100자
- Import: 와일드카드 금지, 알파벳 순

**네이밍 컨벤션**
- 클래스: PascalCase (InterviewService)
- 함수/변수: camelCase (submitAnswer)
- 상수: UPPER_SNAKE_CASE (MAX_REQUESTS_PER_HOUR)
- Nullable: 명시적 ? 사용

**패키지 구조**
- 계층별 분리 (controller, service, repository)
- 복잡한 기능은 서브패키지 (service/ai/prompt)

**DTO 규칙**
- 요청: XxxRequest, XxxDto
- 응답: XxxDto, XxxResponse
- companion object로 팩토리 메서드

**테스트 네이밍**
- 백틱 사용: `점수가 1-5 범위를 벗어나면 예외 발생`
- Given-When-Then 구조

**커밋 메시지**
- feat: 새 기능
- fix: 버그 수정
- refactor: 리팩토링
- docs: 문서
- test: 테스트
```

---

## 13. 시니어 레벨 검증 (중요도: 상)

### Q13-1. 이 프로젝트에서 가장 어려웠던 기술적 결정은?

#### 질문 의도
복잡한 문제 해결 경험과 의사결정 능력 확인.

#### 답변 예시
```
AI 비용 최적화와 사용자 경험의 균형이 가장 어려웠습니다.

**문제 상황**
- gpt-4o-mini도 호출당 ~$0.04 비용
- 무제한 호출 시 월 비용 폭발
- 그러나 Rate Limit이 너무 빡빡하면 UX 저하

**의사결정 과정**

1. 비용 분석
   - 예산: 월 $30
   - 호출당 비용: $0.04
   - 가능 횟수: 750회/월

2. 사용 패턴 분석
   - 동일 질문 반복 답변 가능성 높음
   - 학습 목적이라 유사 답변 제출 빈번

3. 해결책 도출
   - SHA-256 해싱으로 중복 감지
   - 24시간 캐싱으로 동일 답변 재사용
   - 캐시 히트 시 AI 호출 스킵

4. 결과
   - 캐시 히트율 예상: 30-50%
   - 실제 비용: 예산의 50-70%
   - UX: 캐시 히트 시 즉시 응답 (3ms)

**대안 검토**

A. 더 저렴한 모델 (gpt-3.5)
   → 품질 저하 우려로 기각

B. 프롬프트 최적화
   → 토큰 20% 절감, 채택

C. 답변 길이 제한 강화
   → UX 저하로 기각

이 결정에서 배운 점:
- 기술 선택에는 항상 비용이 있다
- 캐싱은 비용과 성능 모두 개선 가능
- 사용자 행동 패턴 분석이 중요
```

---

### Q13-2. 다시 처음부터 만든다면 무엇을 바꾸겠습니까?

#### 질문 의도
회고 능력과 성장 마인드셋 확인.

#### 답변 예시
```
세 가지를 바꾸겠습니다.

**1. 처음부터 Redis 도입**

현재 상태:
- Caffeine Cache (in-memory)
- 단일 인스턴스에서만 작동

문제:
- 서버 재시작 시 캐시 손실
- 수평 확장 불가

변경:
- Spring Session + Redis
- 분산 Rate Limiting
- 캐시 영속성

**2. API 우선 설계 (API-First)**

현재 상태:
- Thymeleaf SSR
- HTML 응답

문제:
- 모바일 앱 지원 어려움
- API 스펙 문서화 부족

변경:
- OpenAPI Spec 먼저 작성
- Spring Cloud Gateway
- REST API + Swagger UI
- 프론트엔드 분리 (Next.js)

**3. 이벤트 기반 아키텍처**

현재 상태:
- 동기 처리 (요청 → AI → 응답)
- 긴 응답 시간 (3-10초)

문제:
- AI 호출 시 HTTP 커넥션 점유
- 타임아웃 위험

변경:
- 요청 → 큐(SQS) → Worker → 웹소켓 푸시
- AI 처리 비동기화
- 즉시 응답 + 백그라운드 처리

**하지만 MVP에서는 현재 선택이 적절했다고 생각합니다.**
- 빠른 검증이 목표
- 1인 개발 리소스 한계
- 과도한 설계는 오버엔지니어링
```

---

### Q13-3. 운영 중 DB 부하가 급증하면 어떻게 대응하겠습니까?

#### 질문 의도
운영 경험과 문제 해결 역량 확인.

#### 답변 예시
```
단계별로 대응하겠습니다.

**1단계: 즉시 대응 (5분 내)**

1. 슬로우 쿼리 확인
   - PostgreSQL pg_stat_statements
   - 상위 10개 쿼리 분석

2. 커넥션 풀 확인
   - HikariCP 모니터링
   - 대기 커넥션 수 확인

3. 캐시 히트율 확인
   - DuplicateRequestCache 효율
   - 캐시 미스 급증 여부

**2단계: 단기 조치 (1시간 내)**

1. 인덱스 추가
   - EXPLAIN ANALYZE로 실행 계획 확인
   - 누락된 인덱스 추가

2. 쿼리 최적화
   - N+1 쿼리 발견 시 배치 조회 전환
   - 불필요한 SELECT * 제거

3. Rate Limit 강화
   - 임시로 제한 수치 감소
   - 트래픽 분산

**3단계: 중기 조치 (1일)**

1. Read Replica 추가
   - @Transactional(readOnly = true) 쿼리 분리
   - 읽기/쓰기 라우팅

2. 커넥션 풀 튜닝
   - maximumPoolSize 조정
   - connectionTimeout 최적화

3. 캐시 계층 강화
   - Redis 도입
   - 질문 목록 캐싱

**현재 프로젝트에서 준비된 것**

✅ 30개 인덱스 사전 설계
✅ readOnly 트랜잭션 분리
✅ 배치 조회 패턴 적용
✅ Rate Limiting 구조

❌ Read Replica 미구성
❌ 쿼리 모니터링 미설정
❌ 알람 시스템 미구축
```

---

## 부록: 프로젝트 약점 요약

면접에서 솔직하게 인정하고 개선 방향을 제시할 부분입니다.

| 약점 | 현재 상태 | 개선 방향 |
|------|---------|---------|
| CI/CD | 미구현 | GitHub Actions 구축 |
| Redis | 미사용 | 분산 캐시/세션 도입 |
| JWT | 미사용 (세션) | API 분리 시 전환 |
| Nginx | 미구현 | 리버스 프록시 + SSL |
| 부하 테스트 | 미실시 | k6, Gatling 도입 |
| 테스트 커버리지 | 미측정 | Jacoco 설정 |
| 알람 | 미구현 | CloudWatch Alarms |
| Circuit Breaker | 미구현 | Resilience4j 도입 |

---

## 부록: 기술 면접 Tip

### 답변 구조 (STAR 기법)
1. **S**ituation: 상황/문제 설명
2. **T**ask: 해결해야 할 과제
3. **A**ction: 실제 수행한 행동
4. **R**esult: 결과 및 배운 점

### 모르는 질문 대처
```
"정확히는 모르지만, 제가 아는 범위에서 말씀드리면..."
"이 부분은 깊이 다루지 않았는데, 면접 후 공부해보겠습니다."
```

### 코드 레벨 질문 대비
- 핵심 파일 위치 암기
- 주요 메서드 흐름 설명 준비
- 실제 코드 라인 언급하면 신뢰도 상승

---

> **문서 작성**: Claude Code
> **최종 업데이트**: 2026-06-20
