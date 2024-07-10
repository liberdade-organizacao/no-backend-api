INSERT INTO files (filename, filepath, app_id, owner_id, file_size)
VALUES (
    '%{filename}',
    '%{filepath}',
    %{app_id},
    %{user_id},
    %{file_size}
)
ON CONFLICT (filepath) DO UPDATE
SET file_size=%{file_size}
RETURNING *;
