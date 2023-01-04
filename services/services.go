package services

import (
    "net/http"
    "io"
    "liberdade.bsb.br/baas/api/model"
)

/***************
 * HTTP ROUTES *
 ***************/

// Placeholder function until the rest of the API is available
func sayHello(w http.ResponseWriter, r *http.Request) {
    io.WriteString(w, "Hello World!")
}

// Creates a new account
func generateHandleSignup(m *model.Model) {
    return func(w http.ResponseWriter, r *http.Request) {
        io.WriteString(w, "TODO implement me!")
    }
}

// Logins with that account
func generateHandleLogin(m *model.Model) {
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
    m := model.NewModel(config)
    defer m.Close()

    http.HandleFunc("/", sayHello)
    http.HandleFunc("/clients/signup", generateHandleSignup(m))
    http.HandleFunc("/clients/login", generateHandleLogin(m))

    http.ListenAndServe(port, nil)
}

