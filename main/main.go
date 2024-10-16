package main

import (
	"errors"
	"fmt"
	"os"
	"github.com/liberdade-organizacao/no-backend-api/controller"
	"github.com/liberdade-organizacao/no-backend-api/model"
)

func main() {
	args := os.Args[1:]

	if len(args) == 0 {
		panic(errors.New("Not enough arguments"))
	}

	db, err := model.NewDatabaseInstance("./db/db.sqlite", "./resources")
	if err != nil {
		panic(err)
	}

	switch op := args[0]; op {
	case "migrate_up":
		db.MigrateUp()
		db.Close()
	case "up":
		// TODO load this from the environment
		config := make(map[string]string)
		config["port"] = ":7780"
		router := controller.NewRouter(config, db)
		router.Start()
	default:
		fmt.Printf("Unknown argument %s\n", op)
	}
}

