package business

import (
	"errors"
	"fmt"
	"github.com/liberdade-organizacao/no-backend-api/model"
	"github.com/liberdade-organizacao/no-backend-api/utils"
)

type Context struct {
	Database *model.Database
}

func (context *Context) Free() error {
	return context.Database.Close()
}

func newClientAuthKey(clientId int, isAdmin bool) string {
	return ""
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

	rows, err := context.Database.Query(query)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var resultId int = -1
	var resultIsAdmin bool = false
	for rows.Next() {
		err = rows.Scan(&resultId, &resultIsAdmin)
		if err != nil {
			return nil, err
		}
	}

	// TODO wrap id and is_admin into auth_key
	
	errMsg := fmt.Sprintf("NOT IMPLEMENTED YET BUT GOT %d, %v", resultId, resultIsAdmin)
	return nil, errors.New(errMsg)
}

// Returns `auth_key`
func (context *Context) AuthClient(email, password string) (map[string]any, error) {
	// TODO complete me!
	return nil, errors.New("not implemented yet")
}

