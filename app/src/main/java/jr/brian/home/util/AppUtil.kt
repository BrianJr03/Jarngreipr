package jr.brian.home.util

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference

fun launchApp(
    context: Context,
    packageName: String,
    displayPreference: DisplayPreference = DisplayPreference.CURRENT_DISPLAY,
    intent: Intent? = null
) {
    try {
        val intent = intent ?: context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            when (displayPreference) {
                DisplayPreference.PRIMARY_DISPLAY -> {
                    val options = ActivityOptions.makeBasic()
                    options.launchDisplayId = 0
                    context.startActivity(intent, options.toBundle())
                }

                DisplayPreference.CURRENT_DISPLAY -> {
                    context.startActivity(intent)
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Launches an app on the opposite display from the current preference.
 * If current preference is PRIMARY_DISPLAY, launches on CURRENT_DISPLAY and vice versa.
 */
fun launchAppOnOppositeDisplay(
    context: Context,
    packageName: String,
    currentPreference: DisplayPreference
) {
    val oppositePreference = if (currentPreference == DisplayPreference.PRIMARY_DISPLAY) {
        DisplayPreference.CURRENT_DISPLAY
    } else {
        DisplayPreference.PRIMARY_DISPLAY
    }
    launchApp(context, packageName, oppositePreference)
}

fun openAppInfo(
    context: Context,
    packageName: String
) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:$packageName".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}