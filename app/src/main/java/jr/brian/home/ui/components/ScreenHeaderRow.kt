package jr.brian.home.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import jr.brian.home.ui.animations.animatedRotation
import jr.brian.home.ui.extensions.blockHorizontalNavigation
import jr.brian.home.ui.extensions.handleFullNavigation
import jr.brian.home.ui.extensions.handleRightNavigation
import jr.brian.home.viewmodels.PowerViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScreenHeaderRow(
    totalPages: Int,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    leadingIconContentDescription: String? = null,
    onLeadingIconClick: () -> Unit = {},
    trailingIcon: ImageVector? = null,
    trailingIconContentDescription: String? = null,
    onTrailingIconClick: () -> Unit = {},
    leadingIconFocusRequester: FocusRequester? = null,
    trailingIconFocusRequester: FocusRequester? = null,
    onNavigateToGrid: () -> Unit = {},
    onNavigateFromGrid: () -> Unit = {},
    powerViewModel: PowerViewModel? = null,
    showPowerButton: Boolean = false,
) {
    var isLeadingFocused by remember { mutableStateOf(false) }
    var isPowerFocused by remember { mutableStateOf(false) }
    var isTrailingFocused by remember { mutableStateOf(false) }
    val powerIconFocusRequester = remember { FocusRequester() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .blockHorizontalNavigation()
    ) {
        if (leadingIcon != null) {
            IconBox(
                isFocused = isLeadingFocused,
                modifier = Modifier.handleRightNavigation(onNavigateToGrid),
                focusRequester = leadingIconFocusRequester,
                onFocusChanged = { isLeadingFocused = it },
                onClick = onLeadingIconClick
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = leadingIconContentDescription,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(animatedRotation(isLeadingFocused))
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        PageIndicators(
            totalPages = totalPages,
            pagerState = pagerState,
        )

        Spacer(modifier = Modifier.weight(1f))

        if (showPowerButton && powerViewModel != null) {
            IconBox(
                isFocused = isPowerFocused,
                modifier = Modifier.handleFullNavigation(
                    onNavigateLeft = onNavigateFromGrid,
                    onNavigateRight = {
                        trailingIconFocusRequester?.requestFocus()
                    },
                    onNavigateDown = onNavigateToGrid,
                    onEnterPress = {
                        powerViewModel.togglePower()
                    }
                ),
                focusRequester = powerIconFocusRequester,
                onFocusChanged = { isPowerFocused = it },
                onClick = {
                    powerViewModel.togglePower()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Power Button",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(animatedRotation(isPowerFocused))
                )
            }

            Spacer(modifier = Modifier.size(16.dp))
        }

        if (trailingIcon != null) {
            IconBox(
                isFocused = isTrailingFocused,
                modifier = Modifier.handleFullNavigation(
                    onNavigateLeft = {
                        if (showPowerButton) {
                            powerIconFocusRequester.requestFocus()
                        } else {
                            onNavigateFromGrid()
                        }
                    },
                    onNavigateDown = onNavigateToGrid,
                    onEnterPress = onTrailingIconClick
                ),
                focusRequester = trailingIconFocusRequester,
                onFocusChanged = { isTrailingFocused = it },
                onClick = onTrailingIconClick
            ) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = trailingIconContentDescription,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(animatedRotation(isTrailingFocused))
                )
            }
        }
    }
}