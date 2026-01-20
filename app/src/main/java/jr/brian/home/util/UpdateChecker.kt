package jr.brian.home.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

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
    val apkVariants: List<ApkVariant>
) {
    // Convenience properties for backward compatibility
    val apkDownloadUrl: String
        get() = apkVariants.firstOrNull()?.downloadUrl ?: ""

    val apkFileName: String
        get() = apkVariants.firstOrNull()?.fileName ?: ""

    val apkSize: Long
        get() = apkVariants.firstOrNull()?.size ?: 0L

    val hasMultipleVariants: Boolean
        get() = apkVariants.size > 1
}

object UpdateChecker {
    /**
     * Check if a new version of the app is available on GitHub Releases
     *
     * @param currentVersionName Current app version name (e.g., "1.2.0")
     * @param owner GitHub repository owner
     * @param repo GitHub repository name
     * @return UpdateInfo containing update availability and details
     */
    suspend fun checkForUpdate(
        currentVersionName: String,
        owner: String = "BrianJr03",
        repo: String = "Jarngreipr"
    ): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val response = URL(url).readText()

            val tagMatch = Regex("\"tag_name\":\\s*\"([^\"]+)\"").find(response)
            val latestVersion = tagMatch?.groupValues?.get(1)?.removePrefix("v") ?: ""

            val bodyMatch = Regex("\"body\":\\s*\"(.*?)\"").find(response)
            val releaseNotes = bodyMatch?.groupValues?.get(1)
                ?.replace("\\n", "\n")
                ?.replace("\\r", "")
                ?: ""

            val downloadUrlMatch = Regex("\"html_url\":\\s*\"([^\"]+)\"").find(response)
            val downloadUrl = downloadUrlMatch?.groupValues?.get(1) ?: ""

            // Parse all APK assets from the assets array
            val apkVariants = parseApkVariants(response)

            val isUpdateAvailable = isNewerVersion(latestVersion, currentVersionName)

            UpdateInfo(
                isUpdateAvailable = isUpdateAvailable,
                latestVersion = latestVersion,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                apkVariants = apkVariants
            )
        } catch (_: Exception) {
            UpdateInfo(
                isUpdateAvailable = false,
                latestVersion = currentVersionName,
                releaseNotes = "",
                downloadUrl = "",
                apkVariants = emptyList()
            )
        }
    }

    /**
     * Parse all APK variants from the GitHub API response
     */
    private fun parseApkVariants(response: String): List<ApkVariant> {
        val variants = mutableListOf<ApkVariant>()

        // Extract the assets array
        val assetsMatch = Regex("\"assets\":\\s*\\[(.*?)\\]", RegexOption.DOT_MATCHES_ALL).find(response)
        val assetsJson = assetsMatch?.groupValues?.get(1) ?: return emptyList()

        // Find all APK assets - match each asset object
        val assetPattern = Regex(
            "\\{[^{}]*?\"name\":\\s*\"([^\"]+\\.apk)\"[^{}]*?\"size\":\\s*(\\d+)[^{}]*?\"browser_download_url\":\\s*\"([^\"]+)\"[^{}]*?\\}",
            RegexOption.DOT_MATCHES_ALL
        )

        // Also try alternate order pattern
        val altAssetPattern = Regex(
            "\\{[^{}]*?\"browser_download_url\":\\s*\"([^\"]+\\.apk)\"[^{}]*?\"name\":\\s*\"([^\"]+)\"[^{}]*?\"size\":\\s*(\\d+)[^{}]*?\\}",
            RegexOption.DOT_MATCHES_ALL
        )

        // Try primary pattern
        assetPattern.findAll(assetsJson).forEach { match ->
            val fileName = match.groupValues[1]
            val size = match.groupValues[2].toLongOrNull() ?: 0L
            val downloadUrl = match.groupValues[3]

            variants.add(
                ApkVariant(
                    fileName = fileName,
                    downloadUrl = downloadUrl,
                    size = size,
                    variantType = VariantType.fromFileName(fileName)
                )
            )
        }

        // If no matches, try alternate pattern
        if (variants.isEmpty()) {
            altAssetPattern.findAll(assetsJson).forEach { match ->
                val downloadUrl = match.groupValues[1]
                val fileName = match.groupValues[2]
                val size = match.groupValues[3].toLongOrNull() ?: 0L

                variants.add(
                    ApkVariant(
                        fileName = fileName,
                        downloadUrl = downloadUrl,
                        size = size,
                        variantType = VariantType.fromFileName(fileName)
                    )
                )
            }
        }

        // If still no matches, try a simpler fallback
        if (variants.isEmpty()) {
            val simplePattern = Regex("\"browser_download_url\":\\s*\"([^\"]+\\.apk)\"")
            simplePattern.findAll(assetsJson).forEach { match ->
                val downloadUrl = match.groupValues[1]
                val fileName = downloadUrl.substringAfterLast("/")

                variants.add(
                    ApkVariant(
                        fileName = fileName,
                        downloadUrl = downloadUrl,
                        size = 0L,
                        variantType = VariantType.fromFileName(fileName)
                    )
                )
            }
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
