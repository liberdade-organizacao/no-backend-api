package models

import (
    "liberdade.bsb.br/baas/api/database"
)

type Context struct {
    Config map[string]string
    Connection *database.Conn
}

func NewContext(config map[string]string) *Context {
    host := config["host"]
    port := config["port"]
    user := config["user"]
    password := config["password"]
    dbname := config["dbname"]
    connection := database.NewDatabase(host, port, user, password, dbname)
    return &Context {
        Config: config,
        Connection: &connection,
    }
}

func (context *Context) Close() {
    context.Connection.Close()
}

