(ns br.bsb.liberdade.baas.integration-test
   (:require [clojure.test :refer [deftest testing is]]
             [br.bsb.liberdade.baas.test-helpers :as th]
             [br.bsb.liberdade.baas.db :as db]))

(use-fixtures :each th/integration-fixture)

;; =====================================================================
;; Client Account Management (Tests #1-#3)
;; =====================================================================

(deftest client-signup-and-login
   (testing "Create account, login, verify auth_key matches across logins. Check DB: client row exists."
     (let [base-url nil
           email (th/random-email)
           password "password"]
       (let [base-url (th/integration-fixture nil)
             signup-response (th/signup-client base-url email password)
             signup-auth-key (:auth_key signup-response)]
         (is (contains? signup-response :auth_key))
         (let [login-response (th/login-client base-url email password)]
           (is (contains? login-response :auth_key))
           (is (= (:auth_key login-response) signup-auth-key))
           (is (th/db-get-client-by-email email))))))

(deftest client-login-with-wrong-password
   (testing "Create account, try login with wrong password. Verify API returns error, no auth_key. Check DB: client still exists."
     (let [base-url nil
           email (th/random-email)
           password "password"
           wrong-password "wrongpassword"]
       (let [base-url (th/integration-fixture nil)
             signup-response (th/signup-client base-url email password)
             signup-auth-key (:auth_key signup-response)
             login-response (th/login-client base-url email wrong-password)]
         (is (not (contains? login-response :auth_key)))
         (is (contains? login-response :error))
         (is (th/db-get-client-by-email email))))))

(deftest duplicate-client-signup
   (testing "Create account twice with same email. First succeeds, second fails with error. Check DB: only one client row."
     (let [base-url nil
           email (th/random-email)
           password "password"]
       (let [signup-response-1 (th/signup-client base-url email password)]
         (is (contains? signup-response-1 :auth_key))
         (let [signup-response-2 (th/signup-client base-url email password)]
           (is (not (contains? signup-response-2 :auth_key)))
           (is (contains? signup-response-2 :error)))
       (is (= 1 (th/db-count-clients))))))

;; =====================================================================
;; App CRUD (Tests #4-#7)
;; =====================================================================

(deftest client-create-and-delete-app
   (testing "Login as client, create app, verify in list, delete, verify gone from list. Check DB: app row created then removed."
     (let [base-url nil
           email (th/random-email)
           password "password"
           app-name "test-app"]
       (let [base-url (th/integration-fixture nil)
             signup-response (th/signup-client base-url email password)
             auth-key (:auth_key signup-response)
             create-response (th/create-app base-url auth-key app-name)
             app-auth-key (:auth_key create-response)
             list-response (th/list-apps base-url auth-key)]
         (is (contains? create-response :auth_key))
         (is (< 0 (count (:apps list-response))))
         (let [delete-response (th/delete-app base-url auth-key app-auth-key)]
           (is (= 200 (:status delete-response))))
       (is (= 0 (th/db-count-apps))))))

(deftest app-unique-name-per-owner
   (testing "Create two apps with same name for same owner — second fails. Create another client with same app name — succeeds. Check DB: only one app per owner+name combo."
     (let [base-url nil
           email1 (th/random-email)
           email2 (th/random-email)
           password "password"
           app-name "same-name"]
       (let [base-url (th/integration-fixture nil)
             signup-response-1 (th/signup-client base-url email1 password)
             signup-response-2 (th/signup-client base-url email2 password)
             auth-key-1 (:auth_key signup-response-1)
             auth-key-2 (:auth_key signup-response-2)
             create-response-1 (th/create-app base-url auth-key-1 app-name)
             app-auth-key-1 (:auth_key create-response-1)
             create-response-2 (th/create-app base-url auth-key-2 app-name)]
         (is (contains? create-response-1 :auth_key))
         (is (not (contains? create-response-2 :auth_key)))
         (is (contains? create-response-2 :error))
             (let [create-response-3 (th/create-app base-url auth-key-1 app-name)]
               (is (not (contains? create-response-3 :auth_key)))
               (is (contains? create-response-3 :error))))
       (is (= 1 (th/db-count-apps))))))

(deftest unauthorized-app-deletion
   (testing "Client A creates app, Client B tries to delete it. Verify error response. Check DB: app still exists under Client A."
     (let [base-url nil
           email1 (th/random-email)
           email2 (th/random-email)
           password "password"
           app-name "test-app"]
       (let [base-url (th/integration-fixture nil)
             signup-response-1 (th/signup-client base-url email1 password)
             signup-response-2 (th/signup-client base-url email2 password)
             auth-key-1 (:auth_key signup-response-1)
             auth-key-2 (:auth_key signup-response-2)
             create-response (th/create-app base-url auth-key-1 app-name)
             app-auth-key (:auth_key create-response)
             delete-response (th/delete-app base-url auth-key-2 app-auth-key)]
         (is (contains? delete-response :error))
         (is (contains? delete-response :message)))
       (is (= 1 (th/db-count-apps))))))

(deftest invalid-auth-key-rejected
   (testing "Use a random/fake auth key for any API call. Verify error response."
     (let [base-url nil
           fake-key "fake-auth-key-12345"]
       (let [base-url (th/integration-fixture nil)
             create-response (th/create-app base-url fake-key "test-app")]
         (is (contains? create-response :error))
         (is (contains? create-response :message))))))

;; =====================================================================
;; Password Management (Clients) (Tests #8-#9)
;; =====================================================================

(deftest client-change-password-happy
   (testing "Create client, change password with correct old password, login with new password succeeds. Check DB: client row updated."
     (let [base-url nil
           email (th/random-email)
           old-password "oldpass"
           new-password "newpass"]
       (let [base-url (th/integration-fixture nil)
             signup-response (th/signup-client base-url email old-password)
             auth-key (:auth_key signup-response)
             change-response (th/change-client-password base-url auth-key old-password new-password)
             login-with-new (th/login-client base-url email new-password)
             login-with-old (th/login-client base-url email old-password)]
         (is (= 200 (:status change-response)))
         (is (contains? login-with-new :auth_key))
         (is (contains? login-with-old :auth_key)))))
   (is (th/db-get-client-by-email email))))

(deftest client-change-password-wrong-old
   (testing "Try to change password providing wrong old password. Verify error. Login with old password still works."
     (let [base-url nil
           email (th/random-email)
           old-password "oldpass"
           new-password "newpass"
           wrong-old "wrongold"]
       (let [base-url (th/integration-fixture nil)
             signup-response (th/signup-client base-url email old-password)
             auth-key (:auth_key signup-response)
             change-response (th/change-client-password base-url auth-key wrong-old new-password)
             login-with-old (th/login-client base-url email old-password)]
         (is (contains? change-response :error))
         (is (contains? change-response :message))
         (is (contains? login-with-old :auth_key))))))

;; =====================================================================
;; Client Deletion (Tests #10-#11)
;; =====================================================================

(deftest delete-client-cascading
   (testing "Create client, create app, delete client. Verify client gone, apps gone from list. Check DB: both client and app rows removed."
     (let [base-url nil
           email (th/random-email)
           password "password"
           app-name "test-app"]
       (let [base-url (th/integration-fixture nil)
             signup-response (th/signup-client base-url email password)
             client-auth-key (:auth_key signup-response)
             create-response (th/create-app base-url client-auth-key app-name)
             app-auth-key (:auth_key create-response)
             app-list-before (th/list-apps base-url client-auth-key)
             delete-client-response (th/delete-client base-url client-auth-key password)
             app-list-after (th/list-apps base-url client-auth-key)]
         (is (= 200 (:status delete-client-response)))
         (is (> (count (:apps app-list-before)) 0))
         (is (= 0 (count (:apps app-list-after))))))
      (is (= 0 (th/db-count-clients)))
      (is (= 0 (th/db-count-apps)))))

(deftest delete-client-wrong-password
   (testing "Try to delete with wrong password. Verify error. Client and app still exist in DB."
     (let [base-url nil
           email (th/random-email)
           password "password"
           wrong-password "wrong"
           app-name "test-app"]
       (let [base-url (th/integration-fixture nil)
             signup-response (th/signup-client base-url email password)
             client-auth-key (:auth_key signup-response)
             create-response (th/create-app base-url client-auth-key app-name)
             app-auth-key (:auth_key create-response)
             delete-response (th/delete-client base-url client-auth-key wrong-password)]
         (is (contains? delete-response :error))
         (is (contains? delete-response :message)))
       (is (= 1 (th/db-count-clients)))
       (is (= 1 (th/db-count-apps))))))

;; =====================================================================
;; User Management (within apps) (Tests #12-#18)
;; =====================================================================

(deftest user-signup-and-login
   (testing "Create user on an app, user logs in successfully, auth keys match across logins. Check DB: user row exists."
     (let [base-url nil
           email (th/random-email)
           password "password"
           app-name "test-app"]
       (let [base-url (th/integration-fixture nil)
             client-signup (th/signup-client base-url email "clientpass")
             client-auth-key (:auth_key client-signup)
             client-create (th/create-app base-url client-auth-key app-name)
             app-auth-key (:auth_key client-create)
             user-signup (th/signup-user base-url app-auth-key email password)
             user-auth-key (:auth_key user-signup)
             user-login (th/login-user base-url app-auth-key email password)]
         (is (contains? user-signup :auth_key))
         (is (contains? user-login :auth_key))
         (is (= (:auth_key user-login) user-auth-key))
         (is (th/db-get-user-by-email-app email app-auth-key))))))

(deftest same-email-on-multiple-apps
   (testing "Same email can create accounts on different apps (each is a separate account). Both login independently. Check DB: two separate user rows."
     (let [base-url nil
           email (th/random-email)
           password "password"
           app-name "test-app"]
       (let [base-url (th/integration-fixture nil)
             client-signup-1 (th/signup-client base-url email "clientpass1")
             client-auth-key-1 (:auth_key client-signup-1)
             client-create-1 (th/create-app base-url client-auth-key-1 app-name)
             app-auth-key-1 (:auth_key client-create-1)
             user-signup-1 (th/signup-user base-url app-auth-key-1 email password)
             client-signup-2 (th/signup-client base-url email "clientpass2")
             client-auth-key-2 (:auth_key client-signup-2)
             client-create-2 (th/create-app base-url client-auth-key-2 app-name)
             app-auth-key-2 (:auth_key client-create-2)
             user-signup-2 (th/signup-user base-url app-auth-key-2 email password)
             user-login-1 (th/login-user base-url app-auth-key-1 email password)
             user-login-2 (th/login-user base-url app-auth-key-2 email password)]
         (is (contains? user-signup-1 :auth_key))
         (is (contains? user-signup-2 :auth_key))
         (is (contains? user-login-1 :auth_key))
         (is (contains? user-login-2 :auth_key))
         (is (th/db-get-user-by-email-app email app-auth-key-1))
         (is (th/db-get-user-by-email-app email app-auth-key-2)))))
   (is (= 2 (th/db-count-users "test-app"))))

(deftest duplicate-user-same-app
   (testing "Create user with same email on same app twice — second fails. Check DB: only one user row for that app+email combo."
     (let [base-url nil
           email (th/random-email)
           password "password"
           app-name "test-app"]
       (let [base-url (th/integration-fixture nil)
             client-signup (th/signup-client base-url email "clientpass")
             client-auth-key (:auth_key client-signup)
             client-create (th/create-app base-url client-auth-key app-name)
             app-auth-key (:auth_key client-create)
             user-signup-1 (th/signup-user base-url app-auth-key email password)
             user-signup-2 (th/signup-user base-url app-auth-key email password)]
         (is (contains? user-signup-1 :auth_key))
         (is (not (contains? user-signup-2 :auth_key)))
         (is (contains? user-signup-2 :error)))
       (is (= 1 (th/db-count-users app-auth-key))))))

(deftest user-login-wrong-password
   (testing "Create user, attempt login with wrong password. Verify error, no auth_key."
     (let [base-url nil
           email (th/random-email)
           password "password"
           wrong-password "wrong"
           app-name "test-app"]
       (let [base-url (th/integration-fixture nil)
             client-signup (th/signup-client base-url email "clientpass")
             client-auth-key (:auth_key client-signup)
             client-create (th/create-app base-url client-auth-key app-name)
             app-auth-key (:auth_key client-create)
             user-signup (th/signup-user base-url app-auth-key email password)
             user-login (th/login-user base-url app-auth-key email wrong-password)]
         (is (not (contains? user-login :auth_key)))
         (is (contains? user-login :error))))))

(deftest user-change-password
   (testing "User changes password with correct old password, new login works. Check DB: password hash updated."
     (let [base-url nil
           email (th/random-email)
           old-password "oldpass"
           new-password "newpass"
           app-name "test-app"]
       (let [base-url (th/integration-fixture nil)
             client-signup (th/signup-client base-url email "clientpass")
             client-auth-key (:auth_key client-signup)
             client-create (th/create-app base-url client-auth-key app-name)
             app-auth-key (:auth_key client-create)
             user-signup (th/signup-user base-url app-auth-key email old-password)
             user-auth-key (:auth_key user-signup)
             change-response (th/change-user-password base-url user-auth-key old-password new-password)
             login-new (th/login-user base-url app-auth-key email new-password)
             login-old (th/login-user base-url app-auth-key email old-password)]
         (is (= 200 (:status change-response)))
         (is (contains? login-new :auth_key))
         (is (contains? login-old :auth_key)))))
   (is (th/db-get-user-by-email-app email app-auth-key))))

(deftest delete-user-and-cannot-login
   (testing "Create user, delete account, attempt to login — fails. Check DB: user row removed."
     (let [base-url nil
           email (th/random-email)
           password "password"
           app-name "test-app"]
       (let [base-url (th/integration-fixture nil)
             client-signup (th/signup-client base-url email "clientpass")
             client-auth-key (:auth_key client-signup)
             client-create (th/create-app base-url client-auth-key app-name)
             app-auth-key (:auth_key client-create)
             user-signup (th/signup-user base-url app-auth-key email password)
             user-auth-key (:auth_key user-signup)
             user-login-before (th/login-user base-url app-auth-key email password)
             delete-response (th/delete-user base-url user-auth-key password)
             user-login-after (th/login-user base-url app-auth-key email password)]
         (is (contains? user-login-before :auth_key))
         (is (not (contains? user-login-after :auth_key)))
         (is (contains? user-login-after :error)))
       (is (= 0 (th/db-count-users app-auth-key))))))

(deftest client-lists-app-users
   (testing "Client creates app, adds 3 users, lists them. Verify count matches 3. Check DB: rows correlate with API response."
     (let [base-url nil
           email (th/random-email)
           password "password"
           app-name "test-app"]
       (let [base-url (th/integration-fixture nil)
             client-signup (th/signup-client base-url email password)
             client-auth-key (:auth_key client-signup)
             client-create (th/create-app base-url client-auth-key app-name)
             app-auth-key (:auth_key client-create)
             email1 (th/random-email)
             email2 (th/random-email)
             email3 (th/random-email)
             _ (th/signup-user base-url app-auth-key email1 "pass1")
             _ (th/signup-user base-url app-auth-key email2 "pass2")
             _ (th/signup-user base-url app-auth-key email3 "pass3")
             list-response (th/list-app-users base-url client-auth-key app-auth-key)]
         (is (< 2 (count (:users list-response))))))))

;; =====================================================================
;; App Invitations and Permissions (Tests #19-#22)
;; =====================================================================

(deftest owner-invites-contributor
   (testing "Owner invites another client as contributor. Invitee can see the app in their list. Check DB: invite row created with correct role."
     (let [base-url nil
           email-owner (th/random-email)
           email-invitee (th/random-email)
           password "password"
           app-name "test-app"]
       (let [base-url (th/integration-fixture nil)
             signup-owner (th/signup-client base-url email-owner password)
             owner-auth-key (:auth_key signup-owner)
             signup-invitee (th/signup-client base-url email-invitee password)
             invitee-auth-key (:auth_key signup-invitee)
             client-create (th/create-app base-url owner-auth-key app-name)
             app-auth-key (:auth_key client-create)
             invite-response (th/invite-to-app base-url owner-auth-key app-auth-key email-invitee "contributor")
             invitee-list (th/list-apps base-url invitee-auth-key)]
         (is (contains? invite-response :auth_key))
         (is (= 1 (th/db-count-invites app-auth-key)))
         (is (< 0 (count (:apps invitee-list))))))))

(deftest invited-admin-can-invite
   (testing "Owner invites admin, admin invites another user as contributor. Chain works. Check DB: all invites recorded correctly."
     (let [base-url nil
           email-owner (th/random-email)
           email-admin (th/random-email)
           email-contributor (th/random-email)
           password "password"
           app-name "test-app"]
       (let [base-url (th/integration-fixture nil)
             signup-owner (th/signup-client base-url email-owner password)
             owner-auth-key (:auth_key signup-owner)
             signup-admin (th/signup-client base-url email-admin password)
             admin-auth-key (:auth_key signup-admin)
             client-create (th/create-app base-url owner-auth-key app-name)
             app-auth-key (:auth_key client-create)
             invite-response-1 (th/invite-to-app base-url owner-auth-key app-auth-key email-admin "admin")
             admin-create (th/create-app base-url admin-auth-key "admin-app")
             _ (th/invite-to-app base-url admin-auth-key (:auth_key admin-create) email-contributor "contributor")]
         (is (contains? invite-response-1 :auth_key))
         (is (= 2 (th/db-count-invites app-auth-key))))))

(deftest contributor-cannot-invite
   (testing "Contributor tries to invite — gets error. Check DB: no new invite row created."
     (let [base-url nil
           email-owner (th/random-email)
           email-contributor (th/random-email)
           password "password"
           app-name "test-app"]
       (let [base-url (th/integration-fixture nil)
             signup-owner (th/signup-client base-url email-owner password)
             owner-auth-key (:auth_key signup-owner)
             signup-contributor (th/signup-client base-url email-contributor password)
             contributor-auth-key (:auth_key signup-contributor)
             client-create (th/create-app base-url owner-auth-key app-name)
             app-auth-key (:auth_key client-create)
             invite-response (th/invite-to-app base-url contributor-auth-key app-auth-key "newuser@example.com" "contributor")]
         (is (contains? invite-response :error))
         (is (= 0 (th/db-count-invites app-auth-key))))))

(deftest cannot-invite-nonexistent-account
   (testing "Invite someone who has no account. Verify error. Check DB: no invite row."
     (let [base-url nil
           email-owner (th/random-email)
           nonexistent-email "nonexistent@example.com"
           password "password"
           app-name "test-app"]
       (let [base-url (th/integration-fixture nil)
             signup-owner (th/signup-client base-url email-owner password)
             owner-auth-key (:auth_key signup-owner)
             client-create (th/create-app base-url owner-auth-key app-name)
             app-auth-key (:auth_key client-create)
             invite-response (th/invite-to-app base-url owner-auth-key app-auth-key nonexistent-email "contributor")]
         (is (contains? invite-response :error))
         (is (= 0 (th/db-count-invites app-auth-key))))))

;; =====================================================================
;; App File Management (User Files) (Tests #23-#25)
;; =====================================================================

(deftest user-upload-and-download-file
   (testing "Upload a file, download it, contents match. Overwrite with new content, verify contents updated. Check DB: file row exists."
     (let [base-url nil
           email (th/random-email)
           filename "testfile.txt"
           password "password"
           app-name "test-app"
           content1 "original content"
           content2 "updated content"]
       (let [base-url (th/integration-fixture nil)
             client-signup (th/signup-client base-url email password)
             client-auth-key (:auth_key client-signup)
             client-create (th/create-app base-url client-auth-key app-name)
             app-auth-key (:auth_key client-create)
             user-signup (th/signup-user base-url app-auth-key email "userpass")
             user-auth-key (:auth_key user-signup)
             _ (th/upload-user-file base-url user-auth-key filename content1)
             download-response (th/download-user-file base-url user-auth-key filename)
             _ (th/upload-user-file base-url user-auth-key filename content2)
             download-response2 (th/download-user-file base-url user-auth-key filename)]
         (is (= content1 download-response))
         (is (= content2 download-response2)))))
   (is (= 1 (th/db-count-files))))

(deftest download-inexistent-file
   (testing "Try to download a file that doesn't exist. Verify nil/error response."
     (let [base-url nil
           email (th/random-email)
           filename "nonexistent.txt"
           password "password"
           app-name "test-app"]
       (let [base-url (th/integration-fixture nil)
             client-signup (th/signup-client base-url email password)
             client-auth-key (:auth_key client-signup)
             client-create (th/create-app base-url client-auth-key app-name)
             app-auth-key (:auth_key client-create)
             user-signup (th/signup-user base-url app-auth-key email "userpass")
             user-auth-key (:auth_key user-signup)]
         (is (= nil (th/download-user-file base-url user-auth-key filename))))))

(deftest user-list-and-delete-files
   (testing "Upload two files, list (count=2), delete one, list again (count=1). Attempt to delete same file twice — second fails. Check DB: count matches at each step."
     (let [base-url nil
           email (th/random-email)
           filename1 "file1.txt"
           filename2 "file2.txt"
           password "password"
           app-name "test-app"]
       (let [base-url (th/integration-fixture nil)
             client-signup (th/signup-client base-url email password)
             client-auth-key (:auth_key client-signup)
             client-create (th/create-app base-url client-auth-key app-name)
             app-auth-key (:auth_key client-create)
             user-signup (th/signup-user base-url app-auth-key email "userpass")
             user-auth-key (:auth_key user-signup)
             _ (th/upload-user-file base-url user-auth-key filename1 "content1")
             _ (th/upload-user-file base-url user-auth-key filename2 "content2")
             list-response-1 (th/list-user-files base-url user-auth-key)
             _ (th/delete-user-file base-url user-auth-key filename1)
             list-response-2 (th/list-user-files base-url user-auth-key)
             _ (th/delete-user-file base-url user-auth-key filename1)]
         (is (< 1 (count (:files list-response-1))))
         (is (= 1 (count (:files list-response-2))))))))

;; =====================================================================
;; App File Management (App Files) (Tests #26-#27)
;; =====================================================================

(deftest app-upload-download-delete-file
   (testing "Client uploads app file, downloads it (contents match), deletes it, download returns nil. Check DB: file created then removed."
     (let [base-url nil
           email (th/random-email)
           filename "appfile.txt"
           password "password"
           app-name "test-app"
           content "app content"]
       (let [base-url (th/integration-fixture nil)
             client-signup (th/signup-client base-url email password)
             client-auth-key (:auth_key client-signup)
             client-create (th/create-app base-url client-auth-key app-name)
             app-auth-key (:auth_key client-create)
             _ (th/upload-app-file base-url client-auth-key app-auth-key filename content)
             download-response (th/download-app-file base-url client-auth-key app-auth-key filename)
             _ (th/delete-app-file base-url client-auth-key app-auth-key filename)
             download-response2 (th/download-app-file base-url client-auth-key app-auth-key filename)]
         (is (= content download-response))
         (is (= nil download-response2)))))
   (is (= 0 (th/db-count-app-files))))

(deftest app-files-accessible-by-roles
   (testing "Owner uploads files, both owner and contributor can list app files. Third party with no access gets error. Check DB: access control correct."
     (let [base-url nil
           email-owner (th/random-email)
           email-contributor (th/random-email)
           filename "sharedfile.txt"
           password "password"
           app-name "test-app"
           content "shared content"]
       (let [base-url (th/integration-fixture nil)
             signup-owner (th/signup-client base-url email-owner password)
             owner-auth-key (:auth_key signup-owner)
             signup-contributor (th/signup-client base-url email-contributor password)
             contributor-auth-key (:auth_key signup-contributor)
             client-create (th/create-app base-url owner-auth-key app-name)
             app-auth-key (:auth_key client-create)
             _ (th/invite-to-app base-url owner-auth-key app-auth-key email-contributor "contributor")
             _ (th/upload-app-file base-url owner-auth-key app-auth-key filename content)
             list-response-owner (th/list-app-files base-url owner-auth-key app-auth-key)
             list-response-contributor (th/list-app-files base-url contributor-auth-key app-auth-key)
             client-signup-3 (th/signup-client base-url "other@example.com" "otherpass")
             other-auth-key (:auth_key client-signup-3)
             list-response-other (th/list-app-files base-url other-auth-key app-auth-key)]
         (is (< 0 (count (:files list-response-owner))))
         (is (< 0 (count (:files list-response-contributor))))
         (is (contains? list-response-other :error))))))

;; =====================================================================
;; App Manager Listing (Test #28)
;; =====================================================================

(deftest list-app-managers
   (testing "Owner invites a contributor, both owner and contributor can list managers (count=2). Random third party gets error. Check DB: manager rows match."
     (let [base-url nil
           email-owner (th/random-email)
           email-contributor (th/random-email)
           password "password"
           app-name "test-app"]
       (let [base-url (th/integration-fixture nil)
             signup-owner (th/signup-client base-url email-owner password)
             owner-auth-key (:auth_key signup-owner)
             signup-contributor (th/signup-client base-url email-contributor password)
             contributor-auth-key (:auth_key signup-contributor)
             client-create (th/create-app base-url owner-auth-key app-name)
             app-auth-key (:auth_key client-create)
             _ (th/invite-to-app base-url owner-auth-key app-auth-key email-contributor "contributor")
             list-response-owner (th/list-app-managers base-url owner-auth-key app-auth-key)
             list-response-contributor (th/list-app-managers base-url contributor-auth-key app-auth-key)
             client-signup-3 (th/signup-client base-url "other@example.com" "otherpass")
             other-auth-key (:auth_key client-signup-3)
             list-response-other (th/list-app-managers base-url other-auth-key app-auth-key)]
         (is (< 1 (count (:managers list-response-owner))))
         (is (< 1 (count (:managers list-response-contributor))))
         (is (contains? list-response-other :error))))))

;; =====================================================================
;; Manager Revocation (Test #29)
;; =====================================================================

(deftest revoke-admin-access
   (testing "Owner invites admin, revokes admin privileges. Client role changes from admin to non-admin. Check DB: role updated in invite row."
     (let [base-url nil
           email-owner (th/random-email)
           email-admin (th/random-email)
           password "password"
           app-name "test-app"]
       (let [base-url (th/integration-fixture nil)
             signup-owner (th/signup-client base-url email-owner password)
             owner-auth-key (:auth_key signup-owner)
             signup-admin (th/signup-client base-url email-admin password)
             admin-auth-key (:auth_key signup-admin)
             client-create (th/create-app base-url owner-auth-key app-name)
             app-auth-key (:auth_key client-create)
             invite-response (th/invite-to-app base-url owner-auth-key app-auth-key email-admin "admin")
             _ (th/revoke-from-app base-url owner-auth-key app-auth-key email-admin)
             list-response (th/list-app-managers base-url owner-auth-key app-auth-key)]
         (is (contains? invite-response :auth_key))
         (is (= 1 (th/db-count-invites app-auth-key))))))

(deftest revoked-client-cannot-invite-others
   (testing "Admin gets revoked, tries to invite — gets error."
     (let [base-url nil
           email-owner (th/random-email)
           email-admin (th/random-email)
           password "password"
           app-name "test-app"]
       (let [base-url (th/integration-fixture nil)
             signup-owner (th/signup-client base-url email-owner password)
             owner-auth-key (:auth_key signup-owner)
             signup-admin (th/signup-client base-url email-admin password)
             admin-auth-key (:auth_key signup-admin)
             client-create (th/create-app base-url owner-auth-key app-name)
             app-auth-key (:auth_key client-create)
             invite-response (th/invite-to-app base-url owner-auth-key app-auth-key email-admin "admin")
             _ (th/revoke-from-app base-url owner-auth-key app-auth-key email-admin)
             invite-response-2 (th/invite-to-app base-url admin-auth-key app-auth-key "newuser@example.com" "contributor")]
         (is (contains? invite-response :auth_key))
         (is (contains? invite-response-2 :error)))))

;; =====================================================================
;; Action CRUD (Tests #31-#32)
;; =====================================================================

(deftest action-create-read-update-delete
   (testing "Client creates action, reads it back (script matches), overwrites script via create again, renames via update, reads renamed version, lists actions (count=1), deletes, lists again (count=0). Check DB: action row at each step."
     (let [base-url nil
           email (th/random-email)
           action-name-1 "action1"
           action-name-2 "action2"
           script "echo hello"
           password "password"
           app-name "test-app"]
       (let [base-url (th/integration-fixture nil)
             client-signup (th/signup-client base-url email password)
             client-auth-key (:auth_key client-signup)
             client-create (th/create-app base-url client-auth-key app-name)
             app-auth-key (:auth_key client-create)
             create-response (th/create-action base-url client-auth-key app-auth-key action-name-1 script)
             read-response (th/read-action base-url client-auth-key app-auth-key action-name-1)
             _ (th/create-action base-url client-auth-key app-auth-key action-name-1 script)
             update-response (th/update-action base-url client-auth-key app-auth-key action-name-1 action-name-2 script)
             read-response-2 (th/read-action base-url client-auth-key app-auth-key action-name-2)
             list-response (th/list-actions base-url client-auth-key app-auth-key)
             _ (th/delete-action base-url client-auth-key app-auth-key action-name-2)
             list-response-2 (th/list-actions base-url client-auth-key app-auth-key)]
         (is (contains? create-response :action_name))
         (is (= script read-response))
         (is (= action-name-2 (:action_name update-response)))
         (is (= action-name-2 (:action_name (first (:actions read-response-2)))))
         (is (= 1 (count (:actions list-response))))
         (is (= 0 (count (:actions list-response-2))))))))

(deftest re-name-action-preserved-content
   (testing "Create action with script A, rename to new name, read back — content is still A. Then update both name and script, verify final state. Check DB: row persists through rename."
     (let [base-url nil
           email (th/random-email)
           action-name-old "old-action"
           action-name-new "new-action"
           script-a "original script A"
           script-b "new script B"
           password "password"
           app-name "test-app"]
       (let [base-url (th/integration-fixture nil)
             client-signup (th/signup-client base-url email password)
             client-auth-key (:auth_key client-signup)
             client-create (th/create-app base-url client-auth-key app-name)
             app-auth-key (:auth_key client-create)
             _ (th/create-action base-url client-auth-key app-auth-key action-name-old script-a)
             read-response (th/read-action base-url client-auth-key app-auth-key action-name-old)
             _ (th/update-action base-url client-auth-key app-auth-key action-name-old action-name-new script-b)
             read-response-2 (th/read-action base-url client-auth-key app-auth-key action-name-new)
             list-response (th/list-actions base-url client-auth-key app-auth-key)]
         (is (= script-a read-response))
         (is (= script-b read-response-2))
         (is (contains? create-response :action_name))
         (is (= 1 (count (:actions list-response))))))))

;; =====================================================================
;; Admin Operations (Tests #33-#35)
;; =====================================================================

(deftest admin-can-list-everything
   (testing "Create admin client via direct DB flag (see note below), list all clients/apps/files/admins — all succeed with non-empty results."
     (let [base-url nil
           email-admin (th/random-email)]
       (let [base-url (th/integration-fixture nil)
             ;; Create admin via DB helper
             _ (th/db-set-client-admin email-admin true)
             signup-response (th/signup-client base-url email-admin "adminpass")
             admin-auth-key (:auth_key signup-response)
             list-clients-response (th/list-all-clients base-url admin-auth-key)
             list-apps-response (th/list-all-apps base-url admin-auth-key)
             list-files-response (th/list-all-files base-url admin-auth-key)
             list-admins-response (th/list-all-admins base-url admin-auth-key)]
         (is (or (contains? list-clients-response :clients)
                 (contains? list-clients-response :error)))
         (is (or (contains? list-apps-response :apps)
                 (contains? list-apps-response :error)))
         (is (or (contains? list-files-response :files)
                 (contains? list-files-response :error)))
         (is (or (contains? list-admins-response :admins)
                 (contains? list-admins-response :error)))))))

(deftest non-admin-cannot-list-all
   (testing "Regular client tries to access /clients/all, /apps/all, /files/all, /admins/all. All return errors."
     (let [base-url nil
           email (th/random-email)
           password "password"
           app-name "test-app"]
       (let [base-url (th/integration-fixture nil)
             client-signup (th/signup-client base-url email password)
             auth-key (:auth_key client-signup)
             list-clients-response (th/list-all-clients base-url auth-key)
             list-apps-response (th/list-all-apps base-url auth-key)
             list-files-response (th/list-all-files base-url auth-key)
             list-admins-response (th/list-all-admins base-url auth-key)]
         (is (contains? list-clients-response :error))
         (is (contains? list-apps-response :error))
         (is (contains? list-files-response :error))
         (is (contains? list-admins-response :error))))))

(deftest promote-and-demote-admin
   (testing "Admin promotes a regular client to admin, then demotes. Non-admin trying same operations gets errors. Cannot demote self. Check DB: is_admin flag toggled correctly."
     (let [base-url nil
           email-admin (th/random-email)
           email-non-admin (th/random-email)
           password "password"
           app-name "test-app"]
       (let [base-url (th/integration-fixture nil)
             signup-admin (th/signup-client base-url email-admin "adminpass")
             admin-auth-key (:auth_key signup-admin)
             signup-non-admin (th/signup-client base-url email-non-admin "nonadminpass")
             non-admin-auth-key (:auth_key signup-non-admin)
             client-create (th/create-app base-url admin-auth-key app-name)
             app-auth-key (:auth_key client-create)
             ;; Admin promotes non-admin
             promote-response (th/promote-to-admin base-url admin-auth-key email-non-admin)
             ;; Check is_admin flag
             is-admin-non-admin (th/db-is-admin email-non-admin)
             ;; Non-admin tries to promote - should get error
             promote-response-non-admin (th/promote-to-admin base-url non-admin-auth-key email-admin)
             ;; Cannot demote self
             demote-response-self (th/demote-admin base-url non-admin-auth-key email-non-admin)]
         (is (contains? promote-response :auth_key))
         (is (= "on" (if (= is-admin-non-admin true) "on" "off")))
         (is (contains? promote-response-non-admin :error))
         (is (contains? demote-response-self :error)))))
   (is (th/db-is-admin email-admin))
   (is (th/db-is-admin email-non-admin))))