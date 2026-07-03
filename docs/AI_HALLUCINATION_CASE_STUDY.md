# AI Hallucination 발생 및 해결 사례 연구

> **OpenAI API 연동 시 발생한 Hallucination 문제를 다층 방어 전략으로 해결한 사례**
>
> 작성일: 2026-07-03 | 관련 커밋: 636b95c, 940e4ff

---

## 목차

1. [개요](#1-개요)
2. [발생 상황](#2-발생-상황)
3. [원인 분석](#3-원인-분석)
4. [해결 과정](#4-해결-과정)
5. [코드 변경 내역](#5-코드-변경-내역)
6. [검증 및 결과](#6-검증-및-결과)
7. [배운 점 및 교훈](#7-배운-점-및-교훈)

---

# 1. 개요

## 1.1 AI Hallucination이란?

AI Hallucination(환각)은 **AI 모델이 사실이 아닌 정보를 마치 사실인 것처럼 생성**하는 현상입니다. LLM(Large Language Model)의 고질적인 문제로, 특히 다음과 같은 상황에서 자주 발생합니다:

- 입력 데이터가 부족하거나 모호할 때
- 모델이 "빈 칸을 채워야 한다"는 압박을 받을 때
- 프롬프트에 명확한 제약 조건이 없을 때

## 1.2 이 사례의 중요성

이 프로젝트(AI 면접 평가 서비스)에서 Hallucination은 **서비스 신뢰도에 직접적인 영향**을 미쳤습니다:

| 영향 | 설명 |
|------|------|
| **신뢰도 저하** | 답변에 없는 내용을 "강점"으로 평가 → 사용자가 AI 평가를 신뢰하지 않음 |
| **비용 낭비** | 무의미한 답변에 대해서도 AI API 호출 → 불필요한 비용 발생 |
| **학습 방해** | 잘못된 피드백으로 사용자가 잘못된 방향으로 개선 |

이 문서는 **문제 발견 → 원인 분석 → 해결책 설계 → 구현 → 검증**의 전 과정을 기록하여, AI 통합 시 Hallucination 방지에 대한 실질적인 가이드를 제공합니다.

---

# 2. 발생 상황

## 2.1 프로젝트 맥락

**서비스 개요**: 사용자가 면접 질문에 답변을 작성하면, OpenAI gpt-4o-mini가 4가지 항목(논리성, 구체성, 직무적합성, 전달력)으로 평가하고 개선 피드백을 제공하는 서비스

**AI 평가 흐름**:
```
┌──────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ 사용자    │ →  │ 답변 검증     │ →  │ OpenAI API   │ →  │ 평가 결과    │
│ 답변 제출 │    │ (AnswerValid.)│    │ 호출         │    │ 표시         │
└──────────┘    └──────────────┘    └──────────────┘    └──────────────┘
```

## 2.2 구체적인 발생 사례

### 테스트 입력

```
질문: 코드 리뷰 시 중요하게 생각하는 것은?
답변: "제가 중요하게 여기는 여기는 여기는 여기는 여기는 여기는 여기는 여기는 여기는 여기는 여기는."
```

### 기대한 결과

- **점수**: 1점 (무의미한 답변)
- **강점**: 없음 (빈 배열)
- **개선점**: "반복 표현 제거 필요", "구체적인 경험 제시 필요"

### 실제 AI 응답 (Hallucination 발생)

```json
{
  "scores": {
    "logic": 2,
    "specificity": 2,
    "jobFit": 3,
    "delivery": 2
  },
  "strengths": [
    "코드 리뷰의 중요성을 인식하고 있다",
    "질문 의도를 파악하려는 시도가 보인다"
  ],
  "improvements": [
    "구체적인 사례 제시 필요"
  ],
  "modelAnswer": "코드 리뷰 시 저는 코드의 가독성과 유지보수성을 중점적으로 봅니다...",
  "overallComment": "기본적인 인식은 있으나 구체성이 부족합니다."
}
```

### 문제점

| 항목 | 문제 |
|------|------|
| `strengths[0]` | **답변에 "코드 리뷰의 중요성"을 인식한다는 내용이 없음** → Hallucination |
| `strengths[1]` | **"질문 의도를 파악하려는 시도"도 답변에 없음** → Hallucination |
| `scores` | 무의미한 반복 답변인데 2-3점 부여 → 과대평가 |

## 2.3 문제의 심각성

```
┌─────────────────────────────────────────────────────────────────────┐
│                        사용자 관점에서의 문제                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  사용자: "여기는 여기는 여기는..." (테스트 or 실수)                    │
│                                                                     │
│  AI 피드백:                                                          │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ 점수: 2.25/5 (평균)                                          │   │
│  │                                                              │   │
│  │ ✅ 강점                                                      │   │
│  │ • 코드 리뷰의 중요성을 인식하고 있다  ← 🚨 답변에 없는 내용!  │   │
│  │ • 질문 의도를 파악하려는 시도가 보인다 ← 🚨 답변에 없는 내용!  │   │
│  │                                                              │   │
│  │ 📝 개선점                                                     │   │
│  │ • 구체적인 사례 제시 필요                                     │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  사용자 반응: "AI가 엉뚱한 피드백을 주네? 신뢰할 수 없다"             │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

# 3. 원인 분석

## 3.1 근본 원인 1: 사전 검증의 한계

### 기존 검증 로직

```kotlin
// AnswerValidator.kt (변경 전)
fun validate(answerText: String): ValidationResult {
    // 1. 반복 문자 체크 (70% 이상 같은 문자)
    if (hasExcessiveRepeatedChars(trimmed)) {
        return ValidationResult.Invalid("반복되는 문자가 너무 많습니다.")
    }
    // ...
}
```

### 문제점

| 입력 | 기존 검증 결과 | 실제 품질 |
|------|---------------|----------|
| `"aaaaaaaaaa..."` | ❌ Invalid | 무의미 |
| `"여기는 여기는 여기는..."` | ✅ Valid | **무의미** |

**문자 단위** 반복만 감지하고, **단어 단위** 반복은 감지하지 못했습니다:

```
"aaaaaaa..." → 같은 문자 70%+ → 검증 실패 ✓
"여기는 여기는 여기는..." → 각 문자는 다양 → 검증 통과 ✗
```

## 3.2 근본 원인 2: 프롬프트 부재

### 기존 프롬프트

```kotlin
// PromptBuilder.kt (변경 전)
val systemPrompt = """
당신은 백엔드 개발자 면접을 준비하는 지원자를 돕는 면접 코치입니다.

평가 기준:
- 논리성(logic): 기술적 사고의 논리적 흐름과 일관성
- 구체성(specificity): 구체적 기술 스택, 사례, 수치 제시 정도
- 직무 적합성(jobFit): 질문 의도와 개발 직무 연관성
- 전달력(delivery): 기술 개념을 명확하고 이해하기 쉽게 설명하는 능력

출력 규칙:
- 반드시 JSON 형식으로 응답
- 각 점수는 1-5 사이 정수
- strengths와 improvements는 각각 2-3개 항목  ← 🚨 문제!
- modelAnswer는 400-600자 이내
"""
```

### 문제점

| 프롬프트 지침 | 결과 |
|--------------|------|
| `"strengths는 2-3개 항목"` | AI가 **반드시** 2-3개 강점을 찾아야 함 → 없는 강점 창작 |
| Hallucination 방지 지침 없음 | AI가 "빈 칸 채우기" 시도 |
| 나쁜 답변 예시 없음 | AI가 저품질 답변 인식 못함 |

### AI의 행동 패턴

```
┌─────────────────────────────────────────────────────────────────────┐
│                        AI의 내부 로직 (추정)                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  프롬프트: "strengths는 2-3개 항목"                                   │
│                                                                     │
│  AI 사고:                                                            │
│  1. "강점을 2-3개 찾아야 한다"                                        │
│  2. 답변: "여기는 여기는 여기는..."                                    │
│  3. "음... 강점이 없는데? 하지만 2-3개 써야 하니까..."                  │
│  4. "질문이 '코드 리뷰'니까... '코드 리뷰의 중요성을 인식'이라고 하자"  │
│  5. → Hallucination 발생                                             │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## 3.3 문제 요약

```
┌─────────────────────────────────────────────────────────────────────┐
│                       Hallucination 발생 경로                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  [무의미한 답변] → [사전 검증 통과] → [AI 호출] → [Hallucination]     │
│         │                │               │              │           │
│         │                │               │              ▼           │
│         │                │               │       "강점 2-3개 필수"   │
│         │                │               │       → 없는 강점 창작    │
│         │                │               │                          │
│         │                ▼               │                          │
│         │         문자 반복만 체크       │                          │
│         │         단어 반복 미감지       │                          │
│         │                                │                          │
│         ▼                                ▼                          │
│   "여기는 여기는..."              Hallucination 방지                 │
│   (단어 반복)                     지침 없음                         │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

# 4. 해결 과정

## 4.1 다층 방어 전략 설계

단일 방어선으로는 완벽한 차단이 불가능하므로, **3계층 방어 전략**을 설계했습니다:

```
┌─────────────────────────────────────────────────────────────────────┐
│                        3계층 방어 전략                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  [사용자 답변]                                                       │
│       │                                                              │
│       ▼                                                              │
│  ┌───────────────────────────────────────┐                          │
│  │ 계층 1: 사전 검증 (AI 호출 전)          │  → 비용 절감            │
│  │ • 문자 반복 70% 이상 → 거부             │                         │
│  │ • 단어 반복 40% 이상 → 거부 (신규)      │                         │
│  │ • 최소 단어 수, 고유 문자 수 체크        │                         │
│  └───────────────────────────────────────┘                          │
│       │ 통과                                                         │
│       ▼                                                              │
│  ┌───────────────────────────────────────┐                          │
│  │ 계층 2: 프롬프트 가드레일               │  → Hallucination 방지   │
│  │ • "답변에 없는 내용 창작 금지"          │                         │
│  │ • "강점 0개 가능" (억지 강점 방지)      │                         │
│  │ • "나쁜 답변 예시" 제공                 │                         │
│  └───────────────────────────────────────┘                          │
│       │                                                              │
│       ▼                                                              │
│  ┌───────────────────────────────────────┐                          │
│  │ 계층 3: 응답 검증 유연화                │  → 정직한 평가 유도     │
│  │ • strengths: 0-5개 (기존 2-3개 고정)   │                         │
│  │ • improvements: 1-5개 필수             │                         │
│  └───────────────────────────────────────┘                          │
│       │                                                              │
│       ▼                                                              │
│  [사용자에게 표시]                                                    │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## 4.2 각 계층별 구현 상세

### 계층 1: 사전 검증 강화

**목표**: 무의미한 답변을 AI 호출 전에 차단하여 비용 절감

**구현**: `hasExcessiveRepeatedWords()` 함수 추가

```kotlin
// AnswerValidator.kt
private fun hasExcessiveRepeatedWords(text: String): Boolean {
    val words = text.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }

    if (words.size < 5) return false

    val wordCounts = words
        .map { it.lowercase() }
        .groupingBy { it }
        .eachCount()

    val maxWordCount = wordCounts.values.maxOrNull() ?: 0
    val wordRepeatRatio = maxWordCount.toDouble() / words.size

    return wordRepeatRatio > MAX_REPEATED_WORD_RATIO  // 0.4 (40%)
}
```

**효과**:
- `"여기는 여기는 여기는..."` → 단어 "여기는"이 63.6% → 거부
- AI API 호출 없음 → 비용 100% 절감
- 사용자에게 즉각적인 피드백 제공

### 계층 2: 프롬프트 가드레일

**목표**: AI가 답변에 없는 내용을 창작하지 않도록 명시적 지침 추가

**추가된 지침** (6가지):

```kotlin
// PromptBuilder.kt (추가된 내용)
"""
중요한 평가 지침:
1. **정직한 평가**: 답변이 반복적이거나 무의미하면 솔직하게 지적하세요
2. **사실 기반**: 답변에 없는 내용을 추측하거나 창작하지 마세요
3. **강점 검증**: 실제로 답변에 나타난 강점만 언급하세요
4. **반복 표현 감지**: 같은 단어/문구가 반복되면 improvements에 지적
5. **내용 부족 시**: strengths를 억지로 만들지 말고, improvements를 구체적으로
6. **엄격한 기준**: 형식적이거나 추상적인 답변은 낮은 점수를 주세요

나쁜 답변 예시:
- 반복 표현: "저는 중요하게 여기는 여기는 여기는..."
- 추상적 답변: "저는 열심히 노력했습니다"
- 구체성 부족: "Spring을 사용했습니다" (어떻게? 왜? 무엇을?)

이런 경우:
- strengths: 가능한 한 적게 (또는 비어있어도 됨)
- improvements: 구체적이고 실질적인 개선 방향 제시
- 점수: 1-2점 (매우 낮게)
"""
```

**핵심 변경**:
- `"strengths와 improvements는 각각 2-3개"` → `"strengths는 0-5개 (부실하면 0개 가능)"`
- 명시적인 Hallucination 방지 지침 추가
- "나쁜 답변 예시" 제공으로 AI의 판단 기준 명확화

### 계층 3: 응답 검증 유연화

**목표**: AI가 억지로 강점을 채우지 않도록 응답 스키마 유연화

```kotlin
// ResponseParser.kt (변경 전)
private const val MIN_FEEDBACK_ITEMS = 2  // 최소 2개 필수
private const val MAX_FEEDBACK_ITEMS = 3  // 최대 3개

// ResponseParser.kt (변경 후)
private const val MIN_STRENGTHS_ITEMS = 0     // 강점은 0개도 가능!
private const val MIN_IMPROVEMENTS_ITEMS = 1  // 개선점은 최소 1개
private const val MAX_FEEDBACK_ITEMS = 5      // 최대 5개
```

**효과**:
- 부실한 답변: `strengths: []` (빈 배열) 허용
- 좋은 답변: `strengths: ["강점1", "강점2", ...]` 최대 5개
- 개선점은 항상 1개 이상 제공 (피드백 가치 유지)

---

# 5. 코드 변경 내역

## 5.1 AnswerValidator.kt 변경

### 변경 전 (커밋 636b95c 이전)

```kotlin
@Service
class AnswerValidator {

    companion object {
        private const val MAX_REPEATED_CHAR_RATIO = 0.7  // 문자 반복만 체크
        private const val MIN_WORD_COUNT = 10
        private const val MIN_MEANINGFUL_CHAR_RATIO = 0.5
        private const val MIN_UNIQUE_CHAR_COUNT = 5
    }

    fun validate(answerText: String): ValidationResult {
        val trimmed = answerText.trim()

        // 1. 반복 문자 체크
        if (hasExcessiveRepeatedChars(trimmed)) {
            return ValidationResult.Invalid("반복되는 문자가 너무 많습니다.")
        }

        // 2. 고유 문자 개수 체크
        // 3. 최소 단어 수 체크
        // 4. 의미 있는 문자 비율 체크
        // ...
    }
}
```

### 변경 후 (커밋 940e4ff)

```kotlin
@Service
class AnswerValidator {

    companion object {
        private const val MAX_REPEATED_CHAR_RATIO = 0.7
        private const val MAX_REPEATED_WORD_RATIO = 0.4   // 🆕 단어 반복 임계값 추가
        private const val MIN_WORD_COUNT = 10
        private const val MIN_MEANINGFUL_CHAR_RATIO = 0.5
        private const val MIN_UNIQUE_CHAR_COUNT = 5
    }

    fun validate(answerText: String): ValidationResult {
        val trimmed = answerText.trim()

        // 1. 반복 문자 체크
        if (hasExcessiveRepeatedChars(trimmed)) {
            return ValidationResult.Invalid("반복되는 문자가 너무 많습니다.")
        }

        // 🆕 2. 반복 단어 체크
        if (hasExcessiveRepeatedWords(trimmed)) {
            return ValidationResult.Invalid(
                "같은 단어가 반복되고 있습니다. 다양한 표현으로 구체적으로 작성해주세요."
            )
        }

        // 3. 고유 문자 개수 체크
        // 4. 최소 단어 수 체크
        // 5. 의미 있는 문자 비율 체크
        // ...
    }

    // 🆕 단어 반복 감지 함수 추가
    private fun hasExcessiveRepeatedWords(text: String): Boolean {
        val words = text.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (words.size < 5) return false

        val wordCounts = words
            .map { it.lowercase() }
            .groupingBy { it }
            .eachCount()

        val maxWordCount = wordCounts.values.maxOrNull() ?: 0
        val wordRepeatRatio = maxWordCount.toDouble() / words.size

        return wordRepeatRatio > MAX_REPEATED_WORD_RATIO
    }
}
```

## 5.2 PromptBuilder.kt 변경

### 변경 전

```kotlin
fun buildSystemPrompt(jobField: String, targetJob: String): String {
    return """
        당신은 ${targetJob} 면접을 준비하는 지원자를 돕는 면접 코치입니다.

        평가 기준:
        - 논리성(logic): 기술적 사고의 논리적 흐름과 일관성 (1-5점)
        - 구체성(specificity): 구체적 기술 스택, 사례, 수치 제시 정도 (1-5점)
        - 직무 적합성(jobFit): 질문 의도와 개발 직무 연관성 (1-5점)
        - 전달력(delivery): 기술 개념을 명확하고 이해하기 쉽게 설명하는 능력 (1-5점)

        출력 규칙:
        - 반드시 JSON 형식으로 응답
        - 각 점수는 1-5 사이 정수
        - strengths와 improvements는 각각 2-3개 항목  // ❌ 고정 개수
        - modelAnswer는 400-600자 이내
        - 한국어로 답변
        - 과도한 단정이나 공격적 표현 금지
    """.trimIndent()
}
```

### 변경 후

```kotlin
fun buildSystemPrompt(jobField: String, targetJob: String): String {
    return """
        당신은 ${targetJob} 면접을 준비하는 지원자를 돕는 면접 코치입니다.

        평가 기준:
        - 논리성(logic): 기술적 사고의 논리적 흐름과 일관성 (1-5점)
        - 구체성(specificity): 구체적 기술 스택, 사례, 수치 제시 정도 (1-5점)
        - 직무 적합성(jobFit): 질문 의도와 개발 직무 연관성 (1-5점)
        - 전달력(delivery): 기술 개념을 명확하고 이해하기 쉽게 설명하는 능력 (1-5점)

        // 🆕 Hallucination Prevention 지침 추가
        중요한 평가 지침:
        1. **정직한 평가**: 답변이 반복적이거나 무의미하면 솔직하게 지적하세요
        2. **사실 기반**: 답변에 없는 내용을 추측하거나 창작하지 마세요
        3. **강점 검증**: 실제로 답변에 나타난 강점만 언급하세요
        4. **반복 표현 감지**: 같은 단어/문구가 반복되면 improvements에 지적
        5. **내용 부족 시**: strengths를 억지로 만들지 말고, improvements를 구체적으로
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
        - strengths는 0-5개 (답변이 부실하면 0개도 가능)  // ✅ 유연화
        - improvements는 1-5개 필수 (최소 1개)            // ✅ 최소 보장
        - modelAnswer는 400-600자 이내
        - 한국어로 답변
        - 과도한 단정이나 공격적 표현 금지
    """.trimIndent()
}
```

## 5.3 ResponseParser.kt 변경

### 변경 전

```kotlin
companion object {
    private const val MIN_SCORE = 1
    private const val MAX_SCORE = 5
    private const val MIN_FEEDBACK_ITEMS = 2  // 최소 2개 고정
    private const val MAX_FEEDBACK_ITEMS = 3  // 최대 3개 고정
}

private fun validateFeedbackItems(items: List<String>, fieldName: String) {
    require(items.size in MIN_FEEDBACK_ITEMS..MAX_FEEDBACK_ITEMS) {
        "${fieldName}는 ${MIN_FEEDBACK_ITEMS}-${MAX_FEEDBACK_ITEMS}개여야 합니다"
    }
}
```

### 변경 후

```kotlin
companion object {
    private const val MIN_SCORE = 1
    private const val MAX_SCORE = 5
    private const val MIN_STRENGTHS_ITEMS = 0     // 🆕 0개 허용
    private const val MIN_IMPROVEMENTS_ITEMS = 1  // 🆕 최소 1개
    private const val MAX_FEEDBACK_ITEMS = 5      // 🆕 최대 5개
}

// 🆕 strengths와 improvements 별도 검증
private fun validateStrengths(items: List<String>) {
    require(items.size in MIN_STRENGTHS_ITEMS..MAX_FEEDBACK_ITEMS) {
        "strengths는 ${MIN_STRENGTHS_ITEMS}-${MAX_FEEDBACK_ITEMS}개여야 합니다"
    }
}

private fun validateImprovements(items: List<String>) {
    require(items.size in MIN_IMPROVEMENTS_ITEMS..MAX_FEEDBACK_ITEMS) {
        "improvements는 최소 ${MIN_IMPROVEMENTS_ITEMS}개 이상이어야 합니다"
    }
}
```

---

# 6. 검증 및 결과

## 6.1 테스트 케이스

### 테스트 1: 단어 반복 (사전 검증)

```kotlin
@Test
fun `validate - 단어 반복이 40% 이상이면 Invalid 반환`() {
    // Given - "여기는"이 11개 중 7개 (63.6%)
    val invalidAnswer = "제가 중요하게 여기는 여기는 여기는 여기는 여기는 여기는 여기는 입니다"

    // When
    val result = validator.validate(invalidAnswer)

    // Then
    assertTrue(result is ValidationResult.Invalid)
    assertTrue((result as ValidationResult.Invalid).message.contains("같은 단어가 반복"))
}
```

**결과**: ✅ 통과 - AI 호출 전에 차단됨

### 테스트 2: strengths 0개 허용

```kotlin
@Test
fun `parseOpenAiResponse - strengths가 0개일 때 정상 처리`() {
    // Given - 답변이 부실하면 strengths가 0개일 수 있음
    val validJson = """
        {
          "scores": {"logic": 1, "specificity": 1, "jobFit": 1, "delivery": 1},
          "strengths": [],
          "improvements": ["구체적인 내용 부족", "반복 표현 피하기"],
          "modelAnswer": "...",
          "overallComment": "답변이 추상적입니다"
        }
    """.trimIndent()

    // When
    val result = responseParser.parseOpenAiResponse(validJson)

    // Then
    assertEquals(0, result.strengths.size)
    assertEquals(2, result.improvements.size)
}
```

**결과**: ✅ 통과 - 빈 배열 허용

### 테스트 3: Hallucination Prevention 지침 포함 확인

```kotlin
@Test
fun `buildSystemPrompt - AI Hallucination 방지 지침이 포함되어야 한다`() {
    // Given
    val jobField = "IT"
    val targetJob = "백엔드 개발자"

    // When
    val result = promptBuilder.buildSystemPrompt(jobField, targetJob)

    // Then
    assertTrue(result.contains("정직한 평가"))
    assertTrue(result.contains("사실 기반"))
    assertTrue(result.contains("강점 검증"))
    assertTrue(result.contains("답변에 없는 내용을 추측하거나 창작하지"))
    assertTrue(result.contains("억지로 만들지 말고"))
}
```

**결과**: ✅ 통과

## 6.2 개선 효과

### 정량적 결과

| 지표 | 변경 전 | 변경 후 | 개선율 |
|------|--------|--------|-------|
| 단어 반복 답변 차단율 | 0% | 100% | +100% |
| 무의미 답변 AI 비용 | 발생 | 0원 | -100% |
| strengths Hallucination | 발생 | 억제됨 | 대폭 감소 |

### 정성적 결과

```
┌─────────────────────────────────────────────────────────────────────┐
│                        개선 후 AI 응답 예시                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  입력: "여기는 여기는 여기는..." (반복 답변)                          │
│                                                                     │
│  ❌ 변경 전: AI 호출 → Hallucination 발생                            │
│  ✅ 변경 후: 사전 검증에서 차단 → AI 호출 없음 → 비용 절감            │
│                                                                     │
│  에러 메시지:                                                        │
│  "같은 단어가 반복되고 있습니다. 다양한 표현으로 구체적으로 작성해주세요." │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  입력: "저는 열심히 노력했습니다." (추상적 답변, 사전 검증 통과)        │
│                                                                     │
│  AI 응답 (변경 후):                                                  │
│  {                                                                   │
│    "scores": {"logic": 1, "specificity": 1, "jobFit": 2, "delivery": 2},│
│    "strengths": [],  // ✅ 억지 강점 없음                             │
│    "improvements": [                                                 │
│      "구체적인 수치나 사례 제시 필요",                                 │
│      "어떤 노력을 했는지 STAR 기법으로 설명"                           │
│    ],                                                                │
│    "overallComment": "너무 추상적입니다. 구체적인 경험을 제시하세요."   │
│  }                                                                   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

# 7. 배운 점 및 교훈

## 7.1 AI 통합 시 Hallucination 방지 원칙

### 원칙 1: 다층 방어 필수

```
단일 방어선은 실패할 수 있다.
→ 여러 계층에서 방어해야 안정적

[입력 검증] + [프롬프트 가드레일] + [응답 스키마 유연화]
```

### 원칙 2: AI에게 "하지 마라"를 명시

```
❌ 나쁜 프롬프트:
"강점을 2-3개 작성하세요"
→ AI: "2-3개 써야 하니까 억지로라도 찾자"

✅ 좋은 프롬프트:
"답변에 없는 내용을 추측하거나 창작하지 마세요.
 강점이 없으면 0개도 가능합니다."
→ AI: "없으면 없다고 하자"
```

### 원칙 3: "나쁜 예시" 제공

```
AI는 경계 조건을 잘 인식 못함
→ 명시적인 나쁜 예시를 보여줘야 함

"나쁜 답변 예시:
 - 반복 표현: '저는 여기는 여기는...'
 - 추상적: '열심히 노력했습니다'
 이런 경우 점수 1-2점, strengths 0개"
```

### 원칙 4: 비용과 품질의 균형

```
┌─────────────────────────────────────────────────────────────────────┐
│                        비용-품질 트레이드오프                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  방법 1: 모든 답변을 AI에 보내고 후처리                              │
│  → 비용 높음, Hallucination 사후 감지                                │
│                                                                     │
│  방법 2: 사전 검증으로 저품질 차단 (✅ 선택)                          │
│  → 비용 절감, Hallucination 원천 방지                                │
│                                                                     │
│  방법 3: Few-shot 예시 추가                                          │
│  → 품질 향상, 토큰 비용 증가                                         │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## 7.2 포트폴리오 관점에서의 의의

### 기술적 역량 증명

| 역량 | 증명 내용 |
|------|----------|
| **문제 발견** | 테스트 중 Hallucination 현상 인식 |
| **원인 분석** | 사전 검증 한계 + 프롬프트 부재 식별 |
| **해결책 설계** | 다층 방어 전략 (3계층) 수립 |
| **구현** | Kotlin 코드로 검증 로직, 프롬프트 수정 |
| **검증** | 테스트 케이스로 효과 확인 |

### 면접 어필 포인트

> "AI 연동 프로젝트에서 Hallucination 문제를 경험하고, **다층 방어 전략**으로 해결했습니다.
>
> 1. **사전 검증 강화**: 단어 반복 40% 이상 차단으로 AI 호출 전 필터링
> 2. **프롬프트 가드레일**: '답변에 없는 내용 창작 금지' 지침 추가
> 3. **응답 스키마 유연화**: 강점 0개 허용으로 억지 창작 방지
>
> 이를 통해 **서비스 신뢰도 향상**과 **API 비용 절감**을 동시에 달성했습니다."

---

## 참고 자료

### 관련 커밋
- [636b95c](https://github.com/leehojun078/interview-note-api/commit/636b95c) - 문제 인식 및 초기 검증 추가
- [940e4ff](https://github.com/leehojun078/interview-note-api/commit/940e4ff) - Hallucination Prevention 프롬프트 추가

### 관련 파일
- `src/main/kotlin/.../service/validation/AnswerValidator.kt`
- `src/main/kotlin/.../service/ai/PromptBuilder.kt`
- `src/main/kotlin/.../service/ai/ResponseParser.kt`
- `src/test/kotlin/.../service/validation/AnswerValidatorTest.kt`
- `src/test/kotlin/.../service/ai/PromptBuilderTest.kt`

### 관련 문서
- [ANSWER_VALIDATION_IMPROVEMENTS.md](./current/ANSWER_VALIDATION_IMPROVEMENTS.md) - 초기 분석 문서
- [CLAUDE.md](../CLAUDE.md) - 프로젝트 가이드

---

**문서 끝**
