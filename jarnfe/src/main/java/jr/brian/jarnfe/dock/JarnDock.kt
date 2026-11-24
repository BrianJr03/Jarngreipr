package jr.brian.jarnfe.dock

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.motion.JarnTilePress
import jr.brian.core.ui.motion.rememberJarnTileEnter
import jr.brian.core.ui.focus.jarnInteractive
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.apps.JarnAppOptionsFlow
import jr.brian.jarnfe.apps.JarnAppOptionsTarget
import jr.brian.jarnfe.apps.JarnDrawableIcon
import jr.brian.jarnfe.apps.jarnAppsDataOrNull
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import kotlinx.coroutines.flow.Flow

private val DockTileSize = 52.dp
private val DockIconSize = 36.dp

@Immutable
data class JarnDockApp(
    val packageName: String,
    val label: String,
    val icon: Drawable? = null,
    val iconPath: String? = null,
    val iconVersion: Long = 0L,
)

/** The dock the user already set up. Hidden or uninstalled apps are left out. */
interface JarnDockData {
    val apps: Flow<List<JarnDockApp>>

    fun launch(host: Activity, packageName: String)
}

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnDockDataProvider {
    val jarnDockData: JarnDockData
}

fun Context.jarnDockDataOrNull(): JarnDockData? =
    (applicationContext as? JarnDockDataProvider)?.jarnDockData

@Composable
internal fun JarnDockRow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val data = remember(context) { context.jarnDockDataOrNull() } ?: return
    val apps by remember(data) { data.apps }.collectAsState(emptyList())
    if (apps.isEmpty()) return
    val host = LocalActivity.current
    val options = remember(context) { context.jarnAppsDataOrNull() }
    var focused by remember { mutableStateOf(false) }
    var focusedApp by remember { mutableStateOf<String?>(null) }
    var menuKey by remember { mutableStateOf<String?>(null) }
    val openOptions = options?.let { { packageName: String -> menuKey = packageName } }
    JarnInputLayer(
        enabled = focused && menuKey == null,
        bindings = {
            hint(JarnAction.Confirm, R.string.jarnfe_hint_open)
            val app = focusedApp
            if (app != null && openOptions != null) {
                on(JarnAction.Context, R.string.jarnfe_hint_context) { openOptions(app) }
            }
        },
    ) {
        BoxWithConstraints(modifier.fillMaxWidth().onFocusChanged { focused = it.hasFocus }) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .widthIn(min = maxWidth)
                    .padding(horizontal = JarnTheme.space.focusEdge, vertical = JarnTheme.space.md),
                horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm, Alignment.CenterHorizontally),
            ) {
                apps.forEach { app ->
                    JarnDockTile(
                        app = app,
                        onFocused = { focusedApp = app.packageName },
                        onLongClick = openOptions?.let { open -> { open(app.packageName) } },
                    ) { host?.let { data.launch(it, app.packageName) } }
                }
            }
        }
    }
    val menuApp = menuKey?.let { key -> apps.firstOrNull { it.packageName == key } }
    if (options != null && menuApp != null) {
        JarnAppOptionsFlow(
            data = options,
            target = JarnAppOptionsTarget(menuApp.packageName, menuApp.label, menuApp.iconPath != null),
            extras = emptyList(),
            onClose = { menuKey = null },
            onLeave = { menuKey = null },
        )
    }
}

@Composable
private fun JarnDockTile(
    app: JarnDockApp,
    onFocused: () -> Unit,
    onLongClick: (() -> Unit)?,
    onClick: () -> Unit,
) {
    val shape = JarnTheme.radius.mdShape
    val source = remember { MutableInteractionSource() }
    JarnTilePress {
        Box(
            modifier = Modifier
                .then(rememberJarnTileEnter())
                .size(DockTileSize)
                .semantics { contentDescription = app.label }
                .onFocusChanged { if (it.isFocused) onFocused() }
                .jarnInteractive(shape, source, onLongClick = onLongClick, onClick = onClick)
                .background(JarnTheme.colors.raised, shape),
            contentAlignment = Alignment.Center,
        ) {
            JarnDrawableIcon(app.icon, Modifier.size(DockIconSize))
        }
    }
}
