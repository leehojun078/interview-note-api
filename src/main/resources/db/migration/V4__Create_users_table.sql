-- Phase 4A: 사용자 관리 시스템
-- 생성일: 2026-04-19
-- 목적: 로그인/회원가입 기능 추가

-- users 테이블 생성 (H2와 PostgreSQL 호환)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

-- 이메일 인덱스 (로그인 성능 향상)
CREATE INDEX idx_users_email ON users(email);

-- 역할 인덱스 (관리자 조회 성능 향상)
CREATE INDEX idx_users_role ON users(role);

-- 활성 상태 + 이메일 복합 인덱스 (로그인 성능 향상)
CREATE INDEX idx_users_active_email ON users(is_active, email);

-- 테스트용 사용자 추가 (개발 환경)
-- 비밀번호: "password123" (BCrypt 해시, strength=10)
-- 프로덕션 환경에서는 이 데이터를 삭제하거나 비활성화해야 함
INSERT INTO users (email, password_hash, name, role, is_active, created_at)
VALUES (
    'test@example.com',
    '$2a$10$CSIHFSDRVYcmCoF/icrphetk.xbfsFI47zPiVioS.ONAtHWQe.nUe',
    'Test User',
    'USER',
    true,
    CURRENT_TIMESTAMP
);

-- 테스트용 관리자 추가 (개발 환경)
-- 비밀번호: "admin123" (BCrypt 해시, strength=10)
INSERT INTO users (email, password_hash, name, role, is_active, created_at)
VALUES (
    'admin@example.com',
    '$2a$10$VQN0zViYtFNuOtVhQqSluOLYZEWAN6n0llxyzL.AHmSLTO94QwqH6',
    'Admin User',
    'ADMIN',
    true,
    CURRENT_TIMESTAMP
);

-- 주의: InterviewAnswer에 userId 추가는 Phase 4A-2에서 진행
-- (로그인/회원가입 기능 테스트 완료 후)
