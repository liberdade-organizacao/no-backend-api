CREATE OR REPLACE FUNCTION update_file_size()
RETURNS TRIGGER AS $$
BEGIN
    NEW.file_size = LENGTH(NEW.contents);
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_update_file_size BEFORE INSERT OR UPDATE ON files FOR EACH ROW EXECUTE PROCEDURE update_file_size();
