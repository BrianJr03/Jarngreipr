package jr.brian.home.esde.events

class ESDEEventListenerImpl : ESDEEventManager.ESDEEventListener {
    var onSystemSelected: ((systemName: String) -> Unit)? = null
    var onGameSelected: ((gameFilename: String, gameName: String?, systemName: String) -> Unit)? = null
    var onGameStarted: ((gameFilename: String, gameName: String?, systemName: String) -> Unit)? = null
    var onGameEnded: ((gameFilename: String, gameName: String?, systemName: String) -> Unit)? = null
    var onScreensaverStarted: (() -> Unit)? = null
    var onScreensaverEnded: ((reason: String) -> Unit)? = null
    var onScreensaverGameSelected: ((gameFilename: String, gameName: String?, systemName: String) -> Unit)? = null

    override fun onSystemSelected(systemName: String) {
        onSystemSelected?.invoke(systemName)
    }

    override fun onGameSelected(gameFilename: String, gameName: String?, systemName: String) {
        onGameSelected?.invoke(gameFilename, gameName, systemName)
    }

    override fun onGameStarted(gameFilename: String, gameName: String?, systemName: String) {
        onGameStarted?.invoke(gameFilename, gameName, systemName)
    }

    override fun onGameEnded(gameFilename: String, gameName: String?, systemName: String) {
        onGameEnded?.invoke(gameFilename, gameName, systemName)
    }

    override fun onScreensaverStarted() {
        onScreensaverStarted?.invoke()
    }

    override fun onScreensaverEnded(reason: String) {
        onScreensaverEnded?.invoke(reason)
    }

    override fun onScreensaverGameSelected(
        gameFilename: String,
        gameName: String?,
        systemName: String
    ) {
        onScreensaverGameSelected?.invoke(gameFilename, gameName, systemName)
    }
}
