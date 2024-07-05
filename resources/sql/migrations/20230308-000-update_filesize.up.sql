CREATE TRIGGER IF NOT EXISTS insert_file_size
AFTER INSERT ON files
FOR EACH ROW BEGIN
    UPDATE files SET file_size=LENGTH(NEW.contents) WHERE id = NEW.id;
END;

