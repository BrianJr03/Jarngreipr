package jr.brian.home.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import jr.brian.home.R
import jr.brian.home.data.JarnFeOffer
import jr.brian.home.ui.components.dialog.AppBottomSheet
import jr.brian.home.ui.components.settings.rememberTryJarnFe
import jr.brian.home.ui.theme.ThemePrimaryColor

/** Renders nothing unless this build offers Jarn FE. */
@Composable
fun JarnFeWhatsNewPreview(onOpened: () -> Unit) {
    val context = LocalContext.current
    val isOffered = remember(context) { JarnFeOffer.isOffered(context) }
    if (!isOffered) return

    JarnFePreviewCard(onOpened = onOpened)
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun JarnFePreviewSheet(onDismiss: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    AppBottomSheet(onDismissRequest = onDismiss) {
        JarnFePreviewCard(
            onOpened = onDismiss,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            buttonModifier = Modifier.focusRequester(focusRequester),
        )
    }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }
}

@Composable
private fun JarnFePreviewCard(
    onOpened: () -> Unit,
    modifier: Modifier = Modifier,
    buttonModifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.06f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.jarn_preview_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = stringResource(R.string.jarn_preview_description),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
            Text(
                text = stringResource(R.string.jarn_preview_reassurance),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
            )
            TryJarnFeButton(onOpened = onOpened, modifier = buttonModifier)
        }
    }
}

@Composable
private fun TryJarnFeButton(
    onOpened: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tryJarnFe = rememberTryJarnFe(onOpened)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Button(
        onClick = tryJarnFe,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = ThemePrimaryColor,
            contentColor = Color.White,
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isFocused) BorderStroke(2.dp, Color.White) else null,
        interactionSource = interactionSource,
    ) {
        Text(stringResource(R.string.jarn_preview_try))
    }
}
