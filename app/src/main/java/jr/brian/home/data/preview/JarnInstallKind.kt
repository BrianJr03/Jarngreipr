package jr.brian.home.data.preview

import android.content.Context

/** Whether this launch follows a fresh install or an update. Both get the preview, by the PM's call. */
enum class JarnInstallKind { FreshInstall, Update }

/**
 * An update when What's New already saw a version, which also covers a reinstall that Android's
 * backup restored, or when the package was updated since it was installed.
 */
internal fun installKindOf(
    lastSeenWhatsNewVersion: String?,
    firstInstallTime: Long,
    lastUpdateTime: Long,
): JarnInstallKind =
    if (!lastSeenWhatsNewVersion.isNullOrEmpty() || firstInstallTime != lastUpdateTime) {
        JarnInstallKind.Update
    } else {
        JarnInstallKind.FreshInstall
    }

/** Reads the same pref that `WhatsNewManager` writes, so read it before What's New marks a version seen. */
internal fun Context.detectInstallKind(): JarnInstallKind {
    val lastSeen = getSharedPreferences(WHATS_NEW_PREFS, Context.MODE_PRIVATE)
        .getString(WHATS_NEW_LAST_SEEN_VERSION, null)
    val info = runCatching { packageManager.getPackageInfo(packageName, 0) }.getOrNull()
    return installKindOf(lastSeen, info?.firstInstallTime ?: 0L, info?.lastUpdateTime ?: 0L)
}

private const val WHATS_NEW_PREFS = "whats_new_prefs"
private const val WHATS_NEW_LAST_SEEN_VERSION = "last_seen_version"
