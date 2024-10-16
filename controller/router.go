package controller

import (
	"net/http"
	"fmt"
	"io"
	"github.com/liberdade-organizacao/no-backend-api/business"
	"github.com/liberdade-organizacao/no-backend-api/model"
)

type Router struct {
	Context  *business.Context
	Database *model.Database
	Port     string
}

func NewRouter(config map[string]string, db *model.Database) *Router {
	context := business.Context {
		Database: db,
	}
	router := Router {
		Context: context,
		Database: db,
		Port: config["port"],
	}

	return &router
}

func (router *Router) Start() {
	http.HandleFunc("/clients/signup", router.HandleSignup)
	http.HandleFunc("/clients/login", router.HandleLogin)

	http.ListenAndServer(router.Port, nil)
}

/* ############ *
 * # HANDLERS # *
 * ############ */

// Handles `sign up` calls.
// POST request
// Expects:
// - email
// - password
func (router *Router) HandleSignup(
	responseWriter http.ResponseWriter, 
	request *http.Request,
) {
	// performing initial validations
	if request.Method != "POST" {
		io.WriteString(responseWriter, `{"error": "invalid method"}`)
		return
	}

	// TODO complete me!
	email := ""
	password := ""
	_, err := router.Context.NewClient(email, password)
	if err != nil {
		response := fmt.Sprintf("{\"error\": \"%s\"}", err)
		io.WriteString(responseWriter, response)
		return
	}
	io.WriteString(responseWriter, `{"result": "OK"}`)
	return
}

// Handles `log in` calls.
// POST request
// Expects:
// - email
// - password
func (router *Router) HandleLogin(
	responseWriter http.ResponseWriter, 
	request *http.Request,
) {
	// performing initial validations
	if request.Method != "POST" {
		io.WriteString(responseWriter, `{"error": "invalid method"}`)
		return
	}

	// TODO complete me!
	io.WriteString(responseWriter, `{"error": "not implemented yet"}`)
	return
}

