package jr.brian.home.esde.setup

data class SetupResult(
    val success: Boolean,
    val message: String,
    val needsPermission: Boolean
)