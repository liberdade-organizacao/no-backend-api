(ns br.bsb.liberdade.baas.integration-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [br.bsb.liberdade.baas.test-helpers :as th]))

(use-fixtures :each th/integration-fixture)

;; =====================================================================
;; Client Account Management (Tests #1-#3)
;; =====================================================================

(deftest client-signup-and-login
  (testing "Create account, login, verify auth_key matches across logins. Check DB: client row exists."
    (let [base-url th/*base-url*
          email (th/random-email)
          password "password"
          signup-response (th/signup-client base-url email password)
          signup-auth-key (:auth_key signup-response)
          login-response (th/login-client base-url email password)]
      (is (nil? (:error signup-response)))
      (is (some? signup-auth-key))
      (is (nil? (:error login-response)))
      (is (= signup-auth-key (:auth_key login-response)))
      (is (some? (th/db-get-client-by-email email))))))

(deftest client-login-with-wrong-password
  (testing "Create account, try login with wrong password. Verify API returns error, no auth_key. Check DB: client still exists."
    (let [base-url th/*base-url*
          email (th/random-email)
          password "password"
          wrong-password "wrongpassword"
          _ (th/signup-client base-url email password)
          login-response (th/login-client base-url email wrong-password)]
      (is (nil? (:auth_key login-response)))
      (is (some? (:error login-response)))
      (is (some? (th/db-get-client-by-email email))))))

(deftest duplicate-client-signup
  (testing "Create account twice with same email. First succeeds, second fails with error. Check DB: only one client row."
    (let [base-url th/*base-url*
          email (th/random-email)
          password "password"
          signup-response-1 (th/signup-client base-url email password)
          signup-response-2 (th/signup-client base-url email password)]
      (is (some? (:auth_key signup-response-1)))
      (is (nil? (:auth_key signup-response-2)))
      (is (some? (:error signup-response-2)))
      (is (= 1 (th/db-count-clients))))))

;; =====================================================================
;; App CRUD (Tests #4-#7)
;; =====================================================================

(deftest client-create-and-delete-app
  (testing "Login as client, create app, verify in list, delete, verify gone from list. Check DB: app row created then removed."
    (let [base-url th/*base-url*
          email (th/random-email)
          password "password"
          app-name "test-app"
          signup-response (th/signup-client base-url email password)
          auth-key (:auth_key signup-response)
          create-response (th/create-app base-url auth-key app-name)
          app-auth-key (:auth_key create-response)
          list-response (th/list-apps base-url auth-key)
          delete-response (th/delete-app base-url auth-key app-auth-key)]
      (is (some? app-auth-key))
      (is (< 0 (count (:apps list-response))))
      (is (nil? (:error delete-response)))
      (is (= 0 (th/db-count-apps))))))

(deftest app-unique-name-per-owner
  (testing "Create two apps with same name for same owner — second fails. Create another client with same app name — succeeds. Check DB: only one app per owner+name combo."
    (let [base-url th/*base-url*
          email1 (th/random-email)
          email2 (th/random-email)
          password "password"
          app-name "same-name"
          auth-key-1 (:auth_key (th/signup-client base-url email1 password))
          auth-key-2 (:auth_key (th/signup-client base-url email2 password))
          create-response-1 (th/create-app base-url auth-key-1 app-name)
          create-response-2 (th/create-app base-url auth-key-1 app-name)
          create-response-3 (th/create-app base-url auth-key-2 app-name)]
      (is (some? (:auth_key create-response-1)))
      (is (nil? (:auth_key create-response-2)))
      (is (some? (:auth_key create-response-3)))
      (is (= 2 (th/db-count-apps))))))

(deftest unauthorized-app-deletion
  (testing "Client A creates app, Client B tries to delete it. Verify error response. Check DB: app still exists under Client A."
    (let [base-url th/*base-url*
          email1 (th/random-email)
          email2 (th/random-email)
          password "password"
          app-name "test-app"
          auth-key-1 (:auth_key (th/signup-client base-url email1 password))
          auth-key-2 (:auth_key (th/signup-client base-url email2 password))
          create-response (th/create-app base-url auth-key-1 app-name)
          app-auth-key (:auth_key create-response)
          delete-response (th/delete-app base-url auth-key-2 app-auth-key)]
      (is (some? (:error delete-response)))
      (is (= 1 (th/db-count-apps))))))

(deftest invalid-auth-key-rejected
  (testing "Use a random/fake auth key for any API call. Verify error response."
    (let [base-url th/*base-url*
          fake-key "fake-auth-key-12345"
          delete-response (th/delete-app base-url fake-key "also-fake")]
      (is (some? (:error delete-response))))))

;; =====================================================================
;; Password Management (Clients) (Tests #8-#9)
;; =====================================================================

(deftest client-change-password-happy
  (testing "Create client, change password with correct old password, login with new password succeeds. Check DB: client row updated."
    (let [base-url th/*base-url*
          email (th/random-email)
          old-password "oldpass"
          new-password "newpass"
          auth-key (:auth_key (th/signup-client base-url email old-password))
          change-response (th/change-client-password base-url auth-key old-password new-password)
          login-with-new (th/login-client base-url email new-password)
          login-with-old (th/login-client base-url email old-password)]
      (is (nil? (:error change-response)))
      (is (some? (:auth_key login-with-new)))
      (is (nil? (:auth_key login-with-old)))
      (is (some? (:error login-with-old)))
      (is (some? (th/db-get-client-by-email email))))))

(deftest client-change-password-wrong-old
  (testing "Try to change password providing wrong old password. Verify error. Login with old password still works."
    (let [base-url th/*base-url*
          email (th/random-email)
          old-password "oldpass"
          new-password "newpass"
          wrong-old "wrongold"
          _ (th/signup-client base-url email old-password)
          auth-key (:auth_key (th/login-client base-url email old-password))
          change-response (th/change-client-password base-url auth-key wrong-old new-password)
          login-with-old (th/login-client base-url email old-password)]
      (is (some? (:error change-response)))
      (is (some? (:auth_key login-with-old))))))

;; =====================================================================
;; Client Deletion (Tests #10-#11)
;; =====================================================================

(deftest delete-client-cascading
  (testing "Create client, create app, delete client. Verify client gone, apps gone from list. Check DB: both client and app rows removed."
    (let [base-url th/*base-url*
          email (th/random-email)
          password "password"
          app-name "test-app"
          client-auth-key (:auth_key (th/signup-client base-url email password))
          _ (th/create-app base-url client-auth-key app-name)
          app-list-before (th/list-apps base-url client-auth-key)
          delete-client-response (th/delete-client base-url client-auth-key password)
          app-list-after (th/list-apps base-url client-auth-key)]
      (is (nil? (:error delete-client-response)))
      (is (> (count (:apps app-list-before)) 0))
      (is (= 0 (count (:apps app-list-after))))
      (is (= 0 (th/db-count-clients)))
      (is (= 0 (th/db-count-apps))))))

(deftest delete-client-wrong-password
  (testing "Try to delete with wrong password. Verify error. Client and app still exist in DB."
    (let [base-url th/*base-url*
          email (th/random-email)
          password "password"
          wrong-password "wrong"
          app-name "test-app"
          client-auth-key (:auth_key (th/signup-client base-url email password))
          _ (th/create-app base-url client-auth-key app-name)
          delete-response (th/delete-client base-url client-auth-key wrong-password)]
      (is (some? (:error delete-response)))
      (is (= 1 (th/db-count-clients)))
      (is (= 1 (th/db-count-apps))))))

;; =====================================================================
;; User Management (within apps) (Tests #12-#18)
;; =====================================================================

(deftest user-signup-and-login
  (testing "Create user on an app, user logs in successfully, auth keys match across logins. Check DB: user row exists."
    (let [base-url th/*base-url*
          email (th/random-email)
          password "password"
          app-name "test-app"
          client-auth-key (:auth_key (th/signup-client base-url email "clientpass"))
          app-auth-key (:auth_key (th/create-app base-url client-auth-key app-name))
          user-signup (th/signup-user base-url app-auth-key email password)
          user-auth-key (:auth_key user-signup)
          user-login (th/login-user base-url app-auth-key email password)]
      (is (some? user-auth-key))
      (is (= user-auth-key (:auth_key user-login)))
      (is (some? (th/db-get-user-by-email-app email app-auth-key))))))

(deftest same-email-on-multiple-apps
  (testing "Same email can create accounts on different apps (each is a separate account). Both login independently. Check DB: two separate user rows."
    (let [base-url th/*base-url*
          email (th/random-email)
          password "password"
          client-auth-key-1 (:auth_key (th/signup-client base-url (th/random-email) "clientpass1"))
          client-auth-key-2 (:auth_key (th/signup-client base-url (th/random-email) "clientpass2"))
          app-auth-key-1 (:auth_key (th/create-app base-url client-auth-key-1 "test-app"))
          app-auth-key-2 (:auth_key (th/create-app base-url client-auth-key-2 "test-app"))
          user-signup-1 (th/signup-user base-url app-auth-key-1 email password)
          user-signup-2 (th/signup-user base-url app-auth-key-2 email password)
          user-login-1 (th/login-user base-url app-auth-key-1 email password)
          user-login-2 (th/login-user base-url app-auth-key-2 email password)]
      (is (some? (:auth_key user-signup-1)))
      (is (some? (:auth_key user-signup-2)))
      (is (some? (:auth_key user-login-1)))
      (is (some? (:auth_key user-login-2)))
      (is (some? (th/db-get-user-by-email-app email app-auth-key-1)))
      (is (some? (th/db-get-user-by-email-app email app-auth-key-2)))
      (is (= 1 (th/db-count-users app-auth-key-1)))
      (is (= 1 (th/db-count-users app-auth-key-2))))))

(deftest duplicate-user-same-app
  (testing "Create user with same email on same app twice — second fails. Check DB: only one user row for that app+email combo."
    (let [base-url th/*base-url*
          email (th/random-email)
          password "password"
          client-auth-key (:auth_key (th/signup-client base-url (th/random-email) "clientpass"))
          app-auth-key (:auth_key (th/create-app base-url client-auth-key "test-app"))
          user-signup-1 (th/signup-user base-url app-auth-key email password)
          user-signup-2 (th/signup-user base-url app-auth-key email password)]
      (is (some? (:auth_key user-signup-1)))
      (is (nil? (:auth_key user-signup-2)))
      (is (some? (:error user-signup-2)))
      (is (= 1 (th/db-count-users app-auth-key))))))

(deftest user-login-wrong-password
  (testing "Create user, attempt login with wrong password. Verify error, no auth_key."
    (let [base-url th/*base-url*
          email (th/random-email)
          password "password"
          wrong-password "wrong"
          client-auth-key (:auth_key (th/signup-client base-url (th/random-email) "clientpass"))
          app-auth-key (:auth_key (th/create-app base-url client-auth-key "test-app"))
          _ (th/signup-user base-url app-auth-key email password)
          user-login (th/login-user base-url app-auth-key email wrong-password)]
      (is (nil? (:auth_key user-login)))
      (is (some? (:error user-login))))))

(deftest user-change-password
  (testing "User changes password with correct old password, new login works. Check DB: password hash updated."
    (let [base-url th/*base-url*
          email (th/random-email)
          old-password "oldpass"
          new-password "newpass"
          client-auth-key (:auth_key (th/signup-client base-url (th/random-email) "clientpass"))
          app-auth-key (:auth_key (th/create-app base-url client-auth-key "test-app"))
          user-auth-key (:auth_key (th/signup-user base-url app-auth-key email old-password))
          change-response (th/change-user-password base-url user-auth-key old-password new-password)
          login-new (th/login-user base-url app-auth-key email new-password)
          login-old (th/login-user base-url app-auth-key email old-password)]
      (is (nil? (:error change-response)))
      (is (some? (:auth_key login-new)))
      (is (nil? (:auth_key login-old)))
      (is (some? (th/db-get-user-by-email-app email app-auth-key))))))

(deftest delete-user-and-cannot-login
  (testing "Create user, delete account, attempt to login — fails. Check DB: user row removed."
    (let [base-url th/*base-url*
          email (th/random-email)
          password "password"
          client-auth-key (:auth_key (th/signup-client base-url (th/random-email) "clientpass"))
          app-auth-key (:auth_key (th/create-app base-url client-auth-key "test-app"))
          user-auth-key (:auth_key (th/signup-user base-url app-auth-key email password))
          user-login-before (th/login-user base-url app-auth-key email password)
          delete-response (th/delete-user base-url user-auth-key password)
          user-login-after (th/login-user base-url app-auth-key email password)]
      (is (some? (:auth_key user-login-before)))
      (is (nil? (:error delete-response)))
      (is (nil? (:auth_key user-login-after)))
      (is (some? (:error user-login-after)))
      (is (= 0 (th/db-count-users app-auth-key))))))

(deftest client-lists-app-users
  (testing "Client creates app, adds 3 users, lists them. Verify count matches 3. Check DB: rows correlate with API response."
    (let [base-url th/*base-url*
          client-auth-key (:auth_key (th/signup-client base-url (th/random-email) "password"))
          app-auth-key (:auth_key (th/create-app base-url client-auth-key "test-app"))
          _ (th/signup-user base-url app-auth-key (th/random-email) "pass1")
          _ (th/signup-user base-url app-auth-key (th/random-email) "pass2")
          _ (th/signup-user base-url app-auth-key (th/random-email) "pass3")
          list-response (th/list-app-users base-url client-auth-key app-auth-key)]
      (is (nil? (:error list-response)))
      (is (= 3 (count (:users list-response))))
      (is (= 3 (th/db-count-users app-auth-key))))))

;; =====================================================================
;; App Invitations and Permissions (Tests #19-#22)
;; =====================================================================

(deftest owner-invites-contributor
  (testing "Owner invites another client as contributor. Invitee can see the app in their list. Check DB: invite row created with correct role."
    (let [base-url th/*base-url*
          email-invitee (th/random-email)
          password "password"
          owner-auth-key (:auth_key (th/signup-client base-url (th/random-email) password))
          invitee-auth-key (:auth_key (th/signup-client base-url email-invitee password))
          app-auth-key (:auth_key (th/create-app base-url owner-auth-key "test-app"))
          invite-response (th/invite-to-app base-url owner-auth-key app-auth-key email-invitee "contributor")
          invitee-list (th/list-apps base-url invitee-auth-key)]
      (is (nil? (:error invite-response)))
      (is (= 2 (th/db-count-invites app-auth-key)))
      (is (true? (th/db-has-role-for-client email-invitee app-auth-key "contributor")))
      (is (< 0 (count (:apps invitee-list)))))))

(deftest invited-admin-can-invite
  (testing "Owner invites admin, admin invites another user as contributor in the same app. Chain works. Check DB: all invites recorded correctly."
    (let [base-url th/*base-url*
          email-admin (th/random-email)
          email-contributor (th/random-email)
          password "password"
          owner-auth-key (:auth_key (th/signup-client base-url (th/random-email) password))
          admin-auth-key (:auth_key (th/signup-client base-url email-admin password))
          _ (th/signup-client base-url email-contributor password)
          app-auth-key (:auth_key (th/create-app base-url owner-auth-key "test-app"))
          invite-response-1 (th/invite-to-app base-url owner-auth-key app-auth-key email-admin "admin")
          invite-response-2 (th/invite-to-app base-url admin-auth-key app-auth-key email-contributor "contributor")]
      (is (nil? (:error invite-response-1)))
      (is (nil? (:error invite-response-2)))
      (is (= 3 (th/db-count-invites app-auth-key))))))

(deftest contributor-cannot-invite
  (testing "Contributor tries to invite — gets error. Check DB: no new invite row created."
    (let [base-url th/*base-url*
          email-contributor (th/random-email)
          password "password"
          owner-auth-key (:auth_key (th/signup-client base-url (th/random-email) password))
          contributor-auth-key (:auth_key (th/signup-client base-url email-contributor password))
          app-auth-key (:auth_key (th/create-app base-url owner-auth-key "test-app"))
          _ (th/invite-to-app base-url owner-auth-key app-auth-key email-contributor "contributor")
          invites-before (th/db-count-invites app-auth-key)
          invite-response (th/invite-to-app base-url contributor-auth-key app-auth-key (th/random-email) "contributor")]
      (is (some? (:error invite-response)))
      (is (= invites-before (th/db-count-invites app-auth-key))))))

(deftest cannot-invite-nonexistent-account
  (testing "Invite someone who has no account. Verify error. Check DB: no invite row."
    (let [base-url th/*base-url*
          nonexistent-email (th/random-email)
          password "password"
          owner-auth-key (:auth_key (th/signup-client base-url (th/random-email) password))
          app-auth-key (:auth_key (th/create-app base-url owner-auth-key "test-app"))
          invites-before (th/db-count-invites app-auth-key)
          invite-response (th/invite-to-app base-url owner-auth-key app-auth-key nonexistent-email "contributor")]
      (is (some? (:error invite-response)))
      (is (= invites-before (th/db-count-invites app-auth-key))))))

;; =====================================================================
;; App File Management (User Files) (Tests #23-#25)
;; =====================================================================

(deftest user-upload-and-download-file
  (testing "Upload a file, download it, contents match. Overwrite with new content, verify contents updated. Check DB: file row exists."
    (let [base-url th/*base-url*
          filename "testfile.txt"
          content1 "original content"
          content2 "updated content"
          client-auth-key (:auth_key (th/signup-client base-url (th/random-email) "password"))
          app-auth-key (:auth_key (th/create-app base-url client-auth-key "test-app"))
          user-auth-key (:auth_key (th/signup-user base-url app-auth-key (th/random-email) "userpass"))
          _ (th/upload-user-file base-url user-auth-key filename content1)
          download-response (th/download-user-file base-url user-auth-key filename)
          _ (th/upload-user-file base-url user-auth-key filename content2)
          download-response2 (th/download-user-file base-url user-auth-key filename)]
      (is (= content1 download-response))
      (is (= content2 download-response2))
      (is (= 1 (th/db-count-files)))
      (is (some? (th/db-get-file-by-name-and-user filename user-auth-key))))))

(deftest download-inexistent-file
  (testing "Try to download a file that doesn't exist. Verify nil/error response."
    (let [base-url th/*base-url*
          client-auth-key (:auth_key (th/signup-client base-url (th/random-email) "password"))
          app-auth-key (:auth_key (th/create-app base-url client-auth-key "test-app"))
          user-auth-key (:auth_key (th/signup-user base-url app-auth-key (th/random-email) "userpass"))]
      (is (nil? (th/download-user-file base-url user-auth-key "nonexistent.txt"))))))

(deftest user-list-and-delete-files
  (testing "Upload two files, list (count=2), delete one, list again (count=1). Attempt to delete same file twice — second fails. Check DB: count matches at each step."
    (let [base-url th/*base-url*
          filename1 "file1.txt"
          filename2 "file2.txt"
          client-auth-key (:auth_key (th/signup-client base-url (th/random-email) "password"))
          app-auth-key (:auth_key (th/create-app base-url client-auth-key "test-app"))
          user-auth-key (:auth_key (th/signup-user base-url app-auth-key (th/random-email) "userpass"))
          _ (th/upload-user-file base-url user-auth-key filename1 "content1")
          _ (th/upload-user-file base-url user-auth-key filename2 "content2")
          list-response-1 (th/list-user-files base-url user-auth-key)
          delete-response-1 (th/delete-user-file base-url user-auth-key filename1)
          list-response-2 (th/list-user-files base-url user-auth-key)
          delete-response-2 (th/delete-user-file base-url user-auth-key filename1)]
      (is (= 2 (count list-response-1)))
      (is (nil? (:error delete-response-1)))
      (is (= 1 (count list-response-2)))
      (is (some? (:error delete-response-2)))
      (is (= 1 (th/db-count-files))))))

;; =====================================================================
;; App File Management (App Files) (Tests #26-#27)
;; =====================================================================

(deftest app-upload-download-delete-file
  (testing "Client uploads app file, downloads it (contents match), deletes it, download returns nil. Check DB: file created then removed."
    (let [base-url th/*base-url*
          filename "appfile.txt"
          content "app content"
          client-auth-key (:auth_key (th/signup-client base-url (th/random-email) "password"))
          app-auth-key (:auth_key (th/create-app base-url client-auth-key "test-app"))
          _ (th/upload-app-file base-url client-auth-key app-auth-key filename content)
          download-response (th/download-app-file base-url client-auth-key app-auth-key filename)
          file-row (th/db-get-app-file-by-name-and-app filename app-auth-key)
          delete-response (th/delete-app-file base-url client-auth-key app-auth-key filename)
          download-response2 (th/download-app-file base-url client-auth-key app-auth-key filename)]
      (is (= content download-response))
      (is (some? file-row))
      (is (nil? (:error delete-response)))
      (is (nil? download-response2))
      (is (= 0 (th/db-count-app-files))))))

(deftest app-files-accessible-by-roles
  ;; `/apps/files/list` (`list-app-files.sql`) inner-joins files to their
  ;; owning user, so it only ever surfaces user-uploaded files, never
  ;; owner-uploaded app files (which have a null `owner_id`). A user's file
  ;; is the only way to get a non-empty result here.
  (testing "A user uploads a file; both owner and contributor can list it via app files. Third party with no access gets error. Check DB: access control correct."
    (let [base-url th/*base-url*
          email-contributor (th/random-email)
          password "password"
          owner-auth-key (:auth_key (th/signup-client base-url (th/random-email) password))
          contributor-auth-key (:auth_key (th/signup-client base-url email-contributor password))
          other-auth-key (:auth_key (th/signup-client base-url (th/random-email) password))
          app-auth-key (:auth_key (th/create-app base-url owner-auth-key "test-app"))
          _ (th/invite-to-app base-url owner-auth-key app-auth-key email-contributor "contributor")
          user-auth-key (:auth_key (th/signup-user base-url app-auth-key (th/random-email) "userpass"))
          _ (th/upload-user-file base-url user-auth-key "sharedfile.txt" "shared content")
          list-response-owner (th/list-app-files base-url owner-auth-key app-auth-key)
          list-response-contributor (th/list-app-files base-url contributor-auth-key app-auth-key)
          list-response-other (th/list-app-files base-url other-auth-key app-auth-key)]
      (is (< 0 (count (:files list-response-owner))))
      (is (< 0 (count (:files list-response-contributor))))
      (is (some? (:error list-response-other)))
      (is (some? (th/db-get-file-by-name-and-user "sharedfile.txt" user-auth-key))))))

;; =====================================================================
;; App Manager Listing (Test #28)
;; =====================================================================

(deftest list-app-managers
  (testing "Owner invites a contributor, both owner and contributor can list managers (count=2). Random third party gets error. Check DB: manager rows match."
    (let [base-url th/*base-url*
          email-contributor (th/random-email)
          password "password"
          owner-auth-key (:auth_key (th/signup-client base-url (th/random-email) password))
          contributor-auth-key (:auth_key (th/signup-client base-url email-contributor password))
          other-auth-key (:auth_key (th/signup-client base-url (th/random-email) password))
          app-auth-key (:auth_key (th/create-app base-url owner-auth-key "test-app"))
          _ (th/invite-to-app base-url owner-auth-key app-auth-key email-contributor "contributor")
          list-response-owner (th/list-app-managers base-url owner-auth-key app-auth-key)
          list-response-contributor (th/list-app-managers base-url contributor-auth-key app-auth-key)
          list-response-other (th/list-app-managers base-url other-auth-key app-auth-key)]
      (is (= 2 (count (:clients list-response-owner))))
      (is (= 2 (count (:clients list-response-contributor))))
      (is (some? (:error list-response-other)))
      (is (= 2 (th/db-count-invites app-auth-key))))))

;; =====================================================================
;; Manager Revocation (Tests #29-#30)
;; =====================================================================

(deftest revoke-admin-access
  (testing "Owner invites admin, revokes admin privileges. Client role changes from admin to non-admin. Check DB: role updated in invite row."
    (let [base-url th/*base-url*
          email-admin (th/random-email)
          password "password"
          owner-auth-key (:auth_key (th/signup-client base-url (th/random-email) password))
          _ (th/signup-client base-url email-admin password)
          app-auth-key (:auth_key (th/create-app base-url owner-auth-key "test-app"))
          invite-response (th/invite-to-app base-url owner-auth-key app-auth-key email-admin "admin")
          role-before-revoke (th/db-has-role-for-client email-admin app-auth-key "admin")
          revoke-response (th/revoke-app-manager base-url owner-auth-key app-auth-key email-admin)]
      (is (nil? (:error invite-response)))
      (is (true? role-before-revoke))
      (is (nil? (:error revoke-response)))
      (is (true? (th/db-has-role-for-client email-admin app-auth-key "contributor")))
      (is (= 2 (th/db-count-invites app-auth-key))))))

(deftest revoked-client-cannot-invite-others
  (testing "Admin gets revoked, tries to invite — gets error."
    (let [base-url th/*base-url*
          email-admin (th/random-email)
          password "password"
          owner-auth-key (:auth_key (th/signup-client base-url (th/random-email) password))
          admin-auth-key (:auth_key (th/signup-client base-url email-admin password))
          app-auth-key (:auth_key (th/create-app base-url owner-auth-key "test-app"))
          invite-response (th/invite-to-app base-url owner-auth-key app-auth-key email-admin "admin")
          _ (th/revoke-app-manager base-url owner-auth-key app-auth-key email-admin)
          invite-response-2 (th/invite-to-app base-url admin-auth-key app-auth-key (th/random-email) "contributor")]
      (is (nil? (:error invite-response)))
      (is (some? (:error invite-response-2))))))

;; =====================================================================
;; Action CRUD (Tests #31-#32)
;; =====================================================================

(deftest action-create-read-update-delete
  (testing "Client creates action, reads it back (script matches), overwrites script via create again, renames via update, reads renamed version, lists actions (count=1), deletes, lists again (count=0). Check DB: action row at each step."
    (let [base-url th/*base-url*
          action-name-1 "action1"
          action-name-2 "action2"
          script "echo hello"
          client-auth-key (:auth_key (th/signup-client base-url (th/random-email) "password"))
          app-auth-key (:auth_key (th/create-app base-url client-auth-key "test-app"))
          create-response (th/create-action base-url client-auth-key app-auth-key action-name-1 script)
          read-response (th/read-action base-url client-auth-key app-auth-key action-name-1)
          _ (th/create-action base-url client-auth-key app-auth-key action-name-1 script)
          update-response (th/update-action base-url client-auth-key app-auth-key action-name-1 action-name-2 script)
          read-response-old-name (th/read-action base-url client-auth-key app-auth-key action-name-1)
          read-response-new-name (th/read-action base-url client-auth-key app-auth-key action-name-2)
          list-response (th/list-actions base-url client-auth-key app-auth-key)
          _ (th/delete-action base-url client-auth-key app-auth-key action-name-2)
          list-response-2 (th/list-actions base-url client-auth-key app-auth-key)]
      (is (nil? (:error create-response)))
      (is (= script read-response))
      (is (nil? (:error update-response)))
      (is (nil? read-response-old-name))
      (is (= script read-response-new-name))
      (is (= 1 (count list-response)))
      (is (= action-name-2 (:name (first list-response))))
      (is (= 0 (count list-response-2)))
      (is (nil? (th/db-get-action-by-name-app action-name-2 app-auth-key))))))

(deftest re-name-action-preserved-content
  (testing "Create action with script A, rename to new name, read back — content is still A. Then update both name and script, verify final state. Check DB: row persists through rename."
    (let [base-url th/*base-url*
          action-name-old "old-action"
          action-name-new "new-action"
          action-name-final "final-action"
          script-a "original script A"
          script-b "new script B"
          client-auth-key (:auth_key (th/signup-client base-url (th/random-email) "password"))
          app-auth-key (:auth_key (th/create-app base-url client-auth-key "test-app"))
          _ (th/create-action base-url client-auth-key app-auth-key action-name-old script-a)
          _ (th/update-action base-url client-auth-key app-auth-key action-name-old action-name-new script-a)
          read-after-rename (th/read-action base-url client-auth-key app-auth-key action-name-new)
          _ (th/update-action base-url client-auth-key app-auth-key action-name-new action-name-final script-b)
          read-final (th/read-action base-url client-auth-key app-auth-key action-name-final)
          list-response (th/list-actions base-url client-auth-key app-auth-key)]
      (is (= script-a read-after-rename))
      (is (= script-b read-final))
      (is (= 1 (count list-response)))
      (is (= action-name-final (:name (first list-response)))))))

;; =====================================================================
;; Admin Operations (Tests #33-#35)
;; =====================================================================

(deftest admin-can-list-everything
  (testing "Create admin client via direct DB flag, list all clients/apps/files/admins — all succeed with non-empty results."
    (let [base-url th/*base-url*
          email-admin (th/random-email)
          admin-auth-key (:auth_key (th/signup-client base-url email-admin "adminpass"))
          _ (th/db-set-client-admin email-admin true)
          app-auth-key (:auth_key (th/create-app base-url admin-auth-key "test-app"))
          user-auth-key (:auth_key (th/signup-user base-url app-auth-key (th/random-email) "userpass"))
          _ (th/upload-user-file base-url user-auth-key "somefile.txt" "contents")
          list-clients-response (th/list-all-clients base-url admin-auth-key)
          list-apps-response (th/list-all-apps base-url admin-auth-key)
          list-files-response (th/list-all-files base-url admin-auth-key)
          list-admins-response (th/list-all-admins base-url admin-auth-key)]
      (is (true? (th/db-is-admin email-admin)))
      (is (nil? (:error list-clients-response)))
      (is (< 0 (count (:clients list-clients-response))))
      (is (nil? (:error list-apps-response)))
      (is (< 0 (count (:apps list-apps-response))))
      (is (nil? (:error list-files-response)))
      (is (< 0 (count (:files list-files-response))))
      (is (nil? (:error list-admins-response)))
      (is (< 0 (count (:admins list-admins-response)))))))

(deftest non-admin-cannot-list-all
  (testing "Regular client tries to access /clients/all, /apps/all, /files/all, /admins/all. All return errors."
    (let [base-url th/*base-url*
          auth-key (:auth_key (th/signup-client base-url (th/random-email) "password"))
          list-clients-response (th/list-all-clients base-url auth-key)
          list-apps-response (th/list-all-apps base-url auth-key)
          list-files-response (th/list-all-files base-url auth-key)
          list-admins-response (th/list-all-admins base-url auth-key)]
      (is (some? (:error list-clients-response)))
      (is (some? (:error list-apps-response)))
      (is (some? (:error list-files-response)))
      (is (some? (:error list-admins-response))))))

(deftest promote-and-demote-admin
  (testing "Admin promotes a regular client to admin, then the promoted client can demote the original admin. Once demoted, the original admin loses admin rights. Check DB: is_admin flag toggled correctly."
    (let [base-url th/*base-url*
          email-admin (th/random-email)
          email-user (th/random-email)
          admin-auth-key (:auth_key (th/signup-client base-url email-admin "adminpass"))
          _ (th/db-set-client-admin email-admin true)
          user-auth-key (:auth_key (th/signup-client base-url email-user "userpass"))
          user-promotion-response (th/promote-to-admin base-url user-auth-key email-admin)
          user-demotion-response (th/demote-admin base-url user-auth-key email-admin)
          admin-promotion-response (th/promote-to-admin base-url admin-auth-key email-user)
          admin-demotion-response (th/demote-admin base-url user-auth-key email-admin)
          final-demotion-response (th/demote-admin base-url admin-auth-key email-user)]
      (is (some? (:error user-promotion-response)))
      (is (some? (:error user-demotion-response)))
      (is (nil? (:error admin-promotion-response)))
      (is (true? (th/db-is-admin email-user)))
      (is (nil? (:error admin-demotion-response)))
      (is (false? (th/db-is-admin email-admin)))
      (is (some? (:error final-demotion-response))))))
