package jr.brian.jarnfe.utilities

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnEmptyState
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.JarnRevealLazyItems
import jr.brian.core.ui.focus.jarnInteractive
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.apps.jarnAppsDataOrNull
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.tools.ToolsInitialFocus

private val RecentIconSize = 36.dp

private sealed interface RecentAppsState {
    data object Loading : RecentAppsState
    data object NeedsAccess : RecentAppsState
    data class Loaded(val apps: List<JarnRecentApp>) : RecentAppsState
}

/** Loads once on open, on each return to this screen, and on Refresh. It never polls. */
@Composable
internal fun JarnRecentAppsPanel(utilities: JarnUtilities, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val host = LocalActivity.current
    val live = rememberUtilityLive()
    var refresh by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<RecentAppsState>(RecentAppsState.Loading) }
    LaunchedEffect(utilities, live, refresh) {
        if (!live) return@LaunchedEffect
        state = if (utilities.hasUsageAccess()) {
            RecentAppsState.Loaded(utilities.recentApps())
        } else {
            RecentAppsState.NeedsAccess
        }
    }
    UtilityLayer(
        onBack = onClose,
        bindings = {
            hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm)
            if (state is RecentAppsState.Loaded) {
                on(JarnAction.Search, R.string.jarnfe_utilities_refresh) { refresh++ }
            }
        },
    ) {
        Box(modifier.fillMaxSize()) {
            when (val current = state) {
                RecentAppsState.Loading -> RecentMessage(
                    stringResource(R.string.jarnfe_utilities_loading),
                    stringResource(R.string.jarnfe_utilities_recents),
                )
                RecentAppsState.NeedsAccess -> UsageAccessPrompt {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
                is RecentAppsState.Loaded -> if (current.apps.isEmpty()) {
                    RecentMessage(
                        stringResource(R.string.jarnfe_utilities_recents_empty_title),
                        stringResource(R.string.jarnfe_utilities_recents_empty_message),
                    )
                } else {
                    RecentAppsList(current.apps) { app ->
                        host?.let { context.jarnAppsDataOrNull()?.launchApp(it, app.packageName) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentMessage(title: String, message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        JarnEmptyState(title = title, message = message)
    }
}

@Composable
private fun UsageAccessPrompt(onGrant: () -> Unit) {
    val button = remember { FocusRequester() }
    ToolsInitialFocus(button)
    Column(
        modifier = Modifier.fillMaxSize().padding(JarnTheme.space.lg),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        UtilityTitle(stringResource(R.string.jarnfe_utilities_usage_title))
        UtilityNote(stringResource(R.string.jarnfe_utilities_usage_message))
        JarnButton(
            label = stringResource(R.string.jarnfe_utilities_usage_grant),
            onClick = onGrant,
            modifier = Modifier.focusRequester(button),
        )
    }
}

@Composable
private fun RecentAppsList(apps: List<JarnRecentApp>, onLaunch: (JarnRecentApp) -> Unit) {
    val first = remember { FocusRequester() }
    val list = rememberLazyListState()
    ToolsInitialFocus(first)
    JarnRevealLazyItems(list) {
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(JarnTheme.space.lg),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
        ) {
            item(key = "title") { UtilityTitle(stringResource(R.string.jarnfe_utilities_recents)) }
            items(apps, key = { it.packageName }) { app ->
                RecentAppRow(
                    app = app,
                    onClick = { onLaunch(app) },
                    modifier = if (app == apps.first()) Modifier.focusRequester(first) else Modifier,
                )
            }
        }
    }
}

@Composable
private fun RecentAppRow(app: JarnRecentApp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = JarnTheme.radius.smShape
    val source = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .jarnInteractive(shape, source, onClick = onClick)
            .background(JarnTheme.colors.surface, shape)
            .padding(horizontal = JarnTheme.space.md, vertical = JarnTheme.space.sm),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(model = app.icon, contentDescription = null, modifier = Modifier.size(RecentIconSize))
        JarnText(
            text = app.label,
            style = JarnTheme.type.label,
            color = JarnTheme.colors.textHigh,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        UtilityNote(recentUsage(app.usageMs))
    }
}

@Composable
private fun recentUsage(ms: Long): String {
    val duration = jarnDuration(ms)
    return if (duration.hours == 0L && duration.minutes == 0L) {
        stringResource(R.string.jarnfe_utilities_under_minute)
    } else {
        stringResource(R.string.jarnfe_utilities_duration, duration.hours, duration.minutes)
    }
}
