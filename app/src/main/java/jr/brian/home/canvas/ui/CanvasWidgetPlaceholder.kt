package jr.brian.home.canvas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.ui.theme.ThemePrimaryColor

/** Shown while a widget tile has nothing to draw. [unavailable] means the widget is gone for good. */
@Composable
internal fun CanvasWidgetPlaceholder(widgetId: Int, unavailable: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Widgets,
            contentDescription = stringResource(R.string.canvas_widget_placeholder_description),
            tint = ThemePrimaryColor,
            modifier = Modifier.size(36.dp)
        )
        Spacer(Modifier.height(4.dp))
        if (unavailable) {
            CanvasWidgetPlaceholderText(stringResource(R.string.canvas_widget_unavailable), maxLines = 2)
            CanvasWidgetPlaceholderText(stringResource(R.string.canvas_widget_unavailable_detail), maxLines = 3)
        } else {
            CanvasWidgetPlaceholderText(stringResource(R.string.canvas_widget_id_label, widgetId), maxLines = 1)
        }
    }
}

@Composable
private fun CanvasWidgetPlaceholderText(text: String, maxLines: Int) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.85f),
        fontSize = 10.sp,
        maxLines = maxLines,
        textAlign = TextAlign.Center,
        overflow = TextOverflow.Ellipsis
    )
}
