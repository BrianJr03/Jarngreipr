package jr.brian.home.data.config

import jr.brian.home.canvas.data.CanvasTabType
import jr.brian.home.data.FolderManager
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.app.Folder

internal const val FOLDER_BACKUP_PAGE_COUNT = 6

/**
 * Present in a backup that lists every readable page, including empty ones, so a page missing
 * from it was unreadable. It has no underscore, which is why older builds skip it on restore.
 */
internal const val FOLDER_BACKUP_EXPLICIT_PAGES_KEY = "explicitPages"

internal val FolderBackupTabTypes = listOf(
    FolderManager.TAB_TYPE_APPS,
    FolderManager.TAB_TYPE_WIDGETS,
    CanvasTabType.VALUE,
)

internal sealed interface FolderRestoreStep {
    val page: Int
    val tabType: String

    data class Replace(
        override val page: Int,
        override val tabType: String,
        val folders: List<FolderItemConfig>,
    ) : FolderRestoreStep

    /** Stored data this device can't read is kept, because clearing it can't be undone. */
    data class ClearIfReadable(
        override val page: Int,
        override val tabType: String,
    ) : FolderRestoreStep
}

/** [read] returns null for a page whose stored folders can't be read, leaving it out. */
internal inline fun buildFolderBackup(
    read: (page: Int, tabType: String) -> List<FolderItemConfig>?,
): Map<String, List<FolderItemConfig>> {
    val result = mutableMapOf<String, List<FolderItemConfig>>(
        FOLDER_BACKUP_EXPLICIT_PAGES_KEY to emptyList(),
    )
    forEachFolderSlot { page, tabType ->
        read(page, tabType)?.let { result[folderBackupKey(tabType, page)] = it }
    }
    return result
}

internal fun omittedFolderPageCount(folders: Map<String, List<FolderItemConfig>>): Int {
    if (FOLDER_BACKUP_EXPLICIT_PAGES_KEY !in folders) return 0
    var omitted = 0
    forEachFolderSlot { page, tabType ->
        if (folderBackupKey(tabType, page) !in folders) omitted += 1
    }
    return omitted
}

/**
 * A listed page is restored as listed. A missing page is left alone in a backup with the explicit
 * pages key. Older backups only list pages with folders, so there a missing page means empty.
 */
internal fun planFolderRestore(folders: Map<String, List<FolderItemConfig>>): List<FolderRestoreStep> {
    val listed = folders.mapNotNull { (key, items) -> restoreStepFor(key, items) }
    if (FOLDER_BACKUP_EXPLICIT_PAGES_KEY in folders) return listed
    val listedSlots = listed.map { it.tabType to it.page }.toSet()
    val missing = mutableListOf<FolderRestoreStep>()
    forEachFolderSlot { page, tabType ->
        if (tabType to page !in listedSlots) {
            missing += FolderRestoreStep.ClearIfReadable(page, tabType)
        }
    }
    return missing + listed
}

/** The non-empty pages a restore would leave, for an accurate preview. */
internal fun foldersAfterRestore(
    backup: Map<String, List<FolderItemConfig>>,
    current: Map<String, List<FolderItemConfig>>,
): Map<String, List<FolderItemConfig>> {
    val result = current.filterValues { it.isNotEmpty() }.toMutableMap()
    planFolderRestore(backup).forEach { step ->
        val key = folderBackupKey(step.tabType, step.page)
        when (step) {
            is FolderRestoreStep.Replace -> result[key] = step.folders
            is FolderRestoreStep.ClearIfReadable -> result.remove(key)
        }
    }
    return result
}

internal fun Folder.toFolderItemConfig() = FolderItemConfig(
    id = id,
    name = name,
    apps = appPackageNames,
    x = position.x,
    y = position.y,
    iconSize = position.iconSize,
    backgroundColorArgb = backgroundColorArgb,
    backgroundImagePath = backgroundImagePath,
)

internal fun FolderItemConfig.toFolder() = Folder(
    id = id,
    name = name,
    appPackageNames = apps,
    position = AppPosition(id, x, y, iconSize),
    backgroundColorArgb = backgroundColorArgb,
    backgroundImagePath = backgroundImagePath,
)

internal fun folderBackupKey(tabType: String, page: Int): String = "${tabType}_$page"

internal inline fun forEachFolderSlot(action: (page: Int, tabType: String) -> Unit) {
    for (page in 0 until FOLDER_BACKUP_PAGE_COUNT) {
        for (tabType in FolderBackupTabTypes) action(page, tabType)
    }
}

private fun restoreStepFor(key: String, items: List<FolderItemConfig>): FolderRestoreStep? {
    val separator = key.lastIndexOf('_')
    if (separator == -1) return null
    val tabType = key.substring(0, separator)
    val page = key.substring(separator + 1).toIntOrNull() ?: return null
    return if (items.isEmpty()) {
        FolderRestoreStep.ClearIfReadable(page, tabType)
    } else {
        FolderRestoreStep.Replace(page, tabType, items)
    }
}
