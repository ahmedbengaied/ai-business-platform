CREATE TABLE workflow_task_dependencies (
    task_id VARCHAR(255) NOT NULL REFERENCES workflow_tasks (id) ON DELETE CASCADE,
    prerequisite_task_id VARCHAR(255) NOT NULL REFERENCES workflow_tasks (id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, prerequisite_task_id),
    CHECK (task_id <> prerequisite_task_id)
);

CREATE INDEX idx_workflow_task_dependencies_prerequisite
    ON workflow_task_dependencies (prerequisite_task_id);
