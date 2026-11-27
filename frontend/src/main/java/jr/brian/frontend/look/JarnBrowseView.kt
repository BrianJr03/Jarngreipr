package jr.brian.frontend.look

/**
 * How Jarn FE lays out systems or games: a scrolling grid, a one-row carousel, or whatever the
 * chosen layout says ([SameAsLayout]). Older builds read an unknown id as [Grid].
 */
enum class JarnBrowseView(val id: String) {
    Grid("grid"),
    Carousel("carousel"),
    SameAsLayout("layout"),
    ;

    companion object {
        fun fromId(id: String?): JarnBrowseView = entries.firstOrNull { it.id == id } ?: Grid
    }
}
