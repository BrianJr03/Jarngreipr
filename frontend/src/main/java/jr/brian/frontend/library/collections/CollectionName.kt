package jr.brian.frontend.library.collections

import jr.brian.frontend.library.db.CollectionEntity

const val COLLECTION_NAME_MAX_LENGTH = 40

/** What a create or rename did. Only [Saved] changed anything. Names are saved trimmed. */
sealed interface CollectionNameResult {
    data class Saved(val collectionId: Long) : CollectionNameResult
    data object Blank : CollectionNameResult
    data object TooLong : CollectionNameResult
    data object Duplicate : CollectionNameResult
    data object NotFound : CollectionNameResult
}

/**
 * Null when the trimmed [name] can be saved next to [existing]. [selfId] is the
 * collection being renamed, so a rename that only changes case is allowed.
 */
fun collectionNameProblem(
    name: String,
    existing: List<CollectionEntity>,
    selfId: Long? = null,
): CollectionNameResult? = when {
    name.isEmpty() -> CollectionNameResult.Blank
    name.length > COLLECTION_NAME_MAX_LENGTH -> CollectionNameResult.TooLong
    existing.any { it.id != selfId && it.name.equals(name, ignoreCase = true) } -> CollectionNameResult.Duplicate
    else -> null
}
