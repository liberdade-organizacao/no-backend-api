package database

import (
    "testing"
)

func TestDatabasePing(t *testing.T) {
    // using the defaults from docker compose
    host := "localhost"
    port := "5434"
    user := "liberdade"
    password := "password"
    dbname := "baas"
    sqlFolder := "../resources/sql"
    connection := NewDatabase(host, port, user, password, dbname, sqlFolder)
    err := connection.CheckDatabase()
    if err != nil {
        t.Errorf("Database connection is not working: %#v\n", err)
    }
}

