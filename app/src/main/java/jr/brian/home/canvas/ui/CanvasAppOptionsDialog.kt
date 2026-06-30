package jr.brian.home.canvas.ui

import androidx.compose.runtime.Composable
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.components.dialog.AppOptionsDialog

/**
 * Options dialog for a single canvas app tile. Mirrors the look of the other
 * canvas dialogs by reusing [AppOptionsDialog]/`AppOptionsMenuContent` directly
 * — the per-app button grid (Info / Hide / Icon / Rename / Top/Bottom display)
 * already lives there, so this wrapper just narrows the surface area to what
 * the canvas needs and routes the "Hide" action through the
 * canvas-removal path supplied by the caller.
 *
 * Resize is intentionally suppressed (`showResizeOption = false`): canvas tiles
 * resize via the grid's corner handle / [CanvasResizeDialog], not via icon
 * scaling.
 */
@Composable
fun CanvasAppOptionsDialog(
    app: AppInfo,
    currentDisplayPreference: DisplayPreference,
    hasExternalDisplay: Boolean,
    onDismiss: () -> Unit,
    onAppInfoClick: () -> Unit,
    onDisplayPreferenceChange: (DisplayPreference) -> Unit,
    onRemoveFromCanvas: () -> Unit,
    onCustomIconClick: () -> Unit,
    onRenameClick: () -> Unit,
    onEditCanvas: () -> Unit
) {
    AppOptionsDialog(
        app = app,
        currentDisplayPreference = currentDisplayPreference,
        hasExternalDisplay = hasExternalDisplay,
        onDismiss = onDismiss,
        onAppInfoClick = onAppInfoClick,
        onDisplayPreferenceChange = onDisplayPreferenceChange,
        onHideApp = onRemoveFromCanvas,
        onCustomIconClick = onCustomIconClick,
        onRenameClick = onRenameClick,
        showResizeOption = false,
        isInDock = false,
        onEditCanvas = onEditCanvas
    )
}
