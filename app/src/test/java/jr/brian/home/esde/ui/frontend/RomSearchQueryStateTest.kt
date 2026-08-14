package jr.brian.home.esde.ui.frontend

import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.util.hiddenGameKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RomSearchQueryStateTest {

    private val mario = game("Super Mario 64", "n64")
    private val zelda = game("Ocarina of Time", "n64")
    private val kirby = game("Kirby", "n3ds")
    private val hiddenPikmin = game("Pikmin", "gc")
    private val allGames = listOf(mario, zelda, kirby, hiddenPikmin)
    private val hiddenGames = setOf(hiddenGameKey(hiddenPikmin))

    @Test
    fun `plain query is not a mode`() {
        val state = compute("mario")
        assertFalse(state.isHiddenMode)
        assertFalse(state.isAndroidMode)
        assertFalse(state.isPlatformMode)
        assertNull(state.platformSearch)
        assertNull(state.selectedPlatform)
        assertEquals("", state.androidModeFilter)
        assertEquals(emptyList<String>(), state.platformSuggestions)
    }

    @Test
    fun `hidden is exact-match case-insensitive`() {
        assertTrue(compute("@hidden").isHiddenMode)
        assertTrue(compute("@Hidden").isHiddenMode)
        assertTrue(compute("@HIDDEN").isHiddenMode)
        assertFalse(compute("@hiddensomething").isHiddenMode)
        assertFalse(compute("@hidden mario").isHiddenMode)
    }

    @Test
    fun `hidden takes precedence over platform`() {
        val state = compute("@hidden")
        assertTrue(state.isHiddenMode)
        assertFalse(state.isPlatformMode)
        assertNull(state.platformSearch)
    }

    @Test
    fun `android exact match enters android mode when preference on`() {
        val state = compute("@android", showAllApps = true)
        assertTrue(state.isAndroidMode)
        assertEquals("", state.androidModeFilter)
        assertFalse(state.isPlatformMode)
    }

    @Test
    fun `android prefix with filter captures remainder trimmed`() {
        val state = compute("@android mario", showAllApps = true)
        assertTrue(state.isAndroidMode)
        assertEquals("mario", state.androidModeFilter)
    }

    @Test
    fun `android prefix with only trailing space still enters mode with empty filter`() {
        val state = compute("@android ", showAllApps = true)
        assertTrue(state.isAndroidMode)
        assertEquals("", state.androidModeFilter)
    }

    @Test
    fun `android is disabled when preference is off`() {
        val state = compute("@android", showAllApps = false)
        assertFalse(state.isAndroidMode)
        // With android disabled, "@android" falls through to platform mode.
        assertTrue(state.isPlatformMode)
        assertEquals("android", state.platformSearch)
    }

    @Test
    fun `android with filter is disabled when preference is off`() {
        val state = compute("@android mario", showAllApps = false)
        assertFalse(state.isAndroidMode)
        assertTrue(state.isPlatformMode)
        assertEquals("android mario", state.platformSearch)
    }

    @Test
    fun `platform mode surfaces platformSearch and selectedPlatform when exact`() {
        val state = compute("@n3ds")
        assertTrue(state.isPlatformMode)
        assertEquals("n3ds", state.platformSearch)
        assertEquals("n3ds", state.selectedPlatform)
        assertTrue(state.platformSuggestions.contains("n3ds"))
    }

    @Test
    fun `platform mode with empty query offers all non-empty-visible platforms`() {
        val state = compute("@")
        assertTrue(state.isPlatformMode)
        assertEquals("", state.platformSearch)
        assertNull(state.selectedPlatform)
        // All-hidden platforms are filtered out. hiddenPikmin/gc is fully hidden.
        assertEquals(listOf("n3ds", "n64"), state.platformSuggestions)
    }

    @Test
    fun `platform suggestions exclude platforms whose games are all hidden`() {
        val state = compute("@g")
        // "gc" matches text but every gc game is hidden — omit it.
        assertFalse(state.platformSuggestions.contains("gc"))
    }

    @Test
    fun `partial platform query still filters against visible-only`() {
        val state = compute("@n")
        // "n3ds" and "n64" both have visible games; "gc" doesn't match anyway.
        assertEquals(listOf("n3ds", "n64"), state.platformSuggestions)
        assertNull(state.selectedPlatform)
    }

    @Test
    fun `allPlatforms is distinct-sorted from allGames regardless of query`() {
        val state = compute("mario")
        assertEquals(listOf("gc", "n3ds", "n64"), state.allPlatforms)
    }

    @Test
    fun `filteredGames matches when driven by extracted state versus inline logic`() {
        // Regression guard: run rememberFilteredGames-equivalent inputs through
        // the compute path and confirm the resulting flag set produces the
        // same filter that the inline body used to emit.
        val cases = listOf(
            "" to allGames,
            "mario" to listOf(mario),
            "@hidden" to listOf(hiddenPikmin),
            "@n64" to listOf(mario, zelda),
            "@n3ds" to listOf(kirby),
        )
        cases.forEach { (query, expected) ->
            val s = compute(query, showAllApps = false)
            val filtered = filterAsInline(query.trim(), s)
            assertEquals("query='$query'", expected.map { it.name }, filtered.map { it.name })
        }
    }

    private fun compute(
        query: String,
        showAllApps: Boolean = false,
    ) = computeRomSearchQueryState(
        queryTrimmed = query.trim(),
        romSearchShowAllAndroidApps = showAllApps,
        allGames = allGames,
        hiddenGames = hiddenGames,
    )

    // Mirrors the shape of the old inline filter closely enough to prove the
    // extracted flag combinations still route to the same subset of games.
    // Deliberately narrow — the full production filter is exercised in
    // rememberFilteredGames; this just checks the wiring.
    private fun filterAsInline(queryTrimmed: String, s: RomSearchQueryState): List<GameInfo> {
        val base = when {
            s.isHiddenMode -> allGames.filter { hiddenGameKey(it) in hiddenGames }
            s.selectedPlatform != null ->
                allGames.filter { it.systemName.equals(s.selectedPlatform, ignoreCase = true) }
            s.isPlatformMode && s.platformSearch != null ->
                allGames.filter { it.systemName.contains(s.platformSearch, ignoreCase = true) }
            queryTrimmed.isBlank() -> allGames
            else -> allGames.filter { it.name.contains(queryTrimmed, ignoreCase = true) }
        }
        val visible = if (s.isHiddenMode) base else base.filter { hiddenGameKey(it) !in hiddenGames }
        return visible.distinctBy { it.name.lowercase() }
    }

    private fun game(name: String, systemName: String) = GameInfo(
        path = "./$name.rom",
        name = name,
        systemName = systemName,
    )
}
