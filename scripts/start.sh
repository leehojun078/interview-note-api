#!/bin/bash
# 컨테이너 시작 스크립트 (중지된 컨테이너 재시작)
# 사용법: bash scripts/start.sh [all]

set -e

echo "=========================================="
echo "컨테이너 시작"
echo "=========================================="

# all 인자가 있으면 PostgreSQL도 시작
START_ALL=false
if [ "$1" == "all" ]; then
    START_ALL=true
    echo "모드: 전체 시작 (PostgreSQL + 애플리케이션)"
else
    echo "모드: 애플리케이션만 시작"
    echo "전체 시작하려면: bash scripts/start.sh all"
fi

echo ""

# PostgreSQL 시작 (선택사항)
if [ "$START_ALL" = true ]; then
    if docker ps -a | grep -q interview-postgres; then
        if ! docker ps | grep -q interview-postgres; then
            echo "PostgreSQL 컨테이너 시작 중..."
            docker start interview-postgres
            echo "PostgreSQL 초기화 대기 중 (10초)..."
            sleep 10
            echo "✅ interview-postgres 시작 완료"
        else
            echo "✅ interview-postgres 이미 실행 중"
        fi
    else
        echo "⚠️  interview-postgres 컨테이너가 존재하지 않습니다"
        echo "PostgreSQL을 새로 생성하려면:"
        echo "  cd ~/apps/interview-note-api"
        echo "  docker-compose up -d postgres"
    fi
    echo ""
fi

# 애플리케이션 시작
if docker ps -a | grep -q interview-note-api; then
    if ! docker ps | grep -q interview-note-api; then
        echo "애플리케이션 컨테이너 시작 중..."
        docker start interview-note-api
        echo "✅ interview-note-api 시작 완료"

        # 간단한 헬스체크
        echo ""
        echo "애플리케이션 시작 대기 중 (최대 60초)..."
        MAX_WAIT=60
        WAITED=0

        while [ $WAITED -lt $MAX_WAIT ]; do
            if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
                echo ""
                echo "✅ 애플리케이션이 정상적으로 시작되었습니다!"
                break
            fi

            echo -n "."
            sleep 3
            WAITED=$((WAITED + 3))
        done

        if [ $WAITED -ge $MAX_WAIT ]; then
            echo ""
            echo "⚠️  타임아웃: 로그를 확인하세요"
            echo "  docker logs interview-note-api"
        fi
    else
        echo "✅ interview-note-api 이미 실행 중"
    fi
else
    echo "❌ interview-note-api 컨테이너가 존재하지 않습니다"
    echo "컨테이너를 새로 생성하려면:"
    echo "  bash scripts/restart.sh"
    exit 1
fi

echo ""
echo "=========================================="
echo "시작 완료"
echo "=========================================="
echo ""
docker ps --filter name=interview
echo ""
echo "로그 확인: docker logs -f interview-note-api"
echo ""
