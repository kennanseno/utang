-- Store owner onboarding profile fields.
-- Existing stores are treated as already onboarded so they are not forced back
-- through the onboarding flow.
ALTER TABLE stores
    ADD COLUMN owner_name VARCHAR(120);

ALTER TABLE stores
    ADD COLUMN onboarded BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE stores SET onboarded = TRUE;
