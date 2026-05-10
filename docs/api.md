# BaaS API Endpoints Documentation

This document outlines the available endpoints for the BaaS (Backend as a Service) API, detailing the HTTP method, path, and expected payload parameters for each function. All payloads are expected to be processed via `application/json` or `application/vnd.msgpack`.

## ⚡ Health Check

- **Endpoint:** `/health`
- **Method:** `GET`
- **Description:** Checks the overall operational status of the API, Database, and scripting engines.
- **Payload:** None (Reads headers for context).
- **Success Response:** Includes status for `api`, `db`, `scripting`, and `version`.

## Client Management Endpoints

### Client Sign Up
- **Endpoint:** `/clients/signup`
- **Method:** `POST`
- **Description:** Registers a new client account.
- **Payload:**
  - `email`: (string) The unique email address of the client.
  - `password`: (string) The initial password for the client.
- **Required Headers:** None.

### Client Login
- **Endpoint:** `/clients/login`
- **Method:** `POST`
- **Description:** Authenticates an existing client.
- **Payload:**
  - `email`: (string) The client's registered email.
  - `password`: (string) The client's password.
- **Required Headers:** None.

### Change Client Password
- **Endpoint:** `/clients/password`
- **Method:** `POST`
- **Description:** Allows a client to change their own password.
- **Payload:**
  - `auth_key`: (string) The client's authentication key/token.
  - `old_password`: (string) The current password.
  - `new_password`: (string) The desired new password.
- **Required Headers:** None.

### Delete Client
- **Endpoint:** `/clients`
- **Method:** `DELETE`
- **Description:** Deletes a client account.
- **Payload:**
  - `auth_key`: (string) The client's authentication key/token.
  - `password`: (string) The client's password (for confirmation).
- **Required Headers:** None.

### List All Clients
- **Endpoint:** `/clients/all`
- **Method:** `GET`
- **Description:** Retrieves a list of all registered clients.
- **Payload:** None.
- **Required Headers:** `x-client-auth-key` (Authentication Token).

## Application Management Endpoints

### Create Application
- **Endpoint:** `/apps`
- **Method:** `POST`
- **Description:** Creates a new operational application instance.
- **Payload:**
  - `auth_key`: (string) The client's authentication key/token.
  - `app_name`: (string) The name of the new application.
- **Required Headers:** None.

### List Applications
- **Endpoint:** `/apps`
- **Method:** `GET`
- **Description:** Retrieves a list of applications associated with the authenticated client. Supports filtering by `auth_key` via query parameters.
- **Payload:** None.
- **Required Headers:** None.
- **Query Params:** `auth_key` (Optional filter key).

### Delete Application
- **Endpoint:** `/apps`
- **Method:** `DELETE`
- **Description:** Marks an application for deletion.
- **Payload:**
  - `client_auth_key`: (string) The authorizing client's key/token.
  - `app_auth_key`: (string) The application's specific key/token.
- **Required Headers:** None.

### Invite User to App
- **Endpoint:** `/apps/invite`
- **Method:** `POST`
- **Description:** Invites a new user to an application with a specified role.
- **Payload:**
  - `inviter_auth_key`: (string) The inviter's key/token.
  - `app_auth_key`: (string) The target application's key/token.
  - `invitee_email`: (string) The email of the user to invite.
  - `invitee_role`: (string, default: `contributor`) The role assigned (e.g., 'contributor', 'admin').
- **Required Headers:** None.

### Revoke User Access to App
- **Endpoint:** `/apps/revoke`
- **Method:** `POST`
- **Description:** Revokes a specific user's access permissions from an application.
- **Payload:**
  - `revoker_auth_key`: (string) The revoker's key/token.
  - `app_auth_key`: (string) The target application's key/token.
  - `revokee_email`: (string) The email of the user whose access is revoked.
- **Required Headers:** None.

### List App Users
- **Endpoint:** `/apps/users`
- **Method:** `GET`
- **Description:** Listens all users associated with a specific application.
- **Payload:** None.
- **Required Query Params:** `client_auth_key` (Client Key), `app_auth_key` (Application Key).

### List All Applications
- **Endpoint:** `/apps/all`
- **Method:** `GET`
- **Description:** Retrieves a list of all applications (for administrative or system-level inspection).
- **Payload:** None.
- **Required Headers:** `x-client-auth-key` (Admin Authentication Token).

## User Management Endpoints

### User Sign Up
- **Endpoint:** `/users/signup`
- **Method:** `POST`
- **Description:** Creates a new user identity bound to a specific application.
- **Payload:**
  - `app_auth_key`: (string) The application's key/token authorizing the user creation.
  - `email`: (string) The user's email.
  - `password`: (string) The user's password.
- **Required Headers:** None.

### User Login
- **Endpoint:** `/users/login`
- **Method:** `POST`
- **Description:** Authenticates an application user.
- **Payload:**
  - `app_auth_key`: (string) The application's key/token.
  - `email`: (string) The user's email.
  - `password`: (string) The user's password.
- **Required Headers:** None.

### Delete User
- **Endpoint:** `/users`
- **Method:** `DELETE`
- **Description:** Deletes a user identity within the application context.
- **Payload:**
  - `user_auth_key`: (string) The user's authentication key/token.
  - `password`: (string) The user's password (for confirmation).
- **Required Headers:** None.

### Update User Password
- **Endpoint:** `/users/password`
- **Method:** `POST`
- **Description:** Allows a user to change their own password.
- **Payload:**
  - `user_auth_key`: (string) The user's authentication key/token.
  - `old_password`: (string) The current password.
  - `new_password`: (string) The desired new password.
- **Required Headers:** None.

### List User Files
- **Endpoint:** `/users/files/list`
- **Method:** `GET`
- **Description:** Lists all files uploaded by a specific user.
- **Payload:** None.
- **Required Headers:** `x-user-auth-key` (User Authentication Token).

### Upload User File
- **Endpoint:** `/users/files`
- **Method:** `POST`
- **Description:** Uploads a file generated by a user.
- **Payload:**
  - `user_auth_key`: (Header) The user's authentication key/token.
  - `x-filename`: (Header) The name of the file.
  - `body`: (bytes/string) The raw content of the file.
- **Required Headers:** `x-user-auth-key`, `x-filename`.

### Download User File
- **Endpoint:** `/users/files`
- **Method:** `GET`
- **Description:** Downloads a specific file uploaded by a user.
- **Payload:** None.
- **Required Headers:** `x-user-auth-key` (User Authentication Token), `x-filename` (File Name).

### Delete User File
- **Endpoint:** `/users/files`
- **Method:** `DELETE`
- **Description:** Deletes a specific file uploaded by a user.
- **Payload:** None.
- **Required Headers:** `x-user-auth-key` (User Authentication Token), `x-filename` (File Name).

### List All User Files
- **Endpoint:** `/files/all`
- **Method:** `GET`
- **Description:** Retrieves a listing of all stored files across the system.
- **Payload:** None.
- **Required Headers:** `x-user-auth-key` (Admin/System Key).

## App File Management Endpoints

### Upload App File
- **Endpoint:** `/apps/files`
- **Method:** `POST`
- **Description:** Uploads a file related to a specific application.
- **Payload:**
  - `x-client-auth-key`: (Header) Client's key/token.
  - `x-app-auth-key`: (Header) Application's key/token.
  - `x-filename`: (Header) The name of the file.
  - `body`: (bytes/string) The raw content of the file.
- **Required Headers:** `x-client-auth-key`, `x-app-auth-key`, `x-filename`.

### Download App File
- **Endpoint:** `/apps/files`
- **Method:** `GET`
- **Description:** Downloads a specific file related to an application.
- **Payload:** None.
- **Required Headers:** `x-client-auth-key`, `x-app-auth-key`, `x-filename`.

### Delete App File
- **Endpoint:** `/apps/files`
- **Method:** `DELETE`
- **Description:** Deletes a specific file associated with an application.
- **Payload:** None.
- **Required Headers:** `x-client-auth-key`, `x-app-auth-key`, `x-filename`.

### List App Files
- **Endpoint:** `/apps/files/list`
- **Method:** `GET`
- **Description:** Lists all files associated with an application.
- **Payload:** None.
- **Required Query Params:** `client_auth_key` (Client Key), `app_auth_key` (Application Key).

### List All App Files
- **Endpoint:** `/files/all`
- **Method:** `GET`
- **Description:** Retrieves a listing of all application-related files.
- **Payload:** None.
- **Required Headers:** `x-client-auth-key` (Admin/System Key).

## Action Management & Automation

### Define Action (Upsert)
- **Endpoint:** `/actions`
- **Method:** `POST`
- **Description:** Uploads or updates an automated action script definition for an application.
- **Payload:**
  - `client_auth_key`: (string) Client's key/token.
  - `app_auth_key`: (string) Application's key/token.
  - `action_name`: (string) The unique name for the action.
  - `action_script`: (string) The actual script content.
- **Required Headers:** None.

### Modify Action
- **Endpoint:** `/actions`
- **Method:** `PATCH`
- **Description:** Updates the definition (name or script) of an existing action.
- **Payload:**
  - `client_auth_key`: (string) Client's key/token.
  - `app_auth_key`: (string) Application's key/token.
  - `old_action_name`: (string) The previous name of the action.
  - `new_action_name`: (string) The desired new name.
  - `action_script`: (string) The updated script content.
- **Required Headers:** None.

### Bulk Action Upload
- **Endpoint:** `/actions/bulk`
- **Method:** `POST`
- **Description:** Uploads a compressed archive of multiple action scripts and definitions.
- **Payload:** (Raw contents of the compressed archive).
- **Required Headers:** `x-client-auth-key`, `x-app-auth-key`.

### Read Action
- **Endpoint:** `/actions`
- **Method:** `GET`
- **Description:** Retrieves the definition of a specific action script.
- **Payload:** None.
- **Required Query Params:** `client_auth_key` (Client Key), `app_auth_key` (Application Key), `action_name` (Action Name).

### List Actions
- **Endpoint:** `/actions/list`
- **Method:** `GET`
- **Description:** Lists all defined actions for an application.
- **Payload:** None.
- **Required Query Params:** `client_auth_key` (Client Key), `app_auth_key` (Application Key).

### Delete Action
- **Endpoint:** `/actions`
- **Method:** `DELETE`
- **Description:** Permanently deletes an action script definition.
- **Payload:**
  - `client_auth_key`: (string) Client's key/token.
  - `app_auth_key`: (string) Application's key/token.
  - `action_name`: (string) The name of the action to delete.
- **Required Headers:** None.

### Run Action
- **Endpoint:** `/actions/run`
- **Method:** `POST`
- **Description:** Executes a defined action script.
- **Payload:**
  - `user_auth_key`: (string) The user initiating the action.
  - `app_auth_key`: (string) The targeted application key.
  - `action_name`: (string) The name of the action to run.
  - `action_param`: (string) Parameters required by the action script.
- **Required Headers:** None.

## Administration & Role Management

### List App Managers
- **Endpoint:** `/apps/clients`
- **Method:** `GET`
- **Description:** Lists all users/clients who have manager rights within the scope of a specific application.
- **Payload:** None.
- **Required Query Params:** `client_auth_key` (Client Key), `app_auth_key` (Application Key).

### Revoke Admin Access
- **Endpoint:** `/apps/clients/revoke`
- **Method:** `POST`
- **Description:** Revokes administrator or elevated privileges from a specified user account within an application.
- **Payload:**
  - `client_auth_key`: (string) The revoker's key/token.
  - `app_auth_key`: (string) The target application's key/token.
  - `email_to_revoke`: (string) The email of the user losing admin status.
- **Required Headers:** None.

### Promote User to Admin
- **Endpoint:** `/admins`
- **Method:** `POST`
- **Description:** Grants administrator privileges to a user account.
- **Payload:**
  - `auth_key`: (string) The authorizer's key/token.
  - `email`: (string) The email of the user to promote.
- **Required Headers:** None.

### Demote Admin
- **Endpoint:** `/admins`
- **Method:** `DELETE`
- **Description:** Revokes administrator privileges from a user account.
- **Payload:**
  - `auth_key`: (string) The authorizer's key/token.
  - `email`: (string) The email of the user to demote.
- **Required Headers:** None.

### Check Admin Status
- **Endpoint:** `/admins/check`
- **Method:** `GET`
- **Description:** Checks the administration status of the current context user.
- **Payload:** None.
- **Required Headers:** None.

## Core Utility Endpoints

### List All Admins (System Wide)
- **Endpoint:** `/admins/all`
- **Method:** `GET`
- **Description:** Lists system administrators across the entire BaaS instance.
- **Payload:** None.
- **Required Headers:** `x-client-auth-key` (Admin Key).

### List All Generic Resources

All clients:

- **Endpoint:** `/clients/all`
- **Method:** `GET`
- **Description:** Lists all client accounts.
- **Payload:** None.
- **Required Headers:** `x-client-auth-key` (Admin Key).

All apps:

- **Endpoint:** `/apps/all`
- **Method:** `GET`
- **Description:** Lists all application definitions.
- **Payload:** None.
- **Required Headers:** `x-client-auth-key` (Admin Key).

All files:

- **Endpoint:** `/files/all`
- **Method:** `GET`
- **Description:** Lists all stored files globally (user and app files combined).
- **Payload:** None.
- **Required Headers:** `x-client-auth-key` (Admin Key).

### General Note on Headers

Multiple endpoints rely on custom HTTP headers for authentication and file context, such as:
- `x-client-auth-key`: Key identifying the originating client.
- `x-app-auth-key`: Key identifying the application context.
- `x-user-auth-key`: Key identifying the logged-in user for file operations.
- `x-filename`: The name of the file being operated on.
