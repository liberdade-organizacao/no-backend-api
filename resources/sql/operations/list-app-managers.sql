SELECT clients.email, app_memberships.role
FROM clients, app_memberships
WHERE app_memberships.app_id='%{app_id}'
AND app_memberships.client_id=clients.id;
