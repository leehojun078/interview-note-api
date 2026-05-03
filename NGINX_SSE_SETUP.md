# Nginx SSE 설정 가이드 (Amazon Linux 2023)

## 📋 사전 확인

### 1. EC2 접속
```bash
ssh -i your-key.pem ec2-user@your-ec2-ip
```

### 2. Nginx 설치 확인
```bash
nginx -v
# nginx version: nginx/1.24.0 등이 출력되면 설치됨

# 설치 안 되어 있으면
sudo dnf install -y nginx
sudo systemctl enable nginx
sudo systemctl start nginx
```

### 3. 현재 설정 백업 (중요!)
```bash
# Amazon Linux 2023은 /etc/nginx/conf.d/ 디렉토리를 사용합니다
sudo cp /etc/nginx/conf.d/interview-note-api.conf /etc/nginx/conf.d/interview-note-api.conf.backup 2>/dev/null || echo "기존 설정 파일 없음"
sudo cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.backup
```

---

## 🔧 설정 적용

### 방법 1: 기존 설정 수정 (추천)

**참고**: Amazon Linux 2023은 `/etc/nginx/conf.d/` 디렉토리에서 설정을 관리합니다.
Ubuntu의 `sites-available`/`sites-enabled` 구조와 다릅니다.

#### 1) 설정 파일 열기
```bash
sudo vim /etc/nginx/conf.d/interview-note-api.conf
```

#### 2) SSE 전용 location 블록 추가

기존 `location /` 블록 **위에** 다음 추가:

```nginx
# SSE 전용 설정 (mock-interviews)
location /mock-interviews {
    proxy_pass http://localhost:8080;

    # SSE 필수 설정
    proxy_buffering off;
    proxy_cache off;
    proxy_http_version 1.1;
    proxy_set_header Connection '';
    chunked_transfer_encoding off;

    # 타임아웃
    proxy_read_timeout 3600s;
    proxy_send_timeout 3600s;
    proxy_connect_timeout 60s;

    # 헤더
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Accel-Buffering no;
}

# 일반 요청
location / {
    proxy_pass http://localhost:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

#### 3) 저장 및 닫기 (vim 에디터)
- `ESC` (명령 모드로 전환)
- `:wq` (저장하고 종료)
- `Enter`

---

### 방법 2: 완전 교체

#### 1) 로컬에서 작성한 설정 파일 업로드
```bash
# 로컬에서 실행
scp -i your-key.pem nginx-sse-config.conf ec2-user@your-ec2-ip:~/
```

#### 2) EC2에서 설정 파일 복사
```bash
# EC2에서 실행
sudo cp ~/nginx-sse-config.conf /etc/nginx/conf.d/interview-note-api.conf
```

---

## ✅ 설정 검증 및 적용

### 1. 설정 문법 검사
```bash
sudo nginx -t
# 출력: syntax is ok, test is successful
```

❌ 에러 발생 시:
```bash
# 설정 롤백
sudo cp /etc/nginx/conf.d/interview-note-api.conf.backup /etc/nginx/conf.d/interview-note-api.conf
sudo nginx -t
```

### 2. Nginx 재시작
```bash
sudo systemctl restart nginx

# 상태 확인
sudo systemctl status nginx
# Active: active (running) 확인
```

### 3. 프로세스 확인
```bash
ps aux | grep nginx
# nginx: master process, nginx: worker process 등이 보이면 정상
```

---

## 🧪 테스트

### 1. SSE 연결 테스트
```bash
# EC2에서 실행
curl -N -H "Accept: text/event-stream" http://localhost/mock-interviews/1/stream
# 연결이 유지되고 타임아웃 없어야 함
```

### 2. 브라우저에서 테스트
1. AI 면접 연습 시작
2. F12 → Network 탭 열기
3. 답변 전송
4. `/stream` 요청 확인:
   - Status: `200` (Pending 상태로 유지)
   - Type: `eventsource`
   - Size: 계속 증가

### 3. 콘솔 로그 확인
```javascript
// 브라우저 콘솔에서
console.log(eventSource.readyState);
// 1 (OPEN) 이어야 함
// 0 = CONNECTING, 2 = CLOSED
```

---

## 🔍 문제 해결

### SSE 여전히 안 되는 경우

#### 1) ALB/로드밸런서 사용 중이라면

**Target Group 설정 변경**:
- Connection Idle Timeout: `60` → `3600` (1시간)
- Stickiness: `Enable` (세션 유지)

**ALB 리스너 규칙 추가**:
```
IF path-pattern /mock-interviews/*
THEN
  - Stickiness: Enable
  - Idle Timeout: 3600
```

#### 2) 방화벽/보안 그룹 확인
```bash
# EC2 보안 그룹에서 확인
- Inbound: 80 (HTTP), 443 (HTTPS) 허용
- Outbound: All traffic 허용
```

#### 3) Nginx 로그 확인
```bash
# 에러 로그
sudo tail -f /var/log/nginx/error.log

# 액세스 로그
sudo tail -f /var/log/nginx/access.log
```

#### 4) Spring Boot 로그 확인
```bash
# 애플리케이션 로그에서 SSE 연결 확인
sudo journalctl -u your-spring-app -f
# "SSE 스트림 연결" 로그 확인
```

---

## 📊 성능 최적화 (선택 사항)

### Nginx worker 설정
```bash
sudo vim /etc/nginx/nginx.conf
```

```nginx
# CPU 코어 수만큼 설정 (t2.micro는 1, t2.small은 1, t2.medium은 2)
worker_processes auto;

# 연결 수 증가
events {
    worker_connections 1024;
}
```

---

## 🎯 최종 체크리스트

- [ ] Nginx 설정 백업 완료
- [ ] SSE location 블록 추가
- [ ] `nginx -t` 통과
- [ ] Nginx 재시작 성공
- [ ] 브라우저에서 SSE 연결 확인 (Network 탭)
- [ ] 답변 전송 후 즉시 표시 확인
- [ ] AI 질문 실시간 표시 확인
- [ ] 로딩 인디케이터 정상 작동 확인

---

## 🆘 여전히 안 되면?

1. **Nginx 로그 확인**:
   ```bash
   sudo tail -50 /var/log/nginx/error.log
   ```

2. **Spring Boot 로그 확인**:
   ```bash
   sudo journalctl -u your-app -n 100
   ```

3. **브라우저 콘솔 에러 확인**:
   - F12 → Console 탭
   - SSE 관련 에러 메시지 확인

4. **설정 공유**:
   ```bash
   sudo cat /etc/nginx/conf.d/interview-note-api.conf
   ```
   - 이 내용을 공유해주시면 추가 지원 가능

---

## 📝 참고 사항

- SSE는 HTTP/1.1 필수 (HTTP/2는 지원 안 함)
- `proxy_buffering off`가 가장 중요한 설정
- 타임아웃을 충분히 길게 설정 (최소 30분 이상)
- HTTPS 사용 시 SSL 설정도 동일하게 적용

---

**작성일**: 2026-05-03
**대상 환경**: AWS EC2 Amazon Linux 2023 + Nginx + Spring Boot
