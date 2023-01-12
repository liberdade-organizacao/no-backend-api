(ns br.bsb.liberdade.baas.api
  (:gen-class)
  (:require [clojure.data.json :as json]
            [clojure.string :as string]
            [org.httpkit.server :as server]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer :all]
            [jumblerg.middleware.cors :refer [wrap-cors]]
            [selmer.parser :refer :all]
            [br.bsb.liberdade.baas.db :as db]
            [br.bsb.liberdade.baas.business :as biz]))

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
  (boilerplate {"status" "ok"}))

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

(defn upload-user-file [req]
  ; XXX is there other way of doing this without the need of headers?
  ; XXX or maybe headers should be used everywhere?
  (let [user-auth-key (-> req :headers (get "x-user-auth-key"))
        filename (-> req :headers (get "x-filename"))
        contents (-> req :body slurp)]
    (boilerplate (biz/upload-user-file user-auth-key filename contents))))

(defn download-user-file [req]
  (let [user-auth-key (-> req :headers (get "x-user-auth-key"))
        filename (-> req :headers (get "x-filename"))]
    (biz/download-user-file user-auth-key filename)))

(defn list-user-files [req]
  (let [user-auth-key nil]
    (biz/list-user-files user-auth-key)))

(defn delete-user-file [req]
  (let [params (-> req :body slurp json/read-str)
        user-auth-key (get params "auth_key" nil)
        filename (get params "filename" nil)]
    (boilerplate (biz/delete-user-file user-auth-key filename))))

(defroutes app-routes
  (POST "/clients/signup" [] clients-signup)
  (POST "/clients/login" [] clients-login)
  (POST "/apps" [] create-app)
  (GET "/apps" [] list-apps)
  (DELETE "/apps" [] delete-app)
  (POST "/apps/invite" [] invite-to-app)
  (POST "/clients/password" [] update-client-password)
  (DELETE "/clients" [] delete-client)
  (POST "/users/signup" [] users-signup)
  (POST "/users/login" [] users-login)
  (POST "/users/files" [] upload-user-file)
  (GET "/users/files" [] download-user-file)
  (GET "/users/files/list" [] list-user-files)
  (DELETE "/users/files" [] delete-user-file)
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
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (server/run-server (wrap-cors #'app-routes #".*" (assoc site-defaults :security nil)) {:port port})
    (println (str "Listening at http://localhost:" port "/"))))


(defn -main [& args]
 (let []
    (when (some #(= "migrate-up" %) args)
      (migrate-up))
    (when (some #(= "migrate-down" %) args)
      (migrate-down))
    (when (some #(= "up" %) args)
      (run))))

