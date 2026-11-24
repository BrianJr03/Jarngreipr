package jr.brian.home.data.config

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val FILE_SCHEME = "file://"
private const val CONTENT_SCHEME = "content://"

/**
 * The v52 backup areas, kept out of [ImportExportManager] so it doesn't grow. Export adds them to a
 * built config, and restore repairs the canvas before it is applied and restores the rest after.
 */
@Singleton
class SettingsBackupExtras @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appearance: AppearanceBackup,
    private val rssFeeds: RssFeedBackup,
    private val jingles: JingleBackup,
) {
    private val widgetManager by lazy { runCatching { AppWidgetManager.getInstance(context) }.getOrNull() }

    suspend fun addTo(config: JarngreiprConfig): JarngreiprConfig = config.copy(
        ui = config.ui.copy(appearance = appearance.export()),
        page = config.page.copy(
            widgetProviders = canvasWidgetProviders(config.page.canvasLayouts.values, ::providerOf),
        ),
        feature = config.feature.copy(
            rssFeeds = rssFeeds.export(),
            jingles = config.feature.jingles.copy(enabled = jingles.enabled, repos = jingles.repos),
        ),
    )

    fun prepareForRestore(config: JarngreiprConfig): JarngreiprConfig = config.copy(
        page = config.page.copy(
            canvasLayouts = repairRestoredCanvas(
                layouts = config.page.canvasLayouts,
                backupProviders = config.page.widgetProviders,
                providerOf = ::providerOf,
                canOpenPhoto = ::canOpenPhoto,
            ),
        ),
    )

    suspend fun restore(config: JarngreiprConfig) {
        appearance.restore(config.ui.appearance)
        rssFeeds.restore(config.feature.rssFeeds)
        jingles.restore(config.feature.jingles.enabled, config.feature.jingles.repos)
    }

    private fun providerOf(widgetId: Int): String? =
        runCatching { widgetManager?.getAppWidgetInfo(widgetId)?.provider?.flattenToString() }.getOrNull()

    /** Our own copied photos, or a picture we still hold read access to. */
    private fun canOpenPhoto(uri: String): Boolean = runCatching {
        when {
            uri.startsWith(FILE_SCHEME) -> {
                val file = File(uri.removePrefix(FILE_SCHEME)).canonicalFile
                val root = context.filesDir.canonicalPath + File.separator
                file.path.startsWith(root) && file.isFile
            }
            uri.startsWith(CONTENT_SCHEME) -> {
                val target = uri.toUri()
                context.contentResolver.persistedUriPermissions.any { it.uri == target && it.isReadPermission }
            }
            else -> false
        }
    }.getOrDefault(false)
}
