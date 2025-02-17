CREATE TRIGGER IF NOT EXISTS update_files_timestamp
BEFORE UPDATE ON files
FOR EACH ROW BEGIN
    UPDATE files SET last_updated_at = TIME('now') WHERE id=OLD.id;
END;

