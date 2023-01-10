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

(defn- insert-app-xf [state]
  (if (some? (:error state))
    state
    (try
      (let [client-id (:client-id state)
            app-name (:app-name state)
            result (db/run-operation-first "create-app.sql"
                                     {"owner_id" client-id
                                      "app_name" app-name})
            app-id (:id result)
            next-state (assoc state :app-id app-id)]
        next-state)
      (catch org.postgresql.util.PSQLException e
        {:error e}))))

(defn- invite-to-app-xf [state]
  (if (some? (:error state))
    state
    (let [app-id (:app-id state)
          owner-id (:client-id state)
          role (:role state)
          result (db/run-operation-first "invite-to-app.sql"
                                   {"app_id" app-id
                                    "client_id" owner-id
                                    "role" role})]
      state)))

(defn- format-new-app-output-xf [state]
  {"error" (:error state)
   "auth_key" (when (some? (:app-id state))
                (new-app-auth-key (:app-id state)))})

(defn new-app [client-auth-key app-name]
  (let [client-info (utils/decode-secret client-auth-key)
        client-id (:client_id client-info)]
    (->> {:error nil 
          :client-id client-id
          :app-name app-name
          :role "admin"}
         insert-app-xf
         invite-to-app-xf
         format-new-app-output-xf)))

(defn get-clients-apps [client-auth-key]
  (let [client-info (utils/decode-secret client-auth-key)
        client-id (:client_id client-info)
        result (db/run-operation "get-clients-apps.sql"
                                      {"id" client-id})]
    {"apps" result
     "error" nil}))

(defn- get-client-role-in-app-xf [state]
  (cond 
    (some? (:error state))
      state
    (nil? (:client-id state))
      {:error "Invalid client"}
    :else
      (let [client-id (:client-id state)
            app-id (:app-id state)
            result (db/run-operation-first "get-client-app-role.sql"
                                           {"client_id" client-id
                                            "app_id" app-id})]
        (assoc state :role (:role result)))))

(defn- maybe-delete-app-xf [state]
  (cond (some? (:error state))
          state
        (not= (:role state) "admin")
          (assoc state :error "User not allowed to delete app")
        :else
          (let [app-id (:app-id state)
                result (db/run-operation-first "delete-app.sql"
                                               {"app_id" app-id})]
            state)))

(defn format-delete-app-output-xf [state]
  {"error" (get state :error nil)})

(defn delete-app [client-auth-key app-auth-key]
  (let [client-info (utils/decode-secret client-auth-key)
        client-id (:client_id client-info)
        app-info (utils/decode-secret app-auth-key)
        app-id (:app_id app-info)]
    (->> {:error nil
          :client-id client-id
          :app-id app-id}
         get-client-role-in-app-xf
         maybe-delete-app-xf
         format-delete-app-output-xf)))

(defn- validate-inviter-role-xf [state]
  (if (not= "admin" (:role state))
    {:error "Inviter has no rights to do this"}
    state))

(defn- get-invitee-client-id-by-email-xf [state]
  (if (some? (:error state))
    state
    (let [result (db/run-operation-first "get-client-by-email.sql"
                                         {"email" (:invitee-email state)})
          client-id (get result :id nil)]
      (if (nil? client-id)
        {:error "Invitee email not found"}
        (assoc state :client-id client-id
                     :role (:invitee-role state))))))

(defn- format-invite-to-app-output-xf [state]
  {"error" (:error state)})

(defn invite-to-app-by-email 
  [inviter-auth-key app-auth-key invitee-email invitee-role]
  (let [inviter-info (utils/decode-secret inviter-auth-key)
        inviter-id (:client_id inviter-info)
        app-info (utils/decode-secret app-auth-key)
        app-id (:app_id app-info)]
    (->> {:error nil
          :client-id inviter-id
          :app-id app-id
          :invitee-email invitee-email
          :invitee-role invitee-role}
         get-client-role-in-app-xf
         validate-inviter-role-xf
         get-invitee-client-id-by-email-xf
         invite-to-app-xf
         format-invite-to-app-output-xf)))
