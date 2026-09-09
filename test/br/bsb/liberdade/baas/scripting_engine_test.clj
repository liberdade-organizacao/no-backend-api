(ns br.bsb.liberdade.baas.scripting-engine-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [clj-http.client :as http]
            [br.bsb.liberdade.baas.proxies :as proxies]
            [br.bsb.liberdade.baas.test-helpers :as th]))

;; Optional integration tests against a real external scripting engine.
;; Enabled only when these deftests are uncommented and SCRIPTING_ENGINE_URL
;; points at a running no-backend-scripting-engine (see README.md "Usage").
;;
;; The `proxies/run-action` contract forwards only
;; user_id/app_id/action_name/action_param to the engine (`proxies.clj:13-16`)
;; and no script, so the engine must fetch the persisted script out-of-band from
;; the shared DB. Happy-path assertions are written against the `{result, error}`
;; shape and flag the fetch contract for review.

#_(deftest run-action-endpoint
    (use-fixtures :each th/scripting-engine-fixture)
    (testing "run-action forwards to the scripting engine"
      (let [base-url (or th/*base-url* "http://localhost:7780")
            echo-script (slurp "resources/echo-action.lua")
            client-auth-key (:auth_key (th/signup-client base-url (th/random-email) "password"))
            app-auth-key (:auth_key (th/create-app base-url client-auth-key "test-app"))
            user-auth-key (:auth_key (th/signup-user base-url app-auth-key (th/random-email) "userpass"))
            _ (th/create-action base-url client-auth-key app-auth-key "echo-action.lua" echo-script)
            response (th/run-action base-url user-auth-key app-auth-key "echo-action.lua" "world")]
        (is (= "world" (:result response)))
        (is (nil? (:error response))))))

#_(deftest health-reports-engine-status
    (use-fixtures :each th/scripting-engine-fixture)
    (testing "GET /health surfaces the scripting engine's live /health body"
      (let [base-url th/*base-url*
            engine-health (-> (str proxies/scripting-engine-url "/health")
                              (http/get {:timeout 2000})
                              :body)
            api-health (-> (str base-url "/health")
                           (http/get {:as :json :timeout 2000})
                           :body)]
        (is (= engine-health (:scripting api-health))))))

#_(deftest run-action-nonexistent-name
    (use-fixtures :each th/scripting-engine-fixture)
    (testing "running a nonexistent action_name surfaces an error"
      (let [base-url th/*base-url*
            client-auth-key (:auth_key (th/signup-client base-url (th/random-email) "password"))
            app-auth-key (:auth_key (th/create-app base-url client-auth-key "test-app"))
            user-auth-key (:auth_key (th/signup-user base-url app-auth-key (th/random-email) "userpass"))
            response (th/run-action base-url user-auth-key app-auth-key "no-such-action" "param")]
        (is (some? (:error response)))
        (is (nil? (:result response))))))
