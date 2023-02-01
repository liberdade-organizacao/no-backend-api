DELETE FROM app_memberships 
WHERE client_id=(SELECT id FROM clients WHERE email='%{revoked_email}')
AND app_id='%{app_id}'
RETURNING *;
