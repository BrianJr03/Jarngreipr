package jr.brian.core.model

/**
 * Which product a Home press opens. Independent of `frontendEnabled`, which
 * stays a Classic-only setting so switching experience never rewrites it.
 *
 * Stored by [name] in preferences and backups, so renaming an entry is a
 * breaking change.
 */
enum class Experience {
    Classic,
    JarnFE;

    companion object {
        val DEFAULT = Classic

        fun fromNameOrDefault(name: String?): Experience =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

/** How a Home request reached the app. */
sealed interface HomeEntry {
    /** `HomeRouterActivity`: keeps the caller's display for external launchers. */
    data object Launcher : HomeEntry

    /** Hardware Home key, from the accessibility service or the activity fallback. */
    data class HomeKey(val target: HomeTarget, val mainScreen: MainScreen) : HomeEntry
}

enum class HomeDestination { CLASSIC, JARN_FE }

/**
 * The routing decision for one Home request. [classicLaunches] is always
 * populated so a caller can fall back if Jarn FE fails to open.
 */
data class HomeRoute(
    val destination: HomeDestination,
    val classicLaunches: List<HomeLaunch>,
)

/**
 * Single decision point for every Home entry path. Classic plans are delegated
 * unchanged to [planLauncherHomeLaunches] and [planHomeLaunches].
 *
 * [Experience.JarnFE] only wins when [jarnFeAvailable] is true, so a restored
 * backup that selects Jarn FE on a build without it still opens Classic.
 */
fun planHomeRoute(
    entry: HomeEntry,
    experience: Experience,
    jarnFeAvailable: Boolean,
    bottomDisplayId: Int?,
    frontendEnabled: Boolean,
    assignment: ScreenAssignment = ScreenAssignment.DEFAULT,
): HomeRoute {
    val classicLaunches = when (entry) {
        HomeEntry.Launcher -> planLauncherHomeLaunches(
            bottomDisplayId = bottomDisplayId,
            frontendEnabled = frontendEnabled,
            assignment = assignment,
        )
        is HomeEntry.HomeKey -> planHomeLaunches(
            bottomDisplayId = bottomDisplayId,
            target = entry.target,
            mainScreen = entry.mainScreen,
            frontendEnabled = frontendEnabled,
            assignment = assignment,
        )
    }
    val destination = if (experience == Experience.JarnFE && jarnFeAvailable) {
        HomeDestination.JARN_FE
    } else {
        HomeDestination.CLASSIC
    }
    return HomeRoute(destination, classicLaunches)
}
