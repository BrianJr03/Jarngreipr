package jr.brian.home.ui.components.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import jr.brian.home.R
import jr.brian.home.canvas.model.EsdeContentScale
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.model.rom.PinnedRomInfo
import jr.brian.home.model.rom.resolveDisplayPath
import jr.brian.home.ui.components.apps.ROM_DEFAULT_ICON_SIZE
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import java.io.File

@Composable
fun PinnedRomOptionsDialog(
    rom: PinnedRomInfo,
    onDismiss: () -> Unit,
    onMediaTypeSelected: (String?) -> Unit,
    onRemove: () -> Unit,
    currentIconSize: Float? = null,
    onIconSizeChange: ((Float) -> Unit)? = null,
    hasExternalDisplay: Boolean = false,
    currentDisplayPreference: DisplayPreference = DisplayPreference.CURRENT_DISPLAY,
    onDisplayPreferenceChange: (DisplayPreference) -> Unit = {},
    currentContentScale: EsdeContentScale? = null,
    onContentScaleChange: ((EsdeContentScale) -> Unit)? = null,
    onEditCanvas: (() -> Unit)? = null,
    continuousSpinEligible: Boolean = false,
    continuousSpinEnabled: Boolean = false,
    onContinuousSpinChange: ((Boolean) -> Unit)? = null
) {
    var showResizeMode by remember { mutableStateOf(false) }
    var previewIconSize by remember(currentIconSize) {
        mutableFloatStateOf(currentIconSize ?: ROM_DEFAULT_ICON_SIZE)
    }

    DimmedDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(vertical = 16.dp)
                .background(OledCardColor, RoundedCornerShape(24.dp))
                .border(2.dp, ThemePrimaryColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                RomOptionsHeader(rom = rom, onDismiss = onDismiss)
                Spacer(Modifier.height(16.dp))

                AnimatedVisibility(visible = !showResizeMode, enter = fadeIn(), exit = fadeOut()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (onEditCanvas != null) {
                            RomEditCanvasRow(onClick = {
                                onDismiss()
                                onEditCanvas()
                            })
                            Spacer(Modifier.height(4.dp))
                        }
                        HorizontalDivider(color = ThemePrimaryColor.copy(alpha = 0.3f))
                        Spacer(Modifier.height(4.dp))
                        RomMediaTypeList(rom = rom, onMediaTypeSelected = { type ->
                            onMediaTypeSelected(type)
                            onDismiss()
                        })
                        if (onIconSizeChange != null) {
                            Spacer(Modifier.height(4.dp))
                            HorizontalDivider(color = ThemePrimaryColor.copy(alpha = 0.3f))
                            Spacer(Modifier.height(4.dp))
                            RomResizeRow(onClick = { showResizeMode = true })
                        }
//                        if (hasExternalDisplay) {
//                            Spacer(Modifier.height(4.dp))
//                            HorizontalDivider(color = ThemePrimaryColor.copy(alpha = 0.3f))
//                            Spacer(Modifier.height(8.dp))
//                            RomDisplayPreferenceSection(
//                                currentDisplayPreference = currentDisplayPreference,
//                                onDisplayPreferenceChange = { pref ->
//                                    onDisplayPreferenceChange(pref)
//                                    onDismiss()
//                                }
//                            )
//                        }
                        if (currentContentScale != null && onContentScaleChange != null) {
                            Spacer(Modifier.height(4.dp))
                            HorizontalDivider(color = ThemePrimaryColor.copy(alpha = 0.3f))
                            Spacer(Modifier.height(8.dp))
                            RomContentScaleSection(
                                current = currentContentScale,
                                onSelected = { scale ->
                                    onContentScaleChange(scale)
                                    onDismiss()
                                }
                            )
                        }
                        if (continuousSpinEligible && onContinuousSpinChange != null) {
                            Spacer(Modifier.height(4.dp))
                            HorizontalDivider(color = ThemePrimaryColor.copy(alpha = 0.3f))
                            Spacer(Modifier.height(4.dp))
                            RomContinuousSpinRow(
                                enabled = continuousSpinEnabled,
                                onToggle = onContinuousSpinChange
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider(color = ThemePrimaryColor.copy(alpha = 0.3f))
                        Spacer(Modifier.height(4.dp))
                        RomRemoveRow(onClick = {
                            onRemove()
                            onDismiss()
                        })
                    }
                }

                AnimatedVisibility(visible = showResizeMode, enter = fadeIn(), exit = fadeOut()) {
                    RomResizePreview(
                        rom = rom,
                        previewIconSize = previewIconSize,
                        onPreviewSizeChange = { previewIconSize = it },
                        onCancel = {
                            previewIconSize = currentIconSize ?: ROM_DEFAULT_ICON_SIZE
                            showResizeMode = false
                        },
                        onApply = {
                            onIconSizeChange?.invoke(previewIconSize)
                            showResizeMode = false
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RomOptionsHeader(rom: PinnedRomInfo, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = rom.name,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.rom_options_choose_media),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
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
private fun RomMediaTypeList(rom: PinnedRomInfo, onMediaTypeSelected: (String?) -> Unit) {
    val autoLabel = stringResource(R.string.rom_media_auto)
    val coversLabel = stringResource(R.string.rom_media_covers)
    val marqueeLabel = stringResource(R.string.rom_media_marquee)
    val screenshotsLabel = stringResource(R.string.rom_media_screenshots)
    val fanartLabel = stringResource(R.string.rom_media_fanart)
    val titleScreensLabel = stringResource(R.string.rom_media_title_screens)
    val mixImagesLabel = stringResource(R.string.rom_media_mix_images)
    val physicalMediaLabel = stringResource(R.string.rom_media_physical_media)

    val options = buildList {
        add(null to autoLabel)
        if (rom.artworkPath != null) add("Covers" to coversLabel)
        if (rom.marqueeImagePath != null) add("Marquee" to marqueeLabel)
        if (rom.screenshotPath != null) add("Screenshots" to screenshotsLabel)
        if (rom.fanartPath != null) add("Fanart" to fanartLabel)
        if (rom.titlescreenPath != null) add("TitleScreens" to titleScreensLabel)
        if (rom.miximagePath != null) add("MixImages" to mixImagesLabel)
        if (rom.physicalMediaPath != null) add("PhysicalMedia" to physicalMediaLabel)
    }

    options.forEach { (key, label) ->
        RomMediaTypeRow(
            label = label,
            isSelected = rom.displayMediaType == key,
            onClick = { onMediaTypeSelected(key) }
        )
    }
}

@Composable
private fun RomMediaTypeRow(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) ThemePrimaryColor.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = if (isSelected) ThemePrimaryColor else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                color = if (isSelected) ThemePrimaryColor else Color.White,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = ThemePrimaryColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun RomResizeRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.OpenInFull,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = stringResource(R.string.app_options_resize),
            color = Color.White,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun RomDisplayPreferenceSection(
    currentDisplayPreference: DisplayPreference,
    onDisplayPreferenceChange: (DisplayPreference) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DisplayPreferenceTile(
            label = stringResource(R.string.app_options_launch_primary_descr),
            isSelected = currentDisplayPreference == DisplayPreference.PRIMARY_DISPLAY,
            onClick = { onDisplayPreferenceChange(DisplayPreference.PRIMARY_DISPLAY) },
            modifier = Modifier.weight(1f)
        )
        DisplayPreferenceTile(
            label = stringResource(R.string.app_options_launch_external_descr),
            isSelected = currentDisplayPreference == DisplayPreference.CURRENT_DISPLAY,
            onClick = { onDisplayPreferenceChange(DisplayPreference.CURRENT_DISPLAY) },
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun DisplayPreferenceTile(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(
                color = if (isSelected) ThemePrimaryColor.copy(alpha = 0.2f)
                else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) ThemePrimaryColor else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) ThemePrimaryColor else Color.White,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun RomEditCanvasRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = null,
            tint = ThemePrimaryColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = stringResource(R.string.canvas_edit_canvas_option),
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RomContentScaleSection(
    current: EsdeContentScale,
    onSelected: (EsdeContentScale) -> Unit
) {
    Text(
        text = stringResource(R.string.canvas_esde_picker_scale_label),
        color = Color.White.copy(alpha = 0.7f),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
    EsdeContentScale.entries.forEach { scale ->
        val label = stringResource(
            when (scale) {
                EsdeContentScale.FIT -> R.string.canvas_esde_picker_scale_fit
                EsdeContentScale.CROP -> R.string.canvas_esde_picker_scale_crop
            }
        )
        RomMediaTypeRow(
            label = label,
            isSelected = scale == current,
            onClick = { onSelected(scale) }
        )
    }
}

@Composable
private fun RomContinuousSpinRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle(!enabled) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Autorenew,
            contentDescription = null,
            tint = if (enabled) ThemePrimaryColor else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.rom_options_continuous_spin),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.rom_options_continuous_spin_description),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ThemePrimaryColor,
                checkedTrackColor = ThemePrimaryColor.copy(alpha = 0.5f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}

@Composable
private fun RomRemoveRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            tint = Color(0xFFFF5252),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = stringResource(R.string.rom_remove_from_page),
            color = Color(0xFFFF5252),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RomResizePreview(
    rom: PinnedRomInfo,
    previewIconSize: Float,
    onPreviewSizeChange: (Float) -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit
) {
    val context = LocalContext.current
    val artworkFile = rom.resolveDisplayPath()?.let { File(it).takeIf { f -> f.exists() } }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.app_options_resize_preview),
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )

        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (artworkFile != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(artworkFile).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(previewIconSize.dp.coerceAtMost(120.dp))
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Default.VideogameAsset,
                    contentDescription = null,
                    tint = ThemePrimaryColor,
                    modifier = Modifier.size((previewIconSize * 0.55f).dp.coerceAtMost(66.dp))
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (previewIconSize > 32f) onPreviewSizeChange(previewIconSize - 8f) },
                enabled = previewIconSize > 32f
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = null,
                    tint = if (previewIconSize > 32f) ThemePrimaryColor else Color.Gray
                )
            }
            Text(
                text = "${previewIconSize.toInt()} dp",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            IconButton(
                onClick = { if (previewIconSize < 256f) onPreviewSizeChange(previewIconSize + 8f) },
                enabled = previewIconSize < 256f
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = if (previewIconSize < 256f) ThemePrimaryColor else Color.Gray
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.3f))
            ) {
                Text(stringResource(R.string.app_options_resize_cancel))
            }
            Button(
                onClick = onApply,
                colors = ButtonDefaults.buttonColors(containerColor = ThemePrimaryColor)
            ) {
                Text(stringResource(R.string.app_options_resize_apply))
            }
        }
    }
}

@Composable
fun RomCustomIconPickerDialog(
    roms: List<PinnedRomInfo>,
    onRomSelected: (PinnedRomInfo) -> Unit,
    onDismiss: () -> Unit
) {
    DimmedDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(vertical = 16.dp)
                .background(OledCardColor, RoundedCornerShape(24.dp))
                .border(2.dp, ThemePrimaryColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.app_options_custom_icon),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.dialog_cancel),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                roms.forEach { rom ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onRomSelected(rom) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideogameAsset,
                            contentDescription = null,
                            tint = ThemePrimaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = rom.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
