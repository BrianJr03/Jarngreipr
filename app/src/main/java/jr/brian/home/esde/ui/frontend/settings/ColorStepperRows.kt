package jr.brian.home.esde.ui.frontend.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.esde.model.SystemCustomization
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.subtleCardGradient
import jr.brian.home.ui.theme.ThemePrimaryColor
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

data class ColorChannels(
    val hueDegrees: Float,
    val saturation: Float,
    val brightness: Float,
    val alpha: Float
) {
    companion object {
        val DEFAULT_WHITE = ColorChannels(
            hueDegrees = 0f,
            saturation = 0f,
            brightness = 1f,
            alpha = 1f
        )
    }
}

fun argbToChannels(argb: Long?): ColorChannels {
    val effective = when {
        argb == null -> return ColorChannels.DEFAULT_WHITE
        argb == SystemCustomization.TRANSPARENT_ARGB -> return ColorChannels.DEFAULT_WHITE.copy(alpha = 0f)
        else -> argb.toInt()
    }
    val alphaByte = (effective ushr 24) and 0xFF
    val r = ((effective ushr 16) and 0xFF) / 255f
    val g = ((effective ushr 8) and 0xFF) / 255f
    val b = (effective and 0xFF) / 255f

    val cMax = max(r, max(g, b))
    val cMin = min(r, min(g, b))
    val delta = cMax - cMin

    val hue = when {
        delta == 0f -> 0f
        cMax == r -> 60f * (((g - b) / delta) % 6f)
        cMax == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }

    val saturation = if (cMax == 0f) 0f else delta / cMax
    return ColorChannels(
        hueDegrees = hue,
        saturation = saturation,
        brightness = cMax,
        alpha = alphaByte / 255f
    )
}

fun channelsToArgb(channels: ColorChannels): Long {
    val h = channels.hueDegrees.coerceIn(0f, 360f).let { if (it == 360f) 0f else it }
    val s = channels.saturation.coerceIn(0f, 1f)
    val v = channels.brightness.coerceIn(0f, 1f)
    val a = channels.alpha.coerceIn(0f, 1f)

    val c = v * s
    val hp = h / 60f
    val x = c * (1f - abs((hp % 2f) - 1f))
    val (r1, g1, b1) = when (floor(hp).toInt()) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val m = v - c
    val red = ((r1 + m) * 255f + 0.5f).toInt().coerceIn(0, 255)
    val green = ((g1 + m) * 255f + 0.5f).toInt().coerceIn(0, 255)
    val blue = ((b1 + m) * 255f + 0.5f).toInt().coerceIn(0, 255)
    val alpha = (a * 255f + 0.5f).toInt().coerceIn(0, 255)

    val argb = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    return argb.toLong() and 0xFFFFFFFFL
}

@Composable
internal fun ColorPreviewSwatch(channels: ColorChannels) {
    val argb = channelsToArgb(channels)
    val displayColor = Color(argb.toInt())
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 96.dp, height = 56.dp)
                .clip(RoundedCornerShape(10.dp))
                .drawBehind { drawCheckerboard() }
                .background(displayColor)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp)
                )
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCheckerboard() {
    val cell = 8.dp.toPx()
    val cols = (size.width / cell).toInt() + 1
    val rows = (size.height / cell).toInt() + 1
    drawRect(color = Color.White, size = size)
    val gray = Color(0xFFCCCCCC)
    for (row in 0 until rows) {
        for (col in 0 until cols) {
            if ((row + col) % 2 == 0) continue
            drawRect(
                color = gray,
                topLeft = Offset(col * cell, row * cell),
                size = Size(cell, cell),
                style = Fill
            )
        }
    }
}

@Composable
internal fun ColorChannelStepperRow(
    title: String,
    focused: Boolean,
    normalizedValue: Float,
    displayValue: String,
    onStep: (Int) -> Unit
) {
    RegisterForHorizontalSteps(focused)
    StepOnHorizontal(focused) { delta ->
        if (delta != 0) onStep(delta)
    }

    val shape = RoundedCornerShape(16.dp)
    Column(
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
            .padding(16.dp)
    ) {
        StepperHeaderRow(title = title, displayValue = displayValue)
        Spacer(Modifier.height(10.dp))
        ChannelTrack(normalizedValue = normalizedValue)
    }
}

@Composable
private fun StepperHeaderRow(title: String, displayValue: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = displayValue,
            color = ThemePrimaryColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ChannelTrack(normalizedValue: Float) {
    val track = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(track)
            .background(Color.White.copy(alpha = 0.15f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(normalizedValue.coerceIn(0f, 1f))
                .height(8.dp)
                .background(ThemePrimaryColor)
        )
    }
}

@Composable
internal fun ColorPresetRow(
    title: String,
    focused: Boolean,
    isSelected: Boolean,
    isTransparent: Boolean,
    isDefault: Boolean
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
        PresetSwatch(isTransparent = isTransparent, isDefault = isDefault)
        Spacer(Modifier.size(width = 12.dp, height = 0.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (isSelected) {
            Text(
                text = stringResource(R.string.frontend_customize_preset_selected),
                color = ThemePrimaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PresetSwatch(isTransparent: Boolean, isDefault: Boolean) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .then(
                if (isTransparent) {
                    Modifier.drawBehind { drawCheckerboard() }
                } else if (isDefault) {
                    Modifier.background(Color.DarkGray)
                } else {
                    Modifier.background(Color.White)
                }
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.4f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isDefault && !isTransparent) {
            Text(
                text = "×",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
