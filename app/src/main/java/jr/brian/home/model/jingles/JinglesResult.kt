package jr.brian.home.model.jingles

sealed class JinglesResult<out T> {
    data class Success<T>(val value: T) : JinglesResult<T>()
    data class Failure(val message: String, val cause: Throwable? = null) : JinglesResult<Nothing>()
}