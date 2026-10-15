package jr.brian.home.esde.data

import android.os.FileObserver
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
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

/**
 * Watches the ES-DE log directory for state transitions. Produces events on
 * the main thread for Compose consumers; all file I/O happens on a background
 * [HandlerThread] so a 500ms poll loop over 2+ files does not run on the main
 * looper for the life of MainActivity.
 *
 * Call [startPolling] only when ES-DE companion mode is actually in use: the
 * [setupPreferences] gate below short-circuits when `scriptsPath` is blank so
 * users who don't run external ES-DE never see the poller at all.
 */
class ESDEEventManager(
    private val eventListener: ESDEEventListener,
    private val setupPreferences: SetupPreferences,
) {
    private var lastEventTime = 0L
    private var fileObserver: FileObserver? = null

    @Volatile private var lastSystemName: String? = null
    @Volatile private var lastGameFilename: String? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var pollThread: HandlerThread? = null
    private var pollHandler: Handler? = null
    @Volatile private var isPolling = false

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

                    // FileObserver already fires on a Binder thread; keep the
                    // debounce delay but hop onto the poll thread for the
                    // actual file reads, then dispatch to main.
                    val poll = pollHandler
                    if (poll != null) {
                        poll.postDelayed({ processEvent(path) }, FILE_WRITE_DELAY.toLong())
                    } else {
                        mainHandler.postDelayed({ processEvent(path) }, FILE_WRITE_DELAY.toLong())
                    }
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
        // Companion-mode gate: no scriptsPath means the user never ran the
        // ES-DE setup wizard, so there's nothing to poll. Running the loop
        // anyway only buys 500ms wake-ups and no payoff.
        if (setupPreferences.scriptsPath.isBlank()) {
            Log.d(TAG, "ES-DE companion mode is not configured; skipping event poll")
            return
        }
        val thread = HandlerThread("ESDEEventPoll").also { it.start() }
        pollThread = thread
        pollHandler = Handler(thread.looper)
        isPolling = true
        pollHandler?.post(::pollForChanges)
    }

    /** Test-only: whether the background poll thread is currently running. */
    @androidx.annotation.VisibleForTesting
    internal fun isPollingForTest(): Boolean = isPolling

    fun stopPolling() {
        isPolling = false
        pollHandler?.removeCallbacksAndMessages(null)
        pollThread?.quitSafely()
        pollHandler = null
        pollThread = null
    }

    private fun pollForChanges() {
        if (!isPolling) return

        val currentSystemName = readTextFile(FILE_SYSTEM_NAME)
        if (currentSystemName != null && currentSystemName != lastSystemName) {
            lastSystemName = currentSystemName
            dispatchToMain { eventListener.onSystemSelected(currentSystemName) }
        }

        val currentGameFilename = readTextFile(FILE_GAME_FILENAME)
        if (currentGameFilename != null && currentGameFilename != lastGameFilename) {
            lastGameFilename = currentGameFilename
            val gameName = readTextFile(FILE_GAME_NAME)
            val systemName = readTextFile(FILE_GAME_SYSTEM)
            if (systemName != null) {
                dispatchToMain { eventListener.onGameSelected(currentGameFilename, gameName, systemName) }
            }
        }

        pollHandler?.postDelayed(::pollForChanges, POLL_INTERVAL)
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
                    dispatchToMain { eventListener.onSystemSelected(systemName) }
                }
            }
            FILE_GAME_FILENAME -> {
                val gameFilename = readTextFile(FILE_GAME_FILENAME)
                val gameName = readTextFile(FILE_GAME_NAME)
                val systemName = readTextFile(FILE_GAME_SYSTEM)
                if (gameFilename != null && systemName != null) {
                    dispatchToMain { eventListener.onGameSelected(gameFilename, gameName, systemName) }
                }
            }
            FILE_GAMESTART_FILENAME -> {
                val gameFilename = readTextFile(FILE_GAMESTART_FILENAME)
                val gameName = readTextFile(FILE_GAMESTART_NAME)
                val systemName = readTextFile(FILE_GAMESTART_SYSTEM)
                if (gameFilename != null && systemName != null) {
                    dispatchToMain { eventListener.onGameStarted(gameFilename, gameName, systemName) }
                }
            }
            FILE_GAMEEND_FILENAME -> {
                val gameFilename = readTextFile(FILE_GAMEEND_FILENAME)
                val gameName = readTextFile(FILE_GAMEEND_NAME)
                val systemName = readTextFile(FILE_GAMEEND_SYSTEM)
                if (gameFilename != null && systemName != null) {
                    dispatchToMain { eventListener.onGameEnded(gameFilename, gameName, systemName) }
                }
            }
            FILE_SCREENSAVER_START -> {
                dispatchToMain { eventListener.onScreensaverStarted() }
            }
            FILE_SCREENSAVER_END -> {
                val reason = readTextFile(FILE_SCREENSAVER_END) ?: DEFAULT_SCREENSAVER_END_REASON
                dispatchToMain { eventListener.onScreensaverEnded(reason) }
            }
            FILE_SCREENSAVER_GAME_FILENAME -> {
                val gameFilename = readTextFile(FILE_SCREENSAVER_GAME_FILENAME)
                val gameName = readTextFile(FILE_SCREENSAVER_GAME_NAME)
                val systemName = readTextFile(FILE_SCREENSAVER_GAME_SYSTEM)
                if (gameFilename != null && systemName != null) {
                    dispatchToMain { eventListener.onScreensaverGameSelected(gameFilename, gameName, systemName) }
                }
            }
        }
    }

    private fun dispatchToMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
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

    companion object {
        private const val TAG = "ESDEEventManager"
    }
}
