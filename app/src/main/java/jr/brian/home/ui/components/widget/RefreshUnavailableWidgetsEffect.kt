package jr.brian.home.ui.components.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import jr.brian.home.viewmodels.WidgetViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PROVIDER_SETTLE_MS = 1_500L

/** The system may still be rebinding a provider when the package broadcast lands, so check twice. */
@Composable
fun RefreshUnavailableWidgetsEffect(viewModel: WidgetViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    DisposableEffect(context, lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshUnavailableWidgets()
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                scope.launch {
                    viewModel.refreshUnavailableWidgets()
                    delay(PROVIDER_SETTLE_MS)
                    viewModel.refreshUnavailableWidgets()
                }
            }
        }
        val packageFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        val storageFilter = IntentFilter(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE)

        lifecycleOwner.lifecycle.addObserver(observer)
        listOf(packageFilter, storageFilter).forEach { filter ->
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            context.unregisterReceiver(receiver)
        }
    }
}
