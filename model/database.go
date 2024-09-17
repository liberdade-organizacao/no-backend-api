package model

import (
	"database/sql"
	_ "github.com/mattn/go-sqlite3"
)

type Database struct {
	Connection *sql.DB
}

func NewDatabaseInstance() (*Database, error) {
	connection, err := sql.Open("sqlite3", "./db/db.sqlite")

	if err != nil {
		return nil, err
	}

	db := Database {
		Connection: connection,
	}

	return &db, nil
}

func (db *Database) MigrateUp() {
	// TODO list files for migration
	// TODO for each file, run migration
}

func (db *Database) Close() {
	db.Connection.Close()
}

