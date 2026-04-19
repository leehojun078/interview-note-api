#!/bin/bash
# BCrypt 해시 생성 스크립트

cat > /tmp/GeneratePasswordHash.kt << 'EOF'
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

fun main() {
    val encoder = BCryptPasswordEncoder()

    println("password123 해시: ${encoder.encode("password123")}")
    println("admin123 해시: ${encoder.encode("admin123")}")
}
EOF

# 간단한 방법: Python을 사용하여 BCrypt 해시 생성
python3 << 'PYTHON'
import bcrypt

password1 = "password123"
password2 = "admin123"

hash1 = bcrypt.hashpw(password1.encode('utf-8'), bcrypt.gensalt(rounds=10))
hash2 = bcrypt.hashpw(password2.encode('utf-8'), bcrypt.gensalt(rounds=10))

print(f"password123 해시: {hash1.decode('utf-8')}")
print(f"admin123 해시: {hash2.decode('utf-8')}")
PYTHON
