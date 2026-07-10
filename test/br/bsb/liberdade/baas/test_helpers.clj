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

(def server-thread-ref (atom nil))
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
      (reset! server-thread-ref
              (doto (Thread. #(server/run-server
                                (wrap-cors #'api/app-routes #".*" {:security nil})
                                {:port port}))
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

;; `integration-fixture` is registered with `use-fixtures`, which invokes it
;; as `(fixture-fn test-thunk)` and calls `test-thunk` with zero arguments --
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

(defn- json-response [response]
  (:body response))

;; Missing files/actions are served by routes that return `nil`/empty body,
;; which compojure turns into a bodiless 404 rather than a JSON error payload.
(defn- raw-response-or-nil [response]
  (let [body (:body response)]
    (if (or (= 404 (:status response)) (nil? body) (= "" body))
      nil
      body)))

(defn signup-client [base-url email password]
  (json-response (http/post (str base-url "/clients/signup")
                            {:body (json/write-str {"email" email
                                                    "password" password})
                             :as :json
                             :throw-exceptions false})))

(defn login-client [base-url email password]
  (json-response (http/post (str base-url "/clients/login")
                            {:body (json/write-str {"email" email
                                                    "password" password})
                             :as :json
                             :throw-exceptions false})))

(defn change-client-password [base-url auth-key old-password new-password]
  (json-response (http/post (str base-url "/clients/password")
                            {:body (json/write-str {"auth_key" auth-key
                                                    "old_password" old-password
                                                    "new_password" new-password})
                             :as :json
                             :throw-exceptions false})))

(defn delete-client [base-url auth-key password]
  (json-response (http/request {:method :delete
                                 :url (str base-url "/clients")
                                 :body (json/write-str {"auth_key" auth-key
                                                        "password" password})
                                 :as :json
                                 :throw-exceptions false})))

(defn create-app [base-url auth-key app-name]
  (json-response (http/post (str base-url "/apps")
                            {:body (json/write-str {"auth_key" auth-key
                                                    "app_name" app-name})
                             :as :json
                             :throw-exceptions false})))

(defn list-apps [base-url auth-key]
  (json-response (http/get (str base-url "/apps")
                           {:query-params {"auth_key" auth-key}
                            :as :json
                            :throw-exceptions false})))

(defn delete-app [base-url client-auth-key app-auth-key]
  (json-response (http/request {:method :delete
                                 :url (str base-url "/apps")
                                 :body (json/write-str {"client_auth_key" client-auth-key
                                                        "app_auth_key" app-auth-key})
                                 :as :json
                                 :throw-exceptions false})))

(defn invite-to-app [base-url inviter-auth-key app-auth-key invitee-email invitee-role]
  (json-response (http/post (str base-url "/apps/invite")
                            {:body (json/write-str {"inviter_auth_key" inviter-auth-key
                                                    "app_auth_key" app-auth-key
                                                    "invitee_email" invitee-email
                                                    "invitee_role" invitee-role})
                             :as :json
                             :throw-exceptions false})))

(defn revoke-from-app [base-url revoker-auth-key app-auth-key revokee-email]
  (json-response (http/post (str base-url "/apps/revoke")
                            {:body (json/write-str {"revoker_auth_key" revoker-auth-key
                                                    "app_auth_key" app-auth-key
                                                    "revokee_email" revokee-email})
                             :as :json
                             :throw-exceptions false})))

(defn signup-user [base-url app-auth-key email password]
  (json-response (http/post (str base-url "/users/signup")
                            {:body (json/write-str {"app_auth_key" app-auth-key
                                                    "email" email
                                                    "password" password})
                             :as :json
                             :throw-exceptions false})))

(defn login-user [base-url app-auth-key email password]
  (json-response (http/post (str base-url "/users/login")
                            {:body (json/write-str {"app_auth_key" app-auth-key
                                                    "email" email
                                                    "password" password})
                             :as :json
                             :throw-exceptions false})))

(defn delete-user [base-url user-auth-key password]
  (json-response (http/request {:method :delete
                                 :url (str base-url "/users")
                                 :body (json/write-str {"user_auth_key" user-auth-key
                                                        "password" password})
                                 :as :json
                                 :throw-exceptions false})))

(defn change-user-password [base-url user-auth-key old-password new-password]
  (json-response (http/post (str base-url "/users/password")
                            {:body (json/write-str {"user_auth_key" user-auth-key
                                                    "old_password" old-password
                                                    "new_password" new-password})
                             :as :json
                             :throw-exceptions false})))

(defn list-app-users [base-url client-auth-key app-auth-key]
  (json-response (http/get (str base-url "/apps/users")
                           {:query-params {"client_auth_key" client-auth-key
                                           "app_auth_key" app-auth-key}
                            :as :json
                            :throw-exceptions false})))

(defn upload-user-file [base-url user-auth-key filename contents]
  (json-response (http/post (str base-url "/users/files")
                            {:headers {"x-user-auth-key" user-auth-key
                                       "x-filename" filename}
                             :body contents
                             :as :json
                             :throw-exceptions false})))

(defn download-user-file [base-url user-auth-key filename]
  (raw-response-or-nil (http/get (str base-url "/users/files")
                                 {:headers {"x-user-auth-key" user-auth-key
                                            "x-filename" filename}
                                  :throw-exceptions false})))

(defn list-user-files [base-url user-auth-key]
  (json-response (http/get (str base-url "/users/files/list")
                           {:headers {"x-user-auth-key" user-auth-key}
                            :as :json
                            :throw-exceptions false})))

(defn delete-user-file [base-url user-auth-key filename]
  (json-response (http/request {:method :delete
                                 :url (str base-url "/users/files")
                                 :headers {"x-user-auth-key" user-auth-key
                                           "x-filename" filename}
                                 :as :json
                                 :throw-exceptions false})))

(defn upload-app-file [base-url client-auth-key app-auth-key filename contents]
  (json-response (http/post (str base-url "/apps/files")
                            {:headers {"x-client-auth-key" client-auth-key
                                       "x-app-auth-key" app-auth-key
                                       "x-filename" filename}
                             :body contents
                             :as :json
                             :throw-exceptions false})))

(defn download-app-file [base-url client-auth-key app-auth-key filename]
  (raw-response-or-nil (http/get (str base-url "/apps/files")
                                 {:headers {"x-client-auth-key" client-auth-key
                                            "x-app-auth-key" app-auth-key
                                            "x-filename" filename}
                                  :throw-exceptions false})))

(defn list-app-files [base-url client-auth-key app-auth-key]
  (json-response (http/get (str base-url "/apps/files/list")
                           {:query-params {"client_auth_key" client-auth-key
                                           "app_auth_key" app-auth-key}
                            :as :json
                            :throw-exceptions false})))

(defn delete-app-file [base-url client-auth-key app-auth-key filename]
  (json-response (http/request {:method :delete
                                 :url (str base-url "/apps/files")
                                 :headers {"x-client-auth-key" client-auth-key
                                           "x-app-auth-key" app-auth-key
                                           "x-filename" filename}
                                 :as :json
                                 :throw-exceptions false})))

(defn list-app-managers [base-url client-auth-key app-auth-key]
  (json-response (http/get (str base-url "/apps/clients")
                           {:query-params {"client_auth_key" client-auth-key
                                           "app_auth_key" app-auth-key}
                            :as :json
                            :throw-exceptions false})))

(defn revoke-app-manager [base-url client-auth-key app-auth-key email-to-revoke]
  (json-response (http/post (str base-url "/apps/clients/revoke")
                            {:body (json/write-str {"client_auth_key" client-auth-key
                                                    "app_auth_key" app-auth-key
                                                    "email_to_revoke" email-to-revoke})
                             :as :json
                             :throw-exceptions false})))

(defn create-action [base-url client-auth-key app-auth-key action-name action-script]
  (json-response (http/post (str base-url "/actions")
                            {:body (json/write-str {"client_auth_key" client-auth-key
                                                    "app_auth_key" app-auth-key
                                                    "action_name" action-name
                                                    "action_script" action-script})
                             :as :json
                             :throw-exceptions false})))

(defn read-action [base-url client-auth-key app-auth-key action-name]
  (raw-response-or-nil (http/get (str base-url "/actions")
                                 {:query-params {"client_auth_key" client-auth-key
                                                 "app_auth_key" app-auth-key
                                                 "action_name" action-name}
                                  :throw-exceptions false})))

(defn list-actions [base-url client-auth-key app-auth-key]
  (json-response (http/get (str base-url "/actions/list")
                           {:query-params {"client_auth_key" client-auth-key
                                           "app_auth_key" app-auth-key}
                            :as :json
                            :throw-exceptions false})))

(defn update-action [base-url client-auth-key app-auth-key old-action-name new-action-name action-script]
  (json-response (http/request {:method :patch
                                 :url (str base-url "/actions")
                                 :body (json/write-str {"client_auth_key" client-auth-key
                                                        "app_auth_key" app-auth-key
                                                        "old_action_name" old-action-name
                                                        "new_action_name" new-action-name
                                                        "action_script" action-script})
                                 :as :json
                                 :throw-exceptions false})))

(defn delete-action [base-url client-auth-key app-auth-key action-name]
  (json-response (http/request {:method :delete
                                 :url (str base-url "/actions")
                                 :body (json/write-str {"client_auth_key" client-auth-key
                                                        "app_auth_key" app-auth-key
                                                        "action_name" action-name})
                                 :as :json
                                 :throw-exceptions false})))

(defn list-all-clients [base-url client-auth-key]
  (json-response (http/get (str base-url "/clients/all")
                           {:headers {"x-client-auth-key" client-auth-key}
                            :as :json
                            :throw-exceptions false})))

(defn list-all-apps [base-url client-auth-key]
  (json-response (http/get (str base-url "/apps/all")
                           {:headers {"x-client-auth-key" client-auth-key}
                            :as :json
                            :throw-exceptions false})))

(defn list-all-files [base-url client-auth-key]
  (json-response (http/get (str base-url "/files/all")
                           {:headers {"x-client-auth-key" client-auth-key}
                            :as :json
                            :throw-exceptions false})))

(defn list-all-admins [base-url client-auth-key]
  (json-response (http/get (str base-url "/admins/all")
                           {:headers {"x-client-auth-key" client-auth-key}
                            :as :json
                            :throw-exceptions false})))

(defn promote-to-admin [base-url auth-key email]
  (json-response (http/post (str base-url "/admins")
                            {:body (json/write-str {"auth_key" auth-key
                                                    "email" email})
                             :as :json
                             :throw-exceptions false})))

(defn demote-admin [base-url auth-key email]
  (json-response (http/request {:method :delete
                                 :url (str base-url "/admins")
                                 :body (json/write-str {"auth_key" auth-key
                                                        "email" email})
                                 :as :json
                                 :throw-exceptions false})))

(defn check-is-admin [base-url client-auth-key]
    (json-response (http/get (str base-url "/admins/check")
                             {:headers {"x-client-auth-key" client-auth-key}
                              :as :json
                              :throw-exceptions false})))

;; ##############
;; # DB HELPERS #
;; ##############

(defn- db-exec [query]
   (with-open [conn (jdbc/get-connection db/ds)]
     (jdbc/execute! conn ["PRAGMA foreign_keys = ON;"])
     (jdbc/execute! conn [query] {:builder-fn rs/as-unqualified-lower-maps})))

(defn db-count-clients []
   (-> "SELECT COUNT(*) AS count FROM clients" db-exec first :count))

(defn db-get-client-by-email [email]
   (first (db-exec (str "SELECT * FROM clients WHERE email='" email "'"))))

(defn db-count-apps []
   (-> "SELECT COUNT(*) AS count FROM apps" db-exec first :count))

(defn db-get-app-by-auth-key [app-auth-key]
   (let [app-id (-> app-auth-key utils/decode-secret :app_id)]
     (first (db-exec (str "SELECT * FROM apps WHERE id=" app-id)))))

(defn db-get-app-by-name-owner [app-name owner-email]
   (first (db-exec (str "SELECT apps.* FROM apps JOIN clients ON apps.owner_id=clients.id WHERE apps.name='" app-name "' AND clients.email='" owner-email "'"))))

(defn db-count-users [app-auth-key]
   (let [app-id (-> app-auth-key utils/decode-secret :app_id)]
     (-> (str "SELECT COUNT(*) AS count FROM users WHERE app_id=" app-id) db-exec first :count)))

(defn db-get-user-by-email-app [email app-auth-key]
   (let [app-id (-> app-auth-key utils/decode-secret :app_id)]
     (first (db-exec (str "SELECT * FROM users WHERE email='" email "' AND app_id=" app-id)))))

(defn db-count-files []
   (-> "SELECT COUNT(*) AS count FROM files" db-exec first :count))

(defn db-get-file-by-name-and-user [filename user-auth-key]
   (let [user-info (utils/decode-secret user-auth-key)
         user-id (:user_id user-info)]
     (first (db-exec (str "SELECT * FROM files WHERE filename='" filename "' AND owner_id=" user-id)))))

(defn db-count-app-files []
   (-> "SELECT COUNT(*) AS count FROM files WHERE owner_id IS NULL" db-exec first :count))

(defn db-get-app-file-by-name-and-app [filename app-auth-key]
   (let [app-id (-> app-auth-key utils/decode-secret :app_id)]
     (first (db-exec (str "SELECT * FROM files WHERE filename='" filename "' AND app_id=" app-id " AND owner_id IS NULL")))))

(defn db-count-invites [app-auth-key]
   (let [app-id (-> app-auth-key utils/decode-secret :app_id)]
     (-> (str "SELECT COUNT(*) AS count FROM app_memberships WHERE app_id=" app-id) db-exec first :count)))

(defn db-has-role-for-client [client-email app-auth-key role]
   (let [app-id (-> app-auth-key utils/decode-secret :app_id)]
     (= role (:role (first (db-exec (str "SELECT role FROM app_memberships JOIN clients ON app_memberships.client_id=clients.id WHERE clients.email='" client-email "' AND app_memberships.app_id=" app-id)))))))

(defn db-is-admin [email]
   (= "on" (:is_admin (first (db-exec (str "SELECT is_admin FROM clients WHERE email='" email "'"))))))

(defn db-count-actions [app-auth-key]
   (let [app-id (-> app-auth-key utils/decode-secret :app_id)]
     (-> (str "SELECT COUNT(*) AS count FROM actions WHERE app_id=" app-id) db-exec first :count)))

(defn db-get-action-by-name-app [action-name app-auth-key]
   (let [app-id (-> app-auth-key utils/decode-secret :app_id)]
     (first (db-exec (str "SELECT * FROM actions WHERE name='" action-name "' AND app_id=" app-id)))))

(defn db-set-client-admin [email is-admin-flag]
   (db-exec (str "UPDATE clients SET is_admin='" (if is-admin-flag "on" "off") "' WHERE email='" email "'")))
