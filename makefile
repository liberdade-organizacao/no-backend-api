.PHONY: default
default: run

build:
	go build -o main.exe main/main.go

run: build
	./main.exe
