package jr.brian.home.esde.util

import android.util.Log
import java.io.File

/**
 * Utility for parsing ES-DE gamelist.xml files to extract game metadata.
 */
object GamelistParser {
    private const val TAG = "GamelistParser"

    /**
     * Parse game description from ES-DE gamelist.xml
     * Returns null if not found or any error occurs
     *
     * @param esdeRootPath The root ES-DE folder path (parent of gamelists folder)
     * @param systemName The system name (e.g., "snes", "n64")
     * @param gameFilename The game filename as received from ES-DE events
     */
    fun getGameDescription(esdeRootPath: String, systemName: String, gameFilename: String): String? {
        try {
            val esdeRoot = File(esdeRootPath)
            if (!esdeRoot.exists()) {
                Log.d(TAG, "ES-DE root folder not found: $esdeRootPath")
                return null
            }

            // Build path to gamelist.xml: ~/ES-DE/gamelists/<systemname>/gamelist.xml
            val gamelistFile = File(esdeRoot, "gamelists/$systemName/gamelist.xml")

            Log.d(TAG, "Looking for gamelist: ${gamelistFile.absolutePath}")

            if (!gamelistFile.exists()) {
                Log.d(TAG, "Gamelist file not found for system: $systemName")
                return null
            }

            // Parse XML to find the game's description
            val xmlContent = gamelistFile.readText()

            // Sanitize the game filename for comparison
            val sanitizedFilename = sanitizeGameFilename(gameFilename)

            Log.d(TAG, "Searching for game: '$sanitizedFilename'")

            // ES-DE only encodes & as &amp; in <path> tags, not other characters like ' or "
            // So we need to try the filename with & encoded
            val filenameWithEncodedAmpersand = sanitizedFilename.replace("&", "&amp;")

            // Try to find the game with different encoding strategies
            var pathMatch: MatchResult? = null

            // Strategy 1: Try with & encoded (most common case)
            if (filenameWithEncodedAmpersand != sanitizedFilename) {
                Log.d(TAG, "  Trying with &amp; encoding: '$filenameWithEncodedAmpersand'")
                val pattern1 = "<path>\\./\\Q$filenameWithEncodedAmpersand\\E</path>".toRegex()
                pathMatch = pattern1.find(xmlContent)
            }

            // Strategy 2: Try exact match (for files without &)
            if (pathMatch == null) {
                Log.d(TAG, "  Trying exact match: '$sanitizedFilename'")
                val pattern2 = "<path>\\./\\Q$sanitizedFilename\\E</path>".toRegex()
                pathMatch = pattern2.find(xmlContent)
            }

            if (pathMatch == null) {
                Log.d(TAG, "Game not found in gamelist: $sanitizedFilename")
                return null
            }

            Log.d(TAG, "Game found in gamelist!")

            // Find the <desc> tag after this <path> tag
            val gameStartIndex = pathMatch.range.first

            // Search for <desc>...</desc> within this game entry (before next <game> tag)
            val remainingXml = xmlContent.substring(gameStartIndex)
            val nextGameIndex = remainingXml.indexOf("<game>", startIndex = 1)
            val searchSpace = if (nextGameIndex > 0) {
                remainingXml.substring(0, nextGameIndex)
            } else {
                remainingXml
            }

            // Extract description text between <desc> and </desc>
            val descPattern = "<desc>([\\s\\S]*?)</desc>".toRegex()
            val descMatch = descPattern.find(searchSpace)

            return if (descMatch != null) {
                // Decode XML entities in the description text
                val rawDescription = descMatch.groupValues[1].trim()
                val description = decodeXmlEntities(rawDescription)
                Log.d(TAG, "Found description: ${description.take(100)}...")
                description
            } else {
                Log.d(TAG, "No <desc> tag found for game: $sanitizedFilename")
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing gamelist.xml", e)
            return null
        }
    }

    /**
     * Sanitize game filename by removing path prefixes and normalizing
     */
    private fun sanitizeGameFilename(gameFilename: String): String {
        // Remove any leading ./ or path components, keep just the filename
        return gameFilename
            .removePrefix("./")
            .trim()
    }

    /**
     * Decode common XML entities in text content
     */
    private fun decodeXmlEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&#34;", "\"")
            .replace("&#10;", "\n")
            .replace("&#13;", "\r")
    }
}
