package jobs

import (
    "liberdade.bsb.br/baas/api/common"
    "liberdade.bsb.br/baas/api/database"
)

// Setup migrations table
func SetupDatabase(config map[string]string) {
    host := config["host"]
    port := config["port"]
    user := config["user"]
    password := config["password"]
    dbname := config["dbname"]
    connection := database.NewDatabase(host, port, user, password, dbname)

    setupDatabaseSql := common.ReadFile("./resources/sql/setup_database.sql")

    _, err := connection.Query(setupDatabaseSql)
    if err != nil {
        panic(err)
    }
}

