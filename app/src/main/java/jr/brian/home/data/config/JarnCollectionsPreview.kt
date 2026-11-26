package jr.brian.home.data.config

import jr.brian.jarnfe.backup.JarnBackupArea
import jr.brian.jarnfe.backup.JarnBackupChange
import jr.brian.jarnfe.backup.JarnBackupItem

/** Restoring collections only adds, so the count is new collections plus new members in existing ones. */
internal fun collectionsItem(
    backup: List<JarnCollectionConfig>?,
    current: List<JarnCollectionConfig>?,
): JarnBackupItem {
    val currentCount = current?.size
    if (backup == null) return JarnBackupItem(JarnBackupArea.Collections, null, currentCount, JarnBackupChange.NotIncluded)
    val existing = current.orEmpty()
    val added = backup.sumOf { collection ->
        val match = existing.firstOrNull { it.name.trim().equals(collection.name.trim(), ignoreCase = true) }
        when {
            match != null -> collection.members.distinct().count { it !in match.members }
            collection.name.isBlank() -> 0
            else -> 1 + collection.members.distinct().size
        }
    }
    return addsItem(JarnBackupArea.Collections, backup.size, currentCount ?: 0, added)
}

/** Restoring favorites only adds, never unfavorites. */
internal fun favoritesItem(backup: List<JarnGameKeyConfig>?, current: List<JarnGameKeyConfig>?): JarnBackupItem {
    if (backup == null) return JarnBackupItem(JarnBackupArea.Favorites, null, current?.size, JarnBackupChange.NotIncluded)
    val existing = current.orEmpty().toSet()
    val added = backup.filter { it.systemId.isNotBlank() && it.relativePath.isNotBlank() }.distinct().count { it !in existing }
    return addsItem(JarnBackupArea.Favorites, backup.size, current?.size ?: 0, added)
}

private fun addsItem(area: JarnBackupArea, backupCount: Int, currentCount: Int, added: Int) = JarnBackupItem(
    area = area,
    backupCount = backupCount,
    currentCount = currentCount,
    change = if (added > 0) JarnBackupChange.Adds(added) else JarnBackupChange.Same,
)
