#!/usr/bin/env bash
# Prints the wiki's Downloads page, built from every release's asset download counts.
# Usage: scripts/wiki-downloads.sh [owner/repo]   (needs gh and jq, authenticated or with GH_TOKEN)
# The last line is the "Updated" stamp, so callers can compare everything above it.
set -euo pipefail

repo="${1:-${GITHUB_REPOSITORY:-BrianJr03/Jarngreipr}}"
today="$(date -u +%Y-%m-%d)"

# Hidden APKs end in h.apk, h-rc.apk, or the older -rch.apk. Prereleases and -rc tags are RCs.
gh api "repos/$repo/releases" --paginate --jq '.[] | select(.draft | not) | {
  tag: .tag_name,
  name: (.name // ""),
  date: ((.published_at // .created_at)[0:10]),
  rc: (.prerelease or (.tag_name | test("-rc"))),
  total: ([.assets[].download_count] | add // 0),
  hidden: ([.assets[] | select(.name | test("(h|h-rc|-rch)\\.apk$")) | .download_count] | add // 0)
}' | jq -rs --arg repo "$repo" --arg today "$today" -f /dev/fd/3 3<<'JQ'
def commas: tostring | until(test("^[0-9]{4}") | not; sub("^(?<a>[0-9]+)(?<b>[0-9]{3})"; "\(.a),\(.b)"));
def pct($n; $d): if $d == 0 then "0%" else "\((($n * 1000 / $d) | round) / 10)%" end;
def bar($n; $max): ($n * 20 / ([$max, 1] | max) | ceil) as $w | ("█" * $w) + ("░" * (20 - $w));
def link: "[\(.tag)](https://github.com/\($repo)/releases/tag/\(.tag))";
def days($from): (($today + "T00:00:00Z" | fromdate) - ($from + "T00:00:00Z" | fromdate)) / 86400 | floor;
def badge($label; $value; $color): "<img src=\"https://img.shields.io/badge/\($label | gsub(" "; "%20"))-\($value | gsub(","; "%2C"))-\($color)?style=for-the-badge\" alt=\"\($label): \($value)\"/>";
# Some early betas were tagged v8.0.x and v9.0.x for 0.8 and 0.9, so the release name wins.
def series: ((.name | capture("^v?(?<m>[0-9]+)\\.")?) // (.tag | capture("^v?(?<m>[0-9]+)"))).m;

map(. + {standard: (.total - .hidden)}) as $all
| ($all | map(.total) | add // 0) as $total
| ($all | map(.hidden) | add // 0) as $hidden
| ($total - $hidden) as $standard
| ($all | length) as $count
| ($all | map(.total) | max // 0) as $max
| ($all | sort_by(-.total)) as $ranked
| ($ranked[0]) as $top
| ($all | map(select(.rc | not)) | .[0]) as $latest
| ($all | map(.total) | sort) as $sorted
| ($sorted | if length == 0 then 0 elif length % 2 == 1 then .[length / 2 | floor] else (.[length / 2 - 1] + .[length / 2]) / 2 | floor end) as $median
| ($all | group_by(series) | map({series: (.[0] | series), total: (map(.total) | add), releases: length}) | sort_by(-(.series | tonumber))) as $bySeries
| ($all | map(select(.rc)) | map(.total) | add // 0) as $rcTotal
| [
    "<div align=\"center\">",
    "",
    "# Jarngreipr Downloads",
    "",
    badge("total downloads"; $total | commas; "4F8FD0") + " " + badge("releases"; $count | tostring; "C46A2A") + " " + badge("most downloaded"; $top.tag; "2EA043"),
    "",
    "_Every APK downloaded from [GitHub releases](https://github.com/\($repo)/releases)._",
    "",
    "</div>",
    "",
    "## Highlights",
    "",
    "| | |",
    "|---|---|",
    "| **Most downloaded** | \($top | link) with **\($top.total | commas)** (\(pct($top.total; $total)) of all downloads) |",
    "| **Latest release** | \($latest | link), \($latest.total | commas) downloads since \($latest.date), about **\(($latest.total / ([days($latest.date), 1] | max)) | floor | commas)/day** |",
    "| **Average per release** | \(($total / ([$count, 1] | max)) | floor | commas) (median \($median | commas)) |",
    "| **Standard vs hidden** | \($standard | commas) standard (\(pct($standard; $total))), \($hidden | commas) hidden (\(pct($hidden; $total))) |",
    "| **Release candidates** | \($rcTotal | commas) downloads across \($all | map(select(.rc)) | length) RCs |",
    "| **First release** | \($all[-1] | link) on \($all[-1].date) |",
    "",
    "## Top 5",
    "",
    "| # | Release | Date | Downloads | |",
    "|---|---|---|---:|---|",
    ($ranked[0:5] | to_entries[] | "| \(.key + 1) | \(.value | link) | \(.value.date) | **\(.value.total | commas)** | `\(bar(.value.total; $max))` |"),
    "",
    "## By version",
    "",
    "| Version | Releases | Downloads | Share |",
    "|---|---:|---:|---|",
    ($bySeries[] | "| **\(.series).x** | \(.releases) | \(.total | commas) | `\(bar(.total; $bySeries | map(.total) | max))` \(pct(.total; $total)) |"),
    "",
    "## Every release",
    "",
    "<details>",
    "<summary><b>Show all \($count) releases</b></summary>",
    "",
    "| Release | Date | Total | Standard | Hidden | |",
    "|---|---|---:|---:|---:|---|",
    ($all[] | "| \(link)\(if .rc then " (RC)" else "" end)\(if .tag == $top.tag then " (most)" else "" end) | \(.date) | **\(.total | commas)** | \(.standard | commas) | \(.hidden | commas) | `\(bar(.total; $max))` |"),
    "",
    "</details>",
    "",
    "_Updated \($today) by the Wiki Downloads workflow._"
  ]
| .[]
JQ
