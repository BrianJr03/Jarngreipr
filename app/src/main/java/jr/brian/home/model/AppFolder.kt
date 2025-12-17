package jr.brian.home.model

data class AppFolder(
    val id: String,
    val name: String,
    val apps: List<String>,
    val x: Float = 0f,
    val y: Float = 0f,
    val iconSize: Float = 64f
)
