CREATE TABLE IF NOT EXISTS files (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    filename VARCHAR(64) NOT NULL,
    filepath VARCHAR(128) NOT NULL UNIQUE,
    contents BYTEA,
    file_size INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    last_updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    app_id INTEGER NOT NULL REFERENCES apps(id) ON DELETE CASCADE,
    owner_id INTEGER REFERENCES users(id) ON DELETE CASCADE
);
