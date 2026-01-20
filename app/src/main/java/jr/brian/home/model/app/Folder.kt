package jr.brian.home.model.app

data class Folder(
    val id: String,
    val name: String,
    val appPackageNames: List<String>,
    val position: AppPosition
)
