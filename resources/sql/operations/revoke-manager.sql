UPDATE app_memberships 
SET role='contributor'
WHERE app_id='%{app_id}'
AND client_id=(SELECT id FROM clients WHERE email='%{revoked_email}')
RETURNING *;

