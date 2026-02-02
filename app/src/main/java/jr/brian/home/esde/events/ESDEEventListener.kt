package jr.brian.home.esde.events

interface ESDEEventListener {
    fun onSystemSelected(systemName: String)
    fun onGameSelected(gameFilename: String, gameName: String?, systemName: String)
    fun onGameStarted(gameFilename: String, gameName: String?, systemName: String)
    fun onGameEnded(gameFilename: String, gameName: String?, systemName: String)
    fun onScreensaverStarted()
    fun onScreensaverEnded(reason: String = "cancel")
    fun onScreensaverGameSelected(gameFilename: String, gameName: String?, systemName: String)
}