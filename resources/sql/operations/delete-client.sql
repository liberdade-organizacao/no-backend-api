DELETE FROM clients WHERE id='%{id}' AND password='%{password}' RETURNING *;
