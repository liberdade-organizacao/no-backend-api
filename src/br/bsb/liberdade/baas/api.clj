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

(defn- parse-body [req]
  (boilerplate-in req (slurp (:body req))))

(defn- respond
  "Runs `on-valid` with the validated params, or returns the validation
  error response if `validated` failed schema validation."
  [req validated on-valid]
  (if (contains? validated :error)
    (boilerplate-out req validated)
    (on-valid validated)))

; ##########
; # ROUTES #
; ##########
(defn check-health [req]
  (boilerplate-out req
                   {"api" "ok"
                    "db" (db/check-health)
                    "scripting" (proxies/check-scripting-engine-health)
                    "version" "0.4.0"}))

(defn clients-signup [req]
  (respond req
           (v/validate {"email" v/required-email
                        "password" v/required-string}
                       (parse-body req))
           (fn [{email "email" password "password"}]
             (boilerplate-out req (biz/new-client email password false)))))

(defn clients-login [req]
  (respond req
           (v/validate {"email" v/required-email
                        "password" v/required-string}
                       (parse-body req))
           (fn [{email "email" password "password"}]
             (boilerplate-out req (biz/auth-client email password)))))

(defn create-app [req]
  (respond req
           (v/validate {"auth_key" v/required-string
                        "app_name" v/required-string}
                       (parse-body req))
           (fn [{auth-key "auth_key" app-name "app_name"}]
             (boilerplate-out req (biz/new-app auth-key app-name)))))

(defn list-apps [req]
  (respond req
           (v/validate-query req {"auth_key" v/required-string} url-search-params)
           (fn [{auth-key "auth_key"}]
             (boilerplate-out req (biz/get-clients-apps auth-key)))))

(defn delete-app [req]
  (respond req
           (v/validate {"client_auth_key" v/required-string
                        "app_auth_key" v/required-string}
                       (parse-body req))
           (fn [{client-auth-key "client_auth_key" app-auth-key "app_auth_key"}]
             (boilerplate-out req (biz/delete-app client-auth-key app-auth-key)))))

(defn invite-to-app [req]
  (respond req
           (v/validate {"inviter_auth_key" v/required-string
                        "app_auth_key" v/required-string
                        "invitee_email" v/required-email}
                       (parse-body req))
           (fn [{inviter-auth-key "inviter_auth_key"
                 app-auth-key "app_auth_key"
                 invitee-email "invitee_email"
                 :as validated}]
             (boilerplate-out req
                              (biz/invite-to-app-by-email inviter-auth-key
                                                          app-auth-key
                                                          invitee-email
                                                          (get validated "invitee_role" "contributor"))))))

(defn revoke-from-app [req]
  (respond req
           (v/validate {"revoker_auth_key" v/required-string
                        "app_auth_key" v/required-string
                        "revokee_email" v/required-email}
                       (parse-body req))
           (fn [{revoker-auth-key "revoker_auth_key"
                 app-auth-key "app_auth_key"
                 revokee-email "revokee_email"}]
             (boilerplate-out req
                              (biz/revoke-from-app-by-email revoker-auth-key
                                                            app-auth-key
                                                            revokee-email)))))

(defn update-client-password [req]
  (respond req
           (v/validate {"auth_key" v/required-string
                        "old_password" v/required-string
                        "new_password" v/required-string}
                       (parse-body req))
           (fn [{client-auth-key "auth_key"
                 old-password "old_password"
                 new-password "new_password"}]
             (boilerplate-out req
                              (biz/change-client-password client-auth-key
                                                          old-password
                                                          new-password)))))

(defn delete-client [req]
  (respond req
           (v/validate {"auth_key" v/required-string
                        "password" v/required-string}
                       (parse-body req))
           (fn [{auth-key "auth_key" password "password"}]
             (boilerplate-out req (biz/delete-client auth-key password)))))

(defn users-signup [req]
  (respond req
           (v/validate {"app_auth_key" v/required-string
                        "email" v/required-email
                        "password" v/required-string}
                       (parse-body req))
           (fn [{app-auth-key "app_auth_key" email "email" password "password"}]
             (boilerplate-out req (biz/new-user app-auth-key email password)))))

(defn users-login [req]
  (respond req
           (v/validate {"app_auth_key" v/required-string
                        "email" v/required-email
                        "password" v/required-string}
                       (parse-body req))
           (fn [{app-auth-key "app_auth_key" email "email" password "password"}]
             (boilerplate-out req (biz/auth-user app-auth-key email password)))))

(defn delete-user [req]
  (respond req
           (v/validate {"user_auth_key" v/required-string
                        "password" v/required-string}
                       (parse-body req))
           (fn [{user-auth-key "user_auth_key" password "password"}]
             (boilerplate-out req (biz/delete-user user-auth-key password)))))

(defn update-user-password [req]
  (respond req
           (v/validate {"user_auth_key" v/required-string
                        "old_password" v/required-string
                        "new_password" v/required-string}
                       (parse-body req))
           (fn [{user-auth-key "user_auth_key"
                 old-password "old_password"
                 new-password "new_password"}]
             (boilerplate-out req
                              (biz/update-user-password user-auth-key
                                                        old-password
                                                        new-password)))))

(defn list-app-users [req]
  (respond req
           (v/validate-query req {"client_auth_key" v/required-string
                                  "app_auth_key" v/required-string} url-search-params)
           (fn [{client-auth-key "client_auth_key" app-auth-key "app_auth_key"}]
             (boilerplate-out req (biz/list-app-users client-auth-key app-auth-key)))))

(defn upload-user-file [req]
  (let [contents (-> req :body slurp)]
    (respond req
             (v/validate-headers req {"x-user-auth-key" v/required-string
                                      "x-filename" v/required-string})
             (fn [{auth-key "x-user-auth-key" filename "x-filename"}]
               (boilerplate-out req (biz/upload-user-file auth-key filename contents))))))

(defn download-user-file [req]
  (respond req
           (v/validate-headers req {"x-user-auth-key" v/required-string
                                    "x-filename" v/required-string})
           (fn [{auth-key "x-user-auth-key" filename "x-filename"}]
             (biz/download-user-file auth-key filename))))

(defn list-user-files [req]
  (respond req
           (v/validate-headers req {"x-user-auth-key" v/required-string})
           (fn [{auth-key "x-user-auth-key"}]
             (boilerplate-out req (biz/list-user-files auth-key)))))

(defn delete-user-file [req]
  (respond req
           (v/validate-headers req {"x-user-auth-key" v/required-string
                                    "x-filename" v/required-string})
           (fn [{auth-key "x-user-auth-key" filename "x-filename"}]
             (boilerplate-out req (biz/delete-user-file auth-key filename)))))

(defn upload-app-file [req]
  (let [contents (-> req :body slurp)]
    (respond req
             (v/validate-headers req {"x-client-auth-key" v/required-string
                                      "x-app-auth-key" v/required-string
                                      "x-filename" v/required-string})
             (fn [{client-auth-key "x-client-auth-key"
                   app-auth-key "x-app-auth-key"
                   filename "x-filename"}]
               (boilerplate-out req (biz/upload-app-file client-auth-key app-auth-key filename contents))))))

(defn download-app-file [req]
  (respond req
           (v/validate-headers req {"x-client-auth-key" v/required-string
                                    "x-app-auth-key" v/required-string
                                    "x-filename" v/required-string})
           (fn [{client-auth-key "x-client-auth-key"
                 app-auth-key "x-app-auth-key"
                 filename "x-filename"}]
             (biz/download-app-file client-auth-key app-auth-key filename))))

(defn delete-app-file [req]
  (respond req
           (v/validate-headers req {"x-client-auth-key" v/required-string
                                    "x-app-auth-key" v/required-string
                                    "x-filename" v/required-string})
           (fn [{client-auth-key "x-client-auth-key"
                 app-auth-key "x-app-auth-key"
                 filename "x-filename"}]
             (boilerplate-out req (biz/delete-app-file client-auth-key app-auth-key filename)))))

(defn list-app-files [req]
  (respond req
           (v/validate-query req {"client_auth_key" v/required-string
                                  "app_auth_key" v/required-string} url-search-params)
           (fn [{client-auth-key "client_auth_key" app-auth-key "app_auth_key"}]
             (boilerplate-out req (biz/list-app-files client-auth-key app-auth-key)))))

(defn list-app-managers [req]
  (respond req
           (v/validate-query req {"client_auth_key" v/required-string
                                  "app_auth_key" v/required-string} url-search-params)
           (fn [{client-auth-key "client_auth_key" app-auth-key "app_auth_key"}]
             (boilerplate-out req
                              (biz/list-app-managers client-auth-key app-auth-key)))))

(defn revoke-app-manager [req]
  (respond req
           (v/validate {"client_auth_key" v/required-string
                        "app_auth_key" v/required-string
                        "email_to_revoke" v/required-email}
                       (parse-body req))
           (fn [{client-auth-key "client_auth_key"
                 app-auth-key "app_auth_key"
                 email-to-revoke "email_to_revoke"}]
             (boilerplate-out req
                              (biz/revoke-admin-access client-auth-key
                                                       app-auth-key
                                                       email-to-revoke)))))

(defn upload-action [req]
  (respond req
           (v/validate {"client_auth_key" v/required-string
                        "app_auth_key" v/required-string
                        "action_name" v/required-string
                        "action_script" v/required-string}
                       (parse-body req))
           (fn [{client-auth-key "client_auth_key"
                 app-auth-key "app_auth_key"
                 action-name "action_name"
                 action-script "action_script"}]
             (boilerplate-out req (biz/upsert-action client-auth-key
                                                     app-auth-key
                                                     action-name
                                                     action-script)))))

(defn update-action [req]
  (respond req
           (v/validate {"client_auth_key" v/required-string
                        "app_auth_key" v/required-string
                        "old_action_name" v/required-string
                        "new_action_name" v/required-string
                        "action_script" v/required-string}
                       (parse-body req))
           (fn [{client-auth-key "client_auth_key"
                 app-auth-key "app_auth_key"
                 old-action-name "old_action_name"
                 new-action-name "new_action_name"
                 action-script "action_script"}]
             (boilerplate-out req
                              (biz/update-action client-auth-key
                                                 app-auth-key
                                                 old-action-name
                                                 new-action-name
                                                 action-script)))))

(defn upload-actions [req]
  (let [compressed-actions (-> req :body untar/slurp-bytes)]
    (respond req
             (v/validate-headers req {"x-client-auth-key" v/required-string
                                      "x-app-auth-key" v/required-string})
             (fn [{client-auth-key "x-client-auth-key" app-auth-key "x-app-auth-key"}]
               (boilerplate-out req
                                (biz/upload-actions client-auth-key
                                                    app-auth-key
                                                    compressed-actions))))))

(defn download-action [req]
  (respond req
           (v/validate-query req {"client_auth_key" v/required-string
                                  "app_auth_key" v/required-string
                                  "action_name" v/required-string} url-search-params)
           (fn [{client-auth-key "client_auth_key"
                 app-auth-key "app_auth_key"
                 action-name "action_name"}]
             (biz/read-action client-auth-key app-auth-key action-name))))

(defn list-actions [req]
  (respond req
           (v/validate-query req {"client_auth_key" v/required-string
                                  "app_auth_key" v/required-string} url-search-params)
           (fn [{client-auth-key "client_auth_key" app-auth-key "app_auth_key"}]
             (boilerplate-out req (biz/list-actions client-auth-key app-auth-key)))))

(defn delete-action [req]
  (respond req
           (v/validate {"client_auth_key" v/required-string
                        "app_auth_key" v/required-string
                        "action_name" v/required-string}
                       (parse-body req))
           (fn [{client-auth-key "client_auth_key"
                 app-auth-key "app_auth_key"
                 action-name "action_name"}]
             (boilerplate-out req
                              (biz/delete-action client-auth-key
                                                 app-auth-key
                                                 action-name)))))

(defn run-action [req]
  (respond req
           (v/validate {"user_auth_key" v/required-string
                        "app_auth_key" v/required-string
                        "action_name" v/required-string
                        "action_param" v/required-string}
                       (parse-body req))
           (fn [{user-auth-key "user_auth_key"
                 app-auth-key "app_auth_key"
                 action-name "action_name"
                 action-param "action_param"}]
             (boilerplate-out req
                              (proxies/run-action user-auth-key
                                                  app-auth-key
                                                  action-name
                                                  action-param)))))

(defn- list-all-things [req f]
  (respond req
           (v/validate-headers req {"x-client-auth-key" v/required-string})
           (fn [{auth-key "x-client-auth-key"}]
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
  (respond req
           (v/validate {"auth_key" v/required-string
                        "email" v/required-email}
                       (parse-body req))
           (fn [{auth-key "auth_key" email "email"}]
             (boilerplate-out req (biz/promote-to-admin auth-key email)))))

(defn demote-admin [req]
  (respond req
           (v/validate {"auth_key" v/required-string
                        "email" v/required-string}
                       (parse-body req))
           (fn [{auth-key "auth_key" email "email"}]
             (boilerplate-out req (biz/demote-admin auth-key email)))))

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
    (when (some #(= "to-recfile" %) args)
      (apply jobs/to-recfile (rest args)))
    (when (some #(= "from-recfile" %) args)
      (apply jobs/from-recfile (rest args)))
    (when (some #(= "up" %) args)
      (run))))

