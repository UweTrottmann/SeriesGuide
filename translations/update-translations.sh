#!/bin/bash

# See the README for details on how directories are named.
# Expects a ZIP archive in the working directory.
# Copies all directories and their contents into app/src/main/res, overwriting any existing files.

set -e

# Find the script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
RES_DIR="$PROJECT_ROOT/app/src/main/res"

# Find the ZIP file
BUNDLE_ZIP=$(find "$SCRIPT_DIR" -maxdepth 1 -name "SeriesGuide*.zip" | head -n 1)

if [ -z "$BUNDLE_ZIP" ]; then
    echo "Error: No SeriesGuide*.zip file found in $SCRIPT_DIR"
    exit 1
fi

echo "Found archive: $BUNDLE_ZIP"

# Create temporary directory in the working directory
TEMP_DIR="$SCRIPT_DIR/temp_translations_$$"
mkdir -p "$TEMP_DIR"
trap 'rm -rf "$TEMP_DIR"' EXIT
echo "Extracting to temporary directory: $TEMP_DIR"

# Extract the ZIP archive
unzip -q "$BUNDLE_ZIP" -d "$TEMP_DIR"

# Find all values-* directories
VALUES_DIRS=$(find "$TEMP_DIR/seriesguide" -type d -name "values-*" | sort)

if [ -z "$VALUES_DIRS" ]; then
    echo "Warning: No values-* directories found in the archive"
    exit 0
fi

# Step 1: Build the list of source -> target directory mappings
declare -a SRC_DIRS
declare -a TARGET_DIRS

echo "Planned changes:"
for dir in $VALUES_DIRS; do
    dirname=$(basename "$dir")

    # Handle special case for Serbian Latin script
    if [ "$dirname" = "values-sr-rCS" ]; then
        target_dirname="values-b+sr+Latn"
    # Keep pt and zh directories as-is since they have distinct regional variants
    elif [[ $dirname =~ ^values-(pt|zh)- ]]; then
        target_dirname="$dirname"
    # For all other directories, remove region suffix if present
    else
        # Remove the -r[A-Z]+ suffix using parameter expansion:
        # % removes the shortest match of the following pattern from the end of the variable
        target_dirname="${dirname%-r[A-Z]*}"
    fi

    echo "  $dirname -> $target_dirname"
    SRC_DIRS+=("$dir")
    TARGET_DIRS+=("$target_dirname")
done

# Step 2: Ask for confirmation before copying
echo ""
read -r -p "Copy ${#SRC_DIRS[@]} directories to $RES_DIR? [y/N] " confirm
if [[ ! $confirm =~ ^[Yy]$ ]]; then
    echo "Aborted."
    exit 0
fi

# Step 3: Copy files to the resource directory
echo ""
for i in "${!SRC_DIRS[@]}"; do
    dir="${SRC_DIRS[$i]}"
    target_dirname="${TARGET_DIRS[$i]}"
    echo "Copying $(basename "$dir") -> $target_dirname"
    mkdir -p "$RES_DIR/$target_dirname"
    cp -rf "$dir"/* "$RES_DIR/$target_dirname/"
done

echo "Translation update complete!"
