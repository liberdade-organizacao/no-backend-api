(ns br.bsb.liberdade.baas.proxies
  (:require [clojure.data.json :as json]
            [clj-http.client :as client]
            [br.bsb.liberdade.baas.utils :as utils]))

(def scripting-engine-url (or (System/getenv "SCRIPTING_ENGINE_URL") "http://localhost:8080"))

(defn run-action [client-auth-key app-auth-key action-name action-param]
  (let [client-info (utils/decode-secret client-auth-key)
        client-id (:client_id client-info)
	app-info (utils/decode-secret app-auth-key)
	app-id (:app_id app-info)
	params {"client_id" client-id
	        "app_id" app-id
		"action_name" action-name
		"action_param" action-param}
	url (str scripting-engine-url "/actions/run")]
    (-> (client/post url
                     {:body (json/write-str params)
		      :content-type :json
		      :accept :json})
        json/read-str)))

