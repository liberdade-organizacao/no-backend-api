# Generic Backend-as-a-Service

> WORK IN PROGRESS!

Run your own no-backend service!

## Setup

Requirements:
- Leiningen
- PostgreSQL
- Docker (optional for development)

First, ensure the environment variables are properly set up:

```
source resources/.env
```

Check [the example ENV file](./resources/.env.example) for the required
parameters.

Setup the database:

```
lein run migrate-up
```

And finally execute the application:

```
lein run up
```

## Usage

To run tests:

``` sh
lein test
```

