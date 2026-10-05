package com.laulegr.videoapp.model

/**
 * CapCut-style cut transitions. All of them are "edge" transitions: the outgoing
 * clip animates out over the last half of the duration, the incoming clip
 * animates in over the first half - clips never overlap, so the total video
 * length doesn't change when you add a transition.
 */
enum class TransitionType(val label: String) {
    NONE("Keiner"),
    FADE_BLACK("Schwarz"),
    FLASH_WHITE("Blitz"),
    ZOOM_IN("Zoom rein"),
    ZOOM_OUT("Zoom raus"),
    SPIN("Drehen"),
    SWIPE_LEFT("Wisch links"),
    SWIPE_RIGHT("Wisch rechts"),
    SWIPE_UP("Wisch hoch"),
    SHAKE("Wackeln"),
    GLITCH("Glitch"),
}

/** The transition at one cut, i.e. from a clip into the next one. */
data class Transition(
    val type: TransitionType = TransitionType.NONE,
    val durationMs: Long = DEFAULT_DURATION_MS,
) {
    companion object {
        const val DEFAULT_DURATION_MS = 300L
        const val MIN_DURATION_MS = 100L
        const val MAX_DURATION_MS = 1_500L
        val NONE = Transition()
    }
}
