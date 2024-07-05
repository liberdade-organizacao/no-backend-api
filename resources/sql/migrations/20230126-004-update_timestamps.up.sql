CREATE TRIGGER IF NOT EXISTS update_actions_timestamp
BEFORE UPDATE ON actions
FOR EACH ROW BEGIN
    UPDATE actions SET last_updated_at = TIME('now') WHERE id=OLD.id;
END;

