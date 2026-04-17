-- Questions table
CREATE TABLE questions (
    id BIGSERIAL PRIMARY KEY,
    job_field VARCHAR(50) NOT NULL DEFAULT 'IT',
    target_job VARCHAR(100) NOT NULL,
    category VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Interview Answers table
CREATE TABLE interview_answers (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL,
    answer_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (question_id) REFERENCES questions(id)
);

-- AI Feedbacks table
CREATE TABLE ai_feedbacks (
    id BIGSERIAL PRIMARY KEY,
    interview_answer_id BIGINT NOT NULL,
    logic_score INT NOT NULL,
    specificity_score INT NOT NULL,
    job_fit_score INT NOT NULL,
    delivery_score INT NOT NULL,
    strengths TEXT NOT NULL,
    improvements TEXT NOT NULL,
    model_answer TEXT NOT NULL,
    overall_comment TEXT NOT NULL,
    job_field VARCHAR(50) NOT NULL DEFAULT 'IT',
    model_name VARCHAR(50) NOT NULL,
    prompt_version VARCHAR(20) NOT NULL,
    token_usage_input INT NOT NULL,
    token_usage_output INT NOT NULL,
    raw_response TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (interview_answer_id) REFERENCES interview_answers(id)
);

-- Indexes for better query performance
CREATE INDEX idx_questions_category ON questions(category);
CREATE INDEX idx_questions_difficulty ON questions(difficulty);
CREATE INDEX idx_questions_active ON questions(is_active);
CREATE INDEX idx_interview_answers_question ON interview_answers(question_id);
CREATE INDEX idx_interview_answers_created ON interview_answers(created_at);
CREATE INDEX idx_ai_feedbacks_answer ON ai_feedbacks(interview_answer_id);
