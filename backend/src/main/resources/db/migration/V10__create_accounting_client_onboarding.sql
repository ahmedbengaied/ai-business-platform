CREATE TABLE accounting_clients (
    id VARCHAR(255) PRIMARY KEY,
    legal_name VARCHAR(200) NOT NULL,
    contact_name VARCHAR(200) NOT NULL,
    contact_email VARCHAR(320),
    contact_phone VARCHAR(40),
    owner_id VARCHAR(255) NOT NULL REFERENCES users (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_accounting_clients_owner_created
    ON accounting_clients (owner_id, created_at);

CREATE TABLE onboarding_documents (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL,
    client_id VARCHAR(255) NOT NULL REFERENCES accounting_clients (id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_onboarding_documents_client_name UNIQUE (client_id, name)
);

CREATE INDEX idx_onboarding_documents_client_id
    ON onboarding_documents (client_id);
