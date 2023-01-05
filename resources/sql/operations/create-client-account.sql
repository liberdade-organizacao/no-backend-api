INSERT INTO 
clients(email, password, is_admin, auth_key) 
VALUES ('%{email}', '%{password}', '%{is_admin}', '%{auth_key}') 
RETURNING *;
