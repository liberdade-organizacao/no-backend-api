

CREATE TRIGGER trigger_update_file_size BEFORE INSERT OR UPDATE ON files FOR EACH ROW EXECUTE PROCEDURE update_file_size();