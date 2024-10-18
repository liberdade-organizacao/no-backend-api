package model

import (
	"database/sql"
	"fmt"
	"io/ioutil"
	"slices"
	"sort"
	"strings"
	_ "github.com/mattn/go-sqlite3"
)

type Database struct {
	Connection *sql.DB
	ResourcesFolder string
	Operations map[string]string
}

func NewDatabaseInstance(dbFile, resourcesFolder string) (*Database, error) {
	// setting connection up
	connection, err := sql.Open("sqlite3", dbFile)
	if err != nil {
		return nil, err
	}

	// caching operations
	operationsFolder := fmt.Sprintf("%s/sql/operations", resourcesFolder)
	operations := make(map[string]string)
	allFiles, err := loadAllFilesFromDir(operationsFolder)

	if err != nil {
		return nil, err
	}

	for _, file := range allFiles {
		// TODO cache all SQL used in operations
		filename := file.Name()
		filepath := fmt.Sprintf("%s/%s", operationsFolder, filename)
		rawBytes, err := ioutil.ReadFile(filepath)

		if err != nil {
			return err
		}

		operations[file] = string(rawBytes)
	}

	// wrapping up
	db := Database {
		Connection: connection,
		ResourcesFolder: resourcesFolder,
		Operations: operations,
	}

	return &db, nil
}

func (db *Database) Execute(query string) ([]interface{}, error) {
	// TODO make this work actual queries
	_, err := db.Connection.Exec(query)
	return nil, err
}

func loadAllFilesInDir(dirname string) ([]string, error) {
	allFiles, err := ioutil.ReadDir(dirname)
	if err != nil {
		return nil, err
	}
	return allFiles, nil
}

// for internal use only!
func (db *Database) Migrate(direction string) error {
	// loading all files from migration folder
	dirname := fmt.Sprintf("%s/sql/migrations", db.ResourcesFolder)
	allFiles, err := loadAllFilesInDir(dirname)
	if err != nil {
		return err
	}

	// filtering filenames to get only important ones
	files := make([]string, 0)
	suffix := fmt.Sprintf("%s.sql", direction)

	for _, file := range allFiles {
		filename := file.Name()

		if strings.HasSuffix(filename, suffix) {
			filename = fmt.Sprintf("%s/%s", dirname, filename)
			files = append(files, filename)
		}
	}

	// sorting files by creation date
	sort.Strings(files)
	if direction == "down" {
		slices.Reverse(files)
	}

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

func (db *Database) MigrateUp() error {
	return db.Migrate("up")
}

func (db *Database) MigrateDown() error {
	return db.Migrate("down")
}

func (db *Database) Close() error {
	db.Connection.Close()
	return nil
}

