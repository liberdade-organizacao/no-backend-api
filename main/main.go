package main

import (
	"errors"
	"fmt"
	"os"
	"github.com/liberdade-organizacao/no-backend-api/model"
)

func main() {
	args := os.Args[1:]

	if len(args) == 0 {
		panic(errors.New("Not enough arguments"))
	}

	db, err := model.NewDatabaseInstance()
	if err != nil {
		panic(err)
	}

	switch op := args[0]; op {
	case "migrate_up":
		db.MigrateUp()
		db.Close()	
	default:
		fmt.Printf("Unknown argument %s\n", op)
	}

	// TODO load database
	// TODO create controller to handle connections
	// TODO start server
}

