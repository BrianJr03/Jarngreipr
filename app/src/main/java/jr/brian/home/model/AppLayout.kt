package jr.brian.home.model

data class AppLayout(
    val id: String,
    val name: String,
    val positions: Map<String, AppPosition>,
    val timestamp: Long = System.currentTimeMillis()
)
