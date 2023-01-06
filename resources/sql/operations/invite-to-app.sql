INSERT INTO
app_memberships(app_id, client_id, role)
VALUES(
	'%{app_id}', 
	'%{client_id}', 
	'%{role}'
)
RETURNING *;
