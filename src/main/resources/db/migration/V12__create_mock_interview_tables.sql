-- Phase 7A: 모의 면접 세션 테이블
-- 실시간 AI 채팅 면접 시스템

-- 모의 면접 세션
CREATE TABLE mock_interviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    job_posting_id BIGINT,                    -- nullable (직무 기반 면접 시 null)
    selected_job_field VARCHAR(50) NOT NULL,  -- 필수 (공고 또는 사용자 선택)
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,
    overall_feedback TEXT,
    key_strengths TEXT,                       -- JSON array string
    key_improvements TEXT,                    -- JSON array string
    average_score DOUBLE PRECISION,
    recommendation TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (job_posting_id) REFERENCES job_postings(id)
);

-- 면접 채팅 메시지
CREATE TABLE interview_messages (
    id BIGSERIAL PRIMARY KEY,
    mock_interview_id BIGINT NOT NULL,
    sender VARCHAR(20) NOT NULL,              -- 'AI' or 'USER'
    content TEXT NOT NULL,
    message_index INT NOT NULL,               -- 순서 (0부터 시작)
    ai_reasoning TEXT,                        -- AI 메시지 전용
    logic_score INT,                          -- USER 메시지 전용 (평가)
    specificity_score INT,
    delivery_score INT,
    feedback_comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (mock_interview_id) REFERENCES mock_interviews(id) ON DELETE CASCADE
);

-- 인덱스
CREATE INDEX idx_mock_interviews_user_id ON mock_interviews(user_id);
CREATE INDEX idx_mock_interviews_status ON mock_interviews(status);
CREATE INDEX idx_interview_messages_order ON interview_messages(mock_interview_id, message_index);

-- 코멘트
COMMENT ON TABLE mock_interviews IS 'Phase 7: 모의 면접 세션 (직무 기반 또는 공고 기반)';
COMMENT ON TABLE interview_messages IS 'Phase 7: 실시간 채팅 메시지 (AI 질문 + 사용자 답변)';
COMMENT ON COLUMN mock_interviews.job_posting_id IS 'nullable - 직무 기반 면접 시 null';
COMMENT ON COLUMN mock_interviews.selected_job_field IS '필수 - 공고의 effectiveJobField 또는 사용자 선택';
COMMENT ON COLUMN interview_messages.message_index IS '대화 순서 (0부터 시작, AI 첫 질문: 0)';
