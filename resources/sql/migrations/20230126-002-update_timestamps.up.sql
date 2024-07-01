CREATE TRIGGER IF NOT EXISTS update_users_timestamp
BEFORE UPDATE ON users
FOR EACH ROW BEGIN
    UPDATE users SET last_updated_at = NOW() WHERE id=OLD.id;
END;
