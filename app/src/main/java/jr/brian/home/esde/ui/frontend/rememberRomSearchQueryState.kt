package jr.brian.home.esde.ui.frontend

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.util.hiddenGameKey

data class RomSearchQueryState(
    val isHiddenMode: Boolean,
    val isAndroidMode: Boolean,
    val androidModeFilter: String,
    val isPlatformMode: Boolean,
    val platformSearch: String?,
    val allPlatforms: List<String>,
    val platformSuggestions: List<String>,
    val selectedPlatform: String?,
)

/**
 * Derives every `@` command flag from the raw text-field query. Extracted
 * verbatim from `RomSearchResultsActivity`'s inline body so the new bottom-sheet
 * search path and the existing full-screen results activity feed identical
 * inputs to [rememberFilteredGames]. The command grammar is user-facing
 * (documented via `romSearchCommands()`), so keep behavior stable.
 */
@Composable
fun rememberRomSearchQueryState(
    queryTrimmed: String,
    romSearchShowAllAndroidApps: Boolean,
    allGames: List<GameInfo>,
    hiddenGames: Set<String>,
): RomSearchQueryState {
    val isHiddenMode = isHiddenModeFor(queryTrimmed)
    val isAndroidMode = isAndroidModeFor(queryTrimmed, romSearchShowAllAndroidApps)
    val androidModeFilter = androidModeFilterFor(queryTrimmed, isAndroidMode)
    val isPlatformMode = isPlatformModeFor(queryTrimmed, isHiddenMode, isAndroidMode)
    val platformSearch = platformSearchFor(queryTrimmed, isPlatformMode)
    val allPlatforms = remember(allGames) { allPlatformsFrom(allGames) }
    val platformSuggestions = remember(platformSearch, allPlatforms, allGames, hiddenGames) {
        platformSuggestionsFor(platformSearch, allPlatforms, allGames, hiddenGames)
    }
    val selectedPlatform = remember(platformSearch, allPlatforms) {
        selectedPlatformFor(platformSearch, allPlatforms)
    }
    return RomSearchQueryState(
        isHiddenMode = isHiddenMode,
        isAndroidMode = isAndroidMode,
        androidModeFilter = androidModeFilter,
        isPlatformMode = isPlatformMode,
        platformSearch = platformSearch,
        allPlatforms = allPlatforms,
        platformSuggestions = platformSuggestions,
        selectedPlatform = selectedPlatform,
    )
}

/**
 * Pure equivalent of [rememberRomSearchQueryState]. The composable exists to
 * plug into Compose's `remember` cache; the derivation itself is trivial and
 * lives here so unit tests can exercise every command shape without a
 * composition. The composable and this function MUST stay in lockstep.
 */
fun computeRomSearchQueryState(
    queryTrimmed: String,
    romSearchShowAllAndroidApps: Boolean,
    allGames: List<GameInfo>,
    hiddenGames: Set<String>,
): RomSearchQueryState {
    val isHiddenMode = isHiddenModeFor(queryTrimmed)
    val isAndroidMode = isAndroidModeFor(queryTrimmed, romSearchShowAllAndroidApps)
    val androidModeFilter = androidModeFilterFor(queryTrimmed, isAndroidMode)
    val isPlatformMode = isPlatformModeFor(queryTrimmed, isHiddenMode, isAndroidMode)
    val platformSearch = platformSearchFor(queryTrimmed, isPlatformMode)
    val allPlatforms = allPlatformsFrom(allGames)
    val platformSuggestions =
        platformSuggestionsFor(platformSearch, allPlatforms, allGames, hiddenGames)
    val selectedPlatform = selectedPlatformFor(platformSearch, allPlatforms)
    return RomSearchQueryState(
        isHiddenMode = isHiddenMode,
        isAndroidMode = isAndroidMode,
        androidModeFilter = androidModeFilter,
        isPlatformMode = isPlatformMode,
        platformSearch = platformSearch,
        allPlatforms = allPlatforms,
        platformSuggestions = platformSuggestions,
        selectedPlatform = selectedPlatform,
    )
}

private fun isHiddenModeFor(queryTrimmed: String): Boolean =
    queryTrimmed.equals("@hidden", ignoreCase = true)

private fun isAndroidModeFor(queryTrimmed: String, romSearchShowAllAndroidApps: Boolean): Boolean =
    romSearchShowAllAndroidApps && (
            queryTrimmed.equals("@android", ignoreCase = true) ||
                    queryTrimmed.startsWith("@android ", ignoreCase = true)
            )

private fun androidModeFilterFor(queryTrimmed: String, isAndroidMode: Boolean): String =
    if (isAndroidMode && queryTrimmed.length > "@android ".length - 1)
        queryTrimmed.drop("@android ".length).trim()
    else ""

private fun isPlatformModeFor(
    queryTrimmed: String,
    isHiddenMode: Boolean,
    isAndroidMode: Boolean,
): Boolean = !isHiddenMode && !isAndroidMode && queryTrimmed.startsWith("@")

private fun platformSearchFor(queryTrimmed: String, isPlatformMode: Boolean): String? =
    if (isPlatformMode) queryTrimmed.removePrefix("@") else null

private fun allPlatformsFrom(allGames: List<GameInfo>): List<String> =
    allGames.map { it.systemName }.distinct().sorted()

private fun platformSuggestionsFor(
    platformSearch: String?,
    allPlatforms: List<String>,
    allGames: List<GameInfo>,
    hiddenGames: Set<String>,
): List<String> = platformSearch?.let { text ->
    val candidates = if (text.isBlank()) allPlatforms
    else allPlatforms.filter { it.contains(text, ignoreCase = true) }
    candidates.filter { platform ->
        allGames.any { game ->
            game.systemName.equals(platform, ignoreCase = true) &&
                    hiddenGameKey(game) !in hiddenGames
        }
    }
} ?: emptyList()

private fun selectedPlatformFor(platformSearch: String?, allPlatforms: List<String>): String? =
    allPlatforms.firstOrNull { it.equals(platformSearch, ignoreCase = true) }
