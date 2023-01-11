DELETE FROM users WHERE id='%{user_id}' AND password='%{password}' RETURNING *;
