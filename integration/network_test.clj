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
    (println body)
    body))

(defn- auth-client [email password]
  (let [url (str service-url "/clients/login")
        params {"email" email
                "password" password}
        response (curl/post url {:body (json/generate-string params)})
        body (json/parse-string (:body response))]
    (println body)
    body))

(defn- main []
  (let [email (str "u" (random-string 6) "@liberdade.bsb.br")
        password (random-string 12)]
    (when (wait-for-server 5)
      (println (str "email: " email))
      (println (str "password: " password))
      (create-client-account email password)
      (auth-client email password)
      (println "..."))))

(main)

