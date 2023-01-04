package models

import (
    "testing"
)

func TestClientAccountCreation_HappyCase(t *testing.T) {
    // TODO setup database for this test only
    // TODO destroy database after test is executed
    config := map[string]string {
        "db_host": "localhost",
        "db_port": "5434",
        "db_user": "liberdade",
        "db_password": "db_password",
        "db_name": "baas",
    }
    context := NewContext(config)
    defer context.Close()

    email := "test@example.net"
    password := "examplePassword"
    isAdmin := false

    _, err := context.CreateClientAccount(email, password, isAdmin)
    if err != nil {
        t.Errorf("Could not create client account: %#v\n", err)
        return
    }

    // TODO verify if auth key is correct
    authKey, err := context.LoginClient(email, password)
    if err != nil || authKey == "" {
        t.Errorf("Could not login client: %#v\n", err)
        return
    }
}

func TestClientAccountCreation_BadCases(t *testing.T) {
    // TODO setup database for this test only
    // TODO destroy database after test is executed
    config := map[string]string {
        "db_host": "localhost",
        "db_port": "5434",
        "db_user": "liberdade",
        "db_password": "db_password",
        "db_name": "baas",
    }
    context := NewContext(config)
    defer context.Close()

    email := "test@example.net"
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
}

