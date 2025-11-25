package jr.brian.home.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import jr.brian.home.R
import jr.brian.home.model.WidgetInfo
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.viewmodels.WidgetViewModel

@Composable
fun WidgetResizeScreen(
    widgetInfo: WidgetInfo,
    pageIndex: Int,
    viewModel: WidgetViewModel,
    onNavigateBack: () -> Unit
) {
    val gridSettingsManager = jr.brian.home.ui.theme.LocalGridSettingsManager.current
    val maxColumns = gridSettingsManager.columnCount
    val maxRows = 10

    var currentWidth by remember { mutableStateOf(widgetInfo.width) }
    var currentHeight by remember { mutableStateOf(widgetInfo.height) }

    val widgetHeightDp = remember(currentHeight) {
        val cellHeight = 80.dp
        (currentHeight * cellHeight.value).dp.coerceAtLeast(80.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E2E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.widget_resize_mode_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.widget_resize_dimensions,
                            currentWidth,
                            currentHeight
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = ThemePrimaryColor
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.widget_resize_mode_instructions),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(widgetHeightDp.coerceAtMost(400.dp))
                        .background(
                            color = Color(0xFF2A2A2A),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = 3.dp,
                            color = ThemePrimaryColor,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    key("resize_static_${widgetInfo.widgetId}") {
                        AndroidView(
                            factory = { ctx ->
                                val host = viewModel.getAppWidgetHost()
                                val widgetView = host?.createView(
                                    ctx,
                                    widgetInfo.widgetId,
                                    widgetInfo.providerInfo
                                )
                                widgetView ?: ComposeView(ctx)
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.3f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ThemePrimaryColor.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInFull,
                                contentDescription = null,
                                tint = ThemePrimaryColor,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$currentWidth × $currentHeight",
                                style = MaterialTheme.typography.headlineMedium,
                                color = ThemePrimaryColor
                            )
                            Text(
                                text = "cells",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2A2A2A)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Width: $currentWidth",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (currentWidth > 1) currentWidth--
                                },
                                enabled = currentWidth > 1,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ThemePrimaryColor
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.width(48.dp)
                            ) {
                                Text("-", style = MaterialTheme.typography.titleLarge)
                            }
                            Button(
                                onClick = {
                                    if (currentWidth < maxColumns) currentWidth++
                                },
                                enabled = currentWidth < maxColumns,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ThemePrimaryColor
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.width(48.dp)
                            ) {
                                Text("+", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2A2A2A)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Height: $currentHeight",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (currentHeight > 1) currentHeight--
                                },
                                enabled = currentHeight > 1,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ThemePrimaryColor
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.width(48.dp)
                            ) {
                                Text("-", style = MaterialTheme.typography.titleLarge)
                            }
                            Button(
                                onClick = {
                                    if (currentHeight < maxRows) currentHeight++
                                },
                                enabled = currentHeight < maxRows,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ThemePrimaryColor
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.width(48.dp)
                            ) {
                                Text("+", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.dialog_cancel),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Button(
                    onClick = {
                        viewModel.updateWidgetSize(
                            widgetId = widgetInfo.widgetId,
                            pageIndex = pageIndex,
                            newWidth = currentWidth,
                            newHeight = currentHeight
                        )
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemePrimaryColor
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.widget_resize_done),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
