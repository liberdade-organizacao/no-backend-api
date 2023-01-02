package main

import (
    "fmt"
    "os"
    "liberdade.bsb.br/baas/api/services"
    "liberdade.bsb.br/baas/api/jobs"
)

func main() {
    args := os.Args[1:]
    port := ":8080"

    if len(args) == 0 {
        fmt.Printf("Starting server at %s\n", port)
        services.StartServer(port)
    } else {
        jobs.SetupDatabase()
    }
}

