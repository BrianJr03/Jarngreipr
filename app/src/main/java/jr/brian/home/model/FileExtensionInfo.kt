package jr.brian.home.model

data class FileExtensionInfo(
    val extension: String,
    val fileCount: Int,
    val totalSize: Long,
    val filePaths: List<String> = emptyList()
)
