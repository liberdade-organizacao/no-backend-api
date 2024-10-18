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

func WriteJson(obj map[string]any) (string, error) {
	bytes, err := json.Marshal(obj)
	if err != nil {
		return "", err
	}
	return string(bytes), nil
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
		responseWriter.WriteHeader(405)
		io.WriteString(responseWriter, `{"error": "invalid method"}`)
		return
	}

	// read
	defer request.Body.Close()
	bodyBytes, err := io.ReadAll(request.Body)
	if err != nil {
		responseWriter.WriteHeader(400)
		io.WriteString(responseWriter, `{"error": "bad request"}`)
		return
	}
	body, err := ParseJson(bodyBytes)
	email := body["email"].(string)
	password := body["password"].(string)

	// evaluate
	_, err = router.Context.NewClient(email, password)
	if err != nil {
		responseWriter.WriteHeader(403)
		response := fmt.Sprintf("{\"error\": \"%s\"}", err)
		io.WriteString(responseWriter, response)
		return
	}

	// print
	// TODO write proper response back
	responseWriter.WriteHeader(200)
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

