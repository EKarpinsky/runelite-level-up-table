#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."

# Linux: JDK 11/17 with the desktop JRE, xvfb and xauth. First run needs network.
for tool in java xvfb-run timeout; do
  command -v "$tool" >/dev/null || { echo "Missing command: $tool" >&2; exit 1; }
done

capture_scratch=$(mktemp -d "${TMPDIR:-/tmp}/skill-unlocks.XXXXXX")
trap 'rm -rf "$capture_scratch"' EXIT
mkdir -p "$capture_scratch/home"

./gradlew shadowJar --no-daemon
timeout --signal=TERM --kill-after=5s 180s \
  xvfb-run -a -s '-screen 0 1440x1000x24' \
  java -Duser.home="$capture_scratch/home" -Dsun.java2d.uiScale=1 -ea \
  -cp build/libs/runelite-skill-unlocks-1.0-SNAPSHOT-all.jar \
  com.runelite.skillunlocks.ClientCapture "$capture_scratch/images"

mkdir -p docs
cp "$capture_scratch/images/panel.png" docs/
ls -la docs/panel.png
