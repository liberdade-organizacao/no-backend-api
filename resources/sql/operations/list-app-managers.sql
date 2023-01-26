SELECT email FROM clients WHERE id=ANY(
    SELECT client_id FROM app_memberships WHERE app_id='%{app_id}'
);
