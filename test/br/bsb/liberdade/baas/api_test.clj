(ns br.bsb.liberdade.baas.api-test
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [br.bsb.liberdade.baas.api :as api]))

(defn- body-as-stream [data]
  (-> (json/write-str data)
      .getBytes
      java.io.ByteArrayInputStream.))

(defn- mock-json-req [body-map & {:keys [query-string headers]}]
  {:body (body-as-stream body-map)
   :headers (merge {"accept" "text/json"}
                   (or headers {}))
   :query-string (or query-string "")})

(defn- mock-binary-req [& {:keys [headers]}]
  {:body (java.io.ByteArrayInputStream. (byte-array 0))
   :headers (merge {"accept" "text/json"}
                   (or headers {}))
   :query-string ""})

(defn- expect-validation-pass
  "Asserts that `handler` does not reject `req` at the validation layer.
  What happens after validation (a thrown exception due to no real db/secret,
  or a handled response) is not this test's concern."
  [handler req]
  (try
    (let [resp (handler req)]
      (is (not= 400 (:status resp)) "should not be rejected by validation"))
    (catch Exception _
      (is true))))

(defn- expect-validation-error
  "Asserts `resp` is a 400 with the given field->message details."
  [resp expected-details]
  (is (= 400 (:status resp)))
  (let [parsed (json/read-str (:body resp) :key-fn keyword)]
    (is (= "Validation Failed" (:error parsed)))
    (doseq [[field message] expected-details]
      (is (= message (get-in parsed [:details field]))))))

;;;;;;;;;;;;;;;;;;;
; Payload Validation - Positive Cases
;;;;;;;;;;;;;;;;;;;

(deftest clients-signup-valid-payload
  (testing "clients-signup passes valid payload through to biz/new-client"
    (expect-validation-pass api/clients-signup
                            (mock-json-req {"email" "test@example.com" "password" "password123"}))))

(deftest create-app-valid-payload
  (testing "create-app passes valid payload through to biz/new-app"
    (expect-validation-pass api/create-app
                            (mock-json-req {"auth_key" "key123" "app_name" "myapp"}))))

(deftest users-signup-valid-payload
  (testing "users-signup passes valid payload through to biz/new-user"
    (expect-validation-pass api/users-signup
                            (mock-json-req {"app_auth_key" "key123" "email" "user@example.com" "password" "pass123"}))))

;;;;;;;;;;;;;;;;;;;
; Payload Validation - Negative Cases (Missing Keys)
;;;;;;;;;;;;;;;;;;;

(deftest clients-signup-missing-email
  (testing "clients-signup returns 400 when email is missing"
    (expect-validation-error (api/clients-signup (mock-json-req {"password" "password123"}))
                             {:email "is required"})))

(deftest clients-signup-missing-password
  (testing "clients-signup returns 400 when password is missing"
    (expect-validation-error (api/clients-signup (mock-json-req {"email" "test@example.com"}))
                             {:password "is required"})))

(deftest clients-signup-missing-all-fields
  (testing "clients-signup returns 400 when all fields are missing"
    (expect-validation-error (api/clients-signup (mock-json-req {}))
                             {:email "is required" :password "is required"})))

(deftest create-app-missing-fields
  (testing "create-app returns 400 when required fields are missing"
    (expect-validation-error (api/create-app (mock-json-req {}))
                             {:auth_key "is required" :app_name "is required"})))

(deftest delete-app-missing-fields
  (testing "delete-app returns 400 when required fields are missing"
    (expect-validation-error (api/delete-app (mock-json-req {}))
                             {:client_auth_key "is required" :app_auth_key "is required"})))

;;;;;;;;;;;;;;;;;;;
; Payload Validation - Negative Cases (Wrong Types)
;;;;;;;;;;;;;;;;;;;

(deftest clients-signup-wrong-type-password
  (testing "clients-signup returns 400 when password is wrong type"
    (expect-validation-error (api/clients-signup (mock-json-req {"email" "test@example.com" "password" 123}))
                             {:password "must be a string"})))

(deftest create-app-wrong-type-auth-key
  (testing "create-app returns 400 when auth_key is wrong type"
    (expect-validation-error (api/create-app (mock-json-req {"auth_key" 123 "app_name" "myapp"}))
                             {:auth_key "must be a string"})))

;;;;;;;;;;;;;;;;;;;
; Header Validation - Positive Cases
;;;;;;;;;;;;;;;;;;;

(deftest upload-user-file-valid-headers
  (testing "upload-user-file passes valid headers through to biz/upload-user-file"
    (expect-validation-pass api/upload-user-file
                            {:body (body-as-stream "")
                             :headers {"accept" "text/json"
                                       "x-user-auth-key" "userkey123"
                                       "x-filename" "test.txt"}})))

(deftest list-user-files-valid-headers
  (testing "list-user-files passes valid headers through to biz/list-user-files"
    (expect-validation-pass api/list-user-files
                            {:body nil
                             :headers {"accept" "text/json"
                                       "x-user-auth-key" "userkey123"}})))

;;;;;;;;;;;;;;;;;;;
; Header Validation - Negative Cases (Missing Headers)
;;;;;;;;;;;;;;;;;;;

(deftest upload-user-file-missing-headers
  (testing "upload-user-file returns 400 when required headers are missing"
    (expect-validation-error (api/upload-user-file (mock-binary-req))
                             {:x-user-auth-key "is required"})))

(deftest download-user-file-missing-headers
  (testing "download-user-file returns 400 when required headers are missing"
    (expect-validation-error (api/download-user-file (mock-binary-req))
                             {:x-user-auth-key "is required" :x-filename "is required"})))

(deftest list-user-files-missing-headers
  (testing "list-user-files returns 400 when x-user-auth-key is missing"
    (expect-validation-error (api/list-user-files (mock-binary-req))
                             {:x-user-auth-key "is required"})))

(deftest delete-user-file-missing-headers
  (testing "delete-user-file returns 400 when required headers are missing"
    (expect-validation-error (api/delete-user-file (mock-binary-req))
                             {:x-user-auth-key "is required" :x-filename "is required"})))

(deftest upload-app-file-missing-headers
  (testing "upload-app-file returns 400 when required headers are missing"
    (expect-validation-error (api/upload-app-file (mock-binary-req))
                             {:x-client-auth-key "is required"
                              :x-app-auth-key "is required"
                              :x-filename "is required"})))

(deftest download-app-file-missing-headers
  (testing "download-app-file returns 400 when required headers are missing"
    (expect-validation-error (api/download-app-file (mock-binary-req))
                             {:x-client-auth-key "is required"
                              :x-app-auth-key "is required"
                              :x-filename "is required"})))

(deftest delete-app-file-missing-headers
  (testing "delete-app-file returns 400 when required headers are missing"
    (expect-validation-error (api/delete-app-file (mock-binary-req))
                             {:x-client-auth-key "is required"
                              :x-app-auth-key "is required"
                              :x-filename "is required"})))

;;;;;;;;;;;;;;;;;;;
; Query Param Validation - Positive Cases
;;;;;;;;;;;;;;;;;;;

(deftest list-apps-valid-query
  (testing "list-apps passes valid query params through to biz/get-clients-apps"
    (expect-validation-pass api/list-apps
                            (mock-json-req {} :query-string "auth_key=key123"))))

(deftest list-app-users-valid-query
  (testing "list-app-users passes valid query params through to biz/list-app-users"
    (expect-validation-pass api/list-app-users
                            (mock-json-req {} :query-string "client_auth_key=key1&app_auth_key=key2"))))

;;;;;;;;;;;;;;;;;;;
; Query Param Validation - Negative Cases (Missing Params)
;;;;;;;;;;;;;;;;;;;

(deftest list-apps-missing-query-param
  (testing "list-apps returns 400 when auth_key query param is missing"
    (expect-validation-error (api/list-apps (mock-json-req {} :query-string ""))
                             {:auth_key "is required"})))

(deftest list-app-users-missing-query-params
  (testing "list-app-users returns 400 when required query params are missing"
    (expect-validation-error (api/list-app-users (mock-json-req {} :query-string ""))
                             {:client_auth_key "is required" :app_auth_key "is required"})))

(deftest list-app-files-missing-query-params
  (testing "list-app-files returns 400 when required query params are missing"
    (expect-validation-error (api/list-app-files (mock-json-req {} :query-string ""))
                             {:client_auth_key "is required" :app_auth_key "is required"})))

(deftest list-actions-missing-query-params
  (testing "list-actions returns 400 when required query params are missing"
    (expect-validation-error (api/list-actions (mock-json-req {} :query-string ""))
                             {:client_auth_key "is required" :app_auth_key "is required"})))

;;;;;;;;;;;;;;;;;;;
; Admin header validation
;;;;;;;;;;;;;;;;;;;

(deftest list-all-clients-missing-header
  (testing "list-all-clients returns 400 when x-client-auth-key is missing"
    (expect-validation-error (api/list-all-clients (mock-binary-req))
                             {:x-client-auth-key "is required"})))

(deftest list-all-clients-valid-header
  (testing "list-all-clients passes valid header through to biz/list-all-clients"
    (expect-validation-pass api/list-all-clients
                            {:body nil
                             :headers {"accept" "text/json"
                                       "x-client-auth-key" "adminkey123"}})))

;;;;;;;;;;;;;;;;;;;
; Payload validation for remaining handlers
;;;;;;;;;;;;;;;;;;;

(deftest invite-to-app-missing-fields
  (testing "invite-to-app returns 400 when required fields are missing"
    (expect-validation-error (api/invite-to-app (mock-json-req {}))
                             {:inviter_auth_key "is required"
                              :app_auth_key "is required"
                              :invitee_email "is required"})))

(deftest invite-to-app-invalid-email
  (testing "invite-to-app returns 400 when invitee_email is invalid"
    (expect-validation-error (api/invite-to-app (mock-json-req {"inviter_auth_key" "key1"
                                                                "app_auth_key" "key2"
                                                                "invitee_email" "not-an-email"}))
                             {:invitee_email "must be a valid email address"})))

(deftest users-login-missing-fields
  (testing "users-login returns 400 when required fields are missing"
    (expect-validation-error (api/users-login (mock-json-req {}))
                             {:app_auth_key "is required"
                              :email "is required"
                              :password "is required"})))

(deftest delete-user-missing-fields
  (testing "delete-user returns 400 when required fields are missing"
    (expect-validation-error (api/delete-user (mock-json-req {}))
                             {:user_auth_key "is required" :password "is required"})))

(deftest update-client-password-missing-fields
  (testing "update-client-password returns 400 when required fields are missing"
    (expect-validation-error (api/update-client-password (mock-json-req {}))
                             {:auth_key "is required"
                              :old_password "is required"
                              :new_password "is required"})))

(deftest update-user-password-missing-fields
  (testing "update-user-password returns 400 when required fields are missing"
    (expect-validation-error (api/update-user-password (mock-json-req {}))
                             {:user_auth_key "is required"
                              :old_password "is required"
                              :new_password "is required"})))

(deftest delete-client-missing-fields
  (testing "delete-client returns 400 when required fields are missing"
    (expect-validation-error (api/delete-client (mock-json-req {}))
                             {:auth_key "is required" :password "is required"})))

(deftest upload-action-missing-fields
  (testing "upload-action returns 400 when required fields are missing"
    (expect-validation-error (api/upload-action (mock-json-req {}))
                             {:client_auth_key "is required"
                              :app_auth_key "is required"
                              :action_name "is required"
                              :action_script "is required"})))

(deftest update-action-missing-fields
  (testing "update-action returns 400 when required fields are missing"
    (expect-validation-error (api/update-action (mock-json-req {}))
                             {:client_auth_key "is required"
                              :app_auth_key "is required"
                              :old_action_name "is required"
                              :new_action_name "is required"
                              :action_script "is required"})))

(deftest delete-action-missing-fields
  (testing "delete-action returns 400 when required fields are missing"
    (expect-validation-error (api/delete-action (mock-json-req {}))
                             {:client_auth_key "is required"
                              :app_auth_key "is required"
                              :action_name "is required"})))

(deftest run-action-missing-fields
  (testing "run-action returns 400 when required fields are missing"
    (expect-validation-error (api/run-action (mock-json-req {}))
                             {:user_auth_key "is required"
                              :app_auth_key "is required"
                              :action_name "is required"
                              :action_param "is required"})))

(deftest download-action-missing-query-params
  (testing "download-action returns 400 when required query params are missing"
    (expect-validation-error (api/download-action (mock-json-req {} :query-string ""))
                             {:client_auth_key "is required"
                              :app_auth_key "is required"
                              :action_name "is required"})))

(deftest list-app-managers-missing-query-params
  (testing "list-app-managers returns 400 when required query params are missing"
    (expect-validation-error (api/list-app-managers (mock-json-req {} :query-string ""))
                             {:client_auth_key "is required" :app_auth_key "is required"})))

(deftest promote-to-admin-missing-fields
  (testing "promote-to-admin returns 400 when required fields are missing"
    (expect-validation-error (api/promote-to-admin (mock-json-req {}))
                             {:auth_key "is required" :email "is required"})))

(deftest demote-admin-missing-fields
  (testing "demote-admin returns 400 when required fields are missing"
    (expect-validation-error (api/demote-admin (mock-json-req {}))
                             {:auth_key "is required" :email "is required"})))

(deftest revoke-from-app-missing-fields
  (testing "revoke-from-app returns 400 when required fields are missing"
    (expect-validation-error (api/revoke-from-app (mock-json-req {}))
                             {:revoker_auth_key "is required"
                              :app_auth_key "is required"
                              :revokee_email "is required"})))

(deftest revoke-app-manager-missing-fields
  (testing "revoke-app-manager returns 400 when required fields are missing"
    (expect-validation-error (api/revoke-app-manager (mock-json-req {}))
                             {:client_auth_key "is required"
                              :app_auth_key "is required"
                              :email_to_revoke "is required"})))

(deftest upload-actions-missing-headers
  (testing "upload-actions returns 400 when required headers are missing"
    (expect-validation-error (api/upload-actions (mock-binary-req))
                             {:x-client-auth-key "is required" :x-app-auth-key "is required"})))

(deftest list-all-admins-missing-header
  (testing "list-all-admins returns 400 when x-client-auth-key is missing"
    (expect-validation-error (api/list-all-admins (mock-binary-req))
                             {:x-client-auth-key "is required"})))

(deftest check-admin-missing-header
  (testing "check-admin returns 400 when x-client-auth-key is missing"
    (expect-validation-error (api/check-admin (mock-binary-req))
                             {:x-client-auth-key "is required"})))
