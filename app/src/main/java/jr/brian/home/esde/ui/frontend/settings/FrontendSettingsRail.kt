package jr.brian.home.esde.ui.frontend.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.subtleCardGradient
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.ThemePrimaryColor

internal val RailWidth = 256.dp
private val RailEntryMaxHeight = 88.dp
private val RailEntryCompactThreshold = 72.dp

@Composable
internal fun CategoryRail(
    entries: List<FrontendSettingsCategory>,
    selected: FrontendSettingsCategory,
    railHasFocus: Boolean
) {
    CategoryRail(
        headerText = stringResource(R.string.frontend_settings_title),
        entries = entries,
        selected = selected,
        railHasFocus = railHasFocus
    )
}

@Composable
internal fun <C : RailCategory> CategoryRail(
    headerText: String,
    entries: List<C>,
    selected: C,
    railHasFocus: Boolean
) {
    Column(
        modifier = Modifier
            .width(RailWidth)
            .fillMaxHeight()
            .padding(all = 12.dp)
    ) {
        RailHeader(text = headerText)
        Spacer(Modifier.height(12.dp))
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                entries.forEach { entry ->
                    val isSelected = entry == selected
                    RailEntry(
                        entry = entry,
                        isSelected = isSelected,
                        showFocusRing = railHasFocus && isSelected,
                        compact = true,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = RailEntryMaxHeight)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun RailHeader(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.9f),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun RailEntry(
    entry: RailCategory,
    isSelected: Boolean,
    showFocusRing: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .scale(animatedFocusedScale(showFocusRing))
            .background(brush = subtleCardGradient(isSelected), shape = shape)
            .border(
                width = if (showFocusRing) 2.dp else 0.dp,
                color = if (showFocusRing) ThemePrimaryColor.copy(alpha = 0.6f) else Color.Transparent,
                shape = shape
            )
            .clip(shape)
            .revealWhenFocused(showFocusRing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(if (isSelected) ThemeAccentColor else Color.Transparent)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = if (compact) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = null,
                tint = if (isSelected) ThemePrimaryColor else Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(if (compact) 20.dp else 24.dp)
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(entry.titleRes),
                    color = Color.White,
                    fontSize = if (compact) 14.sp else 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (!compact) {
                    Text(
                        text = stringResource(entry.summaryRes),
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.sp,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
