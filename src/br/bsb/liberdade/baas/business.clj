(ns br.bsb.liberdade.baas.business
  (:require [br.bsb.liberdade.baas.utils :as utils]
            [br.bsb.liberdade.baas.db :as db]))

(def possible-app-roles ["admin" "contributor"])

(defn- pqbool [b]
  (if b "on" "off"))

(defn new-client-auth-key [client-id is-admin]
  (utils/encode-secret {"client_id" client-id
                        "is_admin" is-admin}))

(defn new-app-auth-key [app-id]
  (utils/encode-secret {"app_id" app-id}))

(defn new-user-auth-key [app-id user-id]
  (utils/encode-secret {"user_id" user-id
                        "app_id" app-id}))

(defn new-client [email password is-admin]
  (try
    (let [params {"email" email
                  "password" (utils/hide password)
                  "is_admin" (pqbool is-admin)}
          result (db/run-operation-first "create-client-account.sql" params)]
      {"auth_key" (new-client-auth-key (:id result) (:is_admin result))
       "error" nil})
    (catch org.postgresql.util.PSQLException e 
      {"auth_key" nil
       "error" "Email already exists"})))

(defn auth-client [email password]
  (let [params {"email" email
                "password" (utils/hide password)}
        result (db/run-operation-first "auth-client.sql" params)
        auth-key (when (some? result)
                  (new-client-auth-key (:id result) (:is_admin result)))
        error (when (nil? result)
                "Wrong email or password")]
    {"auth_key" auth-key
     "error" error}))

(defn- insert-app-xf [state]
  (if (some? (:error state))
    state
    (try
      (let [client-id (:client-id state)
            app-name (:app-name state)
            result (db/run-operation-first "create-app.sql"
                                     {"owner_id" client-id
                                      "app_name" app-name})
            app-id (:id result)
            next-state (assoc state :app-id app-id)]
        next-state)
      (catch org.postgresql.util.PSQLException e
        {:error e}))))

(defn- invite-to-app-xf [state]
  (if (some? (:error state))
    state
    (let [app-id (:app-id state)
          owner-id (:client-id state)
          role (:role state)
          result (db/run-operation-first "invite-to-app.sql"
                                   {"app_id" app-id
                                    "client_id" owner-id
                                    "role" role})]
      state)))

(defn- format-new-app-output-xf [state]
  {"error" (:error state)
   "auth_key" (when (some? (:app-id state))
                (new-app-auth-key (:app-id state)))})

(defn new-app [client-auth-key app-name]
  (let [client-info (utils/decode-secret client-auth-key)
        client-id (:client_id client-info)]
    (->> {:error nil 
          :client-id client-id
          :app-name app-name
          :role "admin"}
         insert-app-xf
         invite-to-app-xf
         format-new-app-output-xf)))

(defn get-clients-apps [client-auth-key]
  (let [client-info (utils/decode-secret client-auth-key)
        client-id (:client_id client-info)
	result (db/run-operation "get-clients-apps.sql"
	                         {"client_id" client-id})]
    {"error" nil
     "apps" (map (fn [app]
                   {"auth_key" (-> app :id new-app-auth-key)
		    "name" (:name app)})
                 result)}))

(defn- get-client-role-in-app-xf [state]
  (cond 
    (some? (:error state))
      state
    (nil? (:client-id state))
      {:error "Invalid client"}
    :else
      (let [client-id (:client-id state)
            app-id (:app-id state)
            result (db/run-operation-first "get-client-app-role.sql"
                                           {"client_id" client-id
                                            "app_id" app-id})]
        (assoc state :role (:role result)))))

(defn- maybe-delete-app-xf [state]
  (cond (some? (:error state))
          state
        (not= (:role state) "admin")
          (assoc state :error "User not allowed to delete app")
        :else
          (let [app-id (:app-id state)
                result (db/run-operation-first "delete-app.sql"
                                               {"app_id" app-id})]
            state)))

(defn format-delete-app-output-xf [state]
  {"error" (get state :error nil)})

(defn delete-app [client-auth-key app-auth-key]
  (let [client-info (utils/decode-secret client-auth-key)
        client-id (:client_id client-info)
        app-info (utils/decode-secret app-auth-key)
        app-id (:app_id app-info)]
    (->> {:error nil
          :client-id client-id
          :app-id app-id}
         get-client-role-in-app-xf
         maybe-delete-app-xf
         format-delete-app-output-xf)))

(defn- validate-inviter-role-xf [state]
  (if (not= "admin" (:role state))
    {:error "Inviter has no rights to do this"}
    state))

(defn- get-invitee-client-id-by-email-xf [state]
  (if (some? (:error state))
    state
    (let [result (db/run-operation-first "get-client-by-email.sql"
                                         {"email" (:invitee-email state)})
          client-id (get result :id nil)]
      (if (nil? client-id)
        {:error "Invitee email not found"}
        (assoc state :client-id client-id
                     :role (:invitee-role state))))))

(defn- format-invite-to-app-output-xf [state]
  {"error" (:error state)})

(defn invite-to-app-by-email 
  [inviter-auth-key app-auth-key invitee-email invitee-role]
  (let [inviter-info (utils/decode-secret inviter-auth-key)
        inviter-id (:client_id inviter-info)
        app-info (utils/decode-secret app-auth-key)
        app-id (:app_id app-info)]
    (->> {:error nil
          :client-id inviter-id
          :app-id app-id
          :invitee-email invitee-email
          :invitee-role invitee-role}
         get-client-role-in-app-xf
         validate-inviter-role-xf
         get-invitee-client-id-by-email-xf
         invite-to-app-xf
         format-invite-to-app-output-xf)))

(defn change-client-password [auth-key old-password new-password]
  (let [client-info (utils/decode-secret auth-key)
        client-id (:client_id client-info)
        result (db/run-operation "change-client-password.sql"
                                 {"client_id" client-id
                                  "old_password" (utils/hide old-password)
                                  "new_password" (utils/hide new-password)})]
    {"error" (if (pos? (count result))
               nil
               "Failed to change password")}))

(defn delete-client [auth-key password]
  (let [client-info (utils/decode-secret auth-key)
        client-id (:client_id client-info)
        result (db/run-operation "delete-client.sql"
                                 {"id" client-id
                                  "password" (utils/hide password)})]
    {"error" (if (pos? (count result))
               nil
               "Failed to delete account")}))

(defn new-user [app-auth-key email password]
  (let [app-info (utils/decode-secret app-auth-key)
        app-id (get app-info :app_id nil)
	params {"app_id" app-id
	        "email" email
		"password" (utils/hide password)}
	result (db/run-operation-first "create-user.sql" params)
	user-id (get result :id)]
    {"error" (when (nil? user-id) "Could not create user")
     "auth_key" (when (some? user-id)
                  (new-user-auth-key app-id user-id))}))

(defn auth-user [app-auth-key email password]
  (let [app-id (-> app-auth-key utils/decode-secret :app_id)
        params {"app_id" app-id
                "email" email
                "password" (utils/hide password)}
        result (db/run-operation-first "auth-user.sql" params)
        user-id (:id result)]
    {"error" (when (nil? user-id)
               "Could not authorize user")
     "auth_key" (when (some? user-id)
                  (new-user-auth-key app-id user-id))}))

(defn delete-user [user-auth-key password]
  (let [user-info (utils/decode-secret user-auth-key)
        app-id (:app_id user-info)
	user-id (:user_id user-info)
	params {"user_id" user-id
	        "password" (utils/hide password)}
	result (db/run-operation "delete-user.sql" params)]
    {"error" (when (= 0 (count result))
               "Failed to delete user")}))

(defn update-user-password [user-auth-key old-password new-password]
  (let [user-info (utils/decode-secret user-auth-key)
        user-id (:user_id user-info)
	      app-id (:app_id user-info)
        params {"user_id" user-id
                "app_id" app-id
                "old_password" (utils/hide old-password)
                "new_password" (utils/hide new-password)}
        result (db/run-operation "change-user-password.sql" params)]
    {"error" (if (-> result count pos?)
               nil
               "Failed to change password")}))

(defn- is-role-invalid? [state]
  (not (utils/in? possible-app-roles (:role state))))

(defn- list-app-users-xf [state]
  (cond
    (-> state :error some?)
      state
    (is-role-invalid? state)
      (assoc state :error "Not enough permissions")
    :else
      (assoc state :users (db/run-operation "list-app-users.sql"
                                            {"app_id" (:app-id state)}))))

(defn- format-list-app-users-output-xf [state]
  {"error" (get state :error nil)
   "users" (get state :users nil)})

(defn list-app-users [client-auth-key app-auth-key]
  (->> {:error nil
        :client-id (-> client-auth-key
                       utils/decode-secret
                       :client_id)
        :app-id (-> app-auth-key
                    utils/decode-secret
                    :app_id)}
       get-client-role-in-app-xf
       list-app-users-xf
       format-list-app-users-output-xf))

(defn- new-file-path [app-id user-id filename]
  (str "a" app-id "/u" user-id "/" filename))

(defn upload-user-file [user-auth-key filename contents]
  (let [user-info (utils/decode-secret user-auth-key)
        app-id (:app_id user-info)
        user-id (:user_id user-info)
        filepath (new-file-path app-id user-id filename)
        params {"app_id" app-id
                "user_id" user-id
                "filename" filename
                "filepath" filepath
                "contents" (utils/encode-data contents)}
        result (db/run-operation "upload-file.sql" params)]
    {"error" (when (= 0 (count result)) "Failed to upload file")}))

(defn download-user-file [user-auth-key filename]
  (let [user-info (utils/decode-secret user-auth-key)
        app-id (:app_id user-info)
        user-id (:user_id user-info)
        filepath (new-file-path app-id user-id filename)
        params {"filepath" filepath}
        result (db/run-operation-first "download-file.sql" params)
        contents (:contents result)]
    (if (some? contents)
      (utils/decode-data contents)
      nil)))

(defn list-user-files [user-auth-key]
  (let [user-info (utils/decode-secret user-auth-key)
        app-id (:app_id user-info)
        user-id (:user_id user-info)
        params {"app_id" app-id
                "user_id" user-id}
        result (db/run-operation "list-user-files.sql" params)]
    result))

(defn delete-user-file [user-auth-key filename]
  (let [user-info (utils/decode-secret user-auth-key)
        app-id (:app_id user-info)
        user-id (:user_id user-info)
        filepath (new-file-path app-id user-id filename)
        params {"filepath" filepath}
        result (db/run-operation "delete-file.sql" params)]
    {"error" (if (pos? (count result))
               nil
               "Failed to delete this file")}))

(defn- maybe-list-app-files-xf [state]
  (cond (some? (:error state)) 
          state
        (is-role-invalid? state) 
	  (assoc state :error "Not enough permissions to do that")
	:else 
	  (let [app-id (:app-id state)
	        user-id (:client-id state)
		params {"app_id" app-id
		        "user_id" user-id}
		result (db/run-operation "list-app-files.sql" params)]
	    (assoc state :files result))))

(defn- format-list-app-files-output-xf [state]
  {"error" (get state :error nil)
   "files" (get state :files [])})

(defn list-app-files [client-auth-key app-auth-key]
  (let [client-info (utils/decode-secret client-auth-key)
        app-info (utils/decode-secret app-auth-key)
        client-id (:client_id client-info)
	app-id (:app_id app-info)]
    (-> {:error nil
         :client-id client-id
	 :app-id app-id}
        get-client-role-in-app-xf
	maybe-list-app-files-xf
	format-list-app-files-output-xf)))

(defn- list-app-managers-xf [state]
  (cond
    (-> state :error some?)
      state
    (is-role-invalid? state)
      (assoc state :error "Not enough permissions")
    :else
      (assoc state 
             :managers 
             (db/run-operation "list-app-managers.sql"
                               {"app_id" (:app-id state)}))))

(defn- format-list-app-managers-output-xf [state]
  {"error" (get state :error nil)
   "clients" (get state :managers nil)})

(defn list-app-managers [client-auth-key app-auth-key]
  (->> {:error nil
        :client-id (-> client-auth-key
                       utils/decode-secret
                       :client_id)
        :app-id (-> app-auth-key
                    utils/decode-secret
                    :app_id)}
       get-client-role-in-app-xf
       list-app-managers-xf
       format-list-app-managers-output-xf))

(defn- maybe-upload-action-xf [state]
  (cond 
    (some? (:error state)) 
      state
    (is-role-invalid? state)
      (assoc state :error "Not enough permissions")
    :else
      (let [app-id (:app-id state)
            action-name (:action-name state)
	    script (:script state)
	    params {"app_id" app-id
	            "name" action-name
		    "script" script}
	    result (db/run-operation "upload-action.sql" params)]
        (assoc state :error (when (= 0 (count result)) "Failed to upload script")))))

(defn- format-standard-xf [state]
  {"error" (get state :error nil)})

(defn upsert-action [client-auth-key app-auth-key action-name script]
  (let [client-id (-> client-auth-key utils/decode-secret :client_id)
	app-id (-> app-auth-key utils/decode-secret :app_id)]
    (-> {:client-id client-id
         :app-id app-id
	 :action-name action-name
	 :script script
	 :error nil}
        get-client-role-in-app-xf
	maybe-upload-action-xf
	format-standard-xf)))

(defn- maybe-download-action-xf [state]
  (if (or (-> state :error some?) (is-role-invalid? state))
    {"error" "failed to download action"}
    (let [app-id (:app-id state)
          action-name (:action-name state)
	  params {"app_id" app-id
	          "name" action-name}
	  result (db/run-operation-first "download-action.sql" params)]
      (:script result))))

(defn read-action [client-auth-key app-auth-key action-name]
  (let [client-id (-> client-auth-key utils/decode-secret :client_id)
        app-id (-> app-auth-key utils/decode-secret :app_id)]
    (-> {:client-id client-id
         :app-id app-id
	 :action-name action-name
	 :error nil}
        get-client-role-in-app-xf
	maybe-download-action-xf)))

(defn- maybe-list-actions-xf [state]
  (if (or (-> state :error some?) (is-role-invalid? state))
    {"error" "failed to download action"}
    (let [app-id (:app-id state)
	  params {"app_id" app-id}
	  result (db/run-operation "list-actions.sql" params)]
      result)))

(defn list-actions [client-auth-key app-auth-key]
  (let [client-id (-> client-auth-key utils/decode-secret :client_id)
        app-id (-> app-auth-key utils/decode-secret :app_id)]
    (-> {:client-id client-id
         :app-id app-id
	 :error nil}
        get-client-role-in-app-xf
	maybe-list-actions-xf)))

(defn- maybe-delete-action-xf [state]
  (cond 
    (some? (:error state)) 
      state
    (is-role-invalid? state)
      (assoc state :error "Not enough permissions")
    :else
      (let [app-id (:app-id state)
            action-name (:action-name state)
	    script (:script state)
	    params {"app_id" app-id
	            "name" action-name}
	    result (db/run-operation "delete-action.sql" params)]
        (assoc state :error (when (= 0 (count result)) "Failed to upload script")))))

(defn delete-action [client-auth-key app-auth-key action-name]
  (let [client-id (-> client-auth-key utils/decode-secret :client_id)
	app-id (-> app-auth-key utils/decode-secret :app_id)]
    (-> {:client-id client-id
         :app-id app-id
	 :action-name action-name
	 :error nil}
        get-client-role-in-app-xf
	maybe-delete-action-xf
	format-standard-xf)))

(defn- is-client-admin-xf [state]
  (if (some? (:error state))
    state
    (let [params {"client_id" (:client_id state)}
          result (db/run-operation-first "is-client-admin.sql" params)]
      (merge state result))))

(defn- maybe-list-all-things-xf [state]
  (cond 
    (-> state :error some?)
      state
    (-> state :is_admin false?)
      (assoc state :error "Not enough permissions")
    :else
      (let [things (:things state)
            operation (str "list-all-" things ".sql")
            result (db/run-operation operation {})]
        (assoc state things result))))

(defn- format-list-all-things-output-xf [state]
  {"error"         (get state :error nil)
   (:things state) (get state (:things state) nil)})

(defn- list-all-things [client-auth-key things]
  (->> {:error nil
        :client_id (-> client-auth-key
	               utils/decode-secret
		       :client_id)
        :things things}
       is-client-admin-xf
       maybe-list-all-things-xf
       format-list-all-things-output-xf))

(defn list-all-clients [client-auth-key]
  (list-all-things client-auth-key "clients"))

(defn list-all-apps [client-auth-key]
  (list-all-things client-auth-key "apps"))

(defn list-all-files [client-auth-key]
  (list-all-things client-auth-key "files"))

(defn list-all-admins [client-auth-key]
  (list-all-things client-auth-key "admins"))

(defn- maybe-promote-to-admin-xf [state]
  (if (-> state :is_admin false?)
    (assoc state :error "Not enough permissions")
    (let [params {"email" (get state :email nil)}
          response (db/run-operation-first "promote-to-admin.sql" params)]
      state)))

(defn- maybe-demote-admin-xf [state]
  (if (-> state :is_admin false?)
    (assoc state :error "Not enough permissions")
    (let [params {"email" (get state :email nil)}
          response (db/run-operation-first "demote-admin.sql" params)]
      state)))

(defn promote-to-admin [client-auth-key email-to-promote]
  (-> {:error nil
       :email email-to-promote
       :client_id (-> client-auth-key utils/decode-secret :client_id)}
      is-client-admin-xf
      maybe-promote-to-admin-xf
      format-standard-xf))

(defn demote-admin [client-auth-key email-to-demote]
  (-> {:error nil
       :email email-to-demote
       :client_id (-> client-auth-key utils/decode-secret :client_id)}
      is-client-admin-xf
      maybe-demote-admin-xf
      format-standard-xf))

