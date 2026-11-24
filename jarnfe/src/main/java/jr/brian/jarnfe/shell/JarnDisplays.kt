package jr.brian.jarnfe.shell

import android.content.Context
import android.hardware.display.DisplayManager
import jr.brian.core.model.PRIMARY_DISPLAY_ID

internal fun pickDeckDisplayId(displayIds: List<Int>): Int? =
    displayIds.firstOrNull { it != PRIMARY_DISPLAY_ID }

internal fun resolveDeckDisplayId(context: Context): Int? {
    val displayManager = context.getSystemService(DisplayManager::class.java)
    return pickDeckDisplayId(displayManager.displays.map { it.displayId })
}
