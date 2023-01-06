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
            [br.bsb.liberdade.baas.controllers :as controllers]))

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
    (boilerplate (controllers/new-client email password false))))

(defn clients-login [req]
  (let [params (json/read-str (slurp (:body req)))
        email (get params "email")
        password (get params "password")]
    (boilerplate (controllers/auth-client email password))))

(defn create-app [req]
  (let [params (json/read-str (slurp (:body req)))
        auth-key (get params "auth_key")
        app-name (get params "app_name")]
    (boilerplate (controllers/new-app auth-key app-name))))

(defn list-apps [req]
  (let [query-string (:query-string req)
        search-params (url-search-params query-string)
        auth-key (get search-params "auth_key")]
    (boilerplate (controllers/get-clients-apps auth-key))))

(defn delete-app [req]
  (let [params (json/read-str (slurp (:body req)))
        client-auth-key (get params "client_auth_key")
        app-auth-key (get params "app_auth_key")]
    (boilerplate (controllers/delete-app client-auth-key app-auth-key))))

(defroutes app-routes
  (POST "/clients/signup" [] clients-signup)
  (POST "/clients/login" [] clients-login)
  (POST "/apps" [] create-app)
  (GET "/apps" [] list-apps)
  (DELETE "/apps" [] delete-app)
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

