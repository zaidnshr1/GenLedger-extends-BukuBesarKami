
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- USERS 
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN_PUSAT', 'ADMIN_PROJECT')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- PROJECTS 
CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_code VARCHAR(20) NOT NULL UNIQUE,
    project_name VARCHAR(100) NOT NULL,
    description TEXT,
    budget NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (budget >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- USER_PROJECT (Admin Project -> Project assignment) 
CREATE TABLE user_projects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    project_id UUID NOT NULL REFERENCES projects(id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    assigned_by UUID NOT NULL REFERENCES users(id),
    UNIQUE(user_id, project_id)
);

-- COA: Chart of Accounts 
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_code VARCHAR(20) NOT NULL UNIQUE,
    account_name VARCHAR(100) NOT NULL,
    account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE')),
    normal_balance VARCHAR(10) NOT NULL CHECK (normal_balance IN ('DEBIT','CREDIT')),
    project_id UUID REFERENCES projects(id),  -- NULL = akun global kantor pusat
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- JOURNAL ENTRIES (Header) 
CREATE TABLE journal_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entry_number VARCHAR(30) NOT NULL UNIQUE,
    project_id UUID REFERENCES projects(id),  -- NULL = transaksi kantor pusat
    entry_date DATE NOT NULL,
    description VARCHAR(255) NOT NULL,
    reference_number VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','POSTED','VOIDED')),
    total_debit NUMERIC(19,2) NOT NULL DEFAULT 0,
    total_credit NUMERIC(19,2) NOT NULL DEFAULT 0,
    created_by UUID NOT NULL REFERENCES users(id),
    posted_by UUID REFERENCES users(id),
    posted_at TIMESTAMPTZ,
    voided_by UUID REFERENCES users(id),
    voided_at TIMESTAMPTZ,
    void_reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- CONSTRAINT: double-entry setelah POSTED
    CONSTRAINT chk_balanced CHECK (
        status != 'POSTED' OR total_debit = total_credit
    )
);

-- JOURNAL LINES (Detail) 
CREATE TABLE journal_lines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    journal_entry_id UUID NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES accounts(id),
    debit_amount NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (debit_amount >= 0),
    credit_amount NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (credit_amount >= 0),
    description VARCHAR(255),
    line_order INTEGER NOT NULL,
    -- Either debit or credit, not both
    CONSTRAINT chk_one_side CHECK (
        (debit_amount > 0 AND credit_amount = 0) OR
        (debit_amount = 0 AND credit_amount > 0)
    )
);

-- AUDIT LOG 
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    project_id UUID REFERENCES projects(id),
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID,
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- REFRESH TOKENS 
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    token VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- INDEXES 
CREATE INDEX idx_journal_entries_project ON journal_entries(project_id);
CREATE INDEX idx_journal_entries_date ON journal_entries(entry_date);
CREATE INDEX idx_journal_entries_status ON journal_entries(status);
CREATE INDEX idx_journal_lines_entry ON journal_lines(journal_entry_id);
CREATE INDEX idx_journal_lines_account ON journal_lines(account_id);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_project ON audit_logs(project_id);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);