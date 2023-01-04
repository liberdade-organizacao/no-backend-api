package models

import (
    "testing"
)

var CONFIG = map[string]string {
    "db_host": "localhost",
    "db_port": "5434",
    "db_user": "liberdade",
    "db_password": "password",
    "db_name": "baas",
    "db_sql_folder": "../resources/sql",
    "server_salt": "01234567890123456789012345678901",
}

func TestClientAccountCreation_HappyCase(t *testing.T) {
    context := NewContext(CONFIG)
    context.Connection.SetupDatabase()
    defer context.Connection.DropDatabase()

    email := "test@example.net"
    password := "examplePassword"
    isAdmin := false

    _, err := context.CreateClientAccount(email, password, isAdmin)
    if err != nil {
        t.Errorf("Could not create client account: %#v\n", err)
        return
    }

    authKey, err := context.LoginClient(email, password)
    if err != nil || authKey == "" {
        t.Errorf("Could not login client: %#v\n", err)
        return
    }
}

func TestClientAccountCreation_BadCases(t *testing.T) {
    context := NewContext(CONFIG)
    context.Connection.SetupDatabase()
    defer context.Connection.DropDatabase()

    email := "another_test@example.net"
    password := "example password"
    wrongPassword := "wrong password"
    isAdmin := false

    _, err := context.CreateClientAccount(email, password, isAdmin)
    if err != nil {
        t.Errorf("Could not create bad client account: %#v\n", err)
        return
    }

    authKey, err := context.LoginClient(email, wrongPassword)
    if err == nil || authKey != "" {
        t.Errorf("Client could login with wrong password: %#v\n", err)
        return
    }

    _, err = context.CreateClientAccount(email, wrongPassword, isAdmin)
    if err == nil {
        t.Errorf("Client should not have been created")
        return
    }
}

