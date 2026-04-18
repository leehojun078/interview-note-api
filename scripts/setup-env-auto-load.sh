#!/bin/bash
# EC2 환경변수 자동 로드 설정 스크립트
# EC2에서 실행: bash scripts/setup-env-auto-load.sh

set -e

echo "=========================================="
echo "환경변수 자동 로드 설정 시작"
echo "=========================================="

# 프로젝트 디렉토리 확인
PROJECT_DIR="$HOME/apps/interview-note-api"
ENV_FILE="$PROJECT_DIR/.env"

if [ ! -f "$ENV_FILE" ]; then
    echo "❌ 에러: .env 파일을 찾을 수 없습니다: $ENV_FILE"
    echo "먼저 EC2에 프로젝트를 클론하고 .env 파일을 생성하세요."
    exit 1
fi

echo "✅ .env 파일 발견: $ENV_FILE"
echo ""

# ~/.bashrc에 환경변수 로드 스크립트 추가
BASHRC="$HOME/.bashrc"

# 이미 추가되어 있는지 확인
if grep -q "# Auto-load interview-note-api environment variables" "$BASHRC" 2>/dev/null; then
    echo "⚠️  이미 .bashrc에 환경변수 로드 스크립트가 추가되어 있습니다."
    echo "기존 설정을 덮어쓰겠습니까? (y/N)"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        echo "설정을 취소합니다."
        exit 0
    fi

    # 기존 설정 제거
    sed -i '/# Auto-load interview-note-api environment variables/,/# End of interview-note-api env setup/d' "$BASHRC"
    echo "✅ 기존 설정 제거 완료"
fi

# 새 설정 추가
cat >> "$BASHRC" << 'EOF'

# Auto-load interview-note-api environment variables
if [ -f "$HOME/apps/interview-note-api/.env" ]; then
    # .env 파일에서 환경변수 로드 (주석 제외)
    set -a  # 모든 변수를 자동으로 export
    source <(grep -v '^#' "$HOME/apps/interview-note-api/.env" | sed 's/\r$//')
    set +a

    # 로드 확인 메시지 (처음 로그인 시에만 표시)
    if [ -z "$ENV_LOADED_NOTIFIED" ]; then
        echo "✅ Interview Note API 환경변수 로드됨"
        export ENV_LOADED_NOTIFIED=1
    fi
fi
# End of interview-note-api env setup
EOF

echo "✅ ~/.bashrc에 환경변수 자동 로드 스크립트 추가 완료"
echo ""

# 현재 세션에 적용
echo "현재 세션에 환경변수를 로드합니다..."
set -a
source <(grep -v '^#' "$ENV_FILE" | sed 's/\r$//')
set +a

# 확인
if [ -n "$OPENAI_API_KEY" ]; then
    echo "✅ OPENAI_API_KEY 로드 확인: ${OPENAI_API_KEY:0:20}..."
else
    echo "⚠️  경고: OPENAI_API_KEY가 로드되지 않았습니다."
    echo ".env 파일을 확인하세요: $ENV_FILE"
fi

echo ""
echo "=========================================="
echo "설정 완료!"
echo "=========================================="
echo ""
echo "다음부터는 EC2 재시작 후에도 환경변수가 자동으로 로드됩니다."
echo ""
echo "현재 터미널에 적용하려면 다음 명령을 실행하세요:"
echo "  source ~/.bashrc"
echo ""
echo "또는 터미널을 재접속하세요:"
echo "  exit"
echo "  ssh -i your-key.pem ec2-user@YOUR_IP"
echo ""
