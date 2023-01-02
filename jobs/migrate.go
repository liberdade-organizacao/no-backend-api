package jobs

import (
    "io/ioutil"
    "liberdade.bsb.br/baas/api/database"
)

// Setup migrations table
func SetupDatabase() {
    // TODO load database values from a config file
    host := "localhost"
    port := 5434
    user := "liberdade"
    password := "password"
    dbname := "baas"
    connection := database.NewDatabase(host, port, user, password, dbname)

    setupDatabaseSqlFileName := "./resources/sql/setup_database.sql"
    setupDatabaseSqlBytes, err := ioutil.ReadFile(setupDatabaseSqlFileName)
    if err != nil {
        panic(err)
    }
    setupDatabaseSql := string(setupDatabaseSqlBytes)

    _, err = connection.Query(setupDatabaseSql)
    if err != nil {
        panic(err)
    }
}

