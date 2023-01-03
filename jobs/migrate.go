package jobs

import (
    "fmt"
    "io/ioutil"
    "strings"
    "liberdade.bsb.br/baas/api/common"
    "liberdade.bsb.br/baas/api/database"
)

const (
    MIGRATIONS_FOLDER = "./resources/sql/migrations/"
)

// Create a new connection from a config map
func newConnection(config map[string]string) *database.Conn {
    host := config["host"]
    port := config["port"]
    user := config["user"]
    password := config["password"]
    dbname := config["dbname"]
    connection := database.NewDatabase(host, port, user, password, dbname)
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

// Reads a migration from memory and executes it
func runMigration(filename string, connection *database.Conn) error {
    migration := common.ReadFile(filename)
    _, err := connection.Query(migration)
    return err
}

// Adds migration to migrations table
func addMigration(filename string, connection *database.Conn) error {
    addMigrationSql := common.ReadFile("./resources/sql/tasks/add_migration.sql")
    migrationName := strings.ReplaceAll(filename, ".up.sql", "")
    migration := fmt.Sprintf(addMigrationSql, migrationName)
    _, err := connection.Query(migration)
    return err
}

// Run all migrations
func MigrateUp(config map[string]string) {
    connection := newConnection(config)

    // listing all migration files
    files, err := ioutil.ReadDir(MIGRATIONS_FOLDER)
    if err != nil {
        panic(err)
    }

    // obtaining migration file names
    totalFileNumber := len(files)
    migrationsToRun := 0
    migrationFiles := make([]string, totalFileNumber)

    for _, f := range files {
        filename := f.Name()
        validFileName := filename[0] != '.'
        correctExtension := strings.Contains(filename, ".up.sql")    
        if validFileName && correctExtension {
            migrationFiles[migrationsToRun] = filename
            migrationsToRun++
        }
    }    

    // for each migration key, load and run the migration
    for i := 0; i < migrationsToRun; i++ {
        filename := migrationFiles[i]
        migrationFileName := fmt.Sprintf("%s%s", MIGRATIONS_FOLDER, filename)
        err := runMigration(migrationFileName, connection)
        if err != nil {
            panic(err)
        }
        err = addMigration(filename, connection)
        if err != nil {
            panic(err)
        }
    }
}

// TODO undo last migration
func MigrateDown(config map[string]string) {
    fmt.Println("Not implemented yet!")
}
