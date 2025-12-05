package jr.brian.home.ui.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import jr.brian.home.R
import jr.brian.home.data.PageType
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun PageTypeSelectionDialog(
    onTypeSelected: (PageType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
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
            modifier = modifier
                .fillMaxWidth(0.9f)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            ThemePrimaryColor.copy(alpha = 0.5f),
                            ThemeSecondaryColor.copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_tab_page_type_dialog_title),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                PageTypeOption(
                    title = stringResource(R.string.home_tab_page_type_apps_tab),
                    description = stringResource(R.string.home_tab_page_type_apps_tab_description),
                    onClick = {
                        onTypeSelected(PageType.APPS_TAB)
                        onDismiss()
                    }
                )

                PageTypeOption(
                    title = stringResource(R.string.home_tab_page_type_apps_and_widgets_tab),
                    description = stringResource(R.string.home_tab_page_type_apps_and_widgets_tab_description),
                    onClick = {
                        onTypeSelected(PageType.APPS_AND_WIDGETS_TAB)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun PageTypeOption(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val backgroundColor = when {
        isFocused -> Brush.horizontalGradient(
            colors = listOf(
                ThemePrimaryColor.copy(alpha = 0.3f),
                ThemeSecondaryColor.copy(alpha = 0.2f)
            )
        )

        else -> Brush.horizontalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.05f),
                Color.White.copy(alpha = 0.05f)
            )
        )
    }

    val borderColor = when {
        isFocused -> ThemePrimaryColor.copy(alpha = 0.6f)
        else -> Color.White.copy(alpha = 0.1f)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(brush = backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 20.dp)
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
    }
}
