package jr.brian.home.model

import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val category: Int = ApplicationInfo.CATEGORY_UNDEFINED,
    val activityName: String? = null,
)
