package jr.brian.home.ui.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import jr.brian.home.R
import jr.brian.home.model.IconPack
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.animations.animatedRotation
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.OledCardLightColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalIconPackManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun IconPackSelectorItem(
    focusRequester: FocusRequester? = null,
    isExpanded: Boolean = false,
    onExpandChanged: (Boolean) -> Unit,
    onIconPackChanged: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val iconPackManager = LocalIconPackManager.current
    var isFocused by remember { mutableStateOf(false) }
    val mainCardFocusRequester = remember { FocusRequester() }

    var iconPacks by remember { mutableStateOf<List<IconPack>>(emptyList()) }
    var selectedPackage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            iconPacks = iconPackManager.getInstalledIconPacks()
            selectedPackage = iconPackManager.selectedIconPack.first()
            isLoading = false
        }
    }

    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            mainCardFocusRequester.requestFocus()
        }
    }

    val selectedPack = iconPacks.find { it.packageName == selectedPackage }

    val cardGradient = Brush.linearGradient(
        colors = if (isFocused) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.8f),
                ThemeSecondaryColor.copy(alpha = 0.8f),
            )
        } else {
            listOf(
                OledCardLightColor,
                OledCardColor,
            )
        }
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = !isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester ?: mainCardFocusRequester)
                    .onFocusChanged {
                        isFocused = it.isFocused
                    }
                    .background(
                        brush = cardGradient,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .border(
                        width = if (isFocused) 2.dp else 0.dp,
                        brush = borderBrush(
                            isFocused = isFocused,
                            colors = listOf(
                                ThemePrimaryColor.copy(alpha = 0.8f),
                                ThemeSecondaryColor.copy(alpha = 0.6f),
                            ),
                        ),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        onExpandChanged(true)
                    }
                    .focusable()
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .rotate(animatedRotation(isFocused)),
                        tint = Color.White,
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.settings_icon_pack_title),
                            color = Color.White,
                            fontSize = if (isFocused) 18.sp else 16.sp,
                            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedPack?.name
                                ?: stringResource(R.string.settings_icon_pack_default),
                            color = if (isFocused) Color.White.copy(alpha = 0.9f) else Color.Gray,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconPackOption(
                    iconPack = null,
                    isSelected = selectedPackage == null,
                    onClick = {
                        scope.launch {
                            iconPackManager.setSelectedIconPack(null)
                            selectedPackage = null
                            onIconPackChanged()
                            onExpandChanged(false)
                        }
                    }
                )

                iconPacks.forEach { pack ->
                    IconPackOption(
                        iconPack = pack,
                        isSelected = selectedPackage == pack.packageName,
                        onClick = {
                            scope.launch {
                                iconPackManager.setSelectedIconPack(pack.packageName)
                                selectedPackage = pack.packageName
                                onIconPackChanged()
                                onExpandChanged(false)
                            }
                        }
                    )
                }

                if (iconPacks.isEmpty() && !isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        OledCardLightColor,
                                        OledCardColor,
                                    )
                                ),
                                shape = RoundedCornerShape(12.dp),
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.settings_icon_pack_none_found),
                            color = Color.Gray,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IconPackOption(
    iconPack: IconPack?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val gradient = Brush.linearGradient(
        colors = if (isFocused) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.8f),
                ThemeSecondaryColor.copy(alpha = 0.6f),
            )
        } else {
            listOf(
                OledCardLightColor,
                OledCardColor,
            )
        }
    )

    val borderColor = when {
        isSelected -> Color.White
        isFocused -> Color.LightGray.copy(alpha = 0.8f)
        else -> Color.Transparent
    }

    val borderWidth = if (isSelected || isFocused) 2.dp else 0.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged {
                isFocused = it.isFocused
            }
            .background(
                brush = gradient,
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .focusable()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            if (iconPack != null && iconPack.icon != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = iconPack.icon),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = iconPack?.name ?: stringResource(R.string.settings_icon_pack_system_default),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}
