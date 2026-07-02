package jr.brian.home.canvas.ui

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import jr.brian.home.R
import jr.brian.home.data.WidgetProviderRepository
import jr.brian.home.model.widget.WidgetCategory
import jr.brian.home.model.widget.WidgetProviderInfo
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.cardGradient
import jr.brian.home.ui.components.dialog.DimmedBottomSheet
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor

/**
 * Lists every widget provider installed on the device (via [WidgetProviderRepository]).
 * On selection, allocates an `appWidgetId` via [appWidgetHost] and binds it through
 * [AppWidgetManager.bindAppWidgetIdIfAllowed]; falls back to launching
 * [AppWidgetManager.ACTION_APPWIDGET_BIND] for permission when the silent bind fails.
 *
 * Calls [onWidgetPicked] with `(widgetId, providerInfo)` once the widget is bound.
 * If the user cancels or binding fails, the allocated id is freed before dismiss.
 */
@Composable
fun CanvasWidgetPickerDialog(
    appWidgetHost: AppWidgetHost?,
    onWidgetPicked: (widgetId: Int, providerInfo: AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { WidgetProviderRepository(context) }
    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }

    var categories by remember { mutableStateOf<List<WidgetCategory>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var pendingProvider by remember { mutableStateOf<WidgetProviderInfo?>(null) }
    var pendingId by remember { mutableStateOf(-1) }

    LaunchedEffect(Unit) {
        categories = repository.getWidgetCategories()
        isLoading = false
    }

    val bindLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val provider = pendingProvider
        val id = pendingId
        pendingProvider = null
        pendingId = -1
        if (provider == null || id == -1) return@rememberLauncherForActivityResult
        if (result.resultCode == Activity.RESULT_OK) {
            onWidgetPicked(id, provider.providerInfo)
            onDismiss()
        } else {
            appWidgetHost?.deleteAppWidgetId(id)
        }
    }

    DimmedBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
                Text(
                    text = stringResource(R.string.canvas_widget_picker_title),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.canvas_widget_picker_loading),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    }
                    categories.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.canvas_widget_picker_empty),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 520.dp),
                            contentPadding = PaddingValues(4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = categories.flatMap { it.widgets },
                                key = { it.providerInfo.provider.flattenToString() + ":" + it.label }
                            ) { provider ->
                                WidgetPickerRow(
                                    provider = provider,
                                    onClick = {
                                        val host = appWidgetHost
                                        if (host == null) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.canvas_widget_host_unavailable),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@WidgetPickerRow
                                        }
                                        val id = host.allocateAppWidgetId()
                                        if (id == -1) return@WidgetPickerRow
                                        val canBind = runCatching {
                                            appWidgetManager.bindAppWidgetIdIfAllowed(
                                                id,
                                                provider.providerInfo.provider
                                            )
                                        }.getOrElse { false }
                                        if (canBind) {
                                            onWidgetPicked(id, provider.providerInfo)
                                            onDismiss()
                                        } else {
                                            pendingProvider = provider
                                            pendingId = id
                                            val intent =
                                                Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                                                    putExtra(
                                                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                                                        id
                                                    )
                                                    putExtra(
                                                        AppWidgetManager.EXTRA_APPWIDGET_PROVIDER,
                                                        provider.providerInfo.provider
                                                    )
                                                }
                                            bindLauncher.launch(intent)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun WidgetPickerRow(
    provider: WidgetProviderInfo,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient(isFocused = isFocused),
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = if (isFocused) 3.dp else 2.dp,
                brush = borderBrush(isFocused = isFocused),
                shape = RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .focusable()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(OledCardColor.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            val painter = provider.previewImage?.let { rememberAsyncImagePainter(it) }
            if (painter != null) {
                androidx.compose.foundation.Image(
                    painter = painter,
                    contentDescription = provider.label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = provider.label,
                    tint = ThemePrimaryColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = provider.label,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            if (provider.appName.isNotBlank()) {
                Text(
                    text = provider.appName,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
    }
}
