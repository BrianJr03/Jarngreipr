package jr.brian.jarnfe.backup

import android.text.format.Formatter
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnButtonVariant
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R

private const val PERCENT = 100f

@Composable
internal fun JarnUpdateSheetContent(sheet: JarnDataSheet, updates: JarnUpdates) {
    when (sheet) {
        is JarnDataSheet.UpdateOffered -> UpdateOfferSheet(sheet.offer, updates)
        is JarnDataSheet.UpdateNeedsPermission -> UpdatePermissionSheet(sheet, updates)
        is JarnDataSheet.UpdateDownloading -> UpdateDownloadingSheet(sheet)
        is JarnDataSheet.UpdateReady -> UpdateReadySheet(sheet, updates)
        is JarnDataSheet.UpdateFailed -> UpdateFailedSheet(sheet, updates)
        else -> Unit
    }
}

@Composable
private fun UpdateOfferSheet(offer: JarnUpdateOffer, updates: JarnUpdates) {
    val later = remember { FocusRequester() }
    JarnDataSheetFrame(stringResource(R.string.jarnfe_update_available_title), initialFocus = later) {
        JarnReadPane(Modifier.weight(1f, fill = false)) { UpdateDetails(offer, updates.currentVersion) }
        offer.variants.forEach { variant -> UpdateVariantRow(offer, variant, updates) }
        if (offer.variants.isEmpty()) {
            JarnButton(
                label = stringResource(R.string.jarnfe_update_open_release),
                onClick = { JarnDataFlow.openReleasePage(updates, offer) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        JarnDataActions {
            JarnButton(
                label = stringResource(R.string.jarnfe_update_later),
                onClick = JarnDataFlow::dismiss,
                modifier = Modifier.weight(1f).focusRequester(later),
                variant = JarnButtonVariant.Secondary,
            )
            JarnButton(
                label = stringResource(R.string.jarnfe_update_skip),
                onClick = { JarnDataFlow.skip(updates, offer) },
                modifier = Modifier.weight(1f),
                variant = JarnButtonVariant.Secondary,
            )
        }
    }
}

@Composable
private fun UpdateDetails(offer: JarnUpdateOffer, currentVersion: String) {
    JarnText(
        text = stringResource(R.string.jarnfe_update_versions, offer.version, currentVersion),
        style = JarnTheme.type.labelStrong,
        color = JarnTheme.colors.textHigh,
    )
    if (offer.isPrerelease) {
        JarnText(
            text = stringResource(R.string.jarnfe_update_prerelease),
            style = JarnTheme.type.caption,
            color = JarnTheme.colors.textMid,
        )
    }
    if (offer.notes.isNotBlank()) {
        JarnText(offer.notes, JarnTheme.type.body, JarnTheme.colors.textMid)
    }
}

@Composable
private fun UpdateVariantRow(offer: JarnUpdateOffer, variant: JarnUpdateVariant, updates: JarnUpdates) {
    val context = LocalContext.current
    val size = Formatter.formatShortFileSize(context, variant.sizeBytes)
    JarnNavigateRow(
        label = stringResource(
            if (variant.isHidden) R.string.jarnfe_update_variant_hidden else R.string.jarnfe_update_variant_standard,
        ),
        onClick = { JarnDataFlow.download(updates, offer, variant) },
        modifier = Modifier.fillMaxWidth(),
        supporting = if (variant.matchesInstall) {
            stringResource(R.string.jarnfe_update_variant_match, size)
        } else {
            size
        },
    )
}

@Composable
private fun UpdatePermissionSheet(sheet: JarnDataSheet.UpdateNeedsPermission, updates: JarnUpdates) {
    val allow = remember { FocusRequester() }
    JarnDataSheetFrame(stringResource(R.string.jarnfe_update_permission_title), initialFocus = allow) {
        JarnText(
            text = stringResource(R.string.jarnfe_update_permission_message),
            style = JarnTheme.type.body,
            color = JarnTheme.colors.textMid,
        )
        JarnDataActions {
            JarnButton(
                label = stringResource(R.string.jarnfe_update_permission_open),
                onClick = { updates.openInstallPermission() },
                modifier = Modifier.weight(1f).focusRequester(allow),
            )
            JarnButton(
                label = stringResource(R.string.jarnfe_update_permission_continue),
                onClick = { JarnDataFlow.download(updates, sheet.offer, sheet.variant) },
                modifier = Modifier.weight(1f),
                variant = JarnButtonVariant.Secondary,
            )
        }
    }
}

@Composable
private fun UpdateDownloadingSheet(sheet: JarnDataSheet.UpdateDownloading) {
    val context = LocalContext.current
    val progress = sheet.progress
    JarnDataSheetFrame(stringResource(R.string.jarnfe_update_downloading), initialFocus = null) {
        JarnDataProgressBar(progress?.percent?.let { it / PERCENT })
        JarnText(
            text = progress?.let {
                Formatter.formatShortFileSize(context, it.downloadedBytes)
            } ?: stringResource(R.string.jarnfe_data_working_hint),
            style = JarnTheme.type.body,
            color = JarnTheme.colors.textMid,
        )
    }
}

@Composable
private fun UpdateReadySheet(sheet: JarnDataSheet.UpdateReady, updates: JarnUpdates) {
    val install = remember { FocusRequester() }
    JarnDataSheetFrame(stringResource(R.string.jarnfe_update_ready_title), initialFocus = install) {
        JarnText(
            text = stringResource(R.string.jarnfe_update_ready_message),
            style = JarnTheme.type.body,
            color = JarnTheme.colors.textMid,
        )
        JarnDataActions {
            JarnButton(
                label = stringResource(R.string.jarnfe_update_later),
                onClick = JarnDataFlow::dismiss,
                modifier = Modifier.weight(1f),
                variant = JarnButtonVariant.Secondary,
            )
            JarnButton(
                label = stringResource(R.string.jarnfe_update_install),
                onClick = { JarnDataFlow.install(updates, sheet.file) },
                modifier = Modifier.weight(1f).focusRequester(install),
            )
        }
    }
}

@Composable
private fun UpdateFailedSheet(sheet: JarnDataSheet.UpdateFailed, updates: JarnUpdates) {
    val retry = remember { FocusRequester() }
    JarnDataSheetFrame(stringResource(R.string.jarnfe_update_failed_title), initialFocus = retry) {
        JarnText(
            text = stringResource(R.string.jarnfe_update_failed_message),
            style = JarnTheme.type.body,
            color = JarnTheme.colors.textMid,
        )
        JarnDataActions {
            JarnButton(
                label = stringResource(R.string.jarnfe_update_retry),
                onClick = { JarnDataFlow.download(updates, sheet.offer, sheet.variant) },
                modifier = Modifier.weight(1f).focusRequester(retry),
            )
            JarnButton(
                label = stringResource(R.string.jarnfe_update_open_release),
                onClick = { JarnDataFlow.openReleasePage(updates, sheet.offer) },
                modifier = Modifier.weight(1f),
                variant = JarnButtonVariant.Secondary,
            )
        }
    }
}
