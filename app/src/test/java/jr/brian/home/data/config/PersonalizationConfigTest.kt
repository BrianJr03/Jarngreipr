package jr.brian.home.data.config

import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.jarnfe.backup.JarnBackupArea
import jr.brian.jarnfe.backup.JarnBackupChange
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalizationConfigTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val custom = CustomThemeConfig("custom_1", "Mine", "#FF112233", "#FF445566", false)

    private val full = JarngreiprConfig(
        ui = UiConfig(
            appearance = AppearanceConfig(
                themeId = "custom_1",
                customThemes = listOf(custom),
                oledMode = true,
                keyboardOledExempt = true,
                iconShape = "SQUARE",
                tabAnimation = true,
            )
        ),
        page = PageConfig(widgetProviders = mapOf("7" to "pkg/.Clock")),
        feature = FeatureConfig(
            rssFeeds = listOf(RssFeedConfig("https://a.example/feed", 60)),
            jingles = JinglesConfig(enabled = false, repos = listOf("owner/jingles")),
        ),
    )

    @Test
    fun `new settings survive a round trip`() {
        val decoded = json.decodeFromString<JarngreiprConfig>(json.encodeToString(full))
        assertEquals(full, decoded)
    }

    @Test
    fun `a v51 backup leaves every new setting unset`() {
        val legacy = """{"version":51,"ui":{},"app":{},"page":{},"feature":{"jingles":{"isMuted":true}},"system":{}}"""
        val decoded = json.decodeFromString<JarngreiprConfig>(legacy)
        assertNull(decoded.ui.appearance)
        assertNull(decoded.page.widgetProviders)
        assertNull(decoded.feature.rssFeeds)
        assertNull(decoded.feature.jingles.enabled)
        assertNull(decoded.feature.jingles.repos)
        assertTrue(decoded.feature.jingles.isMuted)
    }

    @Test
    fun `scraper secrets are still never exported`() {
        val encoded = json.encodeToString(full)
        assertTrue("\"screenScraperUserPassword\": \"\"" in encoded || "\"screenScraperUserPassword\":\"\"" in encoded)
    }

    @Test
    fun `restored custom themes are added and current ones are kept`() {
        val mine = CustomThemeConfig("custom_2", "Kept", "#FF000000", "#FFFFFFFF", true)
        val merged = mergeCustomThemes(listOf(mine), listOf(custom))
        assertEquals(listOf(mine, custom), merged)
    }

    @Test
    fun `a custom theme that would break storage is skipped`() {
        val broken = listOf(
            custom.copy(id = "custom_3", name = "a:::b"),
            custom.copy(id = "custom_4", primaryHex = "red"),
            custom.copy(id = "preset_like"),
            custom.copy(id = "custom_5|||x"),
        )
        assertEquals(emptyList<CustomThemeConfig>(), mergeCustomThemes(emptyList(), broken))
    }

    @Test
    fun `an unknown theme id or icon shape keeps the current one`() {
        val now = AppearanceConfig(themeId = "pink_violet", iconShape = "ROUNDED", customThemes = emptyList())
        val merged = AppearanceConfig(themeId = "custom_missing", iconShape = "STAR").mergedInto(now)
        assertEquals("pink_violet", merged.themeId)
        assertEquals("ROUNDED", merged.iconShape)
    }

    @Test
    fun `missing appearance fields keep current values`() {
        val now = AppearanceConfig(themeId = "x", oledMode = true, tabAnimation = false, customThemes = emptyList())
        val merged = AppearanceConfig(tabAnimation = true).mergedInto(now)
        assertEquals(true, merged.oledMode)
        assertEquals(true, merged.tabAnimation)
        assertEquals("x", merged.themeId)
    }

    @Test
    fun `feeds are added in backup order and none are removed`() {
        val current = listOf(RssFeedConfig("https://b.example", 30), RssFeedConfig("https://a.example", 30))
        val backup = listOf(
            RssFeedConfig("https://a.example", 120),
            RssFeedConfig("https://c.example", 15),
            RssFeedConfig("file:///data/data/x", 30),
            RssFeedConfig("javascript:alert(1)", 30),
            RssFeedConfig("https://bad example", 30),
        )
        assertEquals(
            listOf(
                RssFeedConfig("https://a.example", 120),
                RssFeedConfig("https://c.example", 15),
                RssFeedConfig("https://b.example", 30),
            ),
            mergeRssFeeds(current, backup),
        )
        assertEquals(current, mergeRssFeeds(current, null))
    }

    @Test
    fun `jingle repos are added and odd names are skipped`() {
        val merged = mergeJingleRepos(setOf("me/mine"), listOf("owner/new", "../etc", "a/b/c", "owner/.."))
        assertEquals(setOf("me/mine", "owner/new"), merged)
        assertEquals(setOf("me/mine"), mergeJingleRepos(setOf("me/mine"), null))
    }

    @Test
    fun `widget tiles keep their id only while the same widget answers`() {
        val layouts = mapOf(
            "0" to CanvasLayout(
                items = listOf(
                    CanvasItem.WidgetItem("live", 1),
                    CanvasItem.WidgetItem("reused", 2),
                    CanvasItem.WidgetItem("dead", 3),
                )
            )
        )
        val live = mapOf(1 to "pkg/.Clock", 2 to "other/.Weather")
        val repaired = repairRestoredCanvas(
            layouts = layouts,
            backupProviders = mapOf("1" to "pkg/.Clock", "2" to "pkg/.Notes", "3" to "pkg/.Old"),
            providerOf = live::get,
            canOpenPhoto = { true },
        )
        assertEquals(
            listOf(1, MISSING_WIDGET_ID, MISSING_WIDGET_ID),
            repaired.getValue("0").items.map { (it as CanvasItem.WidgetItem).widgetId },
        )
    }

    @Test
    fun `older backups keep any widget id that is still live`() {
        val layouts = mapOf("0" to CanvasLayout(items = listOf(CanvasItem.WidgetItem("w", 4))))
        val repaired = repairRestoredCanvas(layouts, null, { "any/.Widget" }, { true })
        assertEquals(layouts, repaired)
    }

    @Test
    fun `a photo this device can't open goes back to the picker state`() {
        val layouts = mapOf(
            "0" to CanvasLayout(
                items = listOf(
                    CanvasItem.PhotoContainer("kept", "file:///ok.png"),
                    CanvasItem.PhotoContainer("gone", "file:///missing.png"),
                    CanvasItem.PhotoContainer("empty", null),
                )
            )
        )
        val repaired = repairRestoredCanvas(layouts, emptyMap(), { null }, { it == "file:///ok.png" })
        assertEquals(
            listOf("file:///ok.png", null, null),
            repaired.getValue("0").items.map { (it as CanvasItem.PhotoContainer).imageUri },
        )
    }

    @Test
    fun `widget providers are recorded for live widgets only`() {
        val layouts = listOf(
            CanvasLayout(items = listOf(CanvasItem.WidgetItem("a", 1), CanvasItem.WidgetItem("b", 9))),
        )
        assertEquals(mapOf("1" to "pkg/.Clock"), canvasWidgetProviders(layouts) { if (it == 1) "pkg/.Clock" else null })
    }

    @Test
    fun `a v51 backup previews the new settings as unchanged`() {
        val preview = buildBackupPreview(JarngreiprConfig(version = 51), full)
        val other = preview.items.first { it.area == JarnBackupArea.OtherSettings }
        val home = preview.items.first { it.area == JarnBackupArea.HomeLayout }
        assertEquals(JarnBackupChange.Same, other.change)
        assertEquals(JarnBackupChange.Same, home.change)
    }

    @Test
    fun `a backup with a new feed previews other settings as changed`() {
        val backup = full.copy(feature = full.feature.copy(rssFeeds = listOf(RssFeedConfig("https://z.example", 30))))
        val other = buildBackupPreview(backup, full).items.first { it.area == JarnBackupArea.OtherSettings }
        assertEquals(JarnBackupChange.Replaces, other.change)
    }

    @Test
    fun `photo tiles count as image references`() {
        val backup = JarngreiprConfig(
            page = PageConfig(
                canvasLayouts = mapOf("0" to CanvasLayout(items = listOf(CanvasItem.PhotoContainer("p", "file:///x.png"))))
            )
        )
        assertEquals(1, buildBackupPreview(backup, JarngreiprConfig()).imageReferenceCount)
    }
}
