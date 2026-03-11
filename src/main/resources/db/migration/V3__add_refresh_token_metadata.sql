-- V3: Tambah metadata keamanan pada refresh_tokens
-- User-Agent dan IP digunakan untuk deteksi login yg mencurigakan

ALTER TABLE refresh_tokens
    ADD COLUMN IF NOT EXISTS user_agent  TEXT,
    ADD COLUMN IF NOT EXISTS ip_address  VARCHAR(45),
    ADD COLUMN IF NOT EXISTS device_info VARCHAR(255);

COMMENT ON COLUMN refresh_tokens.user_agent  IS 'Browser/app user agent saat token dibuat';
COMMENT ON COLUMN refresh_tokens.ip_address  IS 'IP address saat token dibuat';
COMMENT ON COLUMN refresh_tokens.device_info IS 'Ringkasan device (OS, browser)';