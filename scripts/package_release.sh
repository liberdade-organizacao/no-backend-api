#!/bin/bash

# Exit on error
set -e

# Configuration
JAR_NAME="br.bsb.liberdade.baas.api.jar"
JAR_PATH="target/uberjar/$JAR_NAME"
RELEASE_NAME="release.tar.gz"

echo "Building the project with lein uberjar..."
lein uberjar

# Check if jar file exists
if [ ! -f "$JAR_PATH" ]; then
    echo "Error: $JAR_PATH not found. Build failed."
    exit 1
fi

echo "Packing files into $RELEASE_NAME..."
# Create a temporary directory for packing to ensure we only include requested files
# and avoid including the full target directory structure
TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

# Copy the jar file and rename it for simplicity in the tarball
cp "$JAR_PATH" "$TMP_DIR/baas.jar"

# Copy other requested files/directories
cp -r resources "$TMP_DIR/"
cp -r db "$TMP_DIR/"
cp .env.example "$TMP_DIR/"

# Create a placeholder Makefile for the release
cat <<EOF > "$TMP_DIR/makefile"
.PHONY: default
default: build run

.PHONY: build
build:
        java -jar baas.jar migrate-up

.PHONY: run
run:
        java -jar baas.jar up
EOF

# Create the tarball
tar -czf "$RELEASE_NAME" -C "$TMP_DIR" .

echo "Build and packing complete: $RELEASE_NAME"
