(ns br.bsb.liberdade.baas.business-test
  (:require [clojure.test :refer :all]
            [br.bsb.liberdade.baas.utils :as utils]
            [br.bsb.liberdade.baas.db :as db]
            [br.bsb.liberdade.baas.business :as biz]))

(deftest handle-clients-accounts--happy-cases
  (testing "Can create an account and login"
    (do
      (db/setup-database)
      (db/run-migrations)
      (let [email "test@example.net"
            password "password"
            is-admin false
            result (biz/new-client email password is-admin)
            first-auth-key (get result "auth_key" nil)
            error (get result "error" nil)
            is-first-error-nil? (= nil error)
            result (biz/auth-client email password)
            second-auth-key (get result "auth_key" nil)
            error (get result "error" nil)
            is-second-error-nil? (= nil error)]
        (is (= (utils/decode-secret first-auth-key)
               (utils/decode-secret second-auth-key)))
        (is (some? first-auth-key))
        (is is-first-error-nil?)
        (is is-second-error-nil?))
      (db/drop-database))))

(deftest handle-clients-accounts--sad-cases
  (testing "Clients try to login with wrong password"
    (db/setup-database)
    (db/run-migrations)
    (let [email "another-test@example.net"
          password "password"
          wrong-password "wrong password"
          _ (biz/new-client email password false)
          result (biz/auth-client email wrong-password)
          auth-key (get result "auth_key" nil)
          error (get result "error" nil)]
      (is (nil? auth-key))
      (is (some? error)))
    (db/drop-database))
  (testing "Clients try to create the same account twice"
    (db/setup-database)
    (db/run-migrations)
    (let [email "test@example.net"
          password1 "password one"
          password2 "password two"
          result (biz/new-client email password1 false)
          first-auth-key (get result "auth_key" nil)
          first-error (get result "error" nil)
          result (biz/new-client email password2 false)
          second-auth-key (get result "auth_key" nil)
          second-error (get result "error" nil)]
      (is (some? first-auth-key))
      (is (nil? first-error))
      (is (nil? second-auth-key))
      (is (some? second-error)))
    (db/drop-database)))

(deftest handle-apps--happy-cases
  (testing "User can create and delete an app"
    (db/setup-database)
    (db/run-migrations)
    (let [email "client1@example.net"
          password "password"
          result (biz/new-client email password false)
          auth-key (get result "auth_key" nil)
          app-name "My Shiny App"
          result (biz/new-app auth-key app-name)
          app-auth-key (get result "auth_key" nil)
          first-error (get result "error" nil)
          result (biz/get-clients-apps auth-key)
          apps-before-deletion (get result "apps" nil)
          result (biz/delete-app auth-key app-auth-key)
          second-error (get result "error" nil)
          result (biz/get-clients-apps auth-key)
          apps-after-deletion (get result "apps" nil)]
      (is (some? app-auth-key))
      (is (nil? first-error))
      (is (pos? (count apps-before-deletion)))
      (is (nil? second-error))
      (is (= 0 (count apps-after-deletion))))
    (db/drop-database)))

(deftest handle-apps--sad-cases
  (testing "Apps from the same owner shouldn't have the same name"
    (db/setup-database)
    (db/run-migrations)
    (let [email "test@example.net"
          password "password"
          result (biz/new-client email password false)
          owner-auth-key (get result "auth_key" nil)
          app-name "new app"
          result (biz/new-app owner-auth-key app-name)
          first-error (get result "error" nil)
          result (biz/new-app owner-auth-key app-name)
          second-error (get result "error" nil)
          email "another_test@example.net"
          result (biz/new-client email password false)
          owner-auth-key (get result "auth_key" nil)
          result (biz/new-app owner-auth-key app-name)
          third-error (get result "error" nil)]
      (is (nil? first-error))
      (is (some? second-error))
      (is (nil? third-error)))
    (db/drop-database))
  (testing "Wrong user should be unable to delete app"
    (db/setup-database)
    (db/run-migrations)
    (let [result (biz/new-client "owner@example.net" "pwd" false)
          owner-auth-key (get result "auth_key" nil)
          result (biz/new-client "client@example.net" "pwd" false)
          client-auth-key (get result "auth_key" nil)
          app-name "seras victoria"
          result (biz/new-app owner-auth-key app-name)
          app-auth-key (get result "auth_key" nil)
          result (biz/delete-app client-auth-key app-auth-key)
          error (get result "error" nil)]
      (is (some? error)))
    (db/drop-database))
  (testing "Users using the wrong auth key shouldn't be able to do anything"
    (db/setup-database)
    (db/run-migrations)
    (let [result (biz/new-client "owner@example.net" "pwd" false)
          owner-auth-key (get result "auth_key" nil)
          result (biz/new-client "client@example.net" "pwd" false)
          client-auth-key (get result "auth_key" nil)
          app-name "seras victoria"
          wrong-auth-key "random auth key"
          result (biz/new-app owner-auth-key app-name)
          app-auth-key (get result "auth_key" nil)
          result (biz/delete-app wrong-auth-key app-auth-key)
          error (get result "error" nil)]
      (is (some? error)))
    (db/drop-database)))

(deftest invite-clients-to-apps--happy-case
  (testing "clients can invite other clients to manage their apps"
    (db/setup-database)
    (db/run-migrations)
    (let [result (biz/new-client "owner@example.net" "pwd" false)
          owner-auth-key (get result "auth_key" nil)
          invitee-email "invitee@example.net"
          result (biz/new-client invitee-email "pwd2" false)
          client-auth-key (get result "auth_key" nil)
          result (biz/new-app owner-auth-key "invite test app")
          app-auth-key (get result "auth_key" nil)
          result (biz/invite-to-app-by-email owner-auth-key
                                             app-auth-key
                                             invitee-email
                                             "contributor")
          first-error (get result "error" nil)
          result (biz/get-clients-apps client-auth-key)
          apps (get result "apps" nil)]
      (is (nil? first-error))
      (is (pos? (count apps))))
    (db/drop-database))
  (testing "Invited admins can invite other users"
    (db/setup-database)
    (db/run-migrations)
    (let [result (biz/new-client "owner@example.net" "ownerpwd" false)
          owner-auth-key (get result "auth_key" nil)
          invited-admin-email "invited_admin@example.net"
          result (biz/new-client invited-admin-email "pwd" false)
          invited-admin-auth-key (get result "auth_key" nil)
          invited-contrib-email "invited_contributor@example.net"
          result (biz/new-client invited-contrib-email "pwd2" false)
          invited-contrib-auth-key (get result "auth_key" nil)
          result (biz/new-app owner-auth-key "invitation test app")
          app-auth-key (get result "auth_key" nil)
          result (biz/invite-to-app-by-email owner-auth-key
                                             app-auth-key
                                             invited-admin-email
                                             "admin")
          result (biz/get-clients-apps invited-admin-auth-key)
          invited-admin-apps (get result "apps" [])
          result (biz/invite-to-app-by-email invited-admin-auth-key
                                             app-auth-key
                                             invited-contrib-email
                                             "contributor")
          result (biz/get-clients-apps invited-contrib-auth-key)
          invited-contrib-apps (get result "apps" [])]
      (is (pos? (count invited-admin-apps)))
      (is (pos? (count invited-contrib-apps))))
    (db/drop-database)))

(deftest invite-clients-to-apps--sad-cases
  (testing "Inexistent accounts cant be invited"
    (db/setup-database)
    (db/run-migrations)
    (let [result (biz/new-client "owner@example.net" "pwd" false)
          client-auth-key (get result "auth_key" nil)
          result (biz/new-app client-auth-key "yet another test app")
          app-auth-key (get result "auth_key" nil)
          result (biz/invite-to-app-by-email client-auth-key
                                             app-auth-key
                                             "not_here@example.net"
                                             "contributor")
          error (get result "error" nil)]
      (is (some? error)))
    (db/drop-database))
  (testing "Contributors cant invite to apps"
    (db/setup-database)
    (db/run-migrations)
    (let [result (biz/new-client "owner@example.net" "ownerpwd" false)
          owner-auth-key (get result "auth_key" nil)
          invited-admin-email "invited_admin@example.net"
          result (biz/new-client invited-admin-email "pwd" false)
          invited-admin-auth-key (get result "auth_key" nil)
          invited-contrib-email "invited_contributor@example.net"
          result (biz/new-client invited-contrib-email "pwd2" false)
          invited-contrib-auth-key (get result "auth_key" nil)
          result (biz/new-app owner-auth-key "invitation test app")
          app-auth-key (get result "auth_key" nil)
          result (biz/invite-to-app-by-email owner-auth-key
                                             app-auth-key
                                             invited-admin-email
                                             "contributor")
          result (biz/get-clients-apps invited-admin-auth-key)
          invited-admin-apps (get result "apps" [])
          result (biz/invite-to-app-by-email invited-admin-auth-key
                                             app-auth-key
                                             invited-contrib-email
                                             "contributor")
          bad-invitation-error (get result "error" nil)
          result (biz/get-clients-apps invited-contrib-auth-key)
          invited-contrib-apps (get result "apps" [])]
      (is (pos? (count invited-admin-apps)))
      (is (some? bad-invitation-error))
      (is (= 0 (count invited-contrib-apps))))
    (db/drop-database))
  (testing "revoked clients should not invite other clients"
    (db/setup-database)
    (db/run-migrations)
    (let [revoker-email "revoker@pm.me"
          result (biz/new-client "revoker@pm.me" "pwd" false)
          revoker-auth-key (get result "auth_key" nil)
          revokee-email "revokee@gmail.com"
          result (biz/new-client revokee-email "pwd2" false)
          revokee-auth-key (get result "auth_key" nil)
          result (biz/new-app revokee-auth-key "revoking people")
          app-auth-key (get result "auth_key" nil)
          result (biz/invite-to-app-by-email revokee-auth-key
                                             app-auth-key
                                             revoker-email
                                             "admin")
          first-invitation-error (get result "error" nil)
          result (biz/revoke-from-app-by-email revoker-auth-key
                                               app-auth-key
                                               revokee-email)
          revocation-error (get result "error" nil)
          result (biz/invite-to-app-by-email revokee-auth-key
                                             app-auth-key
                                             revoker-email
                                             "contributor")
          second-invitation-error (get result "error" nil)
          result (biz/get-clients-apps revokee-auth-key)
          invokee-apps (get result "apps" nil)]
      (is (nil? first-invitation-error))
      (is (nil? revocation-error))
      (is (some? second-invitation-error))
      (is (-> invokee-apps count (= 0))))
    (db/drop-database)))

(deftest clients-change-password
  (testing "Clients change password -- happy case"
    (db/setup-database)
    (db/run-migrations)
    (let [email "client@example.net"
          old-password "old password"
          result (biz/new-client email old-password false)
          auth-key (get result "auth_key" nil)
          new-password "new password"
          result (biz/change-client-password auth-key old-password new-password)
          error (get result "error" nil)
          result (biz/auth-client email new-password)
          auth-key-again (get result "auth_key" nil)]
      (is (nil? error))
      (is (= (utils/decode-secret auth-key)
             (utils/decode-secret auth-key-again))))
    (db/drop-database))
  (testing "Clients change password -- wrong password"
    (db/setup-database)
    (db/run-migrations)
    (let [email "client@example.net"
          old-password "old password"
          result (biz/new-client email old-password false)
          auth-key (get result "auth_key" nil)
          wrong-password "wrong password"
          new-password "new password"
          result (biz/change-client-password auth-key wrong-password new-password)
          error (get result "error" nil)
          result (biz/auth-client email new-password)
          auth-key-again (get result "auth_key" nil)]
      (is (some? error))
      (is (not= auth-key auth-key-again)))
    (db/drop-database)))

(deftest delete-clients
  (testing "Delete actual client and check if their stuff is not there anymore"
    (db/setup-database)
    (db/run-migrations)
    (let [password "random password"
          result (biz/new-client "user@example.net" password false)
          client-auth-key (get result "auth_key" nil)
          result (biz/new-app client-auth-key "delete client test app")
          app-auth-key (get result "auth_key" nil)
          result (biz/get-clients-apps client-auth-key)
          apps-before (get result "apps" [])
          result (biz/delete-client client-auth-key password)
          error (get result "error" nil)
          result (biz/get-clients-apps client-auth-key)
          apps-after (get result "apps" [])]
      (is (nil? error))
      (is (pos? (count apps-before)))
      (is (= 0 (count apps-after))))
    (db/drop-database))
  (testing "Fails to delete account if password is wrong"
    (db/setup-database)
    (db/run-migrations)
    (let [result (biz/new-client "user@example.net" "correctPassword" false)
          client-auth-key (get result "auth_key" nil)
          result (biz/new-app client-auth-key "delete client test app")
          app-auth-key (get result "auth_key" nil)
          result (biz/get-clients-apps client-auth-key)
          apps-before (get result "apps" [])
          result (biz/delete-client client-auth-key "wrong password")
          error (get result "error" nil)
          result (biz/get-clients-apps client-auth-key)
          apps-after (get result "apps" [])]
      (is (some? error))
      (is (pos? (count apps-before)))
      (is (pos? (count apps-after))))
    (db/drop-database)))

(deftest user-accounts
  (testing "User can create account on app and login"
    (db/setup-database)
    (db/run-migrations)
    (let [result (biz/new-client "owner@example.net" "password" false)
          client-auth-key (get result "auth_key" nil)
          result (biz/new-app client-auth-key "user account test app")
          app-auth-key (get result "auth_key" nil)
          user-email "coolguy@hotmail.com"
          user-password "cool guy yo"
          result (biz/new-user app-auth-key user-email user-password)
          user-auth-key (get result "auth_key" nil)
          first-error (get result "error" nil)
          result (biz/auth-user app-auth-key user-email user-password)
          user-auth-key-again (get result "auth_key" nil)
          second-error (get result "error" nil)]
      (is (some? user-auth-key))
      (is (some? user-auth-key-again))
      (is (= (utils/decode-secret user-auth-key)
             (utils/decode-secret user-auth-key-again)))
      (is (nil? first-error))
      (is (nil? second-error)))
    (db/drop-database))
  (testing "User can create accounts on multiple apps with the same email"
    (db/setup-database)
    (db/run-migrations)
    (let [result (biz/new-client "owner@example.net" "password" false)
          client-auth-key (get result "auth_key" nil)
          result (biz/new-app client-auth-key "first test app")
          first-app-auth-key (get result "auth_key" nil)
          result (biz/new-app client-auth-key "second test app")
          second-app-auth-key (get result "auth_key" nil)
          user-email "coolguy@hotmail.com"
          user-password "cool guy yo"
          result (biz/new-user first-app-auth-key user-email user-password)
          first-user-auth-key (get result "auth_key" nil)
          first-error (get result "error" nil)
          result (biz/new-user second-app-auth-key user-email user-password)
          second-user-auth-key (get result "auth_key" nil)
          second-error (get result "error" nil)]
      (is (some? first-user-auth-key))
      (is (nil? first-error))
      (is (some? second-user-auth-key))
      (is (nil? second-error)))
    (db/drop-database))
  (testing "users can change their password"
    (db/setup-database)
    (db/run-migrations)
    (let [result (biz/new-client "owner@liberdade.bsb.br" "passwordy" false)
          client-auth-key (get result "auth_key" nil)
          result (biz/new-app client-auth-key "change user password  app")
          app-auth-key (get result "auth_key" nil)
          user-email "user@liberdade.bsb.br"
          old-password "passwordsAREnice"
          new-password "but passphrases are better"
          result (biz/new-user app-auth-key user-email old-password)
          user-auth-key (get result "auth_key" nil)
          result (biz/update-user-password user-auth-key old-password new-password)
          error (get result "error" nil)
          result (biz/auth-user app-auth-key user-email new-password)
          user-auth-key-again (get result "auth_key" nil)]
      (is (nil? error))
      (is (= (utils/decode-secret user-auth-key)
             (utils/decode-secret user-auth-key-again))))
    (db/drop-database))
  (testing "clients can list the users from their apps"
    (db/setup-database)
    (db/run-migrations)
    (let [result (biz/new-client "owner@liberdade.bsb.br" "pwd" false)
          client-auth-key (get result "auth_key" nil)
          result (biz/new-app client-auth-key "list users apps")
          app-auth-key (get result "auth_key" nil)
          _ (biz/new-user app-auth-key "user1@gmail.com" "pwd")
          _ (biz/new-user app-auth-key "user2@gmail.com" "pwd")
          _ (biz/new-user app-auth-key "user3@gmail.com" "pwd")
          result (biz/list-app-users client-auth-key app-auth-key)
          error (get result "error" nil)
          users (get result "users" nil)]
      (is (nil? error))
      (is (some? users))
      (is (= 3 (count users))))
    (db/drop-database)))

(deftest user-accounts--error-handling
  (testing "User cannot create accounts with the same email on the same app"
    (db/setup-database)
    (db/run-migrations)
    (let [result (biz/new-client "owner@example.net" "password" false)
          client-auth-key (get result "auth_key" nil)
          result (biz/new-app client-auth-key "first test app")
          app-auth-key (get result "auth_key" nil)
          user-email "coolguy@hotmail.com"
          user-password "cool guy yo"
          result (biz/new-user app-auth-key user-email user-password)
          first-user-auth-key (get result "auth_key" nil)
          first-error (get result "error" nil)
          result (biz/new-user app-auth-key user-email user-password)
          second-user-auth-key (get result "auth_key" nil)
          second-error (get result "error" nil)]
      (is (some? first-user-auth-key))
      (is (nil? first-error))
      (is (nil? second-user-auth-key))
      (is (some? second-error)))
    (db/drop-database))
  (testing "User cannot login with wrong password on app"
    (db/setup-database)
    (db/run-migrations)
    (let [result (biz/new-client "owner@example.net" "password" false)
          client-auth-key (get result "auth_key" nil)
          result (biz/new-app client-auth-key "first test app")
          app-auth-key (get result "auth_key" nil)
          user-email "coolguy@hotmail.com"
          user-password "cool guy yo"
          wrong-password "wrong password"
          result (biz/new-user app-auth-key user-email user-password)
          first-user-auth-key (get result "auth_key" nil)
          first-error (get result "error" nil)
          result (biz/auth-user app-auth-key user-email wrong-password)
          second-user-auth-key (get result "auth_key" nil)
          second-error (get result "error" nil)]
      (is (some? first-user-auth-key))
      (is (nil? first-error))
      (is (nil? second-user-auth-key))
      (is (some? second-error)))
    (db/drop-database))
  (testing "Deleted user cannot login anymore"
    (db/setup-database)
    (db/run-migrations)
    (let [result (biz/new-client "owner@example.net" "password" false)
          client-auth-key (get result "auth_key" nil)
          result (biz/new-app client-auth-key "first test app")
          app-auth-key (get result "auth_key" nil)
          user-email "coolguy@hotmail.com"
          user-password "cool guy yo"
          result (biz/new-user app-auth-key user-email user-password)
          first-user-auth-key (get result "auth_key" nil)
          first-error (get result "error" nil)
          result (biz/auth-user app-auth-key user-email user-password)
          second-user-auth-key (get result "auth_key" nil)
          second-error (get result "error" nil)
          result (biz/delete-user second-user-auth-key user-password)
          deletion-error (get result "error" nil)
          result (biz/auth-user app-auth-key user-email user-password)
          third-user-auth-key (get result "auth_key" nil)
          third-error (get result "error" nil)]
      (is (some? first-user-auth-key))
      (is (nil? first-error))
      (is (some? second-user-auth-key))
      (is (nil? second-error))
      (is (nil? third-user-auth-key))
      (is (some? third-error))
      (is (nil? deletion-error)))
    (db/drop-database)))

(deftest test-file-upload-and-download
  (testing "test if it's possible to upload and download user files"
    (db/setup-database)
    (db/run-migrations)
    (let [result (biz/new-client "owner@example.net" "password" false)
          client-auth-key (get result "auth_key" nil)
          result (biz/new-app client-auth-key "file test app")
          app-auth-key (get result "auth_key" nil)
          result (biz/new-user app-auth-key "fud@nft.io" "pwd")
          user-auth-key (get result "auth_key" nil)
          filename "photo.jpg"
          initial-contents (slurp "resources/pokemon.jpg")
          result (biz/upload-user-file user-auth-key
                                       filename
                                       initial-contents)
          upload-error (get result "error" nil)
          initial-contents-again (biz/download-user-file user-auth-key
                                                         filename)
          final-contents (slurp "resources/animal_crossing.jpg")
          result (biz/upload-user-file user-auth-key
                                       filename
                                       final-contents)
          update-error (get result "error" nil)
          final-contents-again (biz/download-user-file user-auth-key
                                                       filename)]
      (is (nil? upload-error))
      (is (= initial-contents initial-contents-again))
      (is (nil? update-error))
      (is (= final-contents final-contents-again))
      (is (not= initial-contents-again final-contents-again)))
    (db/drop-database))
  (testing "test if it's possible to upload and download app files"
    (db/setup-database)
    (db/run-migrations)
    (let [result (biz/new-client "app_file@html.com" "htmlsux" false)
          client-auth-key (get result "auth_key" nil)
          result (biz/new-app client-auth-key "app with files")
          app-auth-key (get result "auth_key" nil)
          filename "file.txt"
          contents "contents"
          result (biz/upload-app-file client-auth-key
                                      app-auth-key
                                      filename
                                      contents)
          upload-error (get result "error" nil)
          result (biz/download-app-file client-auth-key
                                        app-auth-key
                                        filename)

          downloaded-contents result
          result (biz/delete-app-file client-auth-key
                                      app-auth-key
                                      filename)
          deletion-error (get result "error" nil)
          result (biz/download-app-file client-auth-key
                                        app-auth-key
                                        filename)
          not-downloaded-contents result]
      (is (nil? upload-error))
      (is (= contents downloaded-contents))
      (is (nil? deletion-error))
      (is (nil? not-downloaded-contents)))
    (db/drop-database))
  (testing "downloading inexistent files"
    (db/setup-database)
    (db/run-migrations)
    (let [result (biz/new-client "owner@example.net" "password" false)
          client-auth-key (get result "auth_key" nil)
          result (biz/new-app client-auth-key "file test app")
          app-auth-key (get result "auth_key" nil)
          result (biz/new-user app-auth-key "fud@nft.io" "pwd")
          user-auth-key (get result "auth_key" nil)
          download-result (biz/download-user-file user-auth-key
                                                  "random_file.txt")]
      (is (nil? download-result)))
    (db/drop-database)))

(deftest list-and-delete-files
  (testing "list and delete files"
    (db/setup-database)
    (db/run-migrations)
    (let [result (biz/new-client "owner@example.net" "password" false)
          client-auth-key (get result "auth_key" nil)
          result (biz/new-app client-auth-key "file test app")
          app-auth-key (get result "auth_key" nil)
          result (biz/new-user app-auth-key "fud@nft.io" "pwd")
          user-auth-key (get result "auth_key" nil)
          contents (slurp "resources/pokemon.jpg")
          result (biz/upload-user-file user-auth-key
                                       "pokemon.jpg"
                                       contents)
          first-upload-error (get result "error" nil)
          contents (slurp "resources/animal_crossing.jpg")
          result (biz/upload-user-file user-auth-key
                                       "animal_crossing.jpg"
                                       contents)
          second-upload-error (get result "error" nil)
          filenames-before (biz/list-user-files user-auth-key)
          result (biz/delete-user-file user-auth-key "pokemon.jpg")
          first-deletion-error (get result "error" nil)
          filenames-after (biz/list-user-files user-auth-key)
          result (biz/delete-user-file user-auth-key "pokemon.jpg")
          second-deletion-error (get result "error" nil)]
      (is (nil? first-upload-error))
      (is (nil? second-upload-error))
      (is (nil? first-deletion-error))
      (is (some? second-deletion-error))
      (is (= 2 (count filenames-before)))
      (is (= 1 (count filenames-after))))
    (db/drop-database)))

(deftest list-app-files
  (testing "Only admin and contrib clients can list app files"
    (db/setup-database)
    (db/run-migrations)
    (let [result (biz/new-client "owner@example.net" "pwd" false)
          owner-auth-key (get result "auth_key" nil)
          result (biz/new-app owner-auth-key "test list files app")
          app-auth-key (get result "auth_key" nil)
          contrib-email "contrib@example.net"
          result (biz/new-client contrib-email "passw" false)
          contrib-auth-key (get result "auth_key" nil)
          _ (biz/invite-to-app-by-email owner-auth-key
                                        app-auth-key
                                        contrib-email
                                        "contributor")
          result (biz/new-client "thridparty@example.net" "asdf" false)
          thirdparty-auth-key (get result "auth_key" nil)
          result (biz/new-user app-auth-key "user@example.net" "evil twin")
          user-auth-key (get result "auth_key" nil)
          contents (slurp "resources/pokemon.jpg")
          _ (biz/upload-user-file user-auth-key "pokemon.jpg" contents)
          contents (slurp "resources/animal_crossing.jpg")
          _ (biz/upload-user-file user-auth-key "animal_crossing.jpg" contents)
          result (biz/list-app-files owner-auth-key app-auth-key)
          owner-error (get result "error" nil)
          owner-files (get result "files" nil)
          result (biz/list-app-files contrib-auth-key app-auth-key)
          contrib-error (get result "error" nil)
          contrib-files (get result "files" nil)
          result (biz/list-app-files thirdparty-auth-key app-auth-key)
          thirdparty-error (get result "error" nil)
          thirdparty-files (get result "files" nil)]
      (is (nil? owner-error))
      (is (pos? (count owner-files)))
      (is (nil? contrib-error))
      (is (pos? (count contrib-files)))
      (is (some? thirdparty-error))
      (is (= 0 (count thirdparty-files))))
    (db/drop-database)))

(deftest list-app-managers
  (testing "Let clients list managers in an app"
    (db/setup-database)
    (db/run-migrations)
    (let [result (biz/new-client "owner@liberdade.bsb.br" "pwd" false)
          owner-auth-key (get result "auth_key" nil)
          result (biz/new-app owner-auth-key "list managers app")
          app-auth-key (get result "auth_key" nil)
          contrib-email "contrib@liberdade.bsb.br"
          result (biz/new-client contrib-email "pwd2" false)
          contrib-auth-key (get result "auth_key" nil)
          result (biz/new-client "random@gmail.com" "pwd3" false)
          random-auth-key (get result "auth_key" nil)
          result (biz/invite-to-app-by-email owner-auth-key app-auth-key contrib-email "contributor")
          invitation-error (get result "error" nil)
          result (biz/list-app-managers owner-auth-key app-auth-key)
          owner-error (get result "error" nil)
          owner-managers (get result "clients" nil)
          result (biz/list-app-managers contrib-auth-key app-auth-key)
          contrib-error (get result "error" nil)
          contrib-managers (get result "clients" nil)
          result (biz/list-app-managers random-auth-key app-auth-key)
          random-error (get result "error" nil)
          random-managers (get result "clients" nil)]
      (is (nil? invitation-error))
      (is (nil? owner-error))
      (is (some? owner-managers))
      (is (= 2 (count owner-managers)))
      (is (nil? contrib-error))
      (is (some? contrib-managers))
      (is (= 2 (count contrib-managers)))
      (is (some? random-error))
      (is (nil? random-managers)))
    (db/drop-database)))

(deftest revoke-manager-access-to-app
  (testing "clients can revoke access of other clients from their apps"
    (db/setup-database)
    (db/run-migrations)
    (let [result (biz/new-client "admin@liberdade.bsb.br" "pwd" false)
          owner-auth-key (get result "auth_key" nil)
          client-email "client@liberdade.bsb.br"
          result (biz/new-client client-email "pwd2" false)
          client-auth-key (get result "auth_key" nil)
          result (biz/new-app owner-auth-key "revoke managers app")
          app-auth-key (get result "auth_key" nil)
          result (biz/invite-to-app-by-email owner-auth-key app-auth-key client-email "admin")
          invitation-error (get result "error" nil)
          result (biz/get-client-role-in-app client-auth-key app-auth-key)
          was-admin-before (-> result :role (= "admin"))
          result (biz/revoke-admin-access owner-auth-key
                                          app-auth-key
                                          client-email)
          removal-error (get result "error" nil)
          result (biz/get-client-role-in-app client-auth-key app-auth-key)
          is-admin-after (-> result :role (= "admin"))]
      (is (nil? invitation-error))
      (is (true? was-admin-before))
      (is (nil? removal-error))
      (is (false? is-admin-after)))
    (db/drop-database)))

(def action-script-A "
  function main(param)
    print(\"hi\")
    return param
  end
")

(def action-script-B "
  function main(param)
    print(\"hello\")
    return param
  end
")

(deftest actions
  (testing "Clients can create, read, update, and delete actions, as well as listing actions from an app"
    (db/setup-database)
    (db/run-migrations)
    (let [result (biz/new-client "voldemort@hogwarts.co.uk" "fsckharry" false)
          client-auth-key (get result "auth_key" nil)
          result (biz/new-app client-auth-key "test crud actions")
          app-auth-key (get result "auth_key" nil)
          action-name "new_action.sql"
          result (biz/upsert-action client-auth-key
                                    app-auth-key
                                    action-name
                                    action-script-A)
          creation-error (get result "error" nil)
          first-gotten-script (biz/read-action client-auth-key
                                               app-auth-key
                                               action-name)
          _ (biz/upsert-action client-auth-key
                               app-auth-key
                               action-name
                               action-script-B)
          second-gotten-script (biz/read-action client-auth-key
                                                app-auth-key
                                                action-name)
          temp-action-name "strange_aeons.lua"
          result (biz/update-action client-auth-key
                                    app-auth-key
                                    action-name
                                    temp-action-name
                                    action-script-A)
          first-rename-error (get result "error" nil)
          third-gotten-script (biz/read-action client-auth-key
                                               app-auth-key
                                               temp-action-name)
          result (biz/update-action client-auth-key
                                    app-auth-key
                                    temp-action-name
                                    action-name
                                    action-script-B)
          forth-gotten-script (biz/read-action client-auth-key
                                               app-auth-key
                                               action-name)
          second-rename-error (get result "error" nil)
          action-list-before (biz/list-actions client-auth-key app-auth-key)
          result (biz/delete-action client-auth-key
                                    app-auth-key
                                    action-name)
          deletion-error (get result "error" nil)
          action-list-after (biz/list-actions client-auth-key
                                              app-auth-key)]
      (is (nil? creation-error))
      (is (= first-gotten-script action-script-A))
      (is (= second-gotten-script action-script-B))
      (is (nil? first-rename-error))
      (is (nil? second-rename-error))
      (is (= third-gotten-script action-script-A))
      (is (= forth-gotten-script action-script-B))
      (is (= 1 (count action-list-before)))
      (is (= 0 (count action-list-after)))
      (is (nil? deletion-error)))
    (db/drop-database)))

(deftest admin-test
  (testing "Only admins can list all of a thing"
    (db/setup-database)
    (db/run-migrations)
    (let [result (biz/new-client "admin@liberdade.bsb.br" "senha" true)
          admin-auth-key (get result "auth_key" nil)
          result (biz/list-all-clients admin-auth-key)
          app-creation-error (biz/new-app admin-auth-key "random app")
          app-auth-key (get app-creation-error "auth_key" nil)
          user-creation-result (biz/new-user app-auth-key "user@example.net" "yeah yeah")
          user-auth-key (get user-creation-result "auth_key" nil)
          contents (slurp "resources/pokemon.jpg")
          _ (biz/upload-user-file user-auth-key
                                  "pokemon.jpg"
                                  contents)
          all-clients (get result "clients" nil)
          clients-error (get result "error" nil)
          result (biz/list-all-apps admin-auth-key)
          all-apps (get result "apps" nil)
          apps-error (get result "error" nil)
          result (biz/list-all-files admin-auth-key)
          all-files (get result "files" nil)
          files-error (get result "error" nil)
          result (biz/list-all-admins admin-auth-key)
          all-admins (get result "admins" nil)
          admins-error (get result "error" nil)]
      (is (some? all-clients))
      (is (nil? clients-error))
      (is (some? all-apps))
      (is (pos? (count all-apps)))
      (is (nil? apps-error))
      (is (some? all-files))
      (is (pos? (count all-files)))
      (is (nil? files-error))
      (is (some? all-admins))
      (is (nil? admins-error)))
    (db/drop-database))
  (testing "Regular users can't lists all of a thing"
    (db/setup-database)
    (db/run-migrations)
    (let [result (biz/new-client "regular@hotmail.com" "pwd" false)
          client-auth-key (get result "auth_key" nil)
          result (biz/list-all-clients client-auth-key)
          all-clients (get result "clients" nil)
          clients-error (get result "error" nil)
          result (biz/list-all-apps client-auth-key)
          all-apps (get result "apps" nil)
          apps-error (get result "error" nil)
          result (biz/list-all-files client-auth-key)
          all-files (get result "files" nil)
          files-error (get result "error" nil)
          result (biz/list-all-admins client-auth-key)
          all-admins (get result "admins" nil)
          admins-error (get result "error" nil)]
      (is (nil? all-clients))
      (is (some? clients-error))
      (is (nil? all-apps))
      (is (some? apps-error))
      (is (nil? all-files))
      (is (some? files-error))
      (is (nil? all-admins))
      (is (some? admins-error)))
    (db/drop-database)))

(deftest promote-and-demote-admins
  (testing "only admins can promote and demote admins"
    (db/setup-database)
    (db/run-migrations)
    (let [admin-email "admin@liberdade.bsb.br"
          result (biz/new-client admin-email "senha" true)
          admin-auth-key (get result "auth_key" nil)
          user-email "regular@hotmail.com"
          result (biz/new-client user-email "password" false)
          user-auth-key (get result "auth_key" nil)
          result (biz/promote-to-admin user-auth-key admin-email)
          user-promotion-error (get result "error" nil)
          result (biz/demote-admin user-auth-key admin-email)
          user-demotion-error (get result "error" nil)
          result (biz/promote-to-admin admin-auth-key user-email)
          admin-promotion-error (get result "error" nil)
          result (biz/demote-admin user-auth-key admin-email)
          admin-demotion-error (get result "error" nil)
          result (biz/demote-admin admin-auth-key user-auth-key)
          get-pwned (get result "error" nil)]
      (is (some? user-promotion-error))
      (is (some? user-demotion-error))
      (is (nil? admin-promotion-error))
      (is (nil? admin-demotion-error))
      (is (some? get-pwned)))
    (db/drop-database)))

(deftest check-if-admin
  (testing "check if client is an admin"
    (db/setup-database)
    (db/run-migrations)
    (let [result (biz/new-client "admin@liberdade.bsb.br" "senha" true)
          admin-auth-key (get result "auth_key" nil)
          result (biz/new-client "random@hotmail.com" "password" false)
          client-auth-key (get result "auth_key" nil)
          result (biz/check-admin admin-auth-key)
          admin-error (get result "error" nil)
          result (biz/check-admin client-auth-key)
          regular-error (get result "error" nil)]
      (is (nil? admin-error))
      (is (some? regular-error)))
    (db/drop-database)))

