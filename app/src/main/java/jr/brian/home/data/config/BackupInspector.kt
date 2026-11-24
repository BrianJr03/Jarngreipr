package jr.brian.home.data.config

import jr.brian.core.ui.motion.JarnPageTransition
import jr.brian.core.ui.theme.JarnSwitchCue
import jr.brian.frontend.look.JarnBrowseView
import jr.brian.frontend.look.JarnTapControl
import jr.brian.core.model.Experience
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.jarnfe.backup.JarnBackupArea
import jr.brian.jarnfe.backup.JarnBackupChange
import jr.brian.jarnfe.backup.JarnBackupItem
import jr.brian.jarnfe.backup.JarnBackupPreview
import jr.brian.jarnfe.backup.JarnBackupProblem
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.Instant

internal const val MAX_BACKUP_BYTES = 32 * 1024 * 1024

private val RequiredSections = listOf("ui", "app", "page", "feature", "system")

/** [Ready] holds the decoded backup, so it never prints its contents. */
internal sealed interface BackupParse {
    class Ready(val config: JarngreiprConfig) : BackupParse {
        override fun toString(): String = "BackupParse.Ready(redacted)"
    }

    data class Rejected(val problem: JarnBackupProblem) : BackupParse
}

/** Null when [stream] is larger than [limit], so a wrong file can't exhaust memory. */
internal fun readBackupText(stream: InputStream, limit: Int = MAX_BACKUP_BYTES): String? {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val read = stream.read(buffer)
        if (read < 0) break
        if (output.size() + read > limit) return null
        output.write(buffer, 0, read)
    }
    return output.toByteArray().decodeToString().removePrefix("\uFEFF")
}

/**
 * The decoder fills anything missing with defaults, so `{}` would decode and then wipe the user's
 * setup. A backup must therefore state its version and carry every section, which every export
 * since v1 does. Parser messages can quote the file, so they are dropped here.
 */
internal fun parseBackup(
    text: String,
    currentVersion: Int,
    decode: (String) -> JarngreiprConfig,
): BackupParse {
    val root = parseRoot(text) ?: return BackupParse.Rejected(JarnBackupProblem.NotABackup)
    val version = (root["version"] as? JsonPrimitive)
        ?.takeUnless { it.isString }
        ?.intOrNull
        ?.takeIf { it >= 1 }
        ?: return BackupParse.Rejected(JarnBackupProblem.NotABackup)
    if (version > currentVersion) return BackupParse.Rejected(JarnBackupProblem.TooNew)
    if (RequiredSections.any { root[it] !is JsonObject }) {
        return BackupParse.Rejected(JarnBackupProblem.Damaged)
    }
    return try {
        BackupParse.Ready(decode(text))
    } catch (_: SerializationException) {
        BackupParse.Rejected(JarnBackupProblem.Damaged)
    } catch (_: IllegalArgumentException) {
        BackupParse.Rejected(JarnBackupProblem.Damaged)
    }
}

private fun parseRoot(text: String): JsonObject? = try {
    Json.parseToJsonElement(text) as? JsonObject
} catch (_: SerializationException) {
    null
}

internal fun buildBackupPreview(backup: JarngreiprConfig, current: JarngreiprConfig): JarnBackupPreview =
    JarnBackupPreview(
        schemaVersion = backup.version,
        currentSchemaVersion = current.version,
        exportedAtMillis = runCatching { Instant.parse(backup.exportedAt).toEpochMilli() }.getOrNull(),
        items = previewItems(backup, current),
        imageReferenceCount = backup.imageReferences().size,
        leavesJarnFe = backup.feature.experience == Experience.Classic.name &&
            current.feature.experience == Experience.JarnFE.name,
    )

private fun previewItems(backup: JarngreiprConfig, current: JarngreiprConfig): List<JarnBackupItem> {
    val b = backup.feature
    val c = current.feature
    val foldersNow = current.app.folders.filterValues { it.isNotEmpty() }
    val foldersAfter = foldersAfterRestore(backup.app.folders, current.app.folders)
    return listOf(
        replaced(
            JarnBackupArea.HomeLayout,
            backup.page.pageTypes.size,
            current.page.pageTypes.size,
            backup.homeLayout() == current.homeLayout(),
        ),
        replaced(
            JarnBackupArea.Folders,
            foldersAfter.values.sumOf { it.size },
            foldersNow.values.sumOf { it.size },
            foldersAfter == foldersNow,
        ),
        replaced(JarnBackupArea.Dock, b.dock.apps.size, c.dock.apps.size, b.dock == c.dock),
        optional(JarnBackupArea.PinnedApps, b.jarnPinnedApps, c.jarnPinnedApps) { it.size },
        gameFolders(b.libraryFolders, c.libraryFolders),
        replaced(
            JarnBackupArea.HiddenSystems,
            backup.system.hiddenSystems.size,
            current.system.hiddenSystems.size,
            backup.system.hiddenSystems.toSet() == current.system.hiddenSystems.toSet(),
        ),
        optional(JarnBackupArea.ArtChoices, b.jarnArtChoices, c.jarnArtChoices) { it.count() },
        replaced(
            JarnBackupArea.RecentlyPlayed,
            b.recentlyPlayed.size,
            c.recentlyPlayed.size,
            b.recentlyPlayed == c.recentlyPlayed,
        ),
        JarnBackupItem(JarnBackupArea.Look, null, null, lookChange(b, c)),
        replaced(
            JarnBackupArea.Wallpaper,
            null,
            null,
            backup.ui.wallpaper == current.ui.wallpaper &&
                backup.ui.frontendWallpaper == current.ui.frontendWallpaper &&
                wallpaperScreensAfter(b.jarnWallpaperScreens, c.jarnWallpaperScreens) == c.jarnWallpaperScreens,
        ),
        replaced(
            JarnBackupArea.Scraper,
            null,
            null,
            b.romSearch.scraper.withoutSecrets() == c.romSearch.scraper.withoutSecrets(),
        ),
        replaced(
            JarnBackupArea.OtherSettings,
            null,
            null,
            backup.otherSettings(current) == current.otherSettings(current),
        ),
    )
}

/** What restoring [backup] leaves, since a missing field keeps the current value. */
private fun wallpaperScreensAfter(
    backup: JarnWallpaperScreensConfig?,
    current: JarnWallpaperScreensConfig?,
): JarnWallpaperScreensConfig? {
    backup ?: return current
    val same = backup.sameOnBoth ?: current?.sameOnBoth
    return JarnWallpaperScreensConfig(sameOnBoth = same, bottomUri = backup.bottomUri.takeUnless { same == true })
}

private fun replaced(area: JarnBackupArea, backupCount: Int?, currentCount: Int?, same: Boolean) =
    JarnBackupItem(
        area = area,
        backupCount = backupCount,
        currentCount = currentCount,
        change = if (same) JarnBackupChange.Same else JarnBackupChange.Replaces,
    )

private fun <T> optional(area: JarnBackupArea, backup: T?, current: T?, count: (T) -> Int): JarnBackupItem =
    if (backup == null) {
        JarnBackupItem(area, null, current?.let(count), JarnBackupChange.NotIncluded)
    } else {
        replaced(area, count(backup), current?.let(count) ?: 0, backup == current)
    }

/** Restoring game folders only adds the missing ones. */
private fun gameFolders(
    backup: List<LibraryFolderConfig>?,
    current: List<LibraryFolderConfig>?,
): JarnBackupItem {
    if (backup == null) {
        return JarnBackupItem(JarnBackupArea.GameFolders, null, current?.size, JarnBackupChange.NotIncluded)
    }
    val existing = current.orEmpty().map { it.treeUri }.toSet()
    val added = backup.map { it.treeUri }.distinct().count { it !in existing }
    return JarnBackupItem(
        area = JarnBackupArea.GameFolders,
        backupCount = backup.size,
        currentCount = current?.size ?: 0,
        change = if (added > 0) JarnBackupChange.Adds(added) else JarnBackupChange.Same,
    )
}

private fun lookChange(backup: FeatureConfig, current: FeatureConfig): JarnBackupChange {
    val display = backup.jarnDisplay
    val match = backup.jarnMatchCommandPanel
    if (backup.jarnLook == null && backup.jarnReducedMotion == null && display == null && match == null) {
        return JarnBackupChange.NotIncluded
    }
    val shown = current.jarnDisplay
    val merged = display?.let {
        JarnDisplayConfig(
            systemTileScale = it.systemTileScale ?: shown?.systemTileScale,
            gameTileScale = it.gameTileScale ?: shown?.gameTileScale,
            hintsVisible = it.hintsVisible ?: shown?.hintsVisible,
            buttonHaptics = it.buttonHaptics ?: shown?.buttonHaptics,
            tapControl = it.tapControl?.takeIf { id -> JarnTapControl.fromId(id) != null } ?: shown?.tapControl,
            appNamesVisible = it.appNamesVisible ?: shown?.appNamesVisible,
            undoCountdownVisible = it.undoCountdownVisible ?: shown?.undoCountdownVisible,
            switchCue = it.switchCue?.let { id -> JarnSwitchCue.fromId(id).id } ?: shown?.switchCue,
            systemView = it.systemView?.let { id -> JarnBrowseView.fromId(id).id } ?: shown?.systemView,
            gameView = it.gameView?.let { id -> JarnBrowseView.fromId(id).id } ?: shown?.gameView,
            pageTransition = it.pageTransition?.let { id -> JarnPageTransition.fromId(id).id } ?: shown?.pageTransition,
            systemNamesVisible = it.systemNamesVisible ?: shown?.systemNamesVisible,
            randomVisible = it.randomVisible ?: shown?.randomVisible,
            lastPlayedVisible = it.lastPlayedVisible ?: shown?.lastPlayedVisible,
            systemLogoVisible = it.systemLogoVisible ?: shown?.systemLogoVisible,
            gameCountVisible = it.gameCountVisible ?: shown?.gameCountVisible,
            lastPlayedTextVisible = it.lastPlayedTextVisible ?: shown?.lastPlayedTextVisible,
            gameRowVisible = it.gameRowVisible ?: shown?.gameRowVisible,
        )
    } ?: shown
    val same = (backup.jarnLook ?: current.jarnLook) == current.jarnLook &&
        (backup.jarnReducedMotion ?: current.jarnReducedMotion) == current.jarnReducedMotion &&
        (match ?: current.jarnMatchCommandPanel) == current.jarnMatchCommandPanel &&
        merged == shown
    return if (same) JarnBackupChange.Same else JarnBackupChange.Replaces
}

private fun JarngreiprConfig.homeLayout(): List<Any> =
    listOf(page.copy(widgetProviders = null), app.positions, app.visibility)

private fun JarnArtChoicesConfig.count(): Int =
    topBySystem.size + bottomBySystem.size + topByGame.size + bottomByGame.size

private fun ScraperConfig.withoutSecrets(): ScraperConfig = copy(
    screenScraperDevId = "",
    screenScraperDevPassword = "",
    screenScraperUserId = "",
    screenScraperUserPassword = "",
    steamGridDbApiKey = "",
)

/** Everything the other areas don't list. Fields a backup lacks count as keeping [current]. */
private fun JarngreiprConfig.otherSettings(current: JarngreiprConfig): JarngreiprConfig = copy(
    version = 0,
    exportedAt = "",
    ui = ui.copy(
        wallpaper = WallpaperConfig(),
        frontendWallpaper = WallpaperConfig(),
        appearance = current.ui.appearance?.let { now -> ui.appearance?.mergedInto(now) ?: now },
    ),
    app = AppConfig(),
    page = PageConfig(),
    feature = feature.copy(
        dock = DockConfig(),
        recentlyPlayed = emptyList(),
        libraryFolders = null,
        jarnPlayStats = null,
        jarnLook = null,
        jarnReducedMotion = null,
        jarnArtChoices = null,
        jarnPinnedApps = null,
        jarnDisplay = null,
        jarnFirstRunDone = null,
        jarnMatchCommandPanel = null,
        jarnWallpaperScreens = null,
        jarnPreview = null,
        screenAssignment = feature.screenAssignment ?: current.feature.screenAssignment,
        experience = feature.experience ?: current.feature.experience,
        artSource = feature.artSource ?: current.feature.artSource,
        systemArtSource = feature.systemArtSource ?: current.feature.systemArtSource,
        romSearch = feature.romSearch.copy(scraper = ScraperConfig(), hintsKbVisible = true),
        rssFeeds = mergeRssFeeds(current.feature.rssFeeds.orEmpty(), feature.rssFeeds),
        jingles = feature.jingles.copy(
            enabled = feature.jingles.enabled ?: current.feature.jingles.enabled,
            repos = mergeJingleRepos(current.feature.jingles.repos.orEmpty(), feature.jingles.repos).sorted(),
        ),
    ),
    system = system.copy(hiddenSystems = emptyList()),
)

private fun JarngreiprConfig.imageReferences(): Set<String> = buildSet {
    listOf(ui.wallpaper, ui.frontendWallpaper).forEach { wallpaper ->
        listOfNotNull(
            wallpaper.activeUri,
            wallpaper.savedImageUri,
            wallpaper.savedGifUri,
            wallpaper.savedVideoUri,
        ).filterTo(this) { it.isNotBlank() }
    }
    feature.jarnWallpaperScreens?.bottomUri?.takeIf { it.isNotBlank() }?.let(::add)
    app.folders.values.flatten().mapNotNullTo(this) { folder ->
        folder.backgroundImagePath?.takeIf { it.isNotBlank() }
    }
    page.canvasLayouts.values.flatMap { it.items }
        .filterIsInstance<CanvasItem.PhotoContainer>()
        .mapNotNullTo(this) { photo -> photo.imageUri?.takeIf { it.isNotBlank() } }
}
