package jr.brian.home.esde.model

sealed interface FrontendSelection {
    data class System(val systemName: String) : FrontendSelection

    data class Game(
        val systemName: String,
        val gameFilename: String
    ) : FrontendSelection
}
