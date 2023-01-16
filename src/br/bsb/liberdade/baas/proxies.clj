(ns br.bsb.liberdade.baas.proxies
  (:require [clojure.data.json :as json]
            [clj-http.client :as client]
            [br.bsb.liberdade.baas.utils :as utils]))

(def scripting-engine-url (or (System/getenv "SCRIPTING_ENGINE_URL") "http://localhost:8080"))

(defn run-action [user-auth-key app-auth-key action-name action-param]
  (let [user-info (utils/decode-secret user-auth-key)
        user-id (:user_id user-info)
        app-info (utils/decode-secret app-auth-key)
        app-id (:app_id app-info)
        params {"user_id" user-id
                "app_id" app-id
                "action_name" action-name
                "action_param" action-param}
        url (str scripting-engine-url "/actions/run")]
    (-> (client/post url
                     {:body (-> params json/write-str str)
                      :content-type :json
                      :accept :json})
        :body
        json/read-str)))

