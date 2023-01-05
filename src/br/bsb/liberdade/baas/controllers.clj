(ns br.bsb.liberdade.baas.controllers
  (:require [br.bsb.liberdade.baas.utils :as utils]
            [br.bsb.liberdade.baas.db :as db]))

(defn- pqbool [b]
  (if b "on" "off"))

(defn new-auth-key [email is-admin]
  (utils/encode-secret {"email" email
                        "is_admin" is-admin}))

(defn new-client [email password is-admin]
  (let [params {"email" email
                "password" (utils/hide password)
                "is_admin" (pqbool is-admin)
                "auth_key" (new-auth-key email is-admin)}
        result (db/run-operation "create-client-account.sql" params)
        auth-key (get result :auth_key nil)
        error (when (nil? auth-key)
                "Failed to create client")]
    {"auth_key" auth-key
     "error" error}))

(defn auth-client [email password]
  (let [params {"email" email
                "password" (utils/hide password)}
        result (db/run-operation "auth-client.sql" params)
        auth-key (get result :auth_key nil)
        error (when (nil? auth-key)
                "Wrong email or password")]
    {"auth_key" auth-key
     "error" error}))

