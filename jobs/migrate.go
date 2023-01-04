package jobs

import (
    "fmt"
    "liberdade.bsb.br/baas/api/common"
    "liberdade.bsb.br/baas/api/database"
)

const (
    // TODO replace this with the config sql folder
    MIGRATIONS_FOLDER = "./resources/sql/migrations/"
)

// Create a new connection from a config map
func newConnection(config map[string]string) *database.Conn {
    host := config["db_host"]
    port := config["db_port"]
    user := config["db_user"]
    password := config["db_password"]
    dbname := config["db_name"]
    sqlFolder := config["db_sql_folder"]
    connection := database.NewDatabase(host, port, user, password, dbname, sqlFolder)
    return &connection
}

// Setup migrations table
func SetupDatabase(config map[string]string) {
    connection := newConnection(config)
    setupDatabaseSql := common.ReadFile("./resources/sql/setup_database.sql")
    _, err := connection.Query(setupDatabaseSql)
    if err != nil {
        panic(err)
    }
}

// Read a migration from memory and executes it
func runMigration(filename string, connection *database.Conn) error {
    migration := common.ReadFile(filename)
    _, err := connection.Query(migration)
    return err
}

// Run all migrations
func MigrateUp(config map[string]string) {
    connection := newConnection(config)
    connection.SetupDatabase()
}

// Undo last migration
func MigrateDown(config map[string]string) {
    connection := newConnection(config)

    // getting last migration
    getLastMigrationSql := common.ReadFile("./resources/sql/operations/get_last_migration.sql")
    rows, err := connection.Query(getLastMigrationSql)
    if err != nil {
        panic(err)
    }
    defer rows.Close()

    var migrationId int
    var migrationName string
    var migrationDate string
    for rows.Next() {
        err := rows.Scan(&migrationId, &migrationName, &migrationDate)
        if err != nil {
            panic(err)
        }
    }

    // running last migration
    migrationFileName := fmt.Sprintf(
        "%s%s.down.sql", 
        MIGRATIONS_FOLDER,
        migrationName,
    )
    err = runMigration(migrationFileName, connection)
    if err != nil {
        panic(err)
    }

    // removing last migration from database
    removeLastMigrationSql := common.ReadFile("./resources/sql/operations/remove_last_migration.sql")
    _, err = connection.Query(removeLastMigrationSql)
    if err != nil {
        panic(err)
    }
}
