# Generic Backend-as-a-Service

Run your own no-backend service!

## Setup

Requirements:
- Leiningen
- Docker (optional for development)
- [cljfmt](https://github.com/weavejester/cljfmt) (optional for development)

The following repositories are expected to work with this one as well:
- [Scripting Engine](https://github.com/liberdade-organizacao/no-backend-scripting-engine)
- [No-Backend Web Interface](https://github.com/liberdade-organizacao/no-backend-web)

First, ensure the environment variables are properly set up:

```
cp .env.example .env
source .env
```

Check [the example ENV file](./.env.example) for the required parameters.

Setup the database:

```
lein run migrate-up
```

And finally execute the application:

```
lein run up
```

The whole system can be run with Docker Compose:

```
lein uberjar
docker-compose up
```

## Usage

To run tests:

``` sh
lein test
```

