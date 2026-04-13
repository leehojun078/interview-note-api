# 답변 검증 개선 계획

**작성일**: 2026-04-13
**상태**: 구현 대기
**우선순위**: 높음

## 현재 문제점

### 발견된 이슈

**테스트 케이스**:
```
질문: 코드 리뷰 시 중요하게 생각하는 것은?
답변: "제가 중요하게 여기는 여기는 여기는 여기는 여기는 여기는 여기는 여기는 여기는 여기는 여기는."
```

**문제**:
1. ✅ **사전 검증 통과**: 문자 단위 반복은 아니라서 검증 통과
2. ❌ **AI Hallucination**: 강점에 "코드 리뷰의 중요성을 인식하고 있다" 같은 엉뚱한 피드백 생성
3. ❌ **신뢰도 저하**: 답변에 없는 내용을 AI가 창작

### 근본 원인

1. **사전 검증의 한계**:
   - 현재는 **문자 단위** 반복만 체크 (`"aaaa..."`)
   - **단어 단위** 반복은 감지 못함 (`"여기는 여기는 여기는"`)

2. **프롬프트 부족**:
   - "무의미한 답변은 솔직히 지적하라"는 지침 없음
   - "답변에 없는 내용을 창작하지 마라"는 명시 없음
   - AI가 억지로 강점을 찾으려 함

---

## 개선 계획

### 옵션 1: 프롬프트 개선 (최우선)

#### 구현 위치
`src/main/kotlin/.../service/ai/PromptBuilder.kt`

#### 수정 내용

**현재 System Role**:
```kotlin
"""
당신은 백엔드 개발자 면접을 준비하는 지원자를 돕는 면접 코치입니다.
당신의 역할은 합격/불합격을 판정하는 것이 아니라, 답변을 개선하도록 구체적인 피드백을 제공하는 것입니다.

평가 기준:
- 논리성(logic): 기술적 사고의 논리적 흐름과 일관성
- 구체성(specificity): 구체적 기술 스택, 사례, 수치 제시 정도
- 직무 적합성(jobFit): 질문 의도와 개발 직무 연관성
- 전달력(delivery): 기술 개념을 명확하고 이해하기 쉽게 설명하는 능력

출력 규칙:
- 반드시 JSON 형식으로 응답
- 각 점수는 1-5 사이 정수
- strengths와 improvements는 각각 2-3개 항목
- modelAnswer는 400-600자 이내
- 한국어로 답변
- 과도한 단정이나 공격적 표현 금지
"""
```

**개선 후 System Role** (추가할 내용):
```kotlin
"""
당신은 백엔드 개발자 면접을 준비하는 지원자를 돕는 면접 코치입니다.
당신의 역할은 합격/불합격을 판정하는 것이 아니라, 답변을 개선하도록 구체적인 피드백을 제공하는 것입니다.

평가 기준:
- 논리성(logic): 기술적 사고의 논리적 흐름과 일관성
- 구체성(specificity): 구체적 기술 스택, 사례, 수치 제시 정도
- 직무 적합성(jobFit): 질문 의도와 개발 직무 연관성
- 전달력(delivery): 기술 개념을 명확하고 이해하기 쉽게 설명하는 능력

중요한 평가 지침:
1. **정직한 평가**: 답변이 반복적이거나 무의미하면 솔직하게 지적하세요
2. **사실 기반**: 답변에 없는 내용을 추측하거나 창작하지 마세요
3. **강점 검증**: 실제로 답변에 나타난 강점만 언급하세요
4. **반복 표현 감지**: 같은 단어/문구가 반복되면 improvements에 "반복 표현을 피하고 구체적인 경험과 예시를 들어 설명"을 포함하세요
5. **내용 부족 시**: 답변이 짧거나 구체성이 부족하면 strengths를 억지로 만들지 말고, improvements를 더 구체적으로 작성하세요
6. **엄격한 기준**: 형식적이거나 추상적인 답변은 낮은 점수를 주세요

나쁜 답변 예시:
- 반복 표현: "저는 중요하게 여기는 여기는 여기는..."
- 추상적 답변: "저는 열심히 노력했습니다"
- 구체성 부족: "Spring을 사용했습니다" (어떻게? 왜? 무엇을?)

이런 경우:
- strengths: 가능한 한 적게 (또는 비어있어도 됨)
- improvements: 구체적이고 실질적인 개선 방향 제시
- 점수: 1-2점 (매우 낮게)

출력 규칙:
- 반드시 JSON 형식으로 응답
- 각 점수는 1-5 사이 정수
- strengths는 1-5개 (내용이 부실하면 적게)
- improvements는 1-5개 (내용이 부실하면 많게)
- modelAnswer는 400-600자 이내
- 한국어로 답변
- 과도한 단정이나 공격적 표현 금지
"""
```

#### 기대 효과
- AI가 "여기는 여기는..." 같은 반복 표현을 improvements에 지적
- Hallucination 감소 (답변에 없는 내용 창작 안 함)
- 낮은 점수 → 저품질 경고 활성화

---

### 옵션 2: 사전 검증 강화 (단어 반복 감지)

#### 구현 위치
`src/main/kotlin/.../service/validation/AnswerValidator.kt`

#### 추가할 검증 로직

**새로운 검증 함수**:
```kotlin
/**
 * 단어 단위 반복 체크
 *
 * 예: "여기는 여기는 여기는"처럼 같은 단어가 40% 이상이면 true
 */
private fun hasExcessiveRepeatedWords(text: String): Boolean {
    val words = text.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }

    if (words.size < 5) return false  // 단어가 5개 미만이면 체크 안 함

    // 각 단어의 출현 횟수 계산 (대소문자 무시)
    val wordCounts = words
        .map { it.lowercase() }
        .groupingBy { it }
        .eachCount()

    val maxWordCount = wordCounts.values.maxOrNull() ?: 0
    val wordRepeatRatio = maxWordCount.toDouble() / words.size

    // 같은 단어가 40% 이상이면 true
    return wordRepeatRatio > 0.4
}
```

**validate() 함수에 추가**:
```kotlin
fun validate(answerText: String): ValidationResult {
    val trimmed = answerText.trim()

    // 1. 반복 문자 체크 (기존)
    if (hasExcessiveRepeatedChars(trimmed)) {
        return ValidationResult.Invalid(
            "반복되는 문자가 너무 많습니다. 의미 있는 답변을 작성해주세요."
        )
    }

    // 2. 반복 단어 체크 (신규)
    if (hasExcessiveRepeatedWords(trimmed)) {
        return ValidationResult.Invalid(
            "같은 단어가 반복되고 있습니다. 다양한 표현으로 구체적으로 작성해주세요."
        )
    }

    // 3. 고유 문자 개수 체크 (기존)
    // ... 나머지 기존 로직
}
```

#### 상수 추가
```kotlin
companion object {
    private const val MAX_REPEATED_CHAR_RATIO = 0.7   // 70% 이상 같은 문자
    private const val MAX_REPEATED_WORD_RATIO = 0.4   // 40% 이상 같은 단어 (신규)
    private const val MIN_WORD_COUNT = 10             // 최소 10개 단어
    private const val MIN_UNIQUE_CHAR_COUNT = 5       // 최소 5개의 서로 다른 문자
    private const val MIN_MEANINGFUL_CHAR_RATIO = 0.5 // 50% 이상 의미 있는 문자
}
```

#### 테스트 케이스 추가

**AnswerValidatorTest.kt**:
```kotlin
@Test
fun `validate - 단어 반복이 40% 이상이면 Invalid 반환`() {
    // Given - "여기는"이 11개 중 7개 (63%)
    val invalidAnswer = "제가 중요하게 여기는 여기는 여기는 여기는 여기는 여기는 여기는 입니다"

    // When
    val result = validator.validate(invalidAnswer)

    // Then
    assertTrue(result is ValidationResult.Invalid)
    if (result is ValidationResult.Invalid) {
        assertTrue(result.message.contains("같은 단어가 반복"))
    }
}

@Test
fun `validate - 단어 반복이 40% 미만이면 Valid 반환`() {
    // Given - "Spring"이 10개 중 3개 (30%)
    val validAnswer = "저는 Spring Boot를 사용하여 Spring Security와 Spring Data JPA로 RESTful API를 개발한 경험이 있습니다"

    // When
    val result = validator.validate(validAnswer)

    // Then
    assertTrue(result is ValidationResult.Valid)
}

@Test
fun `validate - 정상적인 답변에서 자주 쓰이는 단어는 허용`() {
    // Given - "개발"이라는 단어가 여러 번 나오지만 의미 있는 답변
    val validAnswer = """
        저는 백엔드 개발 경험이 3년 있습니다.
        주로 웹 애플리케이션 개발을 했고,
        최근에는 마이크로서비스 개발도 경험했습니다.
        개발 과정에서 TDD를 적용하여 품질을 높였습니다.
    """.trimIndent()

    // When
    val result = validator.validate(validAnswer)

    // Then
    assertTrue(result is ValidationResult.Valid)
}
```

#### 기대 효과
- "여기는 여기는 여기는..." 같은 패턴 사전 차단
- AI API 호출 전 거부 → **비용 절감**
- 사용자에게 즉각적인 피드백

#### 주의사항
- **임계값 조정**: 40%가 너무 엄격하면 35%로 완화
- **불용어 처리**: "저는", "입니다" 같은 조사는 제외할지 고려 (선택)
- **False Positive**: 정상 답변에서 특정 기술용어(Spring, JPA 등)가 많이 쓰일 수 있음

---

## 구현 순서

### Phase 1: 프롬프트 개선 (15분)
1. `PromptBuilder.kt` 수정
2. System Role에 평가 지침 추가
3. 빌드 및 테스트

### Phase 2: 사전 검증 강화 (20분)
1. `AnswerValidator.kt`에 `hasExcessiveRepeatedWords()` 추가
2. `validate()` 함수에 단어 반복 체크 추가
3. 상수 정의 추가
4. `AnswerValidatorTest.kt`에 테스트 케이스 3개 추가
5. 전체 테스트 실행

### Phase 3: 검증 (10분)
1. 실제 "여기는 여기는..." 답변으로 테스트
2. AI 피드백 품질 확인
3. 사전 검증이 제대로 작동하는지 확인

**총 예상 시간**: 45분

---

## 테스트 시나리오

### 시나리오 1: 단어 반복 (사전 검증)
**입력**:
```
질문: 코드 리뷰 시 중요하게 생각하는 것은?
답변: "제가 중요하게 여기는 여기는 여기는 여기는 여기는 여기는 여기는 여기는 여기는 여기는."
```

**기대 결과**:
- ❌ 사전 검증 실패
- 에러 메시지: "같은 단어가 반복되고 있습니다. 다양한 표현으로 구체적으로 작성해주세요."
- AI API 호출 안 됨

### 시나리오 2: 추상적 답변 (AI 평가)
**입력**:
```
질문: 성능 개선 경험을 설명해주세요
답변: "저는 열심히 노력했고 성능을 많이 개선했습니다. 팀원들도 만족했습니다."
```

**기대 결과**:
- ✅ 사전 검증 통과 (형식적으로는 문제 없음)
- AI 평가:
  - 점수: 1-2점 (매우 낮음)
  - strengths: [] 또는 최소
  - improvements: ["구체적인 지표와 수치 제시 필요", "어떤 기술/방법을 사용했는지 명시"]
- 저품질 경고 표시

### 시나리오 3: 정상 답변
**입력**:
```
질문: Spring Boot의 장점은?
답변: "Spring Boot는 설정을 자동화하여 개발 생산성을 높입니다.
내장 톰캣으로 배포가 간편하고, Spring Initializr로 프로젝트 구성이 빠릅니다.
저는 실제로 프로젝트에서 개발 시간을 30% 단축한 경험이 있습니다."
```

**기대 결과**:
- ✅ 사전 검증 통과
- AI 평가:
  - 점수: 3-4점
  - strengths: ["구체적인 장점 나열", "실제 경험 수치 제시"]
  - improvements: ["더 깊이 있는 기술적 설명 추가 가능"]
- 정상 피드백 표시

---

## 성공 기준

### 정량적 지표
- [ ] "여기는 여기는..." 패턴 100% 사전 차단
- [ ] AI Hallucination 발생률 < 5%
- [ ] 저품질 답변의 평균 점수 < 1.5점
- [ ] 정상 답변 False Positive < 10%

### 정성적 지표
- [ ] strengths가 실제 답변 내용과 일치
- [ ] improvements가 구체적이고 실질적
- [ ] 사용자 피드백: "AI 평가가 신뢰할 만하다"

---

## 추가 고려사항

### 향후 개선 아이디어

1. **Few-shot Examples** (옵션 3):
   - 프롬프트에 좋은/나쁜 답변 예시 추가
   - 토큰 비용 증가하지만 품질 향상

2. **응답 후처리 검증** (옵션 4):
   - AI 응답에서 언급된 키워드가 실제 답변에 있는지 체크
   - 구현 복잡도 높음, 우선순위 낮음

3. **사용자 피드백 수집**:
   - "이 피드백이 도움이 되었나요?" 버튼
   - 낮은 평가 받은 피드백 분석 → 프롬프트 개선

4. **A/B 테스트**:
   - 프롬프트 버전 A vs B 비교
   - 어떤 프롬프트가 더 정확한 평가를 하는지 측정

---

## 참고 자료

- **현재 구현**: `AnswerValidator.kt`, `PromptBuilder.kt`
- **관련 문서**: `CLAUDE.md` - Answer Quality Validation 섹션
- **테스트**: `AnswerValidatorTest.kt`

---

**다음 작업 시 체크리스트**:
- [ ] 이 문서 읽기
- [ ] Phase 1: 프롬프트 개선 구현
- [ ] Phase 2: 사전 검증 강화 구현
- [ ] 테스트 시나리오 3개 모두 검증
- [ ] 실제 사용자 테스트
- [ ] 이 문서를 최신 상태로 업데이트
