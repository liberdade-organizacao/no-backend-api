package business

import (
	"errors"
	"github.com/liberdade-organizacao/no-backend-api/model"
	"github.com/liberdade-organizacao/no-backend-api/utils"
)

type Context struct {
	Database *model.Database
}

func (context *Context) Free() error {
	return context.Database.Close()
}

// TODO complete me!
// Returns `auth_key`
func (context *Context) NewClient(email, password string, isAdmin bool) (map[string]any, error) {
	rawSql := context.Database.Operations["create-client-account.sql"]
	params := map[string]any {
		"email": email,
		"password": password,
		"is_admin": isAdmin,
	}
	query, err := utils.Format(rawSql, params) 
	if err != nil {
		return nil, err
	}

	_, err = context.Database.Execute(query)
	if err != nil {
		return nil, err
	}

	// TODO parse results

	return nil, errors.New("NOT IMPLEMENTED YET")
}

// Returns `auth_key`
func (context *Context) AuthClient(email, password string) (map[string]any, error) {
	// TODO complete me!
	return nil, errors.New("not implemented yet")
}

