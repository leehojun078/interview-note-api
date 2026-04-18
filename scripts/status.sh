#!/bin/bash
# 전체 시스템 상태 확인 스크립트
# 사용법: bash scripts/status.sh

set -e

echo "=========================================="
echo "Interview Note API 시스템 상태"
echo "=========================================="
echo ""

# 시간
echo "현재 시간: $(date)"
echo ""

# 컨테이너 상태
echo "=== Docker 컨테이너 상태 ==="
docker ps -a --filter name=interview --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""

# 리소스 사용량
echo "=== 리소스 사용량 ==="
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}" interview-note-api interview-postgres 2>/dev/null || echo "컨테이너가 실행 중이지 않습니다"
echo ""

# 시스템 메모리
echo "=== 시스템 메모리 ==="
free -h
echo ""

# 디스크 공간
echo "=== 디스크 공간 ==="
df -h / | grep -E "Filesystem|/$"
echo ""

# 헬스체크
echo "=== 애플리케이션 헬스체크 ==="
if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
    curl -s http://localhost:8080/actuator/health | jq . 2>/dev/null || curl -s http://localhost:8080/actuator/health
else
    echo "❌ 헬스체크 실패 (애플리케이션이 시작되지 않았거나 응답하지 않음)"
fi
echo ""

# 최근 에러 로그
echo "=== 최근 에러 로그 (10줄) ==="
docker logs --tail=100 interview-note-api 2>&1 | grep -i error | tail -10 || echo "에러 없음"
echo ""

# Docker 이미지
echo "=== Docker 이미지 ==="
docker images interview-note-api --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
echo ""

# Nginx 상태 (EC2에서만)
if command -v systemctl &> /dev/null; then
    echo "=== Nginx 상태 ==="
    sudo systemctl status nginx --no-pager -l | head -10 || echo "Nginx가 설치되지 않았거나 실행 중이지 않습니다"
    echo ""
fi

# PostgreSQL 연결 테스트
echo "=== PostgreSQL 연결 테스트 ==="
if docker ps | grep -q interview-postgres; then
    docker exec interview-postgres psql -U interviewuser -d interviewdb -c "SELECT COUNT(*) as question_count FROM questions;" 2>/dev/null || echo "PostgreSQL 연결 실패"
else
    echo "PostgreSQL 컨테이너가 실행 중이지 않습니다"
fi
echo ""

echo "=========================================="
echo "상태 확인 완료"
echo "=========================================="
echo ""
echo "자세한 로그: bash scripts/logs.sh"
echo "재시작:     bash scripts/restart.sh"
echo "전체 재배포: bash scripts/deploy.sh"
echo ""
