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
          result (db/run-operation "create-client-account.sql" params)]
      {"auth_key" (get result :auth_key)
       "error" nil})
    (catch org.postgresql.util.PSQLException e 
      {"auth_key" nil
       "error" "Email already exists"})))

(defn auth-client [email password]
  (let [params {"email" email
                "password" (utils/hide password)}
        result (db/run-operation "auth-client.sql" params)
        auth-key (get result :auth_key nil)
        error (when (nil? auth-key)
                "Wrong email or password")]
    {"auth_key" auth-key
     "error" error}))

(defn- insert-app-xf [state params]
  (if (some? (:error state))
    [state params]
    (let [client-email (:client-email params)
          app-name (:app-name params)
          app-auth-key (utils/encode-secret {"owner_email" client-email
                                             "app_name" app-name})
          result (db/run-operation "create-app.sql"
                                   {"owner_client_email" client-email
                                    "app_name" app-name
                                    "auth_key" app-auth-key})
          next-state (assoc result :error nil)
          next-params (assoc params :app-auth-key app-auth-key)]
      [next-state next-params])))

(defn- invite-to-app-xf [state params]
  (if (some? (:error state))
    [state params]
    (let [app-id (:id state)
          owner-id (:owner_id state)
          role "admin"
          result (db/run-operation "invite-to-app.sql"
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
  {"apps" nil
   "error" "not implemented yet!"})

(defn delete-app [client-auth-key app-auth-key]
  {"error" "not implemented yet!"})

