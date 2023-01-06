(ns br.bsb.liberdade.baas.controllers-test
  (:require [clojure.test :refer :all]
            [br.bsb.liberdade.baas.utils :as utils]
            [br.bsb.liberdade.baas.db :as db]
            [br.bsb.liberdade.baas.controllers :as controllers]))

(deftest handle-clients-accounts--happy-cases
  (testing "Can create an account and login"
    (do
      (db/setup-database)
      (db/run-migrations)
      (let [email "test@example.net"
            password "password"
            is-admin false
            result (controllers/new-client email password is-admin)
            first-auth-key (get result "auth_key" nil)
            error (get result "error" nil)
            is-first-error-nil? (= nil error)
            result (controllers/auth-client email password)
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
          _ (controllers/new-client email password false)
          result (controllers/auth-client email wrong-password)
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
          result (controllers/new-client email password1 false)
          first-auth-key (get result "auth_key" nil)
          first-error (get result "error" nil)
          result (controllers/new-client email password2 false)
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
          result (controllers/new-client email password false)
          auth-key (get result "auth_key" nil)
          app-name "My Shiny App"
          result (controllers/new-app auth-key app-name)
          app-auth-key (get result "auth_key" nil)
          first-error (get result "error" nil)
          result (controllers/delete-app auth-key app-auth-key)
          second-error (get result "error" nil)]
      (is (some? app-auth-key))
      (is (nil? first-error))
      ; TODO list apps to check if the app was there in the first place
      (is (nil? second-error)))
    (db/drop-database)))

; TODO try to delete an app from a user hasn't created the app
(deftest handle-apps--sad-cases
  (testing "Apps from the same owner shouldn't have the same name"
    (db/setup-database)
    (db/run-migrations)
    (let [email "test@example.net"
          password "password"
          result (controllers/new-client email password false)
          owner-auth-key (get result "auth_key" nil)
          app-name "new app"
          result (controllers/new-app owner-auth-key app-name)
          first-error (get result "error" nil)
          result (controllers/new-app owner-auth-key app-name)
          second-error (get result "error" nil)
          email "another_test@example.net"
          result (controllers/new-client email password false)
          owner-auth-key (get result "auth_key" nil)
          result (controllers/new-app owner-auth-key app-name)
          third-error (get result "error" nil)]
      (is (nil? first-error))
      (is (some? second-error))
      (is (nil? third-error)))
    (db/drop-database))
  (testing "Wrong user should be unable to delete app"
    (db/setup-database)
    (db/run-migrations)
    (let [result (controllers/new-client "owner@example.net" "pwd" false)
          owner-auth-key (get result "auth_key" nil)
          result (controllers/new-client "client@example.net" "pwd" false)
          client-auth-key (get result "auth_key" nil)
          app-name "seras victoria"
          result (controllers/new-app owner-auth-key app-name)
          app-auth-key (get result "auth_key" nil)
          result (controllers/delete-app client-auth-key app-auth-key)
          error (get result "error" nil)]
      (is (some? error)))
    (db/drop-database)))

; IDEA rename "controllers" "business"

