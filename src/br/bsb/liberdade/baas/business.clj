(ns br.bsb.liberdade.baas.business
  (:require [br.bsb.liberdade.baas.utils :as utils])
  (:import Baas.BusinessBridge))

;; All business logic lives in the Frege module src-frege/Baas/Business.fr.
;; BusinessBridge is a Java adapter compiled from that module that provides
;; plain uncurried static methods callable from Clojure.
;;
;; The Frege module returns java.lang.Object values that are actually
;; clojure.lang.PersistentHashMap instances, so no conversion is needed —
;; they can be returned to callers as-is.

;; ── Client account ──────────────────────────────────────────────────────────

(defn new-client-auth-key [client-id is-admin]
  ;; Exposed for tests / api layer that build tokens directly.
  (BusinessBridge/newClientAuthKey (str client-id) (str is-admin)))

(defn new-app-auth-key [app-id]
  (BusinessBridge/newAppAuthKey (str app-id)))

(defn new-user-auth-key [app-id user-id]
  (BusinessBridge/newUserAuthKey (str app-id) (str user-id)))

(defn new-client [email password is-admin]
  (BusinessBridge/newClient email password (if is-admin "on" "off")))

(defn auth-client [email password]
  (BusinessBridge/authClient email password))

(defn change-client-password [auth-key old-password new-password]
  (BusinessBridge/changeClientPassword auth-key old-password new-password))

(defn delete-client [auth-key password]
  (BusinessBridge/deleteClient auth-key password))

;; ── App management ───────────────────────────────────────────────────────────

(defn new-app [client-auth-key app-name]
  (BusinessBridge/newApp client-auth-key app-name))

(defn get-clients-apps [client-auth-key]
  (BusinessBridge/getClientsApps client-auth-key))

(defn delete-app [client-auth-key app-auth-key]
  (BusinessBridge/deleteApp client-auth-key app-auth-key))

(defn invite-to-app-by-email [inviter-auth-key app-auth-key invitee-email invitee-role]
  (BusinessBridge/inviteToAppByEmail inviter-auth-key app-auth-key
                                     invitee-email invitee-role))

(defn revoke-from-app-by-email [revoker-auth-key app-auth-key revokee-email]
  (BusinessBridge/revokeFromAppByEmail revoker-auth-key app-auth-key revokee-email))

(defn list-app-users [client-auth-key app-auth-key]
  (BusinessBridge/listAppUsers client-auth-key app-auth-key))

(defn list-app-managers [client-auth-key app-auth-key]
  (BusinessBridge/listAppManagers client-auth-key app-auth-key))

(defn revoke-admin-access [client-auth-key app-auth-key email-to-revoke]
  (BusinessBridge/revokeAdminAccess client-auth-key app-auth-key email-to-revoke))

(defn get-client-role-in-app [client-auth-key app-auth-key]
  (let [client-id (str (-> client-auth-key utils/decode-secret :client_id))
        app-id    (str (-> app-auth-key utils/decode-secret :app_id))
        role      (BusinessBridge/getClientRoleInApp client-id app-id)]
    {:role role}))

;; ── User account ─────────────────────────────────────────────────────────────

(defn new-user [app-auth-key email password]
  (BusinessBridge/newUser app-auth-key email password))

(defn auth-user [app-auth-key email password]
  (BusinessBridge/authUser app-auth-key email password))

(defn delete-user [user-auth-key password]
  (BusinessBridge/deleteUser user-auth-key password))

(defn update-user-password [user-auth-key old-password new-password]
  (BusinessBridge/updateUserPassword user-auth-key old-password new-password))

;; ── File operations ──────────────────────────────────────────────────────────

(defn upload-user-file [user-auth-key filename contents]
  (BusinessBridge/uploadUserFile user-auth-key filename contents))

(defn download-user-file [user-auth-key filename]
  (BusinessBridge/downloadUserFile user-auth-key filename))

(defn list-user-files [user-auth-key]
  (BusinessBridge/listUserFiles user-auth-key))

(defn delete-user-file [user-auth-key filename]
  (BusinessBridge/deleteUserFile user-auth-key filename))

(defn upload-app-file [client-auth-key app-auth-key filename contents]
  (BusinessBridge/uploadAppFile client-auth-key app-auth-key filename contents))

(defn download-app-file [client-auth-key app-auth-key filename]
  (BusinessBridge/downloadAppFile client-auth-key app-auth-key filename))

(defn delete-app-file [client-auth-key app-auth-key filename]
  (BusinessBridge/deleteAppFile client-auth-key app-auth-key filename))

(defn list-app-files [client-auth-key app-auth-key]
  (BusinessBridge/listAppFiles client-auth-key app-auth-key))

;; ── Actions (serverless scripts) ─────────────────────────────────────────────

(defn upsert-action [client-auth-key app-auth-key action-name script]
  (BusinessBridge/upsertAction client-auth-key app-auth-key action-name script))

(defn update-action [client-auth-key app-auth-key old-action-name new-action-name script]
  (BusinessBridge/updateAction client-auth-key app-auth-key
                               old-action-name new-action-name script))

(defn delete-action [client-auth-key app-auth-key action-name]
  (BusinessBridge/deleteAction client-auth-key app-auth-key action-name))

(defn read-action [client-auth-key app-auth-key action-name]
  (BusinessBridge/readAction client-auth-key app-auth-key action-name))

(defn list-actions [client-auth-key app-auth-key]
  (BusinessBridge/listActions client-auth-key app-auth-key))

(defn upload-actions [client-auth-key app-auth-key compressed-actions]
  (BusinessBridge/uploadActions client-auth-key app-auth-key compressed-actions))

;; ── Admin ─────────────────────────────────────────────────────────────────────

(defn list-all-clients [client-auth-key]
  (BusinessBridge/listAllClients client-auth-key))

(defn list-all-apps [client-auth-key]
  (BusinessBridge/listAllApps client-auth-key))

(defn list-all-files [client-auth-key]
  (BusinessBridge/listAllFiles client-auth-key))

(defn list-all-admins [client-auth-key]
  (BusinessBridge/listAllAdmins client-auth-key))

(defn promote-to-admin [client-auth-key email-to-promote]
  (BusinessBridge/promoteToAdmin client-auth-key email-to-promote))

(defn demote-admin [client-auth-key email-to-demote]
  (BusinessBridge/demoteAdmin client-auth-key email-to-demote))

(defn check-admin [client-auth-key]
  (BusinessBridge/checkAdmin client-auth-key))
