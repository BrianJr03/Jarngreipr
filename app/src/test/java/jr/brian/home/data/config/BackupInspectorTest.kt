package jr.brian.home.data.config

import jr.brian.core.model.Experience
import jr.brian.jarnfe.backup.JarnBackupArea
import jr.brian.jarnfe.backup.JarnBackupChange
import jr.brian.jarnfe.backup.JarnBackupProblem
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class BackupInspectorTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val current = JarngreiprConfig.CONFIG_VERSION

    private fun parse(text: String) = parseBackup(text, current) { json.decodeFromString(it) }

    private fun assertRejected(problem: JarnBackupProblem, text: String) {
        assertEquals(BackupParse.Rejected(problem), parse(text))
    }

    private fun valid(config: JarngreiprConfig = JarngreiprConfig()) = json.encodeToString(config)

    @Test
    fun `a real export is accepted`() {
        assertTrue(parse(valid()) is BackupParse.Ready)
    }

    @Test
    fun `an empty object is not a backup, so it can never reset settings`() {
        assertRejected(JarnBackupProblem.NotABackup, "{}")
        assertRejected(JarnBackupProblem.NotABackup, """{"unrelated":true}""")
    }

    @Test
    fun `junk and non objects are not backups`() {
        assertRejected(JarnBackupProblem.NotABackup, "not json")
        assertRejected(JarnBackupProblem.NotABackup, "[1,2,3]")
        assertRejected(JarnBackupProblem.NotABackup, "")
    }

    @Test
    fun `a version that is missing, text, or below one is rejected`() {
        assertRejected(JarnBackupProblem.NotABackup, """{"version":"44","ui":{}}""")
        assertRejected(JarnBackupProblem.NotABackup, """{"version":0,"ui":{}}""")
        assertRejected(JarnBackupProblem.NotABackup, """{"version":null,"ui":{}}""")
    }

    @Test
    fun `a newer schema is rejected before its contents are read`() {
        assertRejected(JarnBackupProblem.TooNew, """{"version":${current + 1}}""")
    }

    @Test
    fun `a missing section is damaged rather than reset to defaults`() {
        assertRejected(JarnBackupProblem.Damaged, """{"version":1,"ui":{},"app":{},"page":{},"feature":{}}""")
    }

    @Test
    fun `a section of the wrong shape is damaged`() {
        assertRejected(
            JarnBackupProblem.Damaged,
            """{"version":1,"ui":{"gridSettings":"x"},"app":{},"page":{},"feature":{},"system":{}}""",
        )
    }

    @Test
    fun `an oversized file is not read`() {
        assertNull(readBackupText(ByteArrayInputStream(ByteArray(11)), limit = 10))
        assertEquals("{}", readBackupText(ByteArrayInputStream("﻿{}".toByteArray()), limit = 10))
    }

    @Test
    fun `identical backups show no changes`() {
        val config = JarngreiprConfig(feature = FeatureConfig(jarnPinnedApps = listOf("a")))
        val preview = buildBackupPreview(config, config)
        assertEquals(0, preview.changeCount)
    }

    @Test
    fun `a different app names choice shows as a look change`() {
        val now = JarngreiprConfig(
            feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(appNamesVisible = true)),
        )
        val backup = JarngreiprConfig(
            feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(appNamesVisible = false)),
        )
        val look = buildBackupPreview(backup, now).items.first { it.area == JarnBackupArea.Look }
        assertEquals(JarnBackupChange.Replaces, look.change)
    }

    @Test
    fun `a backup without app names shows the current choice as staying`() {
        val now = JarngreiprConfig(
            feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(hintsVisible = true, appNamesVisible = false)),
        )
        val backup = JarngreiprConfig(
            feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(hintsVisible = true)),
        )
        val look = buildBackupPreview(backup, now).items.first { it.area == JarnBackupArea.Look }
        assertEquals(JarnBackupChange.Same, look.change)
    }

    @Test
    fun `a different undo countdown choice shows as a look change`() {
        val now = JarngreiprConfig(
            feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(undoCountdownVisible = true)),
        )
        val backup = JarngreiprConfig(
            feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(undoCountdownVisible = false)),
        )
        val look = buildBackupPreview(backup, now).items.first { it.area == JarnBackupArea.Look }
        assertEquals(JarnBackupChange.Replaces, look.change)
    }

    @Test
    fun `a backup without undo countdown shows the current choice as staying`() {
        val now = JarngreiprConfig(
            feature = FeatureConfig(
                jarnDisplay = JarnDisplayConfig(hintsVisible = true, undoCountdownVisible = false),
            ),
        )
        val backup = JarngreiprConfig(
            feature = FeatureConfig(jarnDisplay = JarnDisplayConfig(hintsVisible = true)),
        )
        val look = buildBackupPreview(backup, now).items.first { it.area == JarnBackupArea.Look }
        assertEquals(JarnBackupChange.Same, look.change)
    }

    @Test
    fun `fields an older backup lacks are shown as staying`() {
        val now = JarngreiprConfig(
            feature = FeatureConfig(
                jarnPinnedApps = listOf("a"),
                jarnLook = "ember",
                experience = Experience.JarnFE.name,
                libraryFolders = listOf(LibraryFolderConfig("tree")),
            ),
        )
        val old = JarngreiprConfig(version = 30)
        val items = buildBackupPreview(old, now).items.associateBy { it.area }
        assertEquals(JarnBackupChange.NotIncluded, items.getValue(JarnBackupArea.PinnedApps).change)
        assertEquals(JarnBackupChange.NotIncluded, items.getValue(JarnBackupArea.Look).change)
        assertEquals(JarnBackupChange.NotIncluded, items.getValue(JarnBackupArea.GameFolders).change)
        assertFalse(buildBackupPreview(old, now).leavesJarnFe)
    }

    @Test
    fun `a backup that changes matching shows the look as replaced`() {
        val now = JarngreiprConfig(feature = FeatureConfig(jarnMatchCommandPanel = false))
        val backup = JarngreiprConfig(feature = FeatureConfig(jarnMatchCommandPanel = true))
        val items = buildBackupPreview(backup, now).items.associateBy { it.area }
        assertEquals(JarnBackupChange.Replaces, items.getValue(JarnBackupArea.Look).change)
        assertEquals(JarnBackupChange.Same, items.getValue(JarnBackupArea.OtherSettings).change)
    }

    @Test
    fun `a backup without matching keeps the current choice`() {
        val now = JarngreiprConfig(
            feature = FeatureConfig(jarnLook = "ember", jarnMatchCommandPanel = true),
        )
        val backup = JarngreiprConfig(feature = FeatureConfig(jarnLook = "ember"))
        val look = buildBackupPreview(backup, now).items.first { it.area == JarnBackupArea.Look }
        assertEquals(JarnBackupChange.Same, look.change)
    }

    @Test
    fun `counts and added game folders are reported`() {
        val now = JarngreiprConfig(
            feature = FeatureConfig(
                dock = DockConfig(apps = listOf("a")),
                libraryFolders = listOf(LibraryFolderConfig("kept")),
            ),
        )
        val backup = JarngreiprConfig(
            feature = FeatureConfig(
                dock = DockConfig(apps = listOf("a", "b")),
                libraryFolders = listOf(LibraryFolderConfig("kept"), LibraryFolderConfig("new")),
            ),
        )
        val items = buildBackupPreview(backup, now).items.associateBy { it.area }
        val dock = items.getValue(JarnBackupArea.Dock)
        assertEquals(JarnBackupChange.Replaces, dock.change)
        assertEquals(1, dock.currentCount)
        assertEquals(2, dock.backupCount)
        assertEquals(JarnBackupChange.Adds(1), items.getValue(JarnBackupArea.GameFolders).change)
    }

    @Test
    fun `a backup that returns to Classic is flagged`() {
        val now = JarngreiprConfig(feature = FeatureConfig(experience = Experience.JarnFE.name))
        val backup = JarngreiprConfig(feature = FeatureConfig(experience = Experience.Classic.name))
        assertTrue(buildBackupPreview(backup, now).leavesJarnFe)
    }

    @Test
    fun `image references are counted once`() {
        val backup = JarngreiprConfig(
            ui = UiConfig(wallpaper = WallpaperConfig(activeUri = "a", savedImageUri = "a")),
        )
        assertEquals(1, buildBackupPreview(backup, JarngreiprConfig()).imageReferenceCount)
    }

    @Test
    fun `credentials never appear in the preview or the parsed result`() {
        val secret = "hunter2-secret"
        val scraper = ScraperConfig(
            screenScraperDevId = secret,
            screenScraperDevPassword = secret,
            screenScraperUserId = secret,
            screenScraperUserPassword = secret,
            steamGridDbApiKey = secret,
        )
        val backup = JarngreiprConfig(feature = FeatureConfig(romSearch = RomSearchConfig(scraper = scraper)))
        val preview = buildBackupPreview(backup, JarngreiprConfig())
        assertFalse(preview.toString().contains(secret))
        assertFalse(parse(valid(backup)).toString().contains(secret))
        val item = preview.items.first { it.area == JarnBackupArea.Scraper }
        assertEquals("credentials alone are not a visible change", JarnBackupChange.Same, item.change)
    }
}
