(ns br.bsb.liberdade.baas.integration-test.generate-mock-data
  (:require [babashka.curl :as curl]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]))

(def service-url "http://localhost:7780")

(defn- create-client [email password]
  (let [url (str service-url "/clients/signup")
        params {"email" email
                "password" password}
        response (curl/post url {:body (json/generate-string params)})]
    (json/parse-string (:body response))))

(defn- create-app [auth-key app-name]
  (let [url (str service-url "/apps")
        params {"auth_key" auth-key
                "app_name" app-name}
        response (curl/post url {:body (json/generate-string params)})
        body (json/parse-string (:body response))]
    body))

(defn main []
  (let [client-creation-result (create-client "hello@crisjr.eng.br" "password")
        app-creation-result (create-app (get client-creation-result "auth_key" nil) "Shiny App")]
    (println client-creation-result)
    (println app-creation-result)))

(main)

