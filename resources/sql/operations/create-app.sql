INSERT INTO apps(owner_id, name) 
VALUES (
    '%{owner_id}',
    '%{app_name}'
)
RETURNING *;
