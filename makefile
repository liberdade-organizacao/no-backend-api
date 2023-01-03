.PHONY: default
default: run

test:
	go test ./database
	go test ./services/*.go

build: test
	go build -o main.exe main/main.go

run: build
	./main.exe

