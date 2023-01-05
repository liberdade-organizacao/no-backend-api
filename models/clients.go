package models

import (
    "errors"
    "encoding/json"
    "liberdade.bsb.br/baas/api/common"
)

// Generates a new auth key
func (context *Context) NewAuthKey(email string, isAdmin bool) (string, error)  {
    salt := context.Config["server_salt"]
    isAdminValue := "off"

    if isAdmin {
	isAdminValue = "on"
    }
    authKeyPayload := map[string]string {
	"email": email,
	"is_admin": isAdminValue,
    }
    authKeyBytes, err := json.Marshal(authKeyPayload)
    if err != nil {
	return "", err
    }
    authKeyJson := string(authKeyBytes)

    authKey, err := common.Encode(authKeyJson, salt)

    return authKey, err
}

// Loads an auth key into a map relating the keys "email" and "is_admin" to their respective values
func (context *Context) ParseAuthKey(authKey string) (map[string]string, error) {
    return nil, errors.New("Not implemented yet!")
}

// Creates a new client account
func (context *Context) CreateClientAccount(email string, password string, isAdmin bool) (string, error) {
    salt := context.Config["server_salt"]     
    isAdminToStore := "off"
    if isAdmin {
        isAdminToStore = "on"
    }
    authKey, err := context.NewAuthKey(email, isAdmin)
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

