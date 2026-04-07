package jr.brian.home.util

import jr.brian.home.ui.screens.SettingsConstants.SECTION_APPEARANCE
import jr.brian.home.ui.screens.SettingsConstants.SECTION_ESDE
import jr.brian.home.ui.screens.SettingsConstants.SECTION_EXTRAS
import jr.brian.home.ui.screens.SettingsConstants.SECTION_LAYOUT
import jr.brian.home.ui.screens.SettingsConstants.SECTION_MUSIC
import jr.brian.home.ui.screens.SettingsConstants.SECTION_RSS
import jr.brian.home.ui.screens.SettingsConstants.SECTION_SUPPORT
import jr.brian.home.ui.screens.SettingsConstants.SECTION_SYSTEM

val sectionKeywords = mapOf(
    SECTION_APPEARANCE to listOf(
        "appearance", "theme", "icon pack", "wallpaper", "oled", "icon shape",
        "font", "brightness", "tab animation", "keyboard"
    ),
    SECTION_ESDE to listOf(
        "esde", "es-de", "emulation", "animation", "music", "jingles", "konfetti",
        "marquee", "power", "screensaver", "video", "effects", "custom paths",
        "system apps", "setup wizard", "rom search", "search"
    ),
    SECTION_LAYOUT to listOf(
        "layout", "grid", "back button", "dock", "app drawer", "columns", "visibility",
        "thor", "shortcut", "fab"
    ),
    SECTION_SUPPORT to listOf(
        "support", "faq", "help", "question"
    ),
    SECTION_SYSTEM to listOf(
        "system", "update", "crash logs", "control pad", "monitor", "volume", "notification"
    ),
    SECTION_EXTRAS to listOf(
        "extras", "what's new", "floaty mode", "floaty", "whats new"
    ),
    SECTION_MUSIC to listOf(
        "music", "background music", "audio", "song", "playlist", "volume", "folder", "mp3"
    ),
    SECTION_RSS to listOf(
        "rss", "feed", "feeds", "news", "articles", "refresh", "interval", "atom", "syndication"
    )
)