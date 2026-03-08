package jr.brian.home.ui.screens.themeshare

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.ui.components.QwertyKeyboard
import jr.brian.home.ui.components.settings.CollapsibleSettingsSection
import jr.brian.home.ui.components.settings.PingAutoStartToggleItem
import jr.brian.home.ui.components.settings.WallpaperNearbyAutoStartToggleItem
import jr.brian.home.ui.theme.managers.LocalThemeManager

@Composable
fun ThemeShareSettingsSection(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeManager = LocalThemeManager.current
    var showNameKeyboard by remember { mutableStateOf(false) }
    val keyboardFocusRequesters = remember { SnapshotStateMap<Int, FocusRequester>() }
    var focusedKeyIndex by remember { mutableIntStateOf(0) }

    CollapsibleSettingsSection(
        title = "Settings",
        icon = Icons.Default.Settings,
        isExpanded = isExpanded,
        onToggle = onToggle,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { showNameKeyboard = !showNameKeyboard }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Device name",
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = themeManager.pingDisplayName.ifBlank { Build.MODEL },
                        color = if (themeManager.pingDisplayName.isBlank()) Color.White.copy(alpha = 0.4f)
                        else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.theme_sharing_edit_name_description),
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(15.dp)
                )
            }

            AnimatedVisibility(
                visible = themeManager.pingDisplayName.isBlank(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Text(
                    text = stringResource(R.string.theme_sharing_name_default_hint, Build.MODEL),
                    color = Color.White.copy(alpha = 0.25f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 14.dp, top = 4.dp)
                )
            }

            AnimatedVisibility(
                visible = showNameKeyboard,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    QwertyKeyboard(
                        searchQuery = themeManager.pingDisplayName,
                        showQueryText = false,
                        showFlipLayoutButton = false,
                        onQueryChange = { themeManager.updatePingDisplayName(it) },
                        keyboardFocusRequesters = keyboardFocusRequesters,
                        onFocusChanged = { focusedKeyIndex = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        PingAutoStartToggleItem()
        WallpaperNearbyAutoStartToggleItem()
    }
}
