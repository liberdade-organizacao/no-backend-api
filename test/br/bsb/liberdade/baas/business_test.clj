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
        (is (= first-auth-key second-auth-key))
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
          result(biz/new-app owner-auth-key "invitation test app")
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
          result(biz/new-app owner-auth-key "invitation test app")
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
    (db/drop-database)))

