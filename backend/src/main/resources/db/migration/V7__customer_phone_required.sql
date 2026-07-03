-- Suki (customer) mobile number is now required.
ALTER TABLE customers ALTER COLUMN phone_number SET NOT NULL;
