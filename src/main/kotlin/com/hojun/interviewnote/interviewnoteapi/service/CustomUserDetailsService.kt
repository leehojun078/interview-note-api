package com.hojun.interviewnote.interviewnoteapi.service

import com.hojun.interviewnote.interviewnoteapi.repository.UserRepository
import com.hojun.interviewnote.interviewnoteapi.security.CustomUserDetails
import org.slf4j.LoggerFactory
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

/**
 * Spring Security UserDetailsService 구현
 *
 * Phase 4A에서 추가됨
 * - 로그인 시 데이터베이스에서 사용자 조회
 * - User 엔티티를 Spring Security UserDetails로 변환
 */
@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 이메일로 사용자 조회
     *
     * Spring Security가 로그인 시 자동으로 호출
     *
     * @param username 이메일 (Spring Security에서는 username이지만 우리는 email 사용)
     * @return UserDetails
     * @throws UsernameNotFoundException 사용자를 찾을 수 없거나 비활성화된 경우
     */
    override fun loadUserByUsername(username: String): UserDetails {
        logger.debug("loadUserByUsername: username={}", username)

        // 이메일로 활성화된 사용자 조회
        val user = userRepository.findByEmailAndIsActiveTrue(username)
            ?: run {
                logger.warn("로그인 실패: 사용자를 찾을 수 없거나 비활성화됨 - email={}", username)
                throw UsernameNotFoundException("사용자를 찾을 수 없습니다: $username")
            }

        logger.debug("사용자 조회 성공: userId={}, email={}, name={}, role={}", user.id, user.email, user.name, user.role)

        // User 엔티티를 CustomUserDetails로 변환
        return CustomUserDetails(
            email = user.email,
            password = user.passwordHash,
            authorities = listOf(SimpleGrantedAuthority("ROLE_${user.role.name}")),
            name = user.name,
            isAccountNonExpired = true,
            isAccountNonLocked = user.isActive,
            isCredentialsNonExpired = true,
            isEnabled = user.isActive
        )
    }
}
