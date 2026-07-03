-- Username/password auth for store owners, plus trusted-device tracking.
-- Login now uses username + password; a new device must be verified by OTP sent
-- to the owner's mobile number.
--
-- Existing stores predate credentials (they logged in by phone + OTP only) and
-- therefore cannot log in under the new scheme until they re-register. The
-- columns are nullable so the migration does not fail on legacy rows.
ALTER TABLE stores
    ADD COLUMN username VARCHAR(60);

ALTER TABLE stores
    ADD COLUMN password_hash VARCHAR(100);

ALTER TABLE stores
    ADD CONSTRAINT uq_stores_username UNIQUE (username);

-- Devices the owner has verified via OTP. A trusted device skips OTP on login.
CREATE TABLE trusted_devices (
    id         BIGSERIAL PRIMARY KEY,
    store_id   BIGINT      NOT NULL REFERENCES stores (id),
    device_id  VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_trusted_device UNIQUE (store_id, device_id)
);
CREATE INDEX idx_trusted_store ON trusted_devices (store_id);
