CREATE TABLE sales_customers (
    id VARCHAR(255) PRIMARY KEY,
    firm_id VARCHAR(255) NOT NULL REFERENCES accounting_firms (id),
    name VARCHAR(200) NOT NULL,
    email VARCHAR(320),
    phone VARCHAR(40),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_sales_customers_firm_created
    ON sales_customers (firm_id, created_at);

CREATE TABLE sales_invoices (
    id VARCHAR(255) PRIMARY KEY,
    firm_id VARCHAR(255) NOT NULL REFERENCES accounting_firms (id),
    customer_id VARCHAR(255) NOT NULL REFERENCES sales_customers (id),
    invoice_number VARCHAR(50) NOT NULL,
    description VARCHAR(500) NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    subtotal NUMERIC(19, 2) NOT NULL CHECK (subtotal > 0),
    tax_rate NUMERIC(5, 2) NOT NULL CHECK (tax_rate >= 0 AND tax_rate <= 100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_sales_invoices_firm_number UNIQUE (firm_id, invoice_number),
    CONSTRAINT ck_sales_invoices_due_date CHECK (due_date >= issue_date)
);

CREATE INDEX idx_sales_invoices_firm_issue_date
    ON sales_invoices (firm_id, issue_date DESC, created_at DESC);

CREATE TABLE sales_invoice_payments (
    id VARCHAR(255) PRIMARY KEY,
    invoice_id VARCHAR(255) NOT NULL REFERENCES sales_invoices (id),
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    payment_date DATE NOT NULL,
    note VARCHAR(300),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_sales_invoice_payments_invoice_date
    ON sales_invoice_payments (invoice_id, payment_date, created_at);
