package jr.brian.home.data.config

import jr.brian.home.data.preview.JarnPreviewStore
import kotlinx.serialization.Serializable

/**
 * v57: the Meet Jarn preview's showings, the last version it opened for, and whether Jarn was
 * tried. Each field is nullable so a partial backup keeps the rest.
 */
@Serializable
data class JarnPreviewConfig(
    val showings: Int? = null,
    val lastShownVersion: String? = null,
    val tried: Boolean? = null,
)

internal fun JarnPreviewStore.previewBackup(): JarnPreviewConfig = current.let {
    JarnPreviewConfig(showings = it.showings, lastShownVersion = it.lastShownVersion, tried = it.tried)
}

/** Null means the backup predates v57, so nothing changes. */
internal fun JarnPreviewStore.restorePreview(backup: JarnPreviewConfig?) {
    backup ?: return
    restore(backup.showings, backup.lastShownVersion, backup.tried)
}
