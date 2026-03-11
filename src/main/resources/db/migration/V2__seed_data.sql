-- Seed: Default Chart of Accounts (COA) untuk kantor pusat
-- Password: Admin@123 (BCrypt hashed) - WAJIB GANTI di produksi

INSERT INTO users (id, username, email, password, full_name, role, is_active)
VALUES (
    uuid_generate_v4(),
    'adminpusat',
    'm.zaidanshori04@gmail.com',
    '$2a$12$9ZyMp6Qw6DEh9GZUMfHNr.y3Rt1r3e3Oa3Dq7.Bh2XNz.wWE4Gg6',
    'Admin Pusat',
    'ADMIN_PUSAT',
    TRUE
);

-- Default COA global (project_id = NULL)
INSERT INTO accounts (account_code, account_name, account_type, normal_balance) VALUES
('1-1000', 'Kas',                   'ASSET',     'DEBIT'),
('1-1100', 'Bank',                  'ASSET',     'DEBIT'),
('1-1200', 'Piutang Usaha',         'ASSET',     'DEBIT'),
('1-2000', 'Peralatan',             'ASSET',     'DEBIT'),
('2-1000', 'Hutang Usaha',          'LIABILITY', 'CREDIT'),
('2-1100', 'Hutang Bank',           'LIABILITY', 'CREDIT'),
('3-1000', 'Modal',                 'EQUITY',    'CREDIT'),
('3-1100', 'Laba Ditahan',          'EQUITY',    'CREDIT'),
('4-1000', 'Pendapatan Usaha',      'REVENUE',   'CREDIT'),
('4-1100', 'Pendapatan Lain-lain',  'REVENUE',   'CREDIT'),
('5-1000', 'Beban Gaji',            'EXPENSE',   'DEBIT'),
('5-1100', 'Beban Operasional',     'EXPENSE',   'DEBIT'),
('5-1200', 'Beban Penyusutan',      'EXPENSE',   'DEBIT'),
('5-1300', 'Beban Lain-lain',       'EXPENSE',   'DEBIT');