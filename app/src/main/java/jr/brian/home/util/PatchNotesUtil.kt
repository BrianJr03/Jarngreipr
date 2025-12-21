package jr.brian.home.util

import android.content.Context
import jr.brian.home.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object PatchNotesUtil {
    fun getLocalPatchNotes(context: Context): String {
        return try {
            context.resources.openRawResource(R.raw.patch_notes)
                .bufferedReader()
                .use { it.readText() }
        } catch (_: Exception) {
            getFallbackPatchNotes()
        }
    }

    /**
     * Fetch patch notes with smart fallback strategy:
     * 1. Check if local patch notes contain the current version
     * 2. If not, try to fetch from GitHub Releases
     * 3. If GitHub content is empty or fails, use local resource file
     * 4. If that fails, use hardcoded fallback
     *
     * @param context Android context
     * @param currentVersionName Current app version name
     * @param owner GitHub repository owner
     * @param repo GitHub repository name
     * @return Patch notes content
     */
    suspend fun fetchPatchNotesWithFallback(
        context: Context,
        currentVersionName: String,
        owner: String = "BrianJr03",
        repo: String = "Jarngreipr"
    ): String = withContext(Dispatchers.IO) {
        val localNotes = getLocalPatchNotes(context)

        val hasCurrentVersion = localNotes.endsWith(
            currentVersionName,
            ignoreCase = true
        )

        return@withContext if (hasCurrentVersion) {
            localNotes
        } else {
            try {
                val url = "https://api.github.com/repos/$owner/$repo/releases/latest"
                val response = URL(url).readText()
                val bodyMatch = Regex("\"body\":\\s*\"(.*?)\"").find(response)
                val githubNotes = bodyMatch?.groupValues?.get(1)
                    ?.replace("\\n", "\n")
                    ?.replace("\\r", "")
                if (!githubNotes.isNullOrBlank()) {
                    githubNotes
                } else {
                    localNotes
                }
            } catch (_: Exception) {
                localNotes
            }
        }
    }

    private fun getFallbackPatchNotes(): String {
        return """
            # Welcome!
            
            ## What's New
            
            ### New Features
            - Improved app performance and stability
            
            ### Bug Fixes
            - Fixed various crashes and issues
          
            ---
        """.trimIndent()
    }
}
