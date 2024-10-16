package model

import (
	"database/sql"
	"fmt"
	"io/ioutil"
	"sort"
	"strings"
	_ "github.com/mattn/go-sqlite3"
)

type Database struct {
	Connection *sql.DB
	ResourcesFolder string
}

func NewDatabaseInstance(dbFile, resourcesFolder string) (*Database, error) {
	connection, err := sql.Open("sqlite3", dbFile)

	if err != nil {
		return nil, err
	}

	db := Database {
		Connection: connection,
		ResourcesFolder: resourcesFolder,
	}

	return &db, nil
}

func (db *Database) Execute(query string) ([]interface{}, error) {
	// TODO make this work actual queries
	_, err := db.Connection.Exec(query)
	return nil, err
}

func (db *Database) MigrateUp() error {
	// loading all files from migration folder
	dirname := fmt.Sprintf("%s/sql/migrations", db.ResourcesFolder)
	allFiles, err := ioutil.ReadDir(dirname)
	if err != nil {
		return err
	}

	// filtering filenames to get only important ones
	files := make([]string, 0)

	for _, file := range allFiles {
		filename := file.Name()

		if strings.HasSuffix(filename, "up.sql") {
			filename = fmt.Sprintf("%s/%s", dirname, filename)
			files = append(files, filename)
		}
	}

	// sorting files by creation date
	sort.Strings(files)

	// running migrations
	for _, file := range files {
		rawBytes, err := ioutil.ReadFile(file)
		if err != nil {
			return err
		}

		_, err = db.Execute(string(rawBytes))
		if err != nil {
			return err
		}
	}

	return nil
}

func (db *Database) Close() error {
	db.Connection.Close()
	return nil
}

