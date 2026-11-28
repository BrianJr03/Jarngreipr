package jr.brian.frontend.look.themes

import jr.brian.core.model.theme.ThemeMeta
import jr.brian.core.model.theme.ThemeSource

/** Whether a theme may leave the device as a theme file. */
enum class ThemeShareVerdict {
    /** Made in Jarn: always shareable. */
    Allowed,

    /** An import whose license allows sharing; its author and license stay in the file as credit. */
    AllowedWithCredit,

    /** An import whose license forbids sharing, or doesn't say. */
    LicenseForbids,
}

private val TOKEN_SPLIT = Regex("[^A-Z0-9]+")
private val FORBIDDING = setOf("NC", "ND", "NONCOMMERCIAL", "NODERIVATIVES", "NODERIVS", "PROPRIETARY")
private val PERMISSIVE = setOf(
    "CC0", "MIT", "BSD", "APACHE", "GPL", "GPLV2", "GPLV3", "LGPL", "AGPL", "MPL", "ISC", "ZLIB",
    "UNLICENSE", "WTFPL", "OFL",
)

/**
 * Jarn themes always share. An import shares only when its license plainly allows it: Creative
 * Commons BY or BY-SA, CC0, public domain, or a common open source license. NC, ND, "all rights
 * reserved", an unknown license or none at all keep it on the device.
 */
fun themeShareVerdict(meta: ThemeMeta): ThemeShareVerdict {
    if (meta.source == ThemeSource.Jarn) return ThemeShareVerdict.Allowed
    val text = meta.license?.uppercase()?.trim().orEmpty()
    val tokens = text.split(TOKEN_SPLIT).filter { it.isNotEmpty() }.toSet()
    val forbids = tokens.any { it in FORBIDDING } || "ALL RIGHTS RESERVED" in text
    val creativeCommons = ("CC" in tokens || "CREATIVE" in tokens) && ("BY" in tokens || "ATTRIBUTION" in tokens)
    val allows = creativeCommons || tokens.any { it in PERMISSIVE } || "PUBLIC DOMAIN" in text
    return if (!forbids && allows) ThemeShareVerdict.AllowedWithCredit else ThemeShareVerdict.LicenseForbids
}
