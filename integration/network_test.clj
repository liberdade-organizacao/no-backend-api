(ns br.bsb.liberdade.baas.integration-test.network-test
  (:require [babashka.curl :as curl]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]))

(def service-url "http://localhost:7780")
(def psql-params ["-h" "localhost" "-p" "5434" "-d" "baas" "-U" "liberdade" "-c"])

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

(defn- psql [query]
  (shell/sh "psql" "-h" "localhost" "-p" "5434" "-d" "baas" "-U" "liberdade" "-c" query))

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

(defn- create-user [app-auth-key user-email user-password]
  (let [url (str service-url "/users/signup")
        params {"app_auth_key" app-auth-key
                "email" user-email
                "password" user-password}
        response (curl/post url {:body (json/generate-string params)})
        body (json/parse-string (get response :body))]
    (println "# create user")
    (println body)
    body))

(defn- change-user-password [user-auth-key old-password new-password]
  (let [url (str service-url "/users/password")
        params {"user_auth_key" user-auth-key
                "old_password" old-password
                "new_password" new-password}
        response (curl/post url {:body (json/generate-string params)})
        body (json/parse-string (get response :body))]
    (println "# change user password")
    (println body)
    body))

(defn- login-user [app-auth-key user-email password]
  (let [url (str service-url "/users/login")
        params {"app_auth_key" app-auth-key
                "email" user-email
                "password" password}
        response (curl/post url {:body (json/generate-string params)})
        body (json/parse-string (get response :body))]
    (println "# login user")
    (println body)
    body))

(defn- upload-user-file [user-auth-key filename contents]
  (let [url (str service-url "/users/files")
        headers {"X-USER-AUTH-KEY" user-auth-key
                 "X-FILENAME" filename}
        response (curl/post url {:body contents
                                 :headers headers})
        body (json/parse-string (get response :body))]
    (println "# upload file")
    (println body)
    body))

(defn- download-user-file [user-auth-key filename]
  (let [url (str service-url "/users/files")
        headers {"X-USER-AUTH-KEY" user-auth-key
                 "X-FILENAME" filename}
        response (curl/get url {:headers headers})
        body (:body response)]
    (println "# download file")
    body))

(defn list-user-files [user-auth-key]
  (let [url (str service-url "/users/files/list")
        headers {"X-USER-AUTH-KEY" user-auth-key}
	response (curl/get url {:headers headers})
	body (-> response :body json/parse-string)]
    (println "# list files")
    (println body)
    body))

(defn list-app-users [client-auth-key app-auth-key]
  (let [url (str service-url "/apps/users")
        params {"client_auth_key" client-auth-key
	        "app_auth_key" app-auth-key}
	response (curl/get url {:query-params params})
	body (-> response :body json/parse-string)]
    (println "# list app users")
    (println body)
    body))

(defn delete-user-file [user-auth-key filename]
  (let [url (str service-url "/users/files")
        headers {"X-USER-AUTH-KEY" user-auth-key
                 "X-FILENAME" filename}
        response (curl/delete url {:headers headers})
        body (:body response)]
    (println "# delete file")
    (println body)
    body))

(defn upload-action [client-auth-key app-auth-key action-name action-contents]
  (let [url (str service-url "/actions")
        params {"client_auth_key" client-auth-key
	        "app_auth_key" app-auth-key
		"action_name" action-name
		"action_script" action-contents}
	response (curl/post url {:body (json/generate-string params)})
	body (json/parse-string (get response :body))]
    (println "# upload action")
    (println body)
    body))

(defn list-actions [client-auth-key app-auth-key]
  (let [url (str service-url "/actions/list")
        query-params {"client_auth_key" client-auth-key
                      "app_auth_key" app-auth-key}
        response (curl/get url {:query-params query-params})
        body (json/parse-string (get response :body))]
    (println "# list actions")
    (println body)
    body))

(defn run-action [user-auth-key app-auth-key action-name action-param]
  (let [url (str service-url "/actions/run")
        params {"user_auth_key" user-auth-key
                "app_auth_key" app-auth-key
                "action_name" action-name
                "action_param" action-param}
        response (curl/post url {:body (json/generate-string params)})
        body (-> response (get :body) json/parse-string)]
    (println "# run action")
    (println body)
    body))

(defn create-admin [email password]
  (do
    (println "# create admin #")
    (create-client-account email password)
    (println (psql (str "UPDATE clients " 
                         "SET is_admin='on' "
		         "WHERE email='" email "';")))
    (auth-client email password)))

(defn list-all-clients [admin-auth-key]
  (println "# listing all clients")
  (let [url (str service-url "/clients/all")
        headers {"X-CLIENT-AUTH-KEY" admin-auth-key}
	response (curl/get url {:headers headers})
	body (-> response :body json/parse-string)]
    (println body)
    body))

(defn list-all-apps [admin-auth-key]
  (println "# listing all apps")
  (let [url (str service-url "/apps/all")
        headers {"X-CLIENT-AUTH-KEY" admin-auth-key}
	response (curl/get url {:headers headers})
	body (-> response :body json/parse-string)]
    (println body)
    body))

(defn list-all-files [admin-auth-key]
  (println "# listing all clients")
  (let [url (str service-url "/files/all")
        headers {"X-CLIENT-AUTH-KEY" admin-auth-key}
	response (curl/get url {:headers headers})
	body (-> response :body json/parse-string)]
    (println body)
    body))
    
(defn tar [compressed-file-name files]
  (println "# compressing file")
  (apply shell/sh (concat ["tar" "-czvf" compressed-file-name]
                          files)))

(defn rm [f]
  (println "# deleting file")
  (shell/sh "rm" f))

(defn upload-compressed-actions 
  [client-auth-key app-auth-key compressed-file-name]
  (println "# uploading compressed actions file")
  (let [url (str service-url "/actions/bulk")
        headers {"X-CLIENT-AUTH-KEY" client-auth-key
	         "X-APP-AUTH-KEY" app-auth-key}
        contents (-> compressed-file-name io/file)
        response (curl/post url {:headers headers
	                         :body contents})
	body (-> response :body json/parse-string)]
    (println body)
    body))

(defn- main []
  (let [email (str "c" (random-string 6) "@liberdade.bsb.br")
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
            user-email (str "u" (random-string 7) "@hotmail.com") 
            first-user-password (random-string 7)
            user-password (random-string 9)
            user-auth-key (-> (create-user app-auth-key 
                                           user-email 
                                           first-user-password)
                              (get "auth_key"))
            ; users can change passwords
            _ (change-user-password user-auth-key 
                                    first-user-password
                                    user-password)
            another-user-auth-key (-> (login-user app-auth-key
                                                  user-email
                                                  user-password)
                                      (get "auth_key"))
            _ (println "are user auth keys equal? " 
                       (= user-auth-key another-user-auth-key))
            ; test if files can be uploaded and downloaded correctly
            filename-cloud "player.png"
            filename-local "./honey.png"
            file-contents (slurp filename-local)
            _ (upload-user-file user-auth-key 
                                filename-cloud
                                (io/file filename-local))
            downloaded-contents (download-user-file user-auth-key 
                                                    filename-cloud)
            _ (println (str "are files equal? " 
                            (= file-contents downloaded-contents)))
            filename-local "./lilly.png"
            file-contents (slurp filename-local)
            _ (upload-user-file user-auth-key 
                                filename-cloud
                                (io/file filename-local))
            downloaded-contents (download-user-file user-auth-key 
                                                    filename-cloud)
            _ (println (str "are files equal? " 
                            (= file-contents downloaded-contents)))
            _ (list-user-files user-auth-key)
            _ (delete-user-file user-auth-key filename-cloud)
            _ (list-user-files user-auth-key)
	    _ (list-app-users auth-key app-auth-key)
	    ; test if actions can be correctly proxied
            action-name "integration_test.lua"
            action-contents (slurp "./integration_test.lua")
            _ (upload-action auth-key 
                             app-auth-key 
                             action-name 
                             action-contents)
            _ (list-actions auth-key
                            app-auth-key)
            _ (run-action user-auth-key 
                          app-auth-key 
                          action-name 
                          "Marceline")
            ; uploading file through action
	    action-name "upload-file.lua"
	    action-contents (slurp "./upload-file.lua")
	    filename "crush_name.txt"
	    contents "Princess Bubblegum"
	    _ (upload-action auth-key
	                     app-auth-key
			     action-name
			     action-contents)
	    _ (run-action user-auth-key 
	                  app-auth-key 
			  action-name 
			  (str "filename=" filename "&contents=" contents))
	    download-contents (download-user-file user-auth-key filename)
	    _ (println download-contents)
	    ; downloading file through action
            action-name "download-file.lua"
	    action-contents (slurp "./download-file.lua")
	    filename "core-driver.txt"
            contents "BMO means 'be more'"
            _ (upload-user-file user-auth-key 
                                filename
                                contents)
	    _ (upload-action auth-key
	                     app-auth-key
			     action-name
			     action-contents)
	    response (run-action user-auth-key 
	                         app-auth-key 
                                 action-name
				 (str "filename=" filename))
	    downloaded-contents (get response "result" nil)
	    _ (println (str "are downloaded contents equal? " (= contents downloaded-contents)))

            ; compressed file upload test
	    compressed-file-name "./action.tar.gz"
	    _ (println "# before compressed upload #")
	    _ (list-actions auth-key app-auth-key)
	    _ (println "# after compressed upload #")
	    _ (tar compressed-file-name ["integration_test.lua" "upload-file.lua"])
	    _ (upload-compressed-actions auth-key app-auth-key compressed-file-name)
	    _ (list-actions auth-key app-auth-key)
	    _ (rm compressed-file-name)

            ; admin tests
	    result (create-admin "crisjr@pm.me" "qotsa")
	    admin-auth-key (get result "auth_key" nil)
	    _ (list-all-clients admin-auth-key)
	    _ (list-all-apps admin-auth-key)
	    _ (list-all-files admin-auth-key)

	    ; strip down
            _ (delete-app auth-key app-auth-key)
            _ (list-apps auth-key)]
        nil))
    (println "...")))

(main)

