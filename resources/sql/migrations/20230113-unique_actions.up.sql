ALTER TABLE actions ADD CONSTRAINT unique_action_name UNIQUE(app_id, name);
