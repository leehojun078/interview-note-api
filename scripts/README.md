# Interview Note API 관리 스크립트

이 디렉토리에는 EC2에서 애플리케이션을 관리하기 위한 편의 스크립트들이 포함되어 있습니다.

## 📋 스크립트 목록

### 1. `setup-env-auto-load.sh` - 환경변수 자동 로드 설정

**문제**: EC2 재시작 시 `OPENAI_API_KEY` 환경변수가 사라짐

**해결**: `~/.bashrc`에 자동 로드 스크립트를 추가하여 영구적으로 환경변수 유지

**사용법**:
```bash
# EC2에서 최초 1회만 실행
cd ~/apps/interview-note-api
bash scripts/setup-env-auto-load.sh

# 현재 세션에 적용
source ~/.bashrc

# 또는 터미널 재접속
exit
ssh -i your-key.pem ec2-user@YOUR_IP
```

**효과**:
- EC2 재시작 후에도 환경변수 자동 로드
- `export` 명령 수동 입력 불필요

---

### 2. `deploy.sh` - 전체 재배포

**기능**: Git pull + Docker 빌드 + 컨테이너 재시작 전체 과정 자동화

**사용법**:
```bash
cd ~/apps/interview-note-api
bash scripts/deploy.sh
```

**수행 작업**:
1. 최신 코드 가져오기 (`git pull`)
2. 환경변수 로드
3. 데이터베이스 자동 백업
4. 기존 컨테이너 중지 및 삭제
5. PostgreSQL 확인/시작
6. Docker 이미지 재빌드
7. 새 컨테이너 시작
8. 헬스체크 및 상태 확인

**예상 소요 시간**:
- t3.micro: 8-12분
- t3.small: 5-8분

---

### 3. `restart.sh` - 빠른 재시작

**기능**: 빌드 없이 기존 이미지로 컨테이너만 재시작 (설정 변경 시 유용)

**사용법**:
```bash
bash scripts/restart.sh
```

**수행 작업**:
1. 환경변수 로드
2. 기존 컨테이너 중지 및 삭제
3. PostgreSQL 확인
4. 새 컨테이너 시작 (기존 이미지 사용)
5. 헬스체크

**예상 소요 시간**: 30초 ~ 1분

**사용 시나리오**:
- `.env` 파일 수정 후 재시작
- 메모리 부족으로 컨테이너가 죽었을 때
- 설정 변경 후 빠른 재시작

---

### 4. `stop.sh` - 컨테이너 중지

**기능**: 애플리케이션 또는 전체 컨테이너 중지

**사용법**:
```bash
# 애플리케이션만 중지 (PostgreSQL은 유지)
bash scripts/stop.sh

# 전체 중지 (PostgreSQL 포함)
bash scripts/stop.sh all
```

**사용 시나리오**:
- 유지보수 작업 전
- 리소스 절약 (밤에 중지)
- 데이터베이스 백업 전 안전한 중지

---

### 5. `start.sh` - 컨테이너 시작

**기능**: 중지된 컨테이너 재시작

**사용법**:
```bash
# 애플리케이션만 시작
bash scripts/start.sh

# 전체 시작 (PostgreSQL 포함)
bash scripts/start.sh all
```

**참고**:
- 컨테이너가 삭제되었다면 `restart.sh` 사용
- 기존 컨테이너를 그대로 시작만 함

---

### 6. `logs.sh` - 로그 확인

**기능**: 다양한 로그를 쉽게 확인

**사용법**:
```bash
# 애플리케이션 로그 (최근 100줄)
bash scripts/logs.sh app

# 애플리케이션 실시간 로그
bash scripts/logs.sh app follow

# PostgreSQL 로그
bash scripts/logs.sh postgres

# Nginx 접속 로그
bash scripts/logs.sh nginx

# 모든 에러 로그
bash scripts/logs.sh error

# 전체 로그 요약
bash scripts/logs.sh all
```

**로그 대상**:
- `app`, `application` - 애플리케이션 로그
- `postgres`, `db` - PostgreSQL 로그
- `nginx` - Nginx 접속 로그
- `nginx-error` - Nginx 에러 로그
- `error`, `errors` - 모든 에러 필터링
- `all` - 전체 요약

**옵션**:
- `follow`, `f` - 실시간 로그 (`tail -f`)

---

### 7. `status.sh` - 전체 상태 확인

**기능**: 시스템 전반의 상태를 한눈에 확인

**사용법**:
```bash
bash scripts/status.sh
```

**확인 항목**:
- Docker 컨테이너 상태
- 리소스 사용량 (CPU, 메모리)
- 시스템 메모리
- 디스크 공간
- 애플리케이션 헬스체크
- 최근 에러 로그
- Docker 이미지 정보
- Nginx 상태 (EC2만)
- PostgreSQL 연결 테스트

---

## 🚀 일반적인 사용 시나리오

### 최초 EC2 설정 (1회만)

```bash
# 1. 저장소 클론
cd ~/apps
git clone https://github.com/YOUR_USERNAME/interview-note-api.git
cd interview-note-api

# 2. .env 파일 생성 및 편집
cp .env.example .env
vim .env  # OpenAI API 키 입력

# 3. 환경변수 자동 로드 설정
bash scripts/setup-env-auto-load.sh
source ~/.bashrc

# 4. 최초 배포
bash scripts/deploy.sh
```

### 코드 업데이트 후 재배포

```bash
# 전체 재배포 (빌드 포함)
bash scripts/deploy.sh
```

### 설정 변경 후 빠른 재시작

```bash
# .env 파일 수정
vim .env

# 빠른 재시작 (빌드 안 함)
bash scripts/restart.sh
```

### 문제 해결

```bash
# 1. 상태 확인
bash scripts/status.sh

# 2. 로그 확인
bash scripts/logs.sh error

# 3. 애플리케이션 재시작
bash scripts/restart.sh

# 4. 그래도 안 되면 전체 재배포
bash scripts/deploy.sh
```

### 야간 유지보수

```bash
# 백업
~/backup-postgres.sh

# 중지 (전체)
bash scripts/stop.sh all

# ... 유지보수 작업 ...

# 시작 (전체)
bash scripts/start.sh all

# 상태 확인
bash scripts/status.sh
```

---

## 💡 팁

### 스크립트 권한 설정

모든 스크립트를 실행 가능하도록 설정:

```bash
chmod +x scripts/*.sh
```

그러면 `bash` 없이 바로 실행 가능:

```bash
./scripts/deploy.sh
./scripts/status.sh
```

### 별칭(Alias) 설정

자주 사용하는 명령어는 `~/.bashrc`에 별칭 추가:

```bash
# ~/.bashrc에 추가
alias app-deploy="cd ~/apps/interview-note-api && bash scripts/deploy.sh"
alias app-restart="cd ~/apps/interview-note-api && bash scripts/restart.sh"
alias app-status="cd ~/apps/interview-note-api && bash scripts/status.sh"
alias app-logs="cd ~/apps/interview-note-api && bash scripts/logs.sh app follow"
```

적용:
```bash
source ~/.bashrc
```

사용:
```bash
app-status
app-logs
app-restart
```

### 크론탭 활용

매일 새벽 2시 데이터베이스 백업 (이미 설정되어 있다면 생략):

```bash
crontab -e
```

추가:
```
0 2 * * * /home/ec2-user/backup-postgres.sh >> /var/log/postgres-backup.log 2>&1
```

---

## ⚠️ 주의사항

### `deploy.sh` vs `restart.sh`

| 상황 | 사용 스크립트 | 이유 |
|------|-------------|------|
| 코드 변경 (Git pull) | `deploy.sh` | 빌드 필요 |
| `.env` 수정 | `restart.sh` | 빌드 불필요 |
| 의존성 변경 (build.gradle.kts) | `deploy.sh` | 빌드 필요 |
| 컨테이너 재시작만 | `restart.sh` | 빠름 |

### 환경변수 로드 확인

스크립트가 환경변수를 제대로 로드하는지 확인:

```bash
# 환경변수 확인
echo $OPENAI_API_KEY

# 출력: sk-proj-...

# 없다면 다시 로드
source ~/.bashrc
```

### PostgreSQL 데이터 손실 방지

- `deploy.sh`는 자동으로 백업하지만, 수동 재배포 전에도 백업 권장:
  ```bash
  ~/backup-postgres.sh
  bash scripts/deploy.sh
  ```

---

## 🔧 트러블슈팅

### 문제 1: "Permission denied" 에러

**원인**: 스크립트에 실행 권한이 없음

**해결**:
```bash
chmod +x scripts/*.sh
```

### 문제 2: 환경변수가 로드되지 않음

**원인**: `.env` 파일이 없거나 잘못된 위치

**해결**:
```bash
# .env 파일 확인
cat ~/apps/interview-note-api/.env

# 없다면 생성
cd ~/apps/interview-note-api
cp .env.example .env
vim .env  # API 키 입력

# 환경변수 다시 로드
source ~/.bashrc
```

### 문제 3: Docker 빌드 실패

**원인**: 메모리 부족 (t3.micro)

**해결**:
```bash
# Swap 메모리 추가
sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# 재시도
bash scripts/deploy.sh
```

### 문제 4: PostgreSQL 연결 실패

**원인**: PostgreSQL 컨테이너가 시작되지 않음

**해결**:
```bash
# PostgreSQL 상태 확인
docker ps -a | grep postgres

# 강제 재시작
docker stop interview-postgres || true
docker rm interview-postgres || true

cd ~/apps/interview-note-api
docker-compose up -d postgres

# 확인
docker logs interview-postgres
```

---

## 📞 지원

문제가 해결되지 않으면:

1. 상태 확인: `bash scripts/status.sh`
2. 로그 확인: `bash scripts/logs.sh error`
3. GitHub Issues에 문의

---

**문서 버전**: 1.0.0
**최종 수정**: 2026-04-18
