CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    app_id INT NOT NULL REFERENCES apps(id) ON DELETE CASCADE,
    email VARCHAR(32) NOT NULL,
    password VARCHAR(128) NOT NULL,
    auth_key TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    last_updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    UNIQUE(app_id, email)
);