-- Make customer phone_number optional. Owners may add a suki without a
-- mobile number; the "Text via SMS" deep-link is unavailable for those
-- customers until a number is added (copy message still works).
ALTER TABLE customers ALTER COLUMN phone_number DROP NOT NULL;
