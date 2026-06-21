#!/bin/bash
# 전체 재배포 스크립트
# 사용법: bash scripts/deploy.sh

set -e

echo "=========================================="
echo "Interview Note API 전체 재배포 시작"
echo "=========================================="

# 프로젝트 디렉토리
PROJECT_DIR="$HOME/apps/interview-note-api"
cd "$PROJECT_DIR"

# Step 1: Git Pull
echo ""
echo "[1/8] 최신 코드 가져오기..."
git pull origin main

# Step 2: 환경변수 로드
echo ""
echo "[2/8] 환경변수 로드..."
if [ ! -f .env ]; then
    echo "❌ 에러: .env 파일이 없습니다!"
    echo "먼저 .env 파일을 생성하세요:"
    echo "  cp .env.example .env"
    echo "  vim .env  # OpenAI API 키 입력"
    exit 1
fi

set -a
source <(grep -v '^#' .env | sed 's/\r$//')
set +a

if [ -z "$OPENAI_API_KEY" ]; then
    echo "❌ 에러: OPENAI_API_KEY가 로드되지 않았습니다!"
    echo ".env 파일을 확인하세요."
    exit 1
fi

echo "✅ OPENAI_API_KEY: ${OPENAI_API_KEY:0:20}..."

# Step 3: 데이터베이스 백업
echo ""
echo "[3/8] 데이터베이스 백업 중..."
if [ -f "$HOME/backup-postgres.sh" ]; then
    bash "$HOME/backup-postgres.sh"
else
    echo "⚠️  백업 스크립트가 없습니다. 건너뜁니다."
fi

# Step 4: 기존 애플리케이션 컨테이너 중지 및 삭제
echo ""
echo "[4/8] 기존 컨테이너 중지 및 삭제..."
if docker ps -a | grep -q interview-note-api; then
    docker stop interview-note-api || true
    docker rm interview-note-api || true
    echo "✅ 기존 컨테이너 삭제 완료"
else
    echo "✅ 기존 컨테이너 없음"
fi

# Step 5: PostgreSQL 확인 (없으면 시작)
echo ""
echo "[5/8] PostgreSQL 확인..."
if ! docker ps | grep -q interview-postgres; then
    echo "PostgreSQL이 실행 중이지 않습니다. 시작합니다..."
    docker-compose up -d postgres
    echo "PostgreSQL 초기화 대기 중 (15초)..."
    sleep 15
    echo "✅ PostgreSQL 시작 완료"
else
    echo "✅ PostgreSQL 이미 실행 중"
fi

# Step 6: Docker 이미지 빌드
echo ""
echo "[6/8] Docker 이미지 빌드 중..."
echo "⏱️  예상 소요 시간: t3.micro 8-12분, t3.small 5-8분"
docker build -t interview-note-api:latest .
echo "✅ 빌드 완료"

# Step 7: 새 컨테이너 시작
echo ""
echo "[7/8] 새 컨테이너 시작..."
docker run -d \
  --name interview-note-api \
  --network interview-note-api_interview-network \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e OPENAI_API_KEY="${OPENAI_API_KEY}" \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/interviewdb \
  -e SPRING_DATASOURCE_USERNAME=interviewuser \
  -e SPRING_DATASOURCE_PASSWORD=interviewpass \
  --restart unless-stopped \
  interview-note-api:latest

echo "✅ 컨테이너 시작 완료"

# Step 8: 헬스체크
echo ""
echo "[8/8] 애플리케이션 시작 대기 중..."
echo "Spring Boot 애플리케이션이 시작될 때까지 기다립니다..."
echo "(로그를 보려면 Ctrl+C를 누르세요. 컨테이너는 계속 실행됩니다.)"

# 최대 120초 대기
MAX_WAIT=120
WAITED=0

while [ $WAITED -lt $MAX_WAIT ]; do
    if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo ""
        echo "✅ 애플리케이션이 정상적으로 시작되었습니다!"
        break
    fi

    echo -n "."
    sleep 5
    WAITED=$((WAITED + 5))
done

if [ $WAITED -ge $MAX_WAIT ]; then
    echo ""
    echo "⚠️  타임아웃: 애플리케이션이 ${MAX_WAIT}초 내에 시작되지 않았습니다."
    echo "로그를 확인하세요:"
    echo "  docker logs interview-note-api"
    exit 1
fi

# 최종 상태 확인
echo ""
echo "=========================================="
echo "배포 완료!"
echo "=========================================="
echo ""
echo "컨테이너 상태:"
docker ps --filter name=interview

echo ""
echo "헬스체크:"
curl -s http://localhost:8080/actuator/health | jq . || curl -s http://localhost:8080/actuator/health

echo ""
echo "접속 URL:"
echo "  - 내부: http://localhost:8080"
echo "  - 외부: http://YOUR_ELASTIC_IP"
echo ""
echo "로그 확인: docker logs -f interview-note-api"
echo "상태 확인: docker ps"
echo ""
