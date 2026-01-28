package jr.brian.home.ui.components.widgetpicker

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import jr.brian.home.R
import jr.brian.home.data.WidgetProviderRepository
import jr.brian.home.model.widget.WidgetCategory
import jr.brian.home.model.widget.WidgetProviderInfo
import jr.brian.home.model.widget.WidgetWithCategory
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun WidgetCategoryList(
    categories: List<WidgetCategory>,
    repository: WidgetProviderRepository,
    showKeyboard: Boolean,
    onWidgetSelected: (WidgetProviderInfo) -> Unit
) {
    val allWidgets = remember(categories) {
        categories.flatMap { category ->
            category.widgets.map { widget ->
                WidgetWithCategory(
                    widget = widget,
                    categoryName = category.appName,
                    categoryIcon = category.appIcon
                )
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(if (showKeyboard) 1 else 2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = allWidgets,
            key = { "${it.category.packageName}_${it.widget.providerInfo.provider.className}" }
        ) { widgetWithCategory ->
            WidgetPreviewCard(
                widget = widgetWithCategory.widget,
                repository = repository,
                onClick = { onWidgetSelected(widgetWithCategory.widget) }
            )
        }
    }
}

@Composable
fun WidgetPreviewCard(
    widget: WidgetProviderInfo,
    repository: WidgetProviderRepository,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val sizeInfo = remember(widget) { repository.getWidgetSizeInfo(widget.providerInfo) }

    val cardGradient = Brush.linearGradient(
        colors = if (isFocused) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.3f),
                ThemeSecondaryColor.copy(alpha = 0.3f)
            )
        } else {
            listOf(
                OledCardColor,
                OledCardColor
            )
        }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                brush = if (isFocused) {
                    borderBrush(
                        isFocused = true,
                        colors = listOf(
                            ThemePrimaryColor,
                            ThemeSecondaryColor
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            ThemePrimaryColor.copy(alpha = 0.2f),
                            ThemeSecondaryColor.copy(alpha = 0.2f)
                        )
                    )
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .focusable()
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                widget.previewImage?.let { preview ->
                    Image(
                        bitmap = preview.toBitmap(400, 400).asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                } ?: run {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = widget.label,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = widget.appName,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

             Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        R.string.widget_picker_size_info,
                        sizeInfo.targetWidthCells,
                        sizeInfo.targetHeightCells
                    ),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )

                if (sizeInfo.isResizable) {
                    Text(
                        text = " | ",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 15.sp
                    )
                    Text(
                        text = stringResource(R.string.widget_picker_resizable),
                        color = ThemePrimaryColor.copy(alpha = 0.9f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (widget.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = widget.description,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
