INSERT INTO users(app_id, email, password) VALUES('%{app_id}', '%{email}', '%{password}') RETURNING *;
