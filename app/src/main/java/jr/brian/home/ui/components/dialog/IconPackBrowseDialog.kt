package jr.brian.home.ui.components.dialog

import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import jr.brian.home.R
import jr.brian.home.model.IconPack
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.components.InfoBox
import jr.brian.home.ui.components.OnScreenKeyboard
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.OledCardLightColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalCustomIconManager
import jr.brian.home.ui.theme.managers.LocalIconPackManager
import jr.brian.home.util.OverlayInfoUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IconPackBrowseDialog(
    packageName: String,
    onDismiss: () -> Unit,
    onIconChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val iconPackManager = LocalIconPackManager.current
    val customIconManager = LocalCustomIconManager.current

    var selectedIconPack by remember { mutableStateOf<IconPack?>(null) }
    var iconPackDrawables by remember { mutableStateOf<Map<String, Drawable>>(emptyMap()) }
    var isLoadingDrawables by remember { mutableStateOf(false) }
    var iconPacks by remember { mutableStateOf<List<IconPack>>(emptyList()) }
    var isLoadingPacks by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoadingPacks = true
        val packs = iconPackManager.getInstalledIconPacks()
        iconPacks = packs
        isLoadingPacks = false
    }

    LaunchedEffect(selectedIconPack) {
        selectedIconPack?.let { pack ->
            isLoadingDrawables = true
            iconPackDrawables = iconPackManager.getAllDrawablesFromIconPack(pack.packageName)
            isLoadingDrawables = false
        }
    }

    if (selectedIconPack == null) {
        IconPackSelectionDialog(
            iconPacks = iconPacks,
            isLoading = isLoadingPacks,
            onIconPackSelected = { selectedIconPack = it },
            onDismiss = onDismiss
        )
    } else {
        DrawableSelectionDialog(
            iconPack = selectedIconPack!!,
            iconPackDrawables = iconPackDrawables,
            isLoadingDrawables = isLoadingDrawables,
            onDrawableSelected = { drawable ->
                scope.launch {
                    val result = customIconManager.setCustomIcon(packageName, drawable)
                    if (result.isSuccess) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.app_options_custom_icon_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        onIconChanged()
                        onDismiss()
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.app_options_custom_icon_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onBack = { selectedIconPack = null },
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun IconPackSelectionDialog(
    iconPacks: List<IconPack>,
    isLoading: Boolean,
    onIconPackSelected: (IconPack) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
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
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.dialog_cancel),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    Text(
                        text = stringResource(R.string.app_options_icon_pack_browse_title),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.app_options_icon_pack_loading_packs),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                } else if (iconPacks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.settings_icon_pack_none_found),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(iconPacks) { pack ->
                            IconPackSelectionItem(
                                iconPack = pack,
                                onClick = { onIconPackSelected(pack) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawableSelectionDialog(
    iconPack: IconPack,
    iconPackDrawables: Map<String, Drawable>,
    isLoadingDrawables: Boolean,
    onDrawableSelected: (Drawable) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    var showKeyboard by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val keyboardFocusRequesters = remember { SnapshotStateMap<Int, FocusRequester>() }
    var focusedKeyIndex by remember { mutableIntStateOf(0) }

    val filteredDrawables = remember(iconPackDrawables, searchQuery) {
        if (searchQuery.isBlank()) {
            iconPackDrawables
        } else {
            iconPackDrawables.filter { (name, _) ->
                name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)
                .background(
                    color = OledCardColor,
                    shape = RoundedCornerShape(16.dp)
                ).padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = iconPack.name,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isLoadingDrawables) {
                            Text(
                                text = "${filteredDrawables.size} icons",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    IconButton(
                        onClick = { showKeyboard = !showKeyboard },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (showKeyboard) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showKeyboard) "Hide search" else "Show search",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoadingDrawables) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val randomMessage = remember { OverlayInfoUtil.getRandomFact() }
                            Text(
                                text = stringResource(R.string.app_options_icon_pack_loading_icons),
                                color = Color.White.copy(alpha = 0.7f),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            InfoBox(
                                label = stringResource(R.string.welcome_overlay_thor_fact_label),
                                content = stringResource(randomMessage),
                                isPrimary = true,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                    }
                } else if (iconPackDrawables.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.app_options_icon_pack_no_icons),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(if (showKeyboard) 0.6f else 1f)
                                .fillMaxHeight()
                        ) {
                            LazyVerticalGrid(
                                columns = if (showKeyboard) GridCells.Fixed(3) else GridCells.Fixed(6),
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredDrawables.entries.toList()) { (name, drawable) ->
                                DrawableGridItem(
                                    name = name,
                                    drawable = drawable,
                                    onClick = { onDrawableSelected(drawable) }
                                )
                                }
                            }
                        }

                        if (showKeyboard) {
                            Box(
                                modifier = Modifier
                                    .weight(0.4f)
                                    .fillMaxHeight()
                            ) {
                                OnScreenKeyboard(
                                    searchQuery = searchQuery,
                                    onQueryChange = { searchQuery = it },
                                    showQueryText = true,
                                    keyboardFocusRequesters = keyboardFocusRequesters,
                                    onFocusChanged = { focusedKeyIndex = it },
                                    onNavigateRight = {},
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconPackSelectionItem(
    iconPack: IconPack,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
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
                ),
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                brush = borderBrush(
                    isFocused = isFocused,
                    colors = listOf(
                        ThemePrimaryColor.copy(alpha = 0.8f),
                        ThemeSecondaryColor.copy(alpha = 0.6f),
                    )
                ),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable { onClick() }
            .focusable()
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconPack.icon != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = iconPack.icon),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = iconPack.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun DrawableGridItem(
    name: String,
    drawable: Drawable,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .size(72.dp)
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
                        ThemeSecondaryColor.copy(alpha = 0.6f),
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .focusable()
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = drawable),
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = name,
            color = if (isFocused) Color.White else Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}