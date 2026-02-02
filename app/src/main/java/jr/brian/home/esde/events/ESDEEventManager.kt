package jr.brian.home.esde.events

import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import jr.brian.home.esde.util.ESDEEventConstants.DEBOUNCE_DELAY
import jr.brian.home.esde.util.ESDEEventConstants.DEFAULT_SCREENSAVER_END_REASON
import jr.brian.home.esde.util.ESDEEventConstants.FILE_GAMEEND_FILENAME
import jr.brian.home.esde.util.ESDEEventConstants.FILE_GAMEEND_NAME
import jr.brian.home.esde.util.ESDEEventConstants.FILE_GAMEEND_SYSTEM
import jr.brian.home.esde.util.ESDEEventConstants.FILE_GAMESTART_FILENAME
import jr.brian.home.esde.util.ESDEEventConstants.FILE_GAMESTART_NAME
import jr.brian.home.esde.util.ESDEEventConstants.FILE_GAMESTART_SYSTEM
import jr.brian.home.esde.util.ESDEEventConstants.FILE_GAME_FILENAME
import jr.brian.home.esde.util.ESDEEventConstants.FILE_GAME_NAME
import jr.brian.home.esde.util.ESDEEventConstants.FILE_GAME_SYSTEM
import jr.brian.home.esde.util.ESDEEventConstants.FILE_SCREENSAVER_END
import jr.brian.home.esde.util.ESDEEventConstants.FILE_SCREENSAVER_GAME_FILENAME
import jr.brian.home.esde.util.ESDEEventConstants.FILE_SCREENSAVER_GAME_NAME
import jr.brian.home.esde.util.ESDEEventConstants.FILE_SCREENSAVER_GAME_SYSTEM
import jr.brian.home.esde.util.ESDEEventConstants.FILE_SCREENSAVER_START
import jr.brian.home.esde.util.ESDEEventConstants.FILE_SYSTEM_NAME
import jr.brian.home.esde.util.ESDEEventConstants.FILE_WRITE_DELAY
import jr.brian.home.esde.util.ESDEEventConstants.LOGS_PATH
import jr.brian.home.esde.util.ESDEEventConstants.POLL_INTERVAL
import java.io.File

class ESDEEventManager(private val eventListener: ESDEEventListener) {
    private var lastEventTime = 0L
    private var fileObserver: FileObserver? = null
    
    private var lastSystemName: String? = null
    private var lastGameFilename: String? = null
    
    private val pollHandler = Handler(Looper.getMainLooper())
    private var isPolling = false

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

    fun stopWatching() {
        fileObserver?.stopWatching()
        fileObserver = null
        stopPolling()
    }

    fun startPolling() {
        if (isPolling) return
        isPolling = true
        pollForChanges()
    }

    fun stopPolling() {
        isPolling = false
        pollHandler.removeCallbacksAndMessages(null)
    }
    
    private fun pollForChanges() {
        if (!isPolling) return
        
        val currentSystemName = readTextFile(FILE_SYSTEM_NAME)
        if (currentSystemName != null && currentSystemName != lastSystemName) {
            lastSystemName = currentSystemName
            eventListener.onSystemSelected(currentSystemName)
        }
        
        val currentGameFilename = readTextFile(FILE_GAME_FILENAME)
        if (currentGameFilename != null && currentGameFilename != lastGameFilename) {
            lastGameFilename = currentGameFilename
            val gameName = readTextFile(FILE_GAME_NAME)
            val systemName = readTextFile(FILE_GAME_SYSTEM)
            if (systemName != null) {
                eventListener.onGameSelected(currentGameFilename, gameName, systemName)
            }
        }
        
        pollHandler.postDelayed({ pollForChanges() }, POLL_INTERVAL)
    }

    private fun isValidLogFile(path: String): Boolean {
        return path == FILE_GAME_FILENAME ||
                path == FILE_SYSTEM_NAME ||
                path == FILE_GAMESTART_FILENAME ||
                path == FILE_GAMEEND_FILENAME ||
                path == FILE_SCREENSAVER_START ||
                path == FILE_SCREENSAVER_END ||
                path == FILE_SCREENSAVER_GAME_FILENAME
    }

    private fun processEvent(filename: String) {
        when (filename) {
            FILE_SYSTEM_NAME -> {
                val systemName = readTextFile(FILE_SYSTEM_NAME)
                if (systemName != null) {
                    eventListener.onSystemSelected(systemName)
                }
            }
            FILE_GAME_FILENAME -> {
                val gameFilename = readTextFile(FILE_GAME_FILENAME)
                val gameName = readTextFile(FILE_GAME_NAME)
                val systemName = readTextFile(FILE_GAME_SYSTEM)
                if (gameFilename != null && systemName != null) {
                    eventListener.onGameSelected(gameFilename, gameName, systemName)
                }
            }
            FILE_GAMESTART_FILENAME -> {
                val gameFilename = readTextFile(FILE_GAMESTART_FILENAME)
                val gameName = readTextFile(FILE_GAMESTART_NAME)
                val systemName = readTextFile(FILE_GAMESTART_SYSTEM)
                if (gameFilename != null && systemName != null) {
                    eventListener.onGameStarted(gameFilename, gameName, systemName)
                }
            }
            FILE_GAMEEND_FILENAME -> {
                val gameFilename = readTextFile(FILE_GAMEEND_FILENAME)
                val gameName = readTextFile(FILE_GAMEEND_NAME)
                val systemName = readTextFile(FILE_GAMEEND_SYSTEM)
                if (gameFilename != null && systemName != null) {
                    eventListener.onGameEnded(gameFilename, gameName, systemName)
                }
            }
            FILE_SCREENSAVER_START -> {
                eventListener.onScreensaverStarted()
            }
            FILE_SCREENSAVER_END -> {
                val reason = readTextFile(FILE_SCREENSAVER_END) ?: DEFAULT_SCREENSAVER_END_REASON
                eventListener.onScreensaverEnded(reason)
            }
            FILE_SCREENSAVER_GAME_FILENAME -> {
                val gameFilename = readTextFile(FILE_SCREENSAVER_GAME_FILENAME)
                val gameName = readTextFile(FILE_SCREENSAVER_GAME_NAME)
                val systemName = readTextFile(FILE_SCREENSAVER_GAME_SYSTEM)
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
        } catch (_: Exception) {
            null
        }
    }
}
