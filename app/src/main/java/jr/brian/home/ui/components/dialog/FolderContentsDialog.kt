package jr.brian.home.ui.components.dialog

import android.content.Context
import android.hardware.display.DisplayManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import jr.brian.home.R
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.components.apps.AppIconImage
import jr.brian.home.ui.components.settings.AppName
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.OledCardLightColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalCustomIconManager
import jr.brian.home.util.launchApp

@Composable
fun FolderContentsDialog(
    folderName: String,
    apps: List<AppInfo>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val customIconManager = LocalCustomIconManager.current
    val appDisplayPreferenceManager = LocalAppDisplayPreferenceManager.current
    val appVisibilityManager = LocalAppVisibilityManager.current

    val hasExternalDisplay = remember {
        val displayManager =
            context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.displays.size > 1
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(
                    color = OledCardColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 2.dp,
                    color = ThemePrimaryColor.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = folderName,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.dialog_app_visibility_close),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${apps.size} apps",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (apps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.folder_empty),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(apps, key = { it.packageName }) { app ->
                            FolderAppItem(
                                app = app,
                                customIconManager = customIconManager,
                                showAppNames = appVisibilityManager.showAppNames,
                                onClick = {
                                    val displayPreference = if (hasExternalDisplay) {
                                        appDisplayPreferenceManager.getAppDisplayPreference(app.packageName)
                                    } else {
                                        DisplayPreference.CURRENT_DISPLAY
                                    }
                                    launchApp(
                                        context = context,
                                        packageName = app.packageName,
                                        displayPreference = displayPreference
                                    )
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderAppItem(
    app: AppInfo,
    customIconManager: jr.brian.home.data.CustomIconManager,
    showAppNames: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val cardGradient = Brush.linearGradient(
        colors = if (isFocused) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.6f),
                ThemeSecondaryColor.copy(alpha = 0.6f),
            )
        } else {
            listOf(
                OledCardLightColor.copy(alpha = 0.3f),
                OledCardColor.copy(alpha = 0.3f),
            )
        }
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                brush = borderBrush(
                    isFocused = isFocused,
                    colors = listOf(
                        ThemePrimaryColor.copy(alpha = 0.8f),
                        ThemeSecondaryColor.copy(alpha = 0.6f),
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .focusable()
            .padding(8.dp)
    ) {
        AppIconImage(
            defaultIcon = app.icon,
            packageName = app.packageName,
            contentDescription = stringResource(R.string.app_icon_description, app.label),
            customIconManager = customIconManager,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        if (showAppNames) {
            Spacer(modifier = Modifier.height(4.dp))
            app.AppName()
        }
    }
}
