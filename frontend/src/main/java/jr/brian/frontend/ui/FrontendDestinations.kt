package jr.brian.frontend.ui

/**
 * All NavHost destinations in one place. The frontend owns a single
 * NavHost (plan §3.2 `ui/screens/`); routes added here are consumed
 * by [FrontendApp]'s NavHost in a later commit.
 *
 * Phase 3a ships only [Home] as a placeholder; 3c–3g add the real
 * ones (Games, Detail, Search, Settings, Setup).
 */
object FrontendDestinations {
    const val Home = "home"
}
