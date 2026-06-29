package jr.brian.home.esde.util

import jr.brian.home.esde.model.GameImageType
import jr.brian.home.esde.model.GameInfo

/**
 * Map a [GameImageType] to the per-type path on [GameInfo] populated by
 * [RomListParser] (and matched in shape by the inline build in
 * `ESDEViewModel.updateForGame`). The mapping matches `RomListParser`'s
 * resolve functions one-to-one — `Covers` ↔ `FOLDER_COVERS` ↔ `artworkPath`,
 * `MixImages` ↔ `FOLDER_MIXIMAGES` ↔ `miximagePath`, etc.
 *
 * Returns null for the non-image types ([GameImageType.None] /
 * [GameImageType.All] / [GameImageType.Description]) — those aren't a single
 * file the canvas can render.
 */
fun GameInfo.imagePathFor(type: GameImageType): String? = when (type) {
    GameImageType.Marquee -> marqueeImagePath
    GameImageType.Fanart -> fanartPath
    GameImageType.TitleScreens -> titlescreenPath
    GameImageType.Covers -> artworkPath
    GameImageType.PhysicalMedia -> physicalMediaPath
    GameImageType.Screenshots -> screenshotPath
    GameImageType.MixImages -> miximagePath
    GameImageType.None, GameImageType.All, GameImageType.Description -> null
}
