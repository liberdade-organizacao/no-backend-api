SELECT 
  f.id,
  f.filename,
  f.filepath,
  f.created_at,
  f.last_updated_at,
  f.app_id,
  f.owner_id,
  a.name,
  c.email
FROM
  files f,
  apps a,
  clients c
WHERE
  f.app_id = a.id AND
  f.owner_id = c.id
;

