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
	allFiles, err := loadAllFilesInDir(operationsFolder)

	if err != nil {
		return nil, err
	}

	for _, filename := range allFiles {
		filepath := fmt.Sprintf("%s/%s", operationsFolder, filename)
		rawBytes, err := ioutil.ReadFile(filepath)
		if err != nil {
			return nil, err
		}

		operations[filename] = string(rawBytes)
	}

	// wrapping up
	db := Database {
		Connection: connection,
		ResourcesFolder: resourcesFolder,
		Operations: operations,
	}

	return &db, nil
}

func (db *Database) Execute(query string) error {
	_, err := db.Connection.Exec(query)
	if err != nil {
		return err
	}
	return nil
}

func (db *Database) Query(query string) (*sql.Rows, error) {
	rows, err := db.Connection.Query(query)
	if err != nil {
		return nil, err
	}
	return rows, nil
}

func loadAllFilesInDir(dirname string) ([]string, error) {
	allFileInfos, err := ioutil.ReadDir(dirname)
	if err != nil {
		return nil, err
	}

	allFiles := make([]string, len(allFileInfos))
	for i, fileInfo := range allFileInfos {
		allFiles[i] = fileInfo.Name()
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

	for _, filename := range allFiles {
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

		err = db.Execute(string(rawBytes))
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

