INSERT INTO users(app_id, email, password) 
VALUES('%{app_id}', '%{email}', '%{password}') 
ON CONFLICT DO NOTHING
RETURNING *;
