package jr.brian.home.ui.util

import android.text.Html

internal val pubDateFormats = listOf(
    "EEE, dd MMM yyyy HH:mm:ss Z",
    "EEE, dd MMM yyyy HH:mm:ss z",
    "yyyy-MM-dd'T'HH:mm:ssZ",
    "yyyy-MM-dd'T'HH:mm:ss'Z'",
    "yyyy-MM-dd'T'HH:mm:ssz",
    "dd MMM yyyy HH:mm:ss Z"
)

internal fun parsePubDateMillis(raw: String): Long {
    if (raw.isBlank()) return 0L
    for (fmt in pubDateFormats) {
        runCatching {
            val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.ENGLISH)
            sdf.isLenient = false
            return sdf.parse(raw.trim())!!.time
        }
    }
    return 0L
}

internal fun formatPubDate(raw: String, useDMY: Boolean, use24Hour: Boolean): String {
    if (raw.isBlank()) return ""
    val datePattern = if (useDMY) "d/M/yyyy" else "M/d/yyyy"
    val timePattern = if (use24Hour) "HH:mm" else "h:mm a"
    val output =
        java.text.SimpleDateFormat("$datePattern @ $timePattern", java.util.Locale.getDefault())
    for (fmt in pubDateFormats) {
        runCatching {
            val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.ENGLISH)
            sdf.isLenient = false
            return output.format(sdf.parse(raw.trim())!!)
        }
    }
    return raw.take(30).trimEnd()
}

internal fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0L)
    val hours = totalSec / 3600
    val min = (totalSec % 3600) / 60
    val sec = totalSec % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, min, sec)
    else "%d:%02d".format(min, sec)
}

internal fun stripHtml(html: String): String {
    if (html.isBlank()) return ""
    return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString()
}
