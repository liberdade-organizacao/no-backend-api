package Baas;

import frege.baas.Business;
import frege.run7.Func;
import frege.run7.Lazy;
import frege.run7.Thunk;
import frege.runtime.Phantom;
import frege.prelude.PreludeBase;
import frege.prelude.PreludeBase.TMaybe;

/**
 * Java bridge between the Clojure API layer and the Frege-compiled
 * frege.baas.Business module.
 *
 * Frege (run7) compiles IO actions as:
 *   Func$U&lt;Phantom$RealWorld, Result&gt;
 * Running them requires applying Thunk.lazyWorld and calling .call():
 *   Business.someIoFn(args...).apply(Thunk.lazyWorld).call()
 *
 * Arguments annotated Lazy&lt;String&gt; in the generated signature must be
 * wrapped with Thunk.lazy(stringValue).
 *
 * Functions returning Maybe&lt;Object&gt; are unwrapped: Nothing → null,
 * Just(v) → v (callers in business.clj receive null for missing/failed results).
 */
@SuppressWarnings("unchecked")
public class BusinessBridge {

    /** Run an IO action and return its result. */
    private static <T> T run(Func.U<Phantom.RealWorld, T> io) {
        return io.apply(Thunk.lazyWorld).call();
    }

    /** Unwrap TMaybe: Nothing → null, Just(v) → v. */
    private static Object fromMaybe(Object maybeObj) {
        if (maybeObj == null) return null;
        TMaybe<?> maybe = (TMaybe<?>) maybeObj;
        TMaybe.DJust<?> just = maybe.asJust();
        return just == null ? null : just.mem1.call();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // § 0  Auth-key helpers exposed for the Clojure API layer
    // ─────────────────────────────────────────────────────────────────────────

    public static String newClientAuthKey(String clientId, String isAdmin) {
        return run(Business.newClientAuthKey(Thunk.lazy(clientId), Thunk.lazy(isAdmin)));
    }

    public static String newAppAuthKey(String appId) {
        return run(Business.newAppAuthKey(Thunk.lazy(appId)));
    }

    public static String newUserAuthKey(String appId, String userId) {
        return run(Business.newUserAuthKey(Thunk.lazy(appId), Thunk.lazy(userId)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // § 1  Client account
    // Frege sigs: newClient(Lazy<String>, String, Lazy<String>)
    //             authClient(Lazy<String>, String)
    // ─────────────────────────────────────────────────────────────────────────

    public static Object newClient(String email, String password, String isAdmin) {
        return run(Business.newClient(Thunk.lazy(email), password, Thunk.lazy(isAdmin)));
    }

    public static Object authClient(String email, String password) {
        return run(Business.authClient(Thunk.lazy(email), password));
    }

    public static Object changeClientPassword(String authKey, String oldPwd, String newPwd) {
        return run(Business.changeClientPassword(authKey, Thunk.lazy(oldPwd), Thunk.lazy(newPwd)));
    }

    public static Object deleteClient(String authKey, String password) {
        return run(Business.deleteClient(authKey, Thunk.lazy(password)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // § 2  App management
    // ─────────────────────────────────────────────────────────────────────────

    public static Object newApp(String clientAuthKey, String appName) {
        return run(Business.newApp(clientAuthKey, Thunk.lazy(appName)));
    }

    public static Object getClientsApps(String clientAuthKey) {
        return run(Business.getClientsApps(clientAuthKey));
    }

    public static Object deleteApp(String clientAuthKey, String appAuthKey) {
        return run(Business.deleteApp(clientAuthKey, appAuthKey));
    }

    public static Object inviteToAppByEmail(String inviterAuthKey, String appAuthKey,
                                             String inviteeEmail, String inviteeRole) {
        return run(Business.inviteToAppByEmail(
            inviterAuthKey, appAuthKey,
            Thunk.lazy(inviteeEmail), Thunk.lazy(inviteeRole)));
    }

    public static Object revokeFromAppByEmail(String revokerAuthKey, String appAuthKey,
                                               String revokeeEmail) {
        return run(Business.revokeFromAppByEmail(
            revokerAuthKey, appAuthKey, Thunk.lazy(revokeeEmail)));
    }

    public static Object listAppUsers(String clientAuthKey, String appAuthKey) {
        return run(Business.listAppUsers(clientAuthKey, appAuthKey));
    }

    public static Object listAppManagers(String clientAuthKey, String appAuthKey) {
        return run(Business.listAppManagers(clientAuthKey, appAuthKey));
    }

    public static Object revokeAdminAccess(String clientAuthKey, String appAuthKey,
                                            String emailToRevoke) {
        return run(Business.revokeAdminAccess(
            clientAuthKey, appAuthKey, Thunk.lazy(emailToRevoke)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // § 3  User account
    // Frege sigs: newUser(String, Lazy<String>, Lazy<String>)
    //             authUser(String, Lazy<String>, Lazy<String>)
    // ─────────────────────────────────────────────────────────────────────────

    public static Object newUser(String appAuthKey, String email, String password) {
        return run(Business.newUser(appAuthKey, Thunk.lazy(email), Thunk.lazy(password)));
    }

    public static Object authUser(String appAuthKey, String email, String password) {
        return run(Business.authUser(appAuthKey, Thunk.lazy(email), Thunk.lazy(password)));
    }

    public static Object deleteUser(String userAuthKey, String password) {
        return run(Business.deleteUser(userAuthKey, Thunk.lazy(password)));
    }

    public static Object updateUserPassword(String userAuthKey, String oldPwd, String newPwd) {
        return run(Business.updateUserPassword(
            userAuthKey, Thunk.lazy(oldPwd), Thunk.lazy(newPwd)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // § 4  File operations
    // ─────────────────────────────────────────────────────────────────────────

    public static Object uploadUserFile(String userAuthKey, String filename, String contents) {
        return run(Business.uploadUserFile(
            userAuthKey, Thunk.lazy(filename), Thunk.lazy(contents)));
    }

    /** Returns the decoded file contents as a String, or null if not found. */
    public static Object downloadUserFile(String userAuthKey, String filename) {
        Object maybe = run(Business.downloadUserFile(userAuthKey, Thunk.lazy(filename)));
        return fromMaybe(maybe);
    }

    public static Object listUserFiles(String userAuthKey) {
        return run(Business.listUserFiles(userAuthKey));
    }

    public static Object deleteUserFile(String userAuthKey, String filename) {
        return run(Business.deleteUserFile(userAuthKey, Thunk.lazy(filename)));
    }

    public static Object uploadAppFile(String clientAuthKey, String appAuthKey,
                                        String filename, String contents) {
        return run(Business.uploadAppFile(
            clientAuthKey, appAuthKey,
            Thunk.lazy(filename), Thunk.lazy(contents)));
    }

    /** Returns the decoded file contents as a String, or null if not found. */
    public static Object downloadAppFile(String clientAuthKey, String appAuthKey,
                                          String filename) {
        Object maybe = run(Business.downloadAppFile(
            clientAuthKey, appAuthKey, Thunk.lazy(filename)));
        return fromMaybe(maybe);
    }

    public static Object deleteAppFile(String clientAuthKey, String appAuthKey,
                                        String filename) {
        return run(Business.deleteAppFile(
            clientAuthKey, appAuthKey, Thunk.lazy(filename)));
    }

    public static Object listAppFiles(String clientAuthKey, String appAuthKey) {
        return run(Business.listAppFiles(clientAuthKey, appAuthKey));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // § 5  Actions (serverless scripts)
    // ─────────────────────────────────────────────────────────────────────────

    public static Object upsertAction(String clientAuthKey, String appAuthKey,
                                       String actionName, String script) {
        return run(Business.upsertAction(
            clientAuthKey, appAuthKey,
            Thunk.lazy(actionName), Thunk.lazy(script)));
    }

    public static Object updateAction(String clientAuthKey, String appAuthKey,
                                       String oldName, String newName, String script) {
        return run(Business.updateAction(
            clientAuthKey, appAuthKey,
            Thunk.lazy(oldName), Thunk.lazy(newName), Thunk.lazy(script)));
    }

    public static Object deleteAction(String clientAuthKey, String appAuthKey,
                                       String actionName) {
        return run(Business.deleteAction(
            clientAuthKey, appAuthKey, Thunk.lazy(actionName)));
    }

    /** Returns the script String, or null if not found / no access. */
    public static Object readAction(String clientAuthKey, String appAuthKey,
                                     String actionName) {
        Object maybe = run(Business.readAction(
            clientAuthKey, appAuthKey, Thunk.lazy(actionName)));
        return fromMaybe(maybe);
    }

    /** Returns the actions seq, or null on error. */
    public static Object listActions(String clientAuthKey, String appAuthKey) {
        Object maybe = run(Business.listActions(clientAuthKey, appAuthKey));
        return fromMaybe(maybe);
    }

    public static Object uploadActions(String clientAuthKey, String appAuthKey,
                                        Object compressedActions) {
        return run(Business.uploadActions(
            clientAuthKey, appAuthKey, Thunk.lazy(compressedActions)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // § 6  Admin functions
    // ─────────────────────────────────────────────────────────────────────────

    public static Object listAllClients(String clientAuthKey) {
        return run(Business.listAllClients(clientAuthKey));
    }

    public static Object listAllApps(String clientAuthKey) {
        return run(Business.listAllApps(clientAuthKey));
    }

    public static Object listAllFiles(String clientAuthKey) {
        return run(Business.listAllFiles(clientAuthKey));
    }

    public static Object listAllAdmins(String clientAuthKey) {
        return run(Business.listAllAdmins(clientAuthKey));
    }

    public static Object promoteToAdmin(String clientAuthKey, String emailToPromote) {
        return run(Business.promoteToAdmin(clientAuthKey, Thunk.lazy(emailToPromote)));
    }

    public static Object demoteAdmin(String clientAuthKey, String emailToDemote) {
        return run(Business.demoteAdmin(clientAuthKey, Thunk.lazy(emailToDemote)));
    }

    public static Object checkAdmin(String clientAuthKey) {
        return run(Business.checkAdmin(clientAuthKey));
    }

    /** Convert any Number (Integer or Long) to long — used by Frege's seqCount. */
    public static long objToLong(Object o) {
        return ((Number) o).longValue();
    }

    /** Returns the role String ("admin", "user", …), or null if not found. */
    public static String getClientRoleInApp(String clientId, String appId) {
        Object maybe = run(Business.getClientRoleInApp(Thunk.lazy(clientId), Thunk.lazy(appId)));
        Object val = fromMaybe(maybe);
        return val == null ? null : val.toString();
    }
}
