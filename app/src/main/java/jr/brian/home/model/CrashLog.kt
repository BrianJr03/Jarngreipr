package jr.brian.home.model

data class CrashLog(
    val fileName: String,
    val timestamp: Long,
    val content: String,
    val preview: String
)