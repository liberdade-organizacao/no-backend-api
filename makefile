.PHONY: default
default: run

test:
	go test ./database

build: test
	go build -o main.exe main/main.go

run: build
	./main.exe

