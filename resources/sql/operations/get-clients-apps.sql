SELECT id, name FROM apps
WHERE id IN (
    SELECT app_id FROM app_memberships WHERE app_memberships.client_id='%{client_id}'
);
