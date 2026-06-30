package jr.brian.home.canvas.model

import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.app.Folder
import jr.brian.home.model.rom.PinnedRomInfo

/**
 * A [CanvasItem] paired with the live entity it references.
 *
 * The entity is nullable so a layout that references a deleted app / folder /
 * ROM still renders something (the UI decides whether to show a "missing" tile
 * or skip it) without forcing the layout to be mutated on every cold start.
 */
sealed class ResolvedCanvasItem {
    abstract val raw: CanvasItem

    data class App(
        override val raw: CanvasItem.AppItem,
        val info: AppInfo?
    ) : ResolvedCanvasItem()

    data class Folder(
        override val raw: CanvasItem.FolderItem,
        val folder: jr.brian.home.model.app.Folder?
    ) : ResolvedCanvasItem()

    data class Rom(
        override val raw: CanvasItem.RomItem,
        val info: PinnedRomInfo?
    ) : ResolvedCanvasItem()

    data class Widget(
        override val raw: CanvasItem.WidgetItem
    ) : ResolvedCanvasItem()

    data class RssLauncher(
        override val raw: CanvasItem.RssLauncherItem
    ) : ResolvedCanvasItem()

    /**
     * RSS music-widget tile. Display + transport state is read live from
     * [jr.brian.home.viewmodels.RssViewModel] at render time, so the resolved
     * data carries no extra fields beyond the raw item.
     */
    data class RssMusic(
        override val raw: CanvasItem.RssMusicItem
    ) : ResolvedCanvasItem()

    /**
     * ES-DE art tile. Display-only and event-driven — the renderer reads the
     * current path live from
     * [jr.brian.home.esde.util.LocalEsdeWallpaperState], so the resolved
     * data carries no extra fields beyond the raw item.
     */
    data class EsdeArt(
        override val raw: CanvasItem.EsdeArtItem
    ) : ResolvedCanvasItem()
}
