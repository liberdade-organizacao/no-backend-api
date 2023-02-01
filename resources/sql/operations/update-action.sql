UPDATE actions
SET name='%{new_name}',
script='%{script}'
WHERE app_id='%{app_id}'
AND name='%{old_name}';
