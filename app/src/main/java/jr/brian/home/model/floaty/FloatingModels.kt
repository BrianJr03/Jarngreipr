package jr.brian.home.model.floaty
 
/** Input data used when initializing the floaty physics engine. */
data class FloatingAppInit(
    val id: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float = width
)
 
/** Per-item mutable state tracked by the floaty physics engine. */
data class FloatingAppState(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val width: Float,
    val height: Float = width
)