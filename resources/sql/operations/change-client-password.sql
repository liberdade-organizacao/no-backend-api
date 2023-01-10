UPDATE clients 
SET password='%{new_password}' 
WHERE id='%{client_id}' AND password='%{old_password}' 
RETURNING *;
