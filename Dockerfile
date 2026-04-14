# Stage 1: Build
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

# Gradle 캐싱 최적화를 위해 먼저 의존성만 다운로드
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사 및 빌드
COPY src ./src
RUN gradle build -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 타임존 설정 (한국 시간)
RUN apk add --no-cache tzdata
ENV TZ=Asia/Seoul

# 비루트 유저 생성 (보안 강화)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 헬스 체크 (Spring Boot Actuator 사용)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", \
    "app.jar"]
