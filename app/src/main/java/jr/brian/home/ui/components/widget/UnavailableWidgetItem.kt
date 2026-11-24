package jr.brian.home.ui.components.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.ui.components.dialog.ConfirmationDialog
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.util.rememberDialogState

private val WidgetCellHeight = 80.dp
private val PlaceholderShape = RoundedCornerShape(12.dp)

@Composable
fun UnavailableWidgetItem(
    heightCells: Int,
    canRemove: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val removeDialogState = rememberDialogState<Unit>()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height((WidgetCellHeight * heightCells).coerceAtLeast(WidgetCellHeight))
            .clip(PlaceholderShape)
            .background(OledCardColor.copy(alpha = 0.85f))
            .removableWhen(canRemove, ThemePrimaryColor) { removeDialogState.show() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        UnavailableWidgetLabel(canRemove)
    }

    if (removeDialogState.isVisible && canRemove) {
        RemoveUnavailableWidgetDialog(
            onConfirm = {
                onRemove()
                removeDialogState.dismiss()
            },
            onDismiss = removeDialogState::dismiss
        )
    }
}

private fun Modifier.removableWhen(
    canRemove: Boolean,
    borderColor: Color,
    onClick: () -> Unit
): Modifier =
    if (canRemove) {
        border(2.dp, borderColor, PlaceholderShape).clickable(onClick = onClick)
    } else {
        this
    }

@Composable
private fun UnavailableWidgetLabel(canRemove: Boolean) {
    Icon(
        imageVector = Icons.Default.Widgets,
        contentDescription = null,
        tint = ThemePrimaryColor,
        modifier = Modifier.size(24.dp)
    )
    Text(
        text = stringResource(R.string.widget_unavailable_title),
        color = Color.White,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    Text(
        text = stringResource(
            if (canRemove) R.string.widget_unavailable_remove_hint
            else R.string.widget_unavailable_message
        ),
        color = Color.White.copy(alpha = 0.7f),
        fontSize = 10.sp,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun RemoveUnavailableWidgetDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmationDialog(
        title = stringResource(R.string.widget_delete_title),
        message = stringResource(R.string.widget_unavailable_remove_message),
        confirmText = stringResource(R.string.canvas_remove_confirm),
        cancelText = stringResource(R.string.canvas_remove_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
