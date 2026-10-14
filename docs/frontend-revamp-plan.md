# Jarngreipr Frontend Revamp — Plan

*Based on commit `6fe5321` (2026-10-14). Paths are relative to `app/src/main/java/jr/brian/home/` unless stated.*

> **Phase 0 spot-check note (2026-10-14, feature/frontend-revamp-phase-0):**
> The code wins over the plan where they disagree.
> - `esde/viewmodels/RomSearchViewModel.kt:refreshSystem` already wraps its launch in `try { … } finally { store.isLoading.value = false }` (lines 176–184 of the current file). The Phase-0 finally-wrap only still needs to be applied to `loadGames`, `refreshGames`, and `retryIfLastScanWasDegraded`.
> - All other Phase 0 claims were verified against the source at `6fe5321` and hold. The paths in §1's table and in §4 are correct once prefixed with `esde/` as updated below (the plan's original revisions dropped the subpackage).

## 1. Where we are

Jarngreipr started as a launcher, grew an ES-DE companion mode (wallpaper/music/scripts driven by an external ES-DE install), and then grew an in-app frontend on top of that companion plumbing. The result is one `:app` module where the frontend (`esde/`, 162 files, ~31k lines) and the launcher import each other in both directions (47 launcher files import `esde`; `esde` imports launcher theme, `ManagerContainer`, `MainViewModel`, `R`, `DisplayUtils`), and where the in-app frontend still treats the ES-DE folder tree as its source of truth.

### What's actually broken (verified in source)

| # | Problem | Where | Effect on the Thor |
|---|---|---|---|
| 1 | `isLoading` is reset *after* the scan, not in `finally`. The scan runs in `FrontEndActivity`'s `viewModelScope`, and `RomGameLauncher` calls `activity.finish()` after every launch. | `esde/viewmodels/RomSearchViewModel.kt:128-156` (plus `retryIfLastScanWasDegraded` at :584-593; `refreshSystem` is already in a try/finally), `esde/ui/RomGameLauncher.kt:133,299,327` | Launch a game while the library is still loading → the `@Singleton` flag is stuck `true` until process death. Every later load/refresh silently no-ops. "Loading forever" reports. |
| 2 | `RomIndexBuilder.buildForSystem` calls `RomScanner.scan(...)` over **every root and every SAF mapping**, then keeps one system's result. | `esde/util/RomIndexBuilder.kt:146-167` | A refresh is S systems × a full-library walk. Refresh also runs automatically after every scrape. |
| 3 | Art is resolved by brute-force `File.exists()`: roots × system aliases × basename candidates × with/without parent × 5-6 extensions, per media folder, done twice (gamelist source + fallback source). | `esde/util/RomPathUtils.kt:70-110`, `esde/util/CompositeMetadataSource.kt:23-34` | ~1,000 FUSE stats per art-less ROM. A 5k-ROM library is millions of syscalls per rebuild. |
| 4 | Every D-pad focus change does synchronous file I/O on the main thread (`ESDEViewModel.updateForGame` → `findFirstMedia` ×7, `findGame` linear scans ×2, video path lookup). Plus `ESDEEventManager` polls 4 files every 500 ms on the main looper for the life of `MainActivity`. | `esde/viewmodels/ESDEViewModel.kt:230-300`, `esde/data/ESDEEventManager.kt` | Browsing jank. |
| 5 | SAF walk uses `DocumentFile` (`listFiles` + `name` + `isDirectory` + `isFile` per child = ~3 IPCs per file, depth 6). | `esde/util/RomScanner.kt:234-285` | SD-card libraries scan very slowly. |
| 6 | Stamp = system dir mtime + *immediate* child count. Media folders and the media toggles are not in the stamp. `loadGames` never rescans once `allGames` is non-empty. | `esde/util/RomIndexBuilder.kt`, `esde/viewmodels/RomSearchViewModel.loadGames` | New ROMs in subfolders and freshly scraped art are invisible until a (slow) manual refresh, which also wipes the 100 MB Coil disk cache. |
| 7 | Scraper: ScreenScraper only runs if the user supplies **developer** credentials (`esde/scraper/ScraperCoordinator.kt:133`); HTTP/auth/quota errors collapse to `emptyList()` and count as "0 found"; no text metadata is ever written (`GameMetadataSidecarStore.write` has no callers); SteamGridDB takes the first autocomplete hit with no platform or dimension filter; account `maxthreads` and the persisted concurrency override are never used; full-file CRC32 of every ROM cached in SharedPreferences. | `esde/scraper/*` | "Scraping barely works." |
| 8 | ScreenScraper password and SteamGridDB key stored plaintext and **included in config export**. | `esde/data/ESDEPreferencesManagerScraper.kt`, `data/config/ImportExportManager.kt:315-326` | Users share configs with their passwords in them. |
| 9 | State is a 128-field `ESDEPrefsState` + a grab-bag `RomSearchStateHolder` with ~10 cross-activity `SharedFlow` signals coordinating three activities on two displays. Several `RomSearchViewModel`/`RomIndexCache` instances hold divergent snapshots. | `esde/model/ESDEPrefsState.kt`, `esde/data/RomSearchStateHolder.kt` | Hard to reason about; the "blanking"/"destroyed activity" comments in the code are symptoms. |
| 10 | ROM locations are configured in three places with different semantics (file roots with no persisted grant in System Apps; SAF mappings in Add Systems; per-system SAF tree only used at launch), plus a hardcoded `/storage/emulated/0/Roms` fallback and a SAF picker that can pop mid-launch. | `esde/ui/SystemAppsScreen.kt`, `esde/ui/frontend/settings/AddSystemsScreen.kt`, `esde/util/RomPathUtils.kt:114` | A new user has no single "add ROM folder" flow. |

### End-user judgement (what a Thor owner feels)

- **First run:** no setup flow in the frontend itself; empty state is a bare "No systems found" with no button. Settings and search are gamepad-only (START / Y) — touch users have no way in except long-press.
- **Browsing:** every tile focus does disk work; jingles, music, video scheduling and konfetti all hang off the same focus event.
- **Search:** keyboard on the bottom screen, results in a *third* activity on top, sheets forced to the bottom screen because the top window's hidden system bars break `ModalBottomSheet`. If the launcher isn't running on the bottom, search simply doesn't work.
- **Settings sprawl:** frontend rail (5 categories), per-system customization (4 categories, 1,015 lines), `RomSearchSettingsScreen`, `ESDEDisplaySection` in launcher settings (772 lines), `ESDESettingsScreen` with 9 sections, `SystemAppsScreen`. Six places.
- **Back does nothing** on the Systems screen (kiosk). `excludeFromRecents` + no visible exit.
- **No sort, filter, favorites or recents** in the frontend even though `isFavorite`, `lastPlayed`, `playCount` are parsed.
- **Scraping** asks for a "Dev ID / Dev password" that normal users can't get, and reports failure as "nothing found".

### What's worth keeping

- `RomScanner`'s rules (extension sets, directories-as-files, skip lists, depth bound) — not its SAF walk.
- `RomMetadataSource` / `CompositeMetadataSource` / `GamelistMetadataSource` (parser + parse cache) / `UserOverrideMetadataSource`.
- `RomIndexCache` + `SystemStamp` + `RootAvailability` (unmounted-SD protection is good and tested) — as a design, re-homed into a repository.
- `EmulatorRegistry` rows + `RomLaunchContract`, `RomGameLauncher`'s URI/grant/retry logic, `EsdeCommandLauncher.buildIntent` (as an optional adapter).
- Volume/SAF helpers (`splitTreeDocId`, `documentIdToStoragePath`, `storagePathToDocumentId`), `mediaBasenameCandidates`.
- `planHomeLaunches` (pure, tested), `FrontendTile`'s fallback chain, `FrontendTheme` tokens, `FocusableTileLayout`'s ideas (not its `repeat(3){delay(80)}` retries).
- The 45 existing unit tests.

---

## 2. What NeoStation does that we should borrow

Looked at `misobadev/neostation-frontend` (Flutter, GPL-3, runs on the Thor).

| Idea | Why it matters for us |
|---|---|
| **ES-DE is an optional import step, not a dependency.** `esde_import_service.dart` reads `gamelist.xml` once into its own DB. The frontend owns its library. | This is the single biggest architectural change we need. |
| **Config-driven systems + emulators** (`assets/systems/<sys>.json`: id, name, folder aliases, extensions, ScreenScraper/RA ids, emulator list with Android launch args using `{file.path}` / `{file.localuri}` placeholders, `keep_saf_uri` opt-out). Maintainer rule: fix launches in JSON, keep the Kotlin launcher emulator-agnostic. | Replaces our 110 hardcoded `EmulatorSpec` rows + `es_systems.xml` parsing + `ESDEMediaConstants` aliases. Community can PR fixes without touching Kotlin. |
| **SQLite is the single store**; UI observes it. | We already ship Room. Replaces JSON caches, 128-field prefs object, SharedPreferences hash cache. |
| **Native fast-walk** when on the primary volume with all-files access (one `java.io.File` walk, sorted, cycle-safe), with a SAF fallback and a "don't trust an empty fast result" guard. | Direct answer to our slow-scan problem. |
| **Hash-first ScreenScraper matching** (`crc`/`md5`/`romtaille` + `romnom`), per-account `maxthreads` semaphore, 429 handling, app-shipped dev creds + user account. | Direct answer to "scraping barely works". |
| **Separate secondary-display surface** (`SecondaryScreen`: now-playing, achievements, app dock) with the hard-won rules in `android/CLAUDE.md` (bottom screen is a separate window; `FLAG_NOT_FOCUSABLE`; swallow BACK; two-phase restore after a dock-launched app). | We already have the equivalent split (MainActivity bottom / FrontEndActivity top), and we hit the same bugs. Their notes on focus and BACK are worth reading before touching the swap feature. |
| Setup wizard: permissions → folder → scan → optional ES-DE import → optional art pack. | Our first-run story. |

Things *not* to copy: Flutter, cloud sync, the 2,248-line `MainActivity.kt`.

---

## 3. Target architecture

### 3.1 Modules

```
settings.gradle.kts
  :app                 launcher (bottom screen). Depends on :frontend, :core-ui, :core-model.
  :frontend            NEW. Standalone game frontend (top screen). Android library,
                       namespace jr.brian.frontend. Depends on :core-ui, :core-model.
                       Does NOT depend on :app.
  :core-ui             theme (LauncherTheme, Color.kt getters), animations, gesture/key
                       modifiers, focus utils, common components (ScreenHeader, BackButton,
                       AppBottomSheet, DimmedDialog, QwertyKeyboard, settings rows that
                       both sides use). Namespace jr.brian.core.ui.
  :core-model          GameInfo-successor models, LibrarySource, SystemDef, display
                       routing (HomeTarget, MainScreen, planHomeLaunches), and the
                       FrontendBus contract (§3.4). Pure Kotlin + minimal Android.
  :esde-companion      (later, optional) the external-ES-DE mode: ScriptManager,
                       ESDEEventManager, wallpaper container, MusicManager. Depends on
                       :core-*. Can be deleted or kept as a feature flag.
```

Rules:
- `:frontend` never imports `jr.brian.home.*`. If it needs something from the launcher, that thing moves to `:core-*` or goes through the bus.
- `:app` reaches `:frontend` only through `FrontendEntry` (launch intent builder), `FrontendBus`, and `LibraryRepository` (read-only for pinned ROMs / canvas).
- Each library module declares the `mode` flavor dimension with `missingDimensionStrategy("mode", "standard")` so `testStandardDebugUnitTest` / `lintStandardDebug` keep working; CI gets `:frontend:testDebugUnitTest` added.
- `android.nonTransitiveRClass=true` means `:frontend` gets its own `strings.xml`/drawables; move the ~250 frontend strings out of the 1,900-line app `strings.xml`.

### 3.2 `:frontend` internals

```
jr.brian.frontend
├── library/                     "the engine" — no Compose, fully unit-testable
│   ├── db/            Room: SystemEntity, GameEntity, MediaEntity, SourceEntity,
│   │                  ScrapeJobEntity, PlayHistoryEntity. Flow-based DAOs.
│   ├── systems/       SystemCatalog: loads assets/systems/*.json (NeoStation-style schema,
│   │                  our own content). Folder alias → system id. Emulator defs.
│   ├── sources/       LibrarySource = SAF tree URI (+ cached display path, volume id,
│   │                  persisted grant). One model, one screen. Replaces romsPaths /
│   │                  SAF tree map / SystemFolderMapping.
│   ├── scan/          Scanner: per-source, per-system. FastWalk (java.io.File, primary
│   │                  volume + all-files access) with SAF fallback using
│   │                  DocumentsContract.buildChildDocumentsUriUsingTree + projection
│   │                  (1 query per directory, not 3 per file). Emits ScanEvent progress.
│   │                  Stamp includes media dirs. Diff against DB, upsert, prune
│   │                  (RootAvailability rule preserved: never prune an unmounted source).
│   ├── media/         MediaIndex: one listFiles() per media folder → basename→path map.
│   │                  MediaLayout interface with two impls: EsdeLayout
│   │                  (downloaded_media/<sys>/covers|marquees|...), JarngreiprLayout
│   │                  (app-owned media dir written by the scraper). Resolution happens
│   │                  once at index time and is stored in MediaEntity.
│   ├── metadata/      Keep RomMetadataSource/Composite/Gamelist/UserOverride. Gamelist
│   │                  becomes an *import* that writes to GameEntity, not a per-scan join.
│   ├── scrape/        ScreenScraper + SteamGridDB clients, Matcher, ScrapeWorker
│   │                  (WorkManager, foreground notification, per-system jobs).
│   ├── launch/        GameLauncher: SystemCatalog emulator defs → intent; keeps the
│   │                  current URI/grant/retry logic; EsdeCommandAdapter optional.
│   └── LibraryRepository (app-scoped @Singleton, own SupervisorJob scope; every
│                       loading flag set in `finally`; exposes StateFlow<LibraryState>)
├── ui/
│   ├── FrontendActivity           host; owns a NavHost (real back stack, no boolean
│   │                              overlay flags); handles standalone vs. paired mode
│   ├── theme/                     FrontendTokens (kept), 10-foot type scale
│   ├── input/                     one GamepadNav layer stack: focus ownership per route,
│   │                              B=back everywhere, START=settings, Y=search, X=sort
│   ├── screens/
│   │   ├── setup/                 wizard: permissions → add source → detect systems →
│   │   │                          (optional) import ES-DE → (optional) scrape
│   │   ├── home/                  Systems (grid or row), hero backdrop, collections row
│   │   │                          (Favorites / Recent / All), empty state with CTA
│   │   ├── games/                 game grid/list + details pane; sort/filter chip bar;
│   │   │                          letter jump; skeletons from DB, never a blank screen
│   │   ├── detail/                art, description, play stats, Launch / Emulator /
│   │   │                          Favorite / Scrape this / Edit info — all touchable
│   │   ├── search/                in-window overlay (on-screen keyboard rendered in the
│   │   │                          top window; delegates to bottom-screen keyboard only
│   │   │                          when paired)
│   │   └── settings/              ≤5 groups: Library · Appearance · Input · Scraping ·
│   │                              Dual-screen
│   └── components/                tiles, FocusableGrid (rewritten, no delay-retry),
│                                  sheets implemented as in-window overlays (fixes the
│                                  hidden-system-bars ModalBottomSheet trap)
└── bus/  FrontendBusImpl (see 3.4)
```

### 3.3 Standalone vs. paired

`FrontendActivity` works with **no launcher running**. It detects pairing via `FrontendBus.launcherPresent: StateFlow<Boolean>` (set by `MainActivity` onStart/onStop — replaces the static `FrontEndActivity.isRunning` flag in the other direction).

| Capability | Standalone (frontend alone, any screen) | Paired (launcher on the other screen) |
|---|---|---|
| Search keyboard | In-window overlay | Optional: bottom-screen keyboard (user setting, default on for Thor) |
| Companion-app picker, Edit info | In-window overlay | Same (no more forcing them to the bottom) |
| Settings | In-window | In-window |
| Back on Home | Opens an exit sheet: *Swap screens / Launcher / Exit* | Same |
| Focused-game art | n/a | Published on the bus for the widget/wallpaper |

### 3.4 Launcher ↔ frontend contract (`:core-model`)

Replace `RomSearchStateHolder` (10 signals) + `FrontendSelectionStateHolder` + `WallpaperStateHolder` + `FrontEndActivity.isRunning` with one typed bus:

```kotlin
interface FrontendBus {
  // frontend → launcher
  val focus: StateFlow<FrontendFocus?>        // System(id, name, art) | Game(id, title, system, art: ResolvedArt)
  val events: SharedFlow<FrontendEvent>       // GameLaunched, GameEnded, ScreensaverStarted/Ended
  // launcher → frontend
  val commands: SharedFlow<FrontendCommand>   // ShowGame(id), ShowSystem(id), Search(query), SwapScreens
  // presence
  val launcherPresent: StateFlow<Boolean>; val frontendPresent: StateFlow<Boolean>
}
data class ResolvedArt(val cover: Path?, val marquee: Path?, val fanart: Path?, val screenshot: Path?, val titlescreen: Path?, val miximage: Path?, val video: Path?)
```

`ResolvedArt` comes straight from `MediaEntity` — the widget and wallpaper stop re-probing disk, and user overrides are honoured automatically. The external-ES-DE event path (if kept) becomes a second *producer* of `focus`, with a simple rule: whichever producer wrote last wins, and the in-app frontend producer takes priority while `frontendPresent`.

### 3.5 The widget (`FrontendTile`) — preserved

- `CanvasItem.EsdeArtItem` keeps its `@SerialName("esde_art")` and `GameImageType` enum values so existing canvas layouts load unchanged. `GameImageType` moves to `:core-model`.
- `FrontendTile` reads `FrontendBus.focus` instead of `LocalEsdeWallpaperState`; same sticky-last-path behaviour, same gradient fallback, same tap → detail.
- `PinnedRomManager` stores `gameId` and re-resolves art from `LibraryRepository` on display, so pinned tiles stop going stale after a scrape.
- `CanvasFrontendArtChooserSheet` unchanged except for the data source.

### 3.6 Screen swap with animation

Goal: a single action moves the launcher to the top screen and the frontend to the bottom (or back), from either side.

Mechanics (Android can't animate a window across displays, so we choreograph a hand-off):
1. `SwapCoordinator` (in `:core-model`, `@Singleton`) publishes `SwapPhase: Idle → Leaving → Arriving → Idle`.
2. Initiator emits `FrontendCommand.SwapScreens`. Both activities play a ~180 ms **leave** animation (scale 0.92 + fade, content only; wallpaper stays) while `Leaving`.
3. Each side calls `planHomeLaunches` with the inverted `ScreenAssignment` (new persisted pref: `frontendDisplay = TOP|BOTTOM`, replacing the hardcoded `PRIMARY_DISPLAY_ID` assumption) and starts the *other* activity on its own display with `ActivityOptions.setLaunchDisplayId` + `makeCustomAnimation(enter=pop_in, exit=none)`, then finishes itself with no exit transition (current behaviour).
4. Arriving activities read `SwapPhase.Arriving` and play the mirrored **enter** animation, then set `Idle`.
5. `HomeRouterActivity`, `HomeInterceptorService` double/long-press, and `MainActivity.launchFrontendIfEnabled` all consult `ScreenAssignment` instead of `0`, so the swap sticks across Home presses and reboots.
6. Guard: refuse to swap while a game is launching or `SwapPhase != Idle`; single display → the command just finishes the frontend and brings the launcher forward.

Triggers: Back-on-Home sheet in the frontend; a *Swap screens* row in `DrawerOptionsDialog`/`CanvasMenuDialog`; configurable gamepad chord (default L3+R3) in Input settings; optional Home triple-press.

### 3.7 Scraper

- **Credentials:** ship Jarngreipr's own ScreenScraper dev ID/password via `BuildConfig` from a CI secret (`SCREENSCRAPER_DEV_ID/PASSWORD`, like NeoStation). Users enter their *user* account (needed for quota). SteamGridDB stays user-key. Store both via Android Keystore-wrapped values in DataStore; **never** in config export.
- **Matching:** CRC32 (+ MD5 for ≤64 MB files) computed in a `scan`-time background pass and stored in `GameEntity`; name query stripped of `(Region)`/`[tags]`; system id from `SystemCatalog`; region preference (us/eu/jp/world) applied in media selection; confidence threshold for name-only matches with a "needs review" state.
- **Concurrency:** `maxthreads` from `ssuserInfos`; semaphore built once; 429/430/431 surfaced as *Quota reached — resumes at HH:MM*; per-game errors recorded in `ScrapeJobEntity` and shown in a summary.
- **Output:** text metadata → `GameEntity`; media → `Jarngreipr Media/<system>/<kind>/<basename>.<ext>` (`JarngreiprLayout`); DB rows updated immediately so art appears without a refresh; Coil cache evicted per-key, never wholesale.
- **SteamGridDB:** `dimensions=600x900` for covers, `styles=alternate,white_logo` for logos, hero for fanart; pick by platform name similarity, not first hit.
- **Execution:** `ScrapeWorker` via WorkManager with foreground notification; survives the frontend being finished by a game launch.

### 3.8 Performance budget (what "fast" means)

- Cold open with a warm DB: first frame with real tiles **< 300 ms** (DB read, no file I/O).
- Focus change: **zero** disk I/O on the main thread; art via Coil from `MediaEntity` paths.
- Incremental scan of one changed system: proportional to that system's file count only.
- 5,000-ROM primary-volume library full scan: target **< 10 s** via fast-walk; SAF SD card: one query per directory.
- Scrape: bounded by ScreenScraper quota, not by us.

---

## 4. Migration plan

### Phase 0 — Stop the bleeding (1 PR, ships in 2.9.x, before any module work)
Low-risk fixes in the existing code that users feel immediately:
1. `finally { store.isLoading.value = false }` in `loadGames`, `refreshGames`, `retryIfLastScanWasDegraded`; move the scan to an app-scoped scope so a game launch doesn't cancel it.
2. `buildForSystem`: scan only `systemName`'s folder(s) (add a `systems: Set<String>` filter to `RomScanner.scan`).
3. `ESDEViewModel.updateForGame/updateForSystem`: use the indexed `GameInfo` paths; move residual I/O to `Dispatchers.IO`.
4. `ESDEEventManager`: poll on a background `HandlerThread`, and only when ES-DE companion mode is on.
5. Remove scraper secrets from `ImportExportManager`.
6. Surface scraper HTTP errors in `ScrapeState`.

### Phase 1 — Scaffolding (no behaviour change)
- Create `:core-model`, `:core-ui`, `:frontend` (empty), wire flavors/CI.
- Move theme, animations, extensions, focus utils, shared components, `GameImageType`, `HomeTarget`/`MainScreen`/`planHomeLaunches`, `DisplayUtils` into `:core-*`. Fix imports. Tests move with code.
- Add `FrontendBus` to `:core-model` with an adapter that bridges the *existing* state holders, so `:app` can switch consumers one at a time.

### Phase 2 — Library engine
- Room schema, `SystemCatalog` + `assets/systems/*.json` (seed from `EmulatorRegistry` + `ESDEMediaConstants` + `SystemIdMap`), `LibrarySource`, scanner with fast-walk + SAF projection, `MediaIndex`, `LibraryRepository`.
- One-time migration: `rom_index/*.json` + `esde_prefs` (roms paths, SAF trees, folder mappings, hidden systems/games, per-game emulator/core/companion maps, system order/customizations) → DB.
- ES-DE import service (gamelist.xml → `GameEntity`).
- Unit tests for scanner diff, stamp, media index, catalog parsing, migration.
- Behind a hidden flag, `RomSearchViewModel` can read from the new repository to validate parity on real devices.

### Phase 3 — New frontend UI
- `FrontendActivity` in `:frontend` with NavHost, gamepad layer stack, setup wizard, Home/Games/Detail/Search/Settings, in-window overlays. Standalone mode first; paired mode second.
- Launcher switches `FrontendTile`, wallpaper, pinned ROMs, canvas sheets, `DrawerOptionsDialog`, `HomeRouterActivity`, `HomeInterceptorService` to the bus + repository.
- Old `esde.ui.FrontEndActivity` / `RomSearchResultsActivity` kept behind "Legacy frontend" toggle for one release, then deleted.

### Phase 4 — Scraper
- As in §3.7. Ship dev creds via CI secret. Scrape summary UI. Delete `HashCache`, sidecar stores, old coordinator.

### Phase 5 — Screen swap + dual-screen polish
- `ScreenAssignment` pref, `SwapCoordinator`, animations, triggers. Read NeoStation's `android/CLAUDE.md` notes on bottom-window focus/BACK before implementing.
- Update `HomeLaunchPlannerTest` for the assignment parameter.

### Phase 6 — Cleanup
- Move external-ES-DE companion mode (`ScriptManager`, `ESDEEventManager`, `ESDEWallpaperContainer`, `MusicManager`, `SetupWizardDialog`, the 9 `ui/sections`) to `:esde-companion` or delete per decision below.
- Delete `jr.brian.home.esde`, stale ProGuard rules (`esde.widget.OverlayWidget`, `esde.preferences.**`, `esde.wallpaper.WallpaperState`, `esde.events.**`), the `ESDEDisplaySection`/`ESDESettingsContent` launcher screens, the "experimental" dialog.
- Collapse `ESDEPrefsState` — anything not migrated to DB or a typed DataStore proto is dropped.

Each phase is independently shippable; 0 → 1 → 2 can land without users noticing, 3 is the visible change.

---

## 5. Decisions needed

1. **ScreenScraper dev credentials** — register a Jarngreipr dev account and ship it via CI secret (recommended; it's how every frontend does it), or keep asking users for dev creds (scraping stays broken for most)?
2. **Keep external-ES-DE companion mode?** It's the wallpaper/music/scripts path for people who run real ES-DE on top. Options: keep as `:esde-companion` feature flag (more code to carry), or drop it and let the new frontend be the only top-screen experience (simpler, but a breaking change for those users).
3. **Room vs. keep JSON caches** — plan assumes Room (already a dependency, gives Flow-backed UI and incremental updates). Say if you'd rather not.
4. **Default screen assignment** — frontend on top by default (today's behaviour), swap persisted per user?
5. **Phase 0 first, or straight to the rewrite?** Phase 0 is ~1 day and fixes the stuck-loading bug for current users while the rewrite happens on a branch.
6. **Naming** — `:frontend` / `jr.brian.frontend`, or give it a product name (it'll have its own launcher icon in standalone mode).

## 6. Risks

- **Persisted-data compatibility:** canvas `esde_art` items, `JarngreiprConfig` import/export, pinned ROMs, `esde_prefs` keys. Mitigated by the Phase 2 migration + keeping serial names.
- **CI/versioning is hard-wired to `app/`** (version grep, APK paths, flavor task names). Phase 1 touches every workflow.
- **Thor-specific display assumptions** (display 0 = top, bottom = first non-zero) are now a user setting; needs testing on single-screen devices and on the Thor with Mjolnir.
- **Scope:** `esde/` is 31k lines. The plan deliberately rebuilds the engine and the UI and *keeps* the launcher's canvas/widget surface, so the launcher side changes are mostly import swaps.
