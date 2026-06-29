package jr.brian.home.canvas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import jr.brian.home.R
import jr.brian.home.canvas.model.EsdeArtType
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.cardGradient
import jr.brian.home.ui.components.dialog.DimmedDialog
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor

/**
 * Single-step picker for the ES-DE Display tile. Asks **only** Logo vs
 * Background — the tile binds to the live wallpaper state via
 * [jr.brian.home.esde.util.LocalEsdeWallpaperState], so there is no system
 * or game to choose.
 *
 * Tapping Add fires [onConfirm] and then [onDismiss], so the dialog always
 * closes on a successful selection (the bug-class the previous design hit
 * was a stuck second step waiting on a non-empty systems list).
 */
@Composable
fun CanvasEsdeArtPickerDialog(
    onConfirm: (artType: EsdeArtType) -> Unit,
    onDismiss: () -> Unit
) {
    var artType by remember { mutableStateOf(EsdeArtType.LOGO) }

    DimmedDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            color = OledCardColor,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(
                    width = 1.dp,
                    brush = borderBrush(isFocused = true),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.canvas_esde_picker_title),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                ArtTypeChips(
                    selected = artType,
                    onSelected = { artType = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    HeaderActionButton(
                        label = stringResource(R.string.canvas_picker_cancel),
                        isPrimary = false,
                        enabled = true,
                        onClick = onDismiss
                    )
                    HeaderActionButton(
                        label = stringResource(R.string.canvas_esde_picker_confirm),
                        isPrimary = true,
                        enabled = true,
                        onClick = {
                            onConfirm(artType)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtTypeChips(
    selected: EsdeArtType,
    onSelected: (EsdeArtType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.canvas_esde_picker_art_type_label),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ArtTypeChip(
                label = stringResource(R.string.canvas_esde_picker_art_type_logo),
                isSelected = selected == EsdeArtType.LOGO,
                onClick = { onSelected(EsdeArtType.LOGO) },
                modifier = Modifier.weight(1f)
            )
            ArtTypeChip(
                label = stringResource(R.string.canvas_esde_picker_art_type_background),
                isSelected = selected == EsdeArtType.BACKGROUND,
                onClick = { onSelected(EsdeArtType.BACKGROUND) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ArtTypeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient(isFocused = isFocused || isSelected),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isSelected || isFocused) 3.dp else 2.dp,
                brush = borderBrush(isFocused = isSelected || isFocused),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .focusable()
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) ThemePrimaryColor else Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HeaderActionButton(
    label: String,
    isPrimary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val baseAlpha = if (enabled) 1f else 0.4f
    Box(
        modifier = Modifier
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                color = if (isPrimary) ThemePrimaryColor.copy(alpha = 0.85f * baseAlpha)
                else Color.White.copy(alpha = 0.08f * baseAlpha),
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White.copy(alpha = 0.9f)
                else Color.White.copy(alpha = 0.3f * baseAlpha),
                shape = RoundedCornerShape(10.dp)
            )
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onClick() }
            .focusable(enabled = enabled)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = baseAlpha),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
