package models

import (
    "testing"
)

func TestClientAccountCreation_HappyCase(t *testing.T) {
    // TODO setup database for this test only
    // TODO destroy database after test is executed
    config := map[string]string {
        "host": "localhost",
        "port": "5434",
        "user": "liberdade",
        "password": "password",
        "dbname": "baas",
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
        "host": "localhost",
        "port": "5434",
        "user": "liberdade",
        "password": "password",
        "dbname": "baas",
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

