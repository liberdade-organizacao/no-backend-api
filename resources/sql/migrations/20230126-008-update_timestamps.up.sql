
CREATE TRIGGER update_actions_timestamp BEFORE UPDATE ON actions FOR EACH ROW EXECUTE PROCEDURE update_last_updated_at_column();