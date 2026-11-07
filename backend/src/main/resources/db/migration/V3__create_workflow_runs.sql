CREATE TABLE workflow_runs (
    id VARCHAR(255) PRIMARY KEY,
    workflow_id VARCHAR(255) NOT NULL REFERENCES workflows (id),
    started_by VARCHAR(320) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    completed_task_count INTEGER NOT NULL,
    failure_reason VARCHAR(200)
);

CREATE INDEX idx_workflow_runs_workflow_started_at
    ON workflow_runs (workflow_id, started_at DESC);
