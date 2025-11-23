package jr.brian.home.util

import androidx.annotation.StringRes
import jr.brian.home.R
import kotlin.random.Random

object OverlayInfoUtil {
    private val thorFacts = listOf(
        R.string.thor_fact_1,
        R.string.thor_fact_2,
        R.string.thor_fact_3,
        R.string.thor_fact_4,
        R.string.thor_fact_5,
        R.string.thor_fact_6,
        R.string.thor_fact_7,
        R.string.thor_fact_8,
        R.string.thor_fact_9,
        R.string.thor_fact_10,
        R.string.thor_fact_11,
        R.string.thor_fact_12,
        R.string.thor_fact_13,
        R.string.thor_fact_14,
        R.string.thor_fact_15,
        R.string.thor_fact_16,
        R.string.thor_fact_17,
        R.string.thor_fact_18,
        R.string.thor_fact_19,
        R.string.thor_fact_20,
        R.string.thor_fact_21,
        R.string.thor_fact_22,
        R.string.thor_fact_23,
        R.string.thor_fact_24,
        R.string.thor_fact_25,
        R.string.thor_fact_26,
        R.string.thor_fact_27,
        R.string.thor_fact_28,
        R.string.thor_fact_29,
        R.string.thor_fact_30,
        R.string.thor_fact_31,
        R.string.thor_fact_32,
        R.string.thor_fact_33,
        R.string.thor_fact_34,
        R.string.thor_fact_35,
        R.string.thor_fact_36,
        R.string.thor_fact_37,
        R.string.thor_fact_38,
        R.string.thor_fact_39,
        R.string.thor_fact_40,
        R.string.thor_fact_41,
        R.string.thor_fact_42,
        R.string.thor_fact_43,
        R.string.thor_fact_44,
        R.string.thor_fact_45,
        R.string.thor_fact_46,
        R.string.thor_fact_47,
        R.string.thor_fact_48,
        R.string.thor_fact_49,
        R.string.thor_fact_50
    )

    @StringRes
    fun getRandomFact(): Int {
        return thorFacts[Random.nextInt(thorFacts.size)]
    }
}
