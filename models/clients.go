package models

import (
    "errors"
    "liberdade.bsb.br/baas/api/common"
)

// FIXME clients should have their own auth key
// Creates a new client account
func (context *Context) CreateClientAccount(email string, password string, isAdmin bool) (string, error) {
    salt := context.Config["server_salt"]     
    passwordToStore, err := common.Encode(password, salt)
    if err != nil {
        return "", err
    }
    isAdminToStore := "off"
    if isAdmin {
        isAdminToStore = "on"
    }
    
    _, err = context.Connection.RunSqlTask(
        "create_client_account", 
        email,
        passwordToStore,
        isAdminToStore,
    )
    if err != nil {
        return "", err
    }

    return "", nil
}

// TODO complete me!
// Allows clients to login by trading their email and password (if correct)
// with an auth key
func (context *Context) LoginClient(email string, password string) (string, error) {
    return "", errors.New("Not implemented yet!")
}

