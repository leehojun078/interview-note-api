-- Phase 6E 버그 수정: question_id를 nullable로 변경
-- 이유: GeneratedQuestion에 대한 답변은 question_id가 필요 없음

-- 1. 외래키 제약 조건 삭제 (H2/PostgreSQL 모두 지원)
ALTER TABLE interview_answers
DROP CONSTRAINT IF EXISTS interview_answers_question_id_fkey;

-- 2. question_id를 nullable로 변경
ALTER TABLE interview_answers
ALTER COLUMN question_id DROP NOT NULL;

-- 3. 외래키 제약 조건 재생성 (nullable 버전)
ALTER TABLE interview_answers
ADD CONSTRAINT fk_interview_answers_question
FOREIGN KEY (question_id) REFERENCES questions(id);

-- 참고:
-- - question_id IS NOT NULL: 정적 질문에 대한 답변
-- - generated_question_id IS NOT NULL: AI 생성 질문에 대한 답변
-- - 두 컬럼 중 정확히 하나만 NOT NULL이어야 함 (애플리케이션 레벨에서 보장)
