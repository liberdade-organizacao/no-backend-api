package database

import (
    "database/sql"
    _ "github.com/lib/pq"
    "fmt"
    "liberdade.bsb.br/baas/api/common"
    "strings"
)

// Basic database connection
type Conn struct {
    Connection string
    SqlFolder string
    Database *sql.DB
}

// Creates a new database connection
func NewDatabase(host, port, user, password, dbname, sqlFolder string) Conn {
    connString := fmt.Sprintf("host=%s port=%s user=%s password=%s dbname=%s sslmode=disable", host, port, user, password, dbname)
    db, err := sql.Open("postgres", connString)
    if err != nil {
        panic(err)
    }

    connection := Conn {
        Connection: connString,
        SqlFolder: sqlFolder,
        Database: db,
    }

    return connection
}

// Verifies if the database connection is working properly
func (connection *Conn) CheckDatabase() error {
    return connection.Database.Ping()
}

// Execute a SQL query
func (connection *Conn) Query(query string) (*sql.Rows, error) {
    result, err := connection.Database.Query(query)
    return result, err
}

// Closes the database
func (connection *Conn) Close() {
    connection.Database.Close()
}

// Runs a SQL task
func (connection *Conn) RunSqlOperation(taskFileName string, params ...any) (*sql.Rows, error) {
    opFilePath := fmt.Sprintf("%s/operations/%s.sql", connection.SqlFolder, taskFileName)
    rawSqlQuery := common.ReadFile(opFilePath)
    formattedSqlQuery := fmt.Sprintf(rawSqlQuery, params...)
    return connection.Query(formattedSqlQuery)
}

// Runs a SQL migration down
func (connection *Conn) RunSqlMigrationUp(migrationName string) (*sql.Rows, error) {
    migrationFilePath := fmt.Sprintf("%s/migrations/%s.up.sql", connection.SqlFolder, migrationName)
    sqlQuery := common.ReadFile(migrationFilePath)
    return connection.Query(sqlQuery)
}

// Runs a SQL migration down
func (connection *Conn) RunSqlMigrationDown(migrationName string) (*sql.Rows, error) {
    migrationFilePath := fmt.Sprintf("%s/migrations/%s.down.sql", connection.SqlFolder, migrationName)
    sqlQuery := common.ReadFile(migrationFilePath)
    return connection.Query(sqlQuery)

}

// finds out what is the migration name based on the filename
func parseMigrationName(filename string) string {
    return strings.ReplaceAll(filename, ".up.sql", "")    
}

// Setups the database
func (connection *Conn) SetupDatabase() {
    // creating migrations if necessary
    operationFilePath := fmt.Sprintf("%s/setup_database.sql", connection.SqlFolder)
    sqlQuery := common.ReadFile(operationFilePath)
    _, err := connection.Query(sqlQuery)
    if err != nil {
        panic(err)
    }

    // running all migrations
    migrationsDir := fmt.Sprintf("%s/migrations", connection.SqlFolder)
    files := common.ReadDir(migrationsDir)
    totalFileNumber := len(files)
    migrationsToRun := 0
    migrationFiles := make([]string, totalFileNumber)

    for _, filename := range files {
        validFileName := filename[0] != '.'
        correctExtension := strings.Contains(filename, ".up.sql")    
        if validFileName && correctExtension {
            migrationFiles[migrationsToRun] = filename
            migrationsToRun++
        }
    }    

    // for each migration key, load and run the migration
    addMigrationFileName := fmt.Sprintf("%s/operations/add_migration.sql", connection.SqlFolder)
    rawAddMigrationSql := common.ReadFile(addMigrationFileName)

    for i := 0; i < migrationsToRun; i++ {
        filename := migrationFiles[i]
        migrationName := parseMigrationName(filename)

        _, err := connection.RunSqlMigrationUp(migrationName)
        if err != nil {
            panic(err)
        }
        
        migrationSql := fmt.Sprintf(rawAddMigrationSql, migrationName)
        _, err = connection.Query(migrationSql)
        if err != nil {
            panic(err)
        }
    }
}

// Delete all unnecessary tables from database and remove their 
// respective migrations
func (connection *Conn) DropDatabase() {
    listOpFilePath := fmt.Sprintf("%s/list_migrations.sql", connection.SqlFolder) 
    deleteOpFilePath := fmt.Sprintf("%s/remove_migration.sql", connection.SqlFolder)
    listSql := common.ReadFile(listOpFilePath)
    rawDeleteSql := common.ReadFile(deleteOpFilePath)
    migrationRows, err := connection.Query(listSql)

    if err != nil {
        panic(err)
    }

    for migrationRows.Next() {
        var migrationId int
        var migrationName string
        var migrationDate string

        err = migrationRows.Scan(&migrationId, &migrationName, &migrationDate)
        if err != nil {
            panic(err)
        }
        _, err = connection.RunSqlMigrationDown(migrationName) 
        deleteSql := fmt.Sprintf(rawDeleteSql, migrationId)
        _, err := connection.Query(deleteSql)
        if err != nil {
            panic(err)
        }
    }
}

