package business

import (
	"testing"
	"github.com/liberdade-organizacao/no-backend-api/model"
)

func setup() *Context {
	db, err := model.NewDatabaseInstance("../db/db.sqlite", "../resources")
	if err != nil {
		panic(err)
	}
	err = db.MigrateUp()
	if err != nil {
		panic(err)
	}
	context := Context {
		Database: db,
	}
	return &context
}

func teardown(context *Context) {
	err := context.Database.MigrateDown()
	if err != nil {
		panic(err)
	}
	context.Database.Close()	
	return
}

func TestHandleClientsAccounts(t *testing.T) {
	context := setup()
	defer teardown(context)

	// signup
	email := "test@example.net"
	password := "password"
	isAdmin := false
	result, err := context.NewClient(email, password, isAdmin)
	if err != nil {
		t.Fatalf("Failed to create client: %s", err)
		return
	}
	firstAuthKey := result["auth_key"].(string)
	if firstAuthKey == "" {
		t.Fatal("Failed to generate auth key after creation")
		return
	}

	// login
	result, err = context.AuthClient(email, password)
	if err != nil {
		t.Fatalf("Failed to authorize client: %s", err)
		return
	}
	secondAuthKey := result["auth_key"].(string)
	if secondAuthKey == "" {
		t.Fatal("Failed to generate auth key after auth")
		return
	}
	if firstAuthKey == secondAuthKey {
		t.Fatal("unsafe auth keys were generated")
		return
	}

	// gracefully fail repeated signup
	result, err = context.NewClient(email, password, !isAdmin)
	if err == nil || result != nil {
		t.Fatalf("created repeated client")
		return
	}

	// gracefully fail login
	result, err = context.AuthClient("bogus@email.com", "random")
	if result != nil || err == nil {
		t.Fatalf("authenticated invalid user")
		return
	}

	result, err = context.AuthClient("", "")
	if result != nil || err == nil {
		t.Fatalf("authenticated empty user")
		return
	}

}

