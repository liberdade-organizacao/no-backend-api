package services

import (
    "net/http"
    "io"
    "liberdade.bsb.br/baas/api/models"
)

/***************
 * HTTP ROUTES *
 ***************/

// Placeholder function until the rest of the API is available
func sayHello(w http.ResponseWriter, r *http.Request) {
    io.WriteString(w, "Hello World!")
}

// Creates a new account
func generateHandleSignup(context *models.Context) {
    return func(w http.ResponseWriter, r *http.Request) {
        io.WriteString(w, "TODO implement me!")
    }
}

// Logins with that account
func generateHandleLogin(context *models.Context) {
    return func(w http.ResponseWriter, r *http.Request) {
        io.WriteString(w, "TODO implement me!")
    }
}

/***************
 * ENTRY POINT *
 ***************/

// Registers HTTP handles and starts server
func StartServer(config map[string]string) {
    port := config["server_port"]
    context := model.NewContext(config)
    defer context.Close()

    http.HandleFunc("/", sayHello)
    http.HandleFunc("/clients/signup", generateHandleSignup(context))
    http.HandleFunc("/clients/login", generateHandleLogin(context))

    http.ListenAndServe(port, nil)
}

