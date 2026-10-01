package jr.brian.home.esde.ui.frontend

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.util.hiddenGameKey

@Composable
fun rememberFilteredGames(
    allGames: List<GameInfo>,
    hiddenGames: Set<String>,
    forcedPlatform: String? = null,
    queryTrimmed: String = "",
    selectedPlatform: String? = null,
    isPlatformMode: Boolean = false,
    isHiddenMode: Boolean = false,
    isAndroidMode: Boolean = false,
    androidModeFilter: String = "",
    platformSearch: String? = null,
    autoFilterPlatform: String? = null,
    allAndroidApps: List<GameInfo> = emptyList()
): List<GameInfo> {
    val effectiveHiddenMode = forcedPlatform == null && isHiddenMode
    val effectiveAndroidMode = forcedPlatform == null && isAndroidMode

    return remember(
        allGames,
        queryTrimmed,
        selectedPlatform,
        isPlatformMode,
        effectiveHiddenMode,
        effectiveAndroidMode,
        androidModeFilter,
        allAndroidApps,
        hiddenGames,
        autoFilterPlatform,
        forcedPlatform
    ) {
        if (effectiveAndroidMode) {
            return@remember filterAndroidApps(allAndroidApps, androidModeFilter)
        }
        val list = matchGames(
            allGames = allGames,
            allAndroidApps = allAndroidApps,
            forcedPlatform = forcedPlatform,
            isHiddenMode = effectiveHiddenMode,
            hiddenGames = hiddenGames,
            autoFilterPlatform = autoFilterPlatform,
            selectedPlatform = selectedPlatform,
            isPlatformMode = isPlatformMode,
            platformSearch = platformSearch,
            queryTrimmed = queryTrimmed
        )
        list
            .distinctBy { it.name.lowercase() }
            .filterHiddenIfNeeded(effectiveHiddenMode, hiddenGames)
    }
}

private fun filterAndroidApps(apps: List<GameInfo>, modeFilter: String): List<GameInfo> =
    if (modeFilter.isBlank()) apps
    else apps.filter { it.name.contains(modeFilter, ignoreCase = true) }

private fun matchGames(
    allGames: List<GameInfo>,
    allAndroidApps: List<GameInfo>,
    forcedPlatform: String?,
    isHiddenMode: Boolean,
    hiddenGames: Set<String>,
    autoFilterPlatform: String?,
    selectedPlatform: String?,
    isPlatformMode: Boolean,
    platformSearch: String?,
    queryTrimmed: String
): List<GameInfo> = when {
    forcedPlatform != null ->
        allGames.filter { it.systemName.equals(forcedPlatform, ignoreCase = true) }

    isHiddenMode ->
        allGames.filter { hiddenGameKey(it) in hiddenGames }

    autoFilterPlatform != null ->
        allGames.filter { it.systemName.equals(autoFilterPlatform, ignoreCase = true) }

    selectedPlatform != null ->
        allGames.filter { it.systemName.equals(selectedPlatform, ignoreCase = true) }

    isPlatformMode && platformSearch != null ->
        allGames.filter { it.systemName.contains(platformSearch, ignoreCase = true) }

    queryTrimmed.isBlank() -> allGames

    else -> matchTextQuery(allGames, allAndroidApps, queryTrimmed)
}

private fun matchTextQuery(
    allGames: List<GameInfo>,
    allAndroidApps: List<GameInfo>,
    queryTrimmed: String
): List<GameInfo> {
    val esdeMatches = allGames.filter { game ->
        game.name.contains(queryTrimmed, ignoreCase = true) ||
                game.systemName.contains(queryTrimmed, ignoreCase = true) ||
                game.genre?.contains(queryTrimmed, ignoreCase = true) == true ||
                game.developer?.contains(queryTrimmed, ignoreCase = true) == true ||
                game.publisher?.contains(queryTrimmed, ignoreCase = true) == true
    }
    val androidMatches = allAndroidApps.filter { app ->
        app.name.contains(queryTrimmed, ignoreCase = true)
    }
    return esdeMatches + androidMatches
}

private fun List<GameInfo>.filterHiddenIfNeeded(
    isHiddenMode: Boolean,
    hiddenGames: Set<String>
): List<GameInfo> = if (isHiddenMode) this
else filter { hiddenGameKey(it) !in hiddenGames }
