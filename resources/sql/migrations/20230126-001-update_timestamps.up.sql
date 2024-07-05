CREATE TRIGGER IF NOT EXISTS update_apps_timestamp
BEFORE UPDATE ON apps
FOR EACH ROW BEGIN
    UPDATE apps SET last_updated_at = TIME('now') WHERE id=OLD.id;
END;

