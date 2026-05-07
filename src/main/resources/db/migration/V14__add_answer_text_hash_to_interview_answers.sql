-- V14: interview_answers 테이블에 answer_text_hash 컬럼 추가 (중복 답변 제출 방지용)

-- 1. answerTextHash 컬럼 추가
ALTER TABLE interview_answers
ADD COLUMN answer_text_hash VARCHAR(64);

-- 2. 인덱스 생성 (성능 최적화)
-- userId + questionId/generatedQuestionId + answerTextHash 조합으로 중복 검색
CREATE INDEX idx_interview_answers_user_question_hash
ON interview_answers(user_id, question_id, answer_text_hash);

CREATE INDEX idx_interview_answers_user_genq_hash
ON interview_answers(user_id, generated_question_id, answer_text_hash);

-- 참고:
-- - answer_text_hash는 SHA-256 해시 (64자)
-- - 동일한 질문에 동일한 답변 재제출 방지용
-- - NULL 허용 (기존 데이터 호환성)
