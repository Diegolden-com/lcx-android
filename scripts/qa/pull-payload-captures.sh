#!/usr/bin/env bash
set -euo pipefail

# Pulls debug payload captures from Android app external files.
#
# Usage:
#   ./scripts/qa/pull-payload-captures.sh [package_name]
#
# Examples:
#   ./scripts/qa/pull-payload-captures.sh
#   ./scripts/qa/pull-payload-captures.sh com.cleanx.lcx.dev

PACKAGE_NAME="${1:-com.cleanx.lcx.dev}"
TODAY="$(date +%Y%m%d)"
STAMP="$(date +%Y%m%d-%H%M%S)"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT_DIR="$ROOT_DIR/docs/evidence/$TODAY"
OUT_FILE="$OUT_DIR/payload-capture-$STAMP.jsonl"

REMOTE_FILE="/sdcard/Android/data/$PACKAGE_NAME/files/payload-capture/payload-capture.jsonl"

mkdir -p "$OUT_DIR"

echo "[payload-capture] checking adb device..."
adb get-state >/dev/null

echo "[payload-capture] reading $REMOTE_FILE"
if ! adb shell "[ -f '$REMOTE_FILE' ]"; then
  echo "[payload-capture] file not found on device."
  echo "[payload-capture] expected path: $REMOTE_FILE"
  exit 1
fi

adb shell "cat '$REMOTE_FILE'" > "$OUT_FILE"

LINES="$(wc -l < "$OUT_FILE" | tr -d ' ')"
BYTES="$(wc -c < "$OUT_FILE" | tr -d ' ')"

echo "[payload-capture] saved: $OUT_FILE"
echo "[payload-capture] lines=$LINES bytes=$BYTES"
