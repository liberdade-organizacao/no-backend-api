CREATE TABLE IF NOT EXISTS clients (
    id SERIAL PRIMARY KEY,
    email VARCHAR(32) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
    is_admin BOOLEAN NOT NULL DEFAULT false,
    auth_key TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp NOT NULL,
    last_updated_at TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp NOT NULL
);