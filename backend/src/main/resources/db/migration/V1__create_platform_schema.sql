CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE projects (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(2000),
    owner_id VARCHAR(255) NOT NULL REFERENCES users (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_projects_owner_id ON projects (owner_id);

CREATE TABLE workflows (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL,
    project_id VARCHAR(255) NOT NULL REFERENCES projects (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_workflows_project_id ON workflows (project_id);

CREATE TABLE workflow_tasks (
    id VARCHAR(255) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(4000),
    agent_role VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    workflow_id VARCHAR(255) NOT NULL REFERENCES workflows (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_workflow_tasks_workflow_id ON workflow_tasks (workflow_id);
