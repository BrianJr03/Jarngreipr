package jr.brian.home.ui.components.folder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import jr.brian.home.R
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun FolderOptionsDialog(
    onDismiss: () -> Unit,
    onEditName: () -> Unit,
    onDeleteFolder: () -> Unit,
    onResizeIcon: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1C1C1E)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.folder_edit_name),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                FolderOptionButton(
                    text = stringResource(R.string.folder_edit_name),
                    onClick = {
                        onEditName()
                        onDismiss()
                    }
                )

                FolderOptionButton(
                    text = stringResource(R.string.app_options_resize),
                    onClick = {
                        onResizeIcon()
                        onDismiss()
                    }
                )

                FolderOptionButton(
                    text = stringResource(R.string.folder_delete),
                    onClick = {
                        onDeleteFolder()
                        onDismiss()
                    },
                    isDestructive = true
                )

                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.dialog_cancel),
                        color = ThemePrimaryColor,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderOptionButton(
    text: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            color = if (isDestructive) ThemeSecondaryColor else Color.White,
            fontSize = 16.sp
        )
    }
}
