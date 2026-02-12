#!/bin/bash

# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

# Extracts version name and code from the first file and uses it
# - to replace "Next release" with a concrete release string, like "$VERSION - YYYY-MM-DD" in the
#   second file and
# - suggest a commit message, like "Prepare version $VERSION ($VERSION_CODE)"

if [ $# -lt 2 ]; then
    echo "Usage: $0 <build-gradle-file> <target-file>"
    echo "Example: $0 build.gradle.kts CHANGELOG.md"
    exit 1
fi

BUILD_GRADLE_FILE="$1"
TARGET_FILE="$2"

if [ ! -f "$BUILD_GRADLE_FILE" ]; then
    echo "Error: File '$BUILD_GRADLE_FILE' not found"
    exit 1
fi

if [ ! -f "$TARGET_FILE" ]; then
    echo "Error: File '$TARGET_FILE' not found"
    exit 1
fi

# Extract sgVersionName
# Looking for pattern: val sgVersionName by extra("X.Y.Z")
VERSION=$(grep -oP 'val sgVersionName by extra\("\K[^"]+' "$BUILD_GRADLE_FILE")

if [ -z "$VERSION" ]; then
    echo "Error: Could not extract sgVersionName from $BUILD_GRADLE_FILE"
    exit 1
fi

# Extract sgVersionCode
# Looking for pattern: val sgVersionCode by extra(12345678)
VERSION_CODE=$(grep -oP 'val sgVersionCode by extra\(\K[0-9]+' "$BUILD_GRADLE_FILE")

if [ -z "$VERSION_CODE" ]; then
    echo "Error: Could not extract sgVersionCode from $BUILD_GRADLE_FILE"
    exit 1
fi

# Get current date in ISO format (YYYY-MM-DD)
CURRENT_DATE=$(date +%Y-%m-%d)

# Create replacement string
REPLACEMENT="$VERSION - $CURRENT_DATE"

echo "Extracted version: $VERSION"
echo "Extracted version code: $VERSION_CODE"
echo "Current date: $CURRENT_DATE"
echo "Replacement string: $REPLACEMENT"
echo "Updating file: $TARGET_FILE"

# Replace first occurrence of "Next release" with the version and date
sed -i "0,/Next release/{s/Next release/$REPLACEMENT/}" "$TARGET_FILE"

if [ $? -eq 0 ]; then
    echo "Successfully updated '$TARGET_FILE'"
    echo ""
    echo "Suggested commit message:"
    echo "Prepare version $VERSION ($VERSION_CODE)"
else
    echo "Error: Failed to update file"
    exit 1
fi

