package jr.brian.home.data.config

import kotlinx.serialization.Serializable

@Serializable
data class JarngreiprConfig(
    val version: Int = CONFIG_VERSION,
    val exportedAt: String = "",
    val ui: UiConfig = UiConfig(),
    val app: AppConfig = AppConfig(),
    val page: PageConfig = PageConfig(),
    val feature: FeatureConfig = FeatureConfig(),
    val system: SystemConfig = SystemConfig()
) {
    companion object {
        // v2: added page.canvasLayouts for Unified Canvas pages.
        // v3: CanvasLayout split into shared content + per-orientation
        //     arrangements; v2 blobs decode with empty arrangements and are
        //     auto-repaired by CanvasLayoutManager.repair on apply.
        // v5: added CanvasItem.RssMusicItem variant. Older blobs decode
        //     unchanged (no rss_music entries); new blobs with rss_music are
        //     unreadable by pre-v5 builds, so the version bump signals that.
        // v6: added RomSearchConfig.systemCustomizations + systemOrder for per-system
        //     frontend tile customization (background, name overlay toggle, color, order).
        // v7: added RomSearchConfig.frontendHintsVisible for the bottom hint-row toggle.
        // v8: added RomSearchConfig.frontendFloatIntensity for the floating-animation slider.
        // v9: added RomSearchConfig.canvasContinuousSpinRoms for per-rom continuous-spin toggle.
        // v10: added RomSearchConfig.gameMediaMap + systemMediaMap for per-game and per-system
        //      card media type overrides.
        // v11: added RomSearchConfig.frontendFocusHapticEnabled for the frontend focus-tick
        //      haptic toggle (applies to both system and game cards).
        // v12: added SystemConfig.hiddenSystems for the Frontend "Filter Systems" sheet.
        // v13: added CanvasLayout.menuButtonVisible for per-page toggle of the inline
        //      menu tile. Older blobs decode with the default (true) via
        //      ignoreUnknownKeys, matching the pre-toggle behavior.
        // v14: added UiConfig.promptForDisplayOnLaunch — set of package names that
        //      surface a display chooser on tap. Older blobs decode with the empty
        //      default (no prompting), matching pre-toggle behavior.
        // v15: added RomSearchConfig.frontendFocusBackgroundSystems and
        //      frontendFocusBackgroundGames per-route toggles for the focus-background
        //      feature; SystemCustomization.focusBackgroundUri for a per-system full-screen
        //      backdrop distinct from the tile-level backgroundUri. Older blobs decode
        //      with defaults (both routes on, no focus URI) via ignoreUnknownKeys.
        // v16: added RomSearchConfig.frontendTransition + frontendTransitionMs for the
        //      Systems <-> Games route transition preset and its duration. Older blobs
        //      decode with the default preset ("Fade") and default 280ms via
        //      ignoreUnknownKeys.
        // v17: added CanvasItem.EsdeArtItem.backgroundColorArgb + backgroundCornerRadiusDp
        //      for the optional background-box customization on ES-DE Display tiles.
        //      Older blobs decode with both fields null (no background) via
        //      ignoreUnknownKeys, preserving the pre-toggle transparent rendering.
        // v18: split RomSearchConfig.frontendFocusBackgroundDim into
        //      frontendFocusBackgroundDimSystems / frontendFocusBackgroundDimGames so
        //      the two routes can be dimmed independently. The old field is kept
        //      (deprecated) and still written on export so v17-and-earlier builds can
        //      still read a config exported now; import falls back to it for pre-split
        //      configs.
        // v19: added RomSearchConfig.frontendSystemRowAlignment /
        //      frontendGameRowAlignment (row-mode vertical anchor: Top/Center/Bottom)
        //      and frontendSystemTileScale / frontendGameTileScale (tile-size
        //      multiplier 1.0..1.3 controlling row width and grid columns). Older
        //      blobs decode with defaults (Center, 1.0f) via ignoreUnknownKeys.
        // v20: added RomSearchConfig.gamelistDecorationEnabled — toggle for
        //      whether ES-DE gamelist.xml decorates the filesystem scan. Older
        //      blobs decode with the default (true) via ignoreUnknownKeys,
        //      preserving pre-toggle behaviour.
        // v21: added FeatureConfig.homeButton for the "Home Button Interception"
        //      toggle. Older blobs decode with the default (off) via
        //      ignoreUnknownKeys — matching the pre-toggle behaviour of a device
        //      that has never had interception enabled.
        // v22: added SystemConfig.systemFolderMappings for the Frontend "Add
        //      Systems" screen — user-declared "this folder is system X"
        //      entries used to map arbitrary folders (including SD-card ones)
        //      to systems. Older blobs decode with the empty default via
        //      ignoreUnknownKeys, matching pre-feature behaviour.
        // v23: added HomeButtonConfig.homeTarget / mainScreen for the display
        //      target selector under Home Button Interception. Older blobs
        //      decode with both fields null via ignoreUnknownKeys — a null
        //      target means "use the runtime migration default"
        //      (BOTH-when-frontend-enabled, else BOTTOM), matching the
        //      pre-selector routing behaviour.
        // v24: added PowerSettingsConfig.pagerSwipeSensitivity for the tab-
        //      swipe sensitivity slider (0..100). Older blobs decode with
        //      the default (0 = Compose default snap threshold) via
        //      ignoreUnknownKeys, matching pre-slider behaviour.
        // v25: added RomSearchConfig.scraper (ScraperConfig) — ScreenScraper
        //      + SteamGridDB credentials, per-backend enable toggles, media-
        //      type selections, and an optional concurrency override. Older
        //      blobs decode with ScraperConfig() defaults (both backends off,
        //      empty credentials) via ignoreUnknownKeys.
        // v26: added RomSearchConfig.userMetadata (UserMetadataConfig) — a
        //      master toggle for the Edit Info sheet's manual overrides.
        //      The override blobs themselves live on disk at
        //      `<scrapedRoot>/<system>/.jarngreipr_user/` and are not inlined
        //      in config. Older blobs decode with UserMetadataConfig() default
        //      (enabled = true) via ignoreUnknownKeys.
        // v27: HomeButtonConfig.homeTarget is now ignored on import and no
        //      longer written on export — Home press routing is driven by
        //      gestures (single/double/long press) instead of a stored target.
        //      The field is retained in the schema with its null default so
        //      older blobs still decode via ignoreUnknownKeys; the value is
        //      simply never consulted.
        // v28: added HomeButtonConfig.focusLockAutoManageEnabled — the
        //      "Auto-manage Focus Lock" toggle gating whether we write to
        //      AYN's screen_focus_lock on swaps, app launches, game
        //      launches, and boot. Older blobs decode with the default
        //      (true) via ignoreUnknownKeys, matching the on-by-default
        //      behaviour new installs ship with.
        // v29: split RomSearchConfig.gameLayout into
        //      primaryGameLayout / secondaryGameLayout so each display
        //      can show games in Row or Grid independently (matches the
        //      systems split from v26-ish). Legacy gameLayout retained
        //      in the schema and still written on export so pre-split
        //      builds restoring a current config get a sensible single-
        //      layout fallback. Older blobs decode with both new fields
        //      inheriting the legacy value on import.
        // v30: dropped HomeButtonConfig.focusLockAutoManageEnabled
        //      (added in v28). AYN's SettingsProvider gates
        //      `screen_focus_lock` behind a MOVED_TO_SECURE-style
        //      whitelist that third-party apps can't bypass, so the
        //      feature is removed. Older blobs that still carry the
        //      field decode via `ignoreUnknownKeys`; the value is
        //      ignored on import.
        // v31: re-added HomeButtonConfig.focusLockAutoManageEnabled.
        //      Writes now route through Shizuku's shell service, which
        //      runs at the privileged UID AYN's SettingsProvider does
        //      allow. Users opt in by installing Shizuku and granting
        //      the app permission from the Extras section; without
        //      Shizuku the toggle is inert and the write silently
        //      no-ops. Default is true to match the pre-v30 behaviour.
        // v32: replaced the boolean focusLockAutoManageEnabled with
        //      focusLockMode (string: Off/Top/Bottom/Follow). The
        //      legacy boolean is retained in the schema (true -> Follow,
        //      false -> Off on import) so pre-v32 blobs upgrade
        //      without data loss and so pre-v32 app versions still get
        //      a sensible value if they restore a current config.
        //      Default is Follow.
        // v33: added CanvasItem.LastPlayedItem variant and
        //      FeatureConfig.recentlyPlayed, the launch-history list the
        //      Last Played widget binds to. Older blobs decode with the
        //      empty default via ignoreUnknownKeys.
        // v34: added FeatureConfig.screenAssignment — persisted dual-
        //      screen layout name (FRONTEND_ON_TOP / FRONTEND_ON_BOTTOM).
        //      Nullable so pre-v34 blobs decode without a value; import
        //      leaves the device's current assignment alone in that
        //      case rather than reverting to the default, so restoring
        //      an old backup never silently flips a user's layout.
        // v35: added SystemConfig.rcNotificationsEnabled — the "Get
        //      notified about RC builds" toggle gating whether update
        //      checks include GitHub pre-releases. Older blobs decode
        //      with the default (false) via ignoreUnknownKeys, matching
        //      stable-channel behaviour.
        // v36: a ROM can now have up to two companion apps. The schema
        //      adds gameCompanionsMap (Map<String, List<String>>). The
        //      legacy single-value gameCompanionMap is kept (deprecated)
        //      and still written on export so pre-v36 builds restoring
        //      a current config get the first companion. Import prefers
        //      the new field; falls back to converting the legacy single
        //      value to a one-element list.
        // v37: the back button shortcut can hold a second shortcut; when
        //      both are set the back gesture opens a picker. Adds
        //      PowerSettingsConfig.secondaryBackButtonShortcut and
        //      secondaryBackButtonShortcutAppPackage. Older blobs decode
        //      with NONE / null via ignoreUnknownKeys, keeping the
        //      single-shortcut behaviour.
        // v38: added FeatureConfig.experience, the Classic / JarnFE selector.
        //      Nullable so pre-v38 blobs decode without a value; import
        //      leaves the device's current experience alone in that case.
        //      Separate from romSearch.frontendEnabled.
        // v39: added FeatureConfig.artSource, Jarn FE's preferred art source.
        //      Nullable so pre-v39 blobs decode without a value; import
        //      leaves the device's current source alone in that case.
        //      Also added FeatureConfig.systemArtSource, with the same rule.
        // v40: added FeatureConfig.libraryFolders, the Jarn FE game folders.
        //      Null on older blobs, and import then leaves folders alone.
        //      Restored folders never carry a grant and never replace a row.
        // v41: added FeatureConfig.jarnLook and jarnReducedMotion, the Jarn FE
        //      appearance. Null on older blobs, and import then keeps both.
        // v42: added FeatureConfig.jarnArtChoices, the Jarn FE art per screen.
        //      Null on older blobs, and import then keeps the current choices.
        // v43: added FeatureConfig.jarnPinnedApps, the Jarn FE pinned apps in order.
        //      Null on older blobs, and import then keeps the current pins.
        // v44: added FeatureConfig.jarnDisplay, the Jarn FE tile sizes, hints and
        //      button haptics. Null on older blobs or fields, and import keeps those.
        // v45: added FeatureConfig.jarnLiveWallpaper, the Jarn FE live wallpaper id.
        //      Null on older blobs, and import keeps the current choice. "off" clears it.
        // v46: added JarnDisplayConfig.tapControl, the screen that keeps the controller
        //      after a tap. Null or unknown values leave the current choice alone.
        // v47: added JarnDisplayConfig.appNamesVisible and undoCountdownVisible, the Jarn FE
        //      show app names and undo timer choices. Null on older blobs, and import keeps them.
        // v48: added FeatureConfig.jarnPlayStats, play counts and time per game. Null on
        //      older blobs, and import keeps current totals. Import only ever raises them.
        // v49: added FeatureConfig.jarnFirstRunDone, whether the Jarn FE tour was finished.
        //      Null on older blobs, and import keeps the current value. Import only sets it.
        // v50: added FeatureConfig.jarnMatchCommandPanel, whether Jarn FE follows Command
        //      Panel's colors. Null on older blobs, and import keeps the current value.
        //      The palette itself is never backed up.
        // v51: added JarnDisplayConfig.switchCue, how Jarn FE shows a screen switch (pulse, dim
        //      or tab_mark, later also jarn_label). Null on older blobs, and import keeps the
        //      current value. An unknown id falls back to pulse.
        // v52: added UiConfig.appearance (theme, custom themes, OLED, icon shape, tab animation),
        //      FeatureConfig.rssFeeds, JinglesConfig.enabled and repos, and
        //      PageConfig.widgetProviders. All null on older blobs, and import keeps current values.
        //      Feeds, repos and custom themes are only ever added. Restore also clears photo tiles
        //      whose file is missing and widget tiles whose widget isn't the one backed up.
        // v53: added JarnDisplayConfig.systemView and gameView, the Jarn FE Grid or Carousel
        //      choices (grid or carousel). Null on older blobs, and import keeps the current
        //      value. An unknown id falls back to grid.
        // v54: added FeatureConfig.jarnWallpaperScreens, Jarn FE "Same on both screens" and the
        //      bottom screen's own image or GIF. Null on older blobs, and import keeps both.
        // v55: added JarnDisplayConfig.pageTransition, how a Jarn FE Canvas page arrives. Null on
        //      older blobs, and import keeps the current value. An unknown id falls back to jarn_slide.
        // v56: added JarnDisplayConfig.systemNamesVisible, Jarn FE "Show system names" for the
        //      line under system cards. Null on older blobs, and import keeps the current value.
        // v57: added FeatureConfig.jarnPreview (Meet Jarn showings, last version, tried). Null on older
        //      blobs; import only raises the count and only turns tried on.
        // v58: added JarnDisplayConfig.randomVisible and lastPlayedVisible, Jarn FE "Show Random" and
        //      "Show Last Played" on Now. Null on older blobs, and import keeps the current value.
        // v59: added JarnDisplayConfig.systemLogoVisible, gameCountVisible and lastPlayedTextVisible,
        //      the optional parts of Jarn FE Now's system view. Null on older blobs keeps them.
        // v60: added JarnDisplayConfig.gameRowVisible, Jarn FE "Show game row" on Now. Null keeps it.
        // v61: added JarnDisplayConfig.systemBackgroundVisible, Jarn FE "System background" on Now.
        //      Null on older blobs, and import keeps the current value.
        // v62: added FeatureConfig.jarnMatchCommandPanelFonts, whether Jarn FE uses Command Panel's
        //      fonts. Separate from jarnMatchCommandPanel. Null on older blobs keeps the current value.
        // v63: added FeatureConfig.jarnFontsPromptShown, whether Jarn FE's one-time Command Panel fonts
        //      prompt was answered. Null on older blobs keeps the current value. Import only sets it.
        // v64: added RomSearchConfig.gameEmulatorMap, systemEmulatorMap, gameCommandMap and
        //      gameCoreMap, the emulator, core and command choices. Null on older blobs keeps them.
        // v65: added FeatureConfig.jarnEsdeImportNoticeShown, whether Jarn FE's one-time "ES-DE found"
        //      notice after skipping the tour was shown. Null on older blobs keeps it. Import only sets it.
        // v66: added SystemConfig.hiddenGames, games hidden one by one. Null on older blobs keeps them.
        // v67: added FeatureConfig.jarnSystemSorts, each system's game order. Null on older blobs keeps them.
        // v68: added JarnDisplayConfig.playingScreenVisible and playingDim, Jarn FE Now Playing.
        // v69: added JarnDisplayConfig.playingScreenOff, Jarn FE's black bottom screen during a game.
        // v70: added JarnDisplayConfig.videoPreviews and videoPreviewSound, Jarn FE game video
        //      previews and their sound, and FeatureConfig.jarnVideoHintShown, the one-time hint.
        //      Null on older blobs keeps the current values. Import only sets the hint flag.
        // v71: added JarnDisplayConfig.videoFit, Jarn FE video previews cropped or fitted. Null on
        //      older blobs keeps the current value. An unknown id falls back to fit.
        // v72: added FeatureConfig.jarnCollections and jarnFavorites, keyed by system and ROM path so
        //      they match under a different ROM folder. Null on older blobs keeps the device's. Import
        //      only adds: same-name collections (ignoring case) gain members, the rest are made, and
        //      favorites not scanned yet wait on the device until a scan lists them.
        const val CONFIG_VERSION = 72
    }
}
