package jr.brian.home.model.app

import android.graphics.drawable.Drawable

data class RecentAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val usageTimeMs: Long
)
