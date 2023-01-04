package model

import (
    "liberdade.bsb.br/baas/api/database"
)

type Model struct {
    Config map[string]string
    Connection *database.Conn
}

func NewModel(config map[string]string) *Model {
    host := config["host"]
    port := config["port"]
    user := config["user"]
    password := config["password"]
    dbname := config["dbname"]
    connection := database.NewDatabase(host, port, user, password, dbname)
    return &Model {
        Config: config,
        Connection: &connection,
    }
}

func (m *Model) Close() {
    m.Connection.Close()
}

