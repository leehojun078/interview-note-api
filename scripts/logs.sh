#!/bin/bash
# 로그 확인 스크립트
# 사용법: bash scripts/logs.sh [app|postgres|nginx|all] [follow]

set -e

TARGET="${1:-app}"
FOLLOW="${2:-}"

echo "=========================================="
echo "로그 확인: $TARGET"
echo "=========================================="
echo ""

case "$TARGET" in
    app|application)
        if [ "$FOLLOW" == "follow" ] || [ "$FOLLOW" == "f" ]; then
            echo "실시간 로그 (Ctrl+C로 종료):"
            docker logs -f interview-note-api
        else
            echo "최근 로그 100줄:"
            docker logs --tail=100 interview-note-api
        fi
        ;;

    postgres|db)
        if [ "$FOLLOW" == "follow" ] || [ "$FOLLOW" == "f" ]; then
            echo "실시간 로그 (Ctrl+C로 종료):"
            docker logs -f interview-postgres
        else
            echo "최근 로그 100줄:"
            docker logs --tail=100 interview-postgres
        fi
        ;;

    nginx)
        if [ "$FOLLOW" == "follow" ] || [ "$FOLLOW" == "f" ]; then
            echo "실시간 접속 로그 (Ctrl+C로 종료):"
            sudo tail -f /var/log/nginx/interview-note-access.log
        else
            echo "최근 접속 로그 50줄:"
            sudo tail -50 /var/log/nginx/interview-note-access.log
        fi
        ;;

    nginx-error)
        if [ "$FOLLOW" == "follow" ] || [ "$FOLLOW" == "f" ]; then
            echo "실시간 에러 로그 (Ctrl+C로 종료):"
            sudo tail -f /var/log/nginx/interview-note-error.log
        else
            echo "최근 에러 로그 50줄:"
            sudo tail -50 /var/log/nginx/interview-note-error.log
        fi
        ;;

    all)
        echo "=== 애플리케이션 로그 (최근 50줄) ==="
        docker logs --tail=50 interview-note-api
        echo ""
        echo "=== PostgreSQL 로그 (최근 20줄) ==="
        docker logs --tail=20 interview-postgres
        echo ""
        echo "=== Nginx 접속 로그 (최근 20줄) ==="
        sudo tail -20 /var/log/nginx/interview-note-access.log 2>/dev/null || echo "Nginx 로그 없음"
        ;;

    error|errors)
        echo "=== 애플리케이션 에러 로그 ==="
        docker logs interview-note-api 2>&1 | grep -i error | tail -50
        echo ""
        echo "=== PostgreSQL 에러 로그 ==="
        docker logs interview-postgres 2>&1 | grep -i error | tail -20
        echo ""
        echo "=== Nginx 에러 로그 ==="
        sudo tail -20 /var/log/nginx/interview-note-error.log 2>/dev/null || echo "Nginx 에러 로그 없음"
        ;;

    *)
        echo "사용법: bash scripts/logs.sh [대상] [follow]"
        echo ""
        echo "대상:"
        echo "  app, application  - 애플리케이션 로그 (기본값)"
        echo "  postgres, db      - PostgreSQL 로그"
        echo "  nginx             - Nginx 접속 로그"
        echo "  nginx-error       - Nginx 에러 로그"
        echo "  all               - 모든 로그 요약"
        echo "  error, errors     - 에러만 필터링"
        echo ""
        echo "옵션:"
        echo "  follow, f         - 실시간 로그 (tail -f)"
        echo ""
        echo "예시:"
        echo "  bash scripts/logs.sh app          # 애플리케이션 로그 100줄"
        echo "  bash scripts/logs.sh app follow   # 애플리케이션 실시간 로그"
        echo "  bash scripts/logs.sh postgres f   # PostgreSQL 실시간 로그"
        echo "  bash scripts/logs.sh error        # 모든 에러 로그"
        echo ""
        exit 1
        ;;
esac

echo ""
