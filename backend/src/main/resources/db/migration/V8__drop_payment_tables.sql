-- Remove the online-payment (PayMongo/PSP) feature. The app no longer integrates
-- with any payment gateway, so these tables and their indexes are dropped.
DROP TABLE IF EXISTS payment_links;
DROP TABLE IF EXISTS webhook_events;
DROP TABLE IF EXISTS payments;
