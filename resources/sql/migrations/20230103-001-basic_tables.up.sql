

CREATE TABLE IF NOT EXISTS apps (
    id SERIAL PRIMARY KEY,
    owner_id INT NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    name VARCHAR(32) NOT NULL,
    auth_key TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    last_updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    UNIQUE(owner_id, name)
);