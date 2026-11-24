package jr.brian.home.esde.scraper

/**
 * Maps ES-DE system short-names to ScreenScraper's numeric `systemeid`.
 *
 * Derived from ScreenScraper's `systemesListe.php` catalogue; the entries
 * cover the most common console/handheld/arcade platforms. Unknown systems
 * return null and are looked up by filename+extension only (or skipped when
 * that yields no match).
 */
object SystemIdMap {

    private val map: Map<String, Int> = mapOf(
        // Nintendo
        "nes" to 3,
        "famicom" to 3,
        "fds" to 106,
        "snes" to 4,
        "snesna" to 4,
        "sfc" to 4,
        "n64" to 14,
        "n64dd" to 14,
        "gc" to 13,
        "gamecube" to 13,
        "wii" to 16,
        "wiiu" to 18,
        "switch" to 225,
        "nswitch" to 225,
        "gb" to 9,
        "sgb" to 9,
        "gbc" to 10,
        "gba" to 12,
        "nds" to 15,
        "3ds" to 17,
        "virtualboy" to 11,
        "pokemini" to 211,
        // Sega
        "sg-1000" to 109,
        "sg1000" to 109,
        "mastersystem" to 2,
        "sms" to 2,
        "mark3" to 2,
        "megadrive" to 1,
        "genesis" to 1,
        "md" to 1,
        "megacd" to 20,
        "segacd" to 20,
        "32x" to 19,
        "sega32x" to 19,
        "saturn" to 22,
        "saturnjp" to 22,
        "dreamcast" to 23,
        "gamegear" to 21,
        // Sony
        "psx" to 57,
        "ps1" to 57,
        "ps2" to 58,
        "ps3" to 59,
        "psp" to 61,
        "psvita" to 62,
        // Microsoft
        "xbox" to 32,
        "xbox360" to 33,
        // Atari
        "atari2600" to 26,
        "atari5200" to 40,
        "atari7800" to 41,
        "atarilynx" to 28,
        "lynx" to 28,
        "atarijaguar" to 27,
        "jaguar" to 27,
        "atarijaguarcd" to 171,
        // NEC
        "pcengine" to 31,
        "pce" to 31,
        "tg16" to 31,
        "pcenginecd" to 114,
        "pcecd" to 114,
        "tg-cd" to 114,
        "supergrafx" to 105,
        "pcfx" to 72,
        // SNK
        "neogeo" to 142,
        "neogeocd" to 70,
        "neogeocdjp" to 70,
        "ngp" to 25,
        "ngpc" to 82,
        // Arcade / MAME
        "arcade" to 75,
        "mame" to 75,
        "fbneo" to 75,
        "fba" to 75,
        "cps" to 6,
        "cps1" to 6,
        "cps2" to 7,
        "cps3" to 8,
        "naomi" to 56,
        "atomiswave" to 53,
        // Commodore
        "c64" to 66,
        "amiga" to 64,
        "amiga1200" to 64,
        "amiga600" to 64,
        "amigacd32" to 64,
        "cd32" to 64,
        "cdtv" to 129,
        "vic20" to 73,
        // Sinclair
        "zxspectrum" to 76,
        "zx81" to 77,
        // Other
        "3do" to 29,
        "coleco" to 48,
        "colecovision" to 48,
        "intellivision" to 115,
        "vectrex" to 102,
        "msx" to 113,
        "msx1" to 113,
        "msx2" to 116,
        "msxturbor" to 117,
        "pico8" to 234,
        "dos" to 135,
        "pc" to 135,
        "scummvm" to 123,
        "wonderswan" to 45,
        "wonderswancolor" to 46,
        "wswan" to 45,
        "wswanc" to 46,
        "gameandwatch" to 52
    )

    fun idFor(systemName: String): Int? {
        val lower = systemName.lowercase()
        return map[lower]
    }
}
