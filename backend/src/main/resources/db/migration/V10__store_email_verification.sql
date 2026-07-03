-- Switch store-owner verification from SMS OTP to email.
--
-- Login becomes password-only, so trusted devices are no longer needed. The
-- email address is verified once (via a code emailed to the owner); the mobile
-- number stays as an unverified contact used for suki-facing reminders.
--
-- Email is nullable so this migration does not fail on any pre-existing store
-- rows that predate the column; the application requires it for all new accounts.
ALTER TABLE stores
    ADD COLUMN email VARCHAR(255);

ALTER TABLE stores
    ADD CONSTRAINT uq_stores_email UNIQUE (email);

ALTER TABLE stores
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Phone verification is gone; the number is now just a contact field.
ALTER TABLE stores
    DROP COLUMN IF EXISTS phone_verified;

-- Repurpose the one-time-code table to key on email instead of phone number.
ALTER TABLE otp_codes
    RENAME COLUMN phone_number TO email;
ALTER INDEX idx_otp_phone RENAME TO idx_otp_email;

-- Password-only login no longer uses trusted devices.
DROP TABLE IF EXISTS trusted_devices;
