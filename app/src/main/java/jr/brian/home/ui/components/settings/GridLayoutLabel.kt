package jr.brian.home.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun GridLayoutLabel(
    gridCapacity: Int,
    hiddenAppsCount: Int,
    totalAppsCount: Int,
    unlimitedMode: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (unlimitedMode || hiddenAppsCount == 0) {
                    ThemePrimaryColor.copy(alpha = 0.15f)
                } else {
                    ThemeSecondaryColor.copy(alpha = 0.15f)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = if (unlimitedMode || hiddenAppsCount == 0) {
                    ThemePrimaryColor.copy(alpha = 0.5f)
                } else {
                    ThemeSecondaryColor.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = if (unlimitedMode) {
                if (totalAppsCount == 1) {
                    stringResource(
                        R.string.settings_grid_unlimited_mode_singular,
                        totalAppsCount
                    )
                } else {
                    stringResource(
                        R.string.settings_grid_unlimited_mode_plural,
                        totalAppsCount
                    )
                }
            } else if (hiddenAppsCount > 0) {
                if (gridCapacity == 1 && hiddenAppsCount == 1) {
                    stringResource(
                        R.string.settings_grid_apps_hidden_singular,
                        gridCapacity,
                        hiddenAppsCount
                    )
                } else {
                    stringResource(
                        R.string.settings_grid_apps_hidden_plural,
                        gridCapacity,
                        hiddenAppsCount
                    )
                }
            } else {
                if (gridCapacity == 1) {
                    stringResource(
                        R.string.settings_grid_all_apps_visible_singular,
                        gridCapacity
                    )
                } else {
                    stringResource(
                        R.string.settings_grid_all_apps_visible_plural,
                        gridCapacity
                    )
                }
            },
            color = if (unlimitedMode || hiddenAppsCount == 0) {
                ThemePrimaryColor
            } else {
                ThemeSecondaryColor
            },
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
