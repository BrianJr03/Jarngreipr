package jr.brian.home.esde.util

object ESDEEventConstants {
    // Timing constants
    const val DEBOUNCE_DELAY = 100
    const val FILE_WRITE_DELAY = 50
    const val POLL_INTERVAL = 500L

    // Paths
    const val LOGS_PATH = "/storage/emulated/0/ES-DE Jarngreipr/logs"

    // System event files
    const val FILE_SYSTEM_NAME = "esde_system_name.txt"

    // Game selection event files
    const val FILE_GAME_FILENAME = "esde_game_filename.txt"
    const val FILE_GAME_NAME = "esde_game_name.txt"
    const val FILE_GAME_SYSTEM = "esde_game_system.txt"

    // Game start event files
    const val FILE_GAMESTART_FILENAME = "esde_gamestart_filename.txt"
    const val FILE_GAMESTART_NAME = "esde_gamestart_name.txt"
    const val FILE_GAMESTART_SYSTEM = "esde_gamestart_system.txt"

    // Game end event files
    const val FILE_GAMEEND_FILENAME = "esde_gameend_filename.txt"
    const val FILE_GAMEEND_NAME = "esde_gameend_name.txt"
    const val FILE_GAMEEND_SYSTEM = "esde_gameend_system.txt"

    // Screensaver event files
    const val FILE_SCREENSAVER_START = "esde_screensaver_start.txt"
    const val FILE_SCREENSAVER_END = "esde_screensaver_end.txt"
    const val FILE_SCREENSAVER_GAME_FILENAME = "esde_screensavergameselect_filename.txt"
    const val FILE_SCREENSAVER_GAME_NAME = "esde_screensavergameselect_name.txt"
    const val FILE_SCREENSAVER_GAME_SYSTEM = "esde_screensavergameselect_system.txt"

    // Default values
    const val DEFAULT_SCREENSAVER_END_REASON = "cancel"
}