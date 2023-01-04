package models

import (
    "liberdade.bsb.br/baas/api/common"
)

// Creates a new client account
func (context *Context) CreateClientAccount(email string, password string, isAdmin bool) (string, error) {
    salt := context.Config["server_salt"]     
    isAdminToStore := "off"
    if isAdmin {
        isAdminToStore = "on"
    }
    authKey, err := common.Encode(email, salt)
    if err != nil {
        return "", err
    }
    
    _, err = context.Connection.RunSqlOperation(
        "create_client_account", 
        email,
        password,
        salt,
        isAdminToStore,
        authKey,
    )
    if err != nil {
        return "", err
    }

    return authKey, nil
}

// Allows clients to login by trading their email and password (if correct)
// with an auth key
func (context *Context) LoginClient(email string, password string) (string, error) {
    salt := context.Config["server_salt"]     
    rows, err := context.Connection.RunSqlOperation(
        "login_client", 
        email,
        password,
        salt,
    )
    if err != nil {
        return "", err
    }

    var queriedEmail string
    var isAdmin bool
    var authKey string
    rows.Scan(&queriedEmail, &isAdmin, &authKey)
    return authKey, nil
}

