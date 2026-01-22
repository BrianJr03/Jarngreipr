package jr.brian.home.ui.screens

import android.content.Intent
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.rememberAsyncImagePainter
import jr.brian.home.R
import jr.brian.home.data.AppDisplayPreferenceManager
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.model.app.RecentAppInfo
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalRecentAppsCacheManager
import jr.brian.home.util.RecentAppsUtil
import jr.brian.home.util.launchApp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentAppsScreen(
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val recentAppsCache = LocalRecentAppsCacheManager.current
    val scope = rememberCoroutineScope()
    val appDisplayPreferenceManager = remember { AppDisplayPreferenceManager(context) }
    var hasPermission by remember { mutableStateOf(RecentAppsUtil.hasUsageStatsPermission(context)) }
    
    var recentApps by remember { 
        mutableStateOf(recentAppsCache.get() ?: emptyList()) 
    }
    
    var isLoading by remember { 
        mutableStateOf(recentAppsCache.get() == null && hasPermission) 
    }

    var isRefreshing by remember { mutableStateOf(false) }

    BackHandler(onBack = onDismiss)

    fun reload(forceRefresh: Boolean = false) {
        scope.launch {
            hasPermission = RecentAppsUtil.hasUsageStatsPermission(context)
            
            if (!hasPermission) {
                recentApps = emptyList()
                isLoading = false
                return@launch
            }
            
            if (!forceRefresh) {
                val cached = recentAppsCache.get()
                if (cached != null) {
                    recentApps = cached
                    isLoading = false
                    return@launch
                }
            }
            
            if (recentApps.isEmpty()) {
                isLoading = true
            } else {
                isRefreshing = true
            }
            
            val apps = RecentAppsUtil.getRecentApps(context)
            recentAppsCache.set(apps)
            recentApps = apps
            isLoading = false
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    DisposableEffect(Unit) {
        onDispose { 
            recentAppsCache.clear()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Soft reload - will use cache if still valid
                reload()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = OledBackgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                            text = stringResource(R.string.recent_apps_screen_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.update_close_description),
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { reload(forceRefresh = true) },
                        enabled = !isLoading && !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.recent_apps_refresh),
                                tint = if (isLoading) Color.White.copy(alpha = 0.4f) else Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OledBackgroundColor,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OledBackgroundColor)
                .padding(innerPadding)
                .systemBarsPadding()
        ) {
            when {
                isLoading -> LoadingContent()
                !hasPermission -> PermissionRequiredContent(
                    onGrantPermission = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                )

                recentApps.isEmpty() -> EmptyStateContent()
                else -> RecentAppsGrid(
                    recentApps = recentApps,
                    appDisplayPreferenceManager = appDisplayPreferenceManager,
                    onAppClick = { app ->
                        val pref =
                            appDisplayPreferenceManager.getAppDisplayPreference(app.packageName)
                        launchApp(context, app.packageName, pref)
                    }
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = ThemePrimaryColor,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.recent_apps_loading),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun RecentAppsGrid(
    recentApps: List<RecentAppInfo>,
    appDisplayPreferenceManager: AppDisplayPreferenceManager,
    onAppClick: (RecentAppInfo) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 170.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(recentApps, key = { _, app -> app.packageName }) { index, app ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { visible = true }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(300, delayMillis = index * 30)) +
                        slideInVertically(tween(300, delayMillis = index * 30)) { it / 4 }
            ) {
                RecentAppCard(
                    app = app,
                    appDisplayPreferenceManager = appDisplayPreferenceManager,
                    onClick = { onAppClick(app) }
                )
            }
        }
    }
}

@Composable
private fun RecentAppCard(
    app: RecentAppInfo,
    appDisplayPreferenceManager: AppDisplayPreferenceManager,
    onClick: () -> Unit
) {
    var currentPreference by remember {
        mutableStateOf(appDisplayPreferenceManager.getAppDisplayPreference(app.packageName))
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(100),
        label = "cardScale"
    )

    val cardGradient = Brush.verticalGradient(
        colors = listOf(
            OledCardColor.copy(alpha = 0.9f),
            OledCardColor.copy(alpha = 0.6f)
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        tonalElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .background(cardGradient)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = app.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                AppIconContainer(
                    icon = app.icon,
                    label = app.label
                )

                Spacer(modifier = Modifier.height(8.dp))

                UsageTimeBadge(usageTimeMs = app.usageTimeMs)

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DisplayButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.KeyboardArrowUp,
                        label = stringResource(R.string.recent_apps_display_top),
                        isSelected = currentPreference == DisplayPreference.PRIMARY_DISPLAY,
                        selectedColor = ThemePrimaryColor,
                        onClick = {
                            appDisplayPreferenceManager.setAppDisplayPreference(
                                app.packageName,
                                DisplayPreference.PRIMARY_DISPLAY
                            )
                            currentPreference = DisplayPreference.PRIMARY_DISPLAY
                        }
                    )

                    DisplayButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.KeyboardArrowDown,
                        label = stringResource(R.string.recent_apps_display_bottom),
                        isSelected = currentPreference == DisplayPreference.CURRENT_DISPLAY,
                        selectedColor = ThemeSecondaryColor,
                        onClick = {
                            appDisplayPreferenceManager.setAppDisplayPreference(
                                app.packageName,
                                DisplayPreference.CURRENT_DISPLAY
                            )
                            currentPreference = DisplayPreference.CURRENT_DISPLAY
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DisplayButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    val bgColor =
        if (isSelected) selectedColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
    val contentColor = if (isSelected) selectedColor else Color.White.copy(alpha = 0.7f)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}

@Composable
private fun AppIconContainer(
    icon: Drawable,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(88.dp),
        shape = RoundedCornerShape(22.dp),
        color = ThemePrimaryColor.copy(alpha = 0.12f),
        tonalElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = rememberAsyncImagePainter(model = icon),
                contentDescription = stringResource(
                    R.string.recent_apps_icon_description,
                    label
                ),
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

@Composable
private fun UsageTimeBadge(
    usageTimeMs: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = ThemeSecondaryColor.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = ThemeSecondaryColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = RecentAppsUtil.formatUsageDuration(usageTimeMs),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun EmptyStateContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                color = ThemePrimaryColor.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = ThemePrimaryColor,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.recent_apps_empty_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.recent_apps_empty_description),
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun PermissionRequiredContent(
    onGrantPermission: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = OledCardColor.copy(alpha = 0.8f),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(28.dp)
            ) {
                Surface(
                    color = ThemePrimaryColor.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = ThemePrimaryColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.recent_apps_permission_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.recent_apps_permission_description),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onGrantPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = ThemePrimaryColor),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = stringResource(R.string.recent_apps_permission_button),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
