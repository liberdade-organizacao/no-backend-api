(ns br.bsb.liberdade.baas.business
  (:require [br.bsb.liberdade.baas.utils :as utils]
            [br.bsb.liberdade.baas.db :as db]))

(def possible-app-roles ["admin" "contributor"])

(defn- pqbool [b]
  (if b "on" "off"))

(defn new-client-auth-key [client-id is-admin]
  (utils/encode-secret {"client_id" client-id
                        "is_admin" is-admin}))

(defn new-app-auth-key [app-id]
  (utils/encode-secret {"app_id" app-id}))

(defn new-client [email password is-admin]
  (try
    (let [params {"email" email
                  "password" (utils/hide password)
                  "is_admin" (pqbool is-admin)}
          result (db/run-operation-first "create-client-account.sql" params)]
      {"auth_key" (new-client-auth-key (:id result) (:is_admin result))
       "error" nil})
    (catch org.postgresql.util.PSQLException e 
      {"auth_key" nil
       "error" "Email already exists"})))

(defn auth-client [email password]
  (let [params {"email" email
                "password" (utils/hide password)}
        result (db/run-operation-first "auth-client.sql" params)
        auth-key (when (some? result)
                  (new-client-auth-key (:id result) (:is_admin result)))
        error (when (nil? result)
                "Wrong email or password")]
    {"auth_key" auth-key
     "error" error}))

(defn- insert-app-xf [state params]
  (if (some? (:error state))
    [state params]
    (try
      (let [client-id (:client-id params)
            app-name (:app-name params)
            result (db/run-operation-first "create-app.sql"
                                     {"owner_id" client-id
                                      "app_name" app-name})
            app-id (:id result)
            next-state (assoc result :app-id app-id)
            next-params params]
        [next-state next-params])
      (catch org.postgresql.util.PSQLException e
        [{:error e}
         params]))))

(defn- invite-to-app-xf [state params]
  (if (some? (:error state))
    [state params]
    (let [app-id (:app-id state)
          owner-id (:client-id params)
          role "admin"
          result (db/run-operation-first "invite-to-app.sql"
                                   {"app_id" app-id
                                    "client_id" owner-id
                                    "role" role})
          next-state state
          next-params params]
      [next-state next-params])))

(defn- format-new-app-output-xf [state params]
  {"error" (:error state)
   "auth_key" (when (some? (:app-id state))
                (new-app-auth-key (:app-id state)))})

(defn new-app [client-auth-key app-name]
  (let [client-info (utils/decode-secret client-auth-key)
        client-id (:client_id client-info)]
    (->> [{:error nil} 
          {:client-id client-id
           :app-name app-name}]
         (apply insert-app-xf)
         (apply invite-to-app-xf)
         (apply format-new-app-output-xf))))

(defn get-clients-apps [client-auth-key]
  (let [client-info (utils/decode-secret client-auth-key)
        client-id (:client_id client-info)
        result (db/run-operation "get-clients-apps.sql"
                                      {"id" client-id})]
    {"apps" result
     "error" nil}))

(defn- get-client-role-in-app-xf [state params]
  (cond 
    (some? (:error state))
      [state params]
    (nil? (:client-id params))
      [{:error "Invalid client"}
       params]
    :else
      (let [client-id (:client-id params)
            app-id (:app-id params)
            result (db/run-operation-first "get-client-app-role.sql"
                                           {"client_id" client-id
                                            "app_id" app-id})]
        [state (assoc params :role (:role result))])))

(defn- maybe-delete-app-xf [state params]
  (cond (some? (:error state))
          [state params]
        (not= (:role params) "admin")
          [(assoc state :error "User not allowed to delete app")
           params]
        :else
          (let [app-id (:app-id params)
                result (db/run-operation-first "delete-app.sql"
                                               {"app_id" app-id})]
            [state params])))

(defn format-delete-app-output-xf [state params]
  {"error" (get state :error nil)})

(defn delete-app [client-auth-key app-auth-key]
  (let [client-info (utils/decode-secret client-auth-key)
        client-id (:client_id client-info)
        app-info (utils/decode-secret app-auth-key)
        app-id (:app_id app-info)]
    (->> [{:error nil}
          {:client-id client-id
           :app-id app-id}]
         (apply get-client-role-in-app-xf)
         (apply maybe-delete-app-xf)
         (apply format-delete-app-output-xf))))

