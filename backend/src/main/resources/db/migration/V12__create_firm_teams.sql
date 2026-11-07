CREATE TABLE accounting_firms (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE users
    ADD COLUMN firm_id VARCHAR(255),
    ADD COLUMN firm_role VARCHAR(20),
    ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0;

INSERT INTO accounting_firms (id, name, created_at)
SELECT 'firm-' || id, first_name || ' ' || last_name || ' Firm', created_at
FROM users;

UPDATE users
SET firm_id = 'firm-' || id,
    firm_role = 'OWNER';

ALTER TABLE users
    ALTER COLUMN firm_id SET NOT NULL,
    ALTER COLUMN firm_role SET NOT NULL,
    ADD CONSTRAINT fk_users_firm FOREIGN KEY (firm_id) REFERENCES accounting_firms (id);

ALTER TABLE accounting_clients
    ADD COLUMN firm_id VARCHAR(255);

UPDATE accounting_clients client
SET firm_id = app_user.firm_id
FROM users app_user
WHERE client.owner_id = app_user.id;

DROP INDEX idx_accounting_clients_owner_created;

ALTER TABLE accounting_clients
    ALTER COLUMN firm_id SET NOT NULL,
    ADD CONSTRAINT fk_accounting_clients_firm FOREIGN KEY (firm_id) REFERENCES accounting_firms (id),
    DROP CONSTRAINT accounting_clients_owner_id_fkey,
    DROP COLUMN owner_id;

CREATE INDEX idx_accounting_clients_firm_created
   ON accounting_clients (firm_id, created_at);

ALTER TABLE onboarding_templates
    ADD COLUMN firm_id VARCHAR(255);

UPDATE onboarding_templates template
SET firm_id = app_user.firm_id
FROM users app_user
WHERE template.owner_id = app_user.id;

DROP INDEX idx_onboarding_templates_owner_name;

ALTER TABLE onboarding_templates
    ALTER COLUMN firm_id SET NOT NULL,
    ADD CONSTRAINT fk_onboarding_templates_firm FOREIGN KEY (firm_id) REFERENCES accounting_firms (id),
    DROP CONSTRAINT onboarding_templates_owner_id_fkey,
    DROP CONSTRAINT uq_onboarding_templates_owner_name,
    DROP COLUMN owner_id,
    ADD CONSTRAINT uq_onboarding_templates_firm_name UNIQUE (firm_id, name);

CREATE INDEX idx_onboarding_templates_firm_name
    ON onboarding_templates (firm_id, name);

CREATE TABLE firm_invitations (
    id VARCHAR(255) PRIMARY KEY,
    firm_id VARCHAR(255) NOT NULL REFERENCES accounting_firms (id),
    email VARCHAR(320) NOT NULL,
    role VARCHAR(20) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_by_id VARCHAR(255) NOT NULL REFERENCES users (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_firm_invitations_pending
    ON firm_invitations (firm_id, email, expires_at)
    WHERE accepted_at IS NULL AND revoked_at IS NULL;
