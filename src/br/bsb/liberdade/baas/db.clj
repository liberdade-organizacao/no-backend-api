(ns br.bsb.liberdade.baas.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [clojure.java.io :as io]
            [br.bsb.liberdade.strint :as strint]
            [br.bsb.liberdade.baas.utils :as utils]))

(defn- read-resource [resource]
  (with-open [reader (io/reader (str "resources/db/sql/" resource))]
    (slurp reader)))

(defn- load-sql [fns]
  (reduce (fn [box [key value]]
            (assoc box key (read-resource value)))
          {}
          fns))

(def sql-files
  {:auth-user "auth_user.sql"
   :create-tables "create_tables.sql"
   :create-user "create_user.sql"
   :export-users "export_users.sql"
   :import-user "import_user.sql"
   :update-password "update_password.sql"
   :get-notes "get_notes.sql"
   :update-notes "update_notes.sql"
   :drop-database "drop_database.sql"})
(def sql (load-sql sql-files))
(def db (or (System/getenv "JDBC_DATABASE_URL")
            "jdbc:postgresql://localhost:5433/fpcl?user=fpcl&password=password"))
(def ds (jdbc/get-datasource db))
(Class/forName "org.postgresql.Driver")  ; required to get the driver working properly

(defn drop-database []
  (jdbc/execute! ds [(get sql :drop-database)]))

(defn setup-database []
  (jdbc/execute! ds [(get sql :create-tables)]))

(defn- user-id-to-auth [user-id]
  (if (nil? user-id)
    user-id
    (utils/encode-secret {:user-id user-id})))

(defn- get-notes-from-query [query-result]
  (let [encoded-notes (-> query-result first (get :notes))]
    (if (nil? encoded-notes)
      encoded-notes
      (-> (utils/decode-secret encoded-notes) (get :notes)))))

(defn- execute-query [query]
  (jdbc/execute! ds [query] {:builder-fn rs/as-unqualified-lower-maps}))

(defn- add-user [username password notes admin-status]
  (let [params {"admin" admin-status
                "notes" (utils/encode-secret {:notes (or notes "")})
                "password" (utils/hide password)
                "username" username}
        query (strint/strint (get sql :create-user) params)
        result (execute-query query)
        user-id (-> result first (get :id))]
    {"auth_key" (user-id-to-auth user-id)}))

(defn create-user [username password notes]
  (add-user username password notes "off"))

(defn create-admin [username password notes]
  (add-user username password notes "on"))

(defn auth-user [username password]
  (let [params {"password" (utils/hide password)
                "username" username}
        query (strint/strint (get sql :auth-user) params)
        result (execute-query query)
        user-id (-> result first (get :id))
        notes (get-notes-from-query result)]
    {"auth_key" (user-id-to-auth user-id)
     :notes notes}))

(defn update-password [auth-key old-password new-password]
  (let [params {"id" (-> auth-key utils/decode-secret :user-id)
                "oldpassword" (utils/hide old-password)
                "newpassword" (utils/hide new-password)}
        query (strint/strint (get sql :update-password) params)
        result (jdbc/execute! ds [query] {:builder-fn rs/as-unqualified-lower-maps})]
    {:error (if (= 1 (count result))
                nil
                "Failed to change password")}))

(defn get-notes [auth]
  (let [user-id (-> auth utils/decode-secret :user-id)
        params {"id" user-id}
        query (strint/strint (get sql :get-notes) params)
        result (execute-query query)
        notes (get-notes-from-query result)]
    {:notes notes}))

(defn update-notes [auth notes]
  (let [params {"id" (-> auth utils/decode-secret :user-id)
                "notes" (utils/encode-secret {:notes (or notes "")})}
        query (strint/strint (get sql :update-notes) params)
        result (execute-query query)]
    {:error (if (> (count result) 0) nil "Invalid auth key")}))

(defn- format-database-for-export [database]
  (map #(update % :last_updated str)
       database))
(defn backup [auth]
  (let [user-id (-> auth utils/decode-secret :user-id)
        user-query (strint/strint (get sql :get-notes) {"id" user-id})
        is-admin-result (execute-query user-query)
        is-admin? (-> is-admin-result first (get :admin))]
    {:database (when is-admin?
                 (-> sql
                     (get :export-users)
                     execute-query
                     format-database-for-export))}))

(defn- add-raw-entry [state query]
  (let [result (execute-query query)]
    (if (= 0 (count result))
        (str "Failed to execute query: " query)
        state)))
(defn- build-add-raw-entry-query [entry]
  (strint/strint (get sql :import-user) 
                 {"id" (:id entry)
                  "username" (:username entry)
                  "password" (:password entry)
                  "admin" (if (:admin entry) "on" "off")
                  "notes" (:notes entry)
                  "last_updated" (:last_updated entry)}))
(defn- add-raw-entries [entries]
  (let [queries (map build-add-raw-entry-query entries)]
    (drop-database)
    (setup-database)
    (reduce add-raw-entry nil queries)))
(defn import-backup [auth backup]
  (let [user-id (-> auth utils/decode-secret :user-id)
        user-query (strint/strint (get sql :get-notes) {"id" user-id})
        is-admin-result (execute-query user-query)
        is-admin (-> is-admin-result first (get :admin))]
    {:error (if is-admin
                (add-raw-entries (:database backup))
                "Not a valid action")}))

