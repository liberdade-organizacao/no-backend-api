CREATE TRIGGER IF NOT EXISTS insert_file_size
AFTER INSERT ON files
FOR EACH ROW BEGIN
    UPDATE files SET file_size=LENGTH(OLD.contents) WHERE id = OLD.id;
END;

