INSERT INTO 
apps(owner_id, name, auth_key) 
VALUES(
    (SELECT id FROM clients WHERE email='%{owner_client_email}'),
    '%{app_name}',
    '%{auth_key}'
)
RETURNING *;
