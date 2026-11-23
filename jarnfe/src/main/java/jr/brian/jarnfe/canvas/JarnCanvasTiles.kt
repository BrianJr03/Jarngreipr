package jr.brian.jarnfe.canvas

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.compose.runtime.Immutable
import jr.brian.home.canvas.model.CanvasItem
import kotlinx.coroutines.flow.Flow

@Immutable
data class JarnCanvasRssItem(
    val id: String,
    val title: String,
    val link: String,
    val hasAudio: Boolean,
)

@Immutable
data class JarnWidgetProvider(
    val key: String,
    val label: String,
    val appName: String,
    val columns: Int,
    val rows: Int,
)

/**
 * [Live] renders. [Unbound] still owns its id, so a provider can be bound to that same id.
 * [Gone] no longer exists on this device, so the tile can only explain itself or be removed.
 */
enum class JarnWidgetHealth { Live, Unbound, Gone }

sealed interface JarnWidgetBind {
    data class Bound(val widgetId: Int, val columns: Int, val rows: Int) : JarnWidgetBind

    /** [isNewId] is false for a rebind, whose id must survive a cancelled permission prompt. */
    data class NeedsPermission(
        val widgetId: Int,
        val columns: Int,
        val rows: Int,
        val isNewId: Boolean,
        val intent: Intent,
    ) : JarnWidgetBind

    data object Failed : JarnWidgetBind
}

sealed interface JarnCanvasNewTile {
    data class App(val packageName: String) : JarnCanvasNewTile
    data class Folder(val name: String, val packageNames: List<String>) : JarnCanvasNewTile
    data class Rom(val key: String) : JarnCanvasNewTile
    data class Widget(val widgetId: Int, val columns: Int, val rows: Int) : JarnCanvasNewTile
    data object Art : JarnCanvasNewTile
    data object LastPlayed : JarnCanvasNewTile
    data object Photo : JarnCanvasNewTile
    data object RssLauncher : JarnCanvasNewTile
    data object RssMusic : JarnCanvasNewTile
}

/** Held while widgets are on screen. Closing hands live updates back to Classic. */
interface JarnWidgetLease : AutoCloseable {
    fun createView(context: Context, widgetId: Int): View?

    override fun close()
}

/** Tile content and actions. Every write keeps the stored layout format exactly as it is. */
interface JarnCanvasTiles {
    val rssItems: Flow<List<JarnCanvasRssItem>>

    /** Returns the tile's id, or null when the page cannot take it. */
    suspend fun addTile(pageIndex: Int, tile: JarnCanvasNewTile): String?

    /** Replaces a tile's settings in place. Its placement in both orientations is kept. */
    fun updateTile(pageIndex: Int, item: CanvasItem)

    /** The only path that releases a tile's widget id, folder, pin, or photo file. */
    suspend fun removeTile(pageIndex: Int, itemId: String)

    suspend fun installedApps(): List<JarnCanvasApp>

    suspend fun pinnedRoms(): List<JarnCanvasRom>

    fun widgetHealth(widgetId: Int): JarnWidgetHealth

    fun acquireWidgetHost(): JarnWidgetLease

    suspend fun widgetProviders(): List<JarnWidgetProvider>

    /** With [existingId], binds that id and never allocates a replacement. */
    fun bindWidget(provider: JarnWidgetProvider, existingId: Int?): JarnWidgetBind

    /** Frees only an id allocated for this attempt. A rebind's id is left alone. */
    fun cancelWidgetBind(bind: JarnWidgetBind.NeedsPermission)

    /** Returns a uri safe to store, or null when the image could not be kept. */
    suspend fun importPhoto(uri: String): String?

    fun toggleMusic()

    fun skipMusic(forward: Boolean)

    /** Starts the first available audio item. Returns false when there is none. */
    suspend fun startMusic(): Boolean

    /** Plays an audio item, or opens its link. Returns false when neither works. */
    suspend fun openRssItem(host: Activity, item: JarnCanvasRssItem): Boolean
}
