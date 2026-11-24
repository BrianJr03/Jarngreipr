package jr.brian.home.jarn

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.jarnfe.home.JarnHomeApp
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads whether this build is Android's Home app. Any of our activities counts (Home lands on
 * [jr.brian.home.HomeRouterActivity]), and the package is this build's own, so a debug build and
 * a release build (both flavors share an id) each check themselves. Never changes the role, only
 * opens Android's picker.
 */
@Singleton
class AppJarnHomeApp @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : JarnHomeApp {
    private val roles: RoleManager? get() = context.getSystemService(RoleManager::class.java)

    override fun isHome(): Boolean {
        val roles = roles
        val roleAvailable = roles?.isRoleAvailable(RoleManager.ROLE_HOME) == true
        return jarngreiprIsHome(
            roleHeld = if (roleAvailable) roles?.isRoleHeld(RoleManager.ROLE_HOME) else null,
            resolvedHomePackage = resolvedHomePackage(),
            ownPackage = context.packageName,
        )
    }

    override fun pickerIntent(): Intent {
        val roles = roles
        return if (roles?.isRoleAvailable(RoleManager.ROLE_HOME) == true) {
            roles.createRequestRoleIntent(RoleManager.ROLE_HOME)
        } else {
            Intent(Settings.ACTION_HOME_SETTINGS)
        }
    }

    /** The package Home resolves to, or Android's chooser when no default is set. */
    private fun resolvedHomePackage(): String? {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return context.packageManager
            .resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName
    }
}

/**
 * The Home role decides when Android offers it. Otherwise the default Home resolution does,
 * where a chooser (no default set) resolves to another package and so reads as not Home.
 */
internal fun jarngreiprIsHome(roleHeld: Boolean?, resolvedHomePackage: String?, ownPackage: String): Boolean =
    roleHeld ?: (resolvedHomePackage == ownPackage)
