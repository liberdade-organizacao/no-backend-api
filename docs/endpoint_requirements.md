# Endpoint Requirements Mapping

This document serves as the source of truth for incoming request validation in the BaaS API. It maps each API route to its required payload, query parameters, and headers.

| Method | Path | Payload/Query/Header Params | Required Params |
|---|---|---|---|
| POST | `/clients/signup` | payload(email, password) | `email`, `password` |
| POST | `/clients/payload` | payload(email, password) | `email`, `password` |
| POST | `/apps` | payload(auth_key, app_name) | `auth_key`, `app_name` |
| GET | `/apps` | query(auth_key) | `auth_key` |
| DELETE | `/apps` | payload(client_auth_key, app_auth_key) | `client_auth_key`, `app_auth_key` |
| POST | `/apps/invite` | payload(inviter_auth_key, app_auth_key, invitee_email, invitee_role) | `inviter_auth_key`, `app_auth_key`, `invitee_email` |
| POST | `/apps/revoke` | payload(revoker_auth_key, app_auth_key, revokee_email) | `revoker_auth_key`, `app_auth_key`, `revokee_email` |
| POST | `/clients/password` | payload(auth_key, old_password, new_password) | `auth_key`, `old_password`, `new_password` |
| DELETE | `/clients` | payload(auth_key, password) | `auth_key`, `password` |
| POST | `/users/signup` | payload(app_auth_key, email, password) | `app_auth_key`, `email`, `password` |
| POST | `/users/login` | payload(app_auth_key, email, password) | `app_auth_key`, `email`, `password` |
| DELETE | `/users` | payload(user_auth_key, password) | `user_auth_key`, `password` |
| POST | `/users/password` | payload(user_auth_key, old_password, new_password) | `user_auth_key`, `old_password`, `new_password` |
| GET | `/apps/users` | query(client_auth_key, app_auth_key) | `client_auth_key`, `app_auth_key` |
| POST | `/users/files` | payload(contents), header(user_auth_key, filename) | `user_auth_key`, `filename` |
| GET | `/users/files` | header(user_auth_key, filename) | `user_auth_key`, `filename` |
| GET | `/users/files/list` | header(auth_key) | `auth_key` |
| DELETE | `/users/files` | header(user_auth_key, filename) | `user_auth_key`, `filename` |
| POST | `/apps/files` | payload(contents), header(client_auth_key, app_auth_key, filename) | `client_auth_key`, `app_auth_key`, `filename` |
| GET | `/apps/files` | header(client_auth_key, app_auth_key, filename) | `client_auth_key`, `app_auth_key`, `filename` |
| DELETE | `/apps/files` | header(client_auth_key, app_auth_key, filename) | `client_auth_key`, `app_auth_key`, `filename` |
| GET | `/apps/files/list` | query(client_auth_key, app_auth_key) | `client_auth_key`, `app_auth_key` |
| GET | `/apps/clients` | header(client_auth_key) | `client_auth_key` |
| POST | `/apps/clients/revoke` | payload(client_auth_key, app_auth_key, email_to_revoke) | `client_auth_key`, `app_auth_key`, `email_to_revoke` |
| POST | `/actions` | payload(client_auth_key, app_auth_key, action_name, action_script) | `client_auth_key`, `app_auth_key`, `action_name`, `action_script` |
| PATCH | `/actions` | payload(client_auth_key, app_auth_key, old_action_name, new_action_name, action_script) | `client_auth_key`, `app_auth_key`, `old_action_name`, `new_action_name`, `action_script` |
| POST | `/actions/bulk` | payload(compressed_actions) | `compressed_actions` |
| GET | `/actions` | query(client_auth_key, app_auth_key, action_name) | `client_auth_key`, `app_auth_key`, `action_name` |
| GET | `/actions/list` | query(client_auth_key, app_auth_key) | `client_auth_key`, `app_auth_key` |
| DELETE | `/actions` | payload(client_auth_key, app_auth_key, action_name) | `client_auth_key`, `app_auth_key`, `action_name` |
| POST | `/actions/run` | payload(user_auth_key, app_auth_key, action_name, action_param) | `user_auth_key`, `app_auth_key`, `action_name`, `action_param` |
| GET | `/clients/all` | header(x-client-auth-key) | `x-client-auth-key` |
| GET | `/apps/all` | header(x-client-auth-key) | `x-client-auth-key` |
| GET | `/files/all` | header(x-client-auth-key) | `x-client-auth-key` |
| GET | `/admins/all` | header(x-client-auth-key) | `x-client-auth-key` |
| POST | `/admins` | payload(auth_key, email) | `auth_key`, `email` |
| DELETE | `/admins` | payload(auth_key, email) | `auth_key`, `email` |
| GET | `/admins/check` | header(x-client-auth-key) | `x-client-auth-key` |
| GET | `/health` | None | None |
