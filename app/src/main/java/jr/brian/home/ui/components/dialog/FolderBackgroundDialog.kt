package jr.brian.home.ui.components.dialog

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalFolderManager
import kotlinx.coroutines.launch

private val FolderBackgroundPresetColors: List<Color> = listOf(
    Color(0xFF000000),
    Color(0xFF1E1E2E),
    Color(0xFF0F3460),
    Color(0xFF1A3A6B),
    Color(0xFF1F4D2B),
    Color(0xFF3A1F4D),
    Color(0xFF4D1F1F),
    Color(0xFF4D3A1F),
    Color(0xFFE94560),
    Color(0xFFC97B12),
    Color(0xFF2E7D32),
    Color(0xFF6A1B9A)
)

@Composable
fun FolderBackgroundDialog(
    folderId: String,
    folderName: String,
    pageIndex: Int,
    tabType: String,
    currentColorArgb: Int?,
    currentImagePath: String?,
    onBackgroundChanged: (Int?, String?) -> Unit = { _, _ -> },
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val folderManager = LocalFolderManager.current
    val scope = rememberCoroutineScope()

    val hasBackground = currentColorArgb != null || currentImagePath != null

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val result = folderManager.saveFolderBackgroundImage(folderId, it)
                if (result.isSuccess) {
                    val savedPath = result.getOrNull()
                    folderManager.updateFolderBackground(
                        pageIndex = pageIndex,
                        folderId = folderId,
                        backgroundColorArgb = null,
                        backgroundImagePath = savedPath,
                        tabType = tabType
                    )
                    onBackgroundChanged(null, savedPath)
                    onDismiss()
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.folder_background_image_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    DimmedBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
                FolderBackgroundDialogHeader(
                    folderName = folderName,
                    onDismiss = onDismiss
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.folder_background_pick_color),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                ColorSwatchGrid(
                    selectedArgb = currentColorArgb,
                    onColorSelected = { color ->
                        scope.launch {
                            val argb = color.toArgb()
                            folderManager.updateFolderBackground(
                                pageIndex = pageIndex,
                                folderId = folderId,
                                backgroundColorArgb = argb,
                                backgroundImagePath = null,
                                tabType = tabType
                            )
                            onBackgroundChanged(argb, null)
                            onDismiss()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                FolderBackgroundActionButton(
                    text = stringResource(R.string.folder_background_choose_png),
                    icon = Icons.Default.Image,
                    onClick = { imagePickerLauncher.launch("image/png") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                FolderBackgroundActionButton(
                    text = stringResource(R.string.folder_background_choose_gif),
                    icon = Icons.Default.Gif,
                    onClick = { imagePickerLauncher.launch("image/gif") }
                )

                if (hasBackground) {
                    Spacer(modifier = Modifier.height(12.dp))

                    FolderBackgroundActionButton(
                        text = stringResource(R.string.folder_background_remove),
                        icon = Icons.Default.Delete,
                        onClick = {
                            scope.launch {
                                folderManager.updateFolderBackground(
                                    pageIndex = pageIndex,
                                    folderId = folderId,
                                    backgroundColorArgb = null,
                                    backgroundImagePath = null,
                                    tabType = tabType
                                )
                                onBackgroundChanged(null, null)
                                onDismiss()
                            }
                        }
                    )
                }
        }
    }
}

@Composable
private fun FolderBackgroundDialogHeader(
    folderName: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.folder_background_title),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = folderName,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.dialog_cancel),
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ColorSwatchGrid(
    selectedArgb: Int?,
    onColorSelected: (Color) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
    ) {
        items(FolderBackgroundPresetColors) { color ->
            ColorSwatch(
                color = color,
                isSelected = selectedArgb != null && selectedArgb == color.toArgb(),
                onClick = { onColorSelected(color) }
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val highlight = isSelected || isFocused

    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(color = color, shape = CircleShape)
            .border(
                width = if (highlight) 3.dp else 1.dp,
                color = if (highlight) ThemePrimaryColor else Color.White.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable { onClick() }
            .focusable()
    )
}

@Composable
private fun FolderBackgroundActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                color = if (isFocused) {
                    ThemePrimaryColor.copy(alpha = 0.3f)
                } else {
                    OledCardColor.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                brush = borderBrush(
                    isFocused = isFocused,
                    colors = listOf(
                        ThemePrimaryColor.copy(alpha = 0.8f),
                        ThemeSecondaryColor.copy(alpha = 0.6f)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .focusable()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.size(12.dp))

            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

