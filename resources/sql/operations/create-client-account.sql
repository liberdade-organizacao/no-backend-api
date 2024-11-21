INSERT INTO 
clients(email, password, is_admin) 
VALUES ('%{email}', '%{password}', '%{is_admin}') 
RETURNING id, is_admin;
