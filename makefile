API_PORT=7780

.PHONY: default
default: build

.PHONY: test
test:
	rm -f db/db.sqlite
	touch db/db.sqlite
	go test ./model/*.go
	go test ./controller/*.go
	go test ./utils/*.go
	go test ./business/*.go

.PHONY: integration-test
integration-test:
	echo "COMPLETE ME!"

.PHONY: build
build: test
	go build -tags "sqlite_foreign_keys" -o main.exe main/main.go

.PHONY: install
install: build
	echo "Complete me! Run the jarfile"

.PHONY: run
run:
	echo "COMPLETE ME!"

.PHONY: clean
clean:
	echo "COMPLETE ME!"

.PHONY: migrate_up
migrate_up:
	echo "COMPLETE ME!"

.PHONY: migrate_down
migrate_down:
	echo "COMPLETE ME!"

