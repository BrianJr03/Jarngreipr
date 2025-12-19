package jr.brian.home.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.theme.ColorTheme
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun CustomThemeScreen(
    onNavigateBack: () -> Unit,
    onThemeCreated: (ColorTheme) -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    var selectedPrimaryColor by remember { mutableStateOf(Color(0xFF8A2BE2)) }
    var selectedSecondaryColor by remember { mutableStateOf(Color(0xFFFF69B4)) }
    var isSolidColor by remember { mutableStateOf(false) }

    val colorOptions = remember {
        listOf(
            // Reds
            Color(0xFFE94560), Color(0xFFFF6B6B), Color(0xFFFF4757), Color(0xFFEE5A6F),
            // Pinks
            Color(0xFFFF69B4), Color(0xFFFF1493), Color(0xFFFFC0CB), Color(0xFFFF85A1),
            // Purples
            Color(0xFF8A2BE2), Color(0xFF9B59B6), Color(0xFF6A0DAD), Color(0xFFAA00FF),
            // Blues
            Color(0xFF4169E1), Color(0xFF0F3460), Color(0xFF00CED1), Color(0xFF3742FA),
            // Cyans/Teals
            Color(0xFF00CED1), Color(0xFF17A2B8), Color(0xFF1ABC9C), Color(0xFF48C9B0),
            // Greens
            Color(0xFF008B45), Color(0xFF00FF00), Color(0xFF2ECC71), Color(0xFF27AE60),
            // Yellows
            Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFF39C12), Color(0xFFFFEB3B),
            // Oranges
            Color(0xFFFF8C00), Color(0xFFFF6347), Color(0xFFE67E22), Color(0xFFFF5722),
            // Grays/Whites
            Color.LightGray, Color.White, Color(0xFFBDC3C7), Color(0xFF95A5A6),
            // Dark tones
            Color(0xFF2C3E50), Color(0xFF34495E), Color(0xFF7F8C8D), Color(0xFF546E7A),
        )
    }

    Scaffold(
        containerColor = OledBackgroundColor,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .systemBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.custom_theme_dialog_title),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                // Preview box in top bar
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(48.dp)
                        .background(
                            brush = if (isSolidColor) {
                                Brush.linearGradient(
                                    listOf(
                                        selectedPrimaryColor,
                                        selectedPrimaryColor
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        selectedPrimaryColor,
                                        selectedSecondaryColor
                                    )
                                )
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Solid color toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.custom_theme_solid_color),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(
                        checked = isSolidColor,
                        onCheckedChange = { isSolidColor = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ThemePrimaryColor,
                            checkedTrackColor = ThemeSecondaryColor.copy(alpha = 0.5f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }
            }

            // Color selection section
            if (isSolidColor) {
                // Single color section header
                item {
                    Text(
                        text = stringResource(R.string.custom_theme_color),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Color grid rows
                val colorRows = colorOptions.chunked(8)
                items(colorRows.size) { rowIndex ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            8.dp,
                            Alignment.CenterHorizontally
                        )
                    ) {
                        for (color in colorRows[rowIndex]) {
                            ColorSwatch(
                                color = color,
                                isSelected = selectedPrimaryColor == color,
                                onClick = { selectedPrimaryColor = color }
                            )
                        }
                    }
                }
            } else {
                // Split view headers
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.custom_theme_primary_color),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.custom_theme_secondary_color),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Color grid rows - split view
                val colorRows = colorOptions.chunked(4)
                items(colorRows.size) { rowIndex ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Primary color column
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(
                                8.dp,
                                Alignment.CenterHorizontally
                            )
                        ) {
                            for (color in colorRows[rowIndex]) {
                                ColorSwatch(
                                    color = color,
                                    isSelected = selectedPrimaryColor == color,
                                    onClick = { selectedPrimaryColor = color }
                                )
                            }
                        }
                        // Secondary color column
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(
                                8.dp,
                                Alignment.CenterHorizontally)
                        ) {
                            for (color in colorRows[rowIndex]) {
                                ColorSwatch(
                                    color = color,
                                    isSelected = selectedSecondaryColor == color,
                                    onClick = { selectedSecondaryColor = color }
                                )
                            }
                        }
                    }
                }
            }

            // Action buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionButton(
                        text = stringResource(R.string.dialog_cancel),
                        onClick = onNavigateBack,
                        isPrimary = false,
                        modifier = Modifier.weight(1f)
                    )
                    ActionButton(
                        text = stringResource(R.string.custom_theme_create),
                        onClick = {
                            val customTheme = ColorTheme.createCustomTheme(
                                primaryColor = selectedPrimaryColor,
                                secondaryColor = if (isSolidColor) null else selectedSecondaryColor,
                                name = "Custom Theme"
                            )
                            onThemeCreated(customTheme)
                        },
                        isPrimary = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(36.dp)
            .scale(animatedFocusedScale(isFocused))
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else if (isFocused) 2.dp else 0.dp,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
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
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
    isPrimary: Boolean,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val backgroundColor = when {
        isPrimary && isFocused -> Brush.linearGradient(
            listOf(ThemePrimaryColor.copy(alpha = 0.9f), ThemeSecondaryColor.copy(alpha = 0.9f))
        )

        isPrimary -> Brush.linearGradient(
            listOf(ThemePrimaryColor.copy(alpha = 0.7f), ThemeSecondaryColor.copy(alpha = 0.7f))
        )

        isFocused -> Brush.linearGradient(
            listOf(Color.Gray.copy(alpha = 0.5f), Color.DarkGray.copy(alpha = 0.5f))
        )

        else -> Brush.linearGradient(
            listOf(Color.Gray.copy(alpha = 0.3f), Color.DarkGray.copy(alpha = 0.3f))
        )
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .scale(animatedFocusedScale(isFocused))
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = Color.White.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .focusable()
            .onFocusChanged { isFocused = it.isFocused },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium
        )
    }
}
