API_PORT=7780

.PHONY: default
default: build

.PHONY: test
test:
	rm -f db/db.sqlite
	touch db/db.sqlite
	go test ./model/*.go
	go test ./controller/*.go

.PHONY: integration-test
integration-test:
	lein run migrate-up
	lein run up &
	cd integration
	bb network_test.clj
	cd ..
	# fuser -k $(API_PORT)/tcp
	lsof -i tcp:$(API_PORT) | grep -v PID | awk '{print $$2}' | xargs kill

.PHONY: build
build: test
	go build -tags "sqlite_foreign_keys" -o main.exe main/main.go

.PHONY: install
install: build
	echo "Complete me! Run the jarfile"

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
	pg_dump -h localhost -p 5434 -d baas -U liberdade -W >> backup.sql

.PHONY: import_database
import_database:
	psql -h localhost -p 5434 -d baas -U liberdade -W < backup.sql

.PHONY: file_size_job
file_size_job:
	gforth scripts/file_size.fs -e bye < files.rec

.PHONY: lint
lint:
	cljfmt fix

