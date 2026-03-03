#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
WEB_ROOT="${WEB_ROOT:-/Users/diegolden/Code/LCX/v0-lcx-pwa}"
ADB_BIN="${ADB_BIN:-/Users/diegolden/Library/Android/sdk/platform-tools/adb}"
JAVA_HOME_DEFAULT="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

RUN_INSTALL=false
DURATION_SEC="${DURATION_SEC:-120}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --install)
      RUN_INSTALL=true
      shift
      ;;
    --duration)
      DURATION_SEC="${2:-120}"
      shift 2
      ;;
    *)
      echo "Unknown arg: $1"
      echo "Usage: $0 [--install] [--duration <seconds>]"
      exit 1
      ;;
  esac
done

if [[ ! -x "$ADB_BIN" ]]; then
  echo "ERROR: adb not executable at $ADB_BIN"
  exit 1
fi

DATE_DIR="$(date +%Y%m%d)"
TS="$(date +%H%M%S)"
EVIDENCE_DIR="$ANDROID_ROOT/docs/evidence/$DATE_DIR"
LOG_FILE="$EVIDENCE_DIR/porting-demo-smoke-$TS.log"
mkdir -p "$EVIDENCE_DIR"

echo "== Porting Demo Smoke =="
echo "Android root: $ANDROID_ROOT"
echo "Web root: $WEB_ROOT"
echo "Evidence file: $LOG_FILE"

echo "\n== Device preflight =="
DEVICE_OUTPUT="$($ADB_BIN devices -l)"
echo "$DEVICE_OUTPUT"

if ! echo "$DEVICE_OUTPUT" | rg -q "\sdevice\b"; then
  echo "ERROR: no USB device in state 'device'"
  exit 1
fi

$ADB_BIN reverse tcp:3000 tcp:3000
$ADB_BIN reverse tcp:54321 tcp:54321
$ADB_BIN reverse --list

echo "\n== Local backend quick checks =="
if command -v lsof >/dev/null 2>&1; then
  lsof -nP -iTCP:3000 -sTCP:LISTEN || echo "WARN: no listener on 3000"
fi
if command -v supabase >/dev/null 2>&1; then
  (cd "$WEB_ROOT" && supabase status) || true
fi

ANON_KEY=""
if command -v supabase >/dev/null 2>&1; then
  ANON_KEY="$(cd "$WEB_ROOT" && supabase status -o env | awk -F= '/^ANON_KEY=/{print $2}' | sed 's/^"//; s/"$//')"
fi

if [[ "$RUN_INSTALL" == true ]]; then
  echo "\n== installDevDebug =="
  export JAVA_HOME="${JAVA_HOME:-$JAVA_HOME_DEFAULT}"
  export PATH="$JAVA_HOME/bin:$PATH"
  export LCX_DEV_API_BASE_URL="http://127.0.0.1:3000"
  export LCX_DEV_SUPABASE_URL="http://127.0.0.1:54321"
  export LCX_DEV_SUPABASE_ANON_KEY="$ANON_KEY"
  (cd "$ANDROID_ROOT" && ./gradlew :app:installDevDebug --console=plain)
fi

echo "\n== Log capture =="
echo "Capturing for ${DURATION_SEC}s. Run your manual flow now (tabs + agua/caja/checklist/tickets)."
$ADB_BIN logcat -c
(
  "$ADB_BIN" logcat -v threadtime |
    rg -i "TXN|HTTP|TICKET|PAYMENT|PRINT|AUTH|WATER|CHECKLIST|CAJA|Correlation|Session"
) > "$LOG_FILE" &
CAP_PID=$!

sleep "$DURATION_SEC"
kill -INT "$CAP_PID" 2>/dev/null || true
wait "$CAP_PID" 2>/dev/null || true

LINE_COUNT="$(wc -l < "$LOG_FILE" | tr -d ' ')"
echo "Captured $LINE_COUNT lines -> $LOG_FILE"

echo "\n== Quick summary =="
for token in TXN HTTP TICKET PAYMENT PRINT AUTH WATER CHECKLIST CAJA Correlation Session; do
  count="$(rg -c "$token" "$LOG_FILE" || true)"
  echo "$token: $count"
done

echo "\nDone."
