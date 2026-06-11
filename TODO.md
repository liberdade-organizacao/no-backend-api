# Behavioral Integration Test Plan

## Overview

Rewrite the unit-style tests from `test/br/bsb/liberdade/baas/business_test.clj` as behavioral integration tests that:
- Start the application server on a random port in a background thread per test
- Interact exclusively via HTTP API calls (using `clj-http`, already available)
- Verify both API response bodies and database state after each action

The new suite will live in: `test/br/bsb/liberdade/baas/integration_test.clj`

After implementation, the existing integration tests under `integration/` will be deprecated and removed.

---

## Step 1: Create helper namespace for HTTP API wrappers and test fixtures

**File:** `test/br/bsb/liberdade/baas/test_helpers.clj`

Create a namespace with two sets of helpers: (1) server lifecycle management, and (2) HTTP wrapper functions mirroring each API endpoint.

### New requires
- `[clojure.test :refer [deftest testing is]]`
- `[clj-http.client :as http]`
- `[clojure.data.json :as json]`
- `[br.bsb.liberdade.baas.api :as api]`
- `[br.bsb.liberdade.baas.db :as db]`

### Server lifecycle helpers

Define these functions to manage starting/stopping the server per-test:

| Function | Purpose |
|---|---|
| `start-server []` | Calls `(db/setup-database)` and `(db/run-migrations)`, then picks a free port via `(java.net.ServerSocket. 0)`, stores the port in a var, and starts the server in a background thread using `(api/run)` logic (adapted to bind to that port). Returns the base URL string `"http://localhost:<port>"`. |
| `stop-server [base-url]` | Kills the background thread ref. Calls `(db/drop-database)`. Cleans up temp database file if used. |
| `wait-for-server [base-url]` | Polls `base-url/health` until it responds with status 200, retrying up to 10 times with 1s sleep between retries. |

### Fixture function

Define `(integration-fixture [f])` that wraps each test:
1. Sets `DATABASE_FILE` env var to a unique temp file path (e.g., using UUID)
2. Forces re-loading of `db` namespace so it picks up the new DB file path
3. Calls `(start-server)` and waits until ready
4. Runs `f` inside try/finally, calling `(stop-server)` in finally block

### HTTP wrapper functions

Each wrapper takes `base-url` as first argument, constructs the full URL, makes the HTTP call, and parses JSON responses. Add all of these:

| Wrapper | Route | Method | Params (JSON body / query) / Headers |
|---|---|---|---|
| `signup-client` | `/clients/signup` | POST | body: `{email, password}` |
| `login-client` | `/clients/login` | POST | body: `{email, password}` |
| `create-app` | `/apps` | POST | body: `{auth_key, app_name}` |
| `list-apps` | `/apps` | GET | query: `{auth_key}` |
| `delete-app` | `/apps` | DELETE | body: `{client_auth_key, app_auth_key}` |
| `invite-to-app` | `/apps/invite` | POST | body: `{inviter_auth_key, app_auth_key, invitee_email, invitee_role}` |
| `revoke-from-app` | `/apps/revoke` | POST | body: `{revoker_auth_key, app_auth_key, revokee_email}` |
| `change-client-password` | `/clients/password` | POST | body: `{auth_key, old_password, new_password}` |
| `delete-client` | `/clients` | DELETE | body: `{auth_key, password}` |
| `signup-user` | `/users/signup` | POST | body: `{app_auth_key, email, password}` |
| `login-user` | `/users/login` | POST | body: `{app_auth_key, email, password}` |
| `delete-user` | `/users` | DELETE | body: `{user_auth_key, password}` |
| `change-user-password` | `/users/password` | POST | body: `{user_auth_key, old_password, new_password}` |
| `list-app-users` | `/apps/users` | GET | query: `{client_auth_key, app_auth_key}` |
| `upload-user-file` | `/users/files` | POST | headers: `{x-user-auth-key, x-filename}`, body: raw content |
| `download-user-file` | `/users/files` | GET | headers: `{x-user-auth-key, x-filename}`, returns raw body |
| `list-user-files` | `/users/files/list` | GET | header: `{x-user-auth-key}` |
| `delete-user-file` | `/users/files` | DELETE | headers: `{x-user-auth-key, x-filename}` |
| `upload-app-file` | `/apps/files` | POST | headers: `{x-client-auth-key, x-app-auth-key, x-filename}`, body: raw content |
| `download-app-file` | `/apps/files` | GET | headers: `{x-client-auth-key, x-app-auth-key, x-filename}`, returns raw body |
| `delete-app-file` | `/apps/files` | DELETE | headers: `{x-client-auth-key, x-app-auth-key, x-filename}` |
| `list-app-files` | `/apps/files/list` | GET | query: `{client_auth_key, app_auth_key}` |
| `list-app-managers` | `/apps/clients` | GET | query: `{client_auth_key, app_auth_key}` |
| `revoke-app-manager` | `/apps/clients/revoke` | POST | body: `{client_auth_key, app_auth_key, email_to_revoke}` |
| `create-action` | `/actions` | POST | body: `{client_auth_key, app_auth_key, action_name, action_script}` |
| `read-action` | `/actions` | GET | query: `{client_auth_key, app_auth_key, action_name}`, returns raw body |
| `list-actions` | `/actions/list` | GET | query: `{client_auth_key, app_auth_key}` |
| `update-action` | `/actions` | PATCH | body: `{client_auth_key, app_auth_key, old_action_name, new_action_name, action_script}` |
| `delete-action` | `/actions` | DELETE | body: `{client_auth_key, app_auth_key, action_name}` |
| `list-all-clients` | `/clients/all` | GET | header: `{x-client-auth-key}` |
| `list-all-apps` | `/apps/all` | GET | header: `{x-client-auth-key}` |
| `list-all-files` | `/files/all` | GET | header: `{x-client-auth-key}` |
| `list-all-admins` | `/admins/all` | GET | header: `{x-client-auth-key}` |
| `promote-to-admin` | `/admins` | POST | body: `{auth_key, email}` |
| `demote-admin` | `/admins` | DELETE | body: `{auth_key, email}` |
| `check-is-admin` | `/admins/check` | GET | header: `{x-client-auth-key}` |

**Return conventions:** Wrappers for JSON endpoints return parsed clojure maps. Wrappers returning raw content (`download-user-file`, `download-app-file`, `read-action`) return the raw string body without JSON parsing.

### Random helper
- `random-email []` — generates unique email addresses using random strings, e.g. `"test_a1b2c3@example.net"`

---

## Step 2: Create database query helpers for state verification

**File:** `test/br/bsb/liberdade/baas/test_helpers.clj` (same file, section after HTTP helpers)

Add direct DB query functions that use `(db/execute-query ...)` to verify database state. Each test can call these after API calls to confirm persistence.

| Function | SQL Pattern | Returns |
|---|---|---|
| `db-count-clients` | `SELECT COUNT(*) FROM clients` | integer |
| `db-get-client-by-email [email]` | `SELECT * FROM clients WHERE email=?` | map or nil |
| `db-count-apps` | `SELECT COUNT(*) FROM apps` | integer |
| `db-get-app-by-auth-key [auth_key]` | query via auth_keys table join | map or nil |
| `db-get-app_by_name_owner [name owner_email]` | `SELECT ... FROM apps WHERE name=? AND app_client_id=(SELECT id FROM clients WHERE email=?)` | map or nil |
| `db-count-users [app-auth-key]` | join users to app via app_auth_keys | integer |
| `db-get-user-by-email-app [email app-id]` | join users + app | map or nil |
| `db-count-files` | `SELECT COUNT(*) FROM files` | integer |
| `db-get-file-by-name-and-user [filename user-id]` | query files table | map or nil |
| `db-count-app_files` | `SELECT COUNT(*) FROM app_files` | integer |
| `db-get_app_file_by_name_and_app [filename app-id]` | query app_files table | map or nil |
| `db-count-invites [app-auth-key]` | count entries in invites/app_permissions | integer |
| `db-has-role_for_client [client-email, app-id, role]` | check invite role for client in app | boolean |
| `db-is-admin [email]` | `SELECT is_admin FROM clients WHERE email=?` | boolean (true/false) |
| `db-count-actions [app-auth-key]` | count entries in actions table | integer |
| `db-get_action_by_name_app [action-name app-id]` | query actions table | map or nil |

These functions give tests the ability to verify: "after I call this API endpoint, did the database actually change?"

---

## Step 3: Write behavioral test file

**File:** `test/br/bsb/libedere/baas/integration_test.clj`

Create a new test namespace that uses the fixture and helpers. Group tests by feature area, matching the scenarios in the existing `business_test.clj`. Use `(use-fixtures :each integration-fixture)`.

Each test should:
1. Call the HTTP wrapper functions against the running server
2. Check API response bodies for correct fields/status codes
3. Call DB verification helpers to confirm consistency

### Test groups (one test per behavioral scenario)

#### Client Account Management

| # | Deftest Name | Scenario |
|---|---|---|
| 1 | `client-signup-and-login` | Create account, login, verify auth_key is returned and matches across logins. Check DB: client row exists. |
| 2 | `client-login-with-wrong-password` | Create account, try login with wrong password. Verify API returns error, no auth_key. Check DB: client still exists (not deleted). |
| 3 | `duplicate-client-signup` | Create account twice with same email. First succeeds, second fails with error. Check DB: only one client row. |

#### App CRUD

| # | Deftest Name | Scenario |
|---|---|---|
| 4 | `client-create-and-delete-app` | Login as client, create app, verify in list, delete, verify gone from list. Check DB: app row created then removed. |
| 5 | `app-unique-name-per-owner` | Create two apps with same name for same owner — second fails. Create another client with same app name — succeeds. Check DB: only one app per owner+name combo. |
| 6 | `unauthorized-app-deletion` | Client A creates app, Client B tries to delete it. Verify error response. Check DB: app still exists under Client A. |
| 7 | `invalid-auth-key-rejected` | Use a random/fake auth key for any API call. Verify error response. |

#### Password Management (Clients)

| # | Deftest Name | Scenario |
|---|---|---|
| 8 | `client-change-password-happy` | Create client, change password with correct old password, login with new password succeeds. Check DB: client row updated. |
| 9 | `client-change-password-wrong-old` | Try to change password providing wrong old password. Verify error. Login with old password still works. |

#### Client Deletion

| # | Deftest Name | Scenario |
|---|---|---|
| 10 | `delete-client-cascading` | Create client, create app, delete client. Verify client gone, apps gone from list. Check DB: both client and app rows removed. |
| 11 | `delete-client-wrong-password` | Try to delete with wrong password. Verify error. Client and app still exist in DB. |

#### User Management (within apps)

| # | Deftest Name | Scenario |
|---|---|---|
| 12 | `user-signup-and-login` | Create user on an app, user logs in successfully, auth keys match across logins. Check DB: user row exists. |
| 13 | `same-email-on-multiple-apps` | Same email can create accounts on different apps (each is a separate account). Both login independently. Check DB: two separate user rows. |
| 14 | `duplicate-user-same-app` | Create user with same email on same app twice — second fails. Check DB: only one user row for that app+email combo. |
| 15 | `user-login-wrong-password` | Create user, attempt login with wrong password. Verify error, no auth_key. |
| 16 | `user-change-password` | User changes password with correct old password, new login works. Check DB: password hash updated. |
| 17 | `delete-user-and-cannot-login` | Create user, delete account, attempt to login — fails. Check DB: user row removed. |

#### App Users Listing

| # | Deftest Name | Scenario |
|---|---|---|
| 18 | `client-lists-app-users` | Client creates app, adds 3 users, lists them. Verify count matches 3. Check DB: rows correlate with API response. |

#### App Invitations and Permissions

| # | Deftest Name | Scenario |
|---|---|---|
| 19 | `owner-invites-contributor` | Owner invites another client as contributor. Invitee can see the app in their list. Check DB: invite row created with correct role. |
| 20 | `invited-admin-can-invite` | Owner invites admin, admin invites another user as contributor. Chain works. Check DB: all invites recorded correctly. |
| 21 | `contributor-cannot-invite` | Contributor tries to invite — gets error. Check DB: no new invite row created. |
| 22 | `cannot-invite-nonexistent-account` | Invite someone who has no account. Verify error. Check DB: no invite row. |

#### App File Management (User Files)

| # | Deftest Name | Scenario |
|---|---|---|
| 23 | `user-upload-and-download-file` | Upload a file, download it, contents match. Overwrite with new content, verify contents updated. Check DB: file row exists. |
| 24 | `download-inexistent-file` | Try to download a file that doesn't exist. Verify nil/error response. |
| 25 | `user-list-and-delete-files` | Upload two files, list (count=2), delete one, list again (count=1). Attempt to delete same file twice — second fails. Check DB: count matches at each step. |

#### App File Management (App Files)

| # | Deftest Name | Scenario |
|---|---|---|
| 26 | `app-upload-download-delete-file` | Client uploads app file, downloads it (contents match), deletes it, download returns nil. Check DB: file created then removed. |
| 27 | `app-files-accessible-by-roles` | Owner uploads files, both owner and contributor can list app files. Third party with no access gets error. Check DB: access control correct. |

#### App Manager Listing

| # | Deftest Name | Scenario |
|---|---|---|
| 28 | `list-app-managers` | Owner invites a contributor, both owner and contributor can list managers (count=2). Random third party gets error. Check DB: manager rows match. |

#### Manager Revocation

| # | Deftest Name | Scenario |
|---|---|---|
| 29 | `revoke-admin-access` | Owner invites admin, revokes admin privileges. Client role changes from admin to non-admin. Check DB: role updated in invite row. |
| 30 | `revoked-client-cannot-invite-others` | Admin gets revoked, tries to invite — gets error.


#### Action CRUD

| # | Deftest Name | Scenario |
|---|---|---|
| 31 | `action-create-read-update-delete` (create-action-and-manage) | Client creates action, reads it back (script matches), overwrites script via create again, renames via update, reads renamed version, lists actions (count=1), deletes, lists again (count=0). Check DB: action row at each step. |
| 32 | `re-name-action-preserved-content` | Create action with script A, rename to new name, read back — content is still A. Then update both name and script, verify final state. Check DB: row persists through rename. |

#### Admin Operations

| # | Deftest Name | Scenario |
|---|---|---|
| 33 | `admin-can-list-everything` | Create admin client via direct DB flag (see note below), list all clients/apps/files/admins — all succeed with non-empty results. |
| 34 | `non-admin-cannot-list-all` | Regular client tries to access `/clients/all`, `/apps/all`, `/files/all`, `/admins/all`. All return errors. |
| 35 | `promote-and-demote-admin` | Admin promotes a regular client to admin, then demotes. Non-admin trying same operations gets errors. Cannot demote self. Check DB: is_admin flag toggled correctly. |

> **Note on creating admin clients via API:** The current API does not have a public "create admin" endpoint — admins are only created by the existing `clients-signup` route (which always sets `is_admin=false`) or by existing admin using `/admins` (promote-to-admin). To bootstrap the first admin in tests, use the DB helper: insert a client row directly via `(db/run-operation "create-client-account.sql" {...})` and set `is_admin='on'`, or sign up and promote via the promote endpoint if there's already an admin. Plan for the test to bootstrap via: (1) create regular client, (2) use DB helper to set `is_admin=on`, (3) login as promoted admin, then run subsequent admin tests via API only.
>
> Actually, a simpler approach: Step 1 of each admin test should directly manipulate the DB to create the first admin, since there's no seed mechanism. Subsequent promotions work via the normal `/admins` POST endpoint.

#### Database File Storage Verification

The following sub-steps should be interlaced throughout the relevant tests above (not separate tests):
- After any "create file" operation, verify `(db-get-file-by-name-and-user ...)` returns a row
- After any "delete file" operation, verify `(db-get-file-by-name-and-user ...)` returns nil
- After cascading deletes (client delete → files deleted), verify DB cleanup

---

## Step 4: Handle `check-client-role` route verification

**Action needed:** Check if the API has a route to get a client's role in an app. The business function `(biz/get-client-role-in-app ...)` exists, but search `api.clj` for a corresponding HTTP route. If missing, either:
- Add a new route handler in `api.clj` (if scope permits), OR
- Use the existing wrapper and mark this test to use DB-only verification

---

## Step 5: Deprecate old integration tests

**Actions:**
1. Mark the files under `integration/` (`network_test.clj`, `check-health.clj`, `generate-mock-data.clj`) with a comment header noting they are deprecated and replaced by `test/br/bsb/liberdade/baas/integration_test.clj`.
2. Update the `makefile` to remove or update the `integration-test` target:
   - Remove references to `bb network_test.clj`
   - If desired, replace with `lein test :only br.bsb.liberdade.baas.integration-test`
3. Optionally remove the `integration/` directory entirely (recommended).

---

## Step 6: Update makefile for new tests

**Actions:**
1. Add a new target `behavioral-test` that runs:
   ```
   lein test br.bsb.liberdade.baas.integration-test
   ```
2. Update the `test` target to include integration tests optionally, or keep them separate since they use a live server.

---

## Implementation Order (for AI agents)

Execute steps in this order for incremental verification:

1. **Step 1a:** Create `test_helpers.clj` with HTTP wrappers only (no fixture yet). Run manually to verify each wrapper returns expected structure.
2. **Step 1b:** Add server lifecycle helpers and integration fixture. Test by running a trivial `(deftest test-smoke [...] (is true))`.
3. **Step 2:** Add DB helpers. Verify each returns correct values after manual DB manipulation.
4. **Step 3a:** Write tests #1–#3 (Client Account Management). Run `lein test` to verify.
5. **Step 3b:** Write tests #4–#7 (App CRUD). Run and verify.
6. **Step 3c:** Write tests #8–#11 (Password management + Client deletion). Run and verify.
7. **Step 3d:** Write tests #12–#18 (User management). Run and verify.
8. **Step 3e:** Write tests #19–#22 (Invitations/Permissions). Run and verify.
9. **Step 3f:** Write tests #23–#27 (File management). Run and verify.
10. **Step 3g:** Write tests #28–#35 (Managers, Actions, Admins). Run and verify.
11. **Step 4:** Address missing route for `get-client-role-in-app` if needed.
12. **Step 5:** Deprecate/remove old integration tests.
13. **Step 6:** Update makefile.

---

## Notes for implementors

- **Thread safety:** The http-kit `run-server` is blocking. Each test's fixture must run it in a daemon thread so test teardown can proceed even if the server doesn't shut down cleanly. Use `(.setDaemon t true)` on the thread.
- **Port selection:** Use `(with-open [s (java.net.ServerSocket. 0)] (.getLocalPort s))` to find a free port dynamically. Set `API_PORT` env var before starting the server so it binds to that port.
- **Database isolation:** Each test gets its own SQLite file via `DATABASE_FILE` env var. This avoids contention between parallel tests. Note: `(def ds ...)` in `db.clj` is evaluated at namespace load time, so changing `DATABASE_FILE` _after_ the ns is loaded won't work. The fixture must use `alter-var-root` or re-bind the `ds` var, OR delete and recreate the SQLite file directly. **Recommended approach:** Delete the old DB file, then `(db/setup-database)` + `(db/run-migrations)` will create a fresh one — but only if `dbname` is updated too. To do this cleanly:
  - In the fixture, use `System/setProperty` or modify `db/dbname var` via `alter-var-root` before calling setup/migrations.
  - Alternatively, set `DATABASE_FILE` env var in the sub-process by running tests with `lein test` and the env var set. Since we're starting the server in a thread (same JVM), use `(alter-var-root #'db/dbname (constantly new-path))` and `(alter-var-root #'db/ds (constantly (jdbc/get-datasource {:dbtype "sqlite" :dbname new-path})))`.
- **File test resources:** The existing tests reference `resources/pokemon.jpg` and `resources/animal_crossing.jpg`. Ensure these files exist for file upload/download tests.
- **Scripting engine health check:** The `/health` endpoint checks the scripting engine (Lua proxy). If this is not available during testing, it may cause startup delays. Consider that `(proxies/check-scripting-engine-health)` might return `"ko"`. The `/health` endpoint still returns 200 status with the full body, so this should be fine.
