package jr.brian.home.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.ui.components.konfetti.GameKonfettiConfig
import jr.brian.home.ui.components.konfetti.KonfettiPreset
import jr.brian.home.ui.components.konfetti.KonfettiShapeFactory
import jr.brian.home.ui.components.konfetti.KonfettiTrigger
import jr.brian.home.ui.components.konfetti.LetterFormationBurst
import jr.brian.home.ui.components.settings.CollapsibleSettingsSection
import jr.brian.home.ui.components.settings.ScreenHeader
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalGameKonfettiManager
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.compose.OnParticleSystemUpdateListener
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.PartySystem
import kotlin.math.roundToInt

@Composable
fun KonfettiEditorScreen(
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val gameKonfettiManager = LocalGameKonfettiManager.current
    var config by remember { mutableStateOf(gameKonfettiManager.config) }

    var triggerExpanded by remember { mutableStateOf(false) }
    var modeExpanded by remember { mutableStateOf(false) }
    var shapeExpanded by remember { mutableStateOf(false) }
    var customExpanded by remember { mutableStateOf(false) }
    var letterBurstExpanded by remember { mutableStateOf(false) }

    var previewTrigger by remember { mutableIntStateOf(0) }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var isLetterBurstPreview by remember { mutableStateOf(false) }

    val themeColors = listOf(
        ThemePrimaryColor.toArgb(),
        ThemeSecondaryColor.toArgb(),
        ThemeAccentColor.toArgb()
    )

    fun save(newConfig: GameKonfettiConfig) {
        config = newConfig
        gameKonfettiManager.updateConfig(newConfig)
    }

    fun previewParties(): List<Party> {
        val charShape = if (config.useCharShape) {
            KonfettiShapeFactory.createCharShape(context, 'A')
        } else null
        return gameKonfettiManager.buildParties(themeColors, charShape)
    }

    BackHandler { onDismiss() }

    Scaffold(
        containerColor = OledBackgroundColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .systemBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ScreenHeader(
                    showVersion = false,
                    onBackClick = onDismiss
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.konfetti_editor_title),
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    // Enable toggle
                    item {
                        EnableToggleRow(
                            enabled = config.enabled,
                            onToggle = { save(config.copy(enabled = it)) }
                        )
                    }

                    // Trigger section
                    item {
                        CollapsibleSettingsSection(
                            title = stringResource(R.string.konfetti_editor_trigger_title),
                            icon = Icons.Default.TouchApp,
                            isExpanded = triggerExpanded,
                            onToggle = { triggerExpanded = !triggerExpanded }
                        ) {
                            Text(
                                text = stringResource(R.string.konfetti_editor_trigger_description),
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                KonfettiTrigger.entries.forEach { trigger ->
                                    ModeChip(
                                        label = trigger.displayName,
                                        selected = config.trigger == trigger,
                                        onClick = { save(config.copy(trigger = trigger)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Live preview
                    item {
                        PreviewCard(
                            isPlaying = isPreviewPlaying,
                            previewTrigger = previewTrigger,
                            isLetterBurst = config.isLetterBurst,
                            letterBurstConfig = config,
                            themeColors = themeColors,
                            parties = { previewParties() },
                            onPreviewEnd = {
                                isPreviewPlaying = false
                                isLetterBurstPreview = false
                            },
                            onPreviewClick = {
                                previewTrigger++
                                isLetterBurstPreview = config.isLetterBurst
                                isPreviewPlaying = true
                            }
                        )
                    }

                    // Animation Mode section
                    item {
                        CollapsibleSettingsSection(
                            title = stringResource(R.string.konfetti_editor_mode_title),
                            icon = Icons.Default.Celebration,
                            isExpanded = modeExpanded,
                            onToggle = { modeExpanded = !modeExpanded }
                        ) {
                            ModeToggle(
                                usePreset = config.usePreset,
                                onToggle = { save(config.copy(usePreset = it)) }
                            )

                            if (config.usePreset) {
                                PresetSelector(
                                    selectedPreset = config.selectedPreset,
                                    onSelect = { save(config.copy(selectedPreset = it)) }
                                )
                            }
                        }
                    }

                    // Custom params section (only when not using preset)
                    if (!config.usePreset) {
                        item {
                            CollapsibleSettingsSection(
                                title = stringResource(R.string.konfetti_editor_custom_params),
                                icon = Icons.Default.Tune,
                                isExpanded = customExpanded,
                                onToggle = { customExpanded = !customExpanded }
                            ) {
                                LabeledSlider(
                                    label = stringResource(R.string.konfetti_editor_speed),
                                    value = config.speed,
                                    range = 0f..100f,
                                    onValueChange = { save(config.copy(speed = it)) }
                                )
                                LabeledSlider(
                                    label = stringResource(R.string.konfetti_editor_max_speed),
                                    value = config.maxSpeed,
                                    range = 1f..100f,
                                    onValueChange = { save(config.copy(maxSpeed = it)) }
                                )
                                LabeledSlider(
                                    label = stringResource(R.string.konfetti_editor_damping),
                                    value = config.damping,
                                    range = 0.1f..1f,
                                    onValueChange = { save(config.copy(damping = it)) }
                                )
                                LabeledSlider(
                                    label = stringResource(R.string.konfetti_editor_angle),
                                    value = config.angle.toFloat(),
                                    range = 0f..360f,
                                    onValueChange = { save(config.copy(angle = it.roundToInt())) },
                                    showAsInt = true
                                )
                                LabeledSlider(
                                    label = stringResource(R.string.konfetti_editor_spread),
                                    value = config.spread.toFloat(),
                                    range = 1f..360f,
                                    onValueChange = { save(config.copy(spread = it.roundToInt())) },
                                    showAsInt = true
                                )
                                LabeledSlider(
                                    label = stringResource(R.string.konfetti_editor_time_to_live),
                                    value = config.timeToLive.toFloat(),
                                    range = 500f..10000f,
                                    onValueChange = { save(config.copy(timeToLive = it.toLong())) },
                                    showAsInt = true,
                                    suffix = "ms"
                                )
                                LabeledSlider(
                                    label = stringResource(R.string.konfetti_editor_emitter_duration),
                                    value = config.emitterDurationMs.toFloat(),
                                    range = 100f..10000f,
                                    onValueChange = { save(config.copy(emitterDurationMs = it.toLong())) },
                                    showAsInt = true,
                                    suffix = "ms"
                                )
                                LabeledSlider(
                                    label = stringResource(R.string.konfetti_editor_emitter_rate),
                                    value = config.emitterAmountPerSecond.toFloat(),
                                    range = 1f..200f,
                                    onValueChange = { save(config.copy(emitterAmountPerSecond = it.roundToInt())) },
                                    showAsInt = true,
                                    suffix = "/s"
                                )
                            }
                        }
                    }

                    // Shape section
                    item {
                        CollapsibleSettingsSection(
                            title = stringResource(R.string.konfetti_editor_shape_title),
                            icon = Icons.Default.TextFields,
                            isExpanded = shapeExpanded,
                            onToggle = { shapeExpanded = !shapeExpanded }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.konfetti_editor_use_char),
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = stringResource(R.string.konfetti_editor_use_char_description),
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 13.sp
                                    )
                                }
                                Switch(
                                    checked = config.useCharShape,
                                    onCheckedChange = { save(config.copy(useCharShape = it)) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = ThemePrimaryColor,
                                        checkedTrackColor = ThemePrimaryColor.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }
                    }

                    // Letter Burst settings (visible when LETTER_BURST preset is selected)
                    if (config.isLetterBurst) {
                        item {
                            CollapsibleSettingsSection(
                                title = stringResource(R.string.konfetti_editor_letter_burst_title),
                                icon = Icons.Default.TextFields,
                                isExpanded = letterBurstExpanded,
                                onToggle = { letterBurstExpanded = !letterBurstExpanded }
                            ) {
                                // Character input
                                Text(
                                    text = stringResource(R.string.konfetti_editor_letter_burst_char),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = stringResource(R.string.konfetti_editor_letter_burst_char_description),
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                OutlinedTextField(
                                    value = config.letterBurstChar,
                                    onValueChange = { newVal ->
                                        // Allow only a single uppercase letter or digit
                                        val filtered = newVal
                                            .uppercase()
                                            .filter { it.isLetterOrDigit() }
                                            .take(1)
                                        save(config.copy(letterBurstChar = filtered))
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = {
                                        Text(
                                            "Auto (game's first letter)",
                                            color = Color.White.copy(alpha = 0.3f)
                                        )
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = ThemePrimaryColor,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                        cursorColor = ThemePrimaryColor
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Burst style selector
                                Text(
                                    text = stringResource(R.string.konfetti_editor_letter_burst_explode_preset),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = stringResource(R.string.konfetti_editor_letter_burst_explode_description),
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                // Show standard presets (excluding NONE and LETTER_BURST) as burst options
                                val burstOptions = KonfettiPreset.entries.filter {
                                    it != KonfettiPreset.NONE && it != KonfettiPreset.LETTER_BURST
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    burstOptions.forEach { preset ->
                                        val isSelected = preset.name == config.letterBurstExplodePreset
                                        Button(
                                            onClick = {
                                                save(config.copy(letterBurstExplodePreset = preset.name))
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) {
                                                    ThemePrimaryColor.copy(alpha = 0.3f)
                                                } else {
                                                    Color.White.copy(alpha = 0.05f)
                                                },
                                                contentColor = if (isSelected) Color.White else Color.White.copy(
                                                    alpha = 0.7f
                                                )
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = preset.displayName,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Formation speed slider
                                LabeledSlider(
                                    label = stringResource(R.string.konfetti_editor_letter_burst_formation_speed),
                                    value = config.letterBurstFormationMs.toFloat(),
                                    range = 400f..3000f,
                                    onValueChange = {
                                        save(config.copy(letterBurstFormationMs = it.roundToInt()))
                                    },
                                    showAsInt = true,
                                    suffix = "ms"
                                )

                                // Hold duration slider
                                LabeledSlider(
                                    label = stringResource(R.string.konfetti_editor_letter_burst_hold),
                                    value = config.letterBurstHoldMs.toFloat(),
                                    range = 100f..2000f,
                                    onValueChange = {
                                        save(config.copy(letterBurstHoldMs = it.toLong()))
                                    },
                                    showAsInt = true,
                                    suffix = "ms"
                                )

                                // Particle count slider
                                LabeledSlider(
                                    label = stringResource(R.string.konfetti_editor_letter_burst_particles),
                                    value = config.letterBurstParticleCount.toFloat(),
                                    range = 30f..200f,
                                    onValueChange = {
                                        save(config.copy(letterBurstParticleCount = it.roundToInt()))
                                    },
                                    showAsInt = true
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }

            // Full-screen preview overlay
            if (isPreviewPlaying) {
                key(previewTrigger) {
                    if (isLetterBurstPreview) {
                        val previewChar = config.letterBurstChar
                            .firstOrNull()?.uppercaseChar() ?: 'A'
                        LetterFormationBurst(
                            char = previewChar,
                            colors = themeColors,
                            burstPreset = KonfettiPreset.fromName(config.letterBurstExplodePreset),
                            formationDurationMs = config.letterBurstFormationMs,
                            holdDurationMs = config.letterBurstHoldMs,
                            particleCount = config.letterBurstParticleCount,
                            onComplete = {
                                isPreviewPlaying = false
                                isLetterBurstPreview = false
                            }
                        )
                    } else {
                        KonfettiView(
                            modifier = Modifier.fillMaxSize(),
                            parties = previewParties(),
                            updateListener = object : OnParticleSystemUpdateListener {
                                override fun onParticleSystemEnded(
                                    system: PartySystem,
                                    activeSystems: Int
                                ) {
                                    if (activeSystems == 0) isPreviewPlaying = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnableToggleRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = ThemePrimaryColor.copy(alpha = if (enabled) 0.15f else 0.05f),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = ThemePrimaryColor.copy(alpha = if (enabled) 0.5f else 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.konfetti_editor_enable),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.konfetti_editor_enable_description),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ThemePrimaryColor,
                checkedTrackColor = ThemePrimaryColor.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun PreviewCard(
    isPlaying: Boolean,
    previewTrigger: Int,
    isLetterBurst: Boolean = false,
    letterBurstConfig: GameKonfettiConfig? = null,
    themeColors: List<Int> = emptyList(),
    parties: () -> List<Party>,
    onPreviewEnd: () -> Unit,
    onPreviewClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.3f))
            .border(
                width = 1.dp,
                color = ThemePrimaryColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        if (isPlaying) {
            key(previewTrigger) {
                if (isLetterBurst && letterBurstConfig != null) {
                    val previewChar = letterBurstConfig.letterBurstChar
                        .firstOrNull()?.uppercaseChar() ?: 'A'
                    LetterFormationBurst(
                        char = previewChar,
                        colors = themeColors,
                        burstPreset = KonfettiPreset.fromName(letterBurstConfig.letterBurstExplodePreset),
                        formationDurationMs = letterBurstConfig.letterBurstFormationMs,
                        holdDurationMs = letterBurstConfig.letterBurstHoldMs,
                        particleCount = letterBurstConfig.letterBurstParticleCount,
                        onComplete = onPreviewEnd
                    )
                } else {
                    KonfettiView(
                        modifier = Modifier.fillMaxSize(),
                        parties = parties(),
                        updateListener = object : OnParticleSystemUpdateListener {
                            override fun onParticleSystemEnded(
                                system: PartySystem,
                                activeSystems: Int
                            ) {
                                if (activeSystems == 0) onPreviewEnd()
                            }
                        }
                    )
                }
            }
        }

        Button(
            onClick = onPreviewClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ThemePrimaryColor,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(stringResource(R.string.konfetti_editor_preview))
        }
    }
}

@Composable
private fun ModeToggle(
    usePreset: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ModeChip(
            label = stringResource(R.string.konfetti_editor_mode_preset),
            selected = usePreset,
            onClick = { onToggle(true) },
            modifier = Modifier.weight(1f)
        )
        ModeChip(
            label = stringResource(R.string.konfetti_editor_mode_custom),
            selected = !usePreset,
            onClick = { onToggle(false) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) ThemePrimaryColor else Color.White.copy(alpha = 0.08f),
            contentColor = if (selected) Color.White else Color.White.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun PresetSelector(
    selectedPreset: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        KonfettiPreset.entries.forEach { preset ->
            val isSelected = preset.name == selectedPreset
            Button(
                onClick = { onSelect(preset.name) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) {
                        ThemePrimaryColor.copy(alpha = 0.3f)
                    } else {
                        Color.White.copy(alpha = 0.05f)
                    },
                    contentColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = preset.displayName,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    showAsInt: Boolean = false,
    suffix: String = ""
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = if (showAsInt) {
                    "${value.roundToInt()}$suffix"
                } else {
                    "${"%.1f".format(value)}$suffix"
                },
                color = ThemeAccentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = ThemePrimaryColor,
                activeTrackColor = ThemePrimaryColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}
