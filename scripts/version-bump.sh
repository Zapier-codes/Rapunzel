#!/bin/bash
# version-bump.sh — Auto-bump Rapunzel version
# Usage: ./scripts/version-bump.sh [patch|minor|major]
# Reads/writes gradle.properties rapunzel.app.version

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GRADLE_PROPS="$SCRIPT_DIR/../gradle.properties"

BUMP_TYPE="${1:-patch}"

if [[ ! -f "$GRADLE_PROPS" ]]; then
    echo "Error: $GRADLE_PROPS not found"
    exit 1
fi

CURRENT_VERSION=$(grep "^rapunzel.app.version=" "$GRADLE_PROPS" | cut -d'=' -f2)

if [[ -z "$CURRENT_VERSION" ]]; then
    echo "Error: rapunzel.app.version not found in $GRADLE_PROPS"
    exit 1
fi

# Parse version components (format: major.minor.patch)
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"

case "$BUMP_TYPE" in
    major)
        MAJOR=$((MAJOR + 1))
        MINOR=0
        PATCH=0
        ;;
    minor)
        MINOR=$((MINOR + 1))
        PATCH=0
        ;;
    patch)
        PATCH=$((PATCH + 1))
        ;;
    *)
        echo "Usage: $0 [patch|minor|major]"
        exit 1
        ;;
esac

NEW_VERSION="$MAJOR.$MINOR.$PATCH"

# Update gradle.properties
sed -i "s/^rapunzel.app.version=.*/rapunzel.app.version=$NEW_VERSION/" "$GRADLE_PROPS"

# Also bump versionCode
CURRENT_CODE=$(grep "^rapunzel.app.versionCode=" "$GRADLE_PROPS" | cut -d'=' -f2)
NEW_CODE=$((CURRENT_CODE + 1))
sed -i "s/^rapunzel.app.versionCode=.*/rapunzel.app.versionCode=$NEW_CODE/" "$GRADLE_PROPS"

echo "Bumped version: $CURRENT_VERSION -> $NEW_VERSION (code: $CURRENT_CODE -> $NEW_CODE)"
