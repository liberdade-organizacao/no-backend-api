INSERT INTO files (filename, filepath, app_id, owner_id, contents)
VALUES (
    '%{filename}',
    '%{filepath}',
    %{app_id},
    %{user_id},
    E'%{contents}'
)
ON CONFLICT (filepath) DO UPDATE
SET contents=E'%{contents}'
RETURNING *;
