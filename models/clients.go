package models

import (
    "errors"
)

// TODO complete me!
// Creates a new client account
func (context *Context) CreateClientAccount(email string, password string, isAdmin bool) (string, error) {
    return "", errors.New("Not implemented yet!")
}

// TODO complete me!
// Allows clients to login by trading their email and password (if correct)
// with an auth key
func (context *Context) LoginClient(email string, password string) (string, error) {
    return "", errors.New("Not implemented yet!")
}

