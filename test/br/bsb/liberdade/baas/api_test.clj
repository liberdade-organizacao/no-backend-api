(ns br.bsb.liberdade.baas.api-test
    (:require [clojure.test :refer :all]
              [clojure.data.json :as json]
              [br.bsb.liberdade.baas.db :as db]
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

;;;;;;;;;;;;;;;;;;;
; Payload Validation - Positive Cases
;;;;;;;;;;;;;;;;;;;

(deftest clients-signup-valid-payload
  (testing "clients-signup passes valid payload through to biz/new-client"
    (let [req (mock-json-req {"email" "test@example.com" "password" "password123"})]
      (try
        (let [resp (api/clients-signup req)]
          (is (not= 400 (:status resp)) "should not be rejected by validation"))
        (catch Exception _
          (is true))))))

(deftest create-app-valid-payload
  (testing "create-app passes valid payload through to biz/new-app"
    (let [req (mock-json-req {"auth_key" "key123" "app_name" "myapp"})]
      (try
        (let [resp (api/create-app req)]
          (is (not= 400 (:status resp)) "should not be rejected by validation"))
        (catch Exception _
          (is true))))))

(deftest users-signup-valid-payload
  (testing "users-signup passes valid payload through to biz/new-user"
    (let [req (mock-json-req {"app_auth_key" "key123" "email" "user@example.com" "password" "pass123"})]
      (try
        (api/users-signup req)
        (is false "should have thrown or returned a response")
        (catch Exception _
          (is true))))))

;;;;;;;;;;;;;;;;;;;
; Payload Validation - Negative Cases (Missing Keys)
;;;;;;;;;;;;;;;;;;;

(deftest clients-signup-missing-email
  (testing "clients-signup returns 400 when email is missing"
    (let [req (mock-json-req {"password" "password123"})
          resp (api/clients-signup req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= {:error "Validation Failed" :details {:email "is required"}} parsed))))))

(deftest clients-signup-missing-password
  (testing "clients-signup returns 400 when password is missing"
    (let [req (mock-json-req {"email" "test@example.com"})
          resp (api/clients-signup req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= {:error "Validation Failed" :details {:password "is required"}} parsed))))))

(deftest clients-signup-missing-all-fields
  (testing "clients-signup returns 400 when all fields are missing"
    (let [req (mock-json-req {})
          resp (api/clients-signup req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :email])))
        (is (= "is required" (get-in parsed [:details :password])))))))

(deftest create-app-missing-fields
  (testing "create-app returns 400 when required fields are missing"
    (let [req (mock-json-req {})
          resp (api/create-app req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :auth_key])))
        (is (= "is required" (get-in parsed [:details :app_name])))))))

(deftest delete-app-missing-fields
  (testing "delete-app returns 400 when required fields are missing"
    (let [req (mock-json-req {})
          resp (api/delete-app req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :client_auth_key])))
        (is (= "is required" (get-in parsed [:details :app_auth_key])))))))

;;;;;;;;;;;;;;;;;;;
; Payload Validation - Negative Cases (Wrong Types)
;;;;;;;;;;;;;;;;;;;

(deftest clients-signup-wrong-type-password
  (testing "clients-signup returns 400 when password is wrong type"
    (let [req (mock-json-req {"email" "test@example.com" "password" 123})
          resp (api/clients-signup req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "must be a string" (get-in parsed [:details :password])))))))

(deftest create-app-wrong-type-auth-key
  (testing "create-app returns 400 when auth_key is wrong type"
    (let [req (mock-json-req {"auth_key" 123 "app_name" "myapp"})
          resp (api/create-app req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "must be a string" (get-in parsed [:details :auth_key])))))))

;;;;;;;;;;;;;;;;;;;
; Header Validation - Positive Cases
;;;;;;;;;;;;;;;;;;;

(deftest upload-user-file-valid-headers
  (testing "upload-user-file passes valid headers through to biz/upload-user-file"
    (let [req {:body (body-as-stream "")
               :headers {"accept" "text/json"
                         "x-user-auth-key" "userkey123"
                         "x-filename" "test.txt"}}]
      (try
        (api/upload-user-file req)
        (is false "should have thrown or returned a response")
        (catch Exception _
          (is true))))))

(deftest list-user-files-valid-headers
  (testing "list-user-files passes valid headers through to biz/list-user-files"
    (let [req {:body nil
               :headers {"accept" "text/json"
                         "x-user-auth-key" "userkey123"}}]
      (try
        (api/list-user-files req)
        (is false "should have thrown or returned a response")
        (catch Exception _
          (is true))))))

;;;;;;;;;;;;;;;;;;;
; Header Validation - Negative Cases (Missing Headers)
;;;;;;;;;;;;;;;;;;;

(deftest upload-user-file-missing-headers
  (testing "upload-user-file returns 400 when required headers are missing"
    (let [req (mock-binary-req)
          resp (api/upload-user-file req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :x-user-auth-key])))))))

(deftest download-user-file-missing-headers
  (testing "download-user-file returns 400 when required headers are missing"
    (let [req (mock-binary-req)
          resp (api/download-user-file req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :x-user-auth-key])))
        (is (= "is required" (get-in parsed [:details :x-filename])))))))

(deftest list-user-files-missing-headers
  (testing "list-user-files returns 400 when x-user-auth-key is missing"
    (let [req (mock-binary-req)
          resp (api/list-user-files req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :x-user-auth-key])))))))

(deftest delete-user-file-missing-headers
  (testing "delete-user-file returns 400 when required headers are missing"
    (let [req (mock-binary-req)
          resp (api/delete-user-file req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :x-user-auth-key])))
        (is (= "is required" (get-in parsed [:details :x-filename])))))))

(deftest upload-app-file-missing-headers
  (testing "upload-app-file returns 400 when required headers are missing"
    (let [req (mock-binary-req)
          resp (api/upload-app-file req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :x-client-auth-key])))
        (is (= "is required" (get-in parsed [:details :x-app-auth-key])))
        (is (= "is required" (get-in parsed [:details :x-filename])))))))

(deftest download-app-file-missing-headers
  (testing "download-app-file returns 400 when required headers are missing"
    (let [req (mock-binary-req)
          resp (api/download-app-file req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :x-client-auth-key])))
        (is (= "is required" (get-in parsed [:details :x-app-auth-key])))
        (is (= "is required" (get-in parsed [:details :x-filename])))))))

(deftest delete-app-file-missing-headers
  (testing "delete-app-file returns 400 when required headers are missing"
    (let [req (mock-binary-req)
          resp (api/delete-app-file req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :x-client-auth-key])))
        (is (= "is required" (get-in parsed [:details :x-app-auth-key])))
        (is (= "is required" (get-in parsed [:details :x-filename])))))))

;;;;;;;;;;;;;;;;;;;
; Query Param Validation - Positive Cases
;;;;;;;;;;;;;;;;;;;

(deftest list-apps-valid-query
  (testing "list-apps passes valid query params through to biz/get-clients-apps"
    (let [req (mock-json-req {} :query-string "auth_key=key123")]
      (try
        (api/list-apps req)
        (is false "should have thrown or returned a response")
        (catch Exception _
          (is true))))))

(deftest list-app-users-valid-query
  (testing "list-app-users passes valid query params through to biz/list-app-users"
    (let [req (mock-json-req {} :query-string "client_auth_key=key1&app_auth_key=key2")]
      (try
        (let [resp (api/list-app-users req)]
          (is (not= 400 (:status resp)) "should not be rejected by validation"))
        (catch Exception _
          (is true))))))

;;;;;;;;;;;;;;;;;;;
; Query Param Validation - Negative Cases (Missing Params)
;;;;;;;;;;;;;;;;;;;

(deftest list-apps-missing-query-param
  (testing "list-apps returns 400 when auth_key query param is missing"
    (let [req (mock-json-req {} :query-string "")
          resp (api/list-apps req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :auth_key])))))))

(deftest list-app-users-missing-query-params
  (testing "list-app-users returns 400 when required query params are missing"
    (let [req (mock-json-req {} :query-string "")
          resp (api/list-app-users req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :client_auth_key])))
        (is (= "is required" (get-in parsed [:details :app_auth_key])))))))

(deftest list-app-files-missing-query-params
  (testing "list-app-files returns 400 when required query params are missing"
    (let [req (mock-json-req {} :query-string "")
          resp (api/list-app-files req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :client_auth_key])))
        (is (= "is required" (get-in parsed [:details :app_auth_key])))))))

(deftest list-actions-missing-query-params
  (testing "list-actions returns 400 when required query params are missing"
    (let [req (mock-json-req {} :query-string "")
          resp (api/list-actions req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :client_auth_key])))
        (is (= "is required" (get-in parsed [:details :app_auth_key])))))))

;;;;;;;;;;;;;;;;;;;
; Admin header validation
;;;;;;;;;;;;;;;;;;;

(deftest list-all-clients-missing-header
  (testing "list-all-clients returns 400 when x-client-auth-key is missing"
    (let [req (mock-binary-req)
          resp (api/list-all-clients req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :x-client-auth-key])))))))

(deftest list-all-clients-valid-header
  (testing "list-all-clients passes valid header through to biz/list-all-clients"
    (let [req {:body nil
               :headers {"accept" "text/json"
                         "x-client-auth-key" "adminkey123"}}]
      (try
        (api/list-all-clients req)
        (is false "should have thrown or returned a response")
        (catch Exception _
          (is true))))))

;;;;;;;;;;;;;;;;;;;
; Payload validation for remaining handlers
;;;;;;;;;;;;;;;;;;;

(deftest invite-to-app-missing-fields
  (testing "invite-to-app returns 400 when required fields are missing"
    (let [req (mock-json-req {})
          resp (api/invite-to-app req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :inviter_auth_key])))
        (is (= "is required" (get-in parsed [:details :app_auth_key])))
        (is (= "is required" (get-in parsed [:details :invitee_email])))))))

(deftest invite-to-app-invalid-email
  (testing "invite-to-app returns 400 when invitee_email is invalid"
    (let [req (mock-json-req {"inviter_auth_key" "key1"
                              "app_auth_key" "key2"
                              "invitee_email" "not-an-email"})
          resp (api/invite-to-app req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "must be a valid email address"
               (get-in parsed [:details :invitee_email])))))))

(deftest users-login-missing-fields
  (testing "users-login returns 400 when required fields are missing"
    (let [req (mock-json-req {})
          resp (api/users-login req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :app_auth_key])))
        (is (= "is required" (get-in parsed [:details :email])))
        (is (= "is required" (get-in parsed [:details :password])))))))

(deftest delete-user-missing-fields
  (testing "delete-user returns 400 when required fields are missing"
    (let [req (mock-json-req {})
          resp (api/delete-user req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :user_auth_key])))
        (is (= "is required" (get-in parsed [:details :password])))))))

(deftest update-client-password-missing-fields
  (testing "update-client-password returns 400 when required fields are missing"
    (let [req (mock-json-req {})
          resp (api/update-client-password req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :auth_key])))
        (is (= "is required" (get-in parsed [:details :old_password])))
        (is (= "is required" (get-in parsed [:details :new_password])))))))

(deftest update-user-password-missing-fields
  (testing "update-user-password returns 400 when required fields are missing"
    (let [req (mock-json-req {})
          resp (api/update-user-password req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :user_auth_key])))
        (is (= "is required" (get-in parsed [:details :old_password])))
        (is (= "is required" (get-in parsed [:details :new_password])))))))

(deftest delete-client-missing-fields
  (testing "delete-client returns 400 when required fields are missing"
    (let [req (mock-json-req {})
          resp (api/delete-client req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :auth_key])))
        (is (= "is required" (get-in parsed [:details :password])))))))

(deftest upload-action-missing-fields
  (testing "upload-action returns 400 when required fields are missing"
    (let [req (mock-json-req {})
          resp (api/upload-action req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :client_auth_key])))
        (is (= "is required" (get-in parsed [:details :app_auth_key])))
        (is (= "is required" (get-in parsed [:details :action_name])))
        (is (= "is required" (get-in parsed [:details :action_script])))))))

(deftest update-action-missing-fields
  (testing "update-action returns 400 when required fields are missing"
    (let [req (mock-json-req {})
          resp (api/update-action req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :client_auth_key])))
        (is (= "is required" (get-in parsed [:details :app_auth_key])))
        (is (= "is required" (get-in parsed [:details :old_action_name])))
        (is (= "is required" (get-in parsed [:details :new_action_name])))
        (is (= "is required" (get-in parsed [:details :action_script])))))))

(deftest delete-action-missing-fields
  (testing "delete-action returns 400 when required fields are missing"
    (let [req (mock-json-req {})
          resp (api/delete-action req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :client_auth_key])))
        (is (= "is required" (get-in parsed [:details :app_auth_key])))
        (is (= "is required" (get-in parsed [:details :action_name])))))))

(deftest run-action-missing-fields
  (testing "run-action returns 400 when required fields are missing"
    (let [req (mock-json-req {})
          resp (api/run-action req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :user_auth_key])))
        (is (= "is required" (get-in parsed [:details :app_auth_key])))
        (is (= "is required" (get-in parsed [:details :action_name])))
        (is (= "is required" (get-in parsed [:details :action_param])))))))

(deftest download-action-missing-query-params
  (testing "download-action returns 400 when required query params are missing"
    (let [req (mock-json-req {} :query-string "")
          resp (api/download-action req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :client_auth_key])))
        (is (= "is required" (get-in parsed [:details :app_auth_key])))
        (is (= "is required" (get-in parsed [:details :action_name])))))))

(deftest list-app-managers-missing-query-params
  (testing "list-app-managers returns 400 when required query params are missing"
    (let [req (mock-json-req {} :query-string "")
          resp (api/list-app-managers req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :client_auth_key])))
        (is (= "is required" (get-in parsed [:details :app_auth_key])))))))

(deftest promote-to-admin-missing-fields
  (testing "promote-to-admin returns 400 when required fields are missing"
    (let [req (mock-json-req {})
          resp (api/promote-to-admin req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :auth_key])))
        (is (= "is required" (get-in parsed [:details :email])))))))

(deftest demote-admin-missing-fields
  (testing "demote-admin returns 400 when required fields are missing"
    (let [req (mock-json-req {})
          resp (api/demote-admin req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :auth_key])))
        (is (= "is required" (get-in parsed [:details :email])))))))

(deftest revoke-from-app-missing-fields
  (testing "revoke-from-app returns 400 when required fields are missing"
    (let [req (mock-json-req {})
          resp (api/revoke-from-app req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :revoker_auth_key])))
        (is (= "is required" (get-in parsed [:details :app_auth_key])))
        (is (= "is required" (get-in parsed [:details :revokee_email])))))))

(deftest revoke-app-manager-missing-fields
  (testing "revoke-app-manager returns 400 when required fields are missing"
    (let [req (mock-json-req {})
          resp (api/revoke-app-manager req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :client_auth_key])))
        (is (= "is required" (get-in parsed [:details :app_auth_key])))
        (is (= "is required" (get-in parsed [:details :email_to_revoke])))))))

(deftest upload-actions-missing-headers
  (testing "upload-actions returns 400 when required headers are missing"
    (let [req (mock-binary-req)
          resp (api/upload-actions req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :x-client-auth-key])))
        (is (= "is required" (get-in parsed [:details :x-app-auth-key])))))))

(deftest list-all-admins-missing-header
  (testing "list-all-admins returns 400 when x-client-auth-key is missing"
    (let [req (mock-binary-req)
          resp (api/list-all-admins req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :x-client-auth-key])))))))

(deftest check-admin-missing-header
  (testing "check-admin returns 400 when x-client-auth-key is missing"
    (let [req (mock-binary-req)
          resp (api/check-admin req)]
      (is (= 400 (:status resp)))
      (let [parsed (json/read-str (:body resp) :key-fn keyword)]
        (is (= "Validation Failed" (:error parsed)))
        (is (= "is required" (get-in parsed [:details :x-client-auth-key])))))))

