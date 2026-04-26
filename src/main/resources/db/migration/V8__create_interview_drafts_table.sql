-- Phase 3: 답변 자동 저장 (Draft) 테이블 생성
-- 사용자가 답변 작성 중 임시 저장한 내용을 보관

CREATE TABLE interview_drafts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    draft_text TEXT NOT NULL,
    last_saved TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 사용자당 질문당 하나의 Draft만 존재
    CONSTRAINT uk_user_question UNIQUE (user_id, question_id),

    -- 외래키 제약조건
    CONSTRAINT fk_draft_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_draft_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

-- 인덱스 생성 (조회 성능 최적화)
CREATE INDEX idx_draft_user_id ON interview_drafts(user_id);
CREATE INDEX idx_draft_question_id ON interview_drafts(question_id);
CREATE INDEX idx_draft_last_saved ON interview_drafts(last_saved);
