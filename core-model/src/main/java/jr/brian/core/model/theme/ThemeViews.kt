package jr.brian.core.model.theme

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class ThemeView { Systems, Games }

/**
 * One optional layout per view; a missing view keeps Jarn's own layout.
 * Named fields rather than a map so a view added later is skipped by older builds.
 */
@Serializable
data class ThemeViews(
    val systems: ViewSpec? = null,
    val games: ViewSpec? = null,
) {
    operator fun get(view: ThemeView): ViewSpec? = when (view) {
        ThemeView.Systems -> systems
        ThemeView.Games -> games
    }
}

/**
 * Exactly one [primary] (the browsable list) plus decor [layers], drawn in [ThemeLayer.z] order.
 * [narrow] adjusts the view for a narrow screen (see [forArea]).
 */
@Serializable
data class ViewSpec(
    val primary: ThemePrimary,
    @Serializable(with = ThemeLayerListSerializer::class)
    val layers: List<ThemeLayer> = emptyList(),
    val narrow: ThemeViewPatch? = null,
)

/**
 * A rectangle in screen fractions: 0 to 1 on both axes, origin top left.
 * [originX] / [originY] pick the point of the rectangle that sits at ([x], [y]), so 0.5 / 0.5 centers it.
 */
@Serializable
data class ThemeRect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val originX: Float = 0f,
    val originY: Float = 0f,
)

@Serializable
enum class ThemeAxis {
    @SerialName("horizontal") Horizontal,
    @SerialName("vertical") Vertical,
}

@Serializable
enum class ThemeAlign {
    @SerialName("start") Start,
    @SerialName("center") Center,
    @SerialName("end") End,
}

/**
 * The view's browsable list, drawn by a Jarn component so focus and gamepad input always work.
 * [art] is what each item shows; null keeps the view's default (system art, or game cover).
 */
@Serializable
sealed interface ThemePrimary {
    val box: ThemeRect
    val art: ThemePicture?

    @Serializable
    @SerialName("shelf")
    data class Shelf(
        override val box: ThemeRect,
        override val art: ThemePicture? = null,
        val axis: ThemeAxis = ThemeAxis.Horizontal,
        val itemsBefore: Int = 2,
        val itemsAfter: Int = 2,
        val focusedScale: Float = 1.2f,
        val showTitle: Boolean = true,
    ) : ThemePrimary

    @Serializable
    @SerialName("grid")
    data class Grid(
        override val box: ThemeRect,
        override val art: ThemePicture? = null,
        val columns: Int = 5,
        /** Item width over height. */
        val itemAspect: Float = 0.75f,
        /** Gap between items as a fraction of the box width. */
        val spacing: Float = 0.01f,
        val showTitle: Boolean = false,
    ) : ThemePrimary

    @Serializable
    @SerialName("list")
    data class List(
        override val box: ThemeRect,
        override val art: ThemePicture? = null,
        val rows: Int = 10,
        val align: ThemeAlign = ThemeAlign.Start,
        val secondary: ThemeText? = null,
    ) : ThemePrimary
}
