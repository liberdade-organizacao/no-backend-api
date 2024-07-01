
CREATE TRIGGER update_app_memberships_timestamp BEFORE UPDATE ON app_memberships FOR EACH ROW EXECUTE PROCEDURE update_last_updated_at_column();