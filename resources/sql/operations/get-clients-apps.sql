SELECT app_id FROM app_memberships 
WHERE client_id=(
    SELECT id FROM clients
    WHERE email='%{email}'
);
