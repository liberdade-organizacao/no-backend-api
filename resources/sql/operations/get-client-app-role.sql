SELECT * FROM app_memberships 
WHERE app_id=(
    SELECT id FROM apps 
    WHERE owner_id=(
      SELECT id FROM clients
      WHERE email='%{owner_email}'
    )
    AND name='%{app_name}'
) AND client_id=(
    SELECT id FROM clients 
    WHERE email='%{client_email}'
);
