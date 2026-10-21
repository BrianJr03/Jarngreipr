package jr.brian.home.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.core.model.ScreenSlot

/**
 * The [ScreenSlot] the current composition is rendering on. Supplied
 * by [ScreenSlotHost] at the activity's content root; constant for
 * the activity's lifetime because the pinned-slot design never moves
 * a host between displays.
 *
 * Replaces the old `rememberEffectiveAssignment` / `rememberPhysicalAssignment`
 * dance — layout decisions that used to peek at which display an
 * activity was reparented onto now read this static value instead.
 */
val LocalScreenSlot = staticCompositionLocalOf<ScreenSlot> {
    error("LocalScreenSlot not provided — content must be wrapped in ScreenSlotHost")
}
