-- Phase 4A-2: 사용자별 답변 이력 분리
-- 생성일: 2026-04-19
-- 목적: InterviewAnswer 테이블에 userId 추가

-- 1. user_id 컬럼 추가 (NULL 허용으로 먼저 추가)
ALTER TABLE interview_answers
ADD COLUMN user_id BIGINT;

-- 2. 기존 답변에 기본 사용자 할당
-- test@example.com 사용자의 ID를 조회하여 할당
UPDATE interview_answers
SET user_id = (SELECT id FROM users WHERE email = 'test@example.com' LIMIT 1)
WHERE user_id IS NULL;

-- 3. user_id를 NOT NULL로 변경
ALTER TABLE interview_answers
ALTER COLUMN user_id SET NOT NULL;

-- 4. 외래 키 제약조건 추가
ALTER TABLE interview_answers
ADD CONSTRAINT fk_interview_answers_user
FOREIGN KEY (user_id) REFERENCES users(id);

-- 5. 인덱스 추가 (사용자별 조회 성능 향상)
CREATE INDEX idx_interview_answers_user_id ON interview_answers(user_id);

-- 6. 사용자별 + 생성일시 복합 인덱스 (리뷰 목록 조회 성능 향상)
CREATE INDEX idx_interview_answers_user_created ON interview_answers(user_id, created_at DESC);
