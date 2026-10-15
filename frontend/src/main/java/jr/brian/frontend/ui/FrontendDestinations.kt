package jr.brian.frontend.ui

/**
 * All NavHost destinations in one place. The frontend owns a single
 * NavHost (plan §3.2 `ui/screens/`); routes added here are consumed
 * by [FrontendApp]'s NavHost.
 */
object FrontendDestinations {
    const val Home = "home"

    const val GamesRoute = "games/{systemId}"
    fun games(systemId: String) = "games/$systemId"

    const val DetailRoute = "detail/{gameId}"
    fun detail(gameId: Long) = "detail/$gameId"

    const val Search = "search"
}
