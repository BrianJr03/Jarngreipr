package jr.brian.home.esde.events

import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import java.io.File

class ESDEEventManager(private val eventListener: ESDEEventListener) {

    companion object {
        private const val DEBOUNCE_DELAY = 100 // milliseconds
        private const val LOGS_PATH = "/storage/emulated/0/ES-DE Companion/logs"
        private const val FILE_WRITE_DELAY = 50
        private const val POLL_INTERVAL = 500L // Fallback polling interval in ms
    }

    private var lastEventTime = 0L
    private var fileObserver: FileObserver? = null
    
    // Track last known file contents for change detection
    private var lastSystemName: String? = null
    private var lastGameFilename: String? = null
    
    // Handler for polling fallback
    private val pollHandler = Handler(Looper.getMainLooper())
    private var isPolling = false

    /**
     * Interface for event callbacks
     */
    interface ESDEEventListener {
        fun onSystemSelected(systemName: String)
        fun onGameSelected(gameFilename: String, gameName: String?, systemName: String)
        fun onGameStarted(gameFilename: String, gameName: String?, systemName: String)
        fun onGameEnded(gameFilename: String, gameName: String?, systemName: String)
        fun onScreensaverStarted()
        fun onScreensaverEnded(reason: String = "cancel")
        fun onScreensaverGameSelected(gameFilename: String, gameName: String?, systemName: String)
    }

    /**
     * Start monitoring for ES-DE events
     */
    fun startWatching() {
        val watchDir = File(LOGS_PATH)

        if (!watchDir.exists()) {
            watchDir.mkdirs()
        }

        fileObserver = object : FileObserver(watchDir, MODIFY or CLOSE_WRITE) {
            override fun onEvent(event: Int, path: String?) {
                if (path != null && isValidLogFile(path)) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastEventTime < DEBOUNCE_DELAY) return
                    lastEventTime = currentTime

                    Handler(Looper.getMainLooper()).postDelayed({
                        processEvent(path)
                    }, FILE_WRITE_DELAY.toLong())
                }
            }
        }

        fileObserver?.startWatching()
    }

    /**
     * Stop monitoring for ES-DE events
     */
    fun stopWatching() {
        fileObserver?.stopWatching()
        fileObserver = null
        stopPolling()
    }
    
    /**
     * Start polling as a fallback (useful if FileObserver doesn't work)
     */
    fun startPolling() {
        if (isPolling) return
        isPolling = true
        pollForChanges()
    }
    
    /**
     * Stop polling
     */
    fun stopPolling() {
        isPolling = false
        pollHandler.removeCallbacksAndMessages(null)
    }
    
    private fun pollForChanges() {
        if (!isPolling) return
        
        // Check for system changes
        val currentSystemName = readTextFile("esde_system_name.txt")
        if (currentSystemName != null && currentSystemName != lastSystemName) {
            lastSystemName = currentSystemName
            eventListener.onSystemSelected(currentSystemName)
        }
        
        // Check for game changes
        val currentGameFilename = readTextFile("esde_game_filename.txt")
        if (currentGameFilename != null && currentGameFilename != lastGameFilename) {
            lastGameFilename = currentGameFilename
            val gameName = readTextFile("esde_game_name.txt")
            val systemName = readTextFile("esde_game_system.txt")
            if (systemName != null) {
                eventListener.onGameSelected(currentGameFilename, gameName, systemName)
            }
        }
        
        // Schedule next poll
        pollHandler.postDelayed({ pollForChanges() }, POLL_INTERVAL)
    }

    private fun isValidLogFile(path: String): Boolean {
        return path == "esde_game_filename.txt" ||
                path == "esde_system_name.txt" ||
                path == "esde_gamestart_filename.txt" ||
                path == "esde_gameend_filename.txt" ||
                path == "esde_screensaver_start.txt" ||
                path == "esde_screensaver_end.txt" ||
                path == "esde_screensavergameselect_filename.txt"
    }

    private fun processEvent(filename: String) {
        when (filename) {
            "esde_system_name.txt" -> {
                val systemName = readTextFile("esde_system_name.txt")
                if (systemName != null) {
                    eventListener.onSystemSelected(systemName)
                }
            }
            "esde_game_filename.txt" -> {
                val gameFilename = readTextFile("esde_game_filename.txt")
                val gameName = readTextFile("esde_game_name.txt")
                val systemName = readTextFile("esde_game_system.txt")
                if (gameFilename != null && systemName != null) {
                    eventListener.onGameSelected(gameFilename, gameName, systemName)
                }
            }
            "esde_gamestart_filename.txt" -> {
                val gameFilename = readTextFile("esde_gamestart_filename.txt")
                val gameName = readTextFile("esde_gamestart_name.txt")
                val systemName = readTextFile("esde_gamestart_system.txt")
                if (gameFilename != null && systemName != null) {
                    eventListener.onGameStarted(gameFilename, gameName, systemName)
                }
            }
            "esde_gameend_filename.txt" -> {
                val gameFilename = readTextFile("esde_gameend_filename.txt")
                val gameName = readTextFile("esde_gameend_name.txt")
                val systemName = readTextFile("esde_gameend_system.txt")
                if (gameFilename != null && systemName != null) {
                    eventListener.onGameEnded(gameFilename, gameName, systemName)
                }
            }
            "esde_screensaver_start.txt" -> {
                eventListener.onScreensaverStarted()
            }
            "esde_screensaver_end.txt" -> {
                val reason = readTextFile("esde_screensaver_end.txt") ?: "cancel"
                eventListener.onScreensaverEnded(reason)
            }
            "esde_screensavergameselect_filename.txt" -> {
                val gameFilename = readTextFile("esde_screensavergameselect_filename.txt")
                val gameName = readTextFile("esde_screensavergameselect_name.txt")
                val systemName = readTextFile("esde_screensavergameselect_system.txt")
                if (gameFilename != null && systemName != null) {
                    eventListener.onScreensaverGameSelected(gameFilename, gameName, systemName)
                }
            }
        }
    }

    private fun readTextFile(filename: String): String? {
        return try {
            val file = File(LOGS_PATH, filename)
            if (file.exists()) {
                file.readText().trim().takeIf { it.isNotEmpty() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
