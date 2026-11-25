package jr.brian.jarnfe.library

import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import jr.brian.core.ui.components.JarnDialog
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.input.JarnScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Hosts the ES-DE notice on [screen], and picks the flow back up after the file access screen.
 * The modal input layer keeps B on the notice, so it cancels instead of reaching the screen below.
 */
@Composable
internal fun JarnEsdeRomsNoticeHost(screen: JarnScreen) {
    val activity = LocalActivity.current
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && activity != null) JarnEsdeRoms.onResumed(activity)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    if (JarnEsdeRoms.noticeOn != screen) return
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        bindings = {
            hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm)
            on(JarnAction.Back, R.string.jarnfe_hint_back, onPress = JarnEsdeRoms::cancel)
        },
    ) {
        JarnDialog(
            visible = true,
            title = stringResource(R.string.jarnfe_esde_roms_notice_title),
            message = stringResource(R.string.jarnfe_esde_roms_notice_message),
            confirmLabel = stringResource(R.string.jarnfe_esde_roms_notice_continue),
            dismissLabel = stringResource(R.string.jarnfe_esde_roms_notice_cancel),
            onConfirm = { JarnEsdeRoms.proceed(activity) },
            onDismiss = JarnEsdeRoms::cancel,
        )
    }
}

/** Null while checking. ES-DE is only named to players who have it. */
@Composable
internal fun rememberJarnHasEsde(): Boolean? {
    val context = LocalContext.current
    val hasEsde by produceState<Boolean?>(null, context) {
        val source = context.jarnEsdeRomsFolderOrNull()
        value = source != null && withContext(Dispatchers.IO) { runCatching { source.hasEsde() }.getOrDefault(false) }
    }
    return hasEsde
}

/** The games folder action for someone with no games: ES-DE's folder when they have ES-DE. */
internal data class JarnGamesFolderAction(@param:StringRes val labelRes: Int, val run: () -> Unit)

@Composable
internal fun rememberJarnGamesFolderAction(screen: JarnScreen): JarnGamesFolderAction =
    if (rememberJarnHasEsde() == true) {
        JarnGamesFolderAction(R.string.jarnfe_esde_roms_use) { JarnEsdeRoms.ask(screen) }
    } else {
        JarnGamesFolderAction(R.string.jarnfe_setup_add_folder) { JarnFolderPicker.open?.invoke(null) }
    }

@Composable
internal fun JarnGamesFolderAction.label(): String = stringResource(labelRes)
