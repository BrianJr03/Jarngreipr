package jr.brian.home.esde.model

data class SetupResult(
    val success: Boolean,
    val message: String,
    val needsPermission: Boolean
)