UPDATE clients SET is_admin='off' WHERE email='%{email}' RETURNING *;
