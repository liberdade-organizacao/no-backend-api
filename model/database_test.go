package model

import (
	"testing"
)

func TestDatabaseCanMigrate(t *testing.T) {
	db, err := NewDatabaseInstance("../db/db.sqlite", "../resources")
	if err != nil {
		t.Fatalf("failed to start database: %s\n", err)
		return
	}

	err = db.MigrateUp()
	if err != nil {
		t.Fatalf("Failed to migrate up: %s\n", err)
		return
	}

	err = db.MigrateDown()
	if err != nil {
		t.Fatalf("Failed to migrate down: %s\n", err)
		return
	}
}

