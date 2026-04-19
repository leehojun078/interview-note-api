package com.hojun.interviewnote.interviewnoteapi

import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

/**
 * BCrypt 해시 생성 테스트
 *
 * 이 테스트를 실행하면 콘솔에 BCrypt 해시가 출력됩니다.
 */
class PasswordHashGenerator {

    @Test
    fun generatePasswordHashes() {
        val encoder = BCryptPasswordEncoder()

        val password1 = "password123"
        val password2 = "admin123"

        val hash1 = encoder.encode(password1)
        val hash2 = encoder.encode(password2)

        println("=" * 80)
        println("BCrypt 해시 생성 결과:")
        println("=" * 80)
        println("password123 해시: $hash1")
        println("admin123 해시: $hash2")
        println("=" * 80)

        // 검증
        assert(encoder.matches(password1, hash1))
        assert(encoder.matches(password2, hash2))
        println("✅ 해시 검증 성공!")
    }

    private operator fun String.times(n: Int) = this.repeat(n)
}
