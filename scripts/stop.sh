#!/bin/bash
# 컨테이너 중지 스크립트
# 사용법: bash scripts/stop.sh [all]

set -e

echo "=========================================="
echo "컨테이너 중지"
echo "=========================================="

# all 인자가 있으면 PostgreSQL도 중지
STOP_ALL=false
if [ "$1" == "all" ]; then
    STOP_ALL=true
    echo "모드: 전체 중지 (애플리케이션 + PostgreSQL)"
else
    echo "모드: 애플리케이션만 중지 (PostgreSQL은 유지)"
    echo "전체 중지하려면: bash scripts/stop.sh all"
fi

echo ""

# 애플리케이션 중지
if docker ps | grep -q interview-note-api; then
    echo "애플리케이션 컨테이너 중지 중..."
    docker stop interview-note-api
    echo "✅ interview-note-api 중지 완료"
else
    echo "✅ interview-note-api가 이미 중지되어 있습니다"
fi

# PostgreSQL 중지 (선택사항)
if [ "$STOP_ALL" = true ]; then
    if docker ps | grep -q interview-postgres; then
        echo ""
        echo "PostgreSQL 컨테이너 중지 중..."
        docker stop interview-postgres
        echo "✅ interview-postgres 중지 완료"
    else
        echo "✅ interview-postgres가 이미 중지되어 있습니다"
    fi
fi

echo ""
echo "=========================================="
echo "중지 완료"
echo "=========================================="
echo ""
docker ps -a --filter name=interview
echo ""
echo "컨테이너 삭제: docker rm interview-note-api"
echo "컨테이너 시작: bash scripts/start.sh"
echo ""
