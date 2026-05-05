package jr.brian.home.ui.screens.rss

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.ui.colors.animatedGradientBorder
import jr.brian.home.ui.theme.ThemePrimaryColor

@Composable
internal fun NowPlayingBubble(
    title: String,
    isBuffering: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(ThemePrimaryColor.copy(alpha = 0.12f))
            .then(
                if (isBuffering) Modifier.animatedGradientBorder(
                    shape = RoundedCornerShape(20.dp),
                    borderWidth = 1.dp,
                    durationMs = 2500
                ) else Modifier.border(
                    border = BorderStroke(
                        width = 1.dp,
                        color = ThemePrimaryColor
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.SkipPrevious,
            contentDescription = stringResource(R.string.rss_tab_previous_cd),
            tint = ThemePrimaryColor,
            modifier = Modifier
                .size(18.dp)
                .clickable(onClick = onPrevious)
        )
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    color = ThemePrimaryColor,
                    strokeWidth = 1.5.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = null,
                    tint = ThemePrimaryColor,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = title,
                    color = ThemePrimaryColor,
                    fontSize = 11.sp,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
        Icon(
            imageVector = Icons.Default.SkipNext,
            contentDescription = stringResource(R.string.rss_tab_next_cd),
            tint = ThemePrimaryColor,
            modifier = Modifier
                .size(18.dp)
                .clickable(onClick = onNext)
        )
    }
}
