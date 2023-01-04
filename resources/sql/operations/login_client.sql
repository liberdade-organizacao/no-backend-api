SELECT email, is_admin, auth_key
FROM clients
WHERE email='%s' AND password=crypt('%s', '%s');

