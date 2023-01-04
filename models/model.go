package models

import (
    "liberdade.bsb.br/baas/api/database"
)

type Context struct {
    Config map[string]string
    Connection *database.Conn
}

func NewContext(config map[string]string) *Context {
    host := config["db_host"]
    port := config["db_port"]
    user := config["db_user"]
    password := config["db_password"]
    dbname := config["db_name"]
    sqlFolder := config["db_sql_folder"]
    connection := database.NewDatabase(host, port, user, password, dbname, sqlFolder)
    return &Context {
        Config: config,
        Connection: &connection,
    }
}

func (context *Context) Close() {
    context.Connection.Close()
}

