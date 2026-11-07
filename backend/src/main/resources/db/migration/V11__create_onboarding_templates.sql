CREATE TABLE onboarding_templates (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    owner_id VARCHAR(255) NOT NULL REFERENCES users (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_onboarding_templates_owner_name UNIQUE (owner_id, name)
);

CREATE INDEX idx_onboarding_templates_owner_name
    ON onboarding_templates (owner_id, name);

CREATE TABLE onboarding_template_items (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    position INTEGER NOT NULL,
    template_id VARCHAR(255) NOT NULL REFERENCES onboarding_templates (id) ON DELETE CASCADE,
    CONSTRAINT uq_onboarding_template_items_position UNIQUE (template_id, position),
    CONSTRAINT uq_onboarding_template_items_name UNIQUE (template_id, name)
);

CREATE INDEX idx_onboarding_template_items_template_id
    ON onboarding_template_items (template_id);
