
CREATE TRIGGER update_files_timestamp BEFORE UPDATE ON files FOR EACH ROW EXECUTE PROCEDURE update_last_updated_at_column();