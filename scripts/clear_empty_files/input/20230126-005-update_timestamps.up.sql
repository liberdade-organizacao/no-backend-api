
CREATE TRIGGER update_apps_timestamp BEFORE UPDATE ON apps FOR EACH ROW EXECUTE PROCEDURE update_last_updated_at_column();