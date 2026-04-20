package com.hojun.interviewnote.interviewnoteapi.service

import com.hojun.interviewnote.interviewnoteapi.domain.User
import com.hojun.interviewnote.interviewnoteapi.domain.UserRole
import com.hojun.interviewnote.interviewnoteapi.dto.UpdateProfileRequest
import com.hojun.interviewnote.interviewnoteapi.dto.UserProfileDto
import com.hojun.interviewnote.interviewnoteapi.exception.UserNotFoundException
import com.hojun.interviewnote.interviewnoteapi.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * User Service
 *
 * Phase 4A에서 추가됨
 * - 회원가입
 * - 사용자 조회
 * - 비밀번호 검증
 *
 * Phase 5에서 추가됨
 * - 프로필 조회 및 업데이트
 */
@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 회원가입
     *
     * @param email 이메일
     * @param password 평문 비밀번호
     * @param name 사용자 이름
     * @return 생성된 사용자
     * @throws IllegalArgumentException 이메일 중복 시
     */
    @Transactional
    fun register(email: String, password: String, name: String): User {
        logger.info("회원가입 시도: email={}", email)

        // 이메일 중복 체크
        if (userRepository.existsByEmail(email)) {
            logger.warn("회원가입 실패: 이메일 중복 - email={}", email)
            throw IllegalArgumentException("이미 사용 중인 이메일입니다: $email")
        }

        // 비밀번호 유효성 검증
        validatePassword(password)

        // 비밀번호 암호화
        val passwordHash = passwordEncoder.encode(password)

        // 사용자 생성
        val user = User(
            email = email,
            passwordHash = passwordHash,
            name = name,
            role = UserRole.USER
        )

        val savedUser = userRepository.save(user)
        logger.info("회원가입 성공: userId={}, email={}", savedUser.id, savedUser.email)

        return savedUser
    }

    /**
     * 이메일로 사용자 조회
     *
     * @param email 이메일
     * @return 사용자 (없으면 null)
     */
    fun findByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }

    /**
     * 활성화된 사용자만 조회
     *
     * @param email 이메일
     * @return 활성화된 사용자 (없으면 null)
     */
    fun findActiveUserByEmail(email: String): User? {
        return userRepository.findByEmailAndIsActiveTrue(email)
    }

    /**
     * ID로 사용자 조회
     *
     * @param id 사용자 ID
     * @return 사용자 (없으면 null)
     */
    fun findById(id: Long): User? {
        return userRepository.findById(id).orElse(null)
    }

    /**
     * 비밀번호 검증
     *
     * @param user 사용자
     * @param rawPassword 평문 비밀번호
     * @return 비밀번호 일치 여부
     */
    fun verifyPassword(user: User, rawPassword: String): Boolean {
        return passwordEncoder.matches(rawPassword, user.passwordHash)
    }

    /**
     * 마지막 로그인 일시 갱신
     *
     * @param userId 사용자 ID
     */
    @Transactional
    fun updateLastLoginAt(userId: Long) {
        val user = userRepository.findById(userId).orElse(null) ?: return
        user.updateLastLoginAt()
        userRepository.save(user)
        logger.debug("마지막 로그인 일시 갱신: userId={}", userId)
    }

    /**
     * 비밀번호 유효성 검증
     *
     * 규칙:
     * - 최소 8자 이상
     * - 최대 100자 이하
     * - 영문, 숫자, 특수문자 중 2가지 이상 포함
     *
     * @param password 평문 비밀번호
     * @throws IllegalArgumentException 유효하지 않은 비밀번호
     */
    private fun validatePassword(password: String) {
        if (password.length < 8) {
            throw IllegalArgumentException("비밀번호는 최소 8자 이상이어야 합니다")
        }

        if (password.length > 100) {
            throw IllegalArgumentException("비밀번호는 최대 100자 이하여야 합니다")
        }

        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }

        val complexity = listOf(hasLetter, hasDigit, hasSpecial).count { it }
        if (complexity < 2) {
            throw IllegalArgumentException(
                "비밀번호는 영문, 숫자, 특수문자 중 2가지 이상을 포함해야 합니다"
            )
        }
    }

    /**
     * 이메일 유효성 검증
     *
     * @param email 이메일
     * @return 유효한 이메일 형식 여부
     */
    fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
        return email.matches(emailRegex)
    }

    // Phase 5: 프로필 관리

    /**
     * 사용자 프로필 조회
     *
     * @param userId 사용자 ID
     * @return 사용자 프로필 정보
     * @throws UserNotFoundException 사용자가 존재하지 않는 경우
     */
    fun getProfile(userId: Long): UserProfileDto {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("사용자를 찾을 수 없습니다: $userId") }

        return UserProfileDto(
            id = user.id,
            email = user.email,
            name = user.name,
            jobField = user.jobField,
            careerLevel = user.careerLevel,
            createdAt = user.createdAt
        )
    }

    /**
     * 사용자 프로필 업데이트
     *
     * @param userId 사용자 ID
     * @param request 업데이트 요청 정보
     * @return 업데이트된 사용자
     * @throws UserNotFoundException 사용자가 존재하지 않는 경우
     */
    @Transactional
    fun updateProfile(userId: Long, request: UpdateProfileRequest): User {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("사용자를 찾을 수 없습니다: $userId") }

        user.changeName(request.name)
        user.updateJobPreferences(request.jobField, request.careerLevel)

        val updatedUser = userRepository.save(user)
        logger.info(
            "프로필 업데이트: userId={}, name={}, jobField={}, careerLevel={}",
            userId, request.name, request.jobField, request.careerLevel
        )

        return updatedUser
    }
}
