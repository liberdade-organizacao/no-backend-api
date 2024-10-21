package business

import (
	"errors"
	"github.com/liberdade-organizacao/no-backend-api/model"
)

type Context struct {
	Database *model.Database
}

func (context *Context) Free() error {
	return context.Database.Close()
}

// TODO complete me!
func (context *Context) NewClient(email, password string, isAdmin bool) (map[string]any, error) {
	err := context.Database.Operations["create-client-account.sql"]
	return nil, errors.New(err) 
}

func (context *Context) AuthClient(email, password string) (map[string]any, error) {
	// TODO complete me!
	return nil, errors.New("not implemented yet")
}

