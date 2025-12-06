package jr.brian.home.ui.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import jr.brian.home.R
import jr.brian.home.data.IconPackManager
import jr.brian.home.model.IconPack
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import kotlinx.coroutines.launch

@Composable
fun IconPackSelectorItem(
    iconPackManager: IconPackManager,
    focusRequester: FocusRequester = FocusRequester(),
    isExpanded: Boolean = false,
    onExpandChanged: (Boolean) -> Unit = {},
    onIconPackChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var iconPacks by remember { mutableStateOf<List<IconPack>>(emptyList()) }
    var selectedIconPack by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        iconPacks = iconPackManager.getInstalledIconPacks()
        selectedIconPack = iconPackManager.getSelectedIconPack()
        isLoading = false
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SettingItem(
            title = stringResource(R.string.settings_icon_pack_title),
            description = when {
                isLoading -> stringResource(R.string.settings_icon_pack_loading)
                iconPacks.isEmpty() -> stringResource(R.string.settings_icon_pack_none_installed)
                selectedIconPack == null -> stringResource(R.string.settings_icon_pack_default)
                else -> iconPacks.find { it.packageName == selectedIconPack }?.name
                    ?: stringResource(R.string.settings_icon_pack_default)
            },
            icon = Icons.Default.Palette,
            onClick = {
                if (iconPacks.isNotEmpty() && !isLoading) {
                    onExpandChanged(!isExpanded)
                }
            },
            focusRequester = focusRequester
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Default option
                    IconPackOption(
                        name = stringResource(R.string.settings_icon_pack_default),
                        packageName = null,
                        icon = null,
                        isSelected = selectedIconPack == null,
                        onSelect = {
                            scope.launch {
                                iconPackManager.setSelectedIconPack(null)
                                selectedIconPack = null
                                onIconPackChanged()
                            }
                        }
                    )

                    // Installed icon packs
                    iconPacks.forEach { iconPack ->
                        IconPackOption(
                            name = iconPack.name,
                            packageName = iconPack.packageName,
                            icon = iconPack.icon,
                            isSelected = selectedIconPack == iconPack.packageName,
                            onSelect = {
                                scope.launch {
                                    isLoading = true
                                    iconPackManager.setSelectedIconPack(iconPack.packageName)
                                    selectedIconPack = iconPack.packageName
                                    onIconPackChanged()
                                    isLoading = false
                                }
                            }
                        )
                    }

                    if (isLoading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = ThemePrimaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconPackOption(
    name: String,
    packageName: String?,
    icon: android.graphics.drawable.Drawable?,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Image(
                painter = rememberAsyncImagePainter(model = icon),
                contentDescription = name,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(12.dp))
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )

            if (packageName != null) {
                Text(
                    text = packageName,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }

        RadioButton(
            selected = isSelected,
            onClick = { onSelect() },
            colors = RadioButtonDefaults.colors(
                selectedColor = ThemePrimaryColor,
                unselectedColor = ThemeAccentColor
            )
        )
    }
}
