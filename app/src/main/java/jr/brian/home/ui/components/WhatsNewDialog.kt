package jr.brian.home.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.ui.components.dialog.DimmedBottomSheet
import jr.brian.home.ui.components.konfetti.KonfettiPreset
import jr.brian.home.ui.extensions.clickWithHaptic
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun WhatsNewDialog(
    versionName: String,
    patchNotes: String,
    onDismiss: () -> Unit
) {
//    val whatsNewManager = LocalWhatsNewManager.current
//    val selectedPreset = whatsNewManager.selectedKonfettiPreset

//    var konfettiTrigger by remember { mutableIntStateOf(0) }
//    var isKonfettiPlaying by remember { mutableStateOf(selectedPreset != KonfettiPreset.NONE) }

    DimmedBottomSheet(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.whats_new_title),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.whats_new_version, versionName),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.whats_new_close),
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    MarkdownText(
                        text = patchNotes,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

//                    KonfettiAnimationChooser(
//                        selectedPreset = selectedPreset,
//                        onPresetSelected = { preset ->
//                            whatsNewManager.setKonfettiPreset(preset)
//                            if (preset == KonfettiPreset.NONE) {
//                                isKonfettiPlaying = false
//                            } else {
//                                konfettiTrigger++
//                                isKonfettiPlaying = true
//                            }
//                        }
//                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemePrimaryColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.whats_new_got_it))
                }
            }

//            val parties = KonfettiPresets.getParties(selectedPreset)
//            if (isKonfettiPlaying) {
//                key(konfettiTrigger) {
//                    KonfettiView(
//                        modifier = Modifier.fillMaxWidth(),
//                        parties = parties,
//                        updateListener = object : OnParticleSystemUpdateListener {
//                            override fun onParticleSystemEnded(
//                                system: PartySystem,
//                                activeSystems: Int
//                            ) {
//                                if (activeSystems == 0) {
//                                    isKonfettiPlaying = false
//                                }
//                            }
//                        }
//                    )
//                }
//            }
        }
    }
}

@Composable
private fun KonfettiAnimationChooser(
    selectedPreset: KonfettiPreset,
    onPresetSelected: (KonfettiPreset) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(false) }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "konfetti_chevron_rotation"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // Collapsible header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isExpanded) {
                            listOf(
                                ThemePrimaryColor.copy(alpha = 0.3f),
                                ThemeSecondaryColor.copy(alpha = 0.2f)
                            )
                        } else {
                            listOf(
                                ThemePrimaryColor.copy(alpha = 0.1f),
                                ThemeSecondaryColor.copy(alpha = 0.08f)
                            )
                        }
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = if (isExpanded) 2.dp else 1.dp,
                    brush = Brush.linearGradient(
                        colors = if (isExpanded) {
                            listOf(
                                ThemePrimaryColor.copy(alpha = 0.8f),
                                ThemeSecondaryColor.copy(alpha = 0.6f),
                            )
                        } else {
                            listOf(
                                ThemePrimaryColor.copy(alpha = 0.4f),
                                ThemeSecondaryColor.copy(alpha = 0.3f),
                            )
                        }
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .clickWithHaptic(haptic) { isExpanded = !isExpanded }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        imageVector = Icons.Default.Celebration,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.konfetti_section_title),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedPreset.displayName,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) {
                        stringResource(R.string.konfetti_collapse)
                    } else {
                        stringResource(R.string.konfetti_expand)
                    },
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(rotationAngle),
                    tint = Color.White
                )
            }
        }

        // Expandable content with animation options
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KonfettiPreset.entries.forEach { preset ->
                    KonfettiPresetOption(
                        preset = preset,
                        isSelected = preset == selectedPreset,
                        onClick = { onPresetSelected(preset) }
                    )
                }
            }
        }
    }
}

@Composable
private fun KonfettiPresetOption(
    preset: KonfettiPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = if (isSelected) {
                        listOf(
                            ThemePrimaryColor.copy(alpha = 0.4f),
                            ThemeSecondaryColor.copy(alpha = 0.3f)
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0.03f)
                        )
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                brush = Brush.linearGradient(
                    colors = if (isSelected) {
                        listOf(
                            ThemePrimaryColor.copy(alpha = 0.8f),
                            ThemeSecondaryColor.copy(alpha = 0.6f)
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.1f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickWithHaptic(haptic) { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isSelected) ThemePrimaryColor else Color.White.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = preset.displayName,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
            if (isSelected) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ThemePrimaryColor.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = stringResource(R.string.konfetti_selected),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val lines = text.split("\n")
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            when {
                line.startsWith("### ") -> MarkdownHeader3(line)
                line.startsWith("## ") -> MarkdownHeader2(line)
                line.startsWith("# ") -> MarkdownHeader1(line)
                line.trim().startsWith("- ") || line.trim().startsWith("* ") ->
                    MarkdownBulletPoint(line)

                line.trim().startsWith(">") -> MarkdownBlockquote(line)
                line.trim().startsWith("```") -> {
                    i++
                    val codeLines = mutableListOf<String>()
                    while (i < lines.size && !lines[i].trim().startsWith("```")) {
                        codeLines.add(lines[i])
                        i++
                    }
                    MarkdownCodeBlock(codeLines)
                }

                line.contains("**") -> MarkdownBoldText(line)
                line.isBlank() -> MarkdownEmptyLine()
                else -> MarkdownRegularText(line)
            }
            i++
        }
    }
}

@Composable
private fun MarkdownHeader1(line: String) {
    Text(
        text = line.removePrefix("# "),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.padding(vertical = 16.dp)
    )
}

@Composable
private fun MarkdownHeader2(line: String) {
    Text(
        text = line.removePrefix("## "),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
private fun MarkdownHeader3(line: String) {
    Text(
        text = line.removePrefix("### "),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun MarkdownBulletPoint(line: String) {
    val leadingSpaces = line.length - line.trimStart().length
    val indentLevel = leadingSpaces / 2
    val content = line.trim().removePrefix("- ").removePrefix("* ")
    Row(
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
            .padding(start = (indentLevel * 16).dp)
    ) {
        Text(
            text = if (indentLevel > 0) "◦ " else "• ",
            style = MaterialTheme.typography.bodyLarge,
            color = ThemePrimaryColor
        )
        Text(
            text = parseInlineMarkdown(content),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.85f),
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun MarkdownBlockquote(line: String) {
    val quoteText = line.trim().removePrefix(">").trimStart()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Surface(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp),
            color = ThemePrimaryColor.copy(alpha = 0.6f),
            shape = MaterialTheme.shapes.extraSmall
        ) {}
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = parseInlineMarkdown(quoteText),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.6f),
            lineHeight = 24.sp,
            fontStyle = FontStyle.Italic
        )
    }
}

@Composable
private fun MarkdownCodeBlock(codeLines: List<String>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        color = Color.White.copy(alpha = 0.08f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = codeLines.joinToString("\n"),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun MarkdownBoldText(line: String) {
    Text(
        text = parseInlineMarkdown(line),
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun MarkdownEmptyLine() {
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun MarkdownRegularText(line: String) {
    Text(
        text = parseInlineMarkdown(line),
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White.copy(alpha = 0.85f),
        lineHeight = 24.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun parseInlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            i + 1 < text.length && text[i] == '*' && text[i + 1] == '*' -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color.White.copy(alpha = 0.12f),
                            color = ThemePrimaryColor
                        )
                    ) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }
}
