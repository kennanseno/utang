-- Track whether the store owner has verified ownership of their mobile number
-- via OTP. Newly registered owners (and anyone who changes their number) start
-- unverified until they complete the verification flow.
ALTER TABLE stores ADD COLUMN phone_verified BOOLEAN NOT NULL DEFAULT false;
