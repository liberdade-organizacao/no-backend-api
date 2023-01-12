SELECT filename FROM files 
WHERE owner_id='%{user_id}' AND app_id='%{app_id}';
