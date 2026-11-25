package jr.brian.jarnfe.backup

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnButtonVariant
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import java.text.DateFormat
import java.util.Date

@Composable
internal fun JarnRestorePreviewSheet(
    preview: JarnBackupPreview,
    onCancel: () -> Unit,
    onRestore: () -> Unit,
) {
    val cancel = remember { FocusRequester() }
    JarnDataSheetFrame(stringResource(R.string.jarnfe_restore_preview_title), initialFocus = cancel) {
        JarnReadPane(Modifier.weight(1f, fill = false)) {
            RestoreSummary(preview)
            preview.items.forEach { RestoreItemRow(it) }
            RestoreNotices(preview)
        }
        JarnDataActions {
            JarnButton(
                label = stringResource(R.string.jarnfe_restore_cancel),
                onClick = onCancel,
                modifier = Modifier.weight(1f).focusRequester(cancel),
                variant = JarnButtonVariant.Secondary,
            )
            JarnButton(
                label = stringResource(R.string.jarnfe_restore_confirm),
                onClick = onRestore,
                modifier = Modifier.weight(1f),
                variant = JarnButtonVariant.Danger,
            )
        }
    }
}

@Composable
private fun RestoreSummary(preview: JarnBackupPreview) {
    val date = remember(preview.exportedAtMillis) {
        preview.exportedAtMillis?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)) }
    } ?: stringResource(R.string.jarnfe_restore_date_unknown)
    JarnText(
        text = stringResource(
            R.string.jarnfe_restore_summary,
            date,
            preview.schemaVersion,
            preview.currentSchemaVersion,
        ),
        style = JarnTheme.type.body,
        color = JarnTheme.colors.textMid,
    )
    JarnText(
        text = pluralStringResource(R.plurals.jarnfe_restore_change_count, preview.changeCount, preview.changeCount),
        style = JarnTheme.type.labelStrong,
        color = JarnTheme.colors.textHigh,
    )
}

@Composable
private fun RestoreItemRow(item: JarnBackupItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JarnText(
            text = stringResource(item.area.labelRes()),
            style = JarnTheme.type.label,
            color = JarnTheme.colors.textHigh,
            modifier = Modifier.weight(1f),
        )
        JarnText(
            text = restoreStatusText(item),
            style = JarnTheme.type.caption,
            color = if (item.change.isChange()) JarnTheme.colors.textHigh else JarnTheme.colors.textMid,
        )
    }
}

private fun JarnBackupChange.isChange() =
    this is JarnBackupChange.Replaces || this is JarnBackupChange.Adds

@Composable
private fun restoreStatusText(item: JarnBackupItem): String = when (val change = item.change) {
    JarnBackupChange.Same -> stringResource(R.string.jarnfe_restore_status_same)
    JarnBackupChange.NotIncluded -> stringResource(R.string.jarnfe_restore_status_missing)
    is JarnBackupChange.Adds -> stringResource(R.string.jarnfe_restore_status_adds, change.count)
    JarnBackupChange.Replaces -> if (item.backupCount != null && item.currentCount != null) {
        stringResource(R.string.jarnfe_restore_status_counts, item.currentCount, item.backupCount)
    } else {
        stringResource(R.string.jarnfe_restore_status_changes)
    }
}

@Composable
private fun RestoreNotices(preview: JarnBackupPreview) {
    if (preview.leavesJarnFe) RestoreNotice(R.string.jarnfe_restore_notice_experience)
    if (preview.imageReferenceCount > 0) RestoreNotice(R.string.jarnfe_restore_notice_images)
    RestoreNotice(R.string.jarnfe_restore_notice_icons)
}

@Composable
private fun RestoreNotice(@StringRes textRes: Int) {
    JarnText(stringResource(textRes), JarnTheme.type.caption, JarnTheme.colors.textMid)
}

@StringRes
private fun JarnBackupArea.labelRes(): Int = when (this) {
    JarnBackupArea.HomeLayout -> R.string.jarnfe_backup_area_home
    JarnBackupArea.Folders -> R.string.jarnfe_backup_area_folders
    JarnBackupArea.Dock -> R.string.jarnfe_backup_area_dock
    JarnBackupArea.PinnedApps -> R.string.jarnfe_backup_area_pins
    JarnBackupArea.GameFolders -> R.string.jarnfe_backup_area_game_folders
    JarnBackupArea.HiddenSystems -> R.string.jarnfe_backup_area_hidden_systems
    JarnBackupArea.HiddenGames -> R.string.jarnfe_backup_area_hidden_games
    JarnBackupArea.ArtChoices -> R.string.jarnfe_backup_area_art
    JarnBackupArea.RecentlyPlayed -> R.string.jarnfe_backup_area_recent
    JarnBackupArea.Look -> R.string.jarnfe_backup_area_look
    JarnBackupArea.Wallpaper -> R.string.jarnfe_backup_area_wallpaper
    JarnBackupArea.Scraper -> R.string.jarnfe_backup_area_scraper
    JarnBackupArea.OtherSettings -> R.string.jarnfe_backup_area_other
}
