.PHONY: default
default: build run clean

.PHONY: build
build:
	gcc main.c -o main

.PHONY: run
run:
	./main migrations output

.PHONY: clean
clean:
	rm main

.PHONY: purge
purge:
	rm output/*

