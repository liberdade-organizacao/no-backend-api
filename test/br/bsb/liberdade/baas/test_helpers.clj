(ns br.bsb.liberdade.baas.test-helpers
  (:require [clojure.test :refer [deftest testing is]]
            [clj-http.client :as http]
            [clojure.data.json :as json]
            [next.jdbc :as jdbc]
            [br.bsb.liberdade.baas.api :as api]
            [br.bsb.liberdade.baas.db :as db])
  (:import java.net.ServerSocket))

(def server-thread-ref (atom nil))
(def current-database-path (atom nil))

(defn- create-new-datasource [database-path]
  (jdbc/get-datasource {:dbtype "sqlite"
                         :dbname database-path}))

(defn wait-for-server [base-url]
  (loop [retry-count 0]
    (if (>= retry-count 10)
      (throw (ex-info "Server did not start within timeout" {:base-url base-url}))
      (try
        (let [response (http/get (str base-url "/health") {:timeout 2000})]
          (cond
            (= (:status response) 200)
            base-url
            :else
            (do (Thread/sleep 1000)
                (recur (inc retry-count)))))
        (catch Exception _
          (Thread/sleep 1000)
          (recur (inc retry-count)))))))

(defn start-server []
  (let [temp-port-socket (ServerSocket. 0)
        port (.getLocalPort temp-port-socket)]
    (.close temp-port-socket)
    (System/setenv "API_PORT" (str port))
    (reset! server-thread-ref
            (doto (Thread. #(api/-main "up"))
              (.setDaemon true)))
    (.start @server-thread-ref)
    (let [base-url (str "http://localhost:" port)]
      (wait-for-server base-url)
      base-url)))

(defn stop-server [_base-url]
  (when-let [thread @server-thread-ref]
    (.interrupt thread))
  (Thread/sleep 500)
  (reset! server-thread-ref nil)
  (db/drop-database)
  (try
    (when-let [database-path @current-database-path]
      (.delete (java.io.File. database-path)))
    (catch Exception _))
  (reset! current-database-path nil))

(defn integration-fixture [test-function]
  (let [temp-database-path (str "/tmp/" (System/currentTimeMillis) "-" (java.util.UUID/randomUUID) ".sqlite")
        new-datasource (create-new-datasource temp-database-path)]
    (alter-var-root #'db/dbname (constantly temp-database-path))
    (alter-var-root #'db/ds (constantly new-datasource))
    (reset! current-database-path temp-database-path)
    (db/setup-database)
    (db/run-migrations)
    (let [base-url (start-server)]
      (try
        (test-function base-url)
        (finally
          (stop-server base-url))))))

(defn random-email []
  (let [random-suffix (-> (java.util.UUID/randomUUID)
                          .toString
                          (.replace-all "-" ""))]
    (str "test_" (subs random-suffix 0 6) "@example.net")))

(defn- json-response [response]
  (:body response))

(defn signup-client [base-url email password]
  (json-response (http/post (str base-url "/clients/signup")
                            {:body (json/write-str {"email" email
                                                    "password" password})
                             :as :json})))

(defn login-client [base-url email password]
  (json-response (http/post (str base-url "/clients/login")
                            {:body (json/write-str {"email" email
                                                    "password" password})
                             :as :json})))

(defn change-client-password [base-url auth-key old-password new-password]
  (json-response (http/post (str base-url "/clients/password")
                            {:body (json/write-str {"auth_key" auth-key
                                                    "old_password" old-password
                                                    "new_password" new-password})
                             :as :json})))

(defn delete-client [base-url auth-key password]
  (json-response (http/request {:method :delete
                                 :url (str base-url "/clients")
                                 :body (json/write-str {"auth_key" auth-key
                                                        "password" password})
                                 :as :json})))

(defn create-app [base-url auth-key app-name]
  (json-response (http/post (str base-url "/apps")
                            {:body (json/write-str {"auth_key" auth-key
                                                    "app_name" app-name})
                             :as :json})))

(defn list-apps [base-url auth-key]
  (json-response (http/get (str base-url "/apps")
                           {:query-params {"auth_key" auth-key}
                            :as :json})))

(defn delete-app [base-url client-auth-key app-auth-key]
  (json-response (http/request {:method :delete
                                 :url (str base-url "/apps")
                                 :body (json/write-str {"client_auth_key" client-auth-key
                                                        "app_auth_key" app-auth-key})
                                 :as :json})))

(defn invite-to-app [base-url inviter-auth-key app-auth-key invitee-email invitee-role]
  (json-response (http/post (str base-url "/apps/invite")
                            {:body (json/write-str {"inviter_auth_key" inviter-auth-key
                                                    "app_auth_key" app-auth-key
                                                    "invitee_email" invitee-email
                                                    "invitee_role" invitee-role})
                             :as :json})))

(defn revoke-from-app [base-url revoker-auth-key app-auth-key revokee-email]
  (json-response (http/post (str base-url "/apps/revoke")
                            {:body (json/write-str {"revoker_auth_key" revoker-auth-key
                                                    "app_auth_key" app-auth-key
                                                    "revokee_email" revokee-email})
                             :as :json})))

(defn signup-user [base-url app-auth-key email password]
  (json-response (http/post (str base-url "/users/signup")
                            {:body (json/write-str {"app_auth_key" app-auth-key
                                                    "email" email
                                                    "password" password})
                             :as :json})))

(defn login-user [base-url app-auth-key email password]
  (json-response (http/post (str base-url "/users/login")
                            {:body (json/write-str {"app_auth_key" app-auth-key
                                                    "email" email
                                                    "password" password})
                             :as :json})))

(defn delete-user [base-url user-auth-key password]
  (json-response (http/request {:method :delete
                                 :url (str base-url "/users")
                                 :body (json/write-str {"user_auth_key" user-auth-key
                                                        "password" password})
                                 :as :json})))

(defn change-user-password [base-url user-auth-key old-password new-password]
  (json-response (http/post (str base-url "/users/password")
                            {:body (json/write-str {"user_auth_key" user-auth-key
                                                    "old_password" old-password
                                                    "new_password" new-password})
                             :as :json})))

(defn list-app-users [base-url client-auth-key app-auth-key]
  (json-response (http/get (str base-url "/apps/users")
                           {:query-params {"client_auth_key" client-auth-key
                                           "app_auth_key" app-auth-key}
                            :as :json})))

(defn upload-user-file [base-url user-auth-key filename contents]
  (json-response (http/post (str base-url "/users/files")
                            {:headers {"x-user-auth-key" user-auth-key
                                       "x-filename" filename}
                             :body contents
                             :as :json})))

(defn download-user-file [base-url user-auth-key filename]
  (:body (http/get (str base-url "/users/files")
                   {:headers {"x-user-auth-key" user-auth-key
                              "x-filename" filename}})))

(defn list-user-files [base-url user-auth-key]
  (json-response (http/get (str base-url "/users/files/list")
                           {:headers {"x-user-auth-key" user-auth-key}
                            :as :json})))

(defn delete-user-file [base-url user-auth-key filename]
  (json-response (http/request {:method :delete
                                 :url (str base-url "/users/files")
                                 :headers {"x-user-auth-key" user-auth-key
                                           "x-filename" filename}
                                 :as :json})))

(defn upload-app-file [base-url client-auth-key app-auth-key filename contents]
  (json-response (http/post (str base-url "/apps/files")
                            {:headers {"x-client-auth-key" client-auth-key
                                       "x-app-auth-key" app-auth-key
                                       "x-filename" filename}
                             :body contents
                             :as :json})))

(defn download-app-file [base-url client-auth-key app-auth-key filename]
  (:body (http/get (str base-url "/apps/files")
                   {:headers {"x-client-auth-key" client-auth-key
                              "x-app-auth-key" app-auth-key
                              "x-filename" filename}})))

(defn list-app-files [base-url client-auth-key app-auth-key]
  (json-response (http/get (str base-url "/apps/files/list")
                           {:query-params {"client_auth_key" client-auth-key
                                           "app_auth_key" app-auth-key}
                            :as :json})))

(defn delete-app-file [base-url client-auth-key app-auth-key filename]
  (json-response (http/request {:method :delete
                                 :url (str base-url "/apps/files")
                                 :headers {"x-client-auth-key" client-auth-key
                                           "x-app-auth-key" app-auth-key
                                           "x-filename" filename}
                                 :as :json})))

(defn list-app-managers [base-url client-auth-key app-auth-key]
  (json-response (http/get (str base-url "/apps/clients")
                           {:query-params {"client_auth_key" client-auth-key
                                           "app_auth_key" app-auth-key}
                            :as :json})))

(defn revoke-app-manager [base-url client-auth-key app-auth-key email-to-revoke]
  (json-response (http/post (str base-url "/apps/clients/revoke")
                            {:body (json/write-str {"client_auth_key" client-auth-key
                                                    "app_auth_key" app-auth-key
                                                    "email_to_revoke" email-to-revoke})
                             :as :json})))

(defn create-action [base-url client-auth-key app-auth-key action-name action-script]
  (json-response (http/post (str base-url "/actions")
                            {:body (json/write-str {"client_auth_key" client-auth-key
                                                    "app_auth_key" app-auth-key
                                                    "action_name" action-name
                                                    "action_script" action-script})
                             :as :json})))

(defn read-action [base-url client-auth-key app-auth-key action-name]
  (:body (http/get (str base-url "/actions")
                   {:query-params {"client_auth_key" client-auth-key
                                   "app_auth_key" app-auth-key
                                   "action_name" action-name}})))

(defn list-actions [base-url client-auth-key app-auth-key]
  (json-response (http/get (str base-url "/actions/list")
                           {:query-params {"client_auth_key" client-auth-key
                                           "app_auth_key" app-auth-key}
                            :as :json})))

(defn update-action [base-url client-auth-key app-auth-key old-action-name new-action-name action-script]
  (json-response (http/request {:method :patch
                                 :url (str base-url "/actions")
                                 :body (json/write-str {"client_auth_key" client-auth-key
                                                        "app_auth_key" app-auth-key
                                                        "old_action_name" old-action-name
                                                        "new_action_name" new-action-name
                                                        "action_script" action-script})
                                 :as :json})))

(defn delete-action [base-url client-auth-key app-auth-key action-name]
  (json-response (http/request {:method :delete
                                 :url (str base-url "/actions")
                                 :body (json/write-str {"client_auth_key" client-auth-key
                                                        "app_auth_key" app-auth-key
                                                        "action_name" action-name})
                                 :as :json})))

(defn list-all-clients [base-url client-auth-key]
  (json-response (http/get (str base-url "/clients/all")
                           {:headers {"x-client-auth-key" client-auth-key}
                            :as :json})))

(defn list-all-apps [base-url client-auth-key]
  (json-response (http/get (str base-url "/apps/all")
                           {:headers {"x-client-auth-key" client-auth-key}
                            :as :json})))

(defn list-all-files [base-url client-auth-key]
  (json-response (http/get (str base-url "/files/all")
                           {:headers {"x-client-auth-key" client-auth-key}
                            :as :json})))

(defn list-all-admins [base-url client-auth-key]
  (json-response (http/get (str base-url "/admins/all")
                           {:headers {"x-client-auth-key" client-auth-key}
                            :as :json})))

(defn promote-to-admin [base-url auth-key email]
  (json-response (http/post (str base-url "/admins")
                            {:body (json/write-str {"auth_key" auth-key
                                                    "email" email})
                             :as :json})))

(defn demote-admin [base-url auth-key email]
  (json-response (http/request {:method :delete
                                 :url (str base-url "/admins")
                                 :body (json/write-str {"auth_key" auth-key
                                                        "email" email})
                                 :as :json})))

(defn check-is-admin [base-url client-auth-key]
  (json-response (http/get (str base-url "/admins/check")
                           {:headers {"x-client-auth-key" client-auth-key}
                            :as :json})))
