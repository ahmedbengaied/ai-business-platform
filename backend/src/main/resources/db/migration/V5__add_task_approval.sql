ALTER TABLE workflow_tasks
    ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN approval_note VARCHAR(1000),
    ADD COLUMN reviewed_by VARCHAR(320),
    ADD COLUMN reviewed_at TIMESTAMP WITH TIME ZONE;
