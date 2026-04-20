-- Phase 5: Add user job preferences
-- 사용자가 선택한 직무 분야와 경력 수준을 저장

-- Add job field column (nullable)
ALTER TABLE users ADD COLUMN job_field VARCHAR(50);

-- Add career level column (nullable)
ALTER TABLE users ADD COLUMN career_level VARCHAR(20);

-- Create index for job field filtering
CREATE INDEX idx_users_job_field ON users(job_field);

-- Update existing test users with default values
-- 기존 테스트 계정에 IT/ENTRY 기본값 설정
UPDATE users
SET job_field = 'IT', career_level = 'ENTRY'
WHERE email IN ('test@example.com', 'admin@example.com');
