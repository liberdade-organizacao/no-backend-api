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

func newClientAuthKey(clientId int, isAdmin bool) (string, error) {
	message := map[string]any {
		"client_id": clientId,
		"is_admin": isAdmin,
	}
	authKey, err := utils.EncodeSecret(message)
	if err != nil {
		return "", err
	}
	return authKey, nil
}

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

	authKey, err := newClientAuthKey(resultId, resultIsAdmin)
	if err != nil {
		return nil, err
	}

	outlet := map[string]any {
		"auth_key": authKey,
	}
	
	return outlet, nil
}

// Returns `auth_key`
func (context *Context) AuthClient(email, password string) (map[string]any, error) {
	if email == "" || password == "" {
		return nil, errors.New("invalid email/password combination")
	}

	rawSql := context.Database.Operations["auth-client.sql"]
	params := map[string]any {
		"email": email,
		"password": password,
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
	for rows.Next () {
		err = rows.Scan(&resultId, &resultIsAdmin)
		if err != nil {
			return nil, err
		}
	}

	authKey, err := newClientAuthKey(resultId, resultIsAdmin)
	if err != nil {
		return nil, err
	}

	outlet := map[string]any {
		"auth_key": authKey,
	}

	return outlet, nil
}

