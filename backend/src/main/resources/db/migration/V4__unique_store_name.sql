-- Enforce a unique store name (case-insensitive) alongside the existing
-- unique username and unique phone number constraints on stores.
CREATE UNIQUE INDEX uq_stores_name_lower ON stores (LOWER(name));
