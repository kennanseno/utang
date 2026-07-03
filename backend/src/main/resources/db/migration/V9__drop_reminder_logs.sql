-- Reminders are now sent manually by the store owner from their own phone
-- (SMS deep link or copied message). The backend no longer tracks reminders,
-- so the once-per-day lock and its table are removed.
DROP TABLE IF EXISTS reminder_logs;
