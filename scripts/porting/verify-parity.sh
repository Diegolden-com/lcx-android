#!/usr/bin/env bash
# verify-parity.sh
# Verifies route parity registry against real Android navigation wiring.
# Usage:
#   ./scripts/porting/verify-parity.sh [--json] [--section <section>] [--strict]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
REGISTRY="$ANDROID_ROOT/docs/porting/route-registry.json"
SCREEN_CONTRACT="$ANDROID_ROOT/core/src/main/java/com/cleanx/lcx/core/navigation/Screen.kt"

if [[ ! -f "$REGISTRY" ]]; then
  echo "ERROR: route-registry.json not found at $REGISTRY" >&2
  exit 1
fi

if [[ ! -f "$SCREEN_CONTRACT" ]]; then
  echo "ERROR: Screen.kt not found at $SCREEN_CONTRACT" >&2
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "ERROR: jq is required. Install with: brew install jq" >&2
  exit 1
fi

if ! command -v rg >/dev/null 2>&1; then
  echo "ERROR: rg is required. Install with: brew install ripgrep" >&2
  exit 1
fi

OUTPUT_JSON=false
STRICT_MODE=false
FILTER_SECTION=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --json) OUTPUT_JSON=true; shift ;;
    --section) FILTER_SECTION="${2:-}"; shift 2 ;;
    --strict) STRICT_MODE=true; shift ;;
    *) echo "Unknown option: $1" >&2; exit 1 ;;
  esac
done

parse_screen_name() {
  local android_screen="$1"
  if [[ "$android_screen" =~ ^Screen\.([A-Za-z0-9_]+) ]]; then
    echo "${BASH_REMATCH[1]}"
  else
    echo ""
  fi
}

has_contract() {
  local screen_name="$1"
  rg -q "@Serializable data (object|class) ${screen_name}\\b" "$SCREEN_CONTRACT"
}

has_composable_binding() {
  local screen_name="$1"
  rg -q --glob '**/src/main/**' "composable<Screen\\.${screen_name}>" "$ANDROID_ROOT"
}

routes_stream() {
  if [[ -n "$FILTER_SECTION" ]]; then
    jq -r --arg section "$FILTER_SECTION" \
      '.routes | map(select(.section == $section)) | .[] | @base64' \
      "$REGISTRY"
  else
    jq -r '.routes[] | @base64' "$REGISTRY"
  fi
}

tmp_results="$(mktemp)"
trap 'rm -f "$tmp_results"' EXIT

total=0
declared_present=0
verified_present=0
parity_done=0
mismatches=0

while IFS= read -r row; do
  [[ -z "$row" ]] && continue
  total=$((total + 1))

  decode() {
    echo "$row" | base64 --decode | jq -r "$1"
  }

  id="$(decode '.id')"
  pwa_path="$(decode '.pwa_path')"
  section="$(decode '.section')"
  android_screen="$(decode '.android_screen // empty')"
  declared_route_present="$(decode '.route_present')"
  route_parity_done="$(decode '.parity_done')"

  [[ "$declared_route_present" == "true" ]] && declared_present=$((declared_present + 1))
  [[ "$route_parity_done" == "true" ]] && parity_done=$((parity_done + 1))

  verified_route_present=false
  mismatch=false
  reason=""
  screen_name=""

  if [[ -z "$android_screen" ]]; then
    reason="android_screen_missing"
  else
    screen_name="$(parse_screen_name "$android_screen")"
    if [[ -z "$screen_name" ]]; then
      reason="android_screen_unparsed"
    else
      has_contract_flag=false
      has_composable_flag=false
      has_contract "$screen_name" && has_contract_flag=true
      has_composable_binding "$screen_name" && has_composable_flag=true

      if [[ "$has_contract_flag" == true && "$has_composable_flag" == true ]]; then
        verified_route_present=true
      else
        missing_reasons=()
        [[ "$has_contract_flag" == false ]] && missing_reasons+=("screen_contract_missing")
        [[ "$has_composable_flag" == false ]] && missing_reasons+=("composable_binding_missing")
        reason="$(IFS=,; echo "${missing_reasons[*]}")"
      fi
    fi
  fi

  [[ "$verified_route_present" == "true" ]] && verified_present=$((verified_present + 1))

  if [[ "$declared_route_present" != "$verified_route_present" ]]; then
    mismatch=true
    mismatches=$((mismatches + 1))
  fi

  jq -n \
    --arg id "$id" \
    --arg pwa_path "$pwa_path" \
    --arg section "$section" \
    --arg android_screen "$android_screen" \
    --arg screen_name "$screen_name" \
    --arg reason "$reason" \
    --argjson declared_route_present "$declared_route_present" \
    --argjson verified_route_present "$verified_route_present" \
    --argjson parity_done "$route_parity_done" \
    --argjson mismatch "$mismatch" \
    '{
      id: $id,
      pwa_path: $pwa_path,
      section: $section,
      android_screen: (if $android_screen == "" then null else $android_screen end),
      screen_name: (if $screen_name == "" then null else $screen_name end),
      declared_route_present: $declared_route_present,
      verified_route_present: $verified_route_present,
      parity_done: $parity_done,
      mismatch: $mismatch,
      reason: (if $reason == "" then null else $reason end)
    }' >> "$tmp_results"
done < <(routes_stream)

timestamp="$(date '+%Y-%m-%d %H:%M:%S')"
status="ok"
[[ $mismatches -gt 0 ]] && status="drift_detected"

if [[ "$OUTPUT_JSON" == true ]]; then
  jq -n \
    --arg timestamp "$timestamp" \
    --arg registry "$REGISTRY" \
    --arg section_filter "$FILTER_SECTION" \
    --arg status "$status" \
    --argjson total "$total" \
    --argjson declared_present "$declared_present" \
    --argjson verified_present "$verified_present" \
    --argjson parity_done "$parity_done" \
    --argjson mismatches "$mismatches" \
    --slurpfile routes "$tmp_results" \
    '{
      timestamp: $timestamp,
      registry: $registry,
      status: $status,
      section_filter: (if $section_filter == "" then null else $section_filter end),
      summary: {
        total: $total,
        declared_route_present: $declared_present,
        verified_route_present: $verified_present,
        parity_done: $parity_done,
        mismatches: $mismatches
      },
      routes: $routes
    }'
else
  echo "# Parity Verification Report"
  echo "# Generated: $timestamp"
  echo "# Registry: $REGISTRY"
  [[ -n "$FILTER_SECTION" ]] && echo "# Section filter: $FILTER_SECTION"
  echo ""
  echo "## Summary"
  echo "  Total routes:                 $total"
  echo "  Declared ROUTE_PRESENT=YES:   $declared_present"
  echo "  Verified ROUTE_PRESENT=YES:   $verified_present"
  echo "  PARITY_DONE=YES:              $parity_done"
  echo "  Registry mismatches:          $mismatches"
  echo ""

  echo "## Mismatches (declared vs verified)"
  if [[ $mismatches -eq 0 ]]; then
    echo "  None"
  else
    jq -r 'select(.mismatch == true) | "- \(.id): declared=\(.declared_route_present) verified=\(.verified_route_present) reason=\(.reason // "n/a") (\(.pwa_path))"' "$tmp_results"
  fi
  echo ""

  echo "## Route Detail"
  printf "  %-6s %-8s %-6s %-12s %s\n" "DECL" "VERIF" "DONE" "SECTION" "PWA_PATH"
  printf "  %-6s %-8s %-6s %-12s %s\n" "----" "-----" "----" "-------" "--------"
  jq -r '[.declared_route_present,.verified_route_present,.parity_done,.section,.pwa_path] | @tsv' "$tmp_results" \
    | while IFS=$'\t' read -r declared verified done section path; do
      decl_label=$([[ "$declared" == "true" ]] && echo "YES" || echo "NO")
      ver_label=$([[ "$verified" == "true" ]] && echo "YES" || echo "NO")
      done_label=$([[ "$done" == "true" ]] && echo "YES" || echo "NO")
      printf "  %-6s %-8s %-6s %-12s %s\n" "$decl_label" "$ver_label" "$done_label" "$section" "$path"
    done
fi

if [[ "$STRICT_MODE" == true && $mismatches -gt 0 ]]; then
  exit 2
fi
