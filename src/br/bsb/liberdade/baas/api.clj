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
            [br.bsb.liberdade.baas.db :as db]))

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

(defn create-users [req]
  (let [params (json/read-str (slurp (:body req)))
        username (get params "username")
        password (get params "password")
        notes (get params "notes")]
    (boilerplate (db/create-user username password notes))))

(defn auth-users [req]
  (let [params (json/read-str (slurp (:body req)))
        username (get params "username")
        password (get params "password")]
    (boilerplate (db/auth-user username password))))

(defn update-password [req]
  (let [params (-> req :body slurp json/read-str)
        auth-key (get params "auth_key")
        old-password (get params "old_password")
        new-password (get params "new_password")]
    (boilerplate (db/update-password auth-key old-password new-password))))

(defn get-notes [req]
  (-> req
      :query-string
      (url-search-params)
      (get "auth_key")
      (db/get-notes)
      (boilerplate)))

(defn post-notes [req]
  (let [params (json/read-str (slurp (:body req)))
        auth-key (get params "auth_key")
        notes (get params "notes")]
    (boilerplate (db/update-notes auth-key notes))))

(defn export-backup [req]
  (-> req
      :query-string
      (url-search-params)
      (get "auth_key")
      (db/backup)
      (boilerplate)))

(defn import-backup [req]
  (let [params (json/read-str (slurp (:body req)))
        auth-key (get params "auth_key")
        backup (get params "backup")]
    (boilerplate (db/import-backup auth-key backup))))

(defroutes app-routes
  (GET "/health" [] check-health)
  (POST "/users/create" [] create-users)
  (POST "/users/auth" [] auth-users)
  (POST "/users/password" [] update-password)
  (GET "/notes" [] get-notes)
  (POST "/notes" [] post-notes)
  (GET "/backup" [] export-backup)
  (POST "/backup" [] import-backup))

; ################
; # Entry points #
; ################
(defn- migrate []
  (do
    (db/setup-database)))

(defn- run []
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (server/run-server (wrap-cors #'app-routes #".*" (assoc site-defaults :security nil)) {:port port})
    (println (str "Listening at http://localhost:" port "/"))))


(defn -main [& args]
  (do 
    (when (some #(= "m" %) args)
      (migrate))
    (when (nil? args)
      (run))))
