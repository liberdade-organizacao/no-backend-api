UPDATE clients SET is_admin='on' WHERE email='%{email}' RETURNING *;
