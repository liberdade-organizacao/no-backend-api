CREATE TABLE IF NOT EXISTS app_memberships (
    id SERIAL PRIMARY KEY,
    app_id INT NOT NULL REFERENCES apps(id) ON DELETE CASCADE,
    client_id INT NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    last_updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp
);