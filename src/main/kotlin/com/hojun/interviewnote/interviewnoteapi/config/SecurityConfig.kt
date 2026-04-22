package com.hojun.interviewnote.interviewnoteapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

/**
 * Spring Security 설정
 *
 * Phase 4A에서 추가됨
 * - 로그인/회원가입 기능
 * - 세션 기반 인증
 * - BCrypt 비밀번호 암호화
 *
 * Phase 5 Step 16에서 추가됨
 * - @PreAuthorize를 위한 메서드 레벨 보안 활성화
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig {

    /**
     * BCrypt 비밀번호 암호화
     *
     * - strength: 10 (기본값, 2^10 = 1024 라운드)
     * - 더 높은 값은 더 안전하지만 느림
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    /**
     * Security Filter Chain 설정
     *
     * Phase 4A 초기 설정:
     * - 로그인/회원가입 페이지는 인증 없이 접근 가능
     * - 나머지 페이지는 모두 인증 필요
     * - 세션 기반 인증 (JWT는 Phase 4A-2에서 추가 고려)
     */
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth
                    // 공개 페이지 (인증 불필요)
                    .requestMatchers("/").permitAll()
                    .requestMatchers("/home").permitAll()
                    .requestMatchers("/error").permitAll()  // Spring Boot 기본 에러 페이지
                    .requestMatchers("/auth/**").permitAll()  // 로그인/회원가입 관련 모든 경로
                    .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                    .requestMatchers("/favicon.ico").permitAll()

                    // Actuator endpoints (Phase 3에서 추가됨)
                    .requestMatchers("/actuator/**").permitAll()

                    // H2 Console (개발 환경에서만)
                    .requestMatchers("/h2-console/**").permitAll()

                    // 나머지 모든 요청은 인증 필요
                    .anyRequest().authenticated()
            }
            .formLogin { form ->
                form
                    .loginPage("/auth/login")  // 커스텀 로그인 페이지
                    .loginProcessingUrl("/auth/login-process")  // 로그인 처리 URL
                    .usernameParameter("email")  // username 대신 email 사용
                    .passwordParameter("password")
                    .defaultSuccessUrl("/questions", true)  // 로그인 성공 시 질문 목록으로
                    .failureUrl("/auth/login?error=true")  // 로그인 실패 시
                    .permitAll()
            }
            .logout { logout ->
                logout
                    .logoutUrl("/auth/logout")
                    .logoutSuccessUrl("/auth/login?logout=true")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll()
            }
            .csrf { csrf ->
                // H2 Console은 CSRF 비활성화 (개발 환경)
                csrf.ignoringRequestMatchers("/h2-console/**")
            }
            // H2 Console에서 frame 사용 허용 (개발 환경)
            .headers { headers ->
                headers.frameOptions { frameOptions ->
                    frameOptions.sameOrigin()
                }
            }

        return http.build()
    }
}
