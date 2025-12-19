package jr.brian.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as GraphicsColor
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import jr.brian.home.ui.navigation.appSearchScreen
import jr.brian.home.ui.navigation.backButtonShortcutScreen
import jr.brian.home.ui.navigation.customThemeScreen
import jr.brian.home.ui.navigation.faqScreen
import jr.brian.home.ui.navigation.launcherScreen
import jr.brian.home.ui.navigation.monitorScreen
import jr.brian.home.ui.navigation.settingsScreen
import jr.brian.home.ui.screens.PoweredOffScreen
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.util.Routes
import jr.brian.home.viewmodels.MainViewModel
import jr.brian.home.viewmodels.PowerViewModel
import jr.brian.home.viewmodels.WidgetViewModel

@Composable
fun MainContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = viewModel()
    val widgetViewModel: WidgetViewModel = viewModel()
    val powerViewModel: PowerViewModel = viewModel()
    val wallpaperManager = LocalWallpaperManager.current
    val isPoweredOff by powerViewModel.isPoweredOff.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        mainViewModel.loadAllApps(context)
        widgetViewModel.initializeWidgetHost(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                powerViewModel.powerOn()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_PACKAGE_ADDED,
                    Intent.ACTION_PACKAGE_REMOVED,
                    Intent.ACTION_PACKAGE_CHANGED -> {
                        mainViewModel.loadAllApps(context!!)
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }

        context.registerReceiver(receiver, filter)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (wallpaperManager.isTransparent()) {
                GraphicsColor.Transparent
            } else {
                MaterialTheme.colorScheme.background
            }
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.LAUNCHER
            ) {
                launcherScreen(
                    navController = navController,
                    context = context,
                    mainViewModel = mainViewModel,
                    widgetViewModel = widgetViewModel,
                    powerViewModel = powerViewModel
                )

                settingsScreen(
                    navController = navController,
                    context = context,
                    mainViewModel = mainViewModel
                )

                faqScreen(navController = navController)

                customThemeScreen(navController = navController)

                appSearchScreen(
                    navController = navController,
                    mainViewModel = mainViewModel
                )

                backButtonShortcutScreen(
                    navController = navController,
                    mainViewModel = mainViewModel
                )

                monitorScreen(navController = navController)
            }
        }

        AnimatedVisibility(
            visible = isPoweredOff,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PoweredOffScreen(
                onPowerOn = {
                    powerViewModel.powerOn()
                }
            )
        }
    }
}