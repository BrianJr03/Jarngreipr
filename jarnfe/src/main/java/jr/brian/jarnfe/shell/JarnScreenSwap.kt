package jr.brian.jarnfe.shell

import android.content.Context
import kotlinx.coroutines.flow.Flow

/** The saved choice of which physical screen shows Stage. Roles and focus never move with it. */
interface JarnScreenSwap {
    val isSwapped: Boolean
    val swapped: Flow<Boolean>

    fun requestSwap()
}

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnScreenSwapProvider {
    val jarnScreenSwap: JarnScreenSwap
}

fun Context.jarnScreenSwapOrNull(): JarnScreenSwap? =
    (applicationContext as? JarnScreenSwapProvider)?.jarnScreenSwap
