(ns br.bsb.liberdade.baas.integration-test.generate-mock-data
  (:require [babashka.curl :as curl]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]))

(def service-url "http://localhost:7780")
(def standard-script "
  function main(param)
    return \"bye bye \" .. param .. \"?\"
  end
")

(defn- random-string [length]
  (loop [alphabet "abcdefghijklmnopqrstuvwxyz"
         i 0
         state ""]
    (if (>= i length)
      state
      (recur alphabet
             (inc i)
             (str state (nth alphabet (rand-int (count alphabet))))))))

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

(defn- create-user [app-auth-key email password]
  (let [url (str service-url "/users/signup")
        params {"app_auth_key" app-auth-key
	        "email" email
		"password" password}
	response (curl/post url {:body (json/generate-string params)})
	body (json/parse-string (:body response))]
    body))

(defn- create-action 
  [client-auth-key app-auth-key action-name action-script]
  (let [url (str service-url "/actions")
        params {"client_auth_key" client-auth-key
	        "app_auth_key" app-auth-key
		"action_name" action-name
		"action_script" action-script}
	response (curl/post url {:body (json/generate-string params)})
	body (json/parse-string (:body response))]
    body))

(defn main []
  (let [client-creation-result (create-client "hello@crisjr.eng.br" "password")
        app-creation-result (create-app (get client-creation-result "auth_key" nil) "Shiny App")]
    (println client-creation-result)
    (println app-creation-result))
  (println (map (fn [email]
         (let [client-creation-result (create-client email "pwd")
	       client-auth-key (get client-creation-result "auth_key" nil)
	       how-many-apps (+ 3 (rand-int 5))]
	   (loop [app-i 0]
	     (when (< app-i how-many-apps)
	       (let [app-name (str "app " (random-string 10))
	             app-creation-result (create-app client-auth-key 
		                                     app-name)
		     app-auth-key (get app-creation-result "auth_key" nil)
		     how-many-users (+ 20 (rand-int 50))
		     how-many-actions (inc (rand-int 3))]
		 (println (str "users: " how-many-users))
		 (println (str "actions: " how-many-actions))
		 (loop [user-i 0
		        user-info (create-user app-auth-key
			                       (str "u"
					            (random-string 7)
						    "@gmail.com")
					       "pwd")]
		   (when (< user-i how-many-users)
		     (println user-info)
		     ; TODO upload user files
		     (recur (inc user-i)
		            (create-user app-auth-key
			                 (str "U"
					      (random-string 7)
					      "@gmail.com")
					 "pwd"))))
	  	 (loop [action-i 0]
		   (-> (create-action client-auth-key
		                      app-auth-key
				      (str "A" 
				           (random-string 5) 
					   ".lua")
				      standard-script)
		       println)
		   (when (< action-i how-many-actions)
		     (recur (inc action-i))))
		 (recur (inc app-i)))))))
       ["bear@duck.berlin"
        "duck@duck.amsterdam"
	"contato@liberdade.bsb.br"])))

(main)

