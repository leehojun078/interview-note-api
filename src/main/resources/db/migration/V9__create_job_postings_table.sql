-- Job Postings table (Phase 6A)
-- 채용 공고 정보 저장 (사용자 입력 또는 파싱된 데이터)
CREATE TABLE job_postings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    original_url TEXT NOT NULL,
    company_name VARCHAR(200) NOT NULL,
    job_title VARCHAR(200) NOT NULL,
    job_description TEXT NOT NULL,
    selected_job_field VARCHAR(50),          -- 사용자 선택 직무 (우선)
    inferred_job_field VARCHAR(50),          -- AI 추론 직무 (대체)
    required_skills TEXT,                    -- JSON array 문자열
    preferred_skills TEXT,                   -- JSON array 문자열
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Generated Questions table (Phase 6A)
-- AI가 채용 공고 기반으로 생성한 맞춤형 질문
CREATE TABLE generated_questions (
    id BIGSERIAL PRIMARY KEY,
    job_posting_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(100) NOT NULL,
    difficulty VARCHAR(20) NOT NULL CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    ai_reasoning TEXT NOT NULL,
    order_index INT NOT NULL CHECK (order_index BETWEEN 1 AND 10),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (job_posting_id) REFERENCES job_postings(id) ON DELETE CASCADE
);

-- Indexes for better query performance
CREATE INDEX idx_job_postings_user ON job_postings(user_id);
CREATE INDEX idx_job_postings_active ON job_postings(is_active);
CREATE INDEX idx_job_postings_url ON job_postings(original_url);
CREATE INDEX idx_generated_questions_posting ON generated_questions(job_posting_id);
CREATE INDEX idx_generated_questions_order ON generated_questions(job_posting_id, order_index);
