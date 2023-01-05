.PHONY: default
default: build

.PHONY: psql
psql:
	psql -h localhost -p 5434 -d baas -U liberdade -W

.PHONY: test
test:
	lein test

.PHONY: build
build: test
	lein uberjar

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
