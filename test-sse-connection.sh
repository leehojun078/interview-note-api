#!/bin/bash

# SSE 연결 테스트 스크립트 (Amazon Linux 2023)
# EC2 서버에서 실행하여 SSE가 제대로 작동하는지 확인

echo "======================================"
echo "SSE 연결 테스트 시작"
echo "======================================"
echo ""

# 1. Nginx 상태 확인
echo "1. Nginx 상태 확인..."
if systemctl is-active --quiet nginx; then
    echo "✅ Nginx 실행 중"
else
    echo "❌ Nginx 실행되지 않음"
    echo "   sudo systemctl start nginx 실행 필요"
    exit 1
fi
echo ""

# 2. Nginx 설정 검증
echo "2. Nginx 설정 검증..."
if sudo nginx -t 2>&1 | grep -q "successful"; then
    echo "✅ Nginx 설정 정상"
else
    echo "❌ Nginx 설정 오류"
    sudo nginx -t
    exit 1
fi
echo ""

# 3. Spring Boot 애플리케이션 확인
echo "3. Spring Boot 애플리케이션 확인..."
if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ Spring Boot 실행 중"
else
    echo "❌ Spring Boot 실행되지 않음 (포트 8080)"
    echo "   애플리케이션 시작 필요"
    exit 1
fi
echo ""

# 4. SSE 엔드포인트 테스트 (10초 타임아웃)
echo "4. SSE 연결 테스트 (10초)..."
echo "   curl -N -H 'Accept: text/event-stream' --max-time 10 http://localhost/mock-interviews/1/stream"
echo ""

# SSE 요청 (타임아웃 10초)
response=$(curl -N -H "Accept: text/event-stream" --max-time 10 -s -w "\n%{http_code}" http://localhost/mock-interviews/1/stream 2>&1)
http_code=$(echo "$response" | tail -n 1)

if [ "$http_code" = "200" ] || [ "$http_code" = "28" ]; then
    # 28 = 타임아웃 (정상 - SSE는 연결 유지되다가 타임아웃)
    echo "✅ SSE 연결 성공 (HTTP $http_code)"
    echo "   연결이 유지되다가 타임아웃됨 (정상)"
else
    echo "❌ SSE 연결 실패 (HTTP $http_code)"
    echo "   응답: $response"
fi
echo ""

# 5. Nginx 에러 로그 확인
echo "5. Nginx 에러 로그 (최근 10줄)..."
echo "----------------------------------------"
sudo tail -10 /var/log/nginx/error.log
echo "----------------------------------------"
echo ""

# 6. 최종 결과
echo "======================================"
echo "테스트 완료"
echo "======================================"
echo ""
echo "📌 다음 단계:"
echo "1. 브라우저에서 AI 면접 연습 시작"
echo "2. F12 → Network 탭에서 /stream 요청 확인"
echo "3. 답변 전송 후 실시간 표시 확인"
echo ""
echo "문제 발생 시:"
echo "- sudo tail -f /var/log/nginx/error.log"
echo "- sudo journalctl -u your-app -f"
