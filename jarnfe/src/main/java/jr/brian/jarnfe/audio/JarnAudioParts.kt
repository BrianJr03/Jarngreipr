package jr.brian.jarnfe.audio

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnSheet
import jr.brian.core.ui.components.JarnSliderRow
import jr.brian.core.ui.focus.JarnRevealScrollTop
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnBindingsBuilder
import jr.brian.jarnfe.keyboard.JarnTextKeyboard
import jr.brian.jarnfe.shell.JarnNotices
import jr.brian.jarnfe.tools.ToolsInitialFocus
import jr.brian.jarnfe.utilities.UtilityLayer
import jr.brian.jarnfe.utilities.UtilityTitle

@Composable
internal fun AudioPage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bindings: JarnBindingsBuilder.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val first = remember { FocusRequester() }
    val scroll = rememberScrollState()
    ToolsInitialFocus(first)
    UtilityLayer(onBack = onBack, bindings = bindings) {
        JarnRevealScrollTop(scroll) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .focusRequester(first)
                    .focusGroup()
                    .verticalScroll(scroll)
                    .padding(JarnTheme.space.lg),
                verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
            ) {
                UtilityTitle(title)
                content()
            }
        }
    }
}

@Composable
internal fun AudioVolumeRow(label: String, value: Float, enabled: Boolean, onChange: (Float) -> Unit) {
    JarnSliderRow(
        label = label,
        value = value,
        valueText = stringResource(R.string.jarnfe_audio_percent, (value * 100).toInt()),
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        focusableWhenDisabled = true,
    )
}

@Composable
internal fun AudioKeyboardSheet(
    label: String,
    onDone: (String) -> Unit,
    onClose: () -> Unit,
    initialText: String = "",
    maxLength: Int = AUDIO_TEXT_MAX,
) {
    JarnSheet(visible = true, onDismiss = onClose) {
        JarnTextKeyboard(
            label = label,
            masked = false,
            onDone = onDone,
            onCancel = onClose,
            modifier = Modifier.fillMaxWidth(),
            initialText = initialText,
            maxLength = maxLength,
        )
    }
}

/** Opens the system folder picker, and says so when the device has none. */
@Composable
internal fun rememberAudioFolderPicker(onPicked: (Uri) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(onPicked)
    }
    return {
        try {
            launcher.launch(null)
        } catch (_: ActivityNotFoundException) {
            JarnNotices.show(R.string.jarnfe_audio_picker_unavailable)
        }
    }
}

@Composable
internal fun rememberAudioFilePicker(onPicked: (Uri) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onPicked)
    }
    return {
        try {
            launcher.launch(arrayOf("audio/*"))
        } catch (_: ActivityNotFoundException) {
            JarnNotices.show(R.string.jarnfe_audio_picker_unavailable)
        }
    }
}

internal const val AUDIO_TEXT_MAX = 140
