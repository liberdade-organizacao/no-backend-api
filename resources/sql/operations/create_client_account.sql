INSERT INTO 
clients(email, password, is_admin, auth_key) 
VALUES('%s', '%s', '%s', '%s') 
RETURNING *;
