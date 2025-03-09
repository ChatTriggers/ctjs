#!/bin/bash

#JAVADOCS_ARCHIVE_PATH="/apps/storage/javadocs-archive"
JAVADOCS_ARCHIVE_PATH="../javadocs-archive"
BUILD_DIR="./build/javadocs"

if [[ -d "$JAVADOCS_ARCHIVE_PATH" ]]; then
  cp -r "$JAVADOCS_ARCHIVE_PATH" .
fi

./gradlew dokkaHtml

# Copy build docs back into archive folder
output_dir=$(find "$BUILD_DIR" -mindepth 1 -maxdepth 1 -type d | head -n 1)
cp -r "$output_dir" "$JAVADOCS_ARCHIVE_PATH"

# Move to /www to serve
cp -r "$output_dir"/* "/www"
