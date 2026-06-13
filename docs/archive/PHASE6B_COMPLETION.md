# Phase 6B: AI 질문 생성 - 완료 보고서

**작성일**: 2026-04-28
**상태**: ✅ 완료
**테스트**: 268개 테스트 모두 통과

---

## 📋 완료된 작업

### 1. QuestionResponseParser (AI 응답 파싱)

#### 파일
- `src/main/kotlin/.../service/ai/QuestionResponseParser.kt`

#### 기능
- **parseQuestionResponse()**: OpenAI JSON 응답을 ParsedQuestions로 파싱
- **JSON 스키마 검증**:
  - 질문 개수: 정확히 10개
  - difficulty: EASY/MEDIUM/HARD만 허용
  - content: 10-500자
  - reasoning: 10-500자
  - category: 비어있지 않음
- **난이도 분포 로그**: EASY/MEDIUM/HARD 개수 기록
- **inferredJobField 변환**: JobField enum으로 변환

#### DTOs
- `ParsedQuestions`: 파싱 결과 (inferredJobField, questions)
- `GeneratedQuestionData`: 개별 질문 데이터
- `OpenAiQuestionResponse`: OpenAI 응답 구조 (내부용)

---

### 2. PromptBuilder 확장

#### 파일
- `src/main/kotlin/.../service/ai/PromptBuilder.kt` (기존 파일 확장)

#### 추가된 메서드

##### buildQuestionGenerationSystemPrompt()
- 채용 공고 기반 질문 생성용 System Prompt
- 17개 직무별 카테고리 제공
- 난이도 분포 권장 (EASY 3, MEDIUM 4, HARD 3)
- STAR 기법 유도
- 깊이 있는 기술 질문 강조

##### buildQuestionGenerationUserPrompt()
- 공고 내용 전달 (jobDescription, requiredSkills, preferredSkills)
- 기술 스택 섹션 자동 구성

##### getCategoriesForJobField()
- 17개 직무별 카테고리 맵핑
- 예시:
  - IT: "기술역량", "문제해결", "협업경험"
  - SALES: "고객관리", "실적달성", "협상스킬"
  - MARKETING: "캠페인기획", "데이터분석", "콘텐츠전략"

---

### 3. QuestionGeneratorService

#### 파일
- `src/main/kotlin/.../service/QuestionGeneratorService.kt`

#### 핵심 기능

##### generateQuestions()
- 채용 공고 기반 질문 10개 생성
- 흐름:
  1. effectiveJobField 확인
  2. PromptBuilder로 System/User 프롬프트 생성
  3. AiClient로 OpenAI 호출
  4. QuestionResponseParser로 응답 파싱
  5. GeneratedQuestion 엔티티 리스트 생성
  6. AI 실패 시 Fallback 질문 반환

##### generateFallbackQuestions()
- AI 실패 시 사용되는 일반적인 질문 10개
- 직무에 상관없이 사용 가능한 범용 질문
- 예: "자기소개와 지원 동기", "강점과 약점", "팀 갈등 해결 경험"

##### Micrometer 메트릭
- `question_generation.success`: 성공 카운터 (job_field별)
- `question_generation.failure`: 실패 카운터 (job_field별)
- `question_generation.duration`: 소요 시간 (job_field별)

---

### 4. RateLimitService 확장

#### 파일
- `src/main/kotlin/.../service/ratelimit/RateLimitService.kt` (기존 파일 확장)

#### 추가된 기능

##### checkAndRecordQuestionGeneration()
- 사용자당 10회/24시간 제한
- Caffeine Cache 기반 (24시간 자동 만료)
- 초과 시 RateLimitExceededException 발생

##### getCurrentQuestionGenerationCount()
- 테스트용 메서드
- 현재 사용자의 24시간 내 질문 생성 횟수 반환

#### 설정
```kotlin
private const val MAX_QUESTION_GENERATIONS_PER_DAY = 10
private const val QUESTION_GENERATION_WINDOW_HOURS = 24L
```

---

### 5. JobPostingCache

#### 파일
- `src/main/kotlin/.../service/cache/JobPostingCache.kt`

#### 기능

##### findCachedByUrl()
- 동일 URL의 공고를 7일 이내에 재생성하지 않도록 방지
- Repository 활용 (findFirstByOriginalUrlAndCreatedAtAfterOrderByCreatedAtDesc)
- 비용 절감 및 중복 방지

##### isCached()
- 빠른 캐시 존재 여부 확인

#### 설정
```kotlin
private const val CACHE_DURATION_DAYS = 7L
```

---

### 6. 테스트

#### Phase6BIntegrationTest
- **파일**: `src/test/kotlin/.../Phase6BIntegrationTest.kt`
- **테스트 개수**: 11개
- **테스트 범위**:
  1. generateQuestions는 10개의 질문을 생성한다
  2. 생성된 질문은 데이터베이스에 저장된다
  3. 난이도 분포는 EASY, MEDIUM, HARD를 모두 포함한다
  4. effectiveJobField가 null인 경우 Fallback 질문이 생성된다
  5. Fallback 질문은 10개를 생성한다
  6. 질문 생성은 10회까지 허용된다
  7. 질문 생성 11회째는 RateLimitExceededException이 발생한다
  8. 다른 사용자는 독립적인 Rate Limit을 가진다
  9. 동일 URL의 공고는 7일 내 캐시에서 조회된다
  10. 7일 이후 공고는 캐시에서 조회되지 않는다
  11. isCached 메서드는 캐시 존재 여부를 반환한다

---

## ✅ 검증 결과

### 전체 테스트 통과
```
BUILD SUCCESSFUL
268 tests completed, 0 failed, 0 skipped
```

### Phase6B 테스트 통과
```
11 tests completed, 0 failed
```

---

## 📊 코드 통계

### 신규 파일
- Service: 2개 (QuestionGeneratorService, JobPostingCache)
- Parser: 1개 (QuestionResponseParser)
- Test: 1개 (11개 테스트 케이스)

### 확장된 파일
- PromptBuilder: 3개 메서드 추가 (~150줄)
- RateLimitService: 2개 메서드 추가 (~50줄)

### 총 라인 수 (추정)
- 프로덕션 코드: ~700줄
- 테스트 코드: ~300줄

---

## 🎯 완료 기준 충족

- [x] QuestionGeneratorService 구현 (AI 통합)
- [x] PromptBuilder 확장 (17개 직무 프롬프트)
- [x] QuestionResponseParser (JSON 검증)
- [x] Rate Limiting (10회/24시간)
- [x] 7일 URL 캐싱
- [x] Micrometer 메트릭 (counter, timer)
- [x] 통합 테스트: End-to-end 질문 생성
- [x] 전체 테스트 통과 (268개)

---

## 💰 비용 분석 (추정)

### AI API 비용 (OpenAI gpt-4o-mini)

**질문 생성 1회당**:
- 입력: 공고 내용 (평균 2,000자) + 프롬프트 (500자) ≈ 1,250 토큰
- 출력: 10개 질문 + 근거 ≈ 1,500 토큰
- **비용**: $0.00375 (입력) + $0.00225 (출력) = **$0.006/회**

**월간 비용** (100명 사용자, 각 10회):
- 100명 × 10회 × $0.006 = **$6/월**

**절감 효과**:
- 7일 캐싱: 동일 URL 재생성 방지 → 약 30% 절감
- Rate Limiting: 1일 10회 제한 → 남용 방지
- **예상 실제 비용**: **$4.2/월** (100명 기준)

---

## 🚀 주요 기능

### 1. AI 기반 맞춤형 질문 생성
- 채용 공고 분석하여 10개 질문 자동 생성
- 필수 기술, 우대 기술 반영
- STAR 기법 유도 질문

### 2. 17개 직무 지원
- IT, SALES, MARKETING, PLANNING, ACCOUNTING, HR, ADMIN, DESIGN, MD, SERVICE, PRODUCTION, CONSTRUCTION, MEDICAL, EDUCATION, MEDIA, FINANCE, PUBLIC
- 직무별 카테고리 자동 매핑

### 3. 비용 제어
- Rate Limiting: 10회/24시간
- 7일 URL 캐싱
- AI 실패 시 Fallback 질문

### 4. 품질 보장
- 엄격한 JSON 스키마 검증
- 난이도 분포 검증
- 질문 길이 제한

### 5. 모니터링
- Micrometer 메트릭 (성공/실패/소요시간)
- 구조화된 로깅

---

## 📝 개선 사항 (Phase 6A → 6B)

1. **AI 통합 완료**: Phase 6A의 파싱 인프라를 활용한 실제 질문 생성
2. **비용 최적화**: Rate Limiting + 7일 캐싱으로 월 비용 $4.2 수준 유지
3. **Fallback 메커니즘**: AI 실패 시에도 서비스 중단 없음
4. **메트릭 수집**: 프로덕션 모니터링 준비 완료

---

## 🔜 다음 단계 (Phase 6C)

Phase 6C에서 진행할 작업:
1. **JobPostingController** 구현
   - GET /job-postings/create (공고 등록 폼)
   - POST /job-postings (공고 등록 + 질문 생성)
   - GET /job-postings/{id}/questions (질문 목록)
2. **JobPostingService** (Orchestration)
   - Rate limit → 7일 캐시 체크 → 파싱 → 저장 → 질문 생성
3. **UI 템플릿** (Thymeleaf + Tailwind CSS)
   - create.html (URL 입력, 직무 선택)
   - questions.html (공고 정보 + 질문 10개)
4. **기존 답변 플로우 연결**
   - InterviewAnswer에 generatedQuestionId 추가
   - V10 마이그레이션

---

**작성자**: Claude Code
**Phase 6B 완료일**: 2026-04-28
**상태**: ✅ 모든 기능 정상 동작, 테스트 통과, AI 연동 완료
