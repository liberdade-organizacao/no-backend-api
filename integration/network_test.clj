(ns br.bsb.liberdade.baas.integration-test.network-test
  (:require [babashka.curl :as curl]
            [cheshire.core :as json]
            [clojure.java.shell :as shell]))

(def service-url "http://localhost:3000")

(defn- random-string [length]
  (loop [alphabet "abcdefghijklmnopqrstuvwxyz"
         i 0
         state ""]
    (if (>= i length)
      state
      (recur alphabet
             (inc i)
             (str state (nth alphabet (rand-int (count alphabet))))))))

(defn- check-health []
  (try
    (let [url (str service-url "/health")
          response (curl/get url)
          status (-> response (get :body) json/parse-string (get "status"))]
      (= "ok" status))
    (catch Exception e
      false)))

(defn- sleep [t]
  (shell/sh "sleep" (str t)))

(defn- wait-for-server [no-tries]
  (do
    (println "--- # waiting for server")
    (loop [i 0]
      (cond 
        (check-health) true
        (< i no-tries) (do
                         (sleep 2)
                         (recur (inc i)))
        :else false))))

(defn- create-client-account [email password]
  (let [url (str service-url "/clients/signup")
        params {"email" email
                "password" password}
        response (curl/post url {:body (json/generate-string params)})
        body (json/parse-string (:body response))]
    (println "# create client")
    (println body)
    body))

(defn- auth-client [email password]
  (let [url (str service-url "/clients/login")
        params {"email" email
                "password" password}
        response (curl/post url {:body (json/generate-string params)})
        body (json/parse-string (:body response))]
    (println "# auth client")
    (println body)
    body))

(defn- create-app [auth-key app-name]
  (let [url (str service-url "/apps")
        params {"auth_key" auth-key
                "app_name" app-name}
        response (curl/post url {:body (json/generate-string params)})
        body (json/parse-string (:body response))]
    (println "# create app")
    (println body)
    body))

(defn- list-apps [auth-key]
  (let [url (str service-url "/apps")
        query-params {"auth_key" auth-key}
        response (curl/get url {:query-params query-params})
        body (json/parse-string (get response :body))]
    (println "# list apps")
    (println body)
    body))

(defn- delete-app [client-auth-key app-auth-key]
  (let [url (str service-url "/apps")
        params {"client_auth_key" client-auth-key
                "app_auth_key" app-auth-key}
        response (curl/delete url {:body (json/generate-string params)})
        body (json/parse-string (get response :body))]
    (println "# delete app")
    (println body)
    body))

(defn- main []
  (let [email (str "u" (random-string 6) "@liberdade.bsb.br")
        password (random-string 12)]
    (when (wait-for-server 5)
      (println (str "email: " email))
      (println (str "password: " password))
      (create-client-account email password)
      (let [auth-key (-> (auth-client email password) 
                         (get "auth_key"))
            app-auth-key (-> (create-app auth-key (random-string 10))
                             (get "auth_key"))
            _ (list-apps auth-key)
            _ (delete-app auth-key app-auth-key)
            _ (list-apps auth-key)]
        nil)
      (println "..."))))

(main)

