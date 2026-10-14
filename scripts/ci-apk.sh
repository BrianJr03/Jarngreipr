#!/usr/bin/env bash
# ci-apk.sh — trigger the build-apks workflow for <branch>, wait, download,
# install the <debug|release> APK onto a connected adb device.
#
# Usage: scripts/ci-apk.sh <branch> <debug|release> [--no-trigger]
#
# Works on:
#   * a desktop with the device on USB/wireless adb
#   * running ON the Android device inside Termux / proot (uses adb-over-loopback
#     if WIRELESS_DEBUGGING_PORT is set; else falls back to termux-open)
set -euo pipefail

usage() {
  cat <<EOF >&2
Usage: $0 <branch> <debug|release> [--no-trigger]

Options:
  --no-trigger    Skip "gh workflow run"; just find the latest run for <branch>'s
                  current head sha and watch/download/install it.

Env:
  ANDROID_SERIAL          Pick a specific adb device when more than one is attached.
  WIRELESS_DEBUGGING_PORT When running inside Termux without a USB host, adb will
                          connect to 127.0.0.1:\$WIRELESS_DEBUGGING_PORT.
  CI_APK_FLAVOR           Preferred flavor substring when the artifact contains
                          multiple APKs. Defaults to "standard".
EOF
  exit 2
}

[ $# -ge 2 ] || usage
BRANCH="$1"
VARIANT="$2"
case "$VARIANT" in
  debug|release) ;;
  *) usage ;;
esac
NO_TRIGGER=0
if [ "${3:-}" = "--no-trigger" ]; then NO_TRIGGER=1; fi

FLAVOR_PREF="${CI_APK_FLAVOR:-standard}"

die() { echo "error: $*" >&2; exit 1; }
log() { echo "==> $*"; }

# ---------- preflight ----------

command -v gh >/dev/null 2>&1 || die "gh not found. Install the GitHub CLI."
command -v git >/dev/null 2>&1 || die "git not found."

gh auth status >/dev/null 2>&1 || die "gh not authenticated. Run: gh auth login"
SCOPES_LINE=$(gh auth status 2>&1 | grep -E 'Token scopes:' | head -1 || true)
case "$SCOPES_LINE" in
  *repo*) ;;
  *) die "gh token missing 'repo' scope. Run: gh auth refresh -s repo,workflow" ;;
esac
case "$SCOPES_LINE" in
  *workflow*) ;;
  *) die "gh token missing 'workflow' scope. Run: gh auth refresh -s repo,workflow" ;;
esac

REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null) || die "not inside a git repo"
cd "$REPO_ROOT"

# Branch exists locally?
git rev-parse --verify "refs/heads/$BRANCH" >/dev/null 2>&1 \
  || die "local branch '$BRANCH' not found. Check it out first."

LOCAL_SHA=$(git rev-parse "$BRANCH")

# Branch is pushed and up to date?
if ! git ls-remote --exit-code --heads origin "$BRANCH" >/dev/null 2>&1; then
  log "branch $BRANCH not on origin — pushing..."
  git push -u origin "$BRANCH"
else
  REMOTE_SHA=$(git ls-remote origin "refs/heads/$BRANCH" | awk '{print $1}')
  if [ "$REMOTE_SHA" != "$LOCAL_SHA" ]; then
    log "origin/$BRANCH out of date (origin=$REMOTE_SHA, local=$LOCAL_SHA) — pushing..."
    git push origin "$BRANCH"
  fi
fi

REPO_SLUG=$(gh repo view --json nameWithOwner --jq .nameWithOwner)
log "repo: $REPO_SLUG"
log "branch: $BRANCH @ $LOCAL_SHA"

# ---------- trigger ----------

if [ "$NO_TRIGGER" -eq 0 ]; then
  log "triggering build-apks.yml on $BRANCH..."
  gh workflow run build-apks.yml --ref "$BRANCH"
fi

# ---------- find THIS run ----------

RUN_ID=""
log "waiting for a workflow_dispatch run on sha $LOCAL_SHA..."
for _ in $(seq 1 45); do
  RUN_ID=$(gh run list \
    --workflow build-apks.yml \
    --branch "$BRANCH" \
    --event workflow_dispatch \
    --json databaseId,headSha,createdAt \
    --limit 20 \
    --jq "map(select(.headSha == \"$LOCAL_SHA\")) | sort_by(.createdAt) | last | .databaseId // empty")
  [ -n "$RUN_ID" ] && break
  sleep 2
done
[ -n "$RUN_ID" ] || die "no build-apks workflow_dispatch run appeared for $LOCAL_SHA within 90s"

RUN_URL="https://github.com/${REPO_SLUG}/actions/runs/${RUN_ID}"
log "run: $RUN_URL"

# ---------- watch ----------

if ! gh run watch "$RUN_ID" --exit-status --interval 10; then
  echo "" >&2
  echo "build failed. Last 100 lines of failed logs:" >&2
  gh run view "$RUN_ID" --log-failed | tail -100 || true
  exit 1
fi

# ---------- download ----------

OUT_DIR="build/ci/${RUN_ID}"
mkdir -p "$OUT_DIR"
log "downloading ${VARIANT}-apk to $OUT_DIR"
gh run download "$RUN_ID" -n "${VARIANT}-apk" -D "$OUT_DIR"

APK=""
# prefer FLAVOR_PREF, fall back to any apk
while IFS= read -r cand; do
  APK="$cand"
  break
done < <(ls -1 "$OUT_DIR"/*"$FLAVOR_PREF"*.apk 2>/dev/null | sort)
if [ -z "$APK" ]; then
  while IFS= read -r cand; do
    APK="$cand"
    break
  done < <(ls -1 "$OUT_DIR"/*.apk 2>/dev/null | sort)
fi
[ -n "$APK" ] || die "no APK downloaded to $OUT_DIR"
log "APK: $APK"

# ---------- install ----------

try_aapt() {
  local candidates=()
  for base in \
    "${ANDROID_HOME:-}" \
    "$HOME/Library/Android/sdk" \
    "$HOME/Android/Sdk" \
    "/opt/homebrew/share/android-sdk" \
    "/usr/local/share/android-sdk" \
    "/data/data/com.termux/files/usr"; do
    [ -n "$base" ] || continue
    while IFS= read -r f; do
      [ -n "$f" ] && candidates+=("$f")
    done < <(ls -1 "$base"/build-tools/*/aapt 2>/dev/null || true)
  done
  if command -v aapt >/dev/null 2>&1; then
    aapt dump badging "$APK" 2>/dev/null
    return
  fi
  if [ ${#candidates[@]} -gt 0 ]; then
    "${candidates[0]}" dump badging "$APK" 2>/dev/null
  fi
}

detect_app_id() {
  try_aapt | awk -F"'" '/^package: name=/{print $2; exit}'
}

install_via_adb() {
  local -a devs
  mapfile -t devs < <(adb devices | awk 'NR>1 && $2=="device"{print $1}')
  local n=${#devs[@]}
  if [ "$n" -eq 0 ]; then
    return 2
  fi
  if [ "$n" -gt 1 ] && [ -z "${ANDROID_SERIAL:-}" ]; then
    echo "adb sees $n devices; set ANDROID_SERIAL to pick one:" >&2
    adb devices >&2
    return 2
  fi
  local serial="${ANDROID_SERIAL:-${devs[0]}}"
  log "installing to $serial via adb..."
  local out rc
  set +e
  out=$(adb -s "$serial" install -r "$APK" 2>&1)
  rc=$?
  set -e
  echo "$out"
  if [ $rc -ne 0 ] && echo "$out" | grep -q INSTALL_FAILED_UPDATE_INCOMPATIBLE; then
    cat <<EOF >&2

Signature mismatch (INSTALL_FAILED_UPDATE_INCOMPATIBLE).
The APK on this run was signed with a different key than the app already on-device.
Not auto-uninstalling: that wipes app data.

Options:
  1. Uninstall yourself and reinstall:    adb -s $serial uninstall <appId>
  2. Re-sign this APK with the on-device key, or trigger CI with the matching keystore secret.
EOF
    return 2
  fi
  if [ $rc -ne 0 ]; then
    echo "adb install failed with code $rc" >&2
    return 2
  fi
  INSTALLED_SERIAL="$serial"
  return 0
}

install_via_termux() {
  if command -v termux-open >/dev/null 2>&1; then
    log "launching system installer via termux-open; tap Install on the device."
    termux-open "$APK"
    return 0
  fi
  return 2
}

INSTALLED_SERIAL=""
if command -v adb >/dev/null 2>&1; then
  if adb devices | awk 'NR>1 && $2=="device"' | grep -q .; then
    install_via_adb || die "adb install failed"
  elif [ -n "${TERMUX_VERSION:-}" ] || [ -d /data/data/com.termux ]; then
    if [ -n "${WIRELESS_DEBUGGING_PORT:-}" ]; then
      log "trying adb connect 127.0.0.1:$WIRELESS_DEBUGGING_PORT (Termux loopback)..."
      adb connect "127.0.0.1:${WIRELESS_DEBUGGING_PORT}" >/dev/null 2>&1 || true
      if ! install_via_adb; then
        install_via_termux || die "no adb device and termux-open fallback failed"
      fi
    else
      log "no WIRELESS_DEBUGGING_PORT set; using termux-open fallback"
      install_via_termux || die "termux-open not available"
    fi
  else
    die "adb present but no device connected"
  fi
else
  install_via_termux || die "neither adb nor termux-open found; APK is at $APK"
fi

# ---------- report ----------

APP_ID=$(detect_app_id || true)

echo ""
echo "Run URL : $RUN_URL"
echo "Commit  : $LOCAL_SHA"
echo "APK     : $APK"
if [ -n "${APP_ID:-}" ] && [ -n "$INSTALLED_SERIAL" ] && command -v adb >/dev/null 2>&1; then
  echo "App ID  : $APP_ID"
  echo "Installed:"
  adb -s "$INSTALLED_SERIAL" shell dumpsys package "$APP_ID" \
    | grep -E 'version(Name|Code)=' \
    | head -2 \
    | sed 's/^/  /'
fi
