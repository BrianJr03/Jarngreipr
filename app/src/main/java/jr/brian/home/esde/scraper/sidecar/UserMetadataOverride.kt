package jr.brian.home.esde.scraper.sidecar

import kotlinx.serialization.Serializable

/**
 * Per-game manual overrides authored by the user through the Edit Info sheet.
 *
 * Stored at `<scrapedRoot>/<system>/.jarngreipr_user/<basename>.json`. Lives in
 * its own `.jarngreipr_user` directory so a user wiping scraper output (the
 * sibling `.jarngreipr/` folder [GameMetadataSidecar] writes into) does not
 * accidentally destroy their own manual edits.
 *
 * Every field is optional. A blank override is persisted as `null`, not an
 * empty string, so a text field cleared in the UI reads back as "no override"
 * rather than clobbering a gamelist value with an empty string. Image paths
 * point at files copied into `<scrapedRoot>/<system>/.jarngreipr_user/images/`
 * on save; see [UserMetadataOverrideStore] for the copy mechanics.
 */
@Serializable
data class UserMetadataOverride(
    val displayName: String? = null,
    val description: String? = null,
    val developer: String? = null,
    val publisher: String? = null,
    val genre: String? = null,
    val players: String? = null,
    val releaseDate: String? = null,
    val rating: Float? = null,
    val artworkPath: String? = null,
    val screenshotPath: String? = null,
    val fanartPath: String? = null,
) {
    fun isEmpty(): Boolean =
        displayName == null && description == null && developer == null &&
            publisher == null && genre == null && players == null &&
            releaseDate == null && rating == null && artworkPath == null &&
            screenshotPath == null && fanartPath == null
}
