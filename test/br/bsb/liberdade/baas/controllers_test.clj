(ns br.bsb.liberdade.baas.controllers-test
  (:require [clojure.test :refer :all]
            [br.bsb.liberdade.baas.utils :as utils]
            [br.bsb.liberdade.baas.db :as db]
            [br.bsb.liberdade.baas.controllers :as controllers]))

(deftest handle-happy-cases
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

