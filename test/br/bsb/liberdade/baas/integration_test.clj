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
     (let [base-url (th/integration-fixture nil)
           email (th/random-email)
           password "password"]
       (is false))))
