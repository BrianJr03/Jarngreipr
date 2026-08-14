package jr.brian.home.ui.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.esde.ui.components.ToggleSetting
import jr.brian.home.service.HomeInterceptionKeepAliveService
import jr.brian.home.service.HomeInterceptorService
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.managers.LocalHomeButtonManager
import jr.brian.home.util.MjolnirDetection
import jr.brian.home.util.ThorDetection

/**
 * Extras-section entry for the Home Button Interception toggle plus its confirmation
 * dialog. The toggle is a gate, not a raw switch — flipping to ON always opens a
 * dialog (even after the first time) so a fresh Mjolnir install can't slip past.
 *
 * The keep-alive foreground service is started/stopped in lockstep with the
 * persisted flag. Toggling off must NOT require the user to manually revoke the
 * accessibility permission for the feature to stop working, so the service's
 * `onKeyEvent` also falls through immediately once [interceptionEnabled] is false.
 */
@Composable
fun HomeInterceptionSettingItem() {
    // Hidden on non-Thor hardware: on stock AOSP the interception service never
    // sees KEYCODE_HOME (PhoneWindowManager consumes it first), so surfacing the
    // toggle would only confuse users on devices where the feature can't work.
    if (!ThorDetection.isThor()) return

    val context = LocalContext.current
    val homeButtonManager = LocalHomeButtonManager.current
    val interceptionEnabled by homeButtonManager.interceptionEnabled.collectAsStateWithLifecycle()

    var accessibilityGranted by remember {
        mutableStateOf(HomeInterceptorService.isAccessibilityEnabled(context))
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityGranted =
                    HomeInterceptorService.isAccessibilityEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var pendingEnable by remember { mutableStateOf(false) }

    // Re-checked when the dialog is opened (see below) so an install/uninstall
    // between opens is reflected.
    var mjolnirInstalled by remember { mutableStateOf(MjolnirDetection.isInstalled(context)) }
    val description = if (interceptionEnabled && !accessibilityGranted) {
        stringResource(R.string.home_interception_permission_required)
    } else {
        stringResource(R.string.home_interception_description)
    }

    ToggleSetting(
        title = stringResource(R.string.home_interception_title),
        description = description,
        checked = interceptionEnabled,
        onCheckedChange = { requested ->
            if (requested) {
                mjolnirInstalled = MjolnirDetection.isInstalled(context)
                pendingEnable = true
            } else {
                homeButtonManager.setInterceptionEnabled(false)
                HomeInterceptionKeepAliveService.stop(context)
            }
        }
    )

    if (pendingEnable) {
        HomeInterceptionConfirmDialog(
            mjolnirInstalled = mjolnirInstalled,
            onDismiss = { pendingEnable = false },
            onOpenAccessibilitySettings = {
                context.startActivity(HomeInterceptorService.accessibilitySettingsIntent())
            },
            onContinue = {
                pendingEnable = false
                homeButtonManager.setInterceptionEnabled(true)
                HomeInterceptionKeepAliveService.start(context)
                if (!HomeInterceptorService.isAccessibilityEnabled(context)) {
                    context.startActivity(HomeInterceptorService.accessibilitySettingsIntent())
                }
            }
        )
    }
}

@Composable
private fun HomeInterceptionConfirmDialog(
    mjolnirInstalled: Boolean,
    onDismiss: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OledCardColor,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = stringResource(R.string.home_interception_dialog_title),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val body = if (mjolnirInstalled) {
                    stringResource(R.string.home_interception_dialog_body_mjolnir)
                } else {
                    stringResource(R.string.home_interception_dialog_body_generic)
                }
                Text(
                    text = body,
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (mjolnirInstalled) {
                    TextButton(
                        onClick = onOpenAccessibilitySettings,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(stringResource(R.string.home_interception_dialog_open_accessibility))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(stringResource(R.string.home_interception_dialog_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.home_interception_dialog_cancel))
            }
        }
    )
}

