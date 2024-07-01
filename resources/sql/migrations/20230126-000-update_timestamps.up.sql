CREATE TRIGGER IF NOT EXISTS update_clients_timestamp
BEFORE UPDATE ON clients
FOR EACH ROW BEGIN
    UPDATE clients SET last_updated_at = NOW() WHERE id=OLD.id;
END;

