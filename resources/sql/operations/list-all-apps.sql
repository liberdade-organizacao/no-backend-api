SELECT app.*, client.email
FROM apps app, clients client
WHERE app.owner_id = client.id;
