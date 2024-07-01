CREATE OR REPLACE FUNCTION update_last_updated_at_column() 
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated_at = now();
    RETURN NEW; 
END;
$$ language 'plpgsql';
CREATE TRIGGER update_clients_timestamp BEFORE UPDATE ON clients FOR EACH ROW EXECUTE PROCEDURE update_last_updated_at_column();
CREATE TRIGGER update_apps_timestamp BEFORE UPDATE ON apps FOR EACH ROW EXECUTE PROCEDURE update_last_updated_at_column();
CREATE TRIGGER update_users_timestamp BEFORE UPDATE ON users FOR EACH ROW EXECUTE PROCEDURE update_last_updated_at_column();
CREATE TRIGGER update_files_timestamp BEFORE UPDATE ON files FOR EACH ROW EXECUTE PROCEDURE update_last_updated_at_column();
CREATE TRIGGER update_actions_timestamp BEFORE UPDATE ON actions FOR EACH ROW EXECUTE PROCEDURE update_last_updated_at_column();
CREATE TRIGGER update_app_memberships_timestamp BEFORE UPDATE ON app_memberships FOR EACH ROW EXECUTE PROCEDURE update_last_updated_at_column();

