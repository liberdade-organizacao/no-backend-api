INSERT INTO actions(app_id, name, script) 
VALUES ('%{app_id}', '%{name}', '%{script}') 
ON CONFLICT (app_id, name) DO 
UPDATE SET script='%{script}'
RETURNING *;
