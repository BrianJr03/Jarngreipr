#!/usr/bin/env bash
# Command Panel side of the font match checks (#452): the shared /fonts row, look switching through
# the debug broadcast, its prefs, and launch timing. Debug Command Panel only.
# Callers set PKG, CP_PKG, TOP_DISPLAY, STAGE_ACTIVITY, LIB_DIR and OUT_DIR.

CP_MAIN_ACTIVITY="jr.brian.commandpannel.MainActivity"
CP_DEBUG_ACTION="jr.brian.commandpannel.action.DEBUG_BACKGROUND"
CP_PROVIDER_CLASS="jr.brian.commandpannel.system.ThemeProvider"
CP_LOOK_KEY=overlay_look

cp_auth() { echo "$CP_PKG.theme"; }
cp_prefs_path() { echo "/data/user_de/0/$CP_PKG/shared_prefs/command_panel.xml"; }
cp_installed() { text_matches "$(sh_dev pm path "$CP_PKG")" '^package:'; }
cp_query_fonts() { adb shell content query --uri "content://$(cp_auth)/fonts" 2>&1 | tr -d '\r'; }

# The value of column $2 in `content query` output $1 ("Row: 0 a=1, b=NULL, ...").
cp_col() { grep -oE "(^|, | )$2=[^,]*" <<<"$1" | head -1 | sed -E "s/^(, | )?$2=//"; }

cp_prefs() { sh_dev run-as "$CP_PKG" cat "$(cp_prefs_path)"; }
cp_current_look() { cp_prefs | parse_pref "$CP_LOOK_KEY"; }
cp_prefs_tool() { cp_prefs | python3 -I "$LIB_DIR/cpprefs.py" "$@"; }

# Writes stdin over Command Panel's prefs file. The caller stops Command Panel first.
cp_write_prefs() {
  adb shell "run-as $CP_PKG sh -c 'cat > $(cp_prefs_path)'" || return 1
}

# The debug broadcast is handled by AssistantService, which the companion starts.
cp_ensure_service() {
  text_has "$(sh_dev dumpsys activity services "$CP_PKG")" AssistantService && return 0
  sh_dev am start -W --display "$TOP_DISPLAY" -n "$CP_PKG/$CP_MAIN_ACTIVITY" >/dev/null
  pause 3
  text_has "$(sh_dev dumpsys activity services "$CP_PKG")" AssistantService
}

# Applies look $1 (a built-in name or custom:<id>) and waits for the row to show it.
cp_set_look() {
  local want="$1" i
  sh_dev am broadcast -a "$CP_DEBUG_ACTION" -p "$CP_PKG" --es look "$want" >/dev/null
  for i in 1 2 3 4 5 6; do
    pause 0.8
    [ "$(cp_col "$(cp_query_fonts)" look_id)" = "$want" ] && return 0
  done
  return 1
}

# "display label" fonts Command Panel publishes for a built-in look (docs/SHARED_THEME.md).
cp_expected_fonts() {
  case "$1" in
    SEGMENT_LCD) echo "bundled:segment bundled:mono" ;;
    E_INK) echo "bundled:serif bundled:serif" ;;
    TERMINAL) echo "bundled:mono bundled:mono" ;;
    AURORA_GLASS) echo "NULL NULL" ;;
    *) return 1 ;;
  esac
}

cp_provider_component() { echo "$CP_PKG/$CP_PROVIDER_CLASS"; }

# 0 when the theme provider is enabled (as the manifest ships it).
cp_provider_enabled() {
  local disabled
  disabled="$(sh_dev dumpsys package "$CP_PKG" |
    awk '/disabledComponents:/ { on = 1; next } on && /^ *[a-zA-Z]+:/ { on = 0 } on')"
  ! text_has "$disabled" "$CP_PROVIDER_CLASS"
}

# The shell's own query after the provider is disabled: anything but a row counts as unreadable.
cp_fonts_unreadable() { ! text_matches "$(cp_query_fonts)" '^Row: '; }

# True when the installed Jarn APK carries the Command Panel font reader (#449). Detected from the
# cache folder name in its dex, since a debug build isn't minified. JARNFE_CP_READER=1|0 overrides.
jarn_reader_installed() {
  case "${JARNFE_CP_READER:-}" in 1) return 0 ;; 0) return 1 ;; esac
  local dir="$OUT_DIR/apk" path found=1
  mkdir -p "$dir"
  while read -r path; do
    path="${path#package:}"
    [ -n "$path" ] || continue
    adb pull "$path" "$dir/$(basename "$path")" >/dev/null 2>&1 || continue
    # grep -c reads to the end, so unzip never gets SIGPIPE under pipefail.
    if [ "$(unzip -p "$dir/$(basename "$path")" 'classes*.dex' 2>/dev/null | LC_ALL=C grep -ac 'cp_fonts')" -gt 0 ]; then
      found=0
    fi
  done <<<"$(sh_dev pm path "$PKG")"
  rm -rf "$dir"
  return "$found"
}

# Jarn's cached copy of an imported font, by its SHA-256 (#449).
jarn_cached_sha() { sh_dev run-as "$PKG" sha256sum "no_backup/cp_fonts/$1.ttf" | awk '{ print $1 }'; }

# Moves Jarn's Stage to the background by opening the Command Panel companion over it.
send_jarn_back() {
  sh_dev am start -W --display "${STAGE_DISPLAY:-$TOP_DISPLAY}" -n "$CP_PKG/$CP_MAIN_ACTIVITY" >/dev/null
  pause 2
}

# Starts Stage and prints "state total_ms displayed_ms". displayed_ms comes from the
# ActivityTaskManager "Displayed" line and is "-" when logcat has none (a hot start logs none).
timed_jarn_start() {
  local since out state total displayed
  since="$(sh_dev date +%s).000"
  out="$(sh_dev am start -W --display "$TOP_DISPLAY" -n "$PKG/$STAGE_ACTIVITY")"
  state="$(sed -n 's/^LaunchState: *//p' <<<"$out" | head -1)"
  total="$(sed -n 's/^TotalTime: *//p' <<<"$out" | head -1)"
  [ -n "$total" ] || total="$(sed -n 's/^WaitTime: *//p' <<<"$out" | head -1)"
  pause 1
  displayed="$(sh_dev logcat -d -T "$since" -s ActivityTaskManager:I ActivityManager:I |
    grep -F "Displayed $PKG/$STAGE_ACTIVITY" | tail -1 | parse_displayed_ms)"
  echo "${state:-?} ${total:--} ${displayed:--}"
}

# "Displayed pkg/act for user 0: +1s23ms" -> 1023
parse_displayed_ms() {
  sed -nE 's/.*: \+(([0-9]+)s)?([0-9]+)ms.*/\2 \3/p' | awk '{ if (NF == 2) print $1 * 1000 + $2; else print $1 }'
}

# The median of the numbers on stdin, ignoring anything that isn't one.
median() {
  grep -E '^[0-9]+$' | sort -n | awk '{ v[NR] = $1 } END { if (NR) print v[int((NR + 1) / 2)] }'
}
