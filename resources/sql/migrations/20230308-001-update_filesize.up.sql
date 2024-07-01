CREATE TRIGGER IF NOT EXISTS update_file_size
BEFORE UPDATE ON files
FOR EACH ROW BEGIN
    UPDATE files SET file_size=LENGTH(OLD.contents) WHERE id = OLD.id;
END;

