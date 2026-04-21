package com.hojun.interviewnote.interviewnoteapi.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

/**
 * 커스텀 UserDetails 구현
 *
 * Phase 5에서 추가됨
 * - Spring Security의 기본 UserDetails에 사용자 이름(name) 추가
 * - 네비게이션바에 이메일 대신 이름 표시용
 */
class CustomUserDetails(
    private val email: String,
    private val password: String,
    private val authorities: Collection<GrantedAuthority>,
    /**
     * 사용자 이름
     * - 네비게이션바 등에서 표시용
     * - Kotlin val이 자동으로 getName() 메서드 생성
     */
    val name: String,
    private val isAccountNonExpired: Boolean = true,
    private val isAccountNonLocked: Boolean = true,
    private val isCredentialsNonExpired: Boolean = true,
    private val isEnabled: Boolean = true
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> = authorities

    override fun getPassword(): String = password

    override fun getUsername(): String = email

    override fun isAccountNonExpired(): Boolean = isAccountNonExpired

    override fun isAccountNonLocked(): Boolean = isAccountNonLocked

    override fun isCredentialsNonExpired(): Boolean = isCredentialsNonExpired

    override fun isEnabled(): Boolean = isEnabled
}
