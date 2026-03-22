package jr.brian.home.esde.util

import android.util.Log
import android.util.Xml
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.util.ESDEMediaConstants.FOLDER_COVERS
import jr.brian.home.esde.util.ESDEMediaConstants.FOLDER_MARQUEES
import jr.brian.home.esde.util.ESDEMediaConstants.FOLDER_PHYSICALMEDIA
import jr.brian.home.esde.util.ESDEMediaConstants.IMAGE_EXTENSIONS
import jr.brian.home.esde.util.ESDEMediaConstants.IMAGE_EXTENSIONS_WITH_SVG
import jr.brian.home.esde.util.ESDEMediaConstants.MARQUEE_FALLBACK_DIRS
import jr.brian.home.esde.util.ESDEMediaConstants.getMediaSystemName
import org.xmlpull.v1.XmlPullParser
import java.io.File

object RomListParser {
    private const val TAG = "RomListParser"

    fun parseAllSystems(
        esdeRootPath: String,
        mediaPath: String,
        romsPaths: List<String> = emptyList(),
        systemEmulatorMap: Map<String, String?> = emptyMap()
    ): List<GameInfo> {
        val gamelistsDir = File(esdeRootPath, "gamelists")
        if (!gamelistsDir.exists() || !gamelistsDir.isDirectory) {
            Log.d(TAG, "Gamelists directory not found: ${gamelistsDir.absolutePath}")
            return emptyList()
        }

        val systemCommands = parseSystemCommands(File(esdeRootPath, "custom_systems/es_systems.xml"))

        return gamelistsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.flatMap { systemDir ->
                val systemName = systemDir.name
                val gamelistFile = File(systemDir, "gamelist.xml")
                if (gamelistFile.exists()) {
                    parseSystemGamelist(
                        gamelistFile = gamelistFile,
                        systemName = systemName,
                        mediaPath = mediaPath,
                        romsPaths = romsPaths,
                        emulatorPackage = systemEmulatorMap[systemName],
                        launchCommand = systemCommands[systemName]
                    )
                } else {
                    emptyList()
                }
            } ?: emptyList()
    }

    private fun parseSystemCommands(esSystemsFile: File): Map<String, String> {
        if (!esSystemsFile.exists()) return emptyMap()
        val commands = mutableMapOf<String, String>()
        try {
            val parser = Xml.newPullParser()
            esSystemsFile.inputStream().use { input ->
                parser.setInput(input, "UTF-8")
                var currentSystemName: String? = null
                var firstCommand: String? = null
                var inSystem = false
                val textBuffer = StringBuilder()
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            textBuffer.clear()
                            if (parser.name == "system") {
                                inSystem = true
                                currentSystemName = null
                                firstCommand = null
                            }
                        }
                        XmlPullParser.TEXT -> if (inSystem) textBuffer.append(parser.text)
                        XmlPullParser.END_TAG -> {
                            if (inSystem) {
                                val text = textBuffer.toString().trim()
                                when (parser.name) {
                                    "name" -> if (currentSystemName == null) currentSystemName = text
                                    "command" -> if (firstCommand == null && text.isNotEmpty()) firstCommand = text
                                    "system" -> {
                                        val sn = currentSystemName
                                        val cmd = firstCommand
                                        if (sn != null && cmd != null) commands[sn] = cmd
                                        inSystem = false
                                    }
                                }
                            }
                            textBuffer.clear()
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing es_systems.xml", e)
        }
        return commands
    }

    private fun parseSystemGamelist(
        gamelistFile: File,
        systemName: String,
        mediaPath: String,
        romsPaths: List<String>,
        emulatorPackage: String?,
        launchCommand: String?
    ): List<GameInfo> {
        val games = mutableListOf<GameInfo>()
        try {
            val parser = Xml.newPullParser()
            gamelistFile.inputStream().use { input ->
                parser.setInput(input, "UTF-8")

                var inGame = false
                var path = ""
                var name = ""
                var desc: String? = null
                var rating = 0f
                var releaseDate: String? = null
                var developer: String? = null
                var publisher: String? = null
                var genre: String? = null
                var players: String? = null
                var favorite = false
                var playCount = 0
                var playTime = 0
                var lastPlayed: String? = null
                val textBuffer = StringBuilder()

                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            textBuffer.clear()
                            if (parser.name == "game") {
                                inGame = true
                                path = ""
                                name = ""
                                desc = null
                                rating = 0f
                                releaseDate = null
                                developer = null
                                publisher = null
                                genre = null
                                players = null
                                favorite = false
                                playCount = 0
                                playTime = 0
                                lastPlayed = null
                            }
                        }

                        XmlPullParser.TEXT -> {
                            if (inGame) textBuffer.append(parser.text)
                        }

                        XmlPullParser.END_TAG -> {
                            if (inGame) {
                                val tagName = parser.name
                                val text = textBuffer.toString().trim()
                                when (tagName) {
                                    "path" -> path = text.removePrefix("./")
                                    "name" -> name = text
                                    "desc" -> desc = text.ifBlank { null }
                                    "rating" -> rating = text.toFloatOrNull() ?: 0f
                                    "releasedate" -> releaseDate = text.ifBlank { null }
                                    "developer" -> developer = text.ifBlank { null }
                                    "publisher" -> publisher = text.ifBlank { null }
                                    "genre" -> genre = text.ifBlank { null }
                                    "players" -> players = text.ifBlank { null }
                                    "favorite" -> favorite = text.lowercase() == "true"
                                    "playcount" -> playCount = text.toIntOrNull() ?: 0
                                    "playtime" -> playTime = text.toIntOrNull() ?: 0
                                    "lastplayed" -> lastPlayed = text.ifBlank { null }
                                    "game" -> {
                                        if (path.isNotEmpty()) {
                                            val artworkPath = resolveArtworkPath(systemName, path, mediaPath)
                                            val physicalMediaPath = resolvePhysicalMediaPath(systemName, path, mediaPath)
                                            val marqueePath = resolveMarqueePath(systemName, path, mediaPath)
                                            val romAbsPath = resolveRomPath(systemName, path, romsPaths)
                                            games.add(
                                                GameInfo(
                                                    path = path,
                                                    name = name.ifBlank { File(path).nameWithoutExtension },
                                                    description = desc,
                                                    rating = rating,
                                                    releaseDate = releaseDate,
                                                    developer = developer,
                                                    publisher = publisher,
                                                    genre = genre,
                                                    players = players,
                                                    isFavorite = favorite,
                                                    playCount = playCount,
                                                    playTimeMinutes = playTime,
                                                    lastPlayed = lastPlayed,
                                                    systemName = systemName,
                                                    artworkPath = artworkPath,
                                                    physicalMediaPath = physicalMediaPath,
                                                    marqueeImagePath = marqueePath,
                                                    emulatorPackage = emulatorPackage,
                                                    romAbsolutePath = romAbsPath,
                                                    launchCommand = launchCommand
                                                )
                                            )
                                        }
                                        inGame = false
                                    }
                                }
                                textBuffer.clear()
                            }
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing gamelist.xml for $systemName", e)
        }
        return games
    }

    private fun resolveRomPath(systemName: String, gameFilename: String, romsPaths: List<String>): String? {
        for (romsRoot in romsPaths) {
            val file = File(romsRoot, "$systemName/$gameFilename")
            if (file.exists()) return file.absolutePath
        }
        return null
    }

    private fun resolvePhysicalMediaPath(systemName: String, gameFilename: String, mediaPath: String): String? {
        val nameOnly = File(gameFilename).nameWithoutExtension
        val mediaSystemName = getMediaSystemName(systemName)

        for (sysName in listOf(systemName, mediaSystemName).distinct()) {
            for (ext in IMAGE_EXTENSIONS) {
                val file = File(mediaPath, "$sysName/$FOLDER_PHYSICALMEDIA/$nameOnly.$ext")
                if (file.exists()) return file.absolutePath
            }
        }
        return null
    }

    private fun resolveArtworkPath(systemName: String, gameFilename: String, mediaPath: String): String? {
        val nameOnly = File(gameFilename).nameWithoutExtension
        val mediaSystemName = getMediaSystemName(systemName)

        for (sysName in listOf(systemName, mediaSystemName).distinct()) {
            for (ext in IMAGE_EXTENSIONS) {
                val file = File(mediaPath, "$sysName/$FOLDER_COVERS/$nameOnly.$ext")
                if (file.exists()) return file.absolutePath
            }
        }
        return null
    }

    private fun resolveMarqueePath(systemName: String, gameFilename: String, mediaPath: String): String? {
        val nameOnly = File(gameFilename).nameWithoutExtension
        val mediaSystemName = getMediaSystemName(systemName)

        for (sysName in listOf(systemName, mediaSystemName).distinct()) {
            for (ext in IMAGE_EXTENSIONS_WITH_SVG) {
                val file = File(mediaPath, "$sysName/$FOLDER_MARQUEES/$nameOnly.$ext")
                if (file.exists()) return file.absolutePath
            }
        }

        for (dir in MARQUEE_FALLBACK_DIRS) {
            for (sysName in listOf(systemName, mediaSystemName).distinct()) {
                for (ext in IMAGE_EXTENSIONS_WITH_SVG) {
                    val file = File(mediaPath, "$sysName/$dir/$nameOnly.$ext")
                    if (file.exists()) return file.absolutePath
                }
            }
        }
        return null
    }
}
