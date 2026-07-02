package jr.brian.home.model

data class TransferProgress(val fraction: Float) {
    val isComplete: Boolean get() = fraction >= 1f
}
