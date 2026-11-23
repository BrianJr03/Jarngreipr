package jr.brian.jarnfe.utilities

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnButtonVariant
import jr.brian.core.ui.components.JarnEmptyState
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.focus.JarnRevealLazyItems
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.tools.ToolsInitialFocus
import kotlinx.coroutines.launch
import java.util.Date

/** Crash reports stay on the device: this screen can read and delete them, nothing else. */
@Composable
internal fun JarnCrashLogsPanel(utilities: JarnUtilities, onClose: () -> Unit, modifier: Modifier = Modifier) {
    var logs by remember { mutableStateOf<List<JarnCrashEntry>?>(null) }
    var openName by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(utilities) { logs = utilities.crashLogs() }
    val open = logs?.firstOrNull { it.fileName == openName }
    val readerFocus = remember { FocusRequester() }
    val clearFocus = remember { FocusRequester() }
    Box(modifier.fillMaxSize()) {
        if (open != null) {
            CrashLogReader(open, readerFocus, onBack = { openName = null }, onDelete = { confirmDelete = true })
        } else {
            CrashLogList(
                logs = logs,
                clearFocus = clearFocus,
                onOpen = { openName = it.fileName },
                onClear = { confirmClear = true },
                onClose = onClose,
            )
        }
        UtilityConfirmDialog(
            visible = confirmDelete && open != null,
            title = stringResource(R.string.jarnfe_utilities_crash_delete_title),
            message = stringResource(R.string.jarnfe_utilities_crash_delete_message),
            confirmLabel = stringResource(R.string.jarnfe_utilities_delete),
            onConfirm = {
                confirmDelete = false
                openName = null
                open?.let { entry ->
                    scope.launch {
                        utilities.deleteCrashLog(entry.fileName)
                        logs = utilities.crashLogs()
                    }
                }
            },
            onDismiss = { confirmDelete = false },
            restoreFocusTo = readerFocus.takeIf { open != null },
        )
        UtilityConfirmDialog(
            visible = confirmClear,
            title = stringResource(R.string.jarnfe_utilities_crash_clear_title),
            message = stringResource(R.string.jarnfe_utilities_crash_clear_message),
            confirmLabel = stringResource(R.string.jarnfe_utilities_clear_all),
            onConfirm = {
                confirmClear = false
                scope.launch {
                    utilities.clearCrashLogs()
                    logs = utilities.crashLogs()
                }
            },
            onDismiss = { confirmClear = false },
            restoreFocusTo = clearFocus.takeIf { open == null && !logs.isNullOrEmpty() },
        )
    }
}

@Composable
private fun CrashLogReader(
    entry: JarnCrashEntry,
    focus: FocusRequester,
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    UtilityReader(
        title = crashTimestamp(entry.timestamp),
        body = entry.content,
        onBack = onBack,
        monospace = true,
        bodyFocus = focus,
        bindings = { on(JarnAction.Context, R.string.jarnfe_utilities_delete, onPress = onDelete) },
    )
}

@Composable
private fun CrashLogList(
    logs: List<JarnCrashEntry>?,
    clearFocus: FocusRequester,
    onOpen: (JarnCrashEntry) -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
) {
    val first = remember { FocusRequester() }
    val list = rememberLazyListState()
    if (!logs.isNullOrEmpty()) ToolsInitialFocus(first)
    UtilityLayer(onBack = onClose, bindings = {
        if (!logs.isNullOrEmpty()) hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm)
    }) {
        when {
            logs == null -> CrashLogsMessage(R.string.jarnfe_utilities_loading, R.string.jarnfe_utilities_crashes)
            logs.isEmpty() -> CrashLogsMessage(
                R.string.jarnfe_utilities_crash_empty_title,
                R.string.jarnfe_utilities_crash_empty_message,
            )
            else -> JarnRevealLazyItems(list) {
                LazyColumn(
                    state = list,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(JarnTheme.space.lg),
                    verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
                ) {
                    item(key = "title") { UtilityTitle(stringResource(R.string.jarnfe_utilities_crashes)) }
                    items(logs, key = { it.fileName }) { log ->
                        JarnNavigateRow(
                            label = crashTimestamp(log.timestamp),
                            supporting = log.preview,
                            onClick = { onOpen(log) },
                            modifier = if (log == logs.first()) Modifier.focusRequester(first) else Modifier,
                        )
                    }
                    item(key = "clear") {
                        JarnButton(
                            label = stringResource(R.string.jarnfe_utilities_clear_all),
                            onClick = onClear,
                            modifier = Modifier.focusRequester(clearFocus),
                            variant = JarnButtonVariant.Secondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CrashLogsMessage(titleRes: Int, messageRes: Int) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        JarnEmptyState(title = stringResource(titleRes), message = stringResource(messageRes))
    }
}

@Composable
private fun crashTimestamp(timestamp: Long): String {
    val context = LocalContext.current
    return remember(context, timestamp) {
        val date = Date(timestamp)
        "${DateFormat.getMediumDateFormat(context).format(date)} ${DateFormat.getTimeFormat(context).format(date)}"
    }
}
