SELECT files.filepath, users.email
FROM files, users
WHERE files.app_id='%{app_id}'
AND files.owner_id=users.id;
