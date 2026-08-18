(ns br.bsb.liberdade.baas.test-helpers
  (:require [clojure.test :refer [deftest testing is]]
            [clj-http.client :as http]
            [clojure.data.json :as json]
            [next.jdbc :as jdbc]
            [org.httpkit.server :as server]
            [jumblerg.middleware.cors :refer [wrap-cors]]
            [br.bsb.liberdade.baas.api :as api]
            [br.bsb.liberdade.baas.db :as db]
            [br.bsb.liberdade.baas.utils :as utils]
            [next.jdbc.result-set :as rs])
  (:import java.net.ServerSocket))

;; ###############################
;; # SERVER / DATABASE LIFECYCLE #
;; ###############################

(def server-stop-fn (atom nil))
(def current-database-path (atom nil))
(def ^:dynamic *base-url* nil)

(defn- create-new-datasource [database-path]
  (jdbc/get-datasource {:dbtype "sqlite"
                        :dbname database-path}))

(defn- try-health-check [base-url]
  (try
    (= 200 (:status (http/get (str base-url "/health") {:timeout 2000})))
    (catch Exception _ false)))

(defn wait-for-server [base-url]
  (loop [retry-count 0]
    (if (>= retry-count 10)
      (throw (ex-info "Server did not start within timeout" {:base-url base-url}))
      (if (try-health-check base-url)
        base-url
        (do (Thread/sleep 1000)
            (recur (inc retry-count)))))))

(defn start-server []
  (let [temp-port-socket (ServerSocket. 0)
        port (.getLocalPort temp-port-socket)]
    (.close temp-port-socket)
    ;; `run-server` is non-blocking: it starts its own worker threads and
    ;; returns a stop function right away, so no wrapper thread is needed.
    (reset! server-stop-fn
            (server/run-server (wrap-cors #'api/app-routes #".*" {:security nil})
                               {:port port}))
    (let [base-url (str "http://localhost:" port)]
      (wait-for-server base-url)
      base-url)))

(defn stop-server [_base-url]
  (when-let [stop-fn @server-stop-fn]
    (stop-fn :timeout 100))
  (reset! server-stop-fn nil)
  (db/drop-database)
  (try
    (when-let [database-path @current-database-path]
      (.delete (java.io.File. database-path)))
    (catch Exception _))
  (reset! current-database-path nil))

;; `integration-fixture` is registered with `use-fixtures`, which invokes it
;; as `(fixture-fn test-thunk)` and calls `test-thunk` with zero arguments:
;; deftest bodies can't accept a base-url parameter. The server's base-url is
;; instead published through the `*base-url*` dynamic var for the duration of
;; the test.
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
        (binding [*base-url* base-url]
          (test-function))
        (finally
          (stop-server base-url))))))

(defn random-email []
  (let [random-suffix (-> (java.util.UUID/randomUUID)
                          .toString
                          (.replaceAll "-" ""))]
    (str "test_" (subs random-suffix 0 6) "@example.net")))

;; #####################
;; # HTTP API WRAPPERS #
;; #####################

(defn- json-response [response]
  (:body response))

;; Missing files/actions are served by routes that return `nil`/empty body,
;; which compojure turns into a bodiless 404 rather than a JSON error payload.
(defn- raw-response-or-nil [response]
  (let [body (:body response)]
    (if (or (= 404 (:status response)) (nil? body) (= "" body))
      nil
      body)))

(defn- post-json [base-url path params]
  (json-response (http/post (str base-url path)
                            {:body (json/write-str params)
                             :as :json
                             :throw-exceptions false})))

(defn- request-json [method base-url path params]
  (json-response (http/request {:method method
                                :url (str base-url path)
                                :body (json/write-str params)
                                :as :json
                                :throw-exceptions false})))

(defn- get-json [base-url path query-params]
  (json-response (http/get (str base-url path)
                           {:query-params query-params
                            :as :json
                            :throw-exceptions false})))

(defn- get-json-headers [base-url path headers]
  (json-response (http/get (str base-url path)
                           {:headers headers
                            :as :json
                            :throw-exceptions false})))

(defn- delete-json-headers [base-url path headers]
  (json-response (http/request {:method :delete
                                :url (str base-url path)
                                :headers headers
                                :as :json
                                :throw-exceptions false})))

(defn- get-raw-headers [base-url path headers]
  (raw-response-or-nil (http/get (str base-url path)
                                 {:headers headers
                                  :throw-exceptions false})))

(defn- get-raw [base-url path query-params]
  (raw-response-or-nil (http/get (str base-url path)
                                 {:query-params query-params
                                  :throw-exceptions false})))

(defn- post-raw-headers [base-url path headers contents]
  (json-response (http/post (str base-url path)
                            {:headers headers
                             :body contents
                             :as :json
                             :throw-exceptions false})))

(defn signup-client [base-url email password]
  (post-json base-url "/clients/signup" {"email" email "password" password}))

(defn login-client [base-url email password]
  (post-json base-url "/clients/login" {"email" email "password" password}))

(defn change-client-password [base-url auth-key old-password new-password]
  (post-json base-url "/clients/password" {"auth_key" auth-key
                                           "old_password" old-password
                                           "new_password" new-password}))

(defn delete-client [base-url auth-key password]
  (request-json :delete base-url "/clients" {"auth_key" auth-key
                                             "password" password}))

(defn create-app [base-url auth-key app-name]
  (post-json base-url "/apps" {"auth_key" auth-key "app_name" app-name}))

(defn list-apps [base-url auth-key]
  (get-json base-url "/apps" {"auth_key" auth-key}))

(defn delete-app [base-url client-auth-key app-auth-key]
  (request-json :delete base-url "/apps" {"client_auth_key" client-auth-key
                                          "app_auth_key" app-auth-key}))

(defn invite-to-app [base-url inviter-auth-key app-auth-key invitee-email invitee-role]
  (post-json base-url "/apps/invite" {"inviter_auth_key" inviter-auth-key
                                      "app_auth_key" app-auth-key
                                      "invitee_email" invitee-email
                                      "invitee_role" invitee-role}))

(defn revoke-from-app [base-url revoker-auth-key app-auth-key revokee-email]
  (post-json base-url "/apps/revoke" {"revoker_auth_key" revoker-auth-key
                                      "app_auth_key" app-auth-key
                                      "revokee_email" revokee-email}))

(defn signup-user [base-url app-auth-key email password]
  (post-json base-url "/users/signup" {"app_auth_key" app-auth-key
                                       "email" email
                                       "password" password}))

(defn login-user [base-url app-auth-key email password]
  (post-json base-url "/users/login" {"app_auth_key" app-auth-key
                                      "email" email
                                      "password" password}))

(defn delete-user [base-url user-auth-key password]
  (request-json :delete base-url "/users" {"user_auth_key" user-auth-key
                                           "password" password}))

(defn change-user-password [base-url user-auth-key old-password new-password]
  (post-json base-url "/users/password" {"user_auth_key" user-auth-key
                                         "old_password" old-password
                                         "new_password" new-password}))

(defn list-app-users [base-url client-auth-key app-auth-key]
  (get-json base-url "/apps/users" {"client_auth_key" client-auth-key
                                    "app_auth_key" app-auth-key}))

(defn upload-user-file [base-url user-auth-key filename contents]
  (post-raw-headers base-url "/users/files"
                    {"x-user-auth-key" user-auth-key "x-filename" filename}
                    contents))

(defn download-user-file [base-url user-auth-key filename]
  (get-raw-headers base-url "/users/files"
                   {"x-user-auth-key" user-auth-key "x-filename" filename}))

(defn list-user-files [base-url user-auth-key]
  (get-json-headers base-url "/users/files/list" {"x-user-auth-key" user-auth-key}))

(defn delete-user-file [base-url user-auth-key filename]
  (delete-json-headers base-url "/users/files"
                       {"x-user-auth-key" user-auth-key "x-filename" filename}))

(defn upload-app-file [base-url client-auth-key app-auth-key filename contents]
  (post-raw-headers base-url "/apps/files"
                    {"x-client-auth-key" client-auth-key
                     "x-app-auth-key" app-auth-key
                     "x-filename" filename}
                    contents))

(defn download-app-file [base-url client-auth-key app-auth-key filename]
  (get-raw-headers base-url "/apps/files"
                   {"x-client-auth-key" client-auth-key
                    "x-app-auth-key" app-auth-key
                    "x-filename" filename}))

(defn list-app-files [base-url client-auth-key app-auth-key]
  (get-json base-url "/apps/files/list" {"client_auth_key" client-auth-key
                                         "app_auth_key" app-auth-key}))

(defn delete-app-file [base-url client-auth-key app-auth-key filename]
  (delete-json-headers base-url "/apps/files"
                       {"x-client-auth-key" client-auth-key
                        "x-app-auth-key" app-auth-key
                        "x-filename" filename}))

(defn list-app-managers [base-url client-auth-key app-auth-key]
  (get-json base-url "/apps/clients" {"client_auth_key" client-auth-key
                                      "app_auth_key" app-auth-key}))

(defn revoke-app-manager [base-url client-auth-key app-auth-key email-to-revoke]
  (post-json base-url "/apps/clients/revoke" {"client_auth_key" client-auth-key
                                              "app_auth_key" app-auth-key
                                              "email_to_revoke" email-to-revoke}))

(defn create-action [base-url client-auth-key app-auth-key action-name action-script]
  (post-json base-url "/actions" {"client_auth_key" client-auth-key
                                  "app_auth_key" app-auth-key
                                  "action_name" action-name
                                  "action_script" action-script}))

(defn read-action [base-url client-auth-key app-auth-key action-name]
  (get-raw base-url "/actions" {"client_auth_key" client-auth-key
                                "app_auth_key" app-auth-key
                                "action_name" action-name}))

(defn list-actions [base-url client-auth-key app-auth-key]
  (get-json base-url "/actions/list" {"client_auth_key" client-auth-key
                                      "app_auth_key" app-auth-key}))

(defn update-action [base-url client-auth-key app-auth-key old-action-name new-action-name action-script]
  (request-json :patch base-url "/actions" {"client_auth_key" client-auth-key
                                            "app_auth_key" app-auth-key
                                            "old_action_name" old-action-name
                                            "new_action_name" new-action-name
                                            "action_script" action-script}))

(defn delete-action [base-url client-auth-key app-auth-key action-name]
  (request-json :delete base-url "/actions" {"client_auth_key" client-auth-key
                                             "app_auth_key" app-auth-key
                                             "action_name" action-name}))

(defn list-all-clients [base-url client-auth-key]
  (get-json-headers base-url "/clients/all" {"x-client-auth-key" client-auth-key}))

(defn list-all-apps [base-url client-auth-key]
  (get-json-headers base-url "/apps/all" {"x-client-auth-key" client-auth-key}))

(defn list-all-files [base-url client-auth-key]
  (get-json-headers base-url "/files/all" {"x-client-auth-key" client-auth-key}))

(defn list-all-admins [base-url client-auth-key]
  (get-json-headers base-url "/admins/all" {"x-client-auth-key" client-auth-key}))

(defn promote-to-admin [base-url auth-key email]
  (post-json base-url "/admins" {"auth_key" auth-key "email" email}))

(defn demote-admin [base-url auth-key email]
  (request-json :delete base-url "/admins" {"auth_key" auth-key "email" email}))

(defn check-is-admin [base-url client-auth-key]
  (get-json-headers base-url "/admins/check" {"x-client-auth-key" client-auth-key}))

;; ##############
;; # DB HELPERS #
;; ##############

(defn- db-exec [sql-vec]
  (with-open [conn (jdbc/get-connection db/ds)]
    (jdbc/execute! conn ["PRAGMA foreign_keys = ON;"])
    (jdbc/execute! conn sql-vec {:builder-fn rs/as-unqualified-lower-maps})))

(defn db-count-clients []
  (-> (db-exec ["SELECT COUNT(*) AS count FROM clients"]) first :count))

(defn db-get-client-by-email [email]
  (first (db-exec ["SELECT * FROM clients WHERE email=?" email])))

(defn db-count-apps []
  (-> (db-exec ["SELECT COUNT(*) AS count FROM apps"]) first :count))

(defn db-get-app-by-auth-key [app-auth-key]
  (let [app-id (-> app-auth-key utils/decode-secret :app_id)]
    (first (db-exec ["SELECT * FROM apps WHERE id=?" app-id]))))

(defn db-get-app-by-name-owner [app-name owner-email]
  (first (db-exec ["SELECT apps.* FROM apps
                    JOIN clients ON apps.owner_id=clients.id
                    WHERE apps.name=? AND clients.email=?"
                   app-name owner-email])))

(defn db-count-users [app-auth-key]
  (let [app-id (-> app-auth-key utils/decode-secret :app_id)]
    (-> (db-exec ["SELECT COUNT(*) AS count FROM users WHERE app_id=?" app-id]) first :count)))

(defn db-get-user-by-email-app [email app-auth-key]
  (let [app-id (-> app-auth-key utils/decode-secret :app_id)]
    (first (db-exec ["SELECT * FROM users WHERE email=? AND app_id=?" email app-id]))))

(defn db-count-files []
  (-> (db-exec ["SELECT COUNT(*) AS count FROM files"]) first :count))

(defn db-get-file-by-name-and-user [filename user-auth-key]
  (let [user-id (:user_id (utils/decode-secret user-auth-key))]
    (first (db-exec ["SELECT * FROM files WHERE filename=? AND owner_id=?" filename user-id]))))

(defn db-count-app-files []
  (-> (db-exec ["SELECT COUNT(*) AS count FROM files WHERE owner_id IS NULL"]) first :count))

(defn db-get-app-file-by-name-and-app [filename app-auth-key]
  (let [app-id (-> app-auth-key utils/decode-secret :app_id)]
    (first (db-exec ["SELECT * FROM files WHERE filename=? AND app_id=? AND owner_id IS NULL"
                     filename app-id]))))

(defn db-count-invites [app-auth-key]
  (let [app-id (-> app-auth-key utils/decode-secret :app_id)]
    (-> (db-exec ["SELECT COUNT(*) AS count FROM app_memberships WHERE app_id=?" app-id]) first :count)))

(defn db-has-role-for-client [client-email app-auth-key role]
  (let [app-id (-> app-auth-key utils/decode-secret :app_id)]
    (= role (:role (first (db-exec ["SELECT role FROM app_memberships
                                     JOIN clients ON app_memberships.client_id=clients.id
                                     WHERE clients.email=? AND app_memberships.app_id=?"
                                    client-email app-id]))))))

(defn db-is-admin [email]
  (= "on" (:is_admin (first (db-exec ["SELECT is_admin FROM clients WHERE email=?" email])))))

(defn db-count-actions [app-auth-key]
  (let [app-id (-> app-auth-key utils/decode-secret :app_id)]
    (-> (db-exec ["SELECT COUNT(*) AS count FROM actions WHERE app_id=?" app-id]) first :count)))

(defn db-get-action-by-name-app [action-name app-auth-key]
  (let [app-id (-> app-auth-key utils/decode-secret :app_id)]
    (first (db-exec ["SELECT * FROM actions WHERE name=? AND app_id=?" action-name app-id]))))

(defn db-set-client-admin [email is-admin-flag]
  (db-exec ["UPDATE clients SET is_admin=? WHERE email=?" (if is-admin-flag "on" "off") email]))

(defn database-fixture [f]
  (do
    (db/setup-database)
    (db/run-migrations)
    (try
      (f)
      (catch Exception e
        (throw e))
      (finally
        (db/drop-database)))))

