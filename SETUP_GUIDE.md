# 환경 설정 가이드

이 문서는 면접 리뷰 웹 애플리케이션의 개발 환경 설정 방법을 상세히 설명합니다.

## 📋 목차

1. [필수 소프트웨어 설치](#1-필수-소프트웨어-설치)
2. [OpenAI API 키 설정](#2-openai-api-키-설정)
3. [개발 환경별 실행 방법](#3-개발-환경별-실행-방법)
4. [데이터베이스 설정](#4-데이터베이스-설정)
5. [문제 해결](#5-문제-해결)

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

## 5. 문제 해결

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

## 7. 배포 전 체크리스트

- [ ] `.env` 파일이 `.gitignore`에 포함되어 있음
- [ ] 프로덕션 환경변수 설정 (서버/컨테이너)
- [ ] PostgreSQL 데이터베이스 준비
- [ ] Flyway 마이그레이션 테스트
- [ ] Rate Limit 설정 확인
- [ ] OpenAI API 예산 알림 설정
- [ ] 로그 레벨 조정 (INFO 이상)
- [ ] 보안 헤더 설정
- [ ] HTTPS 인증서 구성

---

## 추가 자료

- [README.md](./README.md) - 프로젝트 개요
- [CLAUDE.md](./CLAUDE.md) - 개발 가이드
- [phase2_implementation_plan.md](./phase2_implementation_plan.md) - AI 연동 설계

---

**문제가 해결되지 않나요?** GitHub Issues에 질문을 남겨주세요.
