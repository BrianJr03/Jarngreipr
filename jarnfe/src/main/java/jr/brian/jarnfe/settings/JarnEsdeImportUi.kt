package jr.brian.jarnfe.settings

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import jr.brian.core.ui.components.JarnDialog
import jr.brian.core.ui.components.JarnSurface
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.library.migrate.EsdeImportSummary
import jr.brian.frontend.library.setup.EsdeDetection
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.input.JarnScreen

/** The app-wide importer, or null when the host app has no library. */
@Composable
internal fun rememberJarnEsdeImporter(): JarnEsdeImporter? {
    val context = LocalContext.current
    return remember(context) { JarnEsdeImport.connect(context) }
}

/** Marks this screen as showing the import, for as long as it's on screen. */
@Composable
internal fun WatchJarnEsdeImport(importer: JarnEsdeImporter, wantsNotice: Boolean = false) {
    DisposableEffect(importer, wantsNotice) {
        val end = importer.watch(wantsNotice)
        onDispose { end() }
    }
}

/**
 * The consent dialog on [screen], and the return from the file access screen. One
 * dialog for first run and Settings. Focus starts on Continue, and B is Cancel: the
 * modal input layer keeps B from reaching the screen underneath. The dialog stays
 * composed while closed, so closing it hands focus back to what opened it.
 */
@Composable
internal fun JarnEsdeImportNoticeHost(screen: JarnScreen) {
    val importer = rememberJarnEsdeImporter() ?: return
    val activity = LocalActivity.current
    ResumeEffect(importer::onResumed)
    val open = importer.consentOn == screen
    RefocusAfterClose(open, importer.consentReturnFocus)
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        enabled = open,
        bindings = {
            hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm)
            on(JarnAction.Back, R.string.jarnfe_hint_back, onPress = importer::cancelConsent)
        },
    ) {
        JarnDialog(
            visible = open,
            title = stringResource(R.string.jarnfe_esde_import_notice_title),
            message = stringResource(R.string.jarnfe_esde_import_notice_message),
            confirmLabel = stringResource(R.string.jarnfe_esde_roms_notice_continue),
            dismissLabel = stringResource(R.string.jarnfe_esde_roms_notice_cancel),
            onConfirm = { importer.confirmConsent { openFileAccess(activity) } },
            onDismiss = importer::cancelConsent,
            restoreFocusTo = importer.consentReturnFocus,
        )
    }
}

/**
 * Hands focus back once more a frame after the dialog closes. Settings moves focus to its tabs
 * as a modal goes away, which wins over the dialog's own immediate restore.
 */
@Composable
private fun RefocusAfterClose(open: Boolean, returnFocus: FocusRequester?) {
    var wasOpen by remember { mutableStateOf(false) }
    LaunchedEffect(open) {
        if (open) {
            wasOpen = true
        } else if (wasOpen) {
            wasOpen = false
            withFrameNanos { }
            withFrameNanos { }
            runCatching { returnFocus?.requestFocus() }
        }
    }
}

@Composable
private fun ResumeEffect(onResume: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onResume()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}

/** Opens Android's All files access screen for this app. False when it couldn't. */
private fun openFileAccess(activity: Activity?): Boolean {
    val host = activity ?: return false
    val intent = Intent(
        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
        "package:${host.packageName}".toUri(),
    )
    return runCatching { host.startActivity(intent) }.isSuccess
}

/** Puts focus back on [focus] after the player returns from Android's file access screen. */
@Composable
internal fun RefocusAfterFileAccess(importer: JarnEsdeImporter, focus: FocusRequester) {
    val returns = importer.accessReturns
    LaunchedEffect(returns) {
        if (returns == 0) return@LaunchedEffect
        withFrameNanos { }
        withFrameNanos { }
        runCatching { focus.requestFocus() }
    }
}

/** "Bringing over 3 of 11", or null before the first gamelist. */
@Composable
internal fun esdeImportProgressText(state: JarnEsdeImportState): String? {
    val progress = (state as? JarnEsdeImportState.Running)?.progress ?: return null
    val done = (progress.done + 1).coerceAtMost(progress.total)
    return pluralStringResource(R.plurals.jarnfe_esde_import_progress, progress.total, done, progress.total)
}

/** "Found 120 games and 8 favorites in ES-DE.", or a plain line when the files can't be read yet. */
@Composable
internal fun esdeFoundText(detection: EsdeDetection?): String {
    if (detection == null || !detection.hasFileAccess) return stringResource(R.string.jarnfe_esde_import_found_unread)
    return stringResource(
        R.string.jarnfe_esde_import_found,
        pluralStringResource(R.plurals.jarnfe_esde_import_count_games, detection.games, detection.games),
        pluralStringResource(R.plurals.jarnfe_esde_import_count_favorites, detection.favorites, detection.favorites),
    )
}

/** "Last import: 40 games, 6 favorites". */
@Composable
internal fun esdeLastImportText(summary: EsdeImportSummary): String = stringResource(
    R.string.jarnfe_esde_import_last,
    pluralStringResource(R.plurals.jarnfe_esde_import_count_games, summary.gamesUpdated, summary.gamesUpdated),
    pluralStringResource(R.plurals.jarnfe_esde_import_count_favorites, summary.favoritesAdded, summary.favoritesAdded),
)

/** The emulator lines of a summary, as (plurals, count). Only counts above 0 get a line. */
internal fun esdeEmulatorLines(summary: EsdeImportSummary): List<Pair<Int, Int>> = listOf(
    R.plurals.jarnfe_esde_import_emulator_systems to summary.emulatorSystemsSet,
    R.plurals.jarnfe_esde_import_emulator_games to summary.emulatorGamesSet,
    R.plurals.jarnfe_esde_import_emulator_skipped to summary.emulatorSkipped,
).filter { (_, count) -> count > 0 }

/** What an import did, one line each. Used by first run and its Ready card. */
@Composable
internal fun JarnEsdeImportSummaryCard(summary: EsdeImportSummary, modifier: Modifier = Modifier) {
    JarnSurface(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(JarnTheme.space.md),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
        ) {
            esdeSummaryLines(summary).forEachIndexed { index, line ->
                EsdeSummaryLine(line, strong = index == 0)
            }
        }
    }
}

@Composable
private fun EsdeSummaryLine(text: String, strong: Boolean) {
    JarnText(
        text = text,
        style = if (strong) JarnTheme.type.labelStrong else JarnTheme.type.caption,
        color = if (strong) JarnTheme.colors.textHigh else JarnTheme.colors.textMid,
    )
}

@Composable
private fun esdeSummaryLines(summary: EsdeImportSummary): List<String> = buildList {
    add(pluralStringResource(R.plurals.jarnfe_esde_import_systems, summary.systemsMatched, summary.systemsMatched))
    add(pluralStringResource(R.plurals.jarnfe_esde_import_games, summary.gamesUpdated, summary.gamesUpdated))
    add(pluralStringResource(R.plurals.jarnfe_esde_import_favorites, summary.favoritesAdded, summary.favoritesAdded))
    add(pluralStringResource(R.plurals.jarnfe_esde_import_playtime, summary.playStatsUpdated, summary.playStatsUpdated))
    esdeEmulatorLines(summary).forEach { (res, count) -> add(pluralStringResource(res, count, count)) }
    val unrecognised = summary.unrecognisedSystems
    val separator = stringResource(R.string.jarnfe_esde_import_list_separator)
    if (unrecognised.isNotEmpty()) {
        add(
            pluralStringResource(
                R.plurals.jarnfe_esde_import_unrecognised,
                unrecognised.size,
                unrecognised.size,
                unrecognised.joinToString(separator),
            ),
        )
    }
}
