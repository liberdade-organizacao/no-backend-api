INSERT INTO files (filename, filepath, app_id, owner_id, contents)
VALUES (
    '%{filename}',
    '%{filepath}',
    %{app_id},
    %{user_id},
    X'%{contents}'
)
ON CONFLICT (filepath) DO UPDATE
SET contents=X'%{contents}'
RETURNING *;
