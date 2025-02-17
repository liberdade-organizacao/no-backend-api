(ns br.bsb.liberdade.baas.business
  (:require [br.bsb.liberdade.baas.utils :as utils]
            [br.bsb.liberdade.baas.tar.decompress :as untar]
            [br.bsb.liberdade.baas.fs :as fs]
            [br.bsb.liberdade.baas.db :as db]))

(def possible-app-roles ["admin" "contributor"])

(defn- pqbool [b]
  (if b "on" "off"))

(defn- is-regular-client? [state]
  (= "off" (:is_admin state)))

(defn new-client-auth-key [client-id is-admin]
  (utils/encode-secret {:client_id client-id
                        :is_admin is-admin}))

(defn new-app-auth-key [app-id]
  (utils/encode-secret {:app_id app-id}))

(defn new-user-auth-key [app-id user-id]
  (utils/encode-secret {:user_id user-id
                        :app_id app-id}))

(defn new-client [email password is-admin]
  (try
    (let [params {"email" email
                  "password" (utils/hide password)
                  "is_admin" (pqbool is-admin)}
          result (db/run-operation-first "create-client-account.sql" params)]
      {"auth_key" (new-client-auth-key (:id result) (:is_admin result))
       "error" nil})
    (catch org.sqlite.SQLiteException e
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
            data {"owner_id" client-id
                  "app_name" app-name}
            result (db/run-operation-first "create-app.sql" data)
            app-id (:id result)
            next-state (assoc state :app-id app-id)]
        next-state)
      (catch org.sqlite.SQLiteException e
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

(defn- format-standard-xf [state]
  {"error" (get state :error nil)})

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

(defn get-client-role-in-app [client-auth-key app-auth-key]
  (get-client-role-in-app-xf {:client-id (-> client-auth-key
                                             utils/decode-secret
                                             :client_id)
                              :app-id (-> app-auth-key
                                          utils/decode-secret
                                          :app_id)}))

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

(defn- validate-admin-role-xf [state]
  (if (not= "admin" (:role state))
    {:error "Client has no rights to do this"}
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
         validate-admin-role-xf
         get-invitee-client-id-by-email-xf
         invite-to-app-xf
         format-invite-to-app-output-xf)))

(defn- revoke-from-app-xf [state]
  (if (-> state :error some?)
    state
    (let [result (db/run-operation "revoke-from-app.sql"
                                   {"revoked_email" (:revoked-email state)
                                    "app_id" (:app-id state)})]
      (if (-> result count pos?)
        state
        (assoc state :error "No one was removed")))))

(defn- format-revoke-from-app-by-email-output-xf [state]
  {"error" (get state :error nil)})

(defn revoke-from-app-by-email
  [revoker-auth-key app-auth-key revokee-email]
  (->> {:error nil
        :client-id (-> revoker-auth-key
                       utils/decode-secret
                       :client_id)
        :app-id (-> app-auth-key
                    utils/decode-secret
                    :app_id)
        :revoked-email revokee-email}
       get-client-role-in-app-xf
       validate-admin-role-xf
       revoke-from-app-xf
       format-revoke-from-app-by-email-output-xf))

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

(defn- validate-role-xf [state]
  (cond
    (-> state :error some?)
    state
    (-> state :role nil?)
    state
    :else
    (assoc state :error (when (is-role-invalid? state) "Invalid role"))))

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

(defn- new-file-path
  ([app-id user-id filename]
   (str "a" app-id "/u" user-id "/" filename))
  ([app-id filename]
   (str "a" app-id "/" filename)))

(defn- save-file-to-filesystem [state]
  (if (-> state :error some?)
    state
    (let [{filepath :filepath
           contents :contents} state
          encoded-contents (utils/encode-data contents)]
      (fs/write-file filepath encoded-contents)
      (assoc state :contents encoded-contents))))

(defn- save-file-to-database [state]
  (if (-> state :error some?)
    {"error" (:error state)}
    (let [app-id (:app-id state)
          user-id (get state :user-id "NULL")
          filename (:filename state)
          filepath (:filepath state)
          file-size (count (:contents state))
          params {"app_id" app-id
                  "user_id" user-id
                  "filename" filename
                  "filepath" filepath
                  "file_size" file-size}
          result (db/run-operation "upload-file.sql" params)]
      {"error" (when (= 0 (count result)) "Failed to upload file")})))

(defn- maybe-upload-file-xf [state]
  (if (-> state :error some?)
    {"error" (:error state)}
    (-> state
        save-file-to-filesystem
        save-file-to-database)))

(defn upload-user-file [user-auth-key filename contents]
  (let [user-info (utils/decode-secret user-auth-key)
        app-id (:app_id user-info)
        user-id (:user_id user-info)]
    (-> {:error nil
         :app-id app-id
         :user-id user-id
         :filename filename
         :filepath (new-file-path app-id user-id filename)
         :contents contents}
        maybe-upload-file-xf)))

(defn- maybe-download-file-xf [state]
  (if (-> state :error some?)
    nil
    (let [contents (fs/read-file (:filepath state))]
      (if (nil? contents)
        nil
        (utils/decode-data contents)))))

(defn download-user-file [user-auth-key filename]
  (let [user-info (utils/decode-secret user-auth-key)
        app-id (:app_id user-info)
        user-id (:user_id user-info)
        filepath (new-file-path app-id user-id filename)]
    (maybe-download-file-xf {:filepath filepath})))

(defn list-user-files [user-auth-key]
  (let [user-info (utils/decode-secret user-auth-key)
        app-id (:app_id user-info)
        user-id (:user_id user-info)
        params {"app_id" app-id
                "user_id" user-id}
        result (db/run-operation "list-user-files.sql" params)]
    result))

(defn- maybe-delete-file-xf [state]
  (if (-> state :error some?)
    state
    (let [filepath (:filepath state)
          params {"filepath" filepath}
          _ (fs/delete-file filepath)
          result (db/run-operation "delete-file.sql" params)]
      (assoc state :error (if (pos? (count result))
                            nil
                            "Failed to delete this file")))))

(defn delete-user-file [user-auth-key filename]
  (let [user-info (utils/decode-secret user-auth-key)
        app-id (:app_id user-info)
        user-id (:user_id user-info)
        filepath (new-file-path app-id user-id filename)]
    (-> {:filepath filepath
         :error nil}
        maybe-delete-file-xf
        format-standard-xf)))

(defn upload-app-file [client-auth-key app-auth-key filename contents]
  (let [client-id (-> client-auth-key utils/decode-secret :client_id)
        app-id (-> app-auth-key utils/decode-secret :app_id)]
    (-> {:client-id client-id
         :app-id app-id
         :filename filename
         :filepath (new-file-path app-id filename)
         :contents contents
         :error nil}
        get-client-role-in-app-xf
        validate-role-xf
        maybe-upload-file-xf)))

(defn download-app-file [client-auth-key app-auth-key filename]
  (let [client-id (-> client-auth-key utils/decode-secret :client_id)
        app-id (-> app-auth-key utils/decode-secret :app_id)]
    (-> {:client-id client-id
         :app-id app-id
         :filepath (new-file-path app-id filename)
         :error nil}
        get-client-role-in-app-xf
        validate-role-xf
        maybe-download-file-xf)))

(defn delete-app-file [client-auth-key app-auth-key filename]
  (let [client-id (-> client-auth-key utils/decode-secret :client_id)
        app-id (-> app-auth-key utils/decode-secret :app_id)]
    (-> {:client-id client-id
         :app-id app-id
         :filepath (new-file-path app-id filename)
         :error nil}
        get-client-role-in-app-xf
        validate-role-xf
        maybe-delete-file-xf
        format-standard-xf)))

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

(defn- maybe-revoke-manager-xf [state]
  (cond
    (-> state :error some?)
    state
    (-> state :role (not= "admin"))
    (assoc state :error "not allowed")
    :else
    (let [params {"app_id" (:app-id state)
                  "revoked_email" (:email-to-revoke state)}
          result (db/run-operation "revoke-manager.sql" params)]
      (assoc state :result result))))

(defn revoke-admin-access [client-auth-key app-auth-key email-to-revoke]
  (->> {:error nil
        :client-id (-> client-auth-key
                       utils/decode-secret
                       :client_id)
        :app-id (-> app-auth-key
                    utils/decode-secret
                    :app_id)
        :email-to-revoke email-to-revoke}
       get-client-role-in-app-xf
       maybe-revoke-manager-xf
       format-standard-xf))

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

(defn- update-action-xf [state]
  (cond
    (some? (:error state))
    state
    (is-role-invalid? state)
    (assoc state :error "Not enough permissions")
    :else
    (let [app-id (:app-id state)
          old-action-name (:old-action-name state)
          new-action-name (:new-action-name state)
          script (:script state)
          params {"app_id" app-id
                  "old_name" old-action-name
                  "new_name" new-action-name
                  "script" script}
          result (db/run-operation "update-action.sql" params)]
      (assoc state :error (when (= 0 (count result)) "Failed to upload script")))))

(defn update-action
  [client-auth-key app-auth-key old-action-name new-action-name script]
  (let [client-id (-> client-auth-key utils/decode-secret :client_id)
        app-id (-> app-auth-key utils/decode-secret :app_id)]
    (-> {:client-id client-id
         :app-id app-id
         :old-action-name old-action-name
         :new-action-name new-action-name
         :script script
         :error nil}
        get-client-role-in-app-xf
        update-action-xf
        format-standard-xf)))

(defn- maybe-extract-actions-xf [state]
  (if (-> state :error some?)
    state
    (assoc state
           :actions
           (-> state :compressed-actions untar/extract))))

(defn- maybe-delete-existing-actions-xf [state]
  (cond
    (-> state :error some?)
    state
    (-> state :actions nil?)
    (assoc state :error "Actions could not be decompressed")
    :else
    (let [params {"app_id" (:app-id state)}
          listing-result (db/run-operation "list-actions.sql" params)
          deletion-result (db/run-operation "delete-all-app-actions.sql"
                                            params)]
      (assoc state
             :error
             (if (not= (count listing-result) (count deletion-result))
               "failed to delete actions"
               nil)))))

(defn- maybe-upload-actions-xf [state]
  (if (-> state :error some?)
    state
    (reduce (fn [s [action-name action-script]]
              (let [params {"app_id" (:app-id s)
                            "name" action-name
                            "script" action-script}
                    result (db/run-operation "upload-action.sql"
                                             params)]
                (if (= 0 (count result))
                  (assoc s :error "Failed to upload some actions")
                  s)))
            state
            (:actions state))))

(defn upload-actions [client-auth-key app-auth-key compressed-actions]
  (-> {:client-id (-> client-auth-key utils/decode-secret :client_id)
       :app-id (-> app-auth-key utils/decode-secret :app_id)
       :compressed-actions compressed-actions
       :error nil}
      get-client-role-in-app-xf
      maybe-extract-actions-xf
      maybe-delete-existing-actions-xf
      maybe-upload-actions-xf
      format-standard-xf))

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
    (is-regular-client? state)
    (assoc state :error "Not enough permissions")
    :else
    (let [things (:things state)
          operation (str "list-all-" things ".sql")
          result (db/run-operation operation {})]
      (assoc state things result))))

(def unwanted-fields [:password])
(defn- maybe-cleanup-fields [things]
  (if (nil? things)
    nil
    (map #(reduce (fn [state [k v]]
                    (if (utils/in? unwanted-fields k)
                      state
                      (assoc state k (str v))))
                  {}
                  %)
         things)))

(defn- format-list-all-things-output-xf [state]
  {"error"         (get state :error nil)
   (:things state) (-> (get state (:things state) nil)
                       maybe-cleanup-fields)})

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
  (if (is-regular-client? state)
    (assoc state :error "Not enough permissions")
    (let [params {"email" (get state :email nil)}
          response (db/run-operation-first "promote-to-admin.sql" params)]
      state)))

(defn- maybe-demote-admin-xf [state]
  (if (is-regular-client? state)
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

(defn- format-check-admin-output-xf [state]
  {"error" (if (is-regular-client? state) "not admin" nil)})

(defn check-admin [client-auth-key]
  (-> {:error nil
       :client_id (-> client-auth-key utils/decode-secret :client_id)}
      is-client-admin-xf
      format-check-admin-output-xf))

