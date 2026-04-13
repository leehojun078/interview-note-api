package com.hojun.interviewnote.interviewnoteapi.service.validation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * AnswerValidator 단위 테스트
 */
class AnswerValidatorTest {

    private lateinit var validator: AnswerValidator

    @BeforeEach
    fun setUp() {
        validator = AnswerValidator()
    }

    @Test
    fun `validate - 정상적인 답변은 Valid 반환`() {
        // Given
        val validAnswer = """
            저는 Spring Boot를 사용하여 RESTful API를 개발한 경험이 있습니다.
            프로젝트에서 JPA를 활용하여 데이터베이스를 설계하고,
            비즈니스 로직을 서비스 계층에서 처리했습니다.
            또한 예외 처리와 로깅을 통해 안정성을 높였습니다.
        """.trimIndent()

        // When
        val result = validator.validate(validAnswer)

        // Then
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validate - 반복 문자 70% 이상이면 Invalid 반환`() {
        // Given
        val invalidAnswer = "a".repeat(100)

        // When
        val result = validator.validate(invalidAnswer)

        // Then
        assertTrue(result is ValidationResult.Invalid)
        if (result is ValidationResult.Invalid) {
            assertTrue(result.message.contains("반복되는 문자"))
        }
    }

    @Test
    fun `validate - 고유 문자가 5개 미만이면 Invalid 반환`() {
        // Given
        val invalidAnswer = "aaabbbcccddd"  // 4개의 고유 문자

        // When
        val result = validator.validate(invalidAnswer)

        // Then
        assertTrue(result is ValidationResult.Invalid)
        if (result is ValidationResult.Invalid) {
            assertTrue(result.message.contains("단순한 답변"))
        }
    }

    @Test
    fun `validate - 단어가 10개 미만이면 Invalid 반환`() {
        // Given
        val invalidAnswer = "한 두 세 네 다섯 여섯 일곱 여덟 아홉"  // 9개 단어

        // When
        val result = validator.validate(invalidAnswer)

        // Then
        assertTrue(result is ValidationResult.Invalid)
        if (result is ValidationResult.Invalid) {
            assertTrue(result.message.contains("최소 10개"))
        }
    }

    @Test
    fun `validate - 의미 있는 문자가 50% 미만이면 Invalid 반환`() {
        // Given
        val invalidAnswer = "1234567890!@#$%^&*()[]{}#@$%^&*()[]{}#@$%^&*()[]{}1234567890"  // 숫자와 특수문자만

        // When
        val result = validator.validate(invalidAnswer)

        // Then
        assertTrue(result is ValidationResult.Invalid)
        if (result is ValidationResult.Invalid) {
            assertTrue(result.message.contains("의미 있는") || result.message.contains("단어"))
        }
    }

    @Test
    fun `validate - 공백만으로 이루어진 답변은 Invalid 반환`() {
        // Given
        val invalidAnswer = "                    "

        // When
        val result = validator.validate(invalidAnswer)

        // Then
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `validate - 반복 패턴이지만 공백이 포함된 경우 체크`() {
        // Given
        val invalidAnswer = "aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa"

        // When
        val result = validator.validate(invalidAnswer)

        // Then
        assertTrue(result is ValidationResult.Invalid)
        if (result is ValidationResult.Invalid) {
            assertTrue(result.message.contains("반복되는 문자"))
        }
    }

    @Test
    fun `validate - 최소 요구사항을 만족하는 경계값 테스트`() {
        // Given - 10개 이상 단어, 충분한 고유 문자, 의미 있는 내용
        val borderlineAnswer = "저는 Spring Boot를 활용하여 백엔드 API 서버를 개발한 경험이 있습니다"

        // When
        val result = validator.validate(borderlineAnswer)

        // Then
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validate - 한글과 영어가 혼합된 답변은 Valid`() {
        // Given
        val mixedAnswer = """
            I have experience with Spring Boot and Kotlin.
            저는 백엔드 개발자로서 다양한 프로젝트를 진행했습니다.
            Database design and API development are my strengths.
        """.trimIndent()

        // When
        val result = validator.validate(mixedAnswer)

        // Then
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validate - 숫자가 포함되어도 문장이 의미있으면 Valid`() {
        // Given
        val answerWithNumbers = """
            저는 3년간 백엔드 개발을 했습니다.
            10개 이상의 프로젝트에서 Spring Boot를 사용했고,
            평균 응답 시간을 200ms 이하로 개선한 경험이 있습니다.
            JVM 메모리를 512MB로 설정하여 안정성을 확보했습니다.
        """.trimIndent()

        // When
        val result = validator.validate(answerWithNumbers)

        // Then
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validate - 특수문자가 적절히 사용되면 Valid`() {
        // Given
        val answerWithPunctuation = """
            저는 Spring Boot를 활용한 백엔드 개발 경험이 있습니다.
            구체적으로는: RESTful API 설계, JPA 활용, 예외 처리 등입니다.
            또한 로깅(SLF4J)과 테스트(JUnit)도 작성했습니다.
            이를 통해 안정적인 서비스를 구축할 수 있었습니다.
        """.trimIndent()

        // When
        val result = validator.validate(answerWithPunctuation)

        // Then
        assertTrue(result is ValidationResult.Valid)
    }
}
