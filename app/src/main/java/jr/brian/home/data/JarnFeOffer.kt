package jr.brian.home.data

import android.content.Context
import jr.brian.home.BuildConfig
import jr.brian.home.ui.util.hasJarnFeActivity

/**
 * The one gate for offering Jarn FE from Classic. Flip [OFFER_IN_RELEASE_BUILDS] once RC and
 * release builds ship the Jarn FE activities, which are in the debug manifest only today.
 */
object JarnFeOffer {
    private const val OFFER_IN_RELEASE_BUILDS = false

    fun isOffered(context: Context): Boolean = shouldOfferJarnFe(
        isDebugBuild = BuildConfig.DEBUG,
        offerInReleaseBuilds = OFFER_IN_RELEASE_BUILDS,
        activityInstalled = { hasJarnFeActivity(context) },
    )
}

/** Checks the build first, so a build without the offer never looks up the activity. */
internal fun shouldOfferJarnFe(
    isDebugBuild: Boolean,
    offerInReleaseBuilds: Boolean,
    activityInstalled: () -> Boolean,
): Boolean = (isDebugBuild || offerInReleaseBuilds) && activityInstalled()
