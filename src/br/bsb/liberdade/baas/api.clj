(ns br.bsb.liberdade.baas.api
  (:gen-class)
  (:require [clojure.data.json :as json]
            [clojure.string :as string]
            [org.httpkit.server :as server]
            [compojure.core :refer :all]
            [jumblerg.middleware.cors :refer [wrap-cors]]
            [selmer.parser :refer :all]
            [br.bsb.liberdade.baas.db :as db]
            [br.bsb.liberdade.baas.business :as biz]
            [br.bsb.liberdade.baas.proxies :as proxies]
            [br.bsb.liberdade.baas.tar.decompress :as untar]
            [br.bsb.liberdade.baas.jobs :as jobs]))

; #############
; # UTILITIES #
; #############
(defn- boilerplate [body]
  {:status (if (-> body (get :error) nil?) 200 400)
   :headers {"Content-Type" "text/json"
             "Access-Control-Allow-Origin" "*"
             "Access-Control-Expose-Headers" "*"}
   :body (str (json/write-str body))})

(defn- url-search-params [raw]
  (->> (string/split raw #"&")
       (map #(string/split % #"="))
       (reduce (fn [state [key value]] (assoc state key value)) {})))

; ##########
; # ROUTES #
; ##########
(defn check-health [req]
  (boilerplate {"api" "ok"
                "db" (db/check-health)
                "scripting" (proxies/check-scripting-engine-health)
                "version" "0.3.0"}))

(defn clients-signup [req]
  (let [params (json/read-str (slurp (:body req)))
        email (get params "email")
        password (get params "password")]
    (boilerplate (biz/new-client email password false))))

(defn clients-login [req]
  (let [params (json/read-str (slurp (:body req)))
        email (get params "email")
        password (get params "password")]
    (boilerplate (biz/auth-client email password))))

(defn create-app [req]
  (let [params (json/read-str (slurp (:body req)))
        auth-key (get params "auth_key")
        app-name (get params "app_name")]
    (boilerplate (biz/new-app auth-key app-name))))

(defn list-apps [req]
  (let [query-string (:query-string req)
        search-params (url-search-params query-string)
        auth-key (get search-params "auth_key")]
    (boilerplate (biz/get-clients-apps auth-key))))

(defn delete-app [req]
  (let [params (json/read-str (slurp (:body req)))
        client-auth-key (get params "client_auth_key")
        app-auth-key (get params "app_auth_key")]
    (boilerplate (biz/delete-app client-auth-key app-auth-key))))

(defn invite-to-app [req]
  (let [params (-> req :body slurp json/read-str)
        inviter-auth-key (get params "inviter_auth_key" nil)
        app-auth-key (get params "app_auth_key" nil)
        invitee-email (get params  "invitee_email" nil)
        invitee-role (get params "invitee_role" "contributor")]
    (boilerplate (biz/invite-to-app-by-email inviter-auth-key
                                             app-auth-key
                                             invitee-email
                                             invitee-role))))

(defn revoke-from-app [req]
  (let [params (-> req :body slurp json/read-str)
        revoker-auth-key (get params "revoker_auth_key" nil)
        app-auth-key (get params "app_auth_key" nil)
        revokee-email (get params  "revokee_email" nil)]
    (boilerplate (biz/revoke-from-app-by-email revoker-auth-key
                                               app-auth-key
                                               revokee-email))))

(defn update-client-password [req]
  (let [params (-> req :body slurp json/read-str)
        client-auth-key (get params "auth_key" nil)
        old-password (get params "old_password" nil)
        new-password (get params "new_password" nil)]
    (boilerplate (biz/change-client-password client-auth-key
                                             old-password
                                             new-password))))
(defn delete-client [req]
  (let [params (-> req :body slurp json/read-str)
        auth-key (get params "auth_key" nil)
        password (get params "password" nil)]
    (boilerplate (biz/delete-client auth-key password))))

(defn users-signup [req]
  (let [params (-> req :body slurp json/read-str)
        app-auth-key (get params "app_auth_key" nil)
        email (get params "email" nil)
        password (get params "password" nil)]
    (boilerplate (biz/new-user app-auth-key email password))))

(defn users-login [req]
  (let [params (-> req :body slurp json/read-str)
        app-auth-key (get params "app_auth_key" nil)
        email (get params "email" nil)
        password (get params "password" nil)]
    (boilerplate (biz/auth-user app-auth-key email password))))

(defn delete-user [req]
  (let [params (-> req :body slurp json/read-str)
        user-auth-key (get params "user_auth_key" nil)
        password (get params "password" nil)]
    (boilerplate (biz/delete-user user-auth-key password))))

(defn update-user-password [req]
  (let [params (-> req :body slurp json/read-str)
        user-auth-key (get params "user_auth_key" nil)
        old-password (get params "old_password" nil)
        new-password (get params "new_password" nil)]
    (boilerplate (biz/update-user-password user-auth-key old-password new-password))))

(defn list-app-users [req]
  (let [params (-> req :query-string url-search-params)
        client-auth-key (get params "client_auth_key" nil)
        app-auth-key (get params "app_auth_key" nil)]
    (boilerplate (biz/list-app-users client-auth-key app-auth-key))))

(defn upload-user-file [req]
  (let [user-auth-key (-> req :headers (get "x-user-auth-key"))
        filename (-> req :headers (get "x-filename"))
        contents (-> req :body slurp)]
    (boilerplate (biz/upload-user-file user-auth-key filename contents))))

(defn download-user-file [req]
  (let [user-auth-key (-> req :headers (get "x-user-auth-key"))
        filename (-> req :headers (get "x-filename"))]
    (biz/download-user-file user-auth-key filename)))

(defn list-user-files [req]
  (-> req
      :headers
      (get "x-user-auth-key")
      biz/list-user-files
      boilerplate))

(defn delete-user-file [req]
  (let [user-auth-key (-> req :headers (get "x-user-auth-key"))
        filename (-> req :headers (get "x-filename"))]
    (boilerplate (biz/delete-user-file user-auth-key filename))))

(defn upload-app-file [req]
  (let [client-auth-key (-> req :headers (get "x-client-auth-key"))
        app-auth-key (-> req :headers (get "x-app-auth-key"))
        filename (-> req :headers (get "x-filename"))
        contents (-> req :body slurp)]
    (-> (biz/upload-app-file client-auth-key app-auth-key filename contents)
        boilerplate)))

(defn download-app-file [req]
  (let [client-auth-key (-> req :headers (get "x-client-auth-key"))
        app-auth-key (-> req :headers (get "x-app-auth-key"))
        filename (-> req :headers (get "x-filename"))]
    (biz/download-app-file client-auth-key app-auth-key filename)))

(defn delete-app-file [req]
  (let [client-auth-key (-> req :headers (get "x-client-auth-key"))
        app-auth-key (-> req :headers (get "x-app-auth-key"))
        filename (-> req :headers (get "x-filename"))]
    (boilerplate (biz/delete-app-file client-auth-key app-auth-key filename))))

(defn list-app-files [req]
  (let [params (-> req :query-string url-search-params)
        client-auth-key (get params "client_auth_key")
        app-auth-key (get params "app_auth_key")]
    (boilerplate (biz/list-app-files client-auth-key app-auth-key))))

(defn list-app-managers [req]
  (let [params (-> req :query-string url-search-params)
        client-auth-key (get params "client_auth_key" nil)
        app-auth-key (get params "app_auth_key" nil)]
    (boilerplate (biz/list-app-managers client-auth-key app-auth-key))))

(defn revoke-app-manager [req]
  (let [params (-> req :body slurp json/read-str)
        client-auth-key (get params "client_auth_key" nil)
        app-auth-key (get params "app_auth_key" nil)
        email-to-revoke (get params "email_to_revoke" nil)]
    (boilerplate (biz/revoke-admin-access client-auth-key
                                          app-auth-key
                                          email-to-revoke))))

(defn upload-action [req]
  (let [params (-> req :body slurp json/read-str)
        client-auth-key (get params "client_auth_key" nil)
        app-auth-key (get params "app_auth_key" nil)
        action-name (get params "action_name" nil)
        action-script (get params "action_script" nil)]
    (boilerplate (biz/upsert-action client-auth-key
                                    app-auth-key
                                    action-name
                                    action-script))))

(defn update-action [req]
  (let [params (-> req :body slurp json/read-str)
        client-auth-key (get params "client_auth_key" nil)
        app-auth-key (get params "app_auth_key" nil)
        old-action-name (get params "old_action_name" nil)
        new-action-name (get params "new_action_name" nil)
        action-script (get params "action_script" nil)]
    (boilerplate (biz/update-action client-auth-key
                                    app-auth-key
                                    old-action-name
                                    new-action-name
                                    action-script))))

(defn upload-actions [req]
  (let [client-auth-key (-> req :headers (get "x-client-auth-key"))
        app-auth-key (-> req :headers (get "x-app-auth-key"))
        compressed-actions (-> req :body untar/slurp-bytes)]
    (boilerplate (biz/upload-actions client-auth-key
                                     app-auth-key
                                     compressed-actions))))

(defn download-action [req]
  (let [params (-> req :query-string url-search-params)
        client-auth-key (get params "client_auth_key" nil)
        app-auth-key (get params "app_auth_key" nil)
        action-name (get params "action_name" nil)]
    (biz/read-action client-auth-key app-auth-key action-name)))

(defn list-actions [req]
  (let [params (-> req :query-string url-search-params)
        client-auth-key (get params "client_auth_key" nil)
        app-auth-key (get params "app_auth_key" nil)]
    (boilerplate (biz/list-actions client-auth-key app-auth-key))))

(defn delete-action [req]
  (let [params (-> req :body slurp json/read-str)
        client-auth-key (get params "client_auth_key" nil)
        app-auth-key (get params "app_auth_key" nil)
        action-name (get params "action_name" nil)]
    (boilerplate (biz/delete-action client-auth-key
                                    app-auth-key
                                    action-name))))

(defn run-action [req]
  (let [params (-> req :body slurp json/read-str)
        user-auth-key (get params "user_auth_key" nil)
        app-auth-key (get params "app_auth_key" nil)
        action-name (get params "action_name" nil)
        action-param (get params "action_param" nil)]
    (boilerplate (proxies/run-action user-auth-key
                                     app-auth-key
                                     action-name
                                     action-param))))

(defn- list-all-things [req f]
  (-> req
      :headers
      (get "x-client-auth-key")
      f
      boilerplate))

(defn list-all-clients [req]
  (list-all-things req biz/list-all-clients))

(defn list-all-apps [req]
  (list-all-things req biz/list-all-apps))

(defn list-all-files [req]
  (list-all-things req biz/list-all-files))

(defn list-all-admins [req]
  (list-all-things req biz/list-all-admins))

(defn promote-to-admin [req]
  (let [params (-> req :body slurp json/read-str)
        auth-key (get params "auth_key" nil)
        email (get params "email" nil)]
    (boilerplate (biz/promote-to-admin auth-key email))))

(defn demote-admin [req]
  (let [params (-> req :body slurp json/read-str)
        auth-key (get params "auth_key" nil)
        email (get params "email" nil)]
    (boilerplate (biz/demote-admin auth-key email))))

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

