package jr.brian.home.ui.components.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.app.Folder
import jr.brian.home.ui.components.apps.AppIconImage
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalCustomIconManager


@Composable
fun FolderGridItem(
    folder: Folder,
    apps: List<AppInfo>,
    onClick: () -> Unit
) {
    val customIconManager = LocalCustomIconManager.current
    val appVisibilityManager = LocalAppVisibilityManager.current
    val previewApps = apps.take(4)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(OledCardColor.copy(alpha = 0.9f))
                .border(
                    width = 2.dp,
                    color = ThemePrimaryColor.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            when (previewApps.size) {
                0 -> {
                    Text(
                        text = "Empty",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center
                    )
                }

                1 -> {
                    AppIconImage(
                        defaultIcon = previewApps[0].icon,
                        packageName = previewApps[0].packageName,
                        contentDescription = previewApps[0].label,
                        customIconManager = customIconManager,
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(6.dp)
                    )
                }

                2 -> {
                    Row {
                        previewApps.forEach { app ->
                            AppIconImage(
                                defaultIcon = app.icon,
                                packageName = app.packageName,
                                contentDescription = app.label,
                                customIconManager = customIconManager,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(1.dp),
                                shape = RoundedCornerShape(4.dp)
                            )
                        }
                    }
                }

                3 -> {
                    Column {
                        AppIconImage(
                            defaultIcon = previewApps[0].icon,
                            packageName = previewApps[0].packageName,
                            contentDescription = previewApps[0].label,
                            customIconManager = customIconManager,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(1.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                        Row {
                            previewApps.drop(1).forEach { app ->
                                AppIconImage(
                                    defaultIcon = app.icon,
                                    packageName = app.packageName,
                                    contentDescription = app.label,
                                    customIconManager = customIconManager,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(1.dp),
                                    shape = RoundedCornerShape(4.dp)
                                )
                            }
                        }
                    }
                }

                else -> {
                    Column {
                        Row {
                            previewApps.take(2).forEach { app ->
                                AppIconImage(
                                    defaultIcon = app.icon,
                                    packageName = app.packageName,
                                    contentDescription = app.label,
                                    customIconManager = customIconManager,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(1.dp),
                                    shape = RoundedCornerShape(4.dp)
                                )
                            }
                        }
                        Row {
                            previewApps.drop(2).take(2).forEach { app ->
                                AppIconImage(
                                    defaultIcon = app.icon,
                                    packageName = app.packageName,
                                    contentDescription = app.label,
                                    customIconManager = customIconManager,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(1.dp),
                                    shape = RoundedCornerShape(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        if (appVisibilityManager.showFolderNames) {
            Text(
                text = folder.name,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
