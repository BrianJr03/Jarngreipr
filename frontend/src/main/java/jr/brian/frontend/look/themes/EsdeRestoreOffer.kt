package jr.brian.frontend.look.themes

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

private const val OFFER_FILE = "jarn_esde_restore_offer.json"
private const val MAX_OFFER_BYTES = 64L * 1024

private val offerJson = Json { ignoreUnknownKeys = true }

/**
 * A restored backup chose an ES-DE layout this device doesn't have. Kept until the player brings it
 * back, says no, or picks another layout, so a missing SD card only delays the offer.
 */
@Serializable
data class EsdeRestoreOffer(
    val themeId: String,
    val sourceFolder: String,
    val variant: String? = null,
    val colorScheme: String? = null,
    val aspectRatio: String? = null,
)

/** Where the offer lives: beside `themes`, not in it, so it never reads as a theme. */
fun esdeRestoreOfferFile(filesDir: File): File = File(filesDir, OFFER_FILE)

fun readEsdeRestoreOffer(file: File): EsdeRestoreOffer? = try {
    file.takeIf { it.isFile && it.length() <= MAX_OFFER_BYTES }?.readText()
        ?.let { offerJson.decodeFromString<EsdeRestoreOffer>(it) }
} catch (_: SerializationException) {
    null
} catch (_: IllegalArgumentException) {
    null
} catch (_: IOException) {
    null
}

/** Saves [offer], or clears the file for null. Through a temp file, so a crash never leaves half an offer. */
fun writeEsdeRestoreOffer(file: File, offer: EsdeRestoreOffer?) {
    if (offer == null) {
        file.delete()
        return
    }
    file.parentFile?.mkdirs()
    val temp = File(file.path + ".tmp")
    temp.writeText(offerJson.encodeToString(EsdeRestoreOffer.serializer(), offer))
    if (!temp.renameTo(file)) {
        temp.delete()
        throw IOException("Couldn't save $OFFER_FILE")
    }
}

/** [drop] means the offer no longer applies at all, as opposed to waiting for an SD card or access. */
internal data class EsdeRestoreCheck(val update: EsdeThemeUpdate?, val drop: Boolean)

/**
 * The re-import an offer stands for, when it still applies and its folder can be read now:
 * [activeThemeId] is still the restored id, that theme isn't installed, Jarn has file access and
 * the folder is still a theme.
 */
internal fun checkEsdeRestoreOffer(
    offer: EsdeRestoreOffer,
    activeThemeId: String?,
    isInstalled: (String) -> Boolean,
    locator: EsdeThemeLocator,
    readStamp: (File) -> EsdeThemeStamp?,
): EsdeRestoreCheck {
    if (offer.themeId != activeThemeId || isInstalled(offer.themeId)) return EsdeRestoreCheck(null, drop = true)
    if (locator.scan() == EsdeThemeScan.NoAccess) return EsdeRestoreCheck(null, drop = false)
    val source = File(offer.sourceFolder)
    val stamp = readStamp(source)?.takeIf { it.capabilitiesModifiedMs != null } ?: return EsdeRestoreCheck(null, drop = false)
    val record = EsdeImportRecord(
        sourceFolder = offer.sourceFolder,
        variant = offer.variant,
        colorScheme = offer.colorScheme,
        aspectRatio = offer.aspectRatio,
        stamp = stamp,
    )
    return EsdeRestoreCheck(EsdeThemeUpdate(offer.themeId, record, source, stamp), drop = false)
}
