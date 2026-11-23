package jr.brian.jarnfe.apps

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.jarnfe.R
import jr.brian.jarnfe.canvas.CanvasKeyboardSheet
import jr.brian.jarnfe.canvas.findActivity
import jr.brian.jarnfe.canvas.jarnFolderNameOrNull
import jr.brian.jarnfe.canvas.opposite
import jr.brian.jarnfe.shell.JarnNotices
import kotlinx.coroutines.launch

private val ImageTypes = arrayOf("image/*")

@Immutable
internal data class JarnAppOptionsTarget(
    val packageName: String,
    val label: String,
    val hasCustomIcon: Boolean,
)

/** A row only one surface offers, such as Pin in the drawer or Remove in a folder. */
internal class JarnAppOptionsExtra(@param:StringRes val label: Int, val onClick: () -> Unit)

private sealed interface OptionsStep {
    data object Menu : OptionsStep
    data object Rename : OptionsStep
    data object Icon : OptionsStep
    data object PacksForApp : OptionsStep
    data object PacksForAll : OptionsStep
    data class PackIcons(val pack: JarnIconPack) : OptionsStep
}

/** Opens the system image picker. Cancelling changes nothing. */
@Composable
internal fun rememberJarnImagePicker(onPicked: (String) -> Unit): () -> Unit {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onPicked(it.toString()) }
    }
    return {
        try {
            picker.launch(ImageTypes)
        } catch (_: ActivityNotFoundException) {
            JarnNotices.show(R.string.jarnfe_canvas_notice_picker_failed)
        }
    }
}

/**
 * The one app options flow. [onClose] steps back, [onLeave] closes before another screen opens,
 * and [onListChange] runs just before a pin or hide moves the app in the Apps drawer.
 */
@Composable
internal fun JarnAppOptionsFlow(
    data: JarnAppOptionsData,
    target: JarnAppOptionsTarget,
    extras: List<JarnAppOptionsExtra>,
    onClose: () -> Unit,
    onLeave: () -> Unit,
    onListChange: () -> Unit = {},
) {
    val packageName = target.packageName
    var step by remember(packageName) { mutableStateOf<OptionsStep>(OptionsStep.Menu) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val toMenu: () -> Unit = { step = OptionsStep.Menu }
    val toIcon: () -> Unit = { step = OptionsStep.Icon }
    val chooseImage = rememberJarnImagePicker { uri ->
        scope.launch {
            if (data.setAppIcon(packageName, uri)) {
                step = OptionsStep.Menu
            } else {
                JarnNotices.show(R.string.jarnfe_canvas_notice_photo_failed)
            }
        }
    }

    when (val current = step) {
        OptionsStep.Menu -> JarnAppOptionsMenu(
            data = data,
            target = target,
            extras = extras,
            onOpenOther = { display ->
                onLeave()
                activity?.let { data.launchAppOn(it, packageName, display.opposite()) }
            },
            onAppInfo = {
                onLeave()
                activity?.let { data.openAppInfo(it, packageName) }
            },
            onIcon = toIcon,
            onRename = { step = OptionsStep.Rename },
            onListChange = onListChange,
            onClose = onClose,
        )
        OptionsStep.Rename -> CanvasKeyboardSheet(
            label = stringResource(R.string.jarnfe_folder_app_name_label),
            onDone = { text ->
                jarnFolderNameOrNull(text)?.let { data.setAppName(packageName, it) }
                toMenu()
            },
            onCancel = toMenu,
            initialText = data.appOptions(packageName).customName ?: target.label,
        )
        OptionsStep.Icon -> JarnAppIconSheet(
            data = data,
            target = target,
            onChooseImage = chooseImage,
            onFromPack = { step = OptionsStep.PacksForApp },
            onAllAppsPack = { step = OptionsStep.PacksForAll },
            onRemove = {
                scope.launch { data.clearAppIcon(packageName) }
                toMenu()
            },
            onClose = toMenu,
        )
        OptionsStep.PacksForApp -> JarnIconPackListSheet(
            data = data,
            offerNone = false,
            onPick = { pack -> pack?.let { step = OptionsStep.PackIcons(it) } },
            onClose = toIcon,
        )
        OptionsStep.PacksForAll -> JarnIconPackListSheet(
            data = data,
            offerNone = true,
            onPick = { pack ->
                scope.launch { data.setIconPack(pack?.packageName) }
                toIcon()
            },
            onClose = toIcon,
        )
        is OptionsStep.PackIcons -> JarnIconPackIconsSheet(
            data = data,
            pack = current.pack,
            onPick = { iconName ->
                scope.launch {
                    if (data.setAppIconFromPack(packageName, current.pack.packageName, iconName)) {
                        step = OptionsStep.Menu
                    } else {
                        JarnNotices.show(R.string.jarnfe_app_icon_failed)
                    }
                }
            },
            onClose = { step = OptionsStep.PacksForApp },
        )
    }
}
