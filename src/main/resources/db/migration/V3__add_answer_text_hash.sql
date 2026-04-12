-- V3: ai_feedbacks 테이블에 answer_text_hash 컬럼 추가 (중복 요청 방지용)

-- 1. answerTextHash 컬럼 추가
ALTER TABLE ai_feedbacks
ADD COLUMN answer_text_hash VARCHAR(64);

-- 2. 인덱스 생성 (해시 + 생성일시 복합 인덱스)
CREATE INDEX idx_ai_feedbacks_hash_created
ON ai_feedbacks(answer_text_hash, created_at);

-- 참고:
-- - answer_text_hash는 SHA-256 해시 (64자)
-- - 24시간 내 동일 해시 조회를 위한 복합 인덱스
-- - NULL 허용 (기존 데이터 호환성)
