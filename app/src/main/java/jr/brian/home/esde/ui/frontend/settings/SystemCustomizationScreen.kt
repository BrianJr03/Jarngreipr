package jr.brian.home.esde.ui.frontend.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import jr.brian.home.R
import jr.brian.home.esde.model.SystemCustomization
import jr.brian.home.esde.ui.components.ToggleSetting
import jr.brian.home.esde.util.LocalESDEImageLoader
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.subtleCardGradient
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import kotlinx.coroutines.delay

@Composable
fun SystemCustomizationScreen(
    systemName: String,
    customization: SystemCustomization,
    onDismiss: () -> Unit,
    onChange: (SystemCustomization) -> Unit,
    onReset: () -> Unit,
    onEnterReorder: () -> Unit
) {
    val cursor = rememberRailCursorState(
        entries = SystemCustomizationCategory.entries,
        initial = SystemCustomizationCategory.BACKGROUND
    )
    val rowCount = rowCountFor(cursor.selectedCategory)
    val rootFocus = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { runCatching { rootFocus.requestFocus() } }
    // Any focusable subtree underneath us (e.g. the grid the overlay draws over)
    // can steal focus when it recomposes. Yield a frame so the thief's requestFocus
    // retries settle, then reclaim it.
    LaunchedEffect(hasFocus) {
        if (!hasFocus) {
            withFrameNanos { }
            runCatching { rootFocus.requestFocus() }
        }
    }

    val registerHorizontal = remember(cursor) {
        { claims: Boolean -> cursor.registerHorizontal(claims) }
    }

    CompositionLocalProvider(
        LocalRowActivation provides cursor.activationTick,
        LocalRowStep provides cursor.horizontalStep,
        LocalHorizontalRowRegistration provides registerHorizontal
    ) {
        Surface(
            color = OledBackgroundColor,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(rootFocus)
                .onFocusChanged { state -> hasFocus = state.hasFocus }
                .focusTarget()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    cursor.handleKey(
                        keyCode = event.nativeKeyEvent.keyCode,
                        rowCount = rowCount,
                        onClose = onDismiss
                    )
                }
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                CategoryRail(
                    headerText = systemName.uppercase(),
                    entries = SystemCustomizationCategory.entries,
                    selected = cursor.selectedCategory,
                    railHasFocus = cursor.focusOnRail
                )
                CustomizationRowPane(
                    category = cursor.selectedCategory,
                    customization = customization,
                    focusedRow = if (cursor.focusOnRail) -1 else cursor.focusedRow,
                    onChange = onChange,
                    onReset = onReset,
                    onEnterReorder = onEnterReorder
                )
            }
        }
    }
}

private fun rowCountFor(category: SystemCustomizationCategory): Int = when (category) {
    SystemCustomizationCategory.BACKGROUND -> 3
    SystemCustomizationCategory.COLOR -> 6
    SystemCustomizationCategory.ACTIONS -> 2
}

@Composable
private fun CustomizationRowPane(
    category: SystemCustomizationCategory,
    customization: SystemCustomization,
    focusedRow: Int,
    onChange: (SystemCustomization) -> Unit,
    onReset: () -> Unit,
    onEnterReorder: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp, end = 16.dp, bottom = 16.dp)
    ) {
        PaneHeader(category = category)
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(end = 4.dp)),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CustomizationRows(
                category = category,
                customization = customization,
                focusedRow = focusedRow,
                onChange = onChange,
                onReset = onReset,
                onEnterReorder = onEnterReorder
            )
        }
    }
}

@Composable
private fun PaneHeader(category: SystemCustomizationCategory) {
    Column {
        Text(
            text = stringResource(category.titleRes),
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(category.summaryRes),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun CustomizationRows(
    category: SystemCustomizationCategory,
    customization: SystemCustomization,
    focusedRow: Int,
    onChange: (SystemCustomization) -> Unit,
    onReset: () -> Unit,
    onEnterReorder: () -> Unit
) {
    when (category) {
        SystemCustomizationCategory.BACKGROUND -> BackgroundRows(
            customization = customization,
            focusedRow = focusedRow,
            onChange = onChange
        )
        SystemCustomizationCategory.COLOR -> ColorRows(
            customization = customization,
            focusedRow = focusedRow,
            onChange = onChange
        )
        SystemCustomizationCategory.ACTIONS -> ActionRows(
            focusedRow = focusedRow,
            onReset = onReset,
            onEnterReorder = onEnterReorder
        )
    }
}

@Composable
private fun BackgroundRows(
    customization: SystemCustomization,
    focusedRow: Int,
    onChange: (SystemCustomization) -> Unit
) {
    val tileFocused = focusedRow == 0
    val focusBgFocused = focusedRow == 1
    val showNameFocused = focusedRow == 2

    BackgroundPickerRow(
        title = stringResource(R.string.frontend_customize_tile_background_title),
        chooseLabel = stringResource(R.string.frontend_customize_choose_background),
        changeLabel = stringResource(R.string.frontend_customize_change_background),
        uri = customization.backgroundUri,
        focused = tileFocused,
        onChangeUri = { onChange(customization.copy(backgroundUri = it)) }
    )

    BackgroundPickerRow(
        title = stringResource(R.string.frontend_customize_focus_background_section),
        chooseLabel = stringResource(R.string.frontend_customize_choose_focus_background),
        changeLabel = stringResource(R.string.frontend_customize_change_focus_background),
        uri = customization.focusBackgroundUri,
        focused = focusBgFocused,
        onChangeUri = { onChange(customization.copy(focusBackgroundUri = it)) }
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        ActivateOnConfirm(focused = showNameFocused) {
            onChange(customization.copy(showName = !customization.showName))
        }
        ToggleSetting(
            title = stringResource(R.string.frontend_customize_show_name),
            description = stringResource(R.string.frontend_customize_show_name_description),
            checked = customization.showName,
            onCheckedChange = { onChange(customization.copy(showName = it)) },
            focused = showNameFocused
        )
    }
}

@Composable
private fun BackgroundPickerRow(
    title: String,
    chooseLabel: String,
    changeLabel: String,
    uri: String?,
    focused: Boolean,
    onChangeUri: (String?) -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { picked ->
        if (picked != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    picked,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onChangeUri(picked.toString())
        }
    }

    val hasUri = uri != null
    ActivateOnConfirm(focused = focused) { launcher.launch(BACKGROUND_MIME_TYPES) }

    if (hasUri) {
        RegisterForHorizontalSteps(focused)
        StepOnHorizontal(focused) { delta ->
            if (delta > 0) onChangeUri(null)
        }
    }

    val removeLabel = stringResource(R.string.frontend_customize_remove_trailing)
    BackgroundPickerCard(
        title = title,
        primaryLabel = if (hasUri) changeLabel else chooseLabel,
        trailingLabel = if (hasUri) removeLabel else null,
        previewUri = uri,
        focused = focused
    )
}

@Composable
private fun BackgroundPickerCard(
    title: String,
    primaryLabel: String,
    trailingLabel: String?,
    previewUri: String?,
    focused: Boolean
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(focused))
            .background(brush = subtleCardGradient(focused), shape = shape)
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) ThemePrimaryColor.copy(alpha = 0.5f) else Color.Transparent,
                shape = shape
            )
            .clip(shape)
            .revealWhenFocused(focused)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BackgroundPickerThumbnail(uri = previewUri)
        if (previewUri != null) Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = primaryLabel,
                color = Color.Gray,
                fontSize = 13.sp
            )
        }
        if (trailingLabel != null) {
            Text(
                text = trailingLabel,
                color = ThemePrimaryColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun BackgroundPickerThumbnail(uri: String?) {
    if (uri == null) return
    val context = LocalContext.current
    val imageLoader = LocalESDEImageLoader.current
    val shape = RoundedCornerShape(8.dp)
    AsyncImage(
        model = ImageRequest.Builder(context).data(uri).build(),
        imageLoader = imageLoader,
        contentDescription = null,
        modifier = Modifier
            .size(48.dp)
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.4f), shape)
    )
}

@Composable
private fun ColorRows(
    customization: SystemCustomization,
    focusedRow: Int,
    onChange: (SystemCustomization) -> Unit
) {
    val current = customization.solidColorArgb
    val channels = argbToChannels(current)

    val commit: (ColorChannels) -> Unit = { updated ->
        val argb = channelsToArgb(updated)
        onChange(customization.copy(solidColorArgb = argb))
    }

    val defaultFocused = focusedRow == 0
    val transparentFocused = focusedRow == 1
    val hueFocused = focusedRow == 2
    val satFocused = focusedRow == 3
    val brightFocused = focusedRow == 4
    val alphaFocused = focusedRow == 5

    ColorPreviewSwatch(channels = channels)

    Box(modifier = Modifier.fillMaxWidth()) {
        ActivateOnConfirm(focused = defaultFocused) {
            onChange(customization.copy(solidColorArgb = null))
        }
        ColorPresetRow(
            title = stringResource(R.string.frontend_customize_color_default),
            focused = defaultFocused,
            isSelected = current == null,
            isTransparent = false,
            isDefault = true
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        ActivateOnConfirm(focused = transparentFocused) {
            onChange(customization.copy(solidColorArgb = SystemCustomization.TRANSPARENT_ARGB))
        }
        ColorPresetRow(
            title = stringResource(R.string.frontend_customize_color_transparent),
            focused = transparentFocused,
            isSelected = current == SystemCustomization.TRANSPARENT_ARGB,
            isTransparent = true,
            isDefault = false
        )
    }

    ColorChannelStepperRow(
        title = stringResource(R.string.frontend_customize_channel_hue),
        focused = hueFocused,
        normalizedValue = channels.hueDegrees / 360f,
        displayValue = stringResource(R.string.frontend_customize_channel_hue_value, channels.hueDegrees.toInt()),
        onStep = { delta ->
            val next = (channels.hueDegrees + delta * HUE_STEP).let {
                val wrapped = it % 360f
                if (wrapped < 0f) wrapped + 360f else wrapped
            }
            commit(channels.copy(hueDegrees = next))
        }
    )
    ColorChannelStepperRow(
        title = stringResource(R.string.frontend_customize_channel_saturation),
        focused = satFocused,
        normalizedValue = channels.saturation,
        displayValue = stringResource(R.string.frontend_customize_channel_percent, (channels.saturation * 100).toInt()),
        onStep = { delta ->
            val next = (channels.saturation + delta * SAT_STEP).coerceIn(0f, 1f)
            commit(channels.copy(saturation = next))
        }
    )
    ColorChannelStepperRow(
        title = stringResource(R.string.frontend_customize_channel_brightness),
        focused = brightFocused,
        normalizedValue = channels.brightness,
        displayValue = stringResource(R.string.frontend_customize_channel_percent, (channels.brightness * 100).toInt()),
        onStep = { delta ->
            val next = (channels.brightness + delta * BRIGHT_STEP).coerceIn(0f, 1f)
            commit(channels.copy(brightness = next))
        }
    )
    ColorChannelStepperRow(
        title = stringResource(R.string.frontend_customize_channel_opacity),
        focused = alphaFocused,
        normalizedValue = channels.alpha,
        displayValue = stringResource(R.string.frontend_customize_channel_percent, (channels.alpha * 100).toInt()),
        onStep = { delta ->
            val next = (channels.alpha + delta * ALPHA_STEP).coerceIn(0f, 1f)
            commit(channels.copy(alpha = next))
        }
    )
}

@Composable
private fun ActionRows(
    focusedRow: Int,
    onReset: () -> Unit,
    onEnterReorder: () -> Unit
) {
    val reorderFocused = focusedRow == 0
    val resetFocused = focusedRow == 1

    Box(modifier = Modifier.fillMaxWidth()) {
        ActivateOnConfirm(focused = reorderFocused, onActivate = onEnterReorder)
        ToggleSetting(
            title = stringResource(R.string.frontend_customize_reorder),
            description = stringResource(R.string.frontend_customize_reorder_description),
            checked = false,
            showToggle = false,
            onClick = onEnterReorder,
            focused = reorderFocused
        )
    }

    ResetRow(focused = resetFocused, onReset = onReset)
}

@Composable
private fun ResetRow(focused: Boolean, onReset: () -> Unit) {
    var armed by remember { mutableStateOf(false) }
    var armedTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(focused) {
        if (!focused) armed = false
    }

    LaunchedEffect(armedTick) {
        if (armed) {
            delay(RESET_DISARM_MS)
            armed = false
        }
    }

    ActivateOnConfirm(focused = focused) {
        if (armed) {
            onReset()
            armed = false
        } else {
            armed = true
            armedTick += 1
        }
    }

    val title = if (armed) {
        stringResource(R.string.frontend_customize_reset_confirm)
    } else {
        stringResource(R.string.frontend_customize_reset)
    }
    val description = if (armed) {
        stringResource(R.string.frontend_customize_reset_confirm_description)
    } else {
        stringResource(R.string.frontend_customize_reset_description)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        ToggleSetting(
            title = title,
            description = description,
            checked = false,
            showToggle = false,
            onClick = {},
            focused = focused
        )
    }
}

private const val HUE_STEP = 5f
private const val SAT_STEP = 0.02f
private const val BRIGHT_STEP = 0.02f
private const val ALPHA_STEP = 0.05f
private const val RESET_DISARM_MS = 3000L

private val BACKGROUND_MIME_TYPES = arrayOf(
    "image/png",
    "image/jpeg",
    "image/webp",
    "image/gif"
)
