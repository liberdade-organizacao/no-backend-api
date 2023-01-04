INSERT INTO 
clients(email, password, is_admin) 
VALUES('%s', '%s', '%s') 
RETURNING *;
