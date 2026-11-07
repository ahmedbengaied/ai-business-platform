CREATE TABLE ai_usage_records (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL REFERENCES users (id),
    task_id VARCHAR(255) NOT NULL REFERENCES workflow_tasks (id),
    model VARCHAR(100) NOT NULL,
    prompt_tokens BIGINT NOT NULL,
    completion_tokens BIGINT NOT NULL,
    cost_usd NUMERIC(12, 8) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_ai_usage_records_user_created_at
    ON ai_usage_records (user_id, created_at);
