a# Phase 4 구현 계획서

면접 리뷰 웹 애플리케이션 - 사용자 관리 및 프로덕션 운영

## 목차
1. [개요](#1-개요)
2. [현재 상태 (Phase 3 완료)](#2-현재-상태-phase-3-완료)
3. [Phase 4 목표](#3-phase-4-목표)
4. [상세 구현 계획](#4-상세-구현-계획)
5. [구현 순서](#5-구현-순서)
6. [검증 계획](#6-검증-계획)
7. [향후 확장 (Phase 5 이후)](#7-향후-확장-phase-5-이후)

---

## 1. 개요

### 1.1 목표

Phase 3에서 구축한 프로덕션 준비 환경을 바탕으로, **실제 서비스 운영**에 필요한 핵심 기능을 구현합니다.

**핵심 가치**:
- ✅ **사용자 관리**: 회원가입, 로그인, 개인별 데이터 분리
- ✅ **자동화**: CI/CD 파이프라인으로 배포 자동화
- ✅ **가시성**: 모니터링 대시보드로 운영 가시성 확보
- ✅ **성능**: Redis 캐싱으로 응답 속도 개선
- ✅ **확장성**: 다중 사용자 환경 대응

### 1.2 Phase 4의 범위

**포함되는 기능**:
- Spring Security 기반 인증/인가
- JWT 토큰 기반 stateless 인증
- 사용자별 답변 이력 분리
- GitHub Actions CI/CD
- Grafana 모니터링 대시보드
- Redis 캐싱 (중복 요청, Rate Limit)
- 질문 검색 기능

**제외되는 기능** (Phase 5 이후):
- ❌ 소셜 로그인 (Google, GitHub)
- ❌ 관리자 대시보드
- ❌ 음성 입력/출력
- ❌ 다중 AI 모델 지원
- ❌ 다른 직무 분야 확장

---

## 2. 현재 상태 (Phase 3 완료)

### 2.1 구현 완료된 기능

✅ **백엔드**:
- OpenAI API 연동 (gpt-4o-mini)
- 4가지 평가 기준 + 모범답변
- 중복 요청 방지 (SHA-256 해싱, 24시간 캐싱)
- Rate Limiting (IP당 33회/시간)
- Fallback 메커니즘

✅ **인프라**:
- Docker 컨테이너화 (Multi-stage 빌드, ~180MB)
- Docker Compose (PostgreSQL + App)
- 환경별 설정 분리 (dev, prod)
- 구조화된 로깅 (JSON)
- Prometheus 메트릭 수집

✅ **UI/UX**:
- Tailwind CSS 디자인 시스템
- HTMX 인터랙티브 요소
- 사용자 친화적 에러 페이지

### 2.2 개선 필요 영역

🔸 **사용자 관리**:
- 현재 단일 사용자 모드 (누구나 모든 답변 조회 가능)
- 로그인/회원가입 없음
- 사용자별 데이터 분리 불가

🔸 **배포 자동화**:
- 수동 빌드 및 배포
- CI/CD 파이프라인 없음
- 테스트 자동화 부족

🔸 **운영 가시성**:
- 메트릭은 수집하지만 시각화 없음
- 실시간 모니터링 어려움
- 알림 시스템 없음

🔸 **성능**:
- In-memory 캐싱 (서버 재시작 시 소실)
- 단일 서버 확장 어려움
- 동시 요청 처리 제한

---

## 3. Phase 4 목표

### 3.1 사용자 관리 시스템 (Phase 4A)

**목표**: 다중 사용자 환경 지원

**구현 항목**:
1. **Spring Security 도입**
   - BCrypt 비밀번호 암호화
   - JWT 토큰 기반 인증
   - 역할 기반 접근 제어 (USER, ADMIN)

2. **회원가입/로그인**
   - 이메일 기반 회원가입
   - 비밀번호 유효성 검증
   - 이메일 중복 체크
   - 로그인 실패 제한 (계정 잠금)

3. **사용자 도메인 확장**
   - User 엔티티 추가
   - InterviewAnswer에 userId 추가
   - 사용자별 답변 이력 조회

4. **보안 강화**
   - CSRF 보호
   - XSS 방지
   - SQL Injection 방지 (이미 JPA로 대부분 방지)
   - Rate Limit을 사용자별로 변경 (IP → User ID)

### 3.2 CI/CD 파이프라인 (Phase 4B)

**목표**: 배포 자동화 및 품질 보장

**구현 항목**:
1. **GitHub Actions 워크플로우**
   - PR 생성 시: 빌드 + 테스트
   - main 브랜치 merge 시: Docker 이미지 빌드 + Docker Hub push
   - 선택적: AWS ECS 자동 배포

2. **테스트 자동화**
   - 단위 테스트 실행
   - 통합 테스트 실행
   - 테스트 커버리지 리포트
   - 실패 시 배포 중단

3. **품질 게이트**
   - ktlint/Detekt 코드 스타일 검사
   - 의존성 취약점 스캔
   - Docker 이미지 보안 스캔

### 3.3 모니터링 대시보드 (Phase 4C)

**목표**: 실시간 운영 가시성

**구현 항목**:
1. **Grafana + Prometheus 연동**
   - Docker Compose에 Grafana, Prometheus 추가
   - Spring Boot 메트릭 수집 설정
   - 대시보드 JSON 템플릿 제공

2. **주요 메트릭 시각화**
   - HTTP 요청 수, 응답 시간, 에러율
   - AI API 호출 횟수, 지연 시간, 토큰 사용량
   - 캐시 히트율
   - Rate Limit 거부 횟수
   - JVM 메모리, GC 통계

3. **알림 설정**
   - AI API 비용 임계값 초과
   - 에러율 급증
   - 응답 시간 지연

### 3.4 성능 최적화 (Phase 4D)

**목표**: Redis 캐싱으로 성능 개선

**구현 항목**:
1. **Redis 도입**
   - Spring Data Redis 설정
   - Docker Compose에 Redis 추가
   - 로컬/프로덕션 환경별 설정

2. **캐싱 전략**
   - 중복 요청 방지 캐시 (In-memory → Redis)
   - Rate Limit 저장소 (Caffeine → Redis)
   - 질문 목록 캐싱 (자주 조회되는 데이터)
   - Session Storage (JWT 대신 선택적)

3. **캐시 정책**
   - TTL 설정 (중복 요청: 24시간, Rate Limit: 1시간)
   - 캐시 무효화 전략
   - 캐시 워밍 (애플리케이션 시작 시)

### 3.5 고급 기능 (Phase 4E)

**목표**: 사용자 경험 향상

**구현 항목**:
1. **질문 검색**
   - PostgreSQL Full-Text Search
   - 제목, 내용, 카테고리 통합 검색
   - 검색 결과 하이라이팅

2. **사용자 대시보드**
   - 평균 점수, 카테고리별 통계
   - 최근 답변 이력
   - 학습 진도 추적

3. **답변 개선 기능**
   - 답변 자동 저장 (Draft)
   - 이전 답변과 비교
   - 답변 이력 타임라인

---

## 4. 상세 구현 계획

### 4.1 Spring Security 및 사용자 관리

#### 4.1.1 User 엔티티 설계

```kotlin
@Entity
@Table(name = "users")
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val email: String,

    @Column(nullable = false)
    val passwordHash: String,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val role: UserRole = UserRole.USER,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    val lastLoginAt: LocalDateTime? = null
)

enum class UserRole {
    USER,       // 일반 사용자
    ADMIN       // 관리자
}
```

#### 4.1.2 InterviewAnswer 수정

```kotlin
@Entity
@Table(name = "interview_answers")
class InterviewAnswer(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val questionId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,  // 신규 추가

    @Column(columnDefinition = "TEXT")
    val answerText: String,

    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
```

**Flyway 마이그레이션**:
```sql
-- V4__add_user_management.sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);

ALTER TABLE interview_answers
ADD COLUMN user_id BIGINT;

-- 기존 데이터에 대한 임시 사용자 생성
INSERT INTO users (email, password_hash, name, role)
VALUES ('legacy@example.com', '$2a$10$...', 'Legacy User', 'USER')
RETURNING id INTO @legacy_user_id;

-- 기존 답변에 임시 사용자 할당
UPDATE interview_answers
SET user_id = @legacy_user_id
WHERE user_id IS NULL;

ALTER TABLE interview_answers
ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE interview_answers
ADD CONSTRAINT fk_answer_user
FOREIGN KEY (user_id) REFERENCES users(id);

CREATE INDEX idx_answers_user_id ON interview_answers(user_id);
```

#### 4.1.3 Spring Security 설정

**의존성 추가**:
```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")
}
```

**SecurityConfig**:
```kotlin
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val authenticationProvider: AuthenticationProvider
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }  // JWT 사용 시 CSRF 불필요
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/auth/**").permitAll()  // 회원가입, 로그인
                    .requestMatchers("/actuator/**").permitAll()   // 헬스 체크
                    .requestMatchers("/h2-console/**").permitAll() // 개발 환경
                    .requestMatchers("/error/**").permitAll()      // 에러 페이지
                    .anyRequest().authenticated()
            }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager {
        return config.authenticationManager
    }
}
```

#### 4.1.4 JWT 토큰 관리

**JwtService**:
```kotlin
@Service
class JwtService(
    @Value("\${jwt.secret}") private val secretKey: String,
    @Value("\${jwt.expiration}") private val expiration: Long
) {

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey))
    }

    fun generateToken(user: User): String {
        return Jwts.builder()
            .subject(user.email)
            .claim("userId", user.id)
            .claim("role", user.role.name)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expiration))
            .signWith(key)
            .compact()
    }

    fun extractEmail(token: String): String {
        return extractClaim(token, Claims::getSubject)
    }

    fun extractUserId(token: String): Long {
        return extractClaim(token) { it.get("userId", java.lang.Long::class.java).toLong() }
    }

    fun isTokenValid(token: String, user: User): Boolean {
        val email = extractEmail(token)
        return (email == user.email) && !isTokenExpired(token)
    }

    private fun isTokenExpired(token: String): Boolean {
        return extractExpiration(token).before(Date())
    }

    private fun extractExpiration(token: String): Date {
        return extractClaim(token, Claims::getExpiration)
    }

    private fun <T> extractClaim(token: String, claimsResolver: (Claims) -> T): T {
        val claims = extractAllClaims(token)
        return claimsResolver(claims)
    }

    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
```

**JwtAuthenticationFilter**:
```kotlin
@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val userDetailsService: UserDetailsService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val jwt = authHeader.substring(7)
        val userEmail = jwtService.extractEmail(jwt)

        if (SecurityContextHolder.getContext().authentication == null) {
            val userDetails = userDetailsService.loadUserByUsername(userEmail)

            if (jwtService.isTokenValid(jwt, userDetails as User)) {
                val authToken = UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.authorities
                )
                authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authToken
            }
        }

        filterChain.doFilter(request, response)
    }
}
```

#### 4.1.5 인증 API

**AuthController**:
```kotlin
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        val response = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val response = authService.login(request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/me")
    fun getCurrentUser(@AuthenticationPrincipal user: User): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(UserResponse.from(user))
    }
}

data class RegisterRequest(
    @field:Email(message = "유효한 이메일 주소를 입력하세요")
    val email: String,

    @field:Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
        message = "비밀번호는 대소문자와 숫자를 포함해야 합니다"
    )
    val password: String,

    @field:NotBlank(message = "이름을 입력하세요")
    @field:Size(min = 2, max = 50)
    val name: String
)

data class LoginRequest(
    @field:Email
    val email: String,

    @field:NotBlank
    val password: String
)

data class AuthResponse(
    val token: String,
    val user: UserResponse
)

data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val role: UserRole
) {
    companion object {
        fun from(user: User) = UserResponse(
            id = user.id,
            email = user.email,
            name = user.name,
            role = user.role
        )
    }
}
```

**AuthService**:
```kotlin
@Service
@Transactional
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {

    fun register(request: RegisterRequest): AuthResponse {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.email)) {
            throw DuplicateEmailException("이미 사용 중인 이메일입니다: ${request.email}")
        }

        // 사용자 생성
        val user = User(
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password),
            name = request.name,
            role = UserRole.USER
        )

        val savedUser = userRepository.save(user)

        // JWT 토큰 생성
        val token = jwtService.generateToken(savedUser)

        return AuthResponse(
            token = token,
            user = UserResponse.from(savedUser)
        )
    }

    fun login(request: LoginRequest): AuthResponse {
        // 사용자 조회
        val user = userRepository.findByEmail(request.email)
            ?: throw InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다")

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다")
        }

        // 계정 활성화 확인
        if (!user.isActive) {
            throw AccountDisabledException("비활성화된 계정입니다")
        }

        // 마지막 로그인 시간 업데이트
        userRepository.updateLastLoginAt(user.id, LocalDateTime.now())

        // JWT 토큰 생성
        val token = jwtService.generateToken(user)

        return AuthResponse(
            token = token,
            user = UserResponse.from(user)
        )
    }
}
```

#### 4.1.6 사용자별 답변 조회

**InterviewService 수정**:
```kotlin
@Service
@Transactional
class InterviewService(
    private val questionRepository: QuestionRepository,
    private val interviewAnswerRepository: InterviewAnswerRepository,
    private val aiFeedbackService: AiFeedbackService
) {

    // 사용자별 답변 제출
    fun submitAnswer(dto: AnswerSubmitDto, userId: Long): AnswerWithFeedbackDto {
        val question = questionRepository.findById(dto.questionId)
            .orElseThrow { NotFoundException("질문을 찾을 수 없습니다: ${dto.questionId}") }

        // 답변 저장 (userId 포함)
        val answer = InterviewAnswer(
            questionId = dto.questionId,
            userId = userId,  // 현재 로그인한 사용자
            answerText = dto.answerText
        )
        val savedAnswer = interviewAnswerRepository.save(answer)

        // AI 피드백 생성
        val aiFeedback = aiFeedbackService.generateFeedback(savedAnswer, question)

        return AnswerWithFeedbackDto.from(savedAnswer, question, aiFeedback)
    }

    // 사용자별 리뷰 이력 조회
    fun getUserReviews(userId: Long): List<ReviewDto> {
        val answers = interviewAnswerRepository.findByUserIdOrderByCreatedAtDesc(userId)

        return answers.map { answer ->
            val question = questionRepository.findById(answer.questionId).orElseThrow()
            val feedback = aiFeedbackService.findByInterviewAnswerId(answer.id)
                ?: throw NotFoundException("피드백을 찾을 수 없습니다")

            ReviewDto.from(answer, question, feedback)
        }
    }
}
```

**Controller 수정**:
```kotlin
@Controller
class AnswerController(
    private val interviewService: InterviewService,
    private val rateLimitService: RateLimitService
) {

    @PostMapping("/questions/{questionId}/answer")
    fun submitAnswer(
        @PathVariable questionId: Long,
        @Valid @ModelAttribute dto: AnswerSubmitDto,
        @AuthenticationPrincipal user: User  // 현재 로그인한 사용자
    ): String {
        // Rate Limit 체크 (IP → User ID로 변경)
        rateLimitService.checkAndRecordRequest(user.id.toString())

        // 답변 제출
        val result = interviewService.submitAnswer(dto, user.id)

        return "redirect:/answers/${result.answerId}/feedback"
    }
}
```

---

### 4.2 CI/CD 파이프라인

#### 4.2.1 GitHub Actions 워크플로우

**.github/workflows/ci.yml** (PR 시 테스트):
```yaml
name: CI

on:
  pull_request:
    branches: [main, develop]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'gradle'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Run tests
        run: ./gradlew test

      - name: Run ktlint
        run: ./gradlew ktlintCheck

      - name: Generate test coverage report
        run: ./gradlew jacocoTestReport

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./build/reports/jacoco/test/jacocoTestReport.xml

      - name: Comment PR with coverage
        uses: madrapps/jacoco-report@v1.6
        with:
          paths: ${{ github.workspace }}/build/reports/jacoco/test/jacocoTestReport.xml
          token: ${{ secrets.GITHUB_TOKEN }}
```

**.github/workflows/deploy.yml** (main 브랜치 merge 시 배포):
```yaml
name: Deploy

on:
  push:
    branches: [main]

jobs:
  build-and-push:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'gradle'

      - name: Build with Gradle
        run: ./gradlew build -x test

      - name: Log in to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          token: ${{ secrets.DOCKERHUB_TOKEN }}

      - name: Extract version from build.gradle.kts
        id: version
        run: |
          VERSION=$(grep "^version" build.gradle.kts | cut -d'"' -f2)
          echo "VERSION=$VERSION" >> $GITHUB_ENV

      - name: Build and push Docker image
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: |
            ${{ secrets.DOCKERHUB_USERNAME }}/interview-note-api:latest
            ${{ secrets.DOCKERHUB_USERNAME }}/interview-note-api:${{ env.VERSION }}

      - name: Scan Docker image for vulnerabilities
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: ${{ secrets.DOCKERHUB_USERNAME }}/interview-note-api:latest
          format: 'sarif'
          output: 'trivy-results.sarif'

      - name: Upload Trivy results to GitHub Security
        uses: github/codeql-action/upload-sarif@v2
        with:
          sarif_file: 'trivy-results.sarif'

  # 선택적: AWS ECS 배포
  deploy-ecs:
    needs: build-and-push
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'

    steps:
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ap-northeast-2

      - name: Deploy to ECS
        run: |
          aws ecs update-service \
            --cluster interview-note-cluster \
            --service interview-note-service \
            --force-new-deployment
```

**.github/workflows/dependency-review.yml** (의존성 취약점 스캔):
```yaml
name: Dependency Review

on: [pull_request]

permissions:
  contents: read

jobs:
  dependency-review:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Dependency Review
        uses: actions/dependency-review-action@v3
```

#### 4.2.2 ktlint 설정

**build.gradle.kts에 추가**:
```kotlin
plugins {
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
}

ktlint {
    version.set("1.0.1")
    android.set(false)
    outputToConsole.set(true)
    ignoreFailures.set(false)
}
```

#### 4.2.3 JaCoCo 테스트 커버리지

**build.gradle.kts에 추가**:
```kotlin
plugins {
    jacoco
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/config/**",
                    "**/dto/**",
                    "**/domain/**",
                    "**/*Application.kt"
                )
            }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.70".toBigDecimal()  // 70% 커버리지 요구
            }
        }
    }
}
```

---

### 4.3 모니터링 대시보드 (Grafana)

#### 4.3.1 Docker Compose 확장

**docker-compose.yml 수정**:
```yaml
version: '3.8'

services:
  postgres:
    # ... 기존 설정 유지 ...

  redis:
    image: redis:7-alpine
    container_name: interview-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5
    networks:
      - interview-network

  prometheus:
    image: prom/prometheus:latest
    container_name: interview-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
    networks:
      - interview-network

  grafana:
    image: grafana/grafana:latest
    container_name: interview-grafana
    ports:
      - "3000:3000"
    volumes:
      - grafana_data:/var/lib/grafana
      - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./monitoring/grafana/datasources:/etc/grafana/provisioning/datasources
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_USERS_ALLOW_SIGN_UP=false
    networks:
      - interview-network

  app:
    # ... 기존 설정 유지 ...
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    environment:
      # ... 기존 환경변수 유지 ...
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: 6379

volumes:
  postgres_data:
  redis_data:
  prometheus_data:
  grafana_data:

networks:
  interview-network:
    driver: bridge
```

#### 4.3.2 Prometheus 설정

**monitoring/prometheus.yml**:
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'interview-note-api'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['app:8080']
        labels:
          application: 'interview-note-api'
          environment: 'production'
```

#### 4.3.3 Grafana 대시보드

**monitoring/grafana/datasources/prometheus.yml**:
```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: false
```

**monitoring/grafana/dashboards/dashboard.yml**:
```yaml
apiVersion: 1

providers:
  - name: 'Interview Note API'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /etc/grafana/provisioning/dashboards
```

**monitoring/grafana/dashboards/interview-note-api.json**:
```json
{
  "dashboard": {
    "title": "Interview Note API - Production Metrics",
    "panels": [
      {
        "title": "HTTP Request Rate",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count[5m])"
          }
        ]
      },
      {
        "title": "AI API Calls",
        "targets": [
          {
            "expr": "ai_calls_total"
          }
        ]
      },
      {
        "title": "Cache Hit Rate",
        "targets": [
          {
            "expr": "cache_hits_total / (cache_hits_total + cache_misses_total)"
          }
        ]
      },
      {
        "title": "AI Token Usage",
        "targets": [
          {
            "expr": "ai_tokens_usage"
          }
        ]
      },
      {
        "title": "Response Time (P95)",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))"
          }
        ]
      }
    ]
  }
}
```

#### 4.3.4 알림 설정

**monitoring/prometheus.yml에 알림 규칙 추가**:
```yaml
rule_files:
  - /etc/prometheus/alerts.yml

alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']
```

**monitoring/alerts.yml**:
```yaml
groups:
  - name: interview_note_api
    interval: 30s
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }} requests/second"

      - alert: HighAICost
        expr: ai_tokens_usage > 100000
        for: 1h
        labels:
          severity: warning
        annotations:
          summary: "AI token usage is high"
          description: "Token usage: {{ $value }}"

      - alert: LowCacheHitRate
        expr: cache_hits_total / (cache_hits_total + cache_misses_total) < 0.5
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Cache hit rate is low"
          description: "Hit rate: {{ $value }}"
```

---

### 4.4 Redis 캐싱

#### 4.4.1 의존성 추가

**build.gradle.kts**:
```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")
}
```

#### 4.4.2 Redis 설정

**application-prod.properties**:
```properties
# Redis
spring.data.redis.host=${SPRING_DATA_REDIS_HOST:localhost}
spring.data.redis.port=${SPRING_DATA_REDIS_PORT:6379}
spring.data.redis.timeout=2000ms

# Cache
spring.cache.type=redis
spring.cache.redis.time-to-live=3600000
spring.cache.redis.cache-null-values=false
```

**RedisConfig**:
```kotlin
@Configuration
@EnableCaching
class RedisConfig {

    @Bean
    fun redisConnectionFactory(
        @Value("\${spring.data.redis.host}") host: String,
        @Value("\${spring.data.redis.port}") port: Int
    ): RedisConnectionFactory {
        val config = RedisStandaloneConfiguration(host, port)
        return LettuceConnectionFactory(config)
    }

    @Bean
    fun redisTemplate(
        connectionFactory: RedisConnectionFactory
    ): RedisTemplate<String, Any> {
        return RedisTemplate<String, Any>().apply {
            this.connectionFactory = connectionFactory
            keySerializer = StringRedisSerializer()
            valueSerializer = GenericJackson2JsonRedisSerializer()
            hashKeySerializer = StringRedisSerializer()
            hashValueSerializer = GenericJackson2JsonRedisSerializer()
        }
    }

    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): CacheManager {
        val config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(24))  // 기본 24시간
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    GenericJackson2JsonRedisSerializer()
                )
            )

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withInitialCacheConfigurations(
                mapOf(
                    "duplicateRequests" to config.entryTtl(Duration.ofHours(24)),
                    "rateLimits" to config.entryTtl(Duration.ofHours(1)),
                    "questions" to config.entryTtl(Duration.ofMinutes(30))
                )
            )
            .build()
    }
}
```

#### 4.4.3 DuplicateRequestCache 리팩토링

**변경 전 (In-memory, AiFeedback 테이블 조회)**:
```kotlin
@Service
class DuplicateRequestCache(
    private val aiFeedbackRepository: AiFeedbackRepository
) {
    fun findCached(questionId: Long, answerText: String): AiFeedback? {
        val hash = generateHash(questionId, answerText)
        val cutoffTime = LocalDateTime.now().minusHours(24)
        return aiFeedbackRepository.findByAnswerTextHashAndCreatedAtAfter(hash, cutoffTime)
    }
}
```

**변경 후 (Redis)**:
```kotlin
@Service
class DuplicateRequestCache(
    private val redisTemplate: RedisTemplate<String, Any>
) {

    companion object {
        private const val CACHE_PREFIX = "duplicate:"
        private const val CACHE_TTL_HOURS = 24L
    }

    fun findCached(questionId: Long, answerText: String): AiFeedback? {
        val hash = generateHash(questionId, answerText)
        val key = "$CACHE_PREFIX$hash"

        return redisTemplate.opsForValue().get(key) as? AiFeedback
    }

    fun storeCached(questionId: Long, answerText: String, feedback: AiFeedback) {
        val hash = generateHash(questionId, answerText)
        val key = "$CACHE_PREFIX$hash"

        redisTemplate.opsForValue().set(
            key,
            feedback,
            Duration.ofHours(CACHE_TTL_HOURS)
        )
    }

    fun generateHash(questionId: Long, answerText: String): String {
        val input = "$questionId:$answerText"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
```

#### 4.4.4 RateLimitService 리팩토링

**변경 전 (Caffeine Cache)**:
```kotlin
@Service
class RateLimitService {
    private val requestCache: Cache<String, MutableList<LocalDateTime>> = Caffeine.newBuilder()
        .expireAfterWrite(60, TimeUnit.MINUTES)
        .build()
}
```

**변경 후 (Redis)**:
```kotlin
@Service
class RateLimitService(
    private val redisTemplate: RedisTemplate<String, Any>
) {

    companion object {
        private const val MAX_REQUESTS_PER_HOUR = 33
        private const val RATE_LIMIT_PREFIX = "ratelimit:"
        private const val WINDOW_DURATION_MINUTES = 60L
    }

    fun checkAndRecordRequest(userId: String) {
        val key = "$RATE_LIMIT_PREFIX$userId"
        val now = LocalDateTime.now()
        val cutoffTime = now.minusMinutes(WINDOW_DURATION_MINUTES)

        // Sorted Set에 타임스탬프 저장 (자동 정렬)
        val zSetOps = redisTemplate.opsForZSet()

        // 1시간 이전 요청 제거
        zSetOps.removeRangeByScore(key, 0.0, cutoffTime.toEpochSecond(ZoneOffset.UTC).toDouble())

        // 현재 요청 수 확인
        val currentCount = zSetOps.size(key) ?: 0

        if (currentCount >= MAX_REQUESTS_PER_HOUR) {
            val oldestRequest = zSetOps.range(key, 0, 0)?.firstOrNull()
            val resetTime = if (oldestRequest != null) {
                LocalDateTime.ofEpochSecond(
                    oldestRequest.toString().toLong(),
                    0,
                    ZoneOffset.UTC
                ).plusMinutes(WINDOW_DURATION_MINUTES)
            } else {
                now.plusMinutes(WINDOW_DURATION_MINUTES)
            }

            throw RateLimitExceededException(userId, MAX_REQUESTS_PER_HOUR, resetTime)
        }

        // 새 요청 기록
        val score = now.toEpochSecond(ZoneOffset.UTC).toDouble()
        zSetOps.add(key, score.toString(), score)

        // TTL 설정 (자동 만료)
        redisTemplate.expire(key, Duration.ofMinutes(WINDOW_DURATION_MINUTES))
    }
}
```

#### 4.4.5 질문 목록 캐싱

**QuestionService**:
```kotlin
@Service
class QuestionService(
    private val questionRepository: QuestionRepository
) {

    @Cacheable(value = ["questions"], key = "#category + ':' + #difficulty")
    fun getQuestions(category: String?, difficulty: String?): List<QuestionDto> {
        return when {
            category != null && difficulty != null ->
                questionRepository.findByCategoryAndDifficultyAndIsActiveTrue(category, difficulty)
            category != null ->
                questionRepository.findByCategoryAndIsActiveTrue(category)
            difficulty != null ->
                questionRepository.findByDifficultyAndIsActiveTrue(difficulty)
            else ->
                questionRepository.findByIsActiveTrue()
        }.map { QuestionDto.from(it) }
    }

    @CacheEvict(value = ["questions"], allEntries = true)
    fun createQuestion(dto: CreateQuestionDto): Question {
        // 새 질문 생성 시 캐시 무효화
        return questionRepository.save(Question(...))
    }
}
```

---

### 4.5 질문 검색 기능

#### 4.5.1 PostgreSQL Full-Text Search

**Flyway 마이그레이션**:
```sql
-- V5__add_fulltext_search.sql

-- 검색용 tsvector 컬럼 추가
ALTER TABLE questions
ADD COLUMN search_vector tsvector;

-- 검색 인덱스 생성
CREATE INDEX idx_questions_search ON questions USING GIN(search_vector);

-- 트리거 함수 생성 (자동 업데이트)
CREATE OR REPLACE FUNCTION questions_search_trigger() RETURNS trigger AS $$
BEGIN
  NEW.search_vector :=
    setweight(to_tsvector('korean', coalesce(NEW.content, '')), 'A') ||
    setweight(to_tsvector('korean', coalesce(NEW.category, '')), 'B') ||
    setweight(to_tsvector('korean', coalesce(NEW.target_job, '')), 'C');
  RETURN NEW;
END
$$ LANGUAGE plpgsql;

-- 트리거 생성
CREATE TRIGGER tsvector_update BEFORE INSERT OR UPDATE
ON questions FOR EACH ROW EXECUTE FUNCTION questions_search_trigger();

-- 기존 데이터 업데이트
UPDATE questions SET search_vector =
  setweight(to_tsvector('korean', coalesce(content, '')), 'A') ||
  setweight(to_tsvector('korean', coalesce(category, '')), 'B') ||
  setweight(to_tsvector('korean', coalesce(target_job, '')), 'C');
```

#### 4.5.2 검색 Repository

**QuestionRepository**:
```kotlin
interface QuestionRepository : JpaRepository<Question, Long> {

    // 기존 메서드들...

    @Query(
        """
        SELECT q FROM Question q
        WHERE q.isActive = true
        AND to_tsvector('korean', q.content || ' ' || q.category || ' ' || q.targetJob)
            @@ plainto_tsquery('korean', :keyword)
        ORDER BY
            ts_rank(to_tsvector('korean', q.content || ' ' || q.category || ' ' || q.targetJob),
                    plainto_tsquery('korean', :keyword)) DESC
        """,
        nativeQuery = true
    )
    fun searchByKeyword(keyword: String): List<Question>
}
```

#### 4.5.3 검색 API

**QuestionController**:
```kotlin
@Controller
class QuestionController(
    private val questionService: QuestionService
) {

    @GetMapping("/questions/search")
    fun searchQuestions(
        @RequestParam keyword: String,
        model: Model
    ): String {
        val questions = questionService.searchQuestions(keyword)
        model.addAttribute("questions", questions)
        model.addAttribute("keyword", keyword)
        return "questions/search-results"
    }
}
```

**QuestionService**:
```kotlin
@Service
class QuestionService(
    private val questionRepository: QuestionRepository
) {

    fun searchQuestions(keyword: String): List<QuestionDto> {
        if (keyword.isBlank()) {
            return emptyList()
        }

        return questionRepository.searchByKeyword(keyword)
            .map { QuestionDto.from(it) }
    }
}
```

---

## 5. 구현 순서

### Phase 4A: 사용자 관리 (5-7일)

#### Step 1: User 엔티티 및 마이그레이션
- [ ] User 엔티티 생성
- [ ] UserRole enum 정의
- [ ] UserRepository 생성
- [ ] Flyway 마이그레이션 작성 (V4__add_user_management.sql)
- [ ] InterviewAnswer에 userId 추가
- [ ] 마이그레이션 테스트

#### Step 2: Spring Security 설정
- [ ] 의존성 추가 (Spring Security, JWT)
- [ ] SecurityConfig 작성
- [ ] JwtService 구현
- [ ] JwtAuthenticationFilter 구현
- [ ] UserDetailsService 구현
- [ ] PasswordEncoder Bean 등록

#### Step 3: 인증 API
- [ ] AuthController 작성
- [ ] AuthService 작성
- [ ] 회원가입 DTO 및 Validation
- [ ] 로그인 DTO
- [ ] 예외 클래스 (DuplicateEmailException, InvalidCredentialsException)
- [ ] GlobalExceptionHandler에 예외 핸들러 추가

#### Step 4: 기존 컨트롤러 수정
- [ ] AnswerController에 @AuthenticationPrincipal 추가
- [ ] ReviewController 사용자별 조회로 변경
- [ ] QuestionController 인증 필요 없음 (공개)

#### Step 5: Rate Limit 수정
- [ ] RateLimitService를 User ID 기반으로 변경
- [ ] 테스트 업데이트

#### Step 6: 테스트
- [ ] AuthService 단위 테스트
- [ ] JWT 토큰 생성/검증 테스트
- [ ] 인증 API 통합 테스트
- [ ] 인증 필요한 엔드포인트 테스트

**체크포인트**:
- ✅ 회원가입/로그인 API 동작
- ✅ JWT 토큰으로 인증된 요청 가능
- ✅ 사용자별 답변 이력 분리
- ✅ 모든 테스트 통과

---

### Phase 4B: CI/CD 파이프라인 (2-3일)

#### Step 7: ktlint 및 JaCoCo 설정
- [ ] build.gradle.kts에 ktlint 플러그인 추가
- [ ] ktlint 설정 파일 작성
- [ ] JaCoCo 플러그인 추가
- [ ] 커버리지 최소 기준 설정 (70%)
- [ ] 로컬에서 ./gradlew ktlintCheck 실행 확인

#### Step 8: GitHub Actions 워크플로우 작성
- [ ] .github/workflows/ci.yml 작성 (PR 테스트)
- [ ] .github/workflows/deploy.yml 작성 (Docker 빌드/푸시)
- [ ] .github/workflows/dependency-review.yml 작성
- [ ] Docker Hub Secrets 설정 안내 문서 작성

#### Step 9: 테스트 실행
- [ ] PR 생성하여 CI 워크플로우 테스트
- [ ] main 브랜치 merge하여 Deploy 워크플로우 테스트
- [ ] Docker Hub에 이미지 업로드 확인
- [ ] Trivy 보안 스캔 결과 확인

**체크포인트**:
- ✅ PR 생성 시 자동 테스트 실행
- ✅ main 브랜치 merge 시 Docker 이미지 자동 빌드
- ✅ Docker Hub에 이미지 업로드
- ✅ 테스트 커버리지 리포트 생성

---

### Phase 4C: 모니터링 대시보드 (3-4일)

#### Step 10: Prometheus 및 Grafana 설정
- [ ] docker-compose.yml에 Prometheus 추가
- [ ] docker-compose.yml에 Grafana 추가
- [ ] monitoring/prometheus.yml 작성
- [ ] monitoring/alerts.yml 작성 (알림 규칙)
- [ ] monitoring/grafana/datasources/prometheus.yml 작성
- [ ] monitoring/grafana/dashboards/dashboard.yml 작성

#### Step 11: Grafana 대시보드 구성
- [ ] 대시보드 JSON 템플릿 작성
- [ ] HTTP 요청 메트릭 패널
- [ ] AI API 호출 메트릭 패널
- [ ] 캐시 히트율 패널
- [ ] 토큰 사용량 패널
- [ ] 응답 시간 (P95, P99) 패널
- [ ] JVM 메모리 패널

#### Step 12: 알림 설정 (선택사항)
- [ ] Alertmanager 설정 (선택)
- [ ] Slack webhook 연동
- [ ] 이메일 알림 설정

#### Step 13: 테스트
- [ ] docker-compose up으로 전체 스택 실행
- [ ] Prometheus 접속 (http://localhost:9090)
- [ ] Grafana 접속 (http://localhost:3000, admin/admin)
- [ ] 메트릭 수집 확인
- [ ] 대시보드 정상 표시 확인

**체크포인트**:
- ✅ Grafana 대시보드에서 실시간 메트릭 확인
- ✅ Prometheus에서 메트릭 쿼리 가능
- ✅ 알림 규칙 동작 (선택)

---

### Phase 4D: Redis 캐싱 (2-3일)

#### Step 14: Redis 설정
- [ ] build.gradle.kts에 Redis 의존성 추가
- [ ] RedisConfig 작성
- [ ] application-prod.properties에 Redis 설정 추가
- [ ] docker-compose.yml에 Redis 추가

#### Step 15: 캐싱 리팩토링
- [ ] DuplicateRequestCache를 Redis 기반으로 변경
- [ ] RateLimitService를 Redis 기반으로 변경
- [ ] QuestionService에 @Cacheable 추가
- [ ] 기존 Caffeine Cache 제거

#### Step 16: 테스트
- [ ] Redis 연결 테스트
- [ ] 중복 요청 방지 테스트 (Redis 저장 확인)
- [ ] Rate Limit 테스트 (Redis 저장 확인)
- [ ] 질문 목록 캐싱 테스트
- [ ] 캐시 무효화 테스트

**체크포인트**:
- ✅ Redis 정상 연결
- ✅ 중복 요청 방지가 Redis로 동작
- ✅ Rate Limit이 Redis로 동작
- ✅ 서버 재시작 후에도 캐시 유지

---

### Phase 4E: 고급 기능 (3-4일)

#### Step 17: 질문 검색
- [ ] Flyway 마이그레이션 (Full-Text Search)
- [ ] QuestionRepository에 검색 쿼리 추가
- [ ] QuestionService.searchQuestions() 구현
- [ ] QuestionController.searchQuestions() 추가
- [ ] 검색 결과 페이지 템플릿 작성
- [ ] 검색 테스트

#### Step 18: 사용자 대시보드
- [ ] 대시보드 페이지 라우팅
- [ ] 사용자 통계 계산 (평균 점수, 카테고리별 통계)
- [ ] 최근 답변 이력 조회
- [ ] 대시보드 템플릿 작성
- [ ] Chart.js로 그래프 추가

#### Step 19: 답변 자동 저장 (Draft)
- [ ] InterviewAnswer에 isDraft 필드 추가
- [ ] Draft 저장 API
- [ ] Draft 불러오기 API
- [ ] HTMX로 자동 저장 구현 (30초마다)
- [ ] Draft 목록 페이지

**체크포인트**:
- ✅ 질문 검색 동작
- ✅ 사용자 대시보드 통계 표시
- ✅ 답변 자동 저장 동작

---

### Phase 4F: 문서화 및 배포 (1-2일)

#### Step 20: 문서 업데이트
- [ ] README.md 업데이트 (사용자 관리, CI/CD, Grafana)
- [ ] SETUP_GUIDE.md 업데이트
- [ ] CHANGELOG.md 업데이트 (v0.4.0)
- [ ] API 문서 작성 (선택: Swagger/OpenAPI)

#### Step 21: E2E 테스트
- [ ] 회원가입 → 로그인 → 질문 조회 → 답변 제출 → 리뷰 조회
- [ ] 검색 기능 테스트
- [ ] 대시보드 접근 테스트
- [ ] Draft 저장/불러오기 테스트

#### Step 22: 프로덕션 배포 준비
- [ ] 환경변수 체크리스트 작성
- [ ] Docker Compose로 로컬 테스트
- [ ] AWS ECS 배포 가이드 작성 (선택)
- [ ] 롤백 절차 문서화

**체크포인트**:
- ✅ Phase 4 문서 완성
- ✅ 전체 E2E 테스트 통과
- ✅ 프로덕션 배포 준비 완료

---

## 6. 검증 계획

### 6.1 사용자 관리 검증

**API 테스트**:
```bash
# 회원가입
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test1234!",
    "name": "테스트"
  }'

# 응답 예시
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "email": "test@example.com",
    "name": "테스트",
    "role": "USER"
  }
}

# 로그인
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test1234!"
  }'

# 인증된 요청
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <token>"
```

**보안 테스트**:
- [ ] 잘못된 비밀번호로 로그인 차단
- [ ] 중복 이메일 회원가입 차단
- [ ] JWT 토큰 없이 보호된 엔드포인트 접근 차단 (401)
- [ ] 만료된 토큰으로 접근 차단
- [ ] 비밀번호 복잡도 검증

### 6.2 CI/CD 검증

**GitHub Actions 확인**:
```bash
# PR 생성
git checkout -b feature/test
git commit --allow-empty -m "Test CI"
git push origin feature/test
# GitHub에서 PR 생성 → Actions 탭에서 CI 실행 확인

# main 브랜치 merge
# Actions 탭에서 Deploy 워크플로우 실행 확인
# Docker Hub에서 이미지 업로드 확인
```

**Docker 이미지 확인**:
```bash
docker pull <username>/interview-note-api:latest
docker run -d -p 8080:8080 \
  -e OPENAI_API_KEY=sk-proj-... \
  <username>/interview-note-api:latest
```

### 6.3 모니터링 검증

**Grafana 접속**:
```bash
# Grafana 접속
http://localhost:3000
# 로그인: admin / admin

# 대시보드 확인
- HTTP 요청 메트릭이 그래프로 표시되는지
- AI API 호출 횟수가 증가하는지
- 캐시 히트율이 계산되는지
```

**Prometheus 쿼리**:
```bash
# Prometheus 접속
http://localhost:9090

# 쿼리 예시
ai_calls_total
rate(http_server_requests_seconds_count[5m])
cache_hits_total / (cache_hits_total + cache_misses_total)
```

### 6.4 Redis 검증

**Redis CLI 확인**:
```bash
# Redis 컨테이너 접속
docker exec -it interview-redis redis-cli

# 캐시 키 확인
KEYS duplicate:*
KEYS ratelimit:*
KEYS questions:*

# TTL 확인
TTL duplicate:abc123...

# 값 확인
GET duplicate:abc123...
```

**성능 테스트**:
```bash
# 동일 답변 2회 제출 시간 비교
# 1회차: ~5-10초 (AI 호출)
# 2회차: ~100ms (Redis 캐시)
```

### 6.5 질문 검색 검증

**검색 테스트**:
```bash
# 브라우저에서
http://localhost:8080/questions/search?keyword=Spring

# 예상 결과: "Spring"이 포함된 질문 목록
# 검색어 하이라이팅 확인
```

---

## 7. 향후 확장 (Phase 5 이후)

Phase 4 완료 후 고려할 수 있는 확장 기능:

### 7.1 소셜 로그인 (Phase 5A)
- OAuth 2.0 (Google, GitHub)
- Spring Security OAuth2 Client
- 소셜 계정 연동

### 7.2 다중 AI 모델 지원 (Phase 5B)
- Claude API 연동
- Gemini API 연동
- 사용자가 모델 선택 가능
- 모델별 평가 비교

### 7.3 다양한 직무 분야 (Phase 5C)
- 영업, 경영, 회계, 마케팅 직무 추가
- 직무별 프롬프트 템플릿
- 직무별 평가 기준
- CLAUDE.md의 확장성 활용

### 7.4 음성 기능 (Phase 5D)
- Web Speech API로 음성 입력
- TTS로 피드백 읽어주기
- 실제 면접 시뮬레이션

### 7.5 고급 분석 (Phase 5E)
- 학습 경로 추천 (AI 기반)
- 약점 분석 및 개선 제안
- 카테고리별 강점/약점 시각화
- 답변 품질 추이 그래프

### 7.6 관리자 기능 (Phase 5F)
- 관리자 대시보드
- 질문 관리 (CRUD)
- 사용자 관리
- AI 비용 모니터링

---

## 8. 성공 기준

Phase 4 완료 시 다음 기준을 만족해야 합니다:

### 8.1 사용자 관리
- ✅ 회원가입/로그인 동작
- ✅ JWT 토큰 기반 인증
- ✅ 사용자별 답변 이력 분리
- ✅ 보안 요구사항 충족 (비밀번호 암호화, CSRF 방지)

### 8.2 CI/CD
- ✅ PR 생성 시 자동 테스트
- ✅ main 브랜치 merge 시 Docker 이미지 자동 빌드
- ✅ 테스트 커버리지 70% 이상
- ✅ 코드 스타일 검사 통과

### 8.3 모니터링
- ✅ Grafana 대시보드에서 실시간 메트릭 확인
- ✅ Prometheus 알림 설정 (선택)
- ✅ 주요 메트릭 수집 (HTTP, AI, 캐시)

### 8.4 성능
- ✅ Redis 캐싱으로 응답 속도 개선
- ✅ 중복 요청 방지가 Redis로 동작
- ✅ 서버 재시작 후에도 캐시 유지

### 8.5 기능
- ✅ 질문 검색 동작
- ✅ 사용자 대시보드 통계
- ✅ 답변 자동 저장 (Draft)

---

## 9. 예상 일정

**총 소요 시간**: 약 3-4주

| Phase | 작업 내용 | 예상 일수 |
|-------|---------|----------|
| 4A | 사용자 관리 시스템 | 5-7일 |
| 4B | CI/CD 파이프라인 | 2-3일 |
| 4C | 모니터링 대시보드 | 3-4일 |
| 4D | Redis 캐싱 | 2-3일 |
| 4E | 고급 기능 | 3-4일 |
| 4F | 문서화 및 배포 | 1-2일 |

**병렬 작업 가능**:
- 4B(CI/CD)와 4C(모니터링)는 독립적으로 진행 가능
- 4D(Redis)와 4E(고급 기능)도 일부 병렬 가능

---

**작성일**: 2026-04-15
**버전**: 1.0
**작성자**: Claude Code with 호준

**다음 단계**: Phase 4A부터 시작 (사용자 관리 시스템)
