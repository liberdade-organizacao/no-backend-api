package main

import (
    "fmt"
    "os"
    "liberdade.bsb.br/baas/api/common"
    "liberdade.bsb.br/baas/api/services"
    "liberdade.bsb.br/baas/api/jobs"
)

func main() {
    args := os.Args[1:]
    config := common.LoadConfig()

    if len(args) == 0 {
        fmt.Printf("Starting server at %s\n", config["server_port"])
        services.StartServer(config)
    } else {
        jobs.SetupDatabase(config)
    }
}

