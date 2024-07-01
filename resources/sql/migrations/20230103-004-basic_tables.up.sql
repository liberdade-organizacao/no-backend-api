

CREATE TABLE IF NOT EXISTS actions (
    id SERIAL PRIMARY KEY,
    app_id INT NOT NULL REFERENCES apps(id) ON DELETE CASCADE,
    name VARCHAR(32) NOT NULL,
    script TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    last_updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp
);