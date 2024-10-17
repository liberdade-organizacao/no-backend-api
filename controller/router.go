package controller

import (
	"net/http"
	"fmt"
	"io"
	"encoding/json"
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
		Context: &context,
		Database: db,
		Port: config["port"],
	}

	return &router
}

func (router *Router) Start() {
	http.HandleFunc("/clients/signup", router.HandleSignup)
	http.HandleFunc("/clients/login", router.HandleLogin)

	http.ListenAndServe(router.Port, nil)
}

func ParseJson(inlet []byte) (map[string]any, error) {
	outlet := make(map[string]any)
	err := json.Unmarshal(inlet, &outlet)
	if err != nil {
		return nil, err
	} else {
		return outlet, nil
	}
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
		// TODO set status code to 405
		return
	}

	// read
	defer request.Body.Close()
	bodyBytes, err := io.ReadAll(request.Body)
	if err != nil {
		io.WriteString(responseWriter, `{"error": "bad request"}`)
		// TODO set status code to 400
		return
	}
	body, err := ParseJson(bodyBytes)
	email := body["email"].(string)
	password := body["password"].(string)

	// evaluate
	_, err = router.Context.NewClient(email, password)
	if err != nil {
		response := fmt.Sprintf("{\"error\": \"%s\"}", err)
		io.WriteString(responseWriter, response)
		// TODO set status code to 403
		return
	}

	// print
	// TODO write proper response back
	response := fmt.Sprintf(`{"email": "%s", "count": %d}`, email, len(password))
	io.WriteString(responseWriter, response)
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

