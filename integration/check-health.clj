(ns br.bsb.liberdade.baas.integration.check-health
  (:require [babashka.curl :as curl]
            [cheshire.core :as json]))

(def service-url "http://localhost:7780")

(defn main []
  (let [url (str service-url "/health")
        response (curl/get url)
        body (-> response (get :body) json/parse-string)]
    (clojure.pprint/pprint body)
    body))

(main)

