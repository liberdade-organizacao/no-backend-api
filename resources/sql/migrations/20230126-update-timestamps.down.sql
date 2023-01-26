DROP TRIGGER IF EXISTS update_app_memberships_timestamp ON app_memberships CASCADE;
DROP TRIGGER IF EXISTS update_files_timestamp ON files;
DROP TRIGGER IF EXISTS update_actions_timestamp ON actions;
DROP TRIGGER IF EXISTS update_users_timestamp ON users;
DROP TRIGGER IF EXISTS update_apps_timestamp ON apps;
DROP TRIGGER IF EXISTS update_clients_timestamp ON clients;
DROP FUNCTION IF EXISTS update_last_updated_at_column CASCADE;
