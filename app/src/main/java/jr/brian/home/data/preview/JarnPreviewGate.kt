package jr.brian.home.data.preview

/** Whether the Meet Jarn preview opens on its own, and what happened to it. Backed by its own prefs. */
interface JarnPreviewGate {
    /** True on a new install's first launch, or the first launch after an update, while showings are left and Jarn was never tried. */
    fun shouldShow(versionName: String): Boolean
    /** Counts one showing for [versionName]. Called when it opens on its own, never from Settings. */
    fun recordShown(versionName: String)
    /** Jarn opened from the preview or the Settings entry. Retires the preview for good. */
    fun markTried()
}
