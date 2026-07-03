-- Store owner's payment QR code image, kept inline in the stores table.
ALTER TABLE stores ADD COLUMN qr_code_image BYTEA;
ALTER TABLE stores ADD COLUMN qr_code_content_type VARCHAR(100);
