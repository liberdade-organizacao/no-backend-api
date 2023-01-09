(ns br.bsb.liberdade.baas.controllers
  (:require [br.bsb.liberdade.baas.utils :as utils]
            [br.bsb.liberdade.baas.db :as db]))

(def possible-app-roles ["admin" "contributor"])

(defn- pqbool [b]
  (if b "on" "off"))

(defn new-auth-key [email is-admin]
  (utils/encode-secret {"email" email
                        "is_admin" is-admin}))

(defn new-client [email password is-admin]
  (try
    (let [params {"email" email
                  "password" (utils/hide password)
                  "is_admin" (pqbool is-admin)
                  "auth_key" (new-auth-key email is-admin)}
          result (db/run-operation-first  "create-client-account.sql" params)]
      {"auth_key" (get result :auth_key)
       "error" nil})
    (catch org.postgresql.util.PSQLException e 
      {"auth_key" nil
       "error" "Email already exists"})))

(defn auth-client [email password]
  (let [params {"email" email
                "password" (utils/hide password)}
        result (db/run-operation-first "auth-client.sql" params)
        auth-key (get result :auth_key nil)
        error (when (nil? auth-key)
                "Wrong email or password")]
    {"auth_key" auth-key
     "error" error}))

(defn- insert-app-xf [state params]
  (if (some? (:error state))
    [state params]
    (try
      (let [client-email (:client-email params)
            app-name (:app-name params)
            app-auth-key (utils/encode-secret {"owner_email" client-email
                                               "app_name" app-name})
            result (db/run-operation-first "create-app.sql"
                                     {"owner_client_email" client-email
                                      "app_name" app-name
                                      "auth_key" app-auth-key})
            next-state (assoc result :error nil)
            next-params (assoc params :app-auth-key app-auth-key)]
        [next-state next-params])
      (catch org.postgresql.util.PSQLException e
        [{:error e}
         params]))))

(defn- invite-to-app-xf [state params]
  (if (some? (:error state))
    [state params]
    (let [app-id (:id state)
          owner-id (:owner_id state)
          role "admin"
          result (db/run-operation-first "invite-to-app.sql"
                                   {"app_id" app-id
                                    "client_id" owner-id
                                    "role" role})
          next-state (assoc result :error nil)
          next-params params]
      [next-state next-params])))

(defn- format-new-app-output-xf [state params]
  {"error" (:error state)
   "auth_key" (get params :app-auth-key nil)})

(defn new-app [client-auth-key app-name]
  (let [client-info (utils/decode-secret client-auth-key)
        email (:email client-info)]
    (->> [{:error nil} 
          {:client-email email
           :app-name app-name}]
         (apply insert-app-xf)
         (apply invite-to-app-xf)
         (apply format-new-app-output-xf))))

(defn get-clients-apps [client-auth-key]
  (let [client-info (utils/decode-secret client-auth-key)
        email (:email client-info)
        result (db/run-operation "get-clients-apps.sql"
                                      {"email" email})]
    {"apps" result
     "error" nil}))

(defn- get-client-role-in-app-xf [state params]
  (cond (some? (:error state)) 
          [state params]
        (= (:owner-email params) (:client-email params)) 
          [state (assoc params :role "admin")]
        :else
          (let [owner-email (:owner-email params)
                client-email (:client-email params)
                app-name (:app-name params)
                result (db/run-operation-first "get-client-app-role.sql"
                                         {"owner_email" owner-email
                                          "client_email" client-email
                                          "app_name" app-name})]
            [state (assoc params :role (:role result))])))

(defn- maybe-delete-app-xf [state params]
  (cond (some? (:error state))
          [state params]
        (not= (:role params) "admin")
          [(assoc state :error "User not allowed to delete app")
           params]
        :else
          (let [owner-email (:owner-email params)
                app-name (:app-name params)
                result (db/run-operation-first "delete-app.sql"
                                         {"client_email" owner-email
                                          "app_name" app-name})]
            [state params])))

(defn format-delete-app-output-xf [state params]
  {"error" (get state :error nil)})

(defn delete-app [client-auth-key app-auth-key]
  (let [client-info (utils/decode-secret client-auth-key)
        client-email (:email client-info)
        app-info (utils/decode-secret app-auth-key)
        owner-email (:owner_email app-info)
        app-name (:app_name app-info)]
    (->> [{:error nil}
          {:client-email client-email
           :owner-email owner-email
           :app-name app-name}]
         (apply get-client-role-in-app-xf)
         (apply maybe-delete-app-xf)
         (apply format-delete-app-output-xf))))

