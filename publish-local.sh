#!/usr/bin/env bash
# Publish every framework module to ~/.m2, in dependency order.
#
#   ./publish-local.sh            publish all eight modules
#   ./publish-local.sh --clean    clean first, then publish
#
# Order follows the `api project(...)` edges in each module's build.gradle:
#
#   persistence-common, core      no framework dependencies
#   institute-dal                 -> persistence-common
#   identity-dal                  -> institute-dal, persistence-common
#   profile-dal                   -> identity-dal, persistence-common
#   drive-dal                     -> institute-dal, identity-dal, profile-dal, persistence-common
#   notification-dal, messaging-dal
#
# The services resolve org.yuktisetu:*:1.0.0-SNAPSHOT from mavenLocal(), so a
# service build only sees a framework change after this has run.
set -euo pipefail

cd "$(dirname "$0")"

MODULES=(
  persistence-common
  core
  institute-dal
  identity-dal
  profile-dal
  drive-dal
  notification-dal
  messaging-dal
)
VERSION="1.0.0-SNAPSHOT"
M2="${HOME}/.m2/repository/org/yuktisetu"

# Always stop the Gradle daemon this script started, even on failure.
trap './gradlew --stop >/dev/null 2>&1 || true' EXIT

if [[ "${1:-}" == "--clean" ]]; then
  echo "==> clean"
  ./gradlew -q clean
fi

for m in "${MODULES[@]}"; do
  echo "==> publishing ${m}"
  ./gradlew -q ":${m}:publishToMavenLocal"
  jar="${M2}/${m}/${VERSION}/${m}-${VERSION}.jar"
  if [[ ! -f "${jar}" ]]; then
    echo "FAILED: ${jar} was not written" >&2
    exit 1
  fi
done

echo
echo "Published ${#MODULES[@]} modules to ${M2}:"
for m in "${MODULES[@]}"; do
  printf '  %-20s %s\n' "${m}" "$(date -r "${M2}/${m}/${VERSION}/${m}-${VERSION}.jar" '+%Y-%m-%d %H:%M:%S')"
done
