
CREATE TRIGGER update_clients_timestamp BEFORE UPDATE ON clients FOR EACH ROW EXECUTE PROCEDURE update_last_updated_at_column();