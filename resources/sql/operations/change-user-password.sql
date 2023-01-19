UPDATE users
SET password='%{new_password}'
WHERE id='%{user_id}' AND app_id='%{app_id}' AND password='%{old_password}'
RETURNING *;
