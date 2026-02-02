package jr.brian.home.esde.setup

sealed class SetupStep {
    object Welcome : SetupStep()
    object RequestPermissions : SetupStep()
    object PermissionsGranted : SetupStep()
    object SelectScriptsFolder : SetupStep()
    object CreateScripts : SetupStep()
    object SelectMediaFolder : SetupStep()
    object EnableScriptsInESDE : SetupStep()
    object Complete : SetupStep()
    data class Warning(
        val type: WarningType,
        val path: String
    ) : SetupStep()
}