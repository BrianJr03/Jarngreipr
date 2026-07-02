package jr.brian.home.esde.model

sealed interface FrontendRoute {
    data object Systems : FrontendRoute
    data class Games(val system: String) : FrontendRoute
}
