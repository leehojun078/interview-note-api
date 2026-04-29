-- Phase 6C: 기존 답변 플로우와 생성된 질문 연결
-- InterviewAnswer에 generated_question_id 추가 (nullable)

ALTER TABLE interview_answers
ADD COLUMN generated_question_id BIGINT;

-- FK 제약 조건 추가
ALTER TABLE interview_answers
ADD CONSTRAINT fk_interview_answers_generated_question
FOREIGN KEY (generated_question_id) REFERENCES generated_questions(id);

-- 인덱스 추가 (성능 최적화)
CREATE INDEX idx_interview_answers_generated_question ON interview_answers(generated_question_id);

-- 참고: question_id와 generated_question_id 중 하나만 사용
-- - question_id IS NOT NULL: 정적 질문에 대한 답변
-- - generated_question_id IS NOT NULL: AI 생성 질문에 대한 답변
