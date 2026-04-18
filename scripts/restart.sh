#!/bin/bash
# 빠른 재시작 스크립트 (기존 이미지 사용, 빌드 안 함)
# 사용법: bash scripts/restart.sh

set -e

echo "=========================================="
echo "Interview Note API 빠른 재시작"
echo "=========================================="

# 프로젝트 디렉토리
PROJECT_DIR="$HOME/apps/interview-note-api"
cd "$PROJECT_DIR"

# 환경변수 로드
echo ""
echo "[1/4] 환경변수 로드..."
if [ ! -f .env ]; then
    echo "❌ 에러: .env 파일이 없습니다!"
    exit 1
fi

set -a
source <(grep -v '^#' .env | sed 's/\r$//')
set +a

if [ -z "$OPENAI_API_KEY" ]; then
    echo "❌ 에러: OPENAI_API_KEY가 로드되지 않았습니다!"
    exit 1
fi

echo "✅ OPENAI_API_KEY: ${OPENAI_API_KEY:0:20}..."

# 기존 컨테이너 중지 및 삭제
echo ""
echo "[2/4] 기존 컨테이너 중지 및 삭제..."
if docker ps -a | grep -q interview-note-api; then
    docker stop interview-note-api || true
    docker rm interview-note-api || true
    echo "✅ 기존 컨테이너 삭제 완료"
else
    echo "✅ 기존 컨테이너 없음"
fi

# PostgreSQL 확인
echo ""
echo "[3/4] PostgreSQL 확인..."
if ! docker ps | grep -q interview-postgres; then
    echo "PostgreSQL이 실행 중이지 않습니다. 시작합니다..."
    docker-compose up -d postgres
    echo "PostgreSQL 초기화 대기 중 (15초)..."
    sleep 15
else
    echo "✅ PostgreSQL 실행 중"
fi

# 새 컨테이너 시작 (기존 이미지 사용)
echo ""
echo "[4/4] 컨테이너 시작 (기존 이미지 사용)..."
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

# 간단한 헬스체크
echo ""
echo "애플리케이션 시작 대기 중 (최대 60초)..."
MAX_WAIT=60
WAITED=0

while [ $WAITED -lt $MAX_WAIT ]; do
    if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo ""
        echo "✅ 애플리케이션 재시작 완료!"
        break
    fi

    echo -n "."
    sleep 3
    WAITED=$((WAITED + 3))
done

echo ""
echo "=========================================="
echo "재시작 완료!"
echo "=========================================="
echo ""
docker ps --filter name=interview
echo ""
echo "로그 확인: docker logs -f interview-note-api"
echo ""
