# Generic Backend-as-a-Service

> WORK IN PROGRESS!

Run your own no-backend service!

## Setup

Requirements:
- Go
- PostgreSQL
- Docker (for development)

First, start the database. For development purposes, you can use `docker`:

``` sh
docker-compose up -d
```

Now, edit the configuration file to match your needs:

``` sh
nano resources/config.json
```

Run the required migrations to prepare the database:

``` sh
make migrate_up
```

Finally, start the application:

``` sh
make run
```

This will run all unit tests, build the executable file, and run the server app.

