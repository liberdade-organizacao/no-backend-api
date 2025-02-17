package controller

import (
	"net/http"
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

func SendResponse(
	request *http.Request,
	responseWriter http.ResponseWriter,
	result map[string]any,
	statusCode int,
) {
	response, _ := WriteJson(result)
	responseWriter.WriteHeader(statusCode)
	io.WriteString(responseWriter, response)
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
		SendResponse(
			request,
			responseWriter,
			map[string]any {
				"error": "invalid method", 
			},
			405,
		)
		return
	}

	// read
	defer request.Body.Close()
	bodyBytes, err := io.ReadAll(request.Body)
	if err != nil {
		SendResponse(
			request,
			responseWriter,
			map[string]any {
				"error": "bad request",
			},
			400,
		)
		return
	}
	body, err := ParseJson(bodyBytes)
	email := body["email"].(string)
	password := body["password"].(string)

	// evaluate
	result, err := router.Context.NewClient(email, password, false)
	if err != nil {
		SendResponse(
			request,
			responseWriter,
			map[string]any {
				"error": err.Error(), 
			},
			403,
		)
		return
	}

	// print
	SendResponse(request, responseWriter, result, 200)
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
		SendResponse(
			request,
			responseWriter,
			map[string]any {
				"error": "invalid method", 
			},
			405,
		)
		return
	}

	// read
	defer request.Body.Close()
	bodyBytes, err := io.ReadAll(request.Body)
	if err != nil {
		SendResponse(
			request,
			responseWriter,
			map[string]any {
				"error": "bad request",
			},
			400,
		)
		return
	}
	body, err := ParseJson(bodyBytes)
	email := body["email"].(string)
	password := body["password"].(string)

	// evaluate
	result, err := router.Context.AuthClient(email, password)
	if err != nil {
		SendResponse(
			request,
			responseWriter,
			map[string]any {
				"error": err.Error(), 
			},
			403,
		)
		return
	}

	// print
	SendResponse(request, responseWriter, result, 200)
	return

}

