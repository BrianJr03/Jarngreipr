package jr.brian.home.ui.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.model.BackButtonShortcut
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.screens.ShortcutOptionItem
import jr.brian.home.ui.screens.backButtonShortcutOptions
import jr.brian.home.ui.screens.toShortcut

data class BackButtonShortcutChoice(
    val shortcut: BackButtonShortcut,
    val appPackage: String?
)

@Composable
fun BackButtonShortcutPickerSheet(
    choices: List<BackButtonShortcutChoice>,
    allApps: List<AppInfo>,
    onChoiceSelected: (BackButtonShortcutChoice) -> Unit,
    onDismiss: () -> Unit
) {
    val labels = backButtonShortcutOptions().associate { it.shortcut to it.label }
    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { firstItemFocusRequester.requestFocus() }
    }

    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BackButtonShortcutPickerTitle()
            choices.forEachIndexed { index, choice ->
                val appLabel = allApps.find { it.packageName == choice.appPackage }?.label
                ShortcutOptionItem(
                    label = appLabel ?: labels[choice.shortcut.toShortcut()].orEmpty(),
                    isSelected = false,
                    onClick = { onChoiceSelected(choice) },
                    modifier = if (index == 0) {
                        Modifier.focusRequester(firstItemFocusRequester)
                    } else {
                        Modifier
                    }
                )
            }
        }
    }
}

@Composable
private fun BackButtonShortcutPickerTitle() {
    Text(
        text = stringResource(R.string.settings_back_button_shortcut_choose),
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    )
}
