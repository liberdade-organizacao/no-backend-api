package database

import (
    "database/sql"
    _ "github.com/lib/pq"
    "fmt"
    "liberdade.bsb.br/baas/api/common"
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
func (connection *Conn) RunSqlTask(taskFileName string, params ...any) (*sql.Rows, error) {
    taskFilePath := fmt.Sprintf("%s/tasks/%s.sql", connection.SqlFolder, taskFileName)
    rawSqlQuery := common.ReadFile(taskFilePath)
    formattedSqlQuery := fmt.Sprintf(rawSqlQuery, params...)
    return connection.Query(formattedSqlQuery)
}

