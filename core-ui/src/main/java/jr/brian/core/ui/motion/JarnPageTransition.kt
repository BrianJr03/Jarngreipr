package jr.brian.core.ui.motion

/**
 * How a Jarn FE Canvas page arrives. The ids are stored in prefs and backups. Only the gentle
 * presets are offered here; the 3D ones stay in Classic.
 */
enum class JarnPageTransition(val id: String) {
    JarnSlide("jarn_slide"),
    Fade("fade"),
    Slide("slide"),
    Depth("depth"),
    Stack("stack"),
    GlideUp("glide_up"),
    Carousel("carousel"),
    ;

    companion object {
        fun fromId(id: String?): JarnPageTransition = entries.firstOrNull { it.id == id } ?: JarnSlide
    }
}

/**
 * The arriving page's layer at [offset], as a pager would show it: its own slot position plus the
 * preset's translation. Null for [JarnPageTransition.JarnSlide], which has its own short shift.
 */
fun JarnPageTransition.arrivingLayer(offset: Float, width: Float, height: Float, density: Float): PageLayer? {
    val layer = when (this) {
        JarnPageTransition.JarnSlide -> return null
        JarnPageTransition.Fade -> PageTransitionMath.fade(offset)
        JarnPageTransition.Slide -> PageTransitionMath.slide(offset, width)
        JarnPageTransition.Depth -> PageTransitionMath.depth(offset, width)
        JarnPageTransition.Stack -> PageTransitionMath.stack(offset, width, density)
        JarnPageTransition.GlideUp -> PageTransitionMath.glideUp(offset, height)
        JarnPageTransition.Carousel -> PageTransitionMath.carousel(offset, width)
    }
    return layer.copy(translationX = layer.translationX - width * offset)
}
