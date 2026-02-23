package jr.brian.home.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.ui.animations.animatedRotation
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.cardGradient
import jr.brian.home.ui.theme.managers.IconShape
import jr.brian.home.ui.theme.managers.LocalIconShapeManager
import jr.brian.home.ui.util.rememberConditionalFocus

@Composable
fun IconShapeToggleItem(
    focusRequester: FocusRequester? = null,
    isExpanded: Boolean = false
) {
    val iconShapeManager = LocalIconShapeManager.current
    val currentShape = iconShapeManager.iconShape
    var isFocused by remember { mutableStateOf(false) }
    val mainCardFocusRequester = rememberConditionalFocus(!isExpanded)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester ?: mainCardFocusRequester)
                    .onFocusChanged {
                        isFocused = it.isFocused
                    }
                    .background(
                        brush = cardGradient(isFocused),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .border(
                        width = if (isFocused) 2.dp else 0.dp,
                        brush = borderBrush(isFocused),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        iconShapeManager.toggleIconShape()
                    }
                    .focusable()
                    .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.RoundedCorner,
                        contentDescription = stringResource(R.string.settings_icon_shape_title),
                        modifier =
                            Modifier
                                .size(32.dp)
                                .rotate(animatedRotation(isFocused)),
                        tint = Color.White,
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    Column {
                        Text(
                            text = stringResource(id = R.string.settings_icon_shape_title),
                            color = Color.White,
                            fontSize = if (isFocused) 18.sp else 16.sp,
                            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.settings_icon_shape_description),
                            color = if (isFocused) Color.White.copy(alpha = 0.9f) else Color.Gray,
                            fontSize = 14.sp,
                        )
                    }
                }

                Text(
                    text = when (currentShape) {
                        IconShape.ROUNDED -> stringResource(R.string.settings_icon_shape_rounded)
                        IconShape.SQUARE -> stringResource(R.string.settings_icon_shape_square)
                    },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
