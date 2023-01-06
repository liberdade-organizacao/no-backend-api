DELETE FROM apps 
WHERE id=(
  SELECT id FROM apps
  WHERE owner_id=(
      SELECT id FROM clients
      WHERE email='%{client_email}'
  )
  AND name='%{app_name}'
)
RETURNING *;
