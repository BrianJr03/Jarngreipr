package jr.brian.home.ui.components.dialog

import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import jr.brian.home.R
import jr.brian.home.model.IconPack
import jr.brian.home.ui.components.InfoBox
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor
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

    val currentIconPack = selectedIconPack
    if (currentIconPack == null) {
        IconPackSelectionDialog(
            iconPacks = iconPacks,
            isLoading = isLoadingPacks,
            onIconPackSelected = { selectedIconPack = it },
            onDismiss = onDismiss
        )
    } else {
        DrawableSelectionDialog(
            iconPack = currentIconPack,
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
                            IconPackListItem(
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
                IconPackMetadataDisplay(
                    iconPack = iconPack,
                    filteredIconCount = filteredDrawables.size,
                    isLoadingDrawables = isLoadingDrawables,
                    showKeyboard = showKeyboard,
                    onBack = onBack,
                    onToggleKeyboard = { showKeyboard = !showKeyboard }
                )

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
                        IconPackGrid(
                            filteredDrawables = filteredDrawables,
                            showKeyboard = showKeyboard,
                            onDrawableSelected = onDrawableSelected,
                            modifier = Modifier
                                .weight(if (showKeyboard) 0.6f else 1f)
                                .fillMaxHeight()
                        )

                        if (showKeyboard) {
                            IconPackSearchBar(
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                keyboardFocusRequesters = keyboardFocusRequesters,
                                onFocusChanged = { focusedKeyIndex = it },
                                modifier = Modifier
                                    .weight(0.4f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}

