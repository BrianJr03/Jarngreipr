package jr.brian.frontend.look

/** How Jarn FE lays out systems or games: a scrolling grid or a one-row carousel. */
enum class JarnBrowseView(val id: String) {
    Grid("grid"),
    Carousel("carousel"),
    ;

    companion object {
        fun fromId(id: String?): JarnBrowseView = entries.firstOrNull { it.id == id } ?: Grid
    }
}
