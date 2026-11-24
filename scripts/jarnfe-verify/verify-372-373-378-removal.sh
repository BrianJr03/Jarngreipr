#!/usr/bin/env bash
# Device check for #372, #373 and #378: removing a folder tile never leaves a tile on a missing
# folder, and a removal cut short by a process death is finished on the next start.
#
#   scripts/jarnfe-verify/verify-372-373-378-removal.sh [options]
#
#   --page N                Canvas page to seed (default: the first Canvas page; one is added
#                           when there is none, and removed again on restore).
#   --classic-ui            Also drive Classic's folder delete confirm and Hide All. Waits for
#                           Classic Canvas to show the seeded folders on screen.
#   --jarn-ui               Also check a Jarn tile removal force-stopped inside its undo window.
#                           Waits for you to remove the seeded "JV Undo" tile in Jarn Canvas.
#   --wait SECONDS          How long the UI steps wait for you (default 90).
#   --out DIR               Where backups, pulled files and UI dumps go (default: a new temp folder).
#   --allow-app-focus-sync  Restart the app even if its Focus Lock mode could rewrite
#                           screen_focus_lock on start. This script never writes it itself.
#
# Needs the debug build (run-as). Backs up the canvas layout, the folder store, the removal
# journal and the page list first, and restores them on exit, even after a failure or Ctrl-C. Seeds only folders and
# tiles named "JV ..." on one page. Never uninstalls, clears data or changes settings.
#
# Every check that restarts the app is skipped unless the app's start-up Focus Lock sync is proven
# inert (or --allow-app-focus-sync), since starting the app can sync screen_focus_lock.
#
# Exit codes: 0 no failures, 1 at least one failure, 2 could not run.
set -uo pipefail

SUITE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="$SUITE_DIR/lib"
# shellcheck source=lib/parse.sh
source "$LIB_DIR/parse.sh"
# shellcheck source=lib/common.sh
source "$LIB_DIR/common.sh"

PKG="${JARNFE_PKG:-jr.brian.home.debug}"
LAYOUT_FILE="shared_prefs/canvas_layout_prefs.xml"
FOLDERS_FILE="files/datastore/folders.preferences_pb"
JOURNAL_FILE="shared_prefs/jarn_tile_removals.xml"
PAGE_TYPES_FILE="shared_prefs/page_type_prefs.xml"
PAGE_COUNT_FILE="shared_prefs/page_count_prefs.xml"
FILES="$LAYOUT_FILE $FOLDERS_FILE $JOURNAL_FILE $PAGE_TYPES_FILE $PAGE_COUNT_FILE"
REMOTE_TMP=/data/local/tmp/jarnfe-verify-removal
SEED_PKG="com.android.settings"

PAGE="" CLASSIC_UI=0 JARN_UI=0 WAIT=90 OUT_DIR="" ALLOW_SYNC=0
RESTART_OK=0 RESTART_REASON="" BACKED_UP=0 CHANGED=0 RESTORED=0 START_LOCK=""

usage() { sed -n '2,24p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; }

parse_args() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --page) PAGE="${2:?--page needs a number}"; shift ;;
      --classic-ui) CLASSIC_UI=1 ;;
      --jarn-ui) JARN_UI=1 ;;
      --wait) WAIT="${2:?--wait needs seconds}"; shift ;;
      --out) OUT_DIR="${2:?--out needs a path}"; shift ;;
      --allow-app-focus-sync) ALLOW_SYNC=1 ;;
      -h|--help) usage; exit 0 ;;
      *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
    shift
  done
}

rm_py() { python3 -I "$LIB_DIR/removal.py" "$@"; }

# Lookups exit 0 found, 1 not found, 2 unreadable. Unreadable is never taken as either answer.
saved_has() { rm_py "$@" 2>>"$OUT_DIR/removal-errors.log"; }
saved_lacks() {
  saved_has "$@"
  [ $? = 1 ]
}

select_device() {
  command -v adb >/dev/null || { echo "adb not found" >&2; exit 2; }
  local serials count
  serials="$(adb devices | tr -d '\r' | awk 'NR > 1 && $2 == "device" { print $1 }')"
  count="$(printf '%s' "$serials" | grep -c .)"
  if [ -n "${ANDROID_SERIAL:-}" ]; then
    text_has_line "$serials" "$ANDROID_SERIAL" || { echo "ANDROID_SERIAL=$ANDROID_SERIAL is not attached" >&2; exit 2; }
  elif [ "$count" = 0 ]; then
    echo "no adb device attached" >&2; exit 2
  elif [ "$count" -gt 1 ]; then
    echo "more than one device attached, set ANDROID_SERIAL" >&2; exit 2
  else
    export ANDROID_SERIAL="$serials"
  fi
}

# Same gate as run.sh: Follow (the default) may rewrite screen_focus_lock when the app starts.
read_restart_gate() {
  local prefs mode
  START_LOCK="$(focus_lock_value)"
  if [ "$ALLOW_SYNC" = 1 ]; then RESTART_OK=1; return; fi
  if ! prefs="$(read_prefs focus_lock_prefs)"; then
    RESTART_REASON="Focus Lock mode is unreadable (--allow-app-focus-sync to run)"
    return
  fi
  mode="$(printf '%s' "$prefs" | parse_pref focus_lock_mode)"
  mode="${mode:-Follow}"
  if focus_sync_is_inert "$mode" "$START_LOCK"; then RESTART_OK=1
  else RESTART_REASON="Focus Lock mode $mode may rewrite screen_focus_lock on start (--allow-app-focus-sync to run)"; fi
}

# adb shell joins its arguments into one remote command line, so inner commands are quoted here.
remote_exists() { [ "$(sh_dev "run-as $PKG sh -c '[ -f $1 ] && echo yes'")" = yes ]; }

# Copies device file $1 to local $2. A missing file leaves $2 absent.
pull_file() {
  rm -f "$2"
  remote_exists "$1" || return 0
  adb exec-out run-as "$PKG" cat "$1" >"$2" 2>/dev/null
}

pushed_matches() {
  pull_file "$2" "$OUT_DIR/verify-push.tmp"
  cmp -s "$1" "$OUT_DIR/verify-push.tmp"
}

# Writes local $1 to device file $2 through stdin, falling back to a world-readable temp copy.
push_file() {
  local tmp
  sh_dev "run-as $PKG sh -c 'mkdir -p \$(dirname $2)'" >/dev/null
  adb exec-in "run-as $PKG sh -c 'cat > $2'" <"$1" >/dev/null 2>&1
  pushed_matches "$1" "$2" && return 0
  tmp="$REMOTE_TMP-$(basename "$2")"
  adb push "$1" "$tmp" >/dev/null 2>&1 || return 1
  sh_dev chmod 644 "$tmp" >/dev/null
  sh_dev "run-as $PKG cp $tmp $2" >/dev/null
  sh_dev rm -f "$tmp" >/dev/null
  pushed_matches "$1" "$2"
}

remove_remote() { sh_dev "run-as $PKG rm -f $1" >/dev/null; }

local_copy() { printf '%s/%s' "$1" "$(basename "$2")"; }

snapshot() {
  local dir="$OUT_DIR/$1" file
  mkdir -p "$dir"
  for file in $FILES; do pull_file "$file" "$(local_copy "$dir" "$file")"; done
}

backup() {
  section "Backup"
  stop_app
  snapshot backup
  BACKED_UP=1
  echo "  info  backed up to $OUT_DIR/backup"
}

# Picks the Canvas page to seed, adding a Canvas page and an empty saved layout when needed.
prepare_page() {
  local stage="$OUT_DIR/seed-page" types count
  mkdir -p "$stage"
  read_state seed-page-base
  types="$(local_copy "$OUT_DIR/seed-page-base" "$PAGE_TYPES_FILE")"
  count="$(local_copy "$OUT_DIR/seed-page-base" "$PAGE_COUNT_FILE")"
  [ -n "$PAGE" ] || PAGE="$(rm_py canvas-page "$types")"
  if [ -z "$PAGE" ]; then
    [ -f "$types" ] && [ -f "$count" ] || { echo "page list is not saved yet, open the app once first" >&2; return 1; }
    PAGE="$(rm_py add-canvas-page "$types" "$count" "$stage/types.xml" "$stage/count.xml")" \
      || { echo "could not add a Canvas page" >&2; return 1; }
    CHANGED=1
    push_file "$stage/types.xml" "$PAGE_TYPES_FILE" && push_file "$stage/count.xml" "$PAGE_COUNT_FILE" \
      || { echo "could not save the added Canvas page" >&2; return 1; }
    echo "  info  added Canvas page $PAGE for the run"
  fi
  rm_py ensure-layout "$L" "$stage/layout.xml" "$PAGE" || { echo "could not build a layout for page $PAGE" >&2; return 1; }
  if ! cmp -s "$stage/layout.xml" "$L"; then
    CHANGED=1
    push_file "$stage/layout.xml" "$LAYOUT_FILE" || { echo "could not save a layout for page $PAGE" >&2; return 1; }
    echo "  info  saved an empty layout for Canvas page $PAGE"
  fi
  echo "  info  seeding Canvas page $PAGE"
}

stop_app() { sh_dev am force-stop "$PKG" >/dev/null; pause 1; }

launcher_activity() {
  sh_dev cmd package resolve-activity --brief -a android.intent.action.MAIN \
    -c android.intent.category.LAUNCHER "$PKG" | tail -n 1
}

start_app() {
  local component
  component="$(launcher_activity)"
  case "$component" in "$PKG"/*) ;; *) return 1 ;; esac
  sh_dev am start -W -n "$component" >/dev/null
  pause 3
}

# Restores the backup with the app stopped, so its in-memory copies can't write over it.
restore() {
  [ "$RESTORED" = 0 ] && [ "$BACKED_UP" = 1 ] && [ "$CHANGED" = 1 ] || return 0
  RESTORED=1
  section "Restore"
  local file copy ok=1
  stop_app
  for file in $FILES; do
    copy="$(local_copy "$OUT_DIR/backup" "$file")"
    if [ -f "$copy" ]; then push_file "$copy" "$file" || ok=0
    else remove_remote "$file"; fi
  done
  if [ "$ok" = 1 ]; then pass "canvas layout, folders and journal restored from backup"
  else fail "restore did not verify, the backup is in $OUT_DIR/backup"; fi
  [ "$RESTART_OK" = 1 ] && start_app >/dev/null
}

# Pulls the live files into $OUT_DIR/$1 and sets L, F and J to the local copies.
read_state() {
  snapshot "$1"
  L="$(local_copy "$OUT_DIR/$1" "$LAYOUT_FILE")"
  F="$(local_copy "$OUT_DIR/$1" "$FOLDERS_FILE")"
  J="$(local_copy "$OUT_DIR/$1" "$JOURNAL_FILE")"
}

# Args: slug, label, and 1 when a saved layout must exist by now.
check_no_dangling() {
  local slug="$1" label="$2" required="$3" dangling code
  read_state "audit-$slug"
  if [ ! -f "$L" ]; then
    if [ "$required" = 1 ]; then fail "$label: the canvas layout is missing"
    else skip "$label: no canvas layout saved yet, so there are no tiles to audit"; fi
    return
  fi
  if [ ! -f "$F" ] && [ "$required" = 1 ]; then fail "$label: the folder store is missing"; return; fi
  dangling="$(rm_py dangling "$L" "$F" 2>>"$OUT_DIR/removal-errors.log")"
  code=$?
  if [ "$code" != 0 ]; then
    fail "$label: the layout or folder store could not be read (exit $code, see removal-errors.log)"
  elif [ -n "$dangling" ]; then
    fail "$label: tiles on missing folders (page tile folder): $(printf '%s' "$dangling" | tr '\n' ';')"
  else
    pass "$label: no Canvas tile points at a missing folder"
  fi
}

# Seeds folders and tiles with the app stopped. Args: name:folderId:tileId[:notile] ...
seed() {
  local stage="$OUT_DIR/seed-$1" spec name folder tile mode
  shift
  stop_app
  read_state "seed-base-$(basename "$stage")"
  mkdir -p "$stage"
  [ -f "$L" ] || return 1
  cp "$L" "$stage/layout.xml"
  if [ -f "$F" ]; then cp "$F" "$stage/folders.pb"; else : >"$stage/folders.pb"; fi
  for spec in "$@"; do
    IFS=: read -r name folder tile mode <<<"$spec"
    rm_py add-folder "$stage/folders.pb" "$stage/folders.pb" "$PAGE" "$folder" "$name" "$SEED_PKG" || return 1
    [ "$mode" = notile ] && continue
    rm_py add-tile "$stage/layout.xml" "$stage/layout.xml" "$PAGE" "$tile" "$folder" || return 1
  done
  CHANGED=1
  push_file "$stage/layout.xml" "$LAYOUT_FILE" && push_file "$stage/folders.pb" "$FOLDERS_FILE"
}

wait_for() {
  local seconds="$1" i
  shift
  for ((i = 0; i < seconds; i++)); do
    "$@" && return 0
    sleep 1
  done
  return 1
}

journal_empty() {
  pull_file "$JOURNAL_FILE" "$OUT_DIR/journal-now.xml"
  [ "$(rm_py journal-pending "$OUT_DIR/journal-now.xml")" = 0 ]
}

# #372: a death after the tile left the layout but before its folder was released.
check_recovery() {
  section "#372 recovery on next start"
  if [ "$RESTART_OK" != 1 ]; then skip "recovery needs an app restart: $RESTART_REASON"; return; fi
  seed recovery "JV Crashed:jv-crashed:jv-crashed-tile:notile" "JV Kept:jv-kept:jv-kept-tile" \
    || { fail "could not seed the crash state"; return; }
  rm_py journal "$OUT_DIR/journal-seed.xml" \
    "$PAGE" jv-crashed-tile jv-crashed "$PAGE" jv-kept-tile jv-kept
  push_file "$OUT_DIR/journal-seed.xml" "$JOURNAL_FILE" || { fail "could not write the journal"; return; }
  start_app || { fail "could not start $PKG"; return; }
  if wait_for 15 journal_empty; then pass "pending removals finished on start"
  else fail "removal journal still pending 15s after start"; fi
  read_state recovery-after
  if saved_lacks has-folder "$F" "$PAGE" jv-crashed; then pass "folder whose tile was already gone is deleted"
  else fail "folder with no tile was not deleted on recovery (or the folder store is unreadable)"; fi
  if saved_has has-folder "$F" "$PAGE" jv-kept && saved_has has-tile "$L" "$PAGE" jv-kept-tile; then
    pass "folder whose tile never left disk is kept, with its tile"
  else
    fail "recovery removed a folder or tile still on the Canvas (or the stores are unreadable)"
  fi
  check_no_dangling after-recovery "after recovery" 1
}

ui_dump_top() { ui_dump >/dev/null 2>&1; }

ui_shows() { ui_dump_top && rm_py has-text "$UI_XML" "$1"; }

# Taps the node labelled $1 on the focused display. Waits up to $2 seconds for it.
tap_label() {
  local seconds="${2:-5}" xy display
  wait_for "$seconds" ui_shows "$1" || return 1
  xy="$(rm_py center "$UI_XML" "$1")" || return 1
  display="$(top_display)"
  # shellcheck disable=SC2086
  sh_dev input -d "${display:-0}" tap $xy >/dev/null
  pause
}

classic_removed() {
  local folder="$1" tile="$2" label="$3"
  read_state "classic-$folder"
  if saved_lacks has-tile "$L" "$PAGE" "$tile"; then pass "$label: the folder's tile was removed"
  else fail "$label: the folder's tile is still on the Canvas (or the layout is unreadable)"; fi
  if saved_lacks has-folder "$F" "$PAGE" "$folder"; then pass "$label: the folder was deleted"
  else fail "$label: the folder is still stored (or the folder store is unreadable)"; fi
}

# #373: Classic's delete button asks first, and both delete paths take the tile along.
check_classic_ui() {
  section "#373 Classic folder delete"
  if [ "$CLASSIC_UI" != 1 ]; then skip "pass --classic-ui to drive Classic's folder sheet"; return; fi
  if [ "$RESTART_OK" != 1 ]; then skip "seeding needs an app restart: $RESTART_REASON"; return; fi
  seed classic "JV Delete:jv-delete:jv-delete-tile" "JV Hide:jv-hide:jv-hide-tile" \
    || { fail "could not seed Classic folders"; return; }
  start_app
  echo "  info  open Classic Canvas page $PAGE so JV Delete and JV Hide are on screen (waiting ${WAIT}s)"
  if ! wait_for "$WAIT" ui_shows "JV Delete"; then skip "JV Delete never appeared on screen"; return; fi
  if tap_label "JV Delete" && tap_label "Delete folder"; then
    if ui_shows 'Delete "JV Delete"?'; then pass "delete asks for confirmation first"
    else fail "no confirmation appeared after tapping Delete folder"; fi
    tap_label "Delete" && pause 2
    classic_removed jv-delete jv-delete-tile "delete button"
  else
    fail "could not open JV Delete's sheet or find its delete button"
  fi
  if tap_label "JV Hide" 10 && tap_label "Edit folder apps" && tap_label "Hide All"; then
    pause 2
    classic_removed jv-hide jv-hide-tile "Hide All"
  else
    fail "could not reach Hide All for JV Hide"
  fi
  check_no_dangling after-classic "after Classic deletes" 1
}

# #372: nothing is written during the undo window, so a force-stop there loses nothing.
check_jarn_undo_window() {
  section "#372 force-stop inside Jarn's undo window"
  if [ "$JARN_UI" != 1 ]; then skip "pass --jarn-ui to check a Jarn removal cut short"; return; fi
  if [ "$RESTART_OK" != 1 ]; then skip "seeding needs an app restart: $RESTART_REASON"; return; fi
  seed jarn "JV Undo:jv-undo:jv-undo-tile" || { fail "could not seed JV Undo"; return; }
  start_app
  echo "  info  in Jarn Canvas page $PAGE, remove the JV Undo tile now (waiting ${WAIT}s)"
  if ! wait_for "$WAIT" ui_shows "Tile removed"; then skip "the Tile removed bar never appeared"; return; fi
  stop_app
  pass "force-stopped inside the undo window"
  read_state jarn-stopped
  if saved_has has-tile "$L" "$PAGE" jv-undo-tile && saved_has has-folder "$F" "$PAGE" jv-undo; then
    pass "JV Undo and its folder are still saved"
  else
    fail "the undo window wrote a removal before it ended"
  fi
  start_app
  pause 3
  check_no_dangling after-jarn "after the Jarn restart" 1
  if journal_empty; then pass "no removal left pending"; else fail "removal journal still pending"; fi
}

check_lock() {
  section "Focus lock unchanged"
  local now
  now="$(focus_lock_value)"
  if [ "$now" = "$START_LOCK" ]; then pass "screen_focus_lock still $now"
  else fail "screen_focus_lock changed from $START_LOCK to ${now:-empty} (left as is, not repaired)"; fi
}

summary() {
  printf '\n== Summary\n  PASS %d   FAIL %d   SKIP %d\n  Output: %s\n' \
    "$PASS_COUNT" "$FAIL_COUNT" "$SKIP_COUNT" "$OUT_DIR"
  [ "$FAIL_COUNT" = 0 ]
}

main() {
  parse_args "$@"
  [ -n "$OUT_DIR" ] || OUT_DIR="$(mktemp -d -t jarnfe-removal)"
  mkdir -p "$OUT_DIR"
  UI_XML="$OUT_DIR/ui.xml"
  select_device
  text_matches "$(sh_dev pm path "$PKG")" '^package:' || { echo "$PKG is not installed" >&2; exit 2; }
  read_prefs canvas_layout_prefs >/dev/null || { echo "run-as is unavailable for $PKG (debug build needed)" >&2; exit 2; }
  read_restart_gate
  section "Audit"
  check_no_dangling as-found "as found" 0
  if [ "$RESTART_OK" != 1 ]; then
    skip "seeded checks need an app restart: $RESTART_REASON"
    check_lock
    summary
    return
  fi
  trap restore EXIT
  trap 'restore; exit 130' INT TERM
  backup || exit 2
  prepare_page || exit 2
  start_app
  check_recovery
  check_classic_ui
  check_jarn_undo_window
  restore
  check_lock
  summary
}

main "$@"
