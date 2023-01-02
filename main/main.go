package main

import (
    "fmt"
    "liberdade.bsb.br/baas/api/services"
)

func main() {
    port := ":8080"
    fmt.Printf("Starting server at %s\n", port)
    services.StartServer(port)
}
