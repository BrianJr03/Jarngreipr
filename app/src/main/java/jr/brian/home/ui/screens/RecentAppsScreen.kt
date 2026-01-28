package jr.brian.home.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import jr.brian.home.R
import jr.brian.home.data.AppDisplayPreferenceManager
import jr.brian.home.ui.screens.recentapps.EmptyStateContent
import jr.brian.home.ui.screens.recentapps.LoadingContent
import jr.brian.home.ui.screens.recentapps.PermissionRequiredContent
import jr.brian.home.ui.screens.recentapps.RecentAppsGrid
import jr.brian.home.ui.theme.OledBackgroundColor
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
