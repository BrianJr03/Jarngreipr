package jr.brian.home.esde.ui.frontend

import android.content.Intent
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import jr.brian.home.R
import jr.brian.home.esde.model.SystemCustomization
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.ThemePrimaryColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SystemCustomizationDialog(
    systemName: String,
    customization: SystemCustomization,
    onDismiss: () -> Unit,
    onChange: (SystemCustomization) -> Unit,
    onReset: () -> Unit,
    onEnterReorder: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OledCardColor,
        dragHandle = { SystemCustomizationDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CustomizationTitle(systemName = systemName)
            CustomizationDialogBody(
                customization = customization,
                onChange = onChange,
                onEnterReorder = onEnterReorder,
                onReset = onReset
            )
        }
    }
}

@Composable
private fun SystemCustomizationDragHandle() {
    Box(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .size(width = 40.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.3f))
    )
}

@Composable
private fun CustomizationTitle(systemName: String) {
    Text(
        text = systemName.uppercase(),
        color = Color.White.copy(alpha = 0.9f),
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )
}

@Composable
private fun CustomizationDialogBody(
    customization: SystemCustomization,
    onChange: (SystemCustomization) -> Unit,
    onEnterReorder: () -> Unit,
    onReset: () -> Unit
) {
    BackgroundImageSection(
        backgroundUri = customization.backgroundUri,
        onBackgroundUriChange = { uri -> onChange(customization.copy(backgroundUri = uri)) }
    )

    ShowNameToggleRow(
        showName = customization.showName,
        onShowNameChange = { onChange(customization.copy(showName = it)) }
    )

    BackgroundColorSection(
        colorArgb = customization.solidColorArgb,
        onColorChange = { argb -> onChange(customization.copy(solidColorArgb = argb)) }
    )

    ReorderButton(onEnterReorder = onEnterReorder)

    ResetButton(onReset = onReset)
}

@Composable
private fun BackgroundImageSection(
    backgroundUri: String?,
    onBackgroundUriChange: (String?) -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onBackgroundUriChange(uri.toString())
        }
    }

    SectionTitle(text = stringResource(R.string.frontend_customize_background_section))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SecondaryButton(
            label = stringResource(
                if (backgroundUri == null) R.string.frontend_customize_choose_background
                else R.string.frontend_customize_change_background
            ),
            onClick = { launcher.launch(BACKGROUND_MIME_TYPES) },
            modifier = Modifier.weight(1f)
        )
        if (backgroundUri != null) {
            SecondaryButton(
                label = stringResource(R.string.frontend_customize_remove_background),
                onClick = { onBackgroundUriChange(null) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ShowNameToggleRow(
    showName: Boolean,
    onShowNameChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onShowNameChange(!showName) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.frontend_customize_show_name),
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = showName,
            onCheckedChange = onShowNameChange,
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
private fun BackgroundColorSection(
    colorArgb: Long?,
    onColorChange: (Long?) -> Unit
) {
    SectionTitle(text = stringResource(R.string.frontend_customize_color_section))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        ColorPresetSwatch(
            label = stringResource(R.string.frontend_customize_color_default),
            color = Color.Transparent,
            useCheckerboard = false,
            isSelected = colorArgb == null,
            showRemoveSlash = true,
            onClick = { onColorChange(null) }
        )
        ColorPresetSwatch(
            label = stringResource(R.string.frontend_customize_color_transparent),
            color = Color.Transparent,
            useCheckerboard = true,
            isSelected = colorArgb == SystemCustomization.TRANSPARENT_ARGB,
            showRemoveSlash = false,
            onClick = { onColorChange(SystemCustomization.TRANSPARENT_ARGB) }
        )
    }

    val controller = rememberColorPickerController()
    val initialColor = colorArgb
        ?.takeIf { it != SystemCustomization.TRANSPARENT_ARGB }
        ?.let { Color(it.toInt()) }
        ?: Color.White

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HsvColorPicker(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            controller = controller,
            initialColor = initialColor,
            onColorChanged = { envelope ->
                if (envelope.fromUser) {
                    onColorChange(envelope.color.toArgb().toLong() and 0xFFFFFFFFL)
                }
            }
        )
        BrightnessSlider(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            controller = controller
        )
        AlphaSlider(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            controller = controller
        )
    }
}

@Composable
private fun ColorPresetSwatch(
    label: String,
    color: Color,
    useCheckerboard: Boolean,
    isSelected: Boolean,
    showRemoveSlash: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .then(
                    if (useCheckerboard) Modifier.background(checkerboardBrush())
                    else Modifier.background(if (showRemoveSlash) Color.DarkGray else color)
                )
                .border(
                    width = if (isSelected) 3.dp else if (isFocused) 2.dp else 1.dp,
                    color = when {
                        isSelected -> ThemePrimaryColor
                        isFocused -> Color.White.copy(alpha = 0.5f)
                        else -> Color.White.copy(alpha = 0.2f)
                    },
                    shape = CircleShape
                )
                .clickable { onClick() }
                .focusable()
                .onFocusChanged { isFocused = it.isFocused },
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else if (showRemoveSlash) {
                Text(
                    text = "×",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp
        )
    }
}

private fun checkerboardBrush(): androidx.compose.ui.graphics.Brush {
    return androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(Color.LightGray, Color.White)
    )
}

@Composable
private fun ReorderButton(onEnterReorder: () -> Unit) {
    SecondaryButton(
        label = stringResource(R.string.frontend_customize_reorder),
        onClick = onEnterReorder,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ResetButton(onReset: () -> Unit) {
    SecondaryButton(
        label = stringResource(R.string.frontend_customize_reset),
        onClick = onReset,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    )
}

@Composable
private fun SecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) ThemeAccentColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) ThemeAccentColor else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

private val BACKGROUND_MIME_TYPES = arrayOf(
    "image/png",
    "image/jpeg",
    "image/webp",
    "image/gif"
)
