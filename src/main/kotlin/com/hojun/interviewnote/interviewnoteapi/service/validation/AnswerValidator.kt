package com.hojun.interviewnote.interviewnoteapi.service.validation

import org.springframework.stereotype.Service

/**
 * 답변 품질 검증 결과
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}

/**
 * 답변 품질 사전 검증
 *
 * AI API 호출 전에 기본적인 답변 품질을 검증하여 무의미한 답변을 걸러냅니다.
 * 이를 통해 API 비용을 절감하고 사용자에게 즉각적인 피드백을 제공합니다.
 */
@Service
class AnswerValidator {

    companion object {
        private const val MAX_REPEATED_CHAR_RATIO = 0.7  // 70% 이상 같은 문자
        private const val MIN_WORD_COUNT = 10            // 최소 10개 단어
        private const val MIN_MEANINGFUL_CHAR_RATIO = 0.5 // 50% 이상 의미 있는 문자
        private const val MIN_UNIQUE_CHAR_COUNT = 5      // 최소 5개의 서로 다른 문자
    }

    /**
     * 답변 품질 검증
     *
     * @param answerText 검증할 답변 텍스트
     * @return ValidationResult.Valid 또는 ValidationResult.Invalid(message)
     */
    fun validate(answerText: String): ValidationResult {
        val trimmed = answerText.trim()

        // 1. 반복 문자 체크
        if (hasExcessiveRepeatedChars(trimmed)) {
            return ValidationResult.Invalid(
                "반복되는 문자가 너무 많습니다. 의미 있는 답변을 작성해주세요."
            )
        }

        // 2. 고유 문자 개수 체크
        val uniqueChars = trimmed.filter { it.isLetterOrDigit() }.toSet().size
        if (uniqueChars < MIN_UNIQUE_CHAR_COUNT) {
            return ValidationResult.Invalid(
                "너무 단순한 답변입니다. 다양한 표현으로 작성해주세요."
            )
        }

        // 3. 최소 단어 수 체크
        val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size < MIN_WORD_COUNT) {
            return ValidationResult.Invalid(
                "답변이 너무 짧습니다. 최소 ${MIN_WORD_COUNT}개 이상의 단어로 작성해주세요."
            )
        }

        // 4. 의미 있는 문자 비율 체크
        if (!hasSufficientMeaningfulChars(trimmed)) {
            return ValidationResult.Invalid(
                "의미 있는 문장으로 작성해주세요. 숫자나 특수문자만으로는 답변할 수 없습니다."
            )
        }

        return ValidationResult.Valid
    }

    /**
     * 반복 문자 비율 체크
     *
     * 예: "aaaaaaa..."처럼 같은 문자가 70% 이상이면 true
     */
    private fun hasExcessiveRepeatedChars(text: String): Boolean {
        if (text.length < 10) return false

        // 공백 제거 후 체크
        val withoutSpaces = text.replace(Regex("\\s"), "")
        if (withoutSpaces.isEmpty()) return true

        // 각 문자의 출현 횟수 계산
        val charCounts = withoutSpaces.groupingBy { it.lowercaseChar() }.eachCount()
        val maxCount = charCounts.values.maxOrNull() ?: 0

        return maxCount > withoutSpaces.length * MAX_REPEATED_CHAR_RATIO
    }

    /**
     * 의미 있는 문자 비율 체크
     *
     * 한글, 영어, 기본 문장 부호가 50% 이상이어야 함
     */
    private fun hasSufficientMeaningfulChars(text: String): Boolean {
        val meaningfulChars = text.count { char ->
            char.isLetter() ||                           // 한글, 영어 등
            char.isWhitespace() ||                       // 공백
            char in listOf('.', ',', '!', '?', ':', ';', '-', '(', ')')  // 기본 문장 부호
        }

        return meaningfulChars >= text.length * MIN_MEANINGFUL_CHAR_RATIO
    }
}
