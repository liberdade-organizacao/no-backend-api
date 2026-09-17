(ns br.bsb.liberdade.baas.proxies
  (:require [clj-http.client :as client]
            [msgpack.core :as msgpack]
            [br.bsb.liberdade.baas.utils :as utils]))

(def scripting-engine-url (or (System/getenv "SCRIPTING_ENGINE_URL") "http://localhost:7781"))

(defn- spy [it]
  (println it)
  it)

(defn run-action [user-auth-key app-auth-key action-name action-param]
  (let [user-id (:user_id (utils/decode-secret user-auth-key))
        app-id (:app_id (utils/decode-secret app-auth-key))
        params {"user_id" (double user-id)
                "app_id" (double app-id)
                "action_name" action-name
                "action_param" action-param}
        url (str scripting-engine-url "/actions/run")]
    (try
      (-> (client/post url
                       {:body (spy (msgpack/pack params))
                        :headers {"Content-Type" "application/vnd.msgpack"}})
          :body
          spy
          .getBytes
          spy
          msgpack/unpack
          spy)
      (catch Exception e
        (println e)
        {"error" e}))))

(defn check-scripting-engine-health []
  (try
    (-> scripting-engine-url
        (str "/health")
        client/get
        :body)
    (catch Exception ex
      "KO")))

