ALTER TABLE workflow_tasks
    ADD COLUMN execution_attempts INTEGER NOT NULL DEFAULT 0;

ALTER TABLE workflow_tasks
    ADD COLUMN last_execution_failure VARCHAR(200);
