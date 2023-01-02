package database

import (
    "database/sql"
    _ "github.com/lib/pq"
    "fmt"
)

// Basic database connection
type Conn struct {
    Connection string
    Database *sql.DB
}

// Creates a new database connection
func NewDatabase(host string,
                 port int,
                 user string,
                 password string,
                 dbname string) Conn {
    connString := fmt.Sprintf("host=%s port=%d user=%s password=%s dbname=%s sslmode=disable", host, port, user, password, dbname)
    db, err := sql.Open("postgres", connString)
    if err != nil {
        panic(err)
    }

    connection := Conn {
        Connection: connString,
        Database: db,
    }

    return connection
}

// Verifies if the database connection is working properly
func CheckDatabase(connection Conn) error {
    return connection.Database.Ping()
}

