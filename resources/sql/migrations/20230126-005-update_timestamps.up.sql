CREATE TRIGGER IF NOT EXISTS update_app_memberships_timestamp
BEFORE UPDATE ON app_memberships
FOR EACH ROW BEGIN
    UPDATE app_memberships SET last_updated_at = TIME('now') WHERE id=OLD.id;
END;

