package jr.brian.jarnfe.wallpaper

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import jr.brian.frontend.look.JARN_LIVE_WALLPAPER_OFF
import jr.brian.jarnfe.R

/**
 * A live wallpaper. [id] is saved and backed up, so it must never change or equal
 * [JARN_LIVE_WALLPAPER_OFF]. [content] must apply the given modifier to what it draws.
 */
@Immutable
internal class JarnLiveWallpaper(
    val id: String,
    @param:StringRes val nameRes: Int,
    val content: @Composable (Modifier) -> Unit,
)

/** Add a wallpaper to [All] to ship it. The picker stays hidden while the list is empty. */
internal object JarnLiveWallpapers {
    val All: List<JarnLiveWallpaper> = listOf(
        JarnLiveWallpaper("starstave", R.string.jarnfe_wallpaper_starstave) { modifier ->
            StarstaveWallpaper(modifier)
        },
        JarnLiveWallpaper("ironweave", R.string.jarnfe_wallpaper_ironweave) { modifier ->
            IronweaveWallpaper(modifier)
        },
        JarnLiveWallpaper("hearthrise", R.string.jarnfe_wallpaper_hearthrise) { modifier ->
            HearthriseWallpaper(modifier)
        },
        JarnLiveWallpaper("tidemark", R.string.jarnfe_wallpaper_tidemark) { modifier ->
            TidemarkWallpaper(modifier)
        },
    )

    fun fromId(id: String?, catalog: List<JarnLiveWallpaper> = All): JarnLiveWallpaper? =
        catalog.firstOrNull { it.id == id }
}

/** An unsaved choice shown while the picker has focus. [JARN_LIVE_WALLPAPER_OFF] previews none. */
internal object JarnLiveWallpaperPreview {
    var id by mutableStateOf<String?>(null)
}

internal sealed interface JarnBackdrop {
    data class Live(val wallpaper: JarnLiveWallpaper) : JarnBackdrop
    data class Image(val uri: String) : JarnBackdrop
    data object Theme : JarnBackdrop
}

/** Live wins, then the picked image, then the theme. An unknown live id shows what's beneath it. */
internal fun resolveJarnBackdrop(
    showWallpaper: Boolean,
    savedLiveId: String?,
    previewLiveId: String?,
    imageUri: String?,
    catalog: List<JarnLiveWallpaper> = JarnLiveWallpapers.All,
): JarnBackdrop {
    if (!showWallpaper) return JarnBackdrop.Theme
    val live = JarnLiveWallpapers.fromId(previewLiveId ?: savedLiveId, catalog)
    return when {
        live != null -> JarnBackdrop.Live(live)
        imageUri != null -> JarnBackdrop.Image(imageUri)
        else -> JarnBackdrop.Theme
    }
}
