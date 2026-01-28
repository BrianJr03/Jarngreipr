package jr.brian.home.ui.components.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import jr.brian.home.R
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.theme.ThemePrimaryColor

@Composable
fun AppResizePreview(
    app: AppInfo?,
    previewIconSize: Float,
    onPreviewSizeChange: (Float) -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.app_options_resize_preview),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )

        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            app?.let {
                Image(
                    painter = rememberAsyncImagePainter(model = it.icon),
                    contentDescription = null,
                    modifier = Modifier.size(previewIconSize.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (previewIconSize > 32f) {
                        onPreviewSizeChange(previewIconSize - 8f)
                    }
                },
                enabled = previewIconSize > 32f
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease size",
                    tint = if (previewIconSize > 32f) ThemePrimaryColor else Color.Gray
                )
            }

            Text(
                text = "${previewIconSize.toInt()} dp",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )

            IconButton(
                onClick = {
                    if (previewIconSize < 128f) {
                        onPreviewSizeChange(previewIconSize + 8f)
                    }
                },
                enabled = previewIconSize < 128f
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase size",
                    tint = if (previewIconSize < 128f) ThemePrimaryColor else Color.Gray
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Gray.copy(alpha = 0.3f)
                )
            ) {
                Text(stringResource(R.string.app_options_resize_cancel))
            }

            Button(
                onClick = onApply,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemePrimaryColor
                )
            ) {
                Text(stringResource(R.string.app_options_resize_apply))
            }
        }
    }
}
