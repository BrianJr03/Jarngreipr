package jr.brian.home.esde.util

/**
 * Constants for ES-DE media paths, folder names, and file extensions.
 */
object ESDEMediaConstants {
    const val SYSTEM_LOGOS_ASSET_PATH = "file:///android_asset/system_logos"

    // Media folder names
    const val FOLDER_FANART = "fanart"
    const val FOLDER_SCREENSHOTS = "screenshots"
    const val FOLDER_TITLESCREENS = "titlescreens"
    const val FOLDER_COVERS = "covers"
    const val FOLDER_PHYSICALMEDIA = "physicalmedia"
    const val FOLDER_MIXIMAGES = "miximages"
    const val FOLDER_MARQUEES = "marquees"
    const val FOLDER_VIDEOS = "videos"
    const val FOLDER_WHEEL_2D = "images/wheel-2d"
    const val FOLDER_WHEEL_3D = "images/wheel-3d"

    // File extensions
    val IMAGE_EXTENSIONS = listOf("png", "jpg", "jpeg", "webp", "gif")
    val IMAGE_EXTENSIONS_WITH_SVG = listOf("png", "jpg", "jpeg", "webp", "gif", "svg")
    val VIDEO_EXTENSIONS = listOf("mp4", "mkv", "avi", "wmv", "mov", "webm")

    // Fallback folder lists
    val SYSTEM_IMAGE_FALLBACKS = listOf(FOLDER_FANART, FOLDER_SCREENSHOTS, FOLDER_TITLESCREENS)
    val GAME_IMAGE_FALLBACKS = listOf(FOLDER_SCREENSHOTS, FOLDER_FANART, FOLDER_TITLESCREENS, FOLDER_COVERS, FOLDER_MIXIMAGES)
    val MARQUEE_FALLBACK_DIRS = listOf(FOLDER_WHEEL_2D, FOLDER_WHEEL_3D)

    /**
     * Maps system variants to their parent systems for media lookup.
     * When ES-DE reports a system like "snes-msu1", we should look for media
     * under the parent "snes" folder since that's where the assets are stored.
     */
    val SYSTEM_MEDIA_ALIASES = mapOf(
        "snes-msu1" to "snes",
        "snesna-msu1" to "snesna",
        "sfc-msu1" to "sfc",
        "msu-md" to "genesis",
        "genesis-msu" to "genesis",
        "megadrive-msu" to "megadrive",
        "megadrive" to "genesis",
        "megadrivejp" to "genesis",
        "md" to "genesis",
        "megacd" to "segacd",
        "megacdjp" to "segacd",
        "mark3" to "mastersystem",
        "sms" to "mastersystem",
        "saturnjp" to "saturn",
        "sg1000" to "sg-1000",
        "pcengine" to "tg16",
        "pce" to "tg16",
        "pcenginecd" to "tg-cd",
        "pcecd" to "tg-cd",
        "tgcd" to "tg-cd",
        "sfc" to "snes",
        "snesna" to "snes",
        "sufami" to "snes",
        "famicom" to "nes",
        "n64dd" to "n64",
        "sgb" to "gb",
        "nswitch" to "switch",
        "gamecube" to "gc",
        "neogeocdjp" to "neogeocd",
        "fba" to "fbneo",
        "cps" to "cps1",
        "arcade" to "mame",
        "msx1" to "msx",
        "msx2" to "msx",
        "msxturbor" to "msx",
        "amiga1200" to "amiga",
        "amiga600" to "amiga",
        "amigacd32" to "amiga",
        "cd32" to "amiga",
        "cdtv" to "amiga"
    )

    /**
     * Returns the system name to use for media lookups.
     * Maps variant systems to their parent systems where media is typically stored.
     */
    fun getMediaSystemName(systemName: String): String {
        return SYSTEM_MEDIA_ALIASES[systemName] ?: systemName
    }

    /** Systems that use disc-based physical media. Used for the disc spin animation. */
    val DISC_PLATFORMS = setOf(
        // Sony
        "ps1", "psx", "ps2", "ps3", "psp",
        // Sega
        "segacd", "saturn", "dreamcast",
        // Nintendo
        "gamecube", "gc", "wii", "wiiu",
        // Microsoft
        "xbox", "xbox360",
        // Other
        "3do", "atarijaguarcd", "cd32", "amigacd32", "cdtv", "amigacdtv", "steam", "windows"
    )
}
