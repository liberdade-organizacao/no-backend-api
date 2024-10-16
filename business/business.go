package business

import (
	"errors"
	"github.com/liberdade-organizacao/no-backend-api/model"
)

type Context struct {
	Database *model.Database
}

func (context *Context) NewClient(email, password string) (map[string]any, error) {
	// TODO complete me!
	return nil, errors.New("not implemented yet") 
}

func (context *Context) AuthClient(email, password string) (map[string]any, error) {
	// TODO complete me!
	return nil, errors.New("not implemented yet")
}

