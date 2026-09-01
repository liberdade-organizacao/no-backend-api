API_PORT=7780

.PHONY: default
default: build

.PHONY: test
test:
	lein test

.PHONY: build
build: test
	lein uberjar

.PHONY: pack
pack:
	sh scripts/package_release.sh

.PHONY: docker-build
docker-build:  build
	docker build -t baas-api . 

.PHONY: docker-run
docker-run: docker-build
	docker run -p 127.0.0.1:7780:7780 baas-api

.PHONY: docker
docker: docker-run

.PHONY: docker-save
docker-save: docker-build
	docker save -o baas-api.tar baas-api

.PHONY: docker-load
docker-load:
	docker load -i baas-api.tar

.PHONY: run
run:
	lein run up

.PHONY: migrate_up
migrate_up:
	lein run migrate-up

.PHONY: migrate_down
migrate_down:
	lein run migrate-down

.PHONY: export_database
export_database:
	lein run to-recfile backup.rec

.PHONY: import_database
import_database:
	lein run from-recfile backup.rec

.PHONY: file_size_job
file_size_job:
	gforth scripts/file_size.fs -e bye < files.rec

.PHONY: lint
lint:
	cljfmt fix

.PHONY: repl
repl:
	lein repl

.PHONY: outdated
outdated:
	lein ancient check

.PHONY: unused
unused:
	lein unused-deps

