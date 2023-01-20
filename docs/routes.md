# Routes

Fundamental entities:
- Clients
  - The people that are going to use the no-backend server directly
  - For each app, clients can either be "admins" or "contributors"
- Apps
  - An app has access to its own clients to manage, users, files, and actions
- Users
  - The people that are going to use the apps hosted in this service
- Files
  - Each user has access to a small file storage within an app
  - Each file is identified by an unique path that contains the app's id,
    the ussr's id, and the file name
- Actions
  - Scripts that let clients implement custom features for their apps
  - Actions are written in a subset of Lua and required the 
    [scripting engine](https://github.com/liberdade-organizacao/no-backend-scripting-engine)
    to be executed
  - Actions' Lua scripts should contain a `main` function that receive a
    string and return another string. For example:

```
function main(name)
 return "hello " .. name .. "!"
end
```

# Routes API

## `GET /health`

- Quick way to verify if the app is running properly
- Parameters: None
- Returns: `"ok"` if the app is running; or an appropriate error message

## `POST /clients/signup`

- Let clients sign up
- Parameters:
  - `email`: client email
  - `password`: client password
- Returns:
  - `auth_key`: string with the client's auth key; or `null` if the account
    is not created
  - `error`: `null` if the client was properly created, or an appropriate
    error message

## `POST /clients/login`

- Let clients log in
- Parameters:
  - `email`: client email
  - `password`: client password
- Returns:
  - `auth_key`: string with the client's auth key; or `null` is the account
    could not log in
  - `error`: `null` if the client was properly created; or an appropriate
    error message

## `POST /apps`

- Let clients create apps
- Parameters:
  - `auth_key`: client auth key
  - `name`: app's name
- Returns:
  - `auth_key`: app's auth key, or `null` if the app could not be created
  - `error`: `null` if the app was properly created, or an error message

## `GET /apps`

- Let clients list their apps
- Parameters:
  - `auth_key`: client auth key
- Returns:
  - `apps`: a list of all of their apps
  - `error`: `null` or appropriate error message

## `DELETE /apps`

- Let clients delete an app
- Parameters:
  - `client_auth_key`: client auth key
  - `app_auth_key`: app auth key
- Returns:
  - `error`: `null` or appropriate error message

## `POST /apps/invite`

- Let other clients manage an app
- Parameters:
  - `inviter_auth_key`: inviter client's auth key
  - `app_auth_key`: app's auth key
  - `invitee_email`: invited client's email
  - `invitee_role`: invited client's role. It should be either "admin" or
    "contributor". If not included, defaults to "contributor".
- Returns:
  - `error`: `null` or appropriate error message

## `POST /clients/password`

- Let clients change their password
- Parameters:
  - `auth_key`: client's auth key
  - `old_password`: client's old password
  - `new_password`: client's new password
- Returns:
  - `error`: `null` or appropriate error message

## `DELETE /clients`

- Let clients delete their accounts 
- Parameters:
  - `auth_key`: client's auth key
  - `password`: client's password
- Returns:
  - `error`: `null` or appropriate error message

## `POST /users/signup`

- Let users create an account in an app
- Parameters:
  - `app_auth_key`: app's auth key
  - `email`: user's email
  - `password`: user's password
- Returns:
  - `error`: `null` or appropriate error message
  - `auth_key`: user's auth key

## `POST /users/login`

- Let users login
- Parameters:
  - `app_auth_key`: app's auth key
  - `email`: user's email
  - `password`: user's password
- Returns:
  - `error`: `null` or appropriate error message
  - `auth_key`: user's auth key

## `DELETE /users`

- Let users delete their accounts in an app
- Parameters:
  - `user_auth_key`: user's auth key
  - `password`: user's password
- Returns:
  - `error`: `null` or appropriate error message

## `POST /users/password`

- Let users change their password
- Parameters:
  - `user_auth_key`: user's auth key
  - `old_password`: user's old password
  - `new_password`: user's new password
- Returns:
  - `error`: `null` or appropriate error message

## `POST /users/files`

- Let users upload files
- Headers:
  - `X-USER-AUTH-KEY`: user's auth key
  - `X-FILENAME`: file name
- Body: the file's contents
- Returns:
  - `error`: `null` or appropriate error message

## `GET /users/files`

- Let users download files
- Headers:
  - `X-USER-AUTH-KEY`: user's auth key
  - `X-FILENAME`: file name
- Returns:
  - The file's contents, or an empty string if something happens

## `GET /users/files/list`

- Let users list their files in an app
- Headers:
  - `X-USER-AUTH-KEY`: user's auth key
- Returns:
  - `files`: a list of file names
  - `error`: `null` or an appropriate error message

## `DELETE /users/files`

- Let users delete their files
- Headers:
  - `X-USER-AUTH-KEY`: user's auth key
  - `X-FILENAME`: file name
- Returns:
  - `error`: `null` or an appropriate error message

## `GET apps/files/list`

- Let clients list all files in an app
- Parameters:
  - `client_auth_key`: client's auth key
  - `app_auth_key`: app's auth key
- Returns:
  - `files`: a list of file paths
  - `error`: `null` or an appropriate error message

## `POST /actions`

- Let clients upload actions
  - Note that uploading actions with the same name will override the previous ones!
- Parameters:
  - `client_auth_key`: client's auth key
  - `app_auth_key`: app's auth key
  - `action_name`: action name (preferably the Lua script file name)
  - `action_content`: Lua script
- Returns:
  - `error`: `null` or an appropriate error message

## `GET /actions`

- Let clients download actions
- Parameters:
  - `client_auth_key`: client auth key
  - `app_auth_key`: app auth key
  - `action_name`: action name
- Returns:
  - `script`: action's contents

## `GET /actions/list`

- Let clients list all actions within an app
- Parameters:
  - `client_auth_key`: client auth key
  - `app_auth_key`: app auth key
- Returns:
  - `actions`: list of action names
  - `error`: `null` or appropriate error message

## `DELETE /actions`

- Let clients delete an action
- Parameters:
  - `client_auth_key`: client auth key
  - `app_auth_key`: app auth key
  - `action_name`: action name
- Returns:
  - `error`: `null` or appropriate error message

## `POST /actions/run`

- Let clients run actions
- Parameters:
  - `client_auth_key`: client auth key
  - `app_auth_key`: app auth key
  - `action_name`: action name
  - `action_param`: input string for the action
- Returns:
  - `error`: `null` or appropriate error message
  - `result`: output string from the action

