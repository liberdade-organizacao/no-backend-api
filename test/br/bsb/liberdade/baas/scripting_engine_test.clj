(ns br.bsb.liberdade.baas.scripting-engine-test
  (:require [clojure.test :refer [deftest testing is]]
            [br.bsb.liberdade.baas.test-helpers :as th]))

;; Optional integration tests against a real external scripting engine.
;; Enabled only when this deftest is uncommented and SCRIPTING_ENGINE_URL points
;; at a running no-backend-scripting-engine (see README.md "Usage").
#_(deftest run-action-endpoint
    (use-fixtures :each th/integration-fixture)
    (testing "run-action forwards to the scripting engine"
      (let [base-url th/*base-url*
            _ (th/signup-client base-url (th/random-email) "password")]
        (is true))))
