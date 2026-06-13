# 환경 설정 가이드

이 문서는 면접 리뷰 웹 애플리케이션의 개발 환경 설정 방법을 상세히 설명합니다.

## 📋 목차

1. [필수 소프트웨어 설치](#1-필수-소프트웨어-설치)
2. [OpenAI API 키 설정](#2-openai-api-키-설정)
3. [개발 환경별 실행 방법](#3-개발-환경별-실행-방법)
4. [데이터베이스 설정](#4-데이터베이스-설정)
5. [Docker로 실행 (권장)](#5-docker로-실행-권장)
6. [문제 해결](#6-문제-해결)
7. [프로덕션 배포 가이드](#7-프로덕션-배포-가이드)

---

## 1. 필수 소프트웨어 설치

### Java 21

**macOS (Homebrew)**:
```bash
brew install openjdk@21
```

**Windows (Scoop)**:
```powershell
scoop install openjdk21
```

**확인**:
```bash
java -version
# openjdk version "21.0.10" 이상이어야 함
```

### Gradle (선택사항)

프로젝트에 Gradle Wrapper가 포함되어 있어 별도 설치 불필요:
```bash
./gradlew --version
```

---

## 2. OpenAI API 키 설정

### Step 1: API 키 발급

1. [OpenAI Platform](https://platform.openai.com/signup) 계정 생성
2. 결제 방법 등록 (무료 크레딧 또는 유료)
3. [API Keys](https://platform.openai.com/api-keys) 페이지 접속
4. `Create new secret key` 클릭
5. 키 이름 입력 (예: "interview-note-dev")
6. 생성된 키 복사 (**한 번만 표시됨**)

### Step 2: 프로젝트 루트에 `.env` 파일 생성

```bash
cd /path/to/interview-note-api
touch .env
```

`.env` 파일 내용:
```bash
# OpenAI API Key
OPENAI_API_KEY=sk-proj-your-actual-api-key-here
```

**⚠️ 보안 주의사항**:
- `.env` 파일은 `.gitignore`에 포함되어 Git에 커밋되지 않습니다
- API 키를 절대 공개 저장소에 커밋하지 마세요
- API 키가 노출되면 즉시 OpenAI 대시보드에서 삭제하세요

### Step 3: 환경변수 로드 확인

**터미널에서 확인**:
```bash
export $(cat .env | grep -v '^#' | xargs)
echo $OPENAI_API_KEY
# sk-proj-... 출력되어야 함
```

---

## 3. 개발 환경별 실행 방법

### A. 터미널 (권장)

**.env 파일 자동 로드**:
```bash
export $(cat .env | grep -v '^#' | xargs)
./gradlew bootRun
```

**직접 환경변수 설정**:
```bash
OPENAI_API_KEY=sk-proj-... ./gradlew bootRun
```

**빌드 후 JAR 실행**:
```bash
./gradlew build
export $(cat .env | grep -v '^#' | xargs)
java -jar build/libs/interview-note-api-0.0.1-SNAPSHOT.jar
```

### B. IntelliJ IDEA

#### 방법 1: Run Configuration 설정 (권장)

1. `InterviewNoteApiApplication.kt` 우클릭
2. `Modify Run Configuration...` 선택
3. **Environment variables** 필드에 입력:
   ```
   OPENAI_API_KEY=sk-proj-your-actual-api-key-here
   ```
4. `Apply` → `OK`
5. `Run` 버튼 클릭 (▶️)

#### 방법 2: EnvFile 플러그인 사용

1. **플러그인 설치**:
   - `Settings/Preferences` → `Plugins`
   - "EnvFile" 검색 및 설치
   - IntelliJ 재시작

2. **Run Configuration 설정**:
   - `Modify Run Configuration...`
   - `EnvFile` 탭 선택
   - `Enable EnvFile` 체크
   - `+` 버튼 → `.env` 파일 선택
   - `Apply` → `OK`

3. **실행**:
   - `Run` 버튼 클릭

#### 방법 3: macOS/Linux 시스템 환경변수

```bash
# ~/.zshrc 또는 ~/.bashrc에 추가
export OPENAI_API_KEY=sk-proj-your-actual-api-key-here
```

터미널 재시작 후 IntelliJ 실행

### C. VS Code

**tasks.json 설정**:
```json
{
  "version": "2.0.0",
  "tasks": [
    {
      "label": "bootRun",
      "type": "shell",
      "command": "./gradlew bootRun",
      "env": {
        "OPENAI_API_KEY": "sk-proj-your-actual-api-key-here"
      }
    }
  ]
}
```

---

## 4. 데이터베이스 설정

### H2 (개발 환경, 기본값)

**application.properties**에 이미 설정됨:
```properties
spring.datasource.url=jdbc:h2:mem:interviewdb
spring.h2.console.enabled=true
```

**H2 콘솔 접속**:
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:interviewdb`
- Username: `sa`
- Password: (비어있음)

### PostgreSQL (프로덕션 환경)

**1. PostgreSQL 설치**:
```bash
# macOS
brew install postgresql@15
brew services start postgresql@15

# Ubuntu
sudo apt-get install postgresql-15
```

**2. 데이터베이스 생성**:
```sql
CREATE DATABASE interviewdb;
CREATE USER interviewuser WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE interviewdb TO interviewuser;
```

**3. application-prod.properties 생성**:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/interviewdb
spring.datasource.username=interviewuser
spring.datasource.password=your_password
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

**4. 프로덕션 프로필로 실행**:
```bash
export SPRING_PROFILES_ACTIVE=prod
./gradlew bootRun
```

---

## 5. Docker로 실행 (권장)

Docker를 사용하면 환경 설정을 자동화하고 일관된 실행 환경을 보장할 수 있습니다.

### 사전 요구사항

**Docker 설치**:
- **macOS**: [Docker Desktop for Mac](https://docs.docker.com/desktop/install/mac-install/)
- **Windows**: [Docker Desktop for Windows](https://docs.docker.com/desktop/install/windows-install/)
- **Linux**: [Docker Engine](https://docs.docker.com/engine/install/)

**버전 확인**:
```bash
docker --version   # 20.10 이상
docker-compose --version  # v2.0 이상
```

### 빠른 시작

#### 1. 환경변수 설정

```bash
# .env 파일 생성
cp .env.example .env

# .env 파일 편집
vim .env  # 또는 원하는 에디터 사용
```

`.env` 파일 내용:
```bash
OPENAI_API_KEY=sk-proj-your-actual-api-key-here
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/interviewdb
SPRING_DATASOURCE_USERNAME=interviewuser
SPRING_DATASOURCE_PASSWORD=interviewpass
```

#### 2. Docker Compose로 실행

```bash
# 전체 스택 실행 (PostgreSQL + 애플리케이션)
docker-compose up -d

# 로그 확인
docker-compose logs -f app

# 상태 확인
docker-compose ps
```

**기대 출력**:
```
NAME                  STATUS         PORTS
interview-postgres    Up (healthy)   0.0.0.0:5432->5432/tcp
interview-note-api    Up (healthy)   0.0.0.0:8080->8080/tcp
```

#### 3. 애플리케이션 접속

- **웹 애플리케이션**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **Prometheus 메트릭**: http://localhost:8080/actuator/prometheus

#### 4. 중지 및 재시작

```bash
# 중지
docker-compose down

# 중지 및 데이터 삭제 (완전 초기화)
docker-compose down -v

# 재시작
docker-compose restart app

# 특정 서비스만 재시작
docker-compose restart postgres
```

### 개발 환경으로 실행

**H2 데이터베이스 사용** (PostgreSQL 없이):

```bash
# .env 파일 수정
SPRING_PROFILES_ACTIVE=dev

# 애플리케이션만 실행 (PostgreSQL 제외)
docker-compose up -d app
# 또는
docker run -d \
  --name interview-note-api \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e OPENAI_API_KEY=sk-proj-... \
  interview-note-api:latest
```

### 이미지 빌드 및 관리

```bash
# 이미지 빌드
docker build -t interview-note-api:latest .

# 이미지 크기 확인
docker images interview-note-api
# 예상: ~180MB

# 캐시 없이 재빌드
docker-compose build --no-cache

# 이미지 삭제
docker rmi interview-note-api:latest
```

### 데이터베이스 관리

#### PostgreSQL 접속

```bash
# 컨테이너 내부 psql 접속
docker-compose exec postgres psql -U interviewuser -d interviewdb

# SQL 실행 예시
\dt  # 테이블 목록
SELECT * FROM questions LIMIT 5;
\q   # 종료
```

#### 데이터베이스 백업

```bash
# 백업
docker-compose exec postgres pg_dump -U interviewuser interviewdb > backup_$(date +%Y%m%d).sql

# 복원
docker-compose exec -T postgres psql -U interviewuser -d interviewdb < backup_20260414.sql
```

#### 데이터 초기화

```bash
# 모든 데이터 삭제 (볼륨 포함)
docker-compose down -v

# 재시작 (Flyway가 자동으로 마이그레이션 실행)
docker-compose up -d
```

### 로그 확인

```bash
# 전체 로그
docker-compose logs

# 애플리케이션 로그만
docker-compose logs app

# PostgreSQL 로그만
docker-compose logs postgres

# 실시간 로그 (tail -f)
docker-compose logs -f app

# 최근 100줄만
docker-compose logs --tail=100 app
```

### 메트릭 및 모니터링

#### Prometheus 메트릭 수집

```bash
# 메트릭 확인
curl http://localhost:8080/actuator/prometheus

# 특정 메트릭 필터링
curl http://localhost:8080/actuator/prometheus | grep ai_calls
```

**주요 메트릭**:
- `ai_calls_total` - AI API 호출 횟수
- `ai_calls_duration_seconds` - AI API 호출 지연 시간
- `cache_hits_total` - 캐시 히트 횟수
- `http_server_requests_seconds` - HTTP 요청 메트릭

#### Health Check

```bash
# 전체 헬스 체크
curl http://localhost:8080/actuator/health | jq .

# Liveness probe
curl http://localhost:8080/actuator/health/liveness

# Readiness probe
curl http://localhost:8080/actuator/health/readiness
```

### Docker Compose 고급 사용법

#### 환경별 설정 파일

```bash
# 개발 환경
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# 프로덕션 환경
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

#### 스케일링

```bash
# 애플리케이션 컨테이너 3개 실행
docker-compose up -d --scale app=3

# 로드 밸런서 필요 (별도 설정)
```

### 문제 해결 (Docker)

#### 컨테이너가 시작되지 않음

```bash
# 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs app

# 컨테이너 내부 접속
docker-compose exec app sh
```

#### 포트 충돌

```bash
# 포트 8080이 이미 사용 중인 경우
# docker-compose.yml 수정:
ports:
  - "8081:8080"  # 호스트 포트 변경
```

#### 데이터베이스 연결 오류

```bash
# PostgreSQL 상태 확인
docker-compose logs postgres

# PostgreSQL 재시작
docker-compose restart postgres

# 연결 테스트
docker-compose exec app sh
nc -zv postgres 5432
```

#### 이미지 빌드 실패

```bash
# 빌드 캐시 삭제
docker builder prune

# 전체 재빌드
docker-compose build --no-cache --pull
```

---

## 6. 문제 해결

### 문제 1: "더미 피드백"만 반환됨

**증상**: 답변 제출 후 실제 AI 평가가 아닌 더미 데이터가 표시됨

**원인**: OpenAI API 키가 로드되지 않음

**해결**:
1. `.env` 파일 존재 확인:
   ```bash
   cat .env
   ```

2. 환경변수 로드 확인:
   ```bash
   export $(cat .env | grep -v '^#' | xargs)
   echo $OPENAI_API_KEY
   ```

3. IntelliJ인 경우 Run Configuration에 환경변수 추가

4. 애플리케이션 재시작

### 문제 2: "401 Unauthorized" 오류

**증상**: 로그에 `OpenAI API 호출 실패 (401)` 표시

**원인**: 잘못된 API 키 또는 만료된 키

**해결**:
1. API 키 형식 확인 (`sk-proj-...` 또는 `sk-...`로 시작)
2. OpenAI 대시보드에서 키 활성 상태 확인
3. 필요시 새 키 생성 및 교체

### 문제 3: Rate Limit 초과

**증상**: "요청 한도를 초과했습니다" 메시지

**원인**: 1시간 내 33회 이상 요청

**해결**:
1. 1시간 대기
2. 또는 `RateLimitService.kt`에서 `MAX_REQUESTS_PER_HOUR` 값 증가 (개발 환경만)

### 문제 4: 빌드 오류

**증상**: `./gradlew build` 실패

**해결**:
```bash
# 캐시 정리
./gradlew clean

# 의존성 다시 다운로드
./gradlew build --refresh-dependencies

# Java 버전 확인
java -version  # 21 이상이어야 함
```

### 문제 5: Flyway 마이그레이션 오류

**증상**: `FlywayException` 발생

**해결**:
```bash
# H2 데이터베이스 재시작 (메모리 DB라 애플리케이션 재시작으로 초기화됨)
# 프로덕션 환경인 경우:
./gradlew flywayClean  # ⚠️ 주의: 모든 데이터 삭제
./gradlew flywayMigrate
```

---

## 6. 개발 팁

### 로그 레벨 조정

**application.properties**에 추가:
```properties
# 디버깅용
logging.level.com.hojun.interviewnote=DEBUG
logging.level.org.springframework.web=DEBUG

# OpenAI 호출 로그
logging.level.com.hojun.interviewnote.interviewnoteapi.service.ai=DEBUG
```

### Hot Reload 활성화

**build.gradle.kts**에 추가:
```kotlin
dependencies {
    developmentOnly("org.springframework.boot:spring-boot-devtools")
}
```

### 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 특정 테스트만
./gradlew test --tests "*AiFeedbackServiceTest"

# 실제 OpenAI API 테스트 (비용 발생)
export $(cat .env | grep -v '^#' | xargs)
./gradlew test --tests "*Phase2EManualTest"
```

---

## 7. 프로덕션 배포 가이드

### Docker를 사용한 프로덕션 배포

#### 1. 환경변수 설정

**프로덕션 서버에서**:
```bash
# .env 파일 생성 (민감 정보는 Secret Manager 사용 권장)
cat > .env << EOF
OPENAI_API_KEY=sk-proj-production-key-here
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/interviewdb
SPRING_DATASOURCE_USERNAME=interviewuser
SPRING_DATASOURCE_PASSWORD=strong-password-here
EOF

chmod 600 .env  # 파일 권한 제한
```

#### 2. Docker Compose로 배포

```bash
# 이미지 빌드
docker-compose build

# 프로덕션 모드로 실행
docker-compose up -d

# 헬스 체크
curl http://localhost:8080/actuator/health
```

#### 3. 리버스 프록시 설정 (Nginx)

**nginx.conf 예시**:
```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Actuator 엔드포인트 보호
    location /actuator {
        deny all;
        return 403;
    }
}
```

#### 4. SSL/TLS 설정 (Let's Encrypt)

```bash
# Certbot 설치
sudo apt-get install certbot python3-certbot-nginx

# SSL 인증서 발급
sudo certbot --nginx -d your-domain.com

# 자동 갱신 설정
sudo certbot renew --dry-run
```

### AWS ECS 배포

```bash
# ECR에 이미지 푸시
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin <account-id>.dkr.ecr.ap-northeast-2.amazonaws.com

docker tag interview-note-api:latest \
  <account-id>.dkr.ecr.ap-northeast-2.amazonaws.com/interview-note-api:latest

docker push \
  <account-id>.dkr.ecr.ap-northeast-2.amazonaws.com/interview-note-api:latest
```

### 모니터링 설정

#### Prometheus + Grafana

**docker-compose.monitoring.yml**:
```yaml
version: '3.8'
services:
  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    ports:
      - "9090:9090"

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
```

**prometheus.yml**:
```yaml
scrape_configs:
  - job_name: 'interview-note-api'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['app:8080']
```

### 배포 전 체크리스트

#### 보안
- [ ] `.env` 파일이 `.gitignore`에 포함되어 있음
- [ ] 프로덕션 API 키 사용 (개발 키와 분리)
- [ ] 데이터베이스 비밀번호 강력하게 설정
- [ ] Actuator 엔드포인트 접근 제한
- [ ] HTTPS 인증서 구성
- [ ] 보안 헤더 설정 (CSP, HSTS 등)

#### 데이터베이스
- [ ] PostgreSQL 설치 및 설정
- [ ] 데이터베이스 백업 자동화
- [ ] Flyway 마이그레이션 테스트
- [ ] Connection Pool 최적화

#### 애플리케이션
- [ ] 프로덕션 프로필 활성화 (`SPRING_PROFILES_ACTIVE=prod`)
- [ ] 로그 레벨 조정 (INFO 이상)
- [ ] Rate Limit 설정 확인
- [ ] OpenAI API 예산 알림 설정
- [ ] 타임존 설정 (Asia/Seoul)

#### 모니터링
- [ ] Prometheus 메트릭 수집 설정
- [ ] Grafana 대시보드 구성
- [ ] 로그 집계 설정 (ELK Stack 등)
- [ ] 알림 설정 (오류, 과부하 등)

#### Docker
- [ ] 이미지 크기 최적화 확인 (<200MB)
- [ ] Health check 정상 동작 확인
- [ ] 재시작 정책 설정 (`restart: unless-stopped`)
- [ ] 볼륨 백업 계획 수립

### 성능 최적화

#### JVM 튜닝

**Dockerfile에서**:
```dockerfile
ENTRYPOINT ["java", \
    "-Xms512m", \
    "-Xmx1024m", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-jar", \
    "app.jar"]
```

#### Connection Pool

**application-prod.properties**:
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=30000
```

---

## 추가 자료

- **[README.md](./README.md)** - 프로젝트 개요
- **[CLAUDE.md](./CLAUDE.md)** - 개발 가이드
- **[phase2_implementation_plan.md](./phase2_implementation_plan.md)** - AI 연동 설계
- **[phase3_implementation_plan.md](./phase3_implementation_plan.md)** - 프로덕션 준비 계획

---

**문제가 해결되지 않나요?** GitHub Issues에 질문을 남겨주세요.
