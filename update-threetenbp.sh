#!/usr/bin/env bash
# Updates threetenbp to the latest release and replaces the embedded TZDB.dat with the one from
# its jar. Run from anywhere inside the repository, for example with Git Bash on Windows.
set -euo pipefail

repo_url="https://repo1.maven.org/maven2/org/threeten/threetenbp"
tzdb_path="org/threeten/bp/TZDB.dat"

# Run from the repository root so relative paths work regardless of where the script is called.
cd "$(git rev-parse --show-toplevel)"
assets_dir="app/src/main/assets"
catalog="gradle/libs.versions.toml"

temp_dir="$(mktemp -d)"
trap 'rm -rf "$temp_dir"' EXIT

# Look up the latest release version
version="$(curl --fail --silent --show-error --location "$repo_url/maven-metadata.xml" \
  | sed -n -E 's|.*<release>([^<]+)</release>.*|\1|p')"
if [[ -z "$version" ]]; then
  echo "Could not determine latest threetenbp version" >&2
  exit 1
fi
echo "Latest threetenbp version: $version"

# Download the release jar
jar="$temp_dir/threetenbp-$version.jar"
curl --fail --silent --show-error --location --output "$jar" \
  "$repo_url/$version/threetenbp-$version.jar"

# Replace embedded TZDB with the one from the jar
mkdir -p "$assets_dir/$(dirname "$tzdb_path")"
unzip -o -q "$jar" "$tzdb_path" -d "$temp_dir"
mv -f "$temp_dir/$tzdb_path" "$assets_dir/$tzdb_path"

# Bump version in version catalog.
# -b (binary) preserves CRLF line endings on Windows, it is a no-op on other systems.
sed -b -i -E "s/(org\.threeten:threetenbp:)[^\"]+/\1${version}/" "$catalog"

git status
