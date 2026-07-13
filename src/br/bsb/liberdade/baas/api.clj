(ns br.bsb.liberdade.baas.api
  (:gen-class)
  (:require [clojure.data.json :as json]
            [clojure.string :as string]
            [org.httpkit.server :as server]
            [compojure.core :refer :all]
            [jumblerg.middleware.cors :refer [wrap-cors]]
            [selmer.parser :refer :all]
            [msgpack.core :as msgpack]
            [br.bsb.liberdade.baas.db :as db]
            [br.bsb.liberdade.baas.business :as biz]
            [br.bsb.liberdade.baas.proxies :as proxies]
            [br.bsb.liberdade.baas.tar.decompress :as untar]
             [br.bsb.liberdade.baas.jobs :as jobs]
              [br.bsb.liberdade.baas.validation :as v]))

; #############
; # UTILITIES #
; #############
(defn boilerplate-in [req body]
  (if (= "application/vnd.msgpack" (-> req :headers (get "accept")))
    (msgpack/unpack body)
    (json/read-str body)))

(defn- boilerplate-out [req body]
  (let [msgpack? (= "application/vnd.msgpack"
                    (-> req :headers (get "accept")))]
    {:status (if (-> body (get :error) nil?) 200 400)
     :headers {"Content-Type" (if msgpack? 
                                "application/vnd.msgpack"
                                "text/json")
               "Access-Control-Allow-Origin" "*"
               "Access-Control-Expose-Headers" "*"}
     :body (if msgpack?
             (msgpack/pack body)
             (str (json/write-str body)))}))

(defn- url-search-params [raw]
  (->> (string/split raw #"&")
       (map #(string/split % #"="))
       (reduce (fn [state [key value]] (assoc state key value)) {})))

; ##########
; # ROUTES #
; ##########
(defn check-health [req]
  (boilerplate-out req 
               {"api" "ok"
                "db" (db/check-health)
                "scripting" (proxies/check-scripting-engine-health)
                "version" "0.3.1"}))

(defn clients-signup [req]
  (let [params (boilerplate-in req (slurp (:body req)))
        validated (v/validate {"email" (fn [v] (or (v/validate-presence v) (v/validate-email v)))
                               "password" (fn [v] (or (v/validate-presence v) (v/validate-string v)))}
                               params)]
    (if (contains? validated :error)
      (boilerplate-out req validated)
      (let [email (get validated "email")
            password (get validated "password")]
        (boilerplate-out req (biz/new-client email password false))))))

(defn clients-login [req]
  (let [params (boilerplate-in req (slurp (:body req)))
        validated (v/validate {"email" (fn [v] (or (v/validate-presence v) (v/validate-email v)))
                               "password" (fn [v] (or (v/validate-presence v) (v/validate-string v)))}
                               params)]
    (if (contains? validated :error)
      (boilerplate-out req validated)
      (let [email (get validated "email")
            password (get validated "password")]
        (boilerplate-out req (biz/auth-client email password))))))

(defn create-app [req]
  (let [params (boilerplate-in req (slurp (:body req)))
        validated (v/validate {"auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                               "app_name" (fn [v] (or (v/validate-presence v) (v/validate-string v)))}
                               params)]
    (if (contains? validated :error)
      (boilerplate-out req validated)
      (let [auth-key (get validated "auth_key")
            app-name (get validated "app_name")]
        (boilerplate-out req (biz/new-app auth-key app-name))))))

  (defn list-apps [req]
    (let [validated (v/validate-query req {"auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))} url-search-params)]
      (if (contains? validated :error)
        (boilerplate-out req validated)
        (let [auth-key (get validated "auth_key")]
          (boilerplate-out req (biz/get-clients-apps auth-key))))))

(defn delete-app [req]
  (let [params (boilerplate-in req (slurp (:body req)))
        validated (v/validate {"client_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                               "app_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))}
                               params)]
    (if (contains? validated :error)
      (boilerplate-out req validated)
      (let [client-auth-key (get validated "client_auth_key")
            app-auth-key (get validated "app_auth_key")]
        (boilerplate-out req (biz/delete-app client-auth-key app-auth-key))))))

(defn invite-to-app [req]
  (let [params (->> req :body slurp (boilerplate-in req))
        validated (v/validate {"inviter_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                               "app_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                               "invitee_email" (fn [v] (or (v/validate-presence v) (v/validate-email v)))}
                               params)]
    (if (contains? validated :error)
      (boilerplate-out req validated)
      (let [inviter-auth-key (get validated "inviter_auth_key")
            app-auth-key (get validated "app_auth_key")
            invitee-email (get validated "invitee_email")
            invitee-role (get params "invitee_role" "contributor")]
        (boilerplate-out req
                         (biz/invite-to-app-by-email inviter-auth-key
                                                     app-auth-key
                                                     invitee-email
                                                     invitee-role))))))

(defn revoke-from-app [req]
  (let [params (->> req :body slurp (boilerplate-in req))
        validated (v/validate {"revoker_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                               "app_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                               "revokee_email" (fn [v] (or (v/validate-presence v) (v/validate-email v)))}
                               params)]
    (if (contains? validated :error)
      (boilerplate-out req validated)
      (let [revoker-auth-key (get validated "revoker_auth_key")
            app-auth-key (get validated "app_auth_key")
            revokee-email (get validated "revokee_email")]
        (boilerplate-out req
                         (biz/revoke-from-app-by-email revoker-auth-key
                                                       app-auth-key
                                                       revokee-email))))))

(defn update-client-password [req]
  (let [params (->> req :body slurp (boilerplate-in req))
        validated (v/validate {"auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                               "old_password" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                               "new_password" (fn [v] (or (v/validate-presence v) (v/validate-string v)))}
                               params)]
    (if (contains? validated :error)
      (boilerplate-out req validated)
      (let [client-auth-key (get validated "auth_key")
            old-password (get validated "old_password")
            new-password (get validated "new_password")]
        (boilerplate-out req
                         (biz/change-client-password client-auth-key
                                                     old-password
                                                     new-password))))))
(defn delete-client [req]
  (let [params (->> req :body slurp (boilerplate-in req))
        validated (v/validate {"auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                               "password" (fn [v] (or (v/validate-presence v) (v/validate-string v)))}
                               params)]
    (if (contains? validated :error)
      (boilerplate-out req validated)
      (let [auth-key (get validated "auth_key")
            password (get validated "password")]
        (boilerplate-out req (biz/delete-client auth-key password))))))

(defn users-signup [req]
  (let [params (->> req :body slurp (boilerplate-in req))
        validated (v/validate {"app_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                               "email" (fn [v] (or (v/validate-presence v) (int? v) (v/validate-email v)))
                               "password" (fn [v] (or (v/validate-presence v) (v/validate-string v)))}
                               params)]
    (if (contains? validated :error)
      (boilerplate-out req validated)
      (let [app-auth-key (get validated "app_auth_key")
            email (get validated "email")
            password (get validated "password")]
        (boilerplate-out req (biz/new-user app-auth-key email password))))))

(defn users-login [req]
  (let [params (->> req :body slurp (boilerplate-in req))
        validated (v/validate {"app_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                               "email" (fn [v] (or (v/validate-presence v) (int? v) (v/validate-email v)))
                               "password" (fn [v] (or (v/validate-presence v) (v/validate-string v)))}
                               params)]
    (if (contains? validated :error)
      (boilerplate-out req validated)
      (let [app-auth-key (get validated "app_auth_key")
            email (get validated "email")
            password (get validated "password")]
        (boilerplate-out req (biz/auth-user app-auth-key email password))))))

(defn delete-user [req]
  (let [params (->> req :body slurp (boilerplate-in req))
        validated (v/validate {"user_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                               "password" (fn [v] (or (v/validate-presence v) (v/validate-string v)))}
                               params)]
    (if (contains? validated :error)
      (boilerplate-out req validated)
      (let [user-auth-key (get validated "user_auth_key")
            password (get validated "password")]
        (boilerplate-out req (biz/delete-user user-auth-key password))))))

(defn update-user-password [req]
  (let [params (->> req :body slurp (boilerplate-in req))
        validated (v/validate {"user_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                               "old_password" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                               "new_password" (fn [v] (or (v/validate-presence v) (v/validate-string v)))}
                               params)]
    (if (contains? validated :error)
      (boilerplate-out req validated)
      (let [user-auth-key (get validated "user_auth_key")
            old-password (get validated "old_password")
            new-password (get validated "new_password")]
        (boilerplate-out req
                         (biz/update-user-password user-auth-key
                                                   old-password
                                                   new-password))))))

  (defn list-app-users [req]
    (let [validated (v/validate-query req {"client_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                            "app_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))} url-search-params)]
      (if (contains? validated :error)
        (boilerplate-out req validated)
        (let [client-auth-key (get validated "client_auth_key")
              app-auth-key (get validated "app_auth_key")]
          (boilerplate-out req (biz/list-app-users client-auth-key app-auth-key))))))

   (defn upload-user-file [req]
     (let [contents (-> req :body slurp)
          validated (v/validate-headers req {"x-user-auth-key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                              "x-filename" (fn [v] (or (v/validate-presence v) (v/validate-string v)))})
          user-auth-key (get validated "x-user-auth-key")
          filename (get validated "x-filename")]
       (if (contains? validated :error)
         (boilerplate-out req validated)
         (boilerplate-out req (biz/upload-user-file user-auth-key filename contents)))))

   (defn download-user-file [req]
     (let [validated (v/validate-headers req {"x-user-auth-key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                               "x-filename" (fn [v] (or (v/validate-presence v) (v/validate-string v)))})
          user-auth-key (get validated "x-user-auth-key")
          filename (get validated "x-filename")]
       (if (contains? validated :error)
         (boilerplate-out req validated)
         (biz/download-user-file user-auth-key filename)))))

   (defn list-user-files [req]
     (let [validated (v/validate-headers req {"x-user-auth-key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))})
          auth-key (get validated "x-user-auth-key")]
       (if (contains? validated :error)
         (boilerplate-out req validated)
         (boilerplate-out req (biz/list-user-files auth-key)))))

   (defn delete-user-file [req]
     (let [validated (v/validate-headers req {"x-user-auth-key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                               "x-filename" (fn [v] (or (v/validate-presence v) (v/validate-string v)))})
          user-auth-key (get validated "x-user-auth-key")
          filename (get validated "x-filename")]
       (if (contains? validated :error)
         (boilerplate-out req validated)
         (boilerplate-out req (biz/delete-user-file user-auth-key filename)))))

   (defn upload-app-file [req]
     (let [contents (-> req :body slurp)
          validated (v/validate-headers req {"x-client-auth-key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                              "x-app-auth-key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                              "x-filename" (fn [v] (or (v/validate-presence v) (v/validate-string v)))})
          client-auth-key (get validated "x-client-auth-key")
          app-auth-key (get validated "x-app-auth-key")
          filename (get validated "x-filename")]
       (if (contains? validated :error)
         (boilerplate-out req validated)
           (->> (biz/upload-app-file client-auth-key app-auth-key filename contents)
               (boilerplate-out req)))))

   (defn download-app-file [req]
     (let [validated (v/validate-headers req {"x-client-auth-key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                               "x-app-auth-key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                               "x-filename" (fn [v] (or (v/validate-presence v) (v/validate-string v)))})
          client-auth-key (get validated "x-client-auth-key")
          app-auth-key (get validated "x-app-auth-key")
          filename (get validated "x-filename")]
       (if (contains? validated :error)
         (boilerplate-out req validated)
           (biz/download-app-file client-auth-key app-auth-key filename))))

   (defn delete-app-file [req]
     (let [validated (v/validate-headers req {"x-client-auth-key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                               "x-app-auth-key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                               "x-filename" (fn [v] (or (v/validate-presence v) (v/validate-string v)))})
          client-auth-key (get validated "x-client-auth-key")
          app-auth-key (get validated "x-app-auth-key")
          filename (get validated "x-filename")]
       (if (contains? validated :error)
         (boilerplate-out req validated)
           (boilerplate-out req (biz/delete-app-file client-auth-key app-auth-key filename)))))

  (defn list-app-files [req]
    (let [validated (v/validate-query req {"client_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                            "app_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))} url-search-params)]
     (if (contains? validated :error)
        (boilerplate-out req validated)
        (let [client-auth-key (get validated "client_auth_key")
             app-auth-key (get validated "app_auth_key")]
          (boilerplate-out req (biz/list-app-files client-auth-key app-auth-key))))))

  (defn list-app-managers [req]
    (let [validated (v/validate-query req {"client_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                            "app_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))} url-search-params)]
     (if (contains? validated :error)
        (boilerplate-out req validated)
        (let [client-auth-key (get validated "client_auth_key")
             app-auth-key (get validated "app_auth_key")]
          (boilerplate-out req
                           (biz/list-app-managers client-auth-key app-auth-key))))))

(defn revoke-app-manager [req]
   (let [params (->> req :body slurp (boilerplate-in req))
         validated (v/validate {"client_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                 "app_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                 "email_to_revoke" (fn [v] (or (v/validate-presence v) (v/validate-email v)))}
                               params)]
     (if (contains? validated :error)
        (boilerplate-out req validated)
        (let [client-auth-key (get validated "client_auth_key")
             app-auth-key (get validated "app_auth_key")
             email-to-revoke (get validated "email_to_revoke")]
          (boilerplate-out req
                           (biz/revoke-admin-access client-auth-key
                                                   app-auth-key
                                                   email-to-revoke))))))

(defn upload-action [req]
   (let [params (->> req :body slurp (boilerplate-in req))
         validated (v/validate {"client_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                 "app_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                 "action_name" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                 "action_script" (fn [v] (or (v/validate-presence v) (v/validate-string v)))}
                               params)]
     (if (contains? validated :error)
        (boilerplate-out req validated)
        (let [client-auth-key (get validated "client_auth_key")
             app-auth-key (get validated "app_auth_key")
             action-name (get validated "action_name")
             action-script (get validated "action_script")]
          (boilerplate-out req (biz/upsert-action client-auth-key
                                                 app-auth-key
                                                 action-name
                                                 action-script))))))

(defn update-action [req]
   (let [params (->> req :body slurp (boilerplate-in req))
         validated (v/validate {"client_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                 "app_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                 "old_action_name" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                 "new_action_name" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                 "action_script" (fn [v] (or (v/validate-presence v) (v/validate-string v)))}
                               params)]
     (if (contains? validated :error)
        (boilerplate-out req validated)
        (let [client-auth-key (get validated "client_auth_key")
             app-auth-key (get validated "app_auth_key")
             old-action-name (get validated "old_action_name")
             new-action-name (get validated "new_action_name")
             action-script (get validated "action_script")]
          (boilerplate-out req
                           (biz/update-action client-auth-key
                                             app-auth-key
                                             old-action-name
                                             new-action-name
                                             action-script))))))

   (defn upload-actions [req]
     (let [compressed-actions (-> req :body untar/slurp-bytes)
          validated (v/validate-headers req {"x-client-auth-key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                              "x-app-auth-key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))})
          client-auth-key (get validated "x-client-auth-key")
          app-auth-key (get validated "x-app-auth-key")]
       (if (contains? validated :error)
         (boilerplate-out req validated)
         (boilerplate-out req
                           (biz/upload-actions client-auth-key
                                             app-auth-key
                                             compressed-actions)))))

  (defn download-action [req]
    (let [validated (v/validate-query req {"client_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                            "app_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                            "action_name" (fn [v] (or (v/validate-presence v) (v/validate-string v)))} url-search-params)]
     (if (contains? validated :error)
        (boilerplate-out req validated)
        (let [client-auth-key (get validated "client_auth_key")
             app-auth-key (get validated "app_auth_key")
             action-name (get validated "action_name")]
          (biz/read-action client-auth-key app-auth-key action-name)))))

  (defn list-actions [req]
    (let [validated (v/validate-query req {"client_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                            "app_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))} url-search-params)]
     (if (contains? validated :error)
        (boilerplate-out req validated)
        (let [client-auth-key (get validated "client_auth_key")
             app-auth-key (get validated "app_auth_key")]
          (boilerplate-out req (biz/list-actions client-auth-key app-auth-key))))))

(defn delete-action [req]
   (let [params (->> req :body slurp (boilerplate-in req))
         validated (v/validate {"client_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                 "app_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                 "action_name" (fn [v] (or (v/validate-presence v) (v/validate-string v)))}
                               params)]
     (if (contains? validated :error)
        (boilerplate-out req validated)
        (let [client-auth-key (get validated "client_auth_key")
             app-auth-key (get validated "app_auth_key")
             action-name (get validated "action_name")]
          (boilerplate-out req
                           (biz/delete-action client-auth-key
                                             app-auth-key
                                             action-name))))))

(defn run-action [req]
   (let [params (->> req :body slurp (boilerplate-in req))
         validated (v/validate {"user_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                 "app_auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                 "action_name" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                 "action_param" (fn [v] (or (v/validate-presence v) (v/validate-string v)))}
                               params)]
     (if (contains? validated :error)
        (boilerplate-out req validated)
        (let [user-auth-key (get validated "user_auth_key")
             app-auth-key (get validated "app_auth_key")
             action-name (get validated "action_name")
             action-param (get validated "action_param")]
          (boilerplate-out req
                           (proxies/run-action user-auth-key
                                              app-auth-key
                                              action-name
                                              action-param))))))

   (defn- list-all-things [req f]
       (let [validated (v/validate-headers req {"x-client-auth-key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))})
             auth-key (get validated "x-client-auth-key")]
           (if (contains? validated :error)
             (boilerplate-out req validated)
             (boilerplate-out req (f auth-key)))))

(defn list-all-clients [req]
   (list-all-things req biz/list-all-clients))

(defn list-all-apps [req]
   (list-all-things req biz/list-all-apps))

(defn list-all-files [req]
   (list-all-things req biz/list-all-files))

(defn list-all-admins [req]
   (list-all-things req biz/list-all-admins))

(defn promote-to-admin [req]
   (let [params (->> req :body slurp (boilerplate-in req))
         validated (v/validate {"auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                 "email" (fn [v] (or (v/validate-presence v) (v/validate-email v)))}
                               params)]
     (if (contains? validated :error)
        (boilerplate-out req validated)
        (let [auth-key (get validated "auth_key")
             email (get validated "email")]
          (boilerplate-out req (biz/promote-to-admin auth-key email))))))

(defn demote-admin [req]
   (let [params (->> req :body slurp (boilerplate-in req))
         validated (v/validate {"auth_key" (fn [v] (or (v/validate-presence v) (v/validate-string v)))
                                 "email" (fn [v] (or (v/validate-presence v) (v/validate-string v)))}
                               params)]
     (if (contains? validated :error)
        (boilerplate-out req validated)
        (let [auth-key (get validated "auth_key")
             email (get validated "email")]
          (boilerplate-out req (biz/demote-admin auth-key email))))))

(defn check-admin [req]
   (list-all-things req biz/check-admin))

(defroutes app-routes
  (POST "/clients/signup" [] clients-signup)
  (POST "/clients/login" [] clients-login)
  (POST "/apps" [] create-app)
  (GET "/apps" [] list-apps)
  (DELETE "/apps" [] delete-app)
  (POST "/apps/invite" [] invite-to-app)
  (POST "/apps/revoke" [] revoke-from-app)
  (POST "/clients/password" [] update-client-password)
  (DELETE "/clients" [] delete-client)
  (POST "/users/signup" [] users-signup)
  (POST "/users/login" [] users-login)
  (DELETE "/users" [] delete-user)
  (GET "/apps/users" [] list-app-users)
  (POST "/users/password" [] update-user-password)
  (POST "/users/files" [] upload-user-file)
  (GET "/users/files" [] download-user-file)
  (GET "/users/files/list" [] list-user-files)
  (DELETE "/users/files" [] delete-user-file)
  (POST "/apps/files" [] upload-app-file)
  (GET "/apps/files" [] download-app-file)
  (DELETE "/apps/files" [] delete-app-file)
  (GET "/apps/files/list" [] list-app-files)
  (GET "/apps/clients" [] list-app-managers)
  (POST "/apps/clients/revoke" [] revoke-app-manager)
  (POST "/actions" [] upload-action)
  (PATCH "/actions" [] update-action)
  (POST "/actions/bulk" [] upload-actions)
  (GET "/actions" [] download-action)
  (GET "/actions/list" [] list-actions)
  (DELETE "/actions" [] delete-action)
  (POST "/actions/run" [] run-action)
  (GET "/clients/all" [] list-all-clients)
  (GET "/apps/all" [] list-all-apps)
  (GET "/files/all" [] list-all-files)
  (GET "/admins/all" [] list-all-admins)
  (POST "/admins" [] promote-to-admin)
  (DELETE "/admins" [] demote-admin)
  (GET "/admins/check" [] check-admin)
  (GET "/health" [] check-health))

; ################
; # Entry points #
; ################
(defn- migrate-up []
  (do
    (db/setup-database)
    (db/run-migrations)))

(defn- migrate-down []
  (do
    (db/undo-last-migration)))

(defn- run []
  (let [port (Integer/parseInt (or (System/getenv "API_PORT") "7780"))]
    (server/run-server (wrap-cors #'app-routes #".*"
                                  {:security nil})
                       {:port port})
    (println (str "Listening at http://localhost:" port "/"))))

(defn -main [& args]
  (do
    (when (some #(= "migrate-up" %) args)
      (migrate-up))
    (when (some #(= "migrate-down" %) args)
      (migrate-down))
    (when (some #(= "up" %) args)
      (run))
    (when (some #(= "to-recfile" %) args)
      (apply jobs/to-recfile (rest args)))
    (when (some #(= "from-recfile" %) args)
      (apply jobs/from-recfile (rest args)))))

