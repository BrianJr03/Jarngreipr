package jr.brian.home.esde.model

const val FRONTEND_TRANSITION_MS_DEFAULT = 280
const val FRONTEND_TRANSITION_MS_MIN = 150
const val FRONTEND_TRANSITION_MS_MAX = 500
const val FRONTEND_TRANSITION_MS_STEP = 25

enum class FrontendTransition { None, Fade, Slide, Zoom, SlideUp;

    companion object {
        val Default: FrontendTransition = Fade

        fun fromStoredName(name: String?): FrontendTransition =
            entries.firstOrNull { it.name == name } ?: Default
    }
}
