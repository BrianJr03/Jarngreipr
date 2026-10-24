package jr.brian.home.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a single APK variant available for download
 */
data class ApkVariant(
    val fileName: String,
    val downloadUrl: String,
    val size: Long,
    val variantType: VariantType
)

enum class VariantType {
    STANDARD,
    THOR;  // Variants ending in 'h' are for Thor device

    companion object {
        fun fromFileName(fileName: String): VariantType {
            val nameWithoutExtension = fileName.removeSuffix(".apk")
            return if (nameWithoutExtension.endsWith("h", ignoreCase = true)) {
                THOR
            } else {
                STANDARD
            }
        }
    }
}

data class UpdateInfo(
    val isUpdateAvailable: Boolean,
    val latestVersion: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val apkVariants: List<ApkVariant>,
    val isPrerelease: Boolean = false
) {
    val apkSize: Long
        get() = apkVariants.firstOrNull()?.size ?: 0L

    val hasMultipleVariants: Boolean
        get() = apkVariants.size > 1
}

object UpdateChecker {
    /**
     * Check if a new version of the app is available on GitHub Releases.
     *
     * When [includePrereleases] is false (default), queries `/releases/latest`
     * which GitHub excludes `prerelease=true` builds from — i.e. the stable
     * channel. When true, queries `/releases` and picks the newest-tagged
     * entry including RC builds — i.e. the opt-in RC channel.
     *
     * @param currentVersionName Current app version name (e.g., "1.2.0")
     * @param owner GitHub repository owner
     * @param repo GitHub repository name
     * @param includePrereleases Opt in to the RC channel
     * @return UpdateInfo containing update availability and details
     */
    suspend fun checkForUpdate(
        currentVersionName: String,
        owner: String = "BrianJr03",
        repo: String = "Jarngreipr",
        includePrereleases: Boolean = false
    ): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val releaseJson = if (includePrereleases) {
                pickNewestRelease(
                    url = "https://api.github.com/repos/$owner/$repo/releases",
                    currentVersionName = currentVersionName
                ) ?: return@withContext emptyUpdateInfo(currentVersionName)
            } else {
                JSONObject(URL("https://api.github.com/repos/$owner/$repo/releases/latest").readText())
            }

            val latestVersion = releaseJson.optString("tag_name")
                .removePrefix("v")
                .trim()

            val releaseNotes = releaseJson.optString("body").trim()

            val downloadUrl = releaseJson.optString("html_url")

            // Parse all APK assets from the assets array
            val apkVariants = parseApkVariants(releaseJson)

            val isUpdateAvailable = isNewerVersion(latestVersion, currentVersionName)
            val isPrerelease = releaseJson.optBoolean("prerelease", false)

            UpdateInfo(
                isUpdateAvailable = isUpdateAvailable,
                latestVersion = latestVersion,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                apkVariants = apkVariants,
                isPrerelease = isPrerelease
            )
        } catch (_: Exception) {
            emptyUpdateInfo(currentVersionName)
        }
    }

    private fun emptyUpdateInfo(currentVersionName: String) = UpdateInfo(
        isUpdateAvailable = false,
        latestVersion = currentVersionName,
        releaseNotes = "",
        downloadUrl = "",
        apkVariants = emptyList(),
        isPrerelease = false
    )

    /**
     * From the full `/releases` list, pick the newest non-draft entry by
     * semver (ignoring the `prerelease` flag). Falls back to the newest
     * strictly-newer-than-current entry when none dominate the current
     * version, so the RC channel still surfaces an in-flight RC even
     * if its tag happens to equal the installed stable version.
     */
    private fun pickNewestRelease(url: String, currentVersionName: String): JSONObject? {
        val response = URL(url).readText()
        val array = JSONArray(response)
        var best: JSONObject? = null
        var bestVersion = currentVersionName
        for (i in 0 until array.length()) {
            val release = array.optJSONObject(i) ?: continue
            if (release.optBoolean("draft", false)) continue
            val tag = release.optString("tag_name").removePrefix("v").trim()
            if (tag.isBlank()) continue
            if (best == null || isNewerVersion(tag, bestVersion)) {
                best = release
                bestVersion = tag
            }
        }
        return best
    }

    /**
     * Parse all APK variants from the GitHub API response.
     */
    private fun parseApkVariants(releaseJson: JSONObject): List<ApkVariant> {
        val variants = mutableListOf<ApkVariant>()
        val assets = releaseJson.optJSONArray("assets") ?: return emptyList()
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val downloadUrl = asset.optString("browser_download_url")
            if (!downloadUrl.endsWith(".apk", ignoreCase = true)) continue
            val fileName = asset.optString("name").ifBlank { downloadUrl.substringAfterLast("/") }
            val size = asset.optLong("size", 0L)
            variants.add(
                ApkVariant(
                    fileName = fileName,
                    downloadUrl = downloadUrl,
                    size = size,
                    variantType = VariantType.fromFileName(fileName)
                )
            )
        }
        // Sort: Standard first, then Thor variant
        return variants.sortedBy { it.variantType.ordinal }
    }

    /**
     * Compare two semantic version strings
     * @return true if latestVersion is newer than currentVersion
     */
    private fun isNewerVersion(latestVersion: String, currentVersion: String): Boolean {
        if (latestVersion.isBlank() || currentVersion.isBlank()) return false

        // Remove any suffix like 'h' for comparison
        val cleanLatest = latestVersion.replace(Regex("[a-zA-Z]+$"), "")
        val cleanCurrent = currentVersion.replace(Regex("[a-zA-Z]+$"), "")

        val latestParts = cleanLatest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(latestParts.size, currentParts.size)

        for (i in 0 until maxLength) {
            val latestPart = latestParts.getOrElse(i) { 0 }
            val currentPart = currentParts.getOrElse(i) { 0 }

            if (latestPart > currentPart) return true
            if (latestPart < currentPart) return false
        }

        return false
    }
}
