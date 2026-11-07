CREATE TABLE purchase_bills (
    id VARCHAR(255) PRIMARY KEY,
    firm_id VARCHAR(255) NOT NULL REFERENCES accounting_firms (id),
    supplier_name VARCHAR(200) NOT NULL,
    bill_number VARCHAR(50) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    subtotal NUMERIC(19, 2) NOT NULL CHECK (subtotal > 0),
    tax_rate NUMERIC(5, 2) NOT NULL CHECK (tax_rate >= 0 AND tax_rate <= 100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_purchase_bills_firm_number UNIQUE (firm_id, bill_number),
    CONSTRAINT ck_purchase_bills_due_date CHECK (due_date >= issue_date)
);

CREATE INDEX idx_purchase_bills_firm_issue_date
    ON purchase_bills (firm_id, issue_date DESC, created_at DESC);

CREATE TABLE purchase_bill_payments (
    id VARCHAR(255) PRIMARY KEY,
    bill_id VARCHAR(255) NOT NULL REFERENCES purchase_bills (id),
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    payment_date DATE NOT NULL,
    note VARCHAR(300),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_purchase_bill_payments_bill_date
    ON purchase_bill_payments (bill_id, payment_date, created_at);
