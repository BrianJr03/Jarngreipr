package jr.brian.frontend.library.db

/** One game kept with the user data of the copies that are dropped. */
data class GameMerge(val keep: GameEntity, val dropIds: List<Long>)
