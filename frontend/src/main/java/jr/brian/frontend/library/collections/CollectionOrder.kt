package jr.brian.frontend.library.collections

/**
 * [ids] with [id] moved [delta] places, stopping at either end. Null when [id] is missing or
 * already at the end it is moving towards, so a no-op move writes nothing.
 */
fun collectionOrderMoved(ids: List<Long>, id: Long, delta: Int): List<Long>? {
    val from = ids.indexOf(id)
    if (from < 0) return null
    val to = (from + delta).coerceIn(ids.indices)
    if (to == from) return null
    return ids.toMutableList().apply { add(to, removeAt(from)) }
}
